Apache Hive 3.1.3 数据仓库部署手册
部署目标：在 bjc55 上部署 Hive 服务，连接 MySQL 元数据库

服务器节点：bjc55（主节点安装即可）

软件版本：apache-hive-3.1.3-bin.tar.gz

依赖环境：Hadoop 集群已启动，MySQL 5.7/8.0 已安装

一、 环境预检查
1. Hadoop 集群状态
   Hive 所有的表数据都存放在 HDFS 上，必须确保 Hadoop 已在线。

Bash
# 检查 HDFS 进程 (NameNode, DataNode)
jps
# 确保可以创建文件夹
    hadoop fs -mkdir -p /tmp
    hadoop fs -mkdir -p /user/hive/warehouse
    hadoop fs -chmod g+w /tmp
    hadoop fs -chmod g+w /user/hive/warehouse
2. MySQL 元数据库准备
   在 MySQL 中为 Hive 创建用户和数据库：

SQL

        -- 登录 MySQL 执行
        CREATE DATABASE metastore;
        -- 1. 创建用户（兼容Hive）
        CREATE USER 'hive'@'%' IDENTIFIED WITH mysql_native_password BY 'hive';
        -- 2. 授权
        GRANT ALL PRIVILEGES ON *.* TO 'hive'@'%';
        -- 3. 刷新权限
        FLUSH PRIVILEGES;

3. JDBC 驱动准备
   Hive 连接 MySQL 需要驱动包。请准备好 mysql-connector-java-8.0.x.jar（或对应版本），稍后放入 Hive 的 lib 目录。

二、 具体安装步骤
1. 解压与环境配置
   在 bjc55 上执行：

        tar -zxvf /opt/software/apache-hive-3.1.3-bin.tar.gz -C /opt/module/
        mv /opt/module/apache-hive-3.1.3-bin /opt/module/hive


2. 核心配置文件 hive-site.xml

         vim  /opt/module/hive/conf/hive-site.xml

       <?xml version="1.0" encoding="UTF-8" standalone="no"?>
       <?xml-stylesheet type="text/xsl" href="configuration.xsl"?>
       <configuration>
       <property>
       <name>javax.jdo.option.ConnectionURL</name>
       <value>jdbc:mysql://bjc55:3306/metastore?useSSL=false&amp;allowPublicKeyRetrieval=true</value>
       </property>
       <property>
       <name>javax.jdo.option.ConnectionDriverName</name>
       <value>com.mysql.cj.jdbc.Driver</value>
       </property>
       <property>
       <name>javax.jdo.option.ConnectionUserName</name>
       <value>hive</value>
       </property>
       <property>
       <name>javax.jdo.option.ConnectionPassword</name>
       <value>hive_password</value>
       </property>
       
       <property>
       <name>hive.metastore.warehouse.dir</name>
       <value>/user/hive/warehouse</value>
       </property>
       
       <property>
       <name>hive.cli.print.header</name>
       <value>true</value>
       </property>
       </configuration>

   3. 解决 Guava 版本冲突（重要！）
   
   #Hive 3.1.3 自带的 Guava 包和 Hadoop 3.x 不兼容，必须统一版本，否则启动报错。
    # 删除 Hive 的旧版 Guava
        rm /opt/module/hive/lib/guava-19.0.jar
    # 拷贝 Hadoop 的新版 Guava
        cp /opt/module/hadoop/share/hadoop/common/lib/guava-27.0-jre.jar /opt/module/hive/lib/

4. 放置 MySQL 驱动

       将准备好的 mysql-connector-java-xxx.jar 放入 /opt/module/hive/lib/ 目录下。
       本人使用的是 ：mysql-connector-java-8.0.23.jar   mysql版本为 8.0.42

5. 初始化元数据库


    schematool -initSchema -dbType mysql -verbose
    成功标志：看到 Initialization script completed 且 schemaTool completed。

三   服务检查与验证
1. 启动 Hive 交互式命令行 (CLI)

       hive

SQL

    show databases;
    create table test_student(id int, name string);
    insert into test_student values(1, "bjc");
    select * from test_student;


2. 启动 HiveServer2 与 Beeline（生产常用）
   在后台启动 Metastore 和 HiveServer2，以便其他工具（如 DataGrip 或 Flink）连接：


# 后台启动元数据服务
nohup hive --service metastore &
# 后台启动 HiveServer2
nohup hive --service hiveserver2 &
验证连接：


beeline -u jdbc:hive2://bjc55:10000 -n root
# 进去后执行 !sql show tables;
老师的课后总结：
Guava 冲突：这是 90% 的同学第一次装 Hive 3 会遇到的“坑”，记住“以 Hadoop 的版本为准”。

元数据重要性：如果以后 Hive 报错，先查 MySQL 的 metastore 库能不能连上，再查 log/hive.log。

计算引擎：Hive 默认用 MapReduce，速度比较感人。以后咱们可以把它换成 Spark 或者 Tez，那速度就像给拖拉机装了火箭发动机！

同学，Hive 的“地基”打好了。下一课你是想给这一堆服务写一个“一键启停脚本”，还是直接上高进阶的 Flink + Paimon 湖仓架构？