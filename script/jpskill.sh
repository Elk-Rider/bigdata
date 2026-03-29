#!/bin/bash

# 1. 定义集群节点列表 (根据你的实际主机名修改)
HOSTS="bjc55 bjc56 bjc57"

# 2. 获取当前运行脚本的用户名 (通常是 root)
USER=$(whoami)

echo "--- 开始清理集群 Java 进程 ---"

for host in $HOSTS
do
    echo "========== 正在清理节点: $host =========="

    # 核心逻辑：
    # ssh 到目标机器 -> 执行 jps -> 过滤掉 Jps 自身进程 -> 提取 PID -> 批量 kill
    ssh $host "jps | grep -v Jps | awk '{print \$1}' | xargs -n 1 kill -9 2>/dev/null"

    if [ $? -eq 0 ]; then
        echo "节点 $host 的所有 Java 进程已尝试杀掉。"
    else
        echo "节点 $host 清理失败，请检查 SSH 连接。"
    fi
done

echo "--- 集群清理完成 ---"