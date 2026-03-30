# 🚀 Trino 450+ 生产级高可用部署文档

第一步：准备专属“引擎”（JDK 22 隔离部署） Trino 480 极其挑剔，拒绝与 Hadoop 共用 JDK 8。

**不共用jdk原因如下：**

        1. 现代 Java 特性的深度利用 (性能压榨)
           Trino 的定位是“极致的计算速度”，它在 400 之后的版本中大量使用了 Java 新版本的高级特性：
        
        Project Panama (外函数与内存 API): Trino 400+ 开始利用这个 API 直接操作堆外内存。这比老版本 Java 使用的 Unsafe 更安全且高效，能大幅提升计算引擎的内存管理性能。
        
        SIMD 支持 (Vector API): 新版 JDK 提供了更好的向量化计算支持，这与你之前了解的 ClickHouse 架构原理类似，能让 Trino 在处理聚合运算时直接调用 CPU 的底层指令集。
        
        这些特性在 JDK 8/11 中根本不存在。
        
        2. 垃圾回收器 (ZGC & G1) 的优化
           Hadoop: 逻辑相对简单，主要是文件读写和任务调度，对 GC 停顿不那么敏感。
        
        Trino: 作为一个内存计算引擎，动辄占用几百 GB 内存。旧版 JDK 8 的 CMS 或 G1 在处理超大内存时容易产生长时间的 Full GC (STW)，导致查询超时。
        
        原因: Trino 强制要求新版 JDK 是为了强制用户使用 ZGC 或经过深度优化的 G1，实现亚毫秒级的 GC 停顿。
        
        3. 摆脱“历史包袱”
           Hadoop 为了兼容性，其代码库极其庞大且陈旧，更新 JDK 版本的阻力非常大（很多依赖包在 JDK 17+ 下会报错）。
        
        Trino 团队（原 Presto 团队）的风格非常激进，他们认为 “为了兼容性而牺牲性能是不可接受的”。
        
        通过切断对旧版 JDK 的支持，Trino 可以移除大量的冗余代码和过时的第三方库，使引擎更加轻量、安全。

1. 在三台机器上（可以通过 xcall.sh）创建目录：

        mkdir -p /opt/module/jdk22

下载 Linux x64 版本的 JDK 22 压缩包，解压到三台机器的 /opt/module/jdk-22 目录下。

架构师铁律： 绝对不要把这个路径写进 /etc/profile！只要放在那里备用即可。

第二步：解压 Trino 并创建配置目录 在 bjc55 上下载 trino-server-480.tar.gz（或最新稳定版），解压到 /opt/module/ 下，并重命名为 trino。

        tar -zxvf trino-server-480.tar.gz -C /opt/module/
        mv /opt/module/trino-server-480 /opt/module/trino

在 trino 根目录下，手动创建一个极其重要的配置文件夹 etc，以及它里面的 catalog 文件夹。

        cd /opt/module/trino
        mkdir -p /opt/module/trino/etc/catalog
第三步：编写核心配置文件（bjc55 视角）
在 /opt/module/trino/etc 目录下，我们需要创建 4 个核心文件：
1. 节点环境配置：/opt/module/trino/etc/node.properties

        # 集群名称，三台机器必须完全一致
        node.environment=lakehouse
        # 节点唯一ID！三台机器千万不能一样，bjc55写55，bjc56写56
        node.id=node-bjc55
        # 数据和日志的本地存储目录
        node.data-dir=/opt/module/trino/data
2. JVM 内存调优配置：/opt/module/trino/etc/jvm.config
   考虑到虚拟机的内存限制，给个基础配置（生产环境通常给几十 GB）：

        -server
        -Xmx4G
        -XX:+UseG1GC
        -XX:G1HeapRegionSize=32M
        -XX:+ExplicitGCInvokesConcurrent
        -XX:+ExitOnOutOfMemoryError
        -XX:+HeapDumpOnOutOfMemoryError
        -XX:-OmitStackTraceInFastThrow
3. 节点角色配置：/opt/module/trino/etc/config.properties (这是区分主从的关键！)
   当前在 bjc55，它是主节点 (Coordinator)：

        coordinator=true
        # 生产环境建议主节点不参与计算，专注于调度
        node-scheduler.include-coordinator=false
        http-server.http.port=18080
        query.max-memory=8GB
        query.max-memory-per-node=3GB
        #query.max-total-memory-per-node=3GB  该配置已经被弃用，取消注释会报错
        # 堆外内存限制（如果你的 JVM 给了 8G，可以额外给一点点）
        memory.heap-headroom-per-node=1GB
        # 供 Worker 汇报心跳的地址，指向 bjc55
        discovery.uri=http://bjc55:18080
