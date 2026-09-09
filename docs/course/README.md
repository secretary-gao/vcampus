# Course 模块 Reality Track Phase 1 交付说明
## 交付者：刘酝潇
本说明面向项目组长、验收教师和接手维护者。交付对象为 PR #4：`dev/course -> main`，
Course 1.0 已随 PR #6 合入 `main`。本分支在已验收 UI 基础上，将选课模型升级为
“课程本体—教学班—多段排课”，Phase 1 完成后等待新的人工 UAT。

## Reality Track Phase 1

本阶段复现 2024 级计算机科学与技术专业在 2026-2027 秋季学期的选课场景。
附件截图中可完整核对 27 门课程、40 个教学班和 62 条排课；页面标注共有 28 门，
但未展示的第 28 门没有被猜测或录入。

```text
Course 1 ── N TeachingClass 1 ── N CourseSchedule
                         N
                         │
                         M
                      Student
                  through SelectCourse
```

- `Course` 保存课程号、名称、学分、课程性质和开课单位。
- `TeachingClass` 保存班号、教师、容量、已选人数、教学语言和备注。
- `CourseSchedule` 关联教学班，可保存多条起止周、星期、起止节次和教室。
- `SelectCourse` 指向教学班，并保留 `courseId` 兼容列来直接执行“同学生同课程只能选一个教学班”的唯一约束。
- `tblCourse.teacher/capacity/selectedCount` 暂时保留为 Course 1.0 兼容投影；运行时容量判断和人数事务只以 `tblTeachingClass` 为准。

截图未给出的教室写为“待补充”。`startTime/endTime` 是兼容旧课表控件的 demo 钟点映射；
真实排课事实以周次和节次字段为准。模拟学生使用 `C2400001` 至 `C2400150`，姓名明确为
“模拟学生xxx”，不包含真实同学信息。

## 1. Course 模块最终功能

- 学生：按课程号、名称或教师查询具体教学班；查看班号、容量和人数；选择、退选并查看对应课表。
- 教师：按登录用户姓名匹配 `tblTeachingClass.teacher`，查看本人负责的教学班和学生名单。
- 管理员：维护课程和教学班后端数据，并按教学班维护多段周次/节次排课。
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
    -> CourseDAO / TeachingClassDAO / SelectCourseDAO / CourseScheduleDAO / CourseStudentDAO
       / TeacherCourseEnrollmentDAO
    -> MySQL vCampus
