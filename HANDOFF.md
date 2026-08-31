# 交接说明（给接手继续开发的人/工具看）

这份文档是给切换开发工具（比如从 Claude Code 切到 GitHub Copilot）时用的，
把当前进度、约定和马上要做的事写清楚，避免重新踩一遍已经踩过的坑。

## 项目背景

东南大学《专业技能实训》课程项目 Vcampus（C/S 虚拟校园系统）。
- JDK 21（Temurin），MySQL 8.0，VS Code，**不用 Maven**（依赖 jar 手动放
  `lib/` 目录，通过 `.vscode/settings.json` 的 `referencedLibraries` 识别）
- 三模块：`Common/`（客户端服务器共用）、`Server/`、`Client/`，每个模块内部
  按 `view`（界面）/ `srv`或`biz`（业务服务）/ `vo`（实体类）/ `dao`（数据访问）分包
- 6人团队，6个业务模块（用户管理/图书馆/学籍/商店/选课/医院），完整排期见
  仓库根目录 `PLAN.md`（在 `main` 分支上）
- 当前分支：`dev/user`（用户管理模块，我负责）

## 代码规范（照抄现有文件的风格，不要引入新风格）

- 类文件开头要有头注释块：类名/版本/日期/版权（see 任意一个已有 `.java` 文件头部）
- 类和公开方法要写 Javadoc
- **实例变量（非 static 字段）要加下划线前缀**，比如 `private String _uId;`；
  getter/setter 方法名不带下划线，如 `getUId()`；这是照《Java编码规范.doc》来的，
  刻意的，不是笔误
- static 字段/常量不用加下划线，用全大写+下划线分隔（如 `SERVER_PORT`）

## 已经做完、能跑通的东西

- `Common/src/vcampus/common/vo/Message.java` + `MessageType.java`：通信消息类，
  字段 uid/name/type/statusCode/data/sender，实现 Serializable
- `Common/src/vcampus/common/vo/User.java`：用户实体类，对应 `tblUser` 表
- `Common/src/vcampus/common/util/MD5Util.java`：密码 MD5 加密
- `Common/src/vcampus/common/constant/IConstant.java`：常量（服务器地址
  127.0.0.1、端口8888、状态码200/401/404/409/500、消息名
  MSG_LOGIN/MSG_REGISTER/MSG_LOGOUT）
- `Server/src/vcampus/server/dao/DbHelper.java` + `UserDAO.java` +
  对应的 `*Test.java`：数据库连接、用户表增删改查，已验证跑通
- `Server/src/vcampus/server/srv/`：`IUserServerSrv`/`UserServerSrv`（登录密码
  校验、注册查重）、`UserExistsException`、`ServerThread`（一个客户端一个
  线程，处理一次请求/响应）、`Server`（主线程 accept 循环，监听 8888 端口）
- `Client/src/vcampus/client/biz/`：`IUserClientSrv`/`UserClientSrv`（每次
  请求新建 Socket 收发 Message）、`UserClientSrvTest`（无界面端到端验证）
- `Client/src/vcampus/client/view/LoginFrame.java`：登录/注册窗口，已改写为
  JavaFX；当前版是“东南大学风格”的简洁登录页，登录/注册/返回流程已打通
- `Client/src/vcampus/client/view/MainFrame.java`：登录后的主界面骨架，已改写为
  JavaFX；内含图书馆/学籍/医院/教务/宿舍/商店等功能分区占位卡片

已通过测试验证：注册(200)/正确密码登录(200，返回完整用户)/错误密码登录(401)/
登出(200)，服务器多线程日志确认每次请求独立处理；登录页与主界面已能正常启动。

## ⚠️ 首次拉取代码后要做的事

`lib/javafx/` 目录不在仓库里（体积大，`.gitignore` 排除了），拉下代码后要
自己下载 JavaFX 21.0.12 SDK，把它的 `lib/*.jar` 复制到项目的 `lib/javafx/`
目录下（新建这个目录即可），否则编译会报找不到 `javafx.*` 包。

## 马上要做的事（本周计划里的"半周1"）

1. **界面已切到 JavaFX**，后续继续优化各模块页面风格和交互即可。
   - JavaFX SDK 用的版本是 **21.0.12**，jar 包已经复制进项目的 `lib/javafx/`
     目录（这个目录被 `.gitignore` 排除，不提交，每人本地要自己放一份）
   - **不需要设置 `JAVAFX_HOME` 环境变量**——编译/运行命令直接用相对路径
     `lib/javafx` 当 module-path，对所有人的电脑都一样，不用管 JavaFX SDK
     解压到了哪个盘
   - 编译命令要加：`--module-path "lib/javafx" --add-modules javafx.controls,javafx.fxml`
   - 运行客户端命令也要加同样两个参数；`Server`（无界面）不需要
   - VS Code 里直接用 F5 调试也行，`.vscode/launch.json` 已经配好了
     "Run Server" 和 "Run LoginFrame" 两个启动项
   - JavaFX 界面类要继承 `javafx.application.Application`，重写
     `start(Stage stage)`，`main` 里调用 `launch(args)`
   - 登录页和主界面已经按这个模式完成，可直接作为其它模块参考模板
2. **`ServerThread` 请求路由重构**：已经做成可扩展处理器表，后续其他模块沿用
  这个模式继续接入即可。
3. **用户模块收尾**：如果还有未提交的界面微调或文案，需要在半周 1-2 内定稿。

## 参考/规划文档

- `PLAN.md`（main 分支）：完整的2.5周排期计划，5个半周节点，6人分工
- `DEMO.md`（dev/user 分支）：给老师验收用的完整演示流程
- `sql/vcampus_schema.sql`：建库建表脚本
- `Server/db.properties.example`：数据库连接配置模板（真实的 `db.properties`
  不提交，需要自己复制一份填密码，注意 url 要带 `allowPublicKeyRetrieval=true`）

## Git 约定

- 只在 `dev/xxx` 自己的分支上开发，不直接推 `main`（`main` 有分支保护，
  强制走 Pull Request）
- 改 `Common/` 下的公共文件前，先跟组里说一声，避免冲突
