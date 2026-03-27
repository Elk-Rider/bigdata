package jcpark;

import jcpark.generator.OrderTradeGenerator;
import jcpark.generator.PriceGenerator;
import jcpark.generator.UserGenerator;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock 数据生成主程序入口
 *
 * 启动流程：
 *   1. 初始化用户池（首次运行写入 100 名用户，再次运行直接加载）
 *   2. 启动价格生成器（立即执行一次，之后每 30 秒全量刷新）
 *   3. 启动订单+交易生成器（每秒产生 1~10 笔订单，异步完成生命周期流转）
 *
 * 运行方式：
 *   mvn compile exec:java -Dexec.mainClass=jcpark.datamock.MockDataApplication
 *   或打包后：java -jar bigdata-datamock-1.0-SNAPSHOT.jar
 *
 *   项目结构
 * bigdata-datamock/
 * ├── pom.xml                          ← 新增 mysql-connector-java 8.0.33
 * └── src/main/
 * ├── resources/
 * │   └── application.properties   ← DB 连接配置（bjc55:3306）
 * └── java/org/example/
 * ├── MockDataApplication.java  ← 主入口
 * ├── config/DbConfig.java      ← JDBC 连接工厂
 * ├── util/RandomDataUtil.java  ← 随机数据工具库
 * └── generator/
 * ├── UserGenerator.java        ← 用户表（100人）
 * ├── PriceGenerator.java       ← 价格表（每30秒）
 * └── OrderTradeGenerator.java  ← 订单+交易（每秒1-10条）
 *
 *
 * 核心设计说明
 * 用户表（100人）
 * 12个国家按比例分布（USA 25、China 15、UK 10……），每人自带真实姓名库、对应语言、地理位置、注册时间；monitor_currency_config / monitor_transaction_config / user_preferences 三个 JSON 字段留 NULL 供你自行补充。
 *
 * 价格表（每30秒全量）
 * 覆盖 24 种资产：
 *
 * 类别	资产	波动率	数据源
 * Cryptocurrency	BTC/ETH/BNB/SOL/XRP/ADA/DOGE/AVAX	1.2~2.5%	Binance
 * Metal	XAU/XAG/XPT	0.4~0.6%	Kitco
 * Stock	AAPL/GOOGL/TSLA/AMZN/MSFT/NVDA/META	0.7~1.5%	Yahoo Finance
 * Forex	EURUSD/GBPUSD/USDJPY/AUDUSD/USDCAD/USDCHF	0.07~0.09%	OANDA
 * 使用随机游走模型（高斯分布）模拟真实市场波动，用 REPLACE INTO 每轮全量刷新。
 *
 * 订单+交易（生命周期模拟）
 * status=0 待支付
 * ├── 65% → status=1 已支付 → 【立即写 trade 表】
 * │     ├── 5% 直接退款 → status=4
 * │     └── 95% → status=2 已发货 → (90%)status=3 已完成 / (10%)status=4 退款
 * ├── 20% → status=5 已取消
 * └── 15% → 废弃（模拟未支付就流失的订单，不流转）
 */
public class MockDataApplication {

    public static void main(String[] args) throws Exception {

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║       BigData Mock Data Generator v1.0       ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();

        // ── 1. 用户数据初始化 ──────────────────────────────
        UserGenerator userGenerator = new UserGenerator();
        List<String> userIds = userGenerator.initUsers();

        if (userIds.isEmpty()) {
            System.err.println("[启动失败] 用户池为空，请检查数据库连接和 user 表。");
            System.exit(1);
        }
        System.out.printf("[Main] 用户池就绪，共 %d 名用户%n%n", userIds.size());

        // ── 2. 价格生成器（立即执行 + 每 30 秒全量刷新）──────
        PriceGenerator priceGenerator = new PriceGenerator();
        ScheduledExecutorService priceScheduler = Executors.newSingleThreadScheduledExecutor(
                r -> { Thread t = new Thread(r, "price-scheduler"); t.setDaemon(true); return t; }
        );
        priceScheduler.scheduleAtFixedRate(priceGenerator, 0, 30, TimeUnit.SECONDS);
        System.out.println("[Main] 价格生成器已启动 → 立即执行，之后每 30 秒全量刷新");

        // ── 3. 订单 + 交易生成器（每秒 1~10 笔）─────────────
        OrderTradeGenerator orderTradeGenerator = new OrderTradeGenerator(userIds);
        orderTradeGenerator.start();
        System.out.println("[Main] 订单/交易生成器已启动 → 每秒产生 1~10 笔新订单");

        System.out.println();
        System.out.println("所有生成器运行中，按 Ctrl+C 停止...");
        System.out.println("─────────────────────────────────────────────");

        // 注册 JVM 关闭钩子，优雅退出
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[Main] 收到停止信号，正在关闭...");
            priceScheduler.shutdown();
            orderTradeGenerator.shutdown();
            System.out.println("[Main] 已安全停止。");
        }));

        // 主线程保持存活
        Thread.currentThread().join();
    }
}
