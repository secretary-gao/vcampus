# Course 模块最终交付说明
## 交付者：刘酝潇
本说明面向项目组长、验收教师和接手维护者。交付对象为 PR #4：`dev/course -> main`，
代码验证基线为 `4e1d9c2`（2026-09-05）。

## 1. Course 模块最终功能

- 学生：按课程号、名称或教师查询课程；查看容量和已选人数；选课、退课、查看我的课程和每周课程表。
- 管理员界面：查询、新增、修改和删除排课，校验教师及教室时间冲突。
- 学籍接入：将登录用户 ID 映射到正式学号；未关联学籍的账号显示提示。
- 数据一致性：选退课在同一事务中同步修改选课记录和 `selectedCount`，处理重复选课、满员及失败回滚。
- 界面集成：从 MainFrame 的“教务”入口进入；选退课成功后大厅、我的课程、课程表各刷新一次。

管理员界面可见性不等于服务端授权，当前权限边界见第 11 节。

## 2. 模块架构与调用链

```text
MainFrame -> CoursePanel / 各业务页面
    -> ICourseClientSrv / CourseClientSrv
    -> Message + 短连接 Socket
    -> ServerThread -> CourseHandler (ModuleHandler)
    -> ICourseServerSrv / CourseServerSrv
    -> CourseDAO / SelectCourseDAO / CourseScheduleDAO / CourseStudentDAO
    -> MySQL vCampus
```

JavaFX 页面通过后台 Task 发起网络请求，并在 FX 线程更新控件，不直接访问数据库。
Service 负责业务校验和事务；DAO 负责参数化 SQL 与对象映射。选退课先锁定课程行，
各 DAO 共用调用方的 JDBC Connection，失败时回滚。

Course 复用统一 Server：`ServerThread` 仅追加一行注册，`IConstant` 追加消息常量。
本 PR 同时接通 MainFrame 的 Course、Hospital 入口，保留 User / Library / Student / Shop / Hospital 路由。

## 3. 主要文件与职责

| 位置 | 职责 |
|---|---|
| `Common/src/vcampus/common/vo/{Course,SelectCourse,CourseSchedule}.java` | 课程、选课和排课序列化对象 |
| `Common/src/vcampus/common/constant/IConstant.java` | Course 消息名及共享通信常量 |
| `Server/src/vcampus/server/dao/*Course*.java` | 课程、选课、排课持久化及相关自测 |
| `Server/src/vcampus/server/dao/CourseStudentDAO.java` | 登录用户到正式学号的只读映射 |
| `Server/src/vcampus/server/srv/CourseServerSrv.java` | 选退课事务、排课冲突检查、课表汇总 |
| `Server/src/vcampus/server/srv/CourseHandler.java` | 请求解析、Service 调用和响应封装 |
| `Client/src/vcampus/client/biz/CourseClientSrv.java` | Course Socket 客户端 |
| `Client/src/vcampus/client/view/course/` | CoursePanel、课程大厅、我的课程、课程表、排课管理及独立演示入口 |
| `Client/src/vcampus/client/view/{MainFrame,HospitalFrame}.java` | 主界面接入及医院图片加载 |
| `sql/course/` | Course schema 和可选 demo 数据 |
| `build.bat`、`run-*.bat`、`start-all.bat`、`.vscode/launch.json` | 构建、资源复制和启动入口 |

## 4. 数据库设计与初始化

使用 MySQL 8.0、InnoDB 和 UTF-8。先按 `Server/db.properties.example` 创建本地
`Server/db.properties` 并填写连接配置；该文件不提交。

| 表 | 关键约束 |
|---|---|
| `tblCourse` | 课程主键；学分、容量大于零；`0 <= selectedCount <= capacity` |
| `tblSelectCourse` | `UNIQUE(studentId, courseId)`；外键关联正式 `tblStudent` 和 `tblCourse` |
| `tblCourseSchedule` | 外键关联课程；星期 1–7；开始早于结束；同课程同星期同起止时间唯一 |

教师取自 `tblCourse.teacher`。教师和教室的区间重叠由 Service 在事务中检查，相邻时段允许。
跨表人数一致性由选退课 Service 事务维护，直接修改表数据不能替代业务操作。

在仓库根目录的 PowerShell 中启动 MySQL（需将客户端加入 PATH）：

```powershell
mysql -u root -p --default-character-set=utf8mb4
```

输入密码后，在 **MySQL 提示符内**逐条执行。确认每一步没有 `ERROR` 后再继续：

