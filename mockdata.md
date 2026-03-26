项目结构
bigdata-datamock/
├── pom.xml                          ← 新增 mysql-connector-java 8.0.33
└── src/main/
├── resources/
│   └── application.properties   ← DB 连接配置（bjc55:3306）
└── java/org/example/
├── MockDataApplication.java  ← 主入口
├── config/DbConfig.java      ← JDBC 连接工厂
├── util/RandomDataUtil.java  ← 随机数据工具库
└── generator/
├── UserGenerator.java        ← 用户表（100人）
├── PriceGenerator.java       ← 价格表（每30秒）
└── OrderTradeGenerator.java  ← 订单+交易（每秒1-10条）


核心设计说明
用户表（100人）
12个国家按比例分布（USA 25、China 15、UK 10……），每人自带真实姓名库、对应语言、地理位置、注册时间；monitor_currency_config / monitor_transaction_config / user_preferences 三个 JSON 字段留 NULL 供你自行补充。

价格表（每30秒全量）
覆盖 24 种资产：

类别	资产	波动率	数据源
Cryptocurrency	BTC/ETH/BNB/SOL/XRP/ADA/DOGE/AVAX	1.2~2.5%	Binance
Metal	XAU/XAG/XPT	0.4~0.6%	Kitco
Stock	AAPL/GOOGL/TSLA/AMZN/MSFT/NVDA/META	0.7~1.5%	Yahoo Finance
Forex	EURUSD/GBPUSD/USDJPY/AUDUSD/USDCAD/USDCHF	0.07~0.09%	OANDA
使用随机游走模型（高斯分布）模拟真实市场波动，用 REPLACE INTO 每轮全量刷新。

订单+交易（生命周期模拟）
status=0 待支付
├── 65% → status=1 已支付 → 【立即写 trade 表】
│     ├── 5% 直接退款 → status=4
│     └── 95% → status=2 已发货 → (90%)status=3 已完成 / (10%)status=4 退款
├── 20% → status=5 已取消
└── 15% → 废弃（模拟未支付就流失的订单，不流转）