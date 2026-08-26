# Vcampus 虚拟校园系统

东南大学《专业技能实训》课程项目，基于 C/S 架构，MVC 分层 + Socket 多线程设计模式。

## 目录结构

```
coJava/
├── Common/src/vcampus/common/   # 客户端服务器共用：vo（Message等）/ constant / util
├── Server/src/vcampus/server/   # 服务器端：view / srv（业务服务） / vo / dao
├── Server/db.properties.example # 数据库连接配置模板（复制为 db.properties 并填写密码）
├── Client/src/vcampus/client/   # 客户端：view / biz（业务服务） / vo / dao
├── lib/                         # 第三方依赖 jar（mysql-connector-j）
├── sql/vcampus_schema.sql       # 建库建表脚本
└── .vscode/settings.json        # VS Code Java 插件的源码路径与依赖配置
```

## 环境要求

- JDK 21（Eclipse Temurin）
- MySQL 8.0（本地服务需运行在默认 3306 端口）
- VS Code + Java 扩展包（Extension Pack for Java）
- 不使用 Maven：依赖 jar 直接放在 `lib/` 目录下，通过 `.vscode/settings.json`
  的 `java.project.referencedLibraries` 让 VS Code 识别。

## 首次准备

1. 建库建表：
   ```powershell
   mysql -u root -p < sql/vcampus_schema.sql
   ```
2. 配置本地数据库密码：复制 `Server/db.properties.example` 为 `Server/db.properties`，
   并把 `jdbc.password` 改成你本地 MySQL 的密码（该文件已在 `.gitignore` 中，不会被提交）。

## 编译与运行（手动 javac / java，命令均从项目根目录执行）

验证 DbHelper 数据库连通性：

```powershell
javac -d bin -cp "lib\mysql-connector-j-9.7.0.jar" Server\src\vcampus\server\dao\DbHelper.java Server\src\vcampus\server\dao\DbHelperTest.java
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.dao.DbHelperTest
```

编译 Common 模块（如 Message 类）：

```powershell
javac -d bin Common\src\vcampus\common\vo\Message.java Common\src\vcampus\common\vo\MessageType.java
```

后续接入 Server/Client 主程序后，可用类似方式分别在两个终端启动服务器和客户端进行联调。

## 当前进度

- [x] 三模块骨架（Common / Server / Client，按 view/srv-biz/vo/dao 分包）
- [x] 公共消息类 `Message`（Common）
- [x] 数据库连通：`DbHelper` + `DbHelperTest`（Server）+ 建库建表脚本
- [ ] User 实体类、UserDAO、IUserClientSrv/IUserServerSrv 接口
- [ ] 登录/注册 Swing 界面
- [ ] Socket 多线程通信链路