```sql
SOURCE sql/vcampus_schema.sql;
SOURCE sql/Student/BuildTbl.sql;
SOURCE sql/course/vcampus_course.sql;
SOURCE sql/seed_demo_data.sql;
SOURCE sql/course/seed_course_demo.sql;

USE vCampus;
SELECT courseId, courseName, teacher, HEX(courseName) FROM tblCourse ORDER BY courseId;
SELECT scheduleId, classroom, dayOfWeek, startTime, endTime FROM tblCourseSchedule;
EXIT;
```

前三步依次建用户、学籍和 Course 表；后两步创建共享 demo 用户及 Course 演示数据。
`CREATE TABLE IF NOT EXISTS` 和 `INSERT IGNORE` 不会重置已有业务数据，也不替代旧 schema 的迁移。
其他模块的表和示例数据仍按各模块说明初始化；此流程不创建 Shop 商品或 Hospital 医生。

`SOURCE` 让 MySQL 直接读取 UTF-8 文件。不要使用 `Get-Content ... | mysql`：
PowerShell 5.1 的默认文件解码和原生程序管道编码可能损坏中文，单独设置
`-Encoding UTF8` 或 MySQL 字符集参数不足以解决两次转换。可用 `HEX(...)` 区分终端显示与存储字节问题。

## 5. Socket API

请求与响应使用共享 `Message`，每次调用建立一条短连接。下表列出 `Message.data`：

| 消息名 | 请求 data | 成功响应 data |
|---|---|---|
| `courseQuery` | 关键字字符串；空值查询全部 | `List<Course>` |
| `courseSelect` / `courseDrop` | 包含 `studentId`、`courseId` 的 Map | 成功提示字符串 |
| `courseSelectedQuery` | 学号字符串 | `List<SelectCourse>` |
| `courseStudentIdQuery` | 用户 ID 字符串 | 学号或 `null` |
| `courseScheduleQuery` | 无 | `List<CourseSchedule>` |
| `courseScheduleAdd` | `CourseSchedule` | 带记录号的 `CourseSchedule` |
| `courseScheduleUpdate` | `CourseSchedule` | `true` |
| `courseScheduleDelete` | 排课记录号字符串 | `true` |
| `studentTimetableQuery` | 学号字符串 | 已选课程的 `List<CourseSchedule>` |

成功返回 `200`；业务校验或请求参数类型错误返回 `400`；JDBC / IO 异常返回 `500`。

## 6. Java 21 / JavaFX 构建与启动

环境：JDK 21、JavaFX 21.0.12 Windows SDK、MySQL Connector/J 9.7.0；仓库不使用 Maven。
所有命令从仓库根目录执行。JDK 应加入 PATH，先用 `java -version` 和 `javac -version` 确认版本。

本仓库采用平铺 JAR 布局：

```text
lib/
  mysql-connector-j-9.7.0.jar
  javafx/
    javafx.base.jar
    javafx.controls.jar
    javafx.fxml.jar
    javafx.graphics.jar
    ...
  bin/
    glass.dll
    prism_d3d.dll
    prism_sw.dll
    ...
```

首次准备依赖时，将下列占位路径替换为本机解压后的 SDK 目录：

```powershell
$fx = 'C:\path\to\javafx-sdk-21.0.12'
New-Item -ItemType Directory -Force lib\javafx,lib\bin | Out-Null
Copy-Item "$fx\lib\*.jar" lib\javafx\
Copy-Item "$fx\bin\*.dll" lib\bin\
```

Windows native DLL 必须与 JAR 版本匹配；JavaFX 从平铺 JAR 所在目录的同级 `bin` 加载它们。
仅有 JAR 可以编译，但运行可能报 `no suitable pipeline found`。
`lib/javafx` 和 `lib/bin` 均被 gitignore 忽略，不提交本地 SDK 二进制。

正式构建与启动：

```powershell
.\build.bat
# 终端一：
.\run-server.bat
# 终端二：
.\run-client.bat
```

也可单独使用 `start-all.bat` 一次启动 Server 和 Client，不要与上面的已运行实例重复启动。
Server 使用端口 `8888`；VS Code 的 LoginFrame 启动配置使用相同 module-path。
所有 JavaFX 启动入口使用 `lib\javafx`，不是 `lib\javafx\lib`。

`build.bat` 编译 Common / Server / Client，并复制医院图片到
`bin/vcampus/client/view/seu_logo.jpeg`。它不会自动清空旧 `bin`；clean 验证应先备份或清理旧产物。
编译产物可直接运行，不需要把源码目录加入 classpath：

```powershell
java --module-path lib\javafx --add-modules javafx.controls,javafx.fxml `
  -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.client.view.LoginFrame
