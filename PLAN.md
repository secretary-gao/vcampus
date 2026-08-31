# Vcampus 项目排期计划（剩余 2.5 周 / 5 个半周）

制定时间：2026-08-31。剩余时间按"半周"为单位切分（1周=2个半周），共 5 个半周，
最后半周结束即验收/答辩。**这份文档是全组共用的执行手册，不是只给组长看的。**

## 人员与模块

| 模块 | 负责人 | 分支 | 说明书对应表 |
|---|---|---|---|
| 用户管理（组长） | 高鸣谦 | dev/user | tblUser |
| 图书馆 | 赵夏巍 | dev/library | tblBook、tblBorrow |
| 学籍 | 洪荣誉 | dev/student | tblStudent、tblClass |
| 商店 | 孙志平 | dev/shop | tblGoods、tblPurchase |
| 选课 | 刘酝潇 | dev/course | tblCourse、tblSelectCourse |
| 医院 | 李秋实 | dev/hospital | tblDoctor、tblAppointment |

**6 个模块全部必做**（医院不是选做，之前口误说错过一次）。界面统一用
**JavaFX**（不用 Swing）。每人开工前先看《共享说明书.docx》里自己模块那一节的
实体类字段和表结构设计，本文档不重复列字段，避免和说明书对不上。

## 现状（半周0，已完成）

- **用户管理**：三模块骨架、`Message`/`User`/`MD5Util`/`IConstant`、`DbHelper`、
  `UserDAO`、Socket 多线程通信（`Server`/`ServerThread`）、登录/注册 Swing
  界面，已全部跑通并通过 `UserClientSrvTest` 端到端验证。界面本周内要改写成
  JavaFX。
- **图书馆**：`Book`/`BorrowRecord` 的 VO + DAO + 建表 SQL 已完成（对应
  `sql/Library/` 目录），srv 层和界面未开始。
- **学籍/商店/选课/医院**：0 进度。

## 统一开发流水线（每个模块都走这五步，不要跳步）

1. 建表 SQL（放 `sql/<模块名>/` 目录）+ 实体类 VO（放 `Common/.../vo/`）
2. DAO（放 `Server/.../dao/`）+ **自测**：写一个 main 方法，插入一条测试数据
   再查出来打印，参考 `Server/src/vcampus/server/dao/UserDAOTest.java`
3. srv 层（`IxxxClientSrv` 放 `Client/.../biz/`，`IxxxServerSrv` 放
   `Server/.../srv/`）+ 接入 `Server` 的请求路由，能收发 Socket 消息，参考
   `Client/src/vcampus/client/biz/UserClientSrvTest.java` 的无界面验证方式
4. JavaFX 界面（放 `Client/.../view/`）
5. 联调、改 bug、发 Pull Request 合并到 main

**为什么强调"自测"**：数据层、Socket层分别自己先验证过，最后集成时出问题
才能快速定位是哪一层的锅，而不是从界面点不动开始一层层猜。

---

## JavaFX 环境搭建（所有人半周1必须做完，照抄即可）

**统一用 JavaFX 21.0.12**（组长已下载验证过，编译+运行冒烟测试通过），大家都下这个版本，
避免 6 个人版本不一样出现奇怪的兼容问题：

1. 直接下载（Windows x64 SDK）：
   https://download2.gluonhq.com/openjfx/21.0.12/openjfx-21.0.12_windows-x64_bin-sdk.zip
   （约 50MB；如果这个链接过期了，去 https://gluonhq.com/products/javafx/ 手动选 21.0.12）
2. 解压到自己电脑任意路径，**不要放进项目文件夹**，不需要提交进 git
   （比如解压到 `D:\javafx-sdk-21.0.12`）
3. 设置一个**用户级**环境变量 `JAVAFX_HOME`，指向解压出来的那个文件夹，PowerShell 里：
   ```powershell
   [Environment]::SetEnvironmentVariable("JAVAFX_HOME", "D:\javafx-sdk-21.0.12", "User")
   ```
   设完之后要**开一个新终端**才会生效，当前终端里 `$env:JAVAFX_HOME` 还是空的。
