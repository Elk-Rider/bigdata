#!/bin/bash

# ==============================================================================
# 企业级湖仓一体集群 (Lakehouse) 一键启停脚本
# 节点规划: bjc55(主), bjc56(从), bjc57(从)
# ==============================================================================

# 请根据你实际的安装路径修改以下变量
ZK_HOME="/opt/module/zookeeper"
HADOOP_HOME="/opt/module/hadoop"
KAFKA_HOME="/opt/module/kafka"
HIVE_HOME="/opt/module/hive"
TRINO_HOME="/opt/module/trino"
DORIS_HOME="/opt/module/doris"

if [ $# -lt 1 ]; then
    echo "用法: cluster.sh {start|stop}"
    exit 1
fi

case $1 in
"start")
    echo "==================== 🚀 开始启动湖仓一体集群 ===================="

    # 1. 启动 Zookeeper (集群的心脏，必须最先起)
    echo "-------- [1/6] 启动 Zookeeper --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host "source /etc/profile; $ZK_HOME/bin/zkServer.sh start"
    done
    sleep 3 # 给 ZK 选举留出时间

    # 2. 启动 Hadoop (HDFS + YARN)
    echo "-------- [2/6] 启动 Hadoop (HDFS & YARN) --------"
    ssh bjc55 "source /etc/profile; $HADOOP_HOME/sbin/start-dfs.sh"
    ssh bjc55 "source /etc/profile; $HADOOP_HOME/sbin/start-yarn.sh"
    sleep 5 # 等待 NameNode 退出安全模式

    # 3. 启动 Hive Metastore (全局元数据中心)
    echo "-------- [3/6] 启动 Hive Metastore --------"
    ssh bjc55 "source /etc/profile; nohup $HIVE_HOME/bin/hive --service metastore > /dev/null 2>&1 &"
    sleep 5 # 给 HMS 连 MySQL 留出时间

    # 4. 启动 Kafka (消息总线)
    echo "-------- [4/6] 启动 Kafka --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host "source /etc/profile; $KAFKA_HOME/bin/kafka-server-start.sh -daemon $KAFKA_HOME/config/server.properties"
    done

    # 5. 启动 Doris (FE 和 BE)
    echo "-------- [5/6] 启动 Doris --------"
    ssh bjc55 "source /etc/profile; $DORIS_HOME/fe/bin/start_fe.sh --daemon"
    for host in bjc56 bjc57; do
        ssh $host "source /etc/profile; $DORIS_HOME/be/bin/start_be.sh --daemon"
    done

    # 6. 启动 Trino
    echo "-------- [6/6] 启动 Trino --------"
    for host in bjc55 bjc56 bjc57; do
        # 注意：Trino 内部配置了独立的 JDK 22，直接调 launcher 即可
        ssh $host "$TRINO_HOME/bin/launcher start"
    done

    echo "==================== ✅ 集群启动指令下发完毕 ===================="
    ;;

"stop")
    echo "==================== 🛑 开始停止湖仓一体集群 ===================="

    # 1. 停止 Trino
    echo "-------- [1/6] 停止 Trino --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host "$TRINO_HOME/bin/launcher stop"
    done

    # 2. 停止 Doris
    echo "-------- [2/6] 停止 Doris --------"
    for host in bjc56 bjc57; do
        ssh $host "source /etc/profile; $DORIS_HOME/be/bin/stop_be.sh"
    done
    ssh bjc55 "source /etc/profile; $DORIS_HOME/fe/bin/stop_fe.sh"

    # 3. 停止 Kafka
    echo "-------- [3/6] 停止 Kafka --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host "source /etc/profile; $KAFKA_HOME/bin/kafka-server-stop.sh"
    done
    sleep 3 # 给 Kafka 刷盘留点时间

    # 4. 停止 Hive Metastore
    echo "-------- [4/6] 停止 Hive Metastore --------"
    # HMS 是后台进程，我们需要找到 PID 并优雅 kill 掉
    ssh bjc55 "ps -ef | grep 'hive.metastore' | grep -v grep | awk '{print \$2}' | xargs -n1 kill"

    # 5. 停止 Hadoop
    echo "-------- [5/6] 停止 Hadoop (YARN & HDFS) --------"
    ssh bjc55 "source /etc/profile; $HADOOP_HOME/sbin/stop-yarn.sh"
    ssh bjc55 "source /etc/profile; $HADOOP_HOME/sbin/stop-dfs.sh"

    # 6. 停止 Zookeeper (底座最后撤)
    echo "-------- [6/6] 停止 Zookeeper --------"
    for host in bjc55 bjc56 bjc57; do
        ssh $host "source /etc/profile; $ZK_HOME/bin/zkServer.sh stop"
    done

    echo "==================== 💤 集群已安全停止 ===================="
    ;;

*)
    echo "参数错误！请输入 start 或 stop"
    ;;
esac