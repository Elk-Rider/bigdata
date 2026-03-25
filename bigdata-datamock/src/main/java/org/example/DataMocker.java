package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class DataMocker {

    // === 配置参数 ===
    private static final String BOOTSTRAP_SERVERS = "bjc55:9092"; // 替换为你的 Kafka 地址
    private static final String TOPIC_USER = "USERS";
    private static final String TOPIC_TRADE = "TRADES";

    private static final ObjectMapper mapper = new ObjectMapper();
    private static KafkaProducer<String, String> producer;
    private static volatile boolean running = true;

    // 基础价格与随机生成器
    private static double basePrice = 65000.0;
    private static final Random random = new Random();

    // 模拟的用户池
    private static final List<Map<String, Object>> userPool = new ArrayList<>();

    static {
        // 初始化用户池
        String[] names = {"赵", "孙", "钱", "李", "无","郑", "周", "王"};
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("user_id", 1001 + i);
            user.put("username", names[i]);
            user.put("risk_level", random.nextInt(3) + 1);
            userPool.add(user);
        }
    }

    public static void main(String[] args) {
        initProducer();

        // 注册优雅停机钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n收到退出信号，正在关闭 Kafka Producer...");
            running = false;
            if (producer != null) {
                producer.flush();
                producer.close();
            }
            System.out.println("Mock 程序已安全退出。");
        }));

        System.out.println("开始生成 Web3 模拟数据... (按 Ctrl+C 停止)");

        int userChangeCounter = 0;

        while (running) {
            try {
                // 1. 生成高频成交流 (每秒生成 10-20 条)
                int tradesToGen = 10 + random.nextInt(11); // 10 到 20
                for (int i = 0; i < tradesToGen; i++) {
                    mockTradeStream();
                }

                // 2. 模拟用户维表变更 (大约每 5 秒发生一次)
                userChangeCounter++;
                if (userChangeCounter >= 5) {
                    mockUserCdc();
                    userChangeCounter = 0;
                }

                // 控制生成频率，每秒循环一次
                TimeUnit.SECONDS.sleep(1);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                System.err.println("数据生成发生异常: " + e.getMessage());
            }
        }
    }

    /**
     * 初始化 Kafka Producer
     */
    private static void initProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // 建议在本地测试时开启幂等性或者调整 acks
        props.put(ProducerConfig.ACKS_CONFIG, "1");

        try {
            producer = new KafkaProducer<>(props);
            System.out.println("Kafka Producer 初始化成功连接至: " + BOOTSTRAP_SERVERS);
        } catch (Exception e) {
            System.err.println("Kafka 连接失败，请检查配置！如果只是测试打印，请忽略。错误: " + e.getMessage());
            producer = null;
        }
    }

    /**
     * 发送数据 (如果 Kafka 连不上，则退化为控制台打印)
     */
    private static void sendData(String topic, Map<String, Object> data) {
        try {
            String jsonStr = mapper.writeValueAsString(data);
            if (producer != null) {
                // 在实战中，建议把有业务含义的字段（如 user_id 或 trade_id）作为 Key 打入 Kafka，保证局部有序
                producer.send(new ProducerRecord<>(topic, jsonStr));
            } else {
                System.out.println("[" + topic + "] " + jsonStr);
            }
        } catch (Exception e) {
            System.err.println("JSON 序列化或发送失败: " + e.getMessage());
        }
    }

    /**
     * 模拟生成一笔交易流水
     */
    private static void mockTradeStream() {
        // 让价格在 -0.1% 到 +0.1% 之间随机波动
        double pctChange = (random.nextDouble() * 0.002) - 0.001;
        basePrice = basePrice * (1 + pctChange);

        // 随机抽一个用户
        Map<String, Object> user = userPool.get(random.nextInt(userPool.size()));

        Map<String, Object> tradeData = new HashMap<>();
        tradeData.put("ts", System.currentTimeMillis());
        tradeData.put("trade_id", UUID.randomUUID().toString());
        tradeData.put("user_id", user.get("user_id"));
        tradeData.put("symbol", "BTC-USDT");
        tradeData.put("price", Math.round(basePrice * 100.0) / 100.0); // 保留两位小数
        tradeData.put("qty", Math.round((0.01 + random.nextDouble() * 0.49) * 10000.0) / 10000.0); // 0.01 到 0.50 之间，保留四位小数

        sendData(TOPIC_TRADE, tradeData);
    }

    /**
     * 模拟生成一次用户资料变更 (CDC)
     */
    private static void mockUserCdc() {
        Map<String, Object> user = userPool.get(random.nextInt(userPool.size()));

        // 变更风险等级
        int newRiskLevel = random.nextInt(3) + 1;
        user.put("risk_level", newRiskLevel);

        Map<String, Object> cdcData = new HashMap<>();
        cdcData.put("user_id", user.get("user_id"));
        cdcData.put("username", user.get("username"));
        cdcData.put("risk_level", newRiskLevel);
        cdcData.put("op_type", random.nextDouble() > 0.2 ? "UPDATE" : "INSERT");

        sendData(TOPIC_USER, cdcData);
    }
}