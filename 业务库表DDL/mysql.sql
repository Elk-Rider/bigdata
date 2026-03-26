--交易明细表

CREATE TABLE `trade`
(
    -- 交易唯一标识，作为主键便于 Flink CDC 追踪位点
    `trade_id`         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '交易唯一ID',
    `order_id`         VARCHAR(64)    NOT NULL COMMENT '订单全局唯一标识',
    -- 交易双方：关联你维度模型中的 User ID
    `sender_user_id`   VARCHAR(64)    NOT NULL COMMENT '交易转出方(用户ID)',
    `receiver_user_id` VARCHAR(64)    NOT NULL COMMENT '交易转入方(用户ID)',
    -- 交易渠道：如 Meta, AppsFlyer, 或具体的支付网关 [cite: 1, 6]
    `trade_channel`    VARCHAR(50)    NOT NULL COMMENT '交易渠道(如Meta/Google/IAP)',
    -- 交易时间：建议精确到毫秒，满足实时看板需求
    `trade_time`       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '交易发生时间',
    -- 交易种类：如内购(IAP)、广告变现(Ad Revenue)、订阅(Subscription)
    `trade_type`       VARCHAR(30)    NOT NULL COMMENT '交易种类(IAP/Revenue/Subscription)',
    -- 交易量：使用 DECIMAL 保证金融级计算精度
    `amount`           DECIMAL(18, 4) NOT NULL DEFAULT '0.0000' COMMENT '交易金额/价值',
    -- 补充：币种信息，方便计算 ROAS [cite: 1, 6]
    `currency`         VARCHAR(10)             DEFAULT 'USD' COMMENT '币种',
    PRIMARY KEY (`trade_id`),
    -- 索引优化：方便按时间范围查询流水
    KEY                `idx_trade_time` (`trade_time`),
    -- 索引优化：方便关联转出方进行风控分析
    KEY                `idx_sender_user` (`sender_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时交易流水事实表';


--交易订单表
CREATE TABLE `order`
(
    -- 订单基础信息
    `order_id`        VARCHAR(64)    NOT NULL COMMENT '订单全局唯一标识',
    `user_id`         VARCHAR(64)    NOT NULL COMMENT '关联业务系统User ID',
    -- 订单生命周期状态
    `order_status`    TINYINT        NOT NULL DEFAULT '0' COMMENT '状态: 0-待支付, 1-已支付, 2-已发货, 3-已完成, 4-已退款, 5-已取消',
    -- 金额相关 (参考 AppsFlyer 收入指标 )
    `total_amount`    DECIMAL(18, 4) NOT NULL COMMENT '订单总金额(应付)',
    `pay_amount`      DECIMAL(18, 4) NOT NULL COMMENT '实际支付金额(实付)',
    `discount_amount` DECIMAL(18, 4)          DEFAULT '0.0000' COMMENT '优惠金额',
    `currency`        VARCHAR(10)             DEFAULT 'USD' COMMENT '币种',
    -- 时间维度
    `create_time`     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '下单时间',
    `pay_time`        DATETIME(3) NULL COMMENT '支付完成时间',
    `update_time`     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最后更新时间',
    PRIMARY KEY (`order_id`),
    -- 索引优化
    KEY               `idx_user_id` (`user_id`),
    KEY               `idx_create_time` (`create_time`),
    KEY               `idx_campaign` (`campaign_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务订单事实表';


CREATE TABLE `users`
(
    -- 核心关联键
    `user_id`                    VARCHAR(64) NOT NULL COMMENT '业务系统内部唯一用户ID',
    -- ID Mapping (用于跨平台关联)
    `appsflyer_id`               VARCHAR(128) COMMENT 'AppsFlyer 设备唯一ID, 用于关联归因数据',
    -- 基础属性 (对应 Meta/GA4 人口统计数据)
    `user_name`                  VARCHAR(100) COMMENT '用户昵称/姓名',
    `gender`                     VARCHAR(10) COMMENT '性别: male/female/unknown',
    `age_range`                  VARCHAR(20) COMMENT '年龄段: 18-24, 25-34等',
    `language`                   VARCHAR(20) COMMENT '用户语言设置',
    `country`                    VARCHAR(50) COMMENT '国家/地区',
    `province`                   VARCHAR(50) COMMENT '省份/州',
    `city`                       VARCHAR(50) COMMENT '城市',
    -- 生命周期时间戳 (用于 Cohort 留存分析)
    `register_time`              DATETIME(3) NOT NULL COMMENT '用户注册时间',
    `first_install_time`         DATETIME(3) COMMENT '首次安装 App 时间',
    `last_login_time`            DATETIME(3) COMMENT '最后一次活跃时间',
    `monitor_currency_config`    JSON NULL COMMENT '货币价格监控配置: 目标货币及阈值',
    `monitor_transaction_config` JSON NULL COMMENT '用户交易监控配置: 监控对象及金额阈值',
    `user_preferences`           JSON NULL COMMENT '用户通用偏好设置'
    PRIMARY KEY (`user_id`),
    -- 建立索引以支持 Flink 的维表 Lookup Join
    UNIQUE KEY `uk_appsflyer_id` (`appsflyer_id`),
    KEY                          `idx_register_time` (`register_time`),
    KEY                          `idx_first_source` (`first_media_source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户多维属性表(ID映射与画像)';


CREATE TABLE `dim_realtime_assets_price`
(
    -- 唯一标识
    `id`           BIGINT         NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    -- 资产识别
    `asset_symbol` VARCHAR(20)    NOT NULL COMMENT '资产代码 (如 BTC, XAU, AAPL, EUR)',
    `asset_name`   VARCHAR(50) COMMENT '资产全称 (如 Bitcoin, Gold, Apple Inc)',
    -- 交易类型 (要求涵盖加密货币/黄金/股票等)
    `trade_type`   VARCHAR(30)    NOT NULL COMMENT '资产类别 (Cryptocurrency/Metal/Stock/Forex)',
    -- 交易价格 (统一单位: 美元)
    -- 使用 DECIMAL(24, 8) 确保能承载高价值资产(如黄金)和高精度资产(如部分代币)
    `price_usd`    DECIMAL(24, 8) NOT NULL COMMENT '当前实时价格 (单位: USD)',
    -- 数据来源与状态
    `price_source` VARCHAR(50) COMMENT '数据来源 (如 Binance, Yahoo Finance, Kitco)',
    `status`       TINYINT DEFAULT '1' COMMENT '状态: 1-启用, 0-停用',
    -- 价格更新时间 (要求)
    `update_time`  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '价格最后更新时间',
    PRIMARY KEY (`id`),
    -- 唯一索引：确保每个资产代码在同一来源下唯一，方便 Flink CDC 关联
    UNIQUE KEY `uk_asset_source` (`asset_symbol`, `price_source`),
    -- 索引：方便按资产类别快速筛选监控
    KEY            `idx_trade_type` (`trade_type`),
    KEY            `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时资产价格表 (涵盖加密货币、黄金、股票及汇率)';
