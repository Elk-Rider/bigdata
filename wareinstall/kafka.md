Apache Kafka 3.7.2 集群部署手册 (ZK模式)
部署目标：构建 3 节点 Kafka 集群

服务器节点：bjc55, bjc56, bjc57

软件版本：kafka_2.12-3.7.2.tgz

核心逻辑：依赖外部 ZooKeeper 集群进行元数据管理

一、 环境预检查
在“动土”之前，老师再带你过一遍清单，运维就是得细致：

1. ZooKeeper 状态检查
   Kafka 3.7.2 启动前必须确保 ZK 在线。

Bash
# 在三台机器上分别执行，确保看到 Leader/Follower
/opt/module/zookeeper-3.8.4/bin/zkServer.sh status
2. Java 环境检查
   Kafka 3.7.2 推荐使用 JDK 11 或 JDK 17，但 JDK 1.8 依然保持兼容。

Bash
java -version
3. 端口规划
   9092：Kafka 默认的服务端口，确保没有被占用。

检查命令：netstat -nltp | grep 9092

二、 具体安装步骤
1. 解压与分发准备
   在 bjc55 上执行：

Bash
# 解压
tar -zxvf kafka_2.12-3.7.2.tgz -C /opt/module/

# 重命名，去掉版本号后缀，方便以后升级和写脚本
cd /opt/module/
mv kafka_2.12-3.7.2 kafka
2. 核心配置文件修改
vim  /opt/module/kafka/config/server.properties

Properties
# 【1. 节点ID】三台机器必须唯一，建议跟机器编号挂钩
broker.id=55

# 【2. 数据存储目录】记得手动创建这个文件夹
log.dirs=/opt/module/kafka/data

# 【3. ZK连接地址】这里加上 /kafka 路径，方便在 ZK 内部做隔离
zookeeper.connect=bjc55:2181,bjc56:2181,bjc57:2181/kafka

# 【4. Topic 物理删除开关】学习环境建议开启
delete.topic.enable=true

# 【5. 这里的地址建议显式配置，防止多网卡环境下的识别问题】
# listeners=PLAINTEXT://bjc55:9092
3. 集群同步与副本修正
   将配置好的 kafka 文件夹同步到其他节点：

Bash
# 使用你的分发脚本或 SCP
scp -r /opt/module/kafka bjc56:/opt/module/
scp -r /opt/module/kafka bjc57:/opt/module/
⚠️ 关键操作：修改另外两台的 broker.id（千万别忘！）
vim  /opt/module/kafka/config/server.properties
在 bjc56 上：修改 server.properties 中的 broker.id=56

在 bjc57 上：修改 server.properties 中的 broker.id=57

三、 服务检查与验证
1. 启动服务
   在三台机器上分别执行（建议带上 -daemon 参数）：

Bash
/opt/module/kafka/bin/kafka-server-start.sh -daemon /opt/module/kafka/config/server.properties
2. 验证“三部曲”
   第一步：进程验证
   Bash
   jps
# 看到 Kafka 和 QuorumPeerMain 都在就稳了
第二步：Topic 生产测试
在 3.x 版本中，所有的管理命令都直接连 bootstrap-server，不再直接连 ZK 了：

Bash
# 创建一个名为 "bjc-test" 的主题
/opt/module/kafka/bin/kafka-topics.sh --bootstrap-server bjc55:9092 --create --topic bjc-test --partitions 3 --replication-factor 2

# 查看主题列表
/opt/module/kafka/bin/kafka-topics.sh --bootstrap-server bjc55:9092 --list
第三步：消息收发实战（仪式感拉满）
打开一个窗口（消费者） 在 bjc56 上：

Bash
/opt/module/kafka/bin/kafka-console-consumer.sh --bootstrap-server bjc56:9092 --topic bjc-test
打开另一个窗口（生产者） 在 bjc55 上：