```

## 7. 演示账号与人工演示流程

以下账号来自初始化脚本；若对应账号此前已存在，`INSERT IGNORE` 不会覆盖其密码和角色。

| 身份 | 登录 ID | 初始密码 | 说明 |
|---|---|---|---|
| 学生 | `09010101` | `123456` | 正式学号 `2026000001` |
| 教务管理员 | `ADMIN001` | `123456` | 打开排课管理 |

1. 学生登录后进入“教务”，查询并选择一门课程。
2. 在“我的课程”检查记录，在“我的课程表”检查教师、教室和时间。
3. 退课后检查三个页面：大厅人数恢复，已选课程及对应课表记录消失。
4. 管理员登录，新增排课，再尝试教师重叠和教室重叠；检查拒绝提示。
5. 修改、删除本次新增的排课，检查表格刷新；从 MainFrame 打开医院，确认入口和图片正常。

## 8. 自动化测试与验证

先构建并初始化测试数据库。除已隔离的排课 Service 测试外，部分既有 DAO / Socket 测试仍使用
固定夹具 ID；应在测试库运行，并确认这些 ID 未被业务数据占用。

```powershell
$cp = 'bin;lib\mysql-connector-j-9.7.0.jar'
$tests = @(
    'vcampus.server.dao.CourseDAOTest',
    'vcampus.server.dao.SelectCourseDAOTest',
    'vcampus.server.srv.CourseServerSrvTest',
    'vcampus.server.dao.CourseScheduleDAOTest',
    'vcampus.server.srv.CourseScheduleServerSrvTest'
)
foreach ($test in $tests) {
    java -cp $cp $test
    if ($LASTEXITCODE -ne 0) { throw "$test failed" }
}
```

Socket E2E 需另行启动统一 Server；夹具会创建并清理回滚故障触发器，需要测试库的相应权限：

```powershell
$cp = 'bin;lib\mysql-connector-j-9.7.0.jar'
try {
    java -cp $cp vcampus.server.srv.CourseSocketTestFixture setup
    if ($LASTEXITCODE -ne 0) { throw 'Fixture setup failed' }
    java -cp $cp vcampus.client.biz.CourseClientSrvTest
    if ($LASTEXITCODE -ne 0) { throw 'Course Socket E2E failed' }
    java -cp $cp vcampus.client.biz.CourseScheduleClientSrvTest
    if ($LASTEXITCODE -ne 0) { throw 'Schedule Socket E2E failed' }
} finally {
    java -cp $cp vcampus.server.srv.CourseSocketTestFixture cleanup
    java -cp $cp vcampus.server.srv.CourseSocketTestFixture residue
    if ($LASTEXITCODE -ne 0) { throw 'Fixture residue check failed' }
}
```

本轮独立审计及修复回归的实际结果（补充验证使用本地审计程序，并非新增仓库测试入口）：

| 验证范围 | 实际结果 |
|---|---|
| 构建与正式入口 | JDK 21 编译全部 119 个 Java 文件；最终真实工作区 clean build 和 `run-client.bat` 登录窗口启动通过 |
| DAO / Service / Socket | 上述 5 个直接测试及 2 个 Socket E2E 通过；包含正常操作、重复选课、满员、约束和故障回滚 |
| 独立事务与并发 | 24 个客户端争抢 5 个名额，仅 5 个成功；16 次重复选课仅成功一次；12 次并发退课仅扣减一次；SQLException / RuntimeException 注入后数据回滚 |
| 排课 | 教师/教室冲突、相邻时段、排除自身、更新/删除不存在记录均验证；默认 REPEATABLE-READ 下并发冲突排课未双提交，但可能出现 1213 |
| 测试隔离 | 已建表空库、导入 demo、已有正常业务数据三环境通过，前后内容摘要一致，包括保留与旧测试 ID 相同的既有记录 |
| JavaFX / Hospital | 真实 Socket 选退课后三页面同步，每页仅刷新一次；Course 网络调用不在 FX 线程；医院 MainFrame 卡片入口及缺图降级验证通过 |
| 六模块集成 | 统一 Server 处理 User 登录失败响应，以及 Library / Student / Shop / Hospital / Course 查询；Shop 插入测试商品后能查询到，原空列表对应缺少数据 |
| 测试收尾 | 原有 11 张业务表前后内容 SHA-256 一致；测试触发器、临时数据库已清理；测试 Server 停止，8888 释放；代码修复 diff 检查通过 |

六模块 smoke 证明共同编译和路由可用，不等同于其他五模块全部业务流程的 UAT。

## 9. 最终人工 UAT 验收

最终人工 UAT 已完成。项目负责人在业务代码基线
`4e1d9c24093ca174361b0826659ce9bd3efb3520` 上实际验证并确认以下结果：

1. 验收时 `git status` clean，本地 HEAD 与 `origin/dev/course` 一致。
2. 在真实工作区直接执行 `.\build.bat`，JDK 21 编译成功，Hospital 图片资源复制成功，输出 `Build succeeded.`。
3. LoginFrame 实际启动成功，学生账号 `09010101 / 123456` 登录成功，MainFrame 正常。
4. MainFrame → Hospital 实际打开成功，不再出现资源加载异常；MainFrame → Course 实际打开成功。
5. Course 学生端课程大厅正常显示，选课、我的课程、我的课程表及退课均正常。
6. 退课后无需手动点击“查询”，大厅已选人数立即自动刷新（例如 `1/60 -> 0/60`），我的课程和课程表状态同步。
7. JavaFX Windows native DLL 本地运行环境正常。

以上为项目负责人明确确认的人工验收范围。其他模块仍按第 8 节称为 smoke / integration regression，
不表示其他五模块全部业务均经过本人完整 UAT；管理员排课的验证结果仍以工程回归记录为准。

## 10. 本 PR 集成过程中修复的问题

以下修复来自 2026-09-05 下午的独立审计、人工 UAT 反馈及实际回归。

| 问题 | 修法 | 验证结果 / 提交 |
|---|---|---|
| MainFrame 打开医院时找不到 `seu_logo.jpeg` | 构建复制 classpath 资源，按包路径加载，缺图或解码失败时省略非关键图片 | 真实卡片入口正常打开；移除图片后不崩溃。`fa87fdb` |
| PowerShell 5.1 管道执行中文 SQL 损坏编码 | 改用 MySQL `SOURCE` 直接读取 UTF-8，并明确用户、学籍、Course、demo 的执行顺序 | 原管道产生问号；SOURCE 导入后中文 HEX 正确，完整顺序执行通过。`fa87fdb` |
| 排课 Service 测试与 demo 冲突 | 每轮隔离课程、学生、教师和教室标识；取消预先删除，按成功创建记录清理 | 空表、demo、正常业务数据三环境通过，既有数据不变。`304376a` |
| 退课后大厅人数未自动刷新 | CoursePanel 统一刷新三个页面，移除子页面重复刷新 | 真实选退课后人数、已选记录和课表同步，逐页刷新次数为一次。`ed756df` |
| 正式入口假设嵌套 `lib/javafx/lib` | `build.bat`、`run-client.bat`、`start-all.bat` 和 VS Code launch 统一为平铺 `lib/javafx` | 真实仓库 JDK 21 clean build、图片复制、正式 LoginFrame 启动通过。`4e1d9c2` |
| 本地仅有 JavaFX JAR，缺少 Windows native DLL | 在被忽略的 `lib/bin` 补齐匹配版本 DLL；第 6 节说明依赖布局，不提交二进制 | 正式登录窗口正常显示并响应；此项为本地依赖准备，无二进制提交 |

早期 staging 使用完整 SDK 的嵌套目录，未复现真实工作区的平铺布局，因此不能作为正式脚本路径正确的证据。
最终构建验收已改为在真实仓库备份旧 `bin` 后直接运行脚本。

## 11. 当前已知架构边界

- 公共 Socket 架构缺少可信 server session/token，服务端权限控制不完整。Course 管理员 UI 隐藏不能防止直接报文调用，学号参数也不能作为可信身份；本 PR 未重构六模块认证体系。
- 高并发排课可能触发 MySQL `1213` deadlock，目前回滚并返回错误，没有自动完整事务重试。
- Course Socket 暂无统一 connect/read timeout；连接保持但不响应时，后台请求可能持续等待。
- 排课模型暂未覆盖学期、教学周、单双周和临时调课。
- 教师按名称识别，尚未建立教师账号外键。

以上均为保留边界，不属于本轮已经修复的功能。

## 12. 最终 Git / PR 状态

核对快照：2026-09-05；PR #4 交付方向为 `dev/course -> main`。

- 已推送并经最终人工 UAT 确认的业务代码基线：`4e1d9c24093ca174361b0826659ce9bd3efb3520`；验收时本地分支与 `origin/dev/course` 一致。
- 对比基线：`origin/main@422419fd96298e1031eca7a228a2f91f08b70140`。
- 文档整理前 PR diff：41 个变更文件，覆盖 Course 全链路、主界面集成及相关资源/构建入口修复。
- 最终提交 `docs(course): finalize delivery and UAT record` 仅更新本说明，会产生新的 HEAD，业务代码没有再次修改。
- 工程结论：`ready for review / merge`。最终人工 UAT 已完成，第 11 节已知架构边界继续保留。

本次交付包含文档提交和推送，不执行 main 合并；PR Conversation 评论单独提供文本，不自动发布。
Git 远端分支已核对，GitHub 的审批、CI 和合并资格状态未在本说明中作通过声明。
