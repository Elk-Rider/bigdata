# bigdata
一套企业级金融交易类的流式湖仓一体大数据平台，方案整体具备TB级的数据采集，分析功能，结合即席查询能力，具备日常固定报表的开发等

整体学习环境为VMWare克隆的三台linux服务器，操作系统我centos 7  

所用框架下载地址如下：

        https://archive.apache.org/dist/zookeeper/zookeeper-3.8.4/
        https://www.apache.org/dyn/closer.cgi/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz
        https://paimon.apache.org/downloads
        https://www.apache.org/dyn/closer.lua/flink/flink-1.19.3/flink-1.19.3-bin-scala_2.12.tgz
        https://archive.apache.org/dist/hive/hive-3.1.3/apache-hive-3.1.3-bin.tar.gz
        https://kafka.apache.org/community/downloads/
        https://trino.io/download?utm_source=chatgpt.com
        https://download.selectdb.com/apache-doris-2.1.5-bin-x64.tar.gz
        https://vault.centos.org/7.9.2009/isos/x86_64/
        https://download.oracle.com/java/25/latest/jdk-25_linux-x64_bin.tar.gz
        https://repository.apache.org/snapshots/org/apache/paimon/   搭配paimon的 trino476插件 paimon-trino-476-1.3-20260327.003352-10-plugin.tar.gz
        https://dolphinscheduler.apache.org/zh-cn/download/3.4.1

环境变量如下：

    sudo vim /etc/profile.d/my_env.sh

        export TRINO_HOME=/opt/module/trino
        export PATH=$PATH:$TRINO_HOME/bin
        export PATH=$PATH:/opt/bin
        ##hdfs和yarn的用户变量配置
        export HDFS_NAMENODE_USER=root
        export HDFS_DATANODE_USER=root
        export HDFS_SECONDARYNAMENODE_USER=root
        export YARN_RESOURCEMANAGER_USER=root
        export YARN_NODEMANAGER_USER=root
        ##
        export JAVA_HOME=/opt/module/jdk
        export PATH=$PATH:$JAVA_HOME/bin
        export ZK_HOME=/opt/module/zookeeper
        export PATH=$PATH:$ZK_HOME/bin
        export KAFKA_HOME=/opt/module/kafka
        export PATH=$PATH:$KAFKA/bin
        export HADOOP_HOME=/opt/module/hadoop
        export PATH=$PATH:$HADOOP_HOME/bin
        export PATH=$PATH:$HADOOP_HOME/sbin
        export SPARK_HOME=/opt/module/spark
        export PATH=$PATH:$SPARK_HOME/bin
        export PATH=$PATH:$SPARK_HOME/sbin
        export FLINK_HOME=/opt/module/flink
        export PATH=$PATH:$FLINK_HOME/bin
        export HIVE_HOME=/opt/module/hive
        export PATH=$PATH:$HIVE_HOME/bin
        export HBASE_HOME=/opt/module/hbase
        export PATH=$PATH:$HBASE_HOME/bin
        export PHOENIX_HOME=/opt/module/phoenix
        export PHOENIX_CLASSPATH=$PHOENIX_CLASSPATH
        export PATH=$PATH:$PHOENIX_HOME/bin
        export HADOOP_CLASSPATH='hadoop classpath'
        export HADOOP_CONF_DIR=/opt/module/hadoop/etc/hadoop

    刷新环境配置
    source /etc/profile.d/my_env.sh

集群服务部署分布：

    bjc55 (核心主控): Zookeeper, HDFS NameNode (Active), YARN ResourceManager, Hive Metastore, MySQL (元数据库), Kafka Broker, Flink JobManager, Trino Coordinator, Doris FE。
    
    bjc56 (容灾与计算): Zookeeper, HDFS NameNode (Standby), YARN ResourceManager (Standby), DataNode, Kafka Broker, Flink TaskManager, Trino Worker, Doris BE。
    
    bjc57 (纯计算存储): Zookeeper, DataNode, Kafka Broker, Flink TaskManager, Trino Worker, Doris BE。



# 落地方案框架推荐 Gemini3 pro
![img_1.png](picture/集群版本.png)!


# 方案设计的AI建议：
风险组件 / 防线,致命阻塞症状 (不可抗力)

    1. Flink 依赖冲突(Hadoop Shade 机制),Flink 任务疯狂重启，抛出 NoSuchMethodError (通常是 Guava/Protobuf 打架)；或导致下游引擎 JVM 崩溃。,Flink 的 lib/ 目录下，绝对只保留官方的 flink-shaded-hadoop-3-uber.jar。严禁混入任何原生的 hadoop-common.jar 或第三方未经 Shade（遮蔽）的包。
    2. Doris 内存溢出(JNI 外表读取),Doris 挂载 Paimon 外表查询时，BE (Backend) 节点因 Java 堆外内存泄漏 (OOM) 直接宕机脱机。,部署 Doris 时，必须修改 BE 配置文件 be.conf。通过调整 JvmService 的 JAVA_OPTS 参数，强制为 JVM 预留 4GB - 8GB 的内存，专用于 Paimon 底层 Parquet/ORC 文件的解析。
    3. Trino 插件报错(类加载器污染),Trino 集群启动失败，或执行 SQL 时报错“找不到 Catalog”，发生严重的类加载器冲突。,Trino 450+ 已经原生内置了 Paimon 连接器。严禁画蛇添足地把 paimon-trino-*.jar 手动丢进 Trino 的 plugin 目录！只需在 etc/catalog/paimon.properties 中写明 connector.name=paimon 即可。
    
    
