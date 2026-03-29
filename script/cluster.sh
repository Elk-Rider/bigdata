#!/bin/bash

# ==============================================================================
# 企业级湖仓一体集群 (Lakehouse) 模块化统御脚本 3.0
# 节点规划:
#   bjc55: NameNode, ZK, Kafka, Hive(MS+HS2), Doris(FE+BE), Trino
#   bjc56: ResourceManager, ZK, Kafka, Doris(BE), Trino
#   bjc57: ZK, Kafka, Doris(BE), Trino
# ==============================================================================

# --- 环境变量配置 ---
ZK_HOME="/opt/module/zookeeper"
HADOOP_HOME="/opt/module/hadoop"
KAFKA_HOME="/opt/module/kafka"
HIVE_HOME="/opt/module/hive"
TRINO_HOME="/opt/module/trino"
DORIS_HOME="/opt/module/doris"

# --- 核心功能函数定义 ---

# 1. Zookeeper
start_zk() {
    echo "-------- 启动 Zookeeper --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host " $ZK_HOME/bin/zkServer.sh start"
    done
}
stop_zk() {
    echo "-------- 停止 Zookeeper --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host " $ZK_HOME/bin/zkServer.sh stop"
    done
}

# 2. Hadoop (HDFS & YARN)
start_hadoop() {
    echo "-------- 启动 Hadoop (HDFS & YARN) --------"
    ssh bjc55 " $HADOOP_HOME/sbin/start-dfs.sh"
    # RM 节点在 bjc56
    ssh bjc56 " $HADOOP_HOME/sbin/start-yarn.sh"
}
stop_hadoop() {
    echo "-------- 停止 Hadoop (YARN & HDFS) --------"
    ssh bjc56 " $HADOOP_HOME/sbin/stop-yarn.sh"
    ssh bjc55 " $HADOOP_HOME/sbin/stop-dfs.sh"
}

# 3. Hive (MS + HS2)
start_hive() {
    echo "-------- 启动 Hive (Metastore & HS2) --------"
    ssh bjc55 " nohup $HIVE_HOME/bin/hive --service metastore > /dev/null 2>&1 &"
    sleep 10
    ssh bjc55 " nohup $HIVE_HOME/bin/hive --service hiveserver2 > /dev/null 2>&1 &"
}
stop_hive() {
    echo "-------- 停止 Hive (HS2 & Metastore) --------"
    ssh bjc55 "ps -ef | grep 'HiveServer2' | grep -v grep | awk '{print \$2}' | xargs -n1 kill -9"
    ssh bjc55 "ps -ef | grep 'hive.metastore' | grep -v grep | awk '{print \$2}' | xargs -n1 kill -9"
}

# 4. Kafka
start_kafka() {
    echo "-------- 启动 Kafka --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host " $KAFKA_HOME/bin/kafka-server-start.sh -daemon $KAFKA_HOME/config/server.properties"
    done
}
stop_kafka() {
    echo "-------- 停止 Kafka --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host " $KAFKA_HOME/bin/kafka-server-stop.sh"
    done
}

# 5. Doris
start_doris() {
    echo "-------- 启动 Doris (FE & BEs) --------"
    ssh bjc55 " $DORIS_HOME/fe/bin/start_fe.sh --daemon"
    for host in bjc55 bjc56 bjc57; do
        ssh $host " $DORIS_HOME/be/bin/start_be.sh --daemon"
    done
}
stop_doris() {
    echo "-------- 停止 Doris (BEs & FE) --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host " $DORIS_HOME/be/bin/stop_be.sh"
    done
    ssh bjc55 " $DORIS_HOME/fe/bin/stop_fe.sh"
}

# 6. Trino
start_trino() {
    echo "-------- 启动 Trino --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host "$TRINO_HOME/bin/launcher start"
    done
}
stop_trino() {
    echo "-------- 停止 Trino --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host "$TRINO_HOME/bin/launcher stop"
    done
}

# --- 业务逻辑处理 ---

ACTION=$1
TARGET=$2

# 如果未指定目标，默认为 all
if [ -z "$TARGET" ]; then
    TARGET="all"
fi

case $ACTION in
"start")
    case $TARGET in
        "all") start_zk; sleep 2; start_hadoop; sleep 3; start_hive; start_kafka; start_doris; start_trino ;;
        "zk") start_zk ;;
        "hadoop") start_hadoop ;;
        "hive") start_hive ;;
        "kafka") start_kafka ;;
        "doris") start_doris ;;
        "trino") start_trino ;;
        *) echo "未知服务: $TARGET" ;;
    esac
    ;;
"stop")
    case $TARGET in
        "all") stop_trino; stop_doris; stop_kafka; stop_hive; stop_hadoop; sleep 2; stop_zk ;;
        "trino") stop_trino ;;
        "doris") stop_doris ;;
        "kafka") stop_kafka ;;
        "hive") stop_hive ;;
        "hadoop") stop_hadoop ;;
        "zk") stop_zk ;;
        *) echo "未知服务: $TARGET" ;;
    esac
    ;;
*)
    echo "用法: cluster.sh {start|stop} {all|zk|hadoop|hive|kafka|doris|trino}"
    ;;
esac