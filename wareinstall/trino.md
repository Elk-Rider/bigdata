# 📖 企业级 Trino 440 降级与高可用部署指南
🗺️ 架构拓扑复盘
bjc55 (主控节点): Coordinator (仅负责 SQL 解析、生成执行计划与任务调度，不参与底层文件扫描)。

bjc56 (计算节点): Worker (负责从 HDFS 拉取 Paimon 数据并执行高并发计算)。

bjc57 (计算节点): Worker (同上)。

⏳ 核心部署步骤与排雷指南

第一步：优雅卸载旧版本 (清理战场)
既然要降级，为了防止底层脏数据和旧配置干扰，必须把 其他 版本清理干净。

第二步：确认专属 JDK 22 (引擎检查)

⚠️ 避坑： Trino 440 的底层依然要求极高的 Java 版本。请确保之前解压在三台机器 /opt/module/jdk-22 目录下的 JDK 完好无损，且千万不要把它加入系统全局的 /etc/profile 中，以免污染 Hadoop 和 Hive。

第三步：下载与解压 Trino 440

# 下载 Trino 440 稳定版
wget https://repo1.maven.org/maven2/io/trino/trino-server/440/trino-server-440.tar.gz

# 解压并重命名
    tar -zxvf trino-server-440.tar.gz -C /opt/module/
    mv /opt/module/trino-server-440 /opt/module/trino
第四步：恢复核心配置 (恢复元神)
将我们第一步备份的配置目录直接移动到新版本中：

    #数据目录（日志）
    mkdir /opt/module/trino/data/
    #核心配置文件存放目录
    mkdir /opt/module/trino/etc/
1.  etc/node.properties (节点基础属性)

        node.environment=web3_lakehouse
        node.id=node-bjc55  # 注意：分发到另外两台时，这里必须改！
        node.data-dir=/opt/module/trino/data


2. JVM 内存调优配置：jvm.config  **考虑到虚拟机的内存限制，给个基础配置（生产环境通常给几十 GB）**：

         -server
         -Xmx4G
         -XX:+UseG1GC
         -XX:G1HeapRegionSize=32M
         -XX:+ExplicitGCInvokesConcurrent
         -XX:+ExitOnOutOfMemoryError
         -XX:+HeapDumpOnOutOfMemoryError
         -XX:-OmitStackTraceInFastThrow

3. etc/config.properties (Coordinator 角色分配)


    coordinator=true
    node-scheduler.include-coordinator=false
    http-server.http.port=8080
    query.max-memory=8GB
    query.max-memory-per-node=2GB
    query.max-total-memory-per-node=3GB
    discovery.uri=http://bjc55:8080

4. etc/catalog/paimon.properties (连接数据湖的纽带)

        connector.name=paimon
        paimon.meta-store=hive
        paimon.hive.metastore.uri=thrift://bjc55:9083
第五步：重新打通 JDK 环境变量隔离
新解压的目录没有启动脚本的环境变量注入文件，必须重新创建。
在 bjc55 的 /opt/module/trino/bin/ 目录下新建 env.sh：


        #!/bin/bash
        export JAVA_HOME=/opt/module/jdk25
        export PATH=$JAVA_HOME/bin:$PATH

赋予执行权限：

    chmod +x /opt/module/trino/bin/env.sh

第六步：全集群分发与 Worker 节点修正 (关键之战)
一键分发到计算节点：

Bash
xsync.sh /opt/module/trino
登录 bjc56 和 bjc57 进行精准修改：

修改 etc/node.properties：

    bjc56 改为 node.id=node-bjc56
    bjc57 改为 node.id=node-bjc57

修改 etc/config.properties (彻底变为干活的 Worker)：


    coordinator=false
    http-server.http.port=8080
    query.max-memory=8GB
    query.max-memory-per-node=2GB
    query.max-total-memory-per-node=3GB
    discovery.uri=http://bjc55:8080
第七步：点火验证
确认底层的 Hive Metastore 服务是存活状态后，利用我们写好的统御脚本一键拉起：

Bash
cluster.sh start trino
查看日志确认是否有报错：tail -f /opt/module/trino/data/var/log/server.log。如果看到 SERVER STARTED，部署完美成功。
