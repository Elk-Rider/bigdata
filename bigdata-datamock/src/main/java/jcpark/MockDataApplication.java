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
