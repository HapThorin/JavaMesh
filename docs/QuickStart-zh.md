# 服务注册和发现快速开始说明

[简体中文](QuickStart-zh.md) | [English](QuickStart.md) 

本文档基于宿主应用更改注册中心的场景，我们将使用Sermant框架的注册插件，完整的展示了如何使用Sermant插件非侵入式地扩展宿主应用的功能。
另外，更新的插件功能请参见[插件列表](user-guide/feature-list-zh.md)后自行选择。

## 使用Sermant前
如下图所示: 文档中演示使用的应用示例在未使用Sermant时, 会将应用注册到Zookeeper中。
<img src="binary-docs/before_use_agent.png" width="50%" syt height="50%" />

## 使用Sermant后
如下图所示: 文档中演示使用的应用示例在使用Sermant时, Sermant会将原本注册到Zookeeper的应用信息注册到ServiceCenter中; Sermant采用zookeeper作为默认的配置中心，向插件提供了动态配置管理的能力。

<img src="binary-docs/after_use_agent.png" width="50%" syt height="50%" />

# 获取编译结果

## 下载release包
点击 [here](https://github.com/huaweicloud/Sermant/releases) 下载release包

## 源码编译
- 编译机器需具备[git](https://git-scm.com/downloads) , [jdk 8或11](https://www.oracle.com/java/technologies/downloads/) , [maven](https://maven.apache.org/download.cgi) 环境
- 执行`git clone -b develop https://github.com/huaweicloud/Sermant.git` 克隆最新源码
- 执行`cd Sermant`进入源码目录
- 执行`mvn clean package -Dmaven.test.skip -Pexample` 编译示例项目

# 启动
- 运行环境需要具备[ServiceCenter](https://github.com/apache/servicecomb-service-center/releases) ,[zookeeper](https://zookeeper.apache.org/releases.html) 分别作为注册中心和配置中心。
- 进入编译后可执行文件根目录(例如:**sermant-agent-1.0.0**)
- 执行以下命令启动backend，详细的后端模块介绍以及更多的配置修改，请参考[backend模块](user-guide/backend-zh.md)
  ```bash
  # windows
  java -jar server\sermant\sermant-backend-x.x.x.jar
  
  # mac, linux
  java -jar server/sermant/sermant-backend-x.x.x.jar
  ```
- 执行以下命令启动注册插件provider示例应用
  ```bash
  # windows
  java -javaagent:agent\sermant-agent.jar=appName=provider -jar ..\sermant-example\demo-register\resttemplate-provider\target\resttemplate-provider.jar
  # mac linux
  java -javaagent:agent/sermant-agent.jar=appName=provider -jar ../sermant-example/demo-register/resttemplate-provider/target/resttemplate-provider.jar
  ```
- 执行以下命令启动注册插件consumer示例应用
  ```bash
  # windows
  java -javaagent:agent\sermant-agent.jar=appName=consumer -jar ..\sermant-example\demo-register\resttemplate-consumer\target\resttemplate-consumer.jar
  # mac linux
  java -javaagent:agent/sermant-agent.jar=appName=consumer -jar ../sermant-example/demo-register/resttemplate-consumer/target/resttemplate-consumer.jar
  ```
- 浏览器访问：[http://localhost:8900](http://localhost:8900) 查看框架和启用插件的运行状态。
![pic](binary-docs/backend_sermant_info.png)
- 浏览器访问：[http://localhost:30103](http://localhost:30103) 查看应用注册状态。
![pic](binary-docs/register-application.PNG)
- 浏览器访问：[http://localhost:8005/hello](http://localhost:8005/hello) 验证provider和consumer注册和订阅成功。

  <img src="binary-docs/check_application.png" width="50%" syt height="50%" />

## 查看日志
Sermant和Backend日志的存放目录分别位于sermant运行环境下`./logs/sermant/core`和`./logs/sermant/backend`

## 相关文档

插件开发, 请参考 [插件开发相关文档](./README-zh.md)。

常见问题说明与解决指导,请参考[FAQ](./FAQ-zh.md)。