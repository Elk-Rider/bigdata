package jcpark.datamock.generator;

import jcpark.datamock.config.DbConfig;
import jcpark.datamock.util.RandomDataUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 交易订单表 + 交易明细表 Mock 数据生成器
 *
 * 生产节奏：每秒产生 1~10 条新订单（status=0, 待支付）
 *
 * 订单生命周期（真实业务模拟）：
 *   status=0  待支付  ──────────────────────────────────────┐
 *              │ delay: 2~60s (65%)        (20%)  (15%)    │
 *              ▼                            ▼      ▼       │
 *   status=1  已支付  ──► 生成交易明细   status=5  (保持待支付/废弃)
 *              │ delay: 5~30s
 *              ▼
 *   status=2  已发货
 *              │ delay: 10~60s
 *              ▼ (90%)           (10%)
 *   status=3  已完成           status=4  已退款
 *
 * 只有订单状态流转到"已支付(1)"时才会向 trade 表写入交易明细。
 */
public class OrderTradeGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Random RANDOM = new Random();

    private final List<String> userIds;
    /** 调度器：负责 1s 产单 + 各订单生命周期延迟回调 */
    private final ScheduledExecutorService scheduler;

    // ─── 概率配置 ───
    /** 待支付→已支付 概率 65% */
    private static final int PROB_PAY    = 65;
    /** 待支付→已取消 概率 20% */
    private static final int PROB_CANCEL = 85; // cumulative: 65+20
    /** 其余 15% 保持待支付（模拟废弃订单，不再流转） */

    /** 已支付→已退款（跳过发货）概率 5% */
    private static final int PROB_DIRECT_REFUND = 5;
    /** 已发货→已退款 概率 10% */
    private static final int PROB_REFUND_AFTER_SHIP = 10;

    public OrderTradeGenerator(List<String> userIds) {
        this.userIds = userIds;
        this.scheduler = Executors.newScheduledThreadPool(6,
                r -> { Thread t = new Thread(r, "order-trade-pool"); t.setDaemon(true); return t; });
    }

    /** 启动每秒产单调度 */
    public void start() {
        scheduler.scheduleAtFixedRate(this::produceOrders, 1, 1, TimeUnit.SECONDS);
        System.out.println("[OrderTradeGenerator] 启动：每秒产生 1~10 笔新订单");
    }

    // =========================================================
    // 第一阶段：每秒创建 1~10 笔新订单（status=0）
    // =========================================================
    private void produceOrders() {
        int count = RandomDataUtil.randInt(1, 10);
        try (Connection conn = DbConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (int i = 0; i < count; i++) {
                    createOrder(conn);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("[OrderTradeGenerator] 创建订单异常: " + e.getMessage());
        }
    }

    private void createOrder(Connection conn) throws SQLException {
        String orderId    = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        String userId     = userIds.get(RANDOM.nextInt(userIds.size()));
        String tradeType  = RandomDataUtil.pick(RandomDataUtil.ORDER_TRADE_TYPES);
        String currency   = RandomDataUtil.randomCurrency();
        double totalAmt   = RandomDataUtil.randomAmount(tradeType);
        double discount   = Math.round(totalAmt * RandomDataUtil.randDouble(0, RandomDataUtil.DISCOUNT_RATE_MAX) * 100.0) / 100.0;
        double payAmt     = Math.round((totalAmt - discount) * 100.0) / 100.0;
        LocalDateTime now = LocalDateTime.now();

        String sql = "INSERT INTO `order` " +
                "(order_id, user_id, order_status, total_amount, pay_amount, discount_amount, currency, create_time, pay_time, update_time) " +
                "VALUES (?,?,0,?,?,?,?,?,NULL,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, userId);
            ps.setBigDecimal(3, BigDecimal.valueOf(totalAmt).setScale(4, RoundingMode.HALF_UP));
            ps.setBigDecimal(4, BigDecimal.valueOf(payAmt).setScale(4, RoundingMode.HALF_UP));
            ps.setBigDecimal(5, BigDecimal.valueOf(discount).setScale(4, RoundingMode.HALF_UP));
            ps.setString(6, currency);
            ps.setString(7, now.format(FMT));
            ps.setString(8, now.format(FMT));
            ps.executeUpdate();
        }

        // 调度此订单的生命周期流转（2~60秒后执行）
        int delayPaySec = RandomDataUtil.randInt(2, 60);
        scheduler.schedule(
                () -> transitionToPay(orderId, userId, payAmt, currency, tradeType),
                delayPaySec, TimeUnit.SECONDS
        );
    }

    // =========================================================
    // 第二阶段：待支付 → 已支付/已取消/废弃
    // =========================================================
    private void transitionToPay(String orderId, String userId, double payAmt, String currency, String tradeType) {
        int r = RANDOM.nextInt(100);
        try {
            if (r < PROB_PAY) {
                // 65%: 支付成功
                LocalDateTime payTime = LocalDateTime.now();
                updateOrderStatus(orderId, 1, payTime);
                insertTrade(orderId, userId, payAmt, currency, tradeType, payTime);

                // 5%: 支付后直接退款
                if (RANDOM.nextInt(100) < PROB_DIRECT_REFUND) {
                    int refundDelay = RandomDataUtil.randInt(5, 120);
                    scheduler.schedule(() -> {
                        try {
                            updateOrderStatus(orderId, 4, null);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    }, refundDelay, TimeUnit.SECONDS);
                } else {
                    // 进入发货流程
                    int shipDelay = RandomDataUtil.randInt(5, 30);
                    scheduler.schedule(() -> transitionToShip(orderId), shipDelay, TimeUnit.SECONDS);
                }

            } else if (r < PROB_CANCEL) {
                // 20%: 取消
                updateOrderStatus(orderId, 5, null);
            }
            // 15%: 保持 status=0，模拟废弃订单，不再处理

        } catch (Exception e) {
            System.err.println("[OrderTradeGenerator] 订单流转异常 [" + orderId + "]: " + e.getMessage());
        }
    }

    // =========================================================
    // 第三阶段：已支付 → 已发货
    // =========================================================
    private void transitionToShip(String orderId) {
        try {
            updateOrderStatus(orderId, 2, null);
            int completeDelay = RandomDataUtil.randInt(10, 60);
            scheduler.schedule(() -> transitionToComplete(orderId), completeDelay, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("[OrderTradeGenerator] 发货异常 [" + orderId + "]: " + e.getMessage());
        }
    }

    // =========================================================
    // 第四阶段：已发货 → 已完成 / 已退款
    // =========================================================
    private void transitionToComplete(String orderId) {
        try {
            int newStatus = RANDOM.nextInt(100) < PROB_REFUND_AFTER_SHIP ? 4 : 3;
            updateOrderStatus(orderId, newStatus, null);
        } catch (Exception e) {
            System.err.println("[OrderTradeGenerator] 完成/退款异常 [" + orderId + "]: " + e.getMessage());
        }
    }

    // =========================================================
    // 辅助：更新订单状态
    // =========================================================
    private void updateOrderStatus(String orderId, int status, LocalDateTime payTime) throws SQLException {
        String sql = payTime != null
                ? "UPDATE `order` SET order_status=?, pay_time=?, update_time=? WHERE order_id=?"
                : "UPDATE `order` SET order_status=?, update_time=? WHERE order_id=?";

        try (Connection conn = DbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            if (payTime != null) {
                ps.setInt(1, status);
                ps.setString(2, payTime.format(FMT));
                ps.setString(3, now.format(FMT));
                ps.setString(4, orderId);
            } else {
                ps.setInt(1, status);
                ps.setString(2, now.format(FMT));
                ps.setString(3, orderId);
            }
            ps.executeUpdate();
        }
    }

    // =========================================================
    // 辅助：向 trade 表插入交易明细（订单支付成功时触发）
    // =========================================================
    private void insertTrade(String orderId, String senderUserId, double amount,
                              String currency, String tradeType, LocalDateTime tradeTime) throws SQLException {
        // 收款方：从用户池中随机选一个不同的用户（模拟 P2P / 平台账户）
        String receiverUserId = senderUserId;
        while (receiverUserId.equals(senderUserId)) {
            receiverUserId = userIds.get(RANDOM.nextInt(userIds.size()));
        }
        String channel = RandomDataUtil.pick(RandomDataUtil.TRADE_CHANNELS);

        String sql = "INSERT INTO `trade` " +
                "(order_id, sender_user_id, receiver_user_id, trade_channel, trade_time, trade_type, amount, currency) " +
                "VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderId);
            ps.setString(2, senderUserId);
            ps.setString(3, receiverUserId);
            ps.setString(4, channel);
            ps.setString(5, tradeTime.format(FMT));
            ps.setString(6, tradeType);
            ps.setBigDecimal(7, BigDecimal.valueOf(amount).setScale(6, RoundingMode.HALF_UP));
            ps.setString(8, currency);
            ps.executeUpdate();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
