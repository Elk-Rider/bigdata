# bigdata
一套企业级金融交易类的流式湖仓一体大数据平台，方案整体具备TB级的数据采集，分析功能，结合即席查询能力，具备日常固定报表的开发等

整体学习环境为VMWare克隆的三台linux服务器，操作系统我centos 7  
框架版本

zookeeper 3.8.4
kafka 3.8.1

step 1 
模拟生成数据，在mysql中模拟规则数据， kafka中模拟交易数据。