4. 日志级别配置：/opt/module/trino/etc/log.properties

        io.trino=INFO

# 第四步：打通 Paimon 的任督二脉 (Catalog 配置)这是湖仓一体最值钱的一步！
    vim /opt/module/trino/etc/catalog/paimon.properties
        
        # 声明使用内置的 Paimon 原生连接器（绝对不要用 hive！）
        connector.name=paimon
        # 指向我们统御全局的 Hive Metastore
        paimon.meta-store=hive
        paimon.hive.metastore.uri=thrift://bjc55:9083
(注：Trino 480+ 已经内置了 Paimon 插件，你不需要去瞎折腾扔什么 jar 包，配好这三行代码，Trino 就能直接看懂 Paimon 的 Deletion Vectors 和动态分桶了！)

第五步：劫持环境变量（注入 JDK 22）
为了让 Trino 乖乖使用刚才解压的 JDK 22，而不去碰系统的 JDK 8，官方推荐的做法是在 bin/ 目录下创建一个 env.sh。

# 在 /opt/module/trino/bin/env.sh 添加 ：

    #!/bin/bash
    # 强制指定 Trino 启动时使用的 Java 运行环境
        export TRINO_JAVA_HOME=/opt/module/jdk25
        export PATH=$TRINO_JAVA_HOME/bin:$PATH
        记得给它执行权限：chmod +x bin/env.sh

# 第六步：分发集群与修改 Worker 节点配置
使用你之前写的神器脚本，把整个 Trino 目录分发到另外两台机器：

        xsync.sh /opt/module/trino
        致命排雷（务必操作）： 登录到 bjc56 和 bjc57，修改它们各自的配置文件！
        
        修改 etc/node.properties：
        把 node.id=node-bjc55 改为独一无二的 node-bjc56 和 node-bjc57。
        
        修改 etc/config.properties（让它们变成纯干活的 Worker）：
        coordinator=false
        http-server.http.port=8080
        query.max-memory=8GB
        query.max-memory-per-node=2GB
        query.max-total-memory-per-node=3GB
        依然指向主控节点 bjc55
        discovery.uri=http://bjc55:8080
第七步：点火启动！
按照顺序，确保你的底座（HDFS、ZK、Hive Metastore）已经全部存活。

在三台机器上分别执行后台启动命令：

Bash
# 在 bjc55, bjc56, bjc57 上执行
      /opt/module/trino/bin/launcher start
      JAVA_HOME=/opt/module/jdk25 /opt/module/trino/bin/launcher run
如果启动报错，立刻去查看 /opt/module/trino/data/var/log/server.log，一切报错原因都在里面。

💡 架构师温馨提示：如何验证大功告成？
下载一个 trino-cli-480-executable.jar（Trino 官方提供的命令行客户端）。

将其重命名为 trino 并赋予执行权限：chmod +x trino。

连接你的主节点并执行查询：
# 如果你报错如下：
报错信息： recognizes class file versions up to 52.0（只认 JDK 8）。

*根本原因*： 你运行 ./trino 时，这个可执行 Jar 包内部会自动寻找 java 命令。因为它在系统的 PATH 中第一顺位找到了 JDK 8，所以它用了旧版 Java 去跑新版 CLI。

2. 方案 A：直接指定全路径（最推荐，暴力有效）
跳过所有环境变量，直接告诉系统用哪个 java 去跑这个文件：

         /opt/module/jdk25/bin/java -jar ./trino --server localhost:8888 --catalog paimon --schema default

方案 B：临时修正 PATH 环境 **如果你希望直接运行 ./trino，必须把 JDK 25 的 bin 目录插到 PATH 的最前面：**

      export PATH=/opt/module/jdk25/bin:$PATH
      ./trino --server localhost:18080 --catalog paimon --schema default

3. 日常使用的“终极懒人”办法
   为了以后不用每次都敲这么长一串路径，建议给 Trino CLI 做一个 Alias（别名）。

打开你的用户配置文件：

      Bash
      vim ~/.bashrc
      在文件末尾添加一行：
      
      Bash
      alias trino='/opt/module/jdk25/bin/java -jar /opt/bin/trino --server 192.168.88.155:18080'
      (注：请将 /opt/module/software/trino 替换为你文件的实际全路径)
      
      刷新配置：
      
      Bash
      source ~/.bashrc
      以后只需要输入一个词就能进客户端了：
      
      Bash
      trino
      Bash
      ./trino --server bjc55:18080 --catalog paimon
      trino> SHOW SCHEMAS;

只要你能看到底层的库名，甚至能 SELECT * 查出之前 Flink 写入的数据，就ojbk了