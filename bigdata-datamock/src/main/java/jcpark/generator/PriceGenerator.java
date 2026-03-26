package jcpark.generator;

import jcpark.config.DbConfig;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 交易价格表 Mock 数据生成器
 *
 * 每 30 秒生成一次全量价格快照，使用随机游走模型模拟真实市场波动。
 * 使用 REPLACE INTO：每次刷新时通过唯一索引 uk_asset_source 替换旧记录。
 *
 * 资产覆盖：
 *   - 加密货币 (Cryptocurrency): BTC ETH BNB SOL XRP ADA DOGE AVAX
 *   - 贵金属 (Metal):            XAU(黄金) XAG(白银) XPT(铂金)
 *   - 股票 (Stock):              AAPL GOOGL TSLA AMZN MSFT NVDA META
 *   - 外汇 (Forex):              EURUSD GBPUSD USDJPY AUDUSD USDCAD USDCHF
 */
public class PriceGenerator implements Runnable {

    private static final Random RANDOM = new Random();
    private final AtomicInteger round = new AtomicInteger(0);

    // =========================================================
    // 资产基础价格定义（2026年参考价）
    // 每次调用时在此基础上做 ±0.5%~2% 的随机游走
    // =========================================================
    private static class Asset {
        final String symbol;
        final String name;
        final String type;
        final double basePrice;
        final String source;
        final double volatility; // 每次波动标准差百分比

        Asset(String symbol, String name, String type, double basePrice, String source, double volatility) {
            this.symbol = symbol;
            this.name = name;
            this.type = type;
            this.basePrice = basePrice;
            this.source = source;
            this.volatility = volatility;
        }
    }

    /** 所有资产列表（当前价格随游走累积，程序重启后从 basePrice 重新出发） */
    private final double[] currentPrices;

    private static final List<Asset> ASSETS = Arrays.asList(
        // ---- 加密货币 ---- 波动较大 (1%~3%)
        new Asset("BTC",    "Bitcoin",        "Cryptocurrency",  95000.00,  "Binance",       0.015),
        new Asset("ETH",    "Ethereum",       "Cryptocurrency",   3500.00,  "Binance",       0.015),
        new Asset("BNB",    "BNB",            "Cryptocurrency",    620.00,  "Binance",       0.012),
        new Asset("SOL",    "Solana",         "Cryptocurrency",    185.00,  "Binance",       0.018),
        new Asset("XRP",    "XRP",            "Cryptocurrency",      0.65,  "Binance",       0.020),
        new Asset("ADA",    "Cardano",        "Cryptocurrency",      0.48,  "Binance",       0.020),
        new Asset("DOGE",   "Dogecoin",       "Cryptocurrency",      0.13,  "Binance",       0.025),
        new Asset("AVAX",   "Avalanche",      "Cryptocurrency",     38.50,  "Binance",       0.018),

        // ---- 贵金属 ---- 波动较小 (0.3%~0.8%)
        new Asset("XAU",    "Gold",           "Metal",           2250.00,  "Kitco",         0.004),
        new Asset("XAG",    "Silver",         "Metal",             27.50,  "Kitco",         0.006),
        new Asset("XPT",    "Platinum",       "Metal",            960.00,  "Kitco",         0.005),

        // ---- 股票 ---- 中等波动 (0.5%~1.5%)
        new Asset("AAPL",   "Apple Inc.",     "Stock",            225.00,  "Yahoo Finance", 0.008),
        new Asset("GOOGL",  "Alphabet Inc.",  "Stock",            178.00,  "Yahoo Finance", 0.009),
        new Asset("TSLA",   "Tesla Inc.",     "Stock",            285.00,  "Yahoo Finance", 0.015),
        new Asset("AMZN",   "Amazon.com",     "Stock",            200.00,  "Yahoo Finance", 0.009),
        new Asset("MSFT",   "Microsoft Corp.","Stock",            420.00,  "Yahoo Finance", 0.007),
        new Asset("NVDA",   "NVIDIA Corp.",   "Stock",            960.00,  "Yahoo Finance", 0.012),
        new Asset("META",   "Meta Platforms", "Stock",            590.00,  "Yahoo Finance", 0.010),

        // ---- 外汇 ---- 波动极小 (0.05%~0.15%)
        new Asset("EURUSD", "Euro / USD",     "Forex",              1.085, "OANDA",         0.0008),
        new Asset("GBPUSD", "GBP / USD",      "Forex",              1.272, "OANDA",         0.0009),
        new Asset("USDJPY", "USD / JPY",      "Forex",            149.50,  "OANDA",         0.0007),
        new Asset("AUDUSD", "AUD / USD",      "Forex",              0.653, "OANDA",         0.0008),
        new Asset("USDCAD", "USD / CAD",      "Forex",              1.362, "OANDA",         0.0007),
        new Asset("USDCHF", "USD / CHF",      "Forex",              0.893, "OANDA",         0.0007)
    );

    public PriceGenerator() {
        currentPrices = new double[ASSETS.size()];
        for (int i = 0; i < ASSETS.size(); i++) {
            currentPrices[i] = ASSETS.get(i).basePrice;
        }
    }

    @Override
    public void run() {
        int r = round.incrementAndGet();
        try (Connection conn = DbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(buildReplaceSql())) {

            for (int i = 0; i < ASSETS.size(); i++) {
                Asset asset = ASSETS.get(i);

                // 随机游走：新价格 = 当前价格 × (1 + N(0, volatility))
                double change = 1.0 + RANDOM.nextGaussian() * asset.volatility;
                // 防止价格偏离基础价太远（基础价 ±40%）
                double newPrice = currentPrices[i] * change;
                double lower = asset.basePrice * 0.60;
                double upper = asset.basePrice * 1.40;
                newPrice = Math.max(lower, Math.min(upper, newPrice));

                // 按资产类型确定精度
                int scale = asset.type.equals("Forex") ? 6 : (asset.type.equals("Cryptocurrency") && asset.basePrice < 1 ? 8 : 2);
                currentPrices[i] = BigDecimal.valueOf(newPrice).setScale(scale, RoundingMode.HALF_UP).doubleValue();

                ps.setString(1, asset.symbol);
                ps.setString(2, asset.name);
                ps.setString(3, asset.type);
                ps.setBigDecimal(4, BigDecimal.valueOf(currentPrices[i]).setScale(8, RoundingMode.HALF_UP));
                ps.setString(5, asset.source);
                ps.setInt(6, 1);
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            System.out.printf("[PriceGenerator] 第 %d 轮 | 更新 %d 条价格记录%n", r, results.length);

        } catch (SQLException e) {
            System.err.println("[PriceGenerator] 价格写入异常: " + e.getMessage());
        }
    }

    /**
     * REPLACE INTO 策略：
     * 触发 uk_asset_source 唯一索引冲突时删除旧行并写入新行，
     * update_time 由 DEFAULT CURRENT_TIMESTAMP(3) 自动填充。
     */
    private String buildReplaceSql() {
        return "REPLACE INTO `price` " +
               "(asset_symbol, asset_name, trade_type, price_usd, price_source, status) " +
               "VALUES (?, ?, ?, ?, ?, ?)";
    }
}