```

JavaFX 页面通过后台 Task 发起网络请求，并在 FX 线程更新控件，不直接访问数据库。
Service 负责业务校验和事务；DAO 负责参数化 SQL 与对象映射。选退课先锁定教学班行，
各 DAO 共用调用方的 JDBC Connection，失败时回滚。

Course 复用统一 Server：`ServerThread` 仅追加一行注册，`IConstant` 追加消息常量。
本 PR 同时接通 MainFrame 的 Course、Hospital 入口，保留 User / Library / Student / Shop / Hospital 路由。

## 3. 主要文件与职责

| 位置 | 职责 |
|---|---|
| `Common/src/vcampus/common/vo/` | 课程、教学班、选课、排课及教师名单序列化对象 |
| `Common/src/vcampus/common/constant/IConstant.java` | Course 消息名及共享通信常量 |
| `Server/src/vcampus/server/dao/*Course*.java`、`TeachingClassDAO.java` | 课程、教学班、选课、排课持久化及自测 |
| `Server/src/vcampus/server/dao/CourseStudentDAO.java` | 登录用户到正式学号的只读映射 |
| `Server/src/vcampus/server/srv/CourseServerSrv.java` | 课程管理、选退课事务、排课冲突检查、课表及教师名单查询 |
| `Server/src/vcampus/server/srv/CourseHandler.java` | 请求解析、Service 调用和响应封装 |
| `Client/src/vcampus/client/biz/CourseClientSrv.java` | Course Socket 客户端 |
| `Client/src/vcampus/client/view/course/` | 学生选课页、教师名单页、课程/排课管理页及独立演示入口 |
| `Client/src/vcampus/client/view/{MainFrame,HospitalFrame}.java` | 主界面接入及医院图片加载 |
| `sql/course/` | 新库 schema、Course 1.0 migration、真实课程 seed 和旧 demo |
| `build.bat`、`run-*.bat`、`start-all.bat`、`.vscode/launch.json` | 构建、资源复制和启动入口 |

## 4. 数据库设计与初始化

使用 MySQL 8.0、InnoDB 和 UTF-8。先按 `Server/db.properties.example` 创建本地
`Server/db.properties` 并填写连接配置；该文件不提交。

| 表 | 关键约束 |
|---|---|
| `tblCourse` | 课程本体主键；学分大于零；课程性质和开课单位允许待补充 |
| `tblTeachingClass` | 外键关联课程；同课程班号唯一；`0 <= selectedCount <= capacity` |
| `tblSelectCourse` | 外键关联学生和教学班；复合外键保证兼容 `courseId` 与教学班一致；学生+教学班、学生+课程均唯一 |
| `tblCourseSchedule` | 外键关联教学班；复合外键保证兼容 `courseId` 一致；周次 1–30、星期 1–7、节次 1–13；教学班时段唯一 |

教师取自 `tblTeachingClass.teacher`。教师和教室冲突同时比较周次与节次，相邻节次和不重叠周次允许。
跨表人数一致性由选退课 Service 事务维护，直接修改表数据不能替代业务操作。

在仓库根目录的 PowerShell 中启动 MySQL（需将客户端加入 PATH）：

```powershell
mysql -u root -p --default-character-set=utf8mb4
```

输入密码后，在 **MySQL 提示符内**逐条执行。确认每一步没有 `ERROR` 后再继续：

```sql
SOURCE sql/vcampus_schema.sql;
SOURCE sql/Student/BuildTbl.sql;
SOURCE sql/hospital/vcampus_hospital.sql;
SOURCE sql/shop/vcampus_shop.sql;
SOURCE sql/course/vcampus_course.sql;
SOURCE sql/seed_demo_data.sql;
SOURCE sql/course/seed_course_demo.sql;
SOURCE sql/course/seed_cs2024_fall_realistic.sql;

USE vCampus;
SELECT courseId, courseName, teacher, HEX(courseName) FROM tblCourse ORDER BY courseId;
SELECT scheduleId, classroom, dayOfWeek, startTime, endTime FROM tblCourseSchedule;
EXIT;
```

前五步依次建用户、学籍、Hospital、Shop 和 Course 表；后续脚本创建共享 demo、Course demo
与 Reality Track 数据。先建 Hospital 和 Shop 表，是因为共享 `seed_demo_data.sql` 包含挂号与购买记录。
`CREATE TABLE IF NOT EXISTS` 和 `INSERT IGNORE` 不会重置已有业务数据，也不替代旧 schema 的迁移。
其他模块仍按各自说明初始化。

`SOURCE` 让 MySQL 直接读取 UTF-8 文件。不要使用 `Get-Content ... | mysql`：
PowerShell 5.1 的默认文件解码和原生程序管道编码可能损坏中文，单独设置
`-Encoding UTF8` 或 MySQL 字符集参数不足以解决两次转换。可用 `HEX(...)` 区分终端显示与存储字节问题。

已有 Course 1.0 数据库不要重建表，按以下顺序迁移：

```sql
SOURCE sql/course/migration_course_teaching_class.sql;
SOURCE sql/course/seed_cs2024_fall_realistic.sql;
```

migration 只为尚无教学班的旧课程创建 `01` 教学班，将旧选课和排课补上教学班外键，旧排课默认映射为
1-16 周并根据原钟点换算节次；随后按实际选课记录校正教学班人数。脚本已在旧 demo 库连续执行两次，
也已在 realistic seed 后连续执行两次；旧记录数量不变，已有多教学班课程不会产生额外兼容班。

## 5. Socket API

请求与响应使用共享 `Message`，每次调用建立一条短连接。下表列出 `Message.data`：

| 消息名 | 请求 data | 成功响应 data |
|---|---|---|
| `courseQuery` | 关键字字符串；空值查询全部 | `List<Course>` |
| `teachingClassQuery` | 课程/教学班/教师关键字 | `List<TeachingClass>` |
| `teachingClassAdd` / `teachingClassUpdate` | `TeachingClass` | 对象 / `true` |
| `teachingClassDelete` | 教学班 ID | `true` |
| `courseAdd` | `Course` | 已新增的 `Course`；`selectedCount` 由服务端置零 |
| `courseUpdate` | `Course` | `true` |
| `courseDelete` | 课程号字符串 | `true` |
| `courseSelect` / `courseDrop` | 包含 `studentId`、`teachingClassId` 的 Map | 成功提示字符串 |
| `courseSelectedQuery` | 学号字符串 | `List<SelectCourse>` |
| `courseStudentIdQuery` | 用户 ID 字符串 | 学号或 `null` |
| `courseScheduleQuery` | 无 | `List<CourseSchedule>` |
| `courseScheduleAdd` | `CourseSchedule` | 带记录号的 `CourseSchedule` |
| `courseScheduleUpdate` | `CourseSchedule` | `true` |
| `courseScheduleDelete` | 排课记录号字符串 | `true` |
| `studentTimetableQuery` | 学号字符串 | 已选课程的 `List<CourseSchedule>` |
| `teacherCourseEnrollmentsQuery` | 教师姓名字符串 | `List<TeacherCourseEnrollment>` |

成功返回 `200`；业务校验或请求参数类型错误返回 `400`；JDBC / IO 异常返回 `500`。

## 6. Java 21 / JavaFX 构建与启动

环境：JDK 21、JavaFX 21.0.12 Windows SDK、MySQL Connector/J 9.7.0；仓库不使用 Maven。
所有命令从仓库根目录执行。JDK 应加入 PATH，先用 `java -version` 和 `javac -version` 确认版本。

团队统一采用 JavaFX SDK 嵌套布局：

```text
lib/
  mysql-connector-j-9.7.0.jar
  javafx/
    lib/
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
New-Item -ItemType Directory -Force lib\javafx\lib,lib\javafx\bin | Out-Null
Copy-Item "$fx\lib\*" lib\javafx\lib\ -Recurse -Force
Copy-Item "$fx\bin\*" lib\javafx\bin\ -Recurse -Force
```

Windows native DLL 必须与 JAR 版本匹配，并保留在同一 SDK 的 `lib/javafx/bin`。
仅有 JAR 可以编译，但运行可能报 `no suitable pipeline found`。
`lib/javafx` 被 gitignore 忽略，不提交本地 SDK 二进制。

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
所有 JavaFX 启动入口统一使用 `lib\javafx\lib`。

`build.bat` 编译 Common / Server / Client，并复制医院图片到
`bin/vcampus/client/view/seu_logo.jpeg`。它不会自动清空旧 `bin`；clean 验证应先备份或清理旧产物。
编译产物可直接运行，不需要把源码目录加入 classpath：

```powershell
java --module-path lib\javafx\lib --add-modules javafx.controls,javafx.fxml `
  -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.client.view.LoginFrame
```

## 7. 演示账号与人工演示流程

以下账号来自初始化脚本；若对应账号此前已存在，`INSERT IGNORE` 不会覆盖其密码和角色。

| 身份 | 登录 ID | 初始密码 | 说明 |
|---|---|---|---|
| 学生 | `09010101` | `123456` | 正式学号 `2026000001` |
| 教师 | `09010103` | `123456` | 姓名“王老师”，匹配 `CSE1001` |
| 教务管理员 | `ADMIN001` | `123456` | 打开课程管理和排课管理 |
| Reality Track 模拟学生 | `C2400001`–`C2400150` | `123456` | 对应 2024 级计科模拟学籍 |
| Reality Track 教师 demo | `TCH00001` | `123456` | 合成账号；显示名“陈龙”用于匹配教学班 |

1. 学生登录后进入“教务”，查询并选择一门课程。
2. 在“我的课程”检查记录，在“我的课程表”检查教师、教室和时间。
3. 退课后检查三个页面：大厅人数恢复，已选课程及对应课表记录消失。
4. 教师登录，在“我教的课程”查看本人课程及选课学生名单，不显示选退课或管理入口。
5. 管理员登录，新增、修改和删除测试课程；再新增排课并尝试教师重叠和教室重叠，检查拒绝提示。
6. 清理本次新增的课程和排课；从 MainFrame 打开医院，确认入口和图片正常。

## 8. 自动化测试与验证

先构建并初始化测试数据库。除已隔离的排课 Service 测试外，部分既有 DAO / Socket 测试仍使用
固定夹具 ID；应在测试库运行，并确认这些 ID 未被业务数据占用。

```powershell
$cp = 'bin;lib\mysql-connector-j-9.7.0.jar'
$tests = @(
    'vcampus.server.dao.CourseDAOTest',
    'vcampus.server.dao.SelectCourseDAOTest',
    'vcampus.server.srv.CourseServerSrvTest',
    'vcampus.server.srv.CourseRoleServerSrvTest',
    'vcampus.server.dao.CourseScheduleDAOTest',
    'vcampus.server.srv.CourseScheduleServerSrvTest',
    'vcampus.server.dao.TeachingClassDAOTest',
    'vcampus.server.srv.TeachingClassServerSrvTest',
    'vcampus.server.dao.ScheduleMigrationTest',
    'vcampus.server.dao.SelectionMigrationTest'
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

Reality Track Phase 1 新增并实际运行：

- `TeachingClassDAOTest`：教学班 CRUD 和关键字查询。
- `TeachingClassServerSrvTest`：指定教学班选退、容量同步和同课程跨班互斥。
- `ScheduleMigrationTest`：排课外键、周次和节次迁移。
- `SelectionMigrationTest`：选课外键、重复课程、人数漂移和超容量检查。
- 空库初始化与旧 Course 1.0 demo migration 均在临时数据库重放，临时库随后删除。

Course 1.0 独立审计及修复回归的实际结果：

| 验证范围 | 实际结果 |
|---|---|
| 构建与正式入口 | JDK 21 编译全部 142 个 Java 文件；真实工作区构建、Hospital 资源复制和仅依赖 `bin` 的 LoginFrame 启动通过 |
| DAO / Service / Socket | 上述 10 个直接测试及 2 个 Socket E2E 通过；包含课程管理、教学班管理、教师名单、正常选退课、重复选课、满员、约束和故障回滚 |
| 独立事务与并发 | 24 个客户端争抢 5 个名额，仅 5 个成功；16 次重复选课仅成功一次；12 次并发退课仅扣减一次；SQLException / RuntimeException 注入后数据回滚 |
| 排课 | 按周次/节次检查教师与教室冲突；相邻节次、不重叠周次、排除自身、更新/删除不存在记录均验证；默认 REPEATABLE-READ 下并发冲突排课未双提交，但可能出现 1213 |
| 测试隔离 | 已建表空库、导入 demo、已有正常业务数据三环境通过，前后内容摘要一致，包括保留与旧测试 ID 相同的既有记录 |
| JavaFX / Hospital | 真实 Socket 选退课后三页面同步，每页仅刷新一次；Course 网络调用不在 FX 线程；医院 MainFrame 卡片入口及缺图降级验证通过 |
| 六模块集成 | 统一 Server 下 User、Library、Student、Hospital、Shop、Course 六条只读路由均返回有效响应；Shop 空列表仍对应本地缺少商品数据 |
| Reality Track 数据 | 空库得到 27 门课程、40 个教学班、62 条排课、150 名模拟学生和 1050 条选课；每名模拟学生 7 门课，重复、超容量、人数漂移和孤儿引用均为 0 |
| 测试收尾 | 所有测试 fixture residue 为 0；故障触发器、临时数据库已清理；测试 Server 停止，8888 释放；`git diff --check` 通过 |

六模块 smoke 证明共同编译和路由可用，不等同于其他五模块全部业务流程的 UAT。

## 9. 最终人工 UAT 验收

Course 1.0 最终人工 UAT 已完成。项目负责人在业务代码基线
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

Reality Track Phase 1 改变了数据模型和教学班交互，当前等待项目负责人按本文末尾流程重新 UAT；
不能沿用 Course 1.0 的人工 PASS 代替本阶段验收。

## 10. 本 PR 集成过程中修复的问题

以下修复来自 2026-09-05 下午的独立审计、人工 UAT 反馈及实际回归。

| 问题 | 修法 | 验证结果 / 提交 |
|---|---|---|
| MainFrame 打开医院时找不到 `seu_logo.jpeg` | 构建复制 classpath 资源，按包路径加载，缺图或解码失败时省略非关键图片 | 真实卡片入口正常打开；移除图片后不崩溃。`fa87fdb` |
| PowerShell 5.1 管道执行中文 SQL 损坏编码 | 改用 MySQL `SOURCE` 直接读取 UTF-8，并明确用户、学籍、Course、demo 的执行顺序 | 原管道产生问号；SOURCE 导入后中文 HEX 正确，完整顺序执行通过。`fa87fdb` |
| 排课 Service 测试与 demo 冲突 | 每轮隔离课程、学生、教师和教室标识；取消预先删除，按成功创建记录清理 | 空表、demo、正常业务数据三环境通过，既有数据不变。`304376a` |
| 退课后大厅人数未自动刷新 | CoursePanel 统一刷新三个页面，移除子页面重复刷新 | 真实选退课后人数、已选记录和课表同步，逐页刷新次数为一次。`ed756df` |
| Course 分支一度采用平铺 `lib/javafx`，与团队环境不一致 | 按团队约定将 `build.bat`、`run-client.bat`、`start-all.bat` 和 VS Code launch 统一为 `lib/javafx/lib` | 嵌套 SDK 下 JDK 21 构建、图片复制及正式 LoginFrame 启动通过 |
| 本地仅有 JavaFX JAR，缺少 Windows native DLL | 在被忽略的 `lib/javafx/bin` 补齐同一 SDK 的匹配 DLL；不提交二进制 | 正式登录窗口正常显示并响应；此项为本地依赖准备，无二进制提交 |
| Course 页面未区分教师，管理员只能维护排课 | CoursePanel 按登录角色显示学生、教师、管理员页面；新增教师名单查询和管理员课程 CRUD | Service 与 Socket 回归覆盖教师名单、课程 CRUD、引用保护和人数保持；JavaFX 角色页 smoke 通过 |

本地依赖目录必须遵循团队 README 的嵌套 SDK 约定；正式脚本不包含个人绝对路径。

## 11. 当前已知架构边界

- 公共 Socket 架构缺少可信 server session/token，服务端权限控制不完整。Course 管理员 UI 隐藏不能防止直接报文调用，学号参数也不能作为可信身份；本 PR 未重构六模块认证体系。
- 高并发排课可能触发 MySQL `1213` deadlock，目前回滚并返回错误，没有自动完整事务重试。
- Course Socket 暂无统一 connect/read timeout；连接保持但不响应时，后台请求可能持续等待。
- 排课模型暂未单独建模学期，周次只支持连续区间。
- 教师按名称识别，尚未建立教师账号外键。
- 中文班/英文班等价课程组尚未建模；当前只保证同一 `courseId` 的教学班互斥。
- 单双周、指定离散周和临时调课仍待后续阶段。

以上均为保留边界，不属于本轮已经修复的功能。

## 12. 最终 Git / PR 状态

核对日期：2026-09-09。

- Course 1.0 已通过 PR #6 合入；最终工程回归已同步到 `origin/main@8691c80`。
- Reality Track 从稳定 UI `dev/course@c897686` 创建 `feat/course-realistic-model`，不改写既有历史，也不直接推送 `main`。
- Reality Track Phase 1 工程验证已完成并推送当前 feature branch；仍需项目负责人执行下列人工 UAT。

### Reality Track Phase 1 人工 UAT

1. 在 Course 1.0 数据库依次执行 migration 与 realistic seed，再次执行一遍确认无报错、数量不翻倍。
2. 用 `C2400001 / 123456` 登录，在课程大厅搜索“数据库原理”，确认同一课程显示多个教学班。
3. 选择一个教学班，在“我的课程”和课表确认班号、教师、周次、节次、教室一致。
4. 尝试选择同一 `courseId` 的另一个教学班，确认服务端拒绝；退课后确认教学班人数和三个学生页面同步。
5. 用 `TCH00001 / 123456` 登录，确认“陈龙”能看到本人负责的教学班及各自学生名单。
6. 用 `ADMIN001 / 123456` 登录，按教学班新增两条不同周次排课；验证重叠周次的教师/教室冲突被拒绝，不重叠周次允许。
7. 从主界面 smoke User、Library、Student、Hospital、Shop 和 Course，完成后确认 8888 释放。

工程验证结论：Phase 1 已具备人工 UAT 条件；第 11 节边界仍然保留。完成下列 UAT 后再决定是否合并，
本阶段不进入 Phase 2。
