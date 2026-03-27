#!/bin/bash
if [ $# -lt 1 ]; then
    echo "Usage: xcall.sh <command>"
    exit 1
fi
# 遍历你的三台机器
for host in bjc55 bjc56 bjc57; do
    echo "========== $host =========="
    ssh $host "$@"
done