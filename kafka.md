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
bin/kafka-topics.sh --create --topic <name> --bootstrap-server bjc55:9092 --partitions 3 --replication-factor 2

查看 Topic 列表:
bin/kafka-topics.sh --list --bootstrap-server bjc55:9092

查看 Topic 详情:
bin/kafka-topics.sh --describe --topic <name> --bootstrap-server bjc55:9092

修改分区数 (只能增不能减):
bin/kafka-topics.sh --alter --topic <name> --partitions 5 --bootstrap-server bjc55:9092

删除 Topic:
bin/kafka-topics.sh --delete --topic <name> --bootstrap-server bjc55:9092

3. 消费者组管理 (Consumer Group)
使用 kafka-consumer-groups.sh 进行监控和重置位点。

查看所有消费者组:
bin/kafka-consumer-groups.sh --bootstrap-server bjc55:9092 --list

查看消费滞后 (Lag) 详情:
bin/kafka-consumer-groups.sh --bootstrap-server bjc55:9092 --describe --group <group_name>

重置位点 (到最早可用的偏移量):
bin/kafka-consumer-groups.sh --bootstrap-server bjc55:9092 --group <group_name> --reset-offsets --to-earliest --execute --topic <topic_name>

删除消费者组:
bin/kafka-consumer-groups.sh --bootstrap-server bjc55:9092 --delete --group <group_name>

4. 动态配置管理 (Configs)
用于不重启 Broker 的情况下调整参数。

查看 Broker 当前配置:
bin/kafka-configs.sh --bootstrap-server bjc55:9092 --entity-type brokers --entity-name 0 --describe

限制某个 Topic 的写入速率 (每秒 10MB):
bin/kafka-configs.sh --bootstrap-server bjc55:9092 --alter --entity-type topics --entity-name <topic> --add-config 'producer_byte_rate=10485760'

5. 生产与消费测试
开启生产者客户端模式:
bin/kafka-console-producer.sh --topic <name> --bootstrap-server bjc55:9092

控制台消费者 (从头开始消费):
bin/kafka-console-consumer.sh --topic real——trade --from-beginning --bootstrap-server bjc55:9092

6. 集群维护与重平衡
查看当前分区 Leader 副本状态:
bin/kafka-leader-election.sh --bootstrap-server bjc55:9092 --election-type preferred --all-topic-partitions

生成副本迁移计划 (JSON 格式):
bin/kafka-reassign-partitions.sh --bootstrap-server bjc55:9092 --generate --topics-to-move-json-file topics.json --broker-list "0,1,2"

7. 性能测试 (Performance Tools)
生产者压力测试:
bin/kafka-producer-perf-test.sh --topic <name> --num-records 100000 --record-size 1024 --throughput -1 --producer-props bootstrap.servers=bjc55:9092