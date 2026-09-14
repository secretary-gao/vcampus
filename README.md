# Vcampus 虚拟校园系统

东南大学《专业技能实训》课程项目，基于 C/S 架构，MVC 分层 + Socket 多线程设计模式，
客户端界面用 JavaFX。6 人团队，6 个业务模块，见下方"团队分工"。

## 目录结构

```
coJava/
├── Common/src/vcampus/common/   # 客户端服务器共用：vo（Message/User等）/ constant / util
├── Server/src/vcampus/server/   # 服务器端：srv（业务服务）/ dao（数据访问）
├── Server/db.properties.example # 数据库连接配置模板（复制为 db.properties 并填写密码）
├── Client/src/vcampus/client/   # 客户端：view（JavaFX界面）/ biz（业务服务）
├── lib/                         # 第三方依赖 jar（mysql-connector-j）；lib/javafx/ 本地放 JavaFX SDK，不提交
├── sql/                         # 建库建表 + 各模块子目录 + 演示数据种子脚本
├── standard/                    # 课程组下发的说明书/编码规范/课程安排，只读参考
├── DEMO.md / DEMO.shop.md       # 给老师验收用的演示流程脚本
├── build.bat / run-server.bat / run-client.bat / start-all.bat  # 一键编译/启动脚本
└── .vscode/                     # VS Code Java 插件的源码路径、依赖、调试配置
```

## 团队分工

| 模块 | 负责人 | 分支 | 说明书对应表 |
|---|---|---|---|
| 用户管理（组长） | 高鸣谦 | dev/user | tblUser |
| 图书馆 | 赵夏巍 | dev/library | tblBook、tblBorrow |
| 学籍 | 洪荣誉 | dev/student | tblStudent |
| 商店 | 孙志平 | dev/shop | tblGoods、tblPurchase |
| 选课 | 刘酝潇 | dev/course | tblCourse、tblSelectCourse |
| 医院 | 李秋实 | dev/hospital | tblDoctor、tblAppointment |

每人只在自己的 `dev/xxx` 分支上开发，改 `Common/` 下的公共文件前先在群里说一声，
避免几个人同时改 `Message`/`IConstant` 造成合并冲突。完成后发 Pull Request 到
`main`，组长 review 通过后合并。

## 环境要求

- JDK 21（Eclipse Temurin）
- MySQL 8.0（本地服务运行在默认 3306 端口）
- VS Code + Java 扩展包（Extension Pack for Java）
- 不使用 Maven：依赖 jar 直接放在 `lib/` 目录下，通过 `.vscode/settings.json` 的
  `java.project.referencedLibraries` 让 VS Code 识别
- **JavaFX 21.0.12**：官网下载 Windows x64 SDK 解压到本机任意路径（不要放进项目
  文件夹），把解压出来的 **`lib` 和 `bin` 两个文件夹**完整复制到项目的
  `lib/javafx/` 目录下（该目录已加入 `.gitignore`，每人本地自己放一份），最终
  应形如 `lib/javafx/lib/*.jar` + `lib/javafx/bin/*.dll`——**注意路径是嵌套的
  `lib/javafx/lib`，不是 `lib/javafx`**，两者搞混会导致编译/运行报错，是历史上
  出过好几次的坑。缺了 `bin/` 里的原生渲染库还会导致运行时报
  "no suitable pipeline found"。

## 首次准备

1. 建库建表：
   ```powershell
   mysql -u root -p < sql/vcampus_schema.sql
   ```
   如果之前已经建过库、只是新增了字段（如 `uStatus`），改跑对应的 `sql/migration_*.sql`。
2. 配置本地数据库密码：复制 `Server/db.properties.example` 为 `Server/db.properties`，
   把 `jdbc.password` 改成本地 MySQL 密码（该文件已在 `.gitignore` 中，不会被提交，
   url 里要带 `allowPublicKeyRetrieval=true`）。
3. （可选）想让界面演示时有数据可看，跑一遍 `sql/seed_demo_data.sql` 填一批演示账号/
   学籍/挂号/购买记录。

## 校园 AI（Codex + 千问）

校园 AI 的调用链为 Vcampus 服务端 -> Codex Agent -> DashScope Responses API -> 千问。
Codex Agent 的部署文件和完整说明位于 `Server/codex-agent/README.md`。

本地使用时，根据 `Server/ai.properties.example` 创建 `Server/ai.properties`，然后
运行 `start-codex-tunnel.bat username@服务器地址` 建立隧道，再启动 Vcampus。
千问 API Key 只保存在 Agent 服务器上，不会分发到客户端或提交到 Git。

## 编译与运行

最简单：双击根目录的 **`start-all.bat`**——自动编译，然后弹出两个新窗口分别跑
服务器和客户端，不用在终端敲命令。

手动方式（项目根目录执行）：

```powershell
# 编译（收集三个模块的全部 .java 后一起编译到 bin/）
javac -encoding UTF-8 -d bin -cp "lib\mysql-connector-j-9.7.0.jar" --module-path "lib\javafx\lib" --add-modules javafx.controls,javafx.fxml (Get-ChildItem -Recurse -Path Common\src,Server\src,Client\src -Filter *.java).FullName

# 启动服务器（单独一个终端窗口，一直占着跑）
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.Server

# 启动客户端（另开一个终端窗口）
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" --module-path "lib\javafx\lib" --add-modules javafx.controls,javafx.fxml vcampus.client.view.LoginFrame
```

VS Code 里也可以直接按 F5，`.vscode/launch.json` 已配好 "Run Server" 和
"Run LoginFrame" 两个启动项。

## 代码规范（照《Java编码规范.doc》，见 `standard/` 目录）

- 每个类文件开头要有头注释块：类名/版本/日期/版权
- 类和公开方法要写 Javadoc
- **实例变量（非 static 字段）加下划线前缀**，如 `private String _uId;`；
  getter/setter 不带下划线，如 `getUId()`——这是刻意的，不是笔误
- static 字段/常量不加下划线，全大写+下划线分隔，如 `SERVER_PORT`

## 通信协议约定

客户端与服务器通过 `Common/vo/Message` 对象经 Socket 收发（`ObjectOutputStream`/
`ObjectInputStream`，双方都要先创建并 flush 输出流再创建输入流，否则会互相等待
卡死）。`ServerThread` 按 `Message.getName()`（消息名常量见 `IConstant`）把请求
分发给各模块的处理器，新增模块只需要在 `ServerThread.registerHandlers()` 里加
一行注册，不用改已有代码。

## 当前进度（2026-09-07）

用户管理/图书馆/学籍/商店/医院 5 个模块已合并到 `main`；选课模块在
`dev/course` 分支上开发完成，待合并。登录支持学生/教师/管理员三种角色，
主界面按角色区分可见功能（管理员专属的"账号管理"等）。演示流程见
`DEMO.md`（用户模块）、`DEMO.shop.md`（商店模块）。