4. 编译命令（在项目根目录，PowerShell）：
   ```powershell
   javac -encoding UTF-8 --module-path "$env:JAVAFX_HOME\lib" --add-modules javafx.controls,javafx.fxml -d bin -cp "lib\mysql-connector-j-9.7.0.jar" (Get-ChildItem -Recurse -Path Common\src,Server\src,Client\src -Filter *.java).FullName
   ```
5. 运行客户端命令（比运行 Server 多两个参数）：
   ```powershell
   java --module-path "$env:JAVAFX_HOME\lib" --add-modules javafx.controls,javafx.fxml -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.client.view.LoginFrame
   ```
   服务器端不涉及界面，不需要这两个参数，照旧：
   ```powershell
   java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.Server
   ```
6. **JavaFX 代码写法和 Swing 不一样**，界面类要继承 `javafx.application.Application`：
   ```java
   public class XxxFrame extends Application {
       @Override
       public void start(Stage stage) {
           // 用 VBox/GridPane 等布局摆控件，包成 Scene 设置到 stage 上
       }
       public static void main(String[] args) {
           launch(args);
       }
   }
   ```
   组长会把 `LoginFrame` 改写成这个模式作为参考范本，写完发到 `dev/user` 分支，
   写自己模块界面时可以直接抄这个结构。

---

## Server 端请求路由（组长半周1重构，其他人半周3要接入）

现在 `ServerThread` 只认识 `login`/`register` 两种请求（写死的 if-else）。
组长会把它改成可扩展路由，大致是：每个模块提供一个"处理器"，注册到
`Server` 启动时的路由表里，`ServerThread` 收到请求后按 `Message.getName()`
的前缀或约定规则分发给对应模块的处理器，不再需要在同一个文件里堆 if-else。
具体接口形态定下来后，组长会更新这份文档并通知大家，**半周1大家不用等这个，
先把数据层做完**，半周3对接的时候再看更新说明。

---

## Git 协作与合并流程

- 每人只在自己的 `dev/xxx` 分支上开发，不要直接往 `main` 推
- **公共文件（`Common/` 下的东西）改动前先在群里说一声**，避免几个人同时改
  `Message`/`IConstant` 导致合并冲突
- 每个检查点（半周2、半周4、半周5）完成后，在 GitHub 上给 `main` 发一个
  Pull Request，组长 review 通过后合并；不用等到最后才一次性合并，越早合越
  容易解决冲突
- 提交信息（commit message）写清楚做了什么，参考已有的几条提交

## 验收标准（每个检查点"完成"具体指什么）

| 检查点 | 时间 | 达标线 |
|---|---|---|
| 半周1 | 周三晚 | 环境（JDK+MySQL+JavaFX+db.properties）搭好；自己模块的表已建；VO 类写完并能编译通过 |
| 半周2 | 周日晚 | DAO 写完，跑一遍自测 main 方法，能看到"插入成功→查询到刚插入的数据"这样的输出，并 push 到自己分支 |
| 半周3 | 下周三 | srv 层接口写完并接入 Server 路由，用类似 `UserClientSrvTest` 的无界面程序验证过能收到正确的响应（不要求界面） |
| 半周4 | 下周日 | JavaFX 界面做完，能从主菜单点进自己模块，完成至少一个完整操作闭环（比如图书馆：查图书→借书→能看到库存变化） |
| 半周5 | 验收前 | 所有模块从 main 拉下来能一起跑通，没有编译错误，演示脚本准备好 |

## 沟通节奏

- 每个半周结束当晚，群里报一次"做到哪了/卡在哪"，哪怕只有一句话
- 卡住超过半天没进展，主动说，不要自己憋着——组长的时间比一个人卡两天更值钱
- 组长每个检查点会看一遍大家的提交，有问题当面/群里指出，不搞书面考核那一套

## 风险提醒

- JavaFX 比 Swing 多一道环境搭建的坎（`--module-path`/`--add-modules`），
  半周1必须把这份指南发出去，避免 6 个人各自踩坑浪费时间
- 4 个 0 进度模块必须在半周1就真正动起来，半周2数据层不通是本阶段最大风险
- `ServerThread` 路由重构是全组的地基，必须半周1优先做完，但**不阻塞**其他人
  半周1的数据层工作（两条线并行）