Bash
/opt/module/kafka/bin/kafka-console-producer.sh --bootstrap-server bjc55:9092 --topic bjc-test
# 随便输入 "Hello Teacher" 或者 "Flink is cool!"
检查点：如果消费者窗口实时蹦出了文字，说明你的 Kafka 3.7.2 已经完全打通！



kafka常用命令：
Kafka集群运维常用命令（部分不了解命令需要查明后使用）
1. 节点与元数据管理
   Kafka 3.8.1
   生成集群 ID:
   bin/kafka-storage.sh random-uuid

格式化存储目录:
bin/kafka-storage.sh format -t <uuid> -c config/kraft/server.properties

查看元数据仲裁状态:
bin/kafka-metadata-quorum.sh --bootstrap-server bjc55:9092 describe --status

2. 主题管理 (Topic)
   这是最常用的管理脚本 kafka-topics.sh。

创建 Topic:
/opt/module/kafka/bin/kafka-topics.sh --create --topic <name> --bootstrap-server bjc55:9092 --partitions 3 --replication-factor 2

查看 Topic 列表:
/opt/module/kafka/bin/kafka-topics.sh --list --bootstrap-server bjc55:9092

查看 Topic 详情:
/opt/module/kafka/bin/kafka-topics.sh --describe --topic <name> --bootstrap-server bjc55:9092

修改分区数 (只能增不能减):
/opt/module/kafka/bin/kafka-topics.sh --alter --topic <name> --partitions 5 --bootstrap-server bjc55:9092

删除 Topic:
/opt/module/kafka/bin/kafka-topics.sh --delete --topic bjc-test --bootstrap-server bjc55:9092

3. 消费者组管理 (Consumer Group)
   使用 kafka-consumer-groups.sh 进行监控和重置位点。

查看所有消费者组:
/opt/module/kafka/bin/kafka-consumer-groups.sh --bootstrap-server bjc55:9092 --list

查看消费滞后 (Lag) 详情:
/opt/module/kafka/bin/kafka-consumer-groups.sh --bootstrap-server bjc55:9092 --describe --group <group_name>

重置位点 (到最早可用的偏移量):
/opt/module/kafka/bin/kafka-consumer-groups.sh --bootstrap-server bjc55:9092 --group <group_name> --reset-offsets --to-earliest --execute --topic <topic_name>

删除消费者组:
/opt/module/kafka/bin/kafka-consumer-groups.sh --bootstrap-server bjc55:9092 --delete --group <group_name>

4. 动态配置管理 (Configs)
   用于不重启 Broker 的情况下调整参数。

查看 Broker 当前配置:
/opt/module/kafka/bin/kafka-configs.sh --bootstrap-server bjc55:9092 --entity-type brokers --entity-name 0 --describe

限制某个 Topic 的写入速率 (每秒 10MB):
/opt/module/kafka/bin/kafka-configs.sh --bootstrap-server bjc55:9092 --alter --entity-type topics --entity-name <topic> --add-config 'producer_byte_rate=10485760'

5. 生产与消费测试
   开启生产者客户端模式:
   /opt/module/kafka/bin/kafka-console-producer.sh --topic <name> --bootstrap-server bjc55:9092

控制台消费者 (从头开始消费):
/opt/module/kafka/bin/kafka-console-consumer.sh --topic real——trade --from-beginning --bootstrap-server bjc55:9092

6. 集群维护与重平衡
   查看当前分区 Leader 副本状态:
   /opt/module/kafka/bin/kafka-leader-election.sh --bootstrap-server bjc55:9092 --election-type preferred --all-topic-partitions

生成副本迁移计划 (JSON 格式):
/opt/module/kafka/bin/kafka-reassign-partitions.sh --bootstrap-server bjc55:9092 --generate --topics-to-move-json-file topics.json --broker-list "0,1,2"

7. 性能测试 (Performance Tools)
   生产者压力测试:
   /opt/module/kafka/bin/kafka-producer-perf-test.sh --topic <name> --num-records 100000 --record-size 1024 --throughput -1 --producer-props bootstrap.servers=bjc55:9092