Apache ZooKeeper 3.8.4 集群部署手册
部署目标：构建 3 节点高可用 ZooKeeper 集群

服务器节点：bjc55, bjc56, bjc57

软件版本：apache-zookeeper-3.8.4-bin.tar.gz

一、 环境预检查
在安装服务之前，必须确保集群的底层“地基”是稳固的。

1. 防火墙状态
   ZooKeeper 节点间通信需要用到 2181 (客户端)、2888 (原子广播)、3888 (选举) 端口。建议直接关闭防火墙（学习环境常用）或开启对应端口。

Bash
# 在所有节点执行
sudo systemctl stop firewalld

sudo systemctl disable firewalld

2. JDK 环境确认
   ZooKeeper 3.8.4 需要 JDK 1.8+ 环境支持。
# 预期返回 java version "1.8.x" 或更高
java -version
3. 主机名映射与免密
   确保 /etc/hosts 中已配置 bjc55, bjc56, bjc57 的 IP 映射，且 bjc55 可以免密登录其它两台。

4. 推荐工具：集群执行脚本 (xcall)
   为了提高效率，建议在 /usr/local/bin 下创建一个脚本，方便同时查看三台机器的状态。

Bash
# 脚本示例：xcall jps
# 能够批量在 bjc55, bjc56, bjc57 执行命令
二、 具体安装步骤
1. 解压与目录规范
   在 bjc55 节点进行操作：

# 创建安装目录（如果不存在）
mkdir -p /opt/module

# 解压并重命名
tar -zxvf apache-zookeeper-3.8.4-bin.tar.gz -C /opt/module/
mv /opt/module/apache-zookeeper-3.8.4-bin /opt/module/zookeeper
2. 核心配置修改
   进入配置目录并创建 zoo.cfg：

Bash
cd /opt/module/zookeeper/conf
cp zoo_sample.cfg zoo.cfg
vim zoo.cfg
修改并添加以下内容：

Properties
# 1. 存储快照和事务日志的目录
dataDir=/opt/module/zookeeper/zkData

# 2. 客户端连接端口
clientPort=2181

# 3. 集群节点列表 (server.A=B:C:D)
server.55=bjc55:2888:3888
server.56=bjc56:2888:3888
server.57=bjc57:2888:3888
3. 创建标识文件 (myid)
   这一步是集群识别身份的关键：

Bash
mkdir -p /opt/module/zookeeper/zkData
echo "55" > /opt/module/zookeeper/zkData/myid
4. 集群同步分发
   将配置好的安装包同步到其他两个节点：

Bash
# 使用 scp 或你的 xsync 脚本
scp -r /opt/module/zookeeper bjc56:/opt/module/
scp -r /opt/module/zookeeper bjc57:/opt/module/
5. 修正副本节点的 myid
   这是最容易出错的地方，必须分别在对应机器修改：

在 bjc56 上：echo "56" > /opt/module/zookeeper/zkData/myid

在 bjc57 上：echo "57" > /opt/module/zookeeper/zkData/myid

三、 服务启动与检查
1. 逐台启动服务
   分别在三台机器上执行启动命令（或者使用你编写的群发脚本）：

Bash
/opt/module/zookeeper/bin/zkServer.sh start
2. 验证结果（三步走）
   第一步：查看进程
   执行 jps 命令，看到 QuorumPeerMain 进程即代表 JVM 进程已拉起。

Bash
[root@bjc55 ~]# jps
1234 QuorumPeerMain
第二步：检查集群状态（最核心）
执行 status 命令查看角色分配：

Bash
/opt/module/zookeeper/bin/zkServer.sh status
成功标准：三台机器中，应该有 1个 leader 和 2个 follower。

异常提示：如果显示 Error contacting service，请检查 myid 内容和防火墙。

第三步：客户端连接测试
尝试本地连接，确保 ZNode 树可以正常访问：

Bash
/opt/module/zookeeper/bin/zkCli.sh
# 进入后执行 ls /****