# Course Reality Track 最终交付说明

本文面向项目组长、验收教师和后续维护者。Course 1.0 已进入 `main`；
`feat/course-realistic-model` 在其基础上完成真实教学班模型、高保真三角色界面、选课规则、
自动排课、教务统计与工程验证。本 Sprint 完成后，Course 功能冻结，只接受缺陷和集成修复。

## 1. 最终功能

### 学生

- 按关键字和课程性质筛选课程，展开一门课程下的多个教学班。
- 查看教师、容量、已选人数、教室、周次、星期和节次。
- 选择具体教学班，查看“我的课程”和响应式周课表，并执行退课。
- 页面提前说明已选、同课程其他班、满员、时间冲突和培养方案组互斥原因。
- 服务端再次执行全部规则，绕过 JavaFX 直接调用 Socket 也无法违反约束。

### 教师

- 按登录用户姓名查看本人负责的教学班，不混入同一课程的其他教师班级。
- 查看每班容量、已选人数、利用率、排课和学生名单。
- 按学号或姓名搜索当前教学班学生，显示匹配人数；筛选保留表头排序，刷新保留教学班与搜索条件。
- 在名单中选中学生后点击“查看学籍”（也支持双击或 Enter），只读查看该学生的最新学籍；选退课后刷新名单。关联规则和验证方法见 [教师查看学生学籍](../student/README.md)。
- 导出当前教学班名单 CSV，包含学号、姓名、班级、专业和选课时间。

### 管理员

- 查看教务 Dashboard、课程、教学班和排课。
- 维护 Course、TeachingClass 和多段 CourseSchedule。
- 手工排课时检查教师、教室及教学班自身时间冲突。
- 使用 CSP 自动排课生成 Preview；确认后才以单事务写入，取消 Preview 不修改数据库。
- 导出全部教学班选课统计 CSV。

## 2. 领域模型

```text
Course 1 ───── N TeachingClass 1 ───── N CourseSchedule
   │                    │
   N                    N
   │                    │
CourseRequirement       SelectCourse
GroupMember             │
   │                    1
   1                 Student
CourseRequirementGroup
       rule = CHOOSE_ONE
```

- `Course` 表达课程本体：课程号、名称、学分、课程性质、开课单位。
- `TeachingClass` 表达一次具体开班：班号、教师、容量、已选人数、教学语言。
- `CourseSchedule` 表达教学班的一段上课安排；一个教学班可以有多段周次/节次。
- `SelectCourse` 指向教学班，同时保留 `courseId`，由唯一约束保证同一学生对同一 Course 只能选择一个班。
- `CourseRequirementGroup` 将明确等价的中文/全英文课程组成 `CHOOSE_ONE` 组。

`tblCourse.teacher/capacity/selectedCount` 只作为 Course 1.0 兼容投影；运行时容量与人数以
`tblTeachingClass` 为准。截图未提供的教室保留为“待补充”，未猜测课程事实。

## 3. 架构与调用链

```text
JavaFX CoursePanel
  ├─ 学生：CourseHall / SelectedCourses / Timetable
  ├─ 教师：TeacherCourses
  └─ 管理员：Dashboard / CourseAdmin / ScheduleAdmin / AutoSchedule
             │
             ▼
ICourseClientSrv / CourseClientSrv
             │  Message + short-lived Socket
             ▼
ServerThread ──► CourseHandler ──► CourseServerSrv
                                      │
                                      ▼
 CourseDAO / TeachingClassDAO / CourseScheduleDAO / SelectCourseDAO
 CourseRequirementGroupDAO / CourseDashboardDAO / CourseStudentDAO
 TeacherCourseEnrollmentDAO
                                      │
                                      ▼
                                MySQL vCampus
```

JavaFX 使用后台 `Task` 发起网络请求，在 FX 线程更新控件。Client 每次请求建立短连接，
连接超时为 5 秒，读取超时为 10 秒。Handler 只做协议解析和响应封装；Service 负责业务规则与事务；
DAO 负责参数化 SQL 和对象映射。

选课数据流：

```text
点击“选择”
  -> CourseClientSrv.selectCourse(studentId, teachingClassId)
  -> CourseHandler
  -> CourseServerSrv.selectCourse
  -> 锁学生与教学班
  -> 检查同 Course、CHOOSE_ONE、时间冲突、容量
  -> INSERT SelectCourse
  -> TeachingClass.selectedCount + 1
  -> 刷新 Course 兼容投影
  -> COMMIT
  -> 大厅 / 我的课程 / 课表各刷新一次
```

任一步失败均 `ROLLBACK`，因此记录与人数不会出现半成功。

## 4. 主要文件

| 位置 | 职责 |
|---|---|
| `Common/src/vcampus/common/vo/` | Course、TeachingClass、Schedule、规则组、自动排课方案和统计 DTO |
| `Common/src/vcampus/common/constant/IConstant.java` | Course Socket 消息常量 |
| `Server/src/vcampus/server/srv/CourseServerSrv.java` | 选退课事务、管理业务、排课冲突、Preview 与 Apply |
| `Server/src/vcampus/server/srv/CourseAutoScheduler.java` | 独立、可测试的 CSP 回溯求解器 |
| `Server/src/vcampus/server/srv/CourseHandler.java` | Course 请求分发与错误码转换 |
| `Server/src/vcampus/server/dao/` | Course 各实体、规则组、名单和统计查询 |
| `Client/src/vcampus/client/biz/CourseClientSrv.java` | 带 connect/read timeout 的短连接客户端 |
| `Client/src/vcampus/client/view/course/` | 三角色页面、Dashboard、自动排课和 CSV 导出 |
| `sql/course/` | schema、兼容 migration、Reality seed 和规则组 seed |
| `build.bat`、`run-*.bat`、`.vscode/launch.json` | JDK 21 / JavaFX 构建和正式启动入口 |

## 5. 数据库设计与初始化

使用 MySQL 8.0、InnoDB、`utf8mb4`。先按 `Server/db.properties.example` 创建未纳入 Git 的
`Server/db.properties`。

| 表 | 关键规则 |
|---|---|
| `tblCourse` | 课程主键；学分、课程性质和开课单位 |
| `tblTeachingClass` | 课程外键；同课程班号唯一；`0 <= selectedCount <= capacity` |
| `tblCourseSchedule` | 教学班复合外键；周次 1–30、星期 1–7、节次 1–13；时段唯一 |
| `tblSelectCourse` | 学生/教学班外键；学生+教学班、学生+课程均唯一 |
| `tblCourseRequirementGroup` | 规则组定义，当前支持 `CHOOSE_ONE` |
| `tblCourseRequirementGroupMember` | 规则组与课程的关联；组合主键避免同一课程在同一组内重复 |

不要在 PowerShell 5.1 中使用 `Get-Content ... | mysql` 导入中文 SQL。该管道会经历文件解码和
原生程序管道编码转换，可能把 UTF-8 中文写成 `???`。从仓库根目录进入 MySQL：

```powershell
mysql -u root -p --default-character-set=utf8mb4
```

在 **MySQL 提示符内**按顺序执行：

```sql
SOURCE sql/vcampus_schema.sql;
SOURCE sql/Student/BuildTbl.sql;
SOURCE sql/hospital/vcampus_hospital.sql;
SOURCE sql/shop/vcampus_shop.sql;
SOURCE sql/course/vcampus_course.sql;
SOURCE sql/course/migration_course_teaching_class.sql;
SOURCE sql/course/migration_course_requirement_group.sql;
SOURCE sql/seed_demo_data.sql;
SOURCE sql/course/seed_course_demo.sql;
SOURCE sql/course/seed_cs2024_fall_realistic.sql;
SOURCE sql/course/seed_course_requirement_groups.sql;

USE vCampus;
SELECT courseId, courseName, HEX(courseName) FROM tblCourse ORDER BY courseId;
EXIT;
```

这些 migration/seed 可重复执行，不会重置数据库。Reality seed 只重建明确标识的模拟学生选课组合，
用于保证其课程无冲突；不清空其他模块或普通业务数据。

Reality Track 数据为 27 门课程、40 个教学班、62 条排课、150 名模拟学生、1050 条选课。
150 名学生每人 7 门课，共 15 种不同组合；全量扫描结果为：

```text
students_with_schedule_conflict = 0
conflicting_selection_pairs      = 0
oversold                         = 0
selectedCount_drift              = 0
duplicate_student_course         = 0
orphan                           = 0
requirement_group_violations     = 0
```

## 6. 选课与排课规则

学生选课事务按以下顺序执行：锁定学生、锁定教学班、检查同 Course 重复、检查 `CHOOSE_ONE`、
检查课表冲突、检查容量、插入选课、递增人数、刷新兼容投影、提交。

两个 Schedule 冲突，当且仅当星期相同，且周次区间和节次区间都相交：

```text
max(a.weekStart, b.weekStart) <= min(a.weekEnd, b.weekEnd)
max(a.startPeriod, b.startPeriod) <= min(a.endPeriod, b.endPeriod)
```

候选教学班的所有 Schedule 都会与学生已选教学班的所有 Schedule 比较。相邻但不重叠的节次、
相同节次但不重叠的周次均允许。管理员排课用同一重叠定义检查教师、教室和教学班自身冲突。

## 7. 自动排课

自动排课将每个未排课 `TeachingClass` 视为变量，候选域来自 Reality 数据中已有的
`时间模式 × 教室`。硬约束包括教师冲突、教室冲突、教学班自身冲突，并同时考虑周次和节次。

```text
读取目标教学班与已有 Schedule
  -> 构造候选域
  -> MRV：优先选择剩余候选最少的教学班
  -> 尝试候选并做增量冲突检查
  -> 失败则回溯
  -> 生成 Preview + 搜索统计
  -> 管理员确认
  -> 单事务重新校验并批量写入
```

最坏复杂度为 `O(D^N)`（N 个教学班，每班 D 个候选），MRV 和增量约束检查会显著缩小实际搜索。
Preview 不写数据库；Apply 中任意一条失败会回滚全部 Schedule，并拒绝已过期的 Preview。

## 8. Dashboard 与导出

Dashboard 的全部指标实时来自 SQL：课程数、教学班数、选课学生数、选课记录数、平均容量、
平均已选人数、满员班级数、总容量利用率，以及热门教学班 Top 5、剩余名额 Top 5。

CSV 使用 UTF-8 BOM，便于中文 Windows Excel 直接打开。教师导出当前班名单；管理员导出全部教学班
的课程、教师、容量、人数、余量和利用率。字段会按 CSV 规则转义，测试后不保留临时文件。

## 9. Socket API

| 消息 | 请求 data | 成功响应 data |
|---|---|---|
| `courseQuery` | 关键字 | `List<Course>` |
| `teachingClassQuery` | 关键字 | `List<TeachingClass>` |
| `courseSelect` / `courseDrop` | `{studentId, teachingClassId}` | 成功提示 |
| `courseSelectedQuery` | studentId | `List<SelectCourse>` |
| `studentTimetableQuery` | studentId | `List<CourseSchedule>` |
| `teacherCourseEnrollmentsQuery` | 教师姓名 | `List<TeacherCourseEnrollment>` |
| `courseRequirementGroupQuery` | 无 | `List<CourseRequirementGroup>` |
| `courseDashboardQuery` | 无 | `CourseDashboardStats` |
| `courseAutoSchedulePreview` | `AutoScheduleRequest` | `AutoSchedulePlan` |
| `courseAutoScheduleApply` | `AutoSchedulePlan` | 写入条数 |
| `course*` / `teachingClass*` / `courseSchedule*` | 对应 DTO 或 ID | 对象、列表或 `true` |

成功为 `200`，业务规则和参数错误为 `400`，JDBC/IO 故障为 `500`。

## 10. Java 21 / JavaFX 构建与启动

要求 JDK 21、JavaFX 21.0.12 Windows SDK、MySQL Connector/J 9.7.0。团队统一使用嵌套布局：

```text
lib/
  mysql-connector-j-9.7.0.jar
  javafx/
    lib/   # JavaFX JAR
    bin/   # 与 JAR 同版本的 Windows native DLL
```

`lib/javafx` 被 Git 忽略，本地二进制不提交。正式入口均使用 `lib\javafx\lib`：

```powershell
.\build.bat
.\run-server.bat
.\run-client.bat
# 或单独使用 .\start-all.bat
```

`build.bat` 编译 Common、Server、Client，并复制 Hospital 的 `seu_logo.jpeg` 到
`bin/vcampus/client/view/`。正式 LoginFrame 只依赖 `bin`、Connector/J 和本地 JavaFX SDK 运行。

## 11. 演示账号与五分钟 UAT

| 身份 | 登录 ID | 初始密码 | 说明 |
|---|---|---|---|
| 模拟学生 | `C2400001` | `123456` | 映射学号 `2024000001` |
| 模拟教师 | `TCH00001` | `123456` | 姓名“陈龙”，匹配教学班教师字段 |
| 管理员 | `ADMIN001` | `123456` | Dashboard 与全部管理页 |

建议只做以下人工检查：

1. 学生展开一门多教学班课程，确认状态文案；选退一门课并查看三页同步。
2. 教师打开一个教学班名单，确认视觉与 CSV 保存对话框。
3. 管理员查看 Dashboard，生成一次自动排课 Preview 后取消，确认方案可读。
4. 将窗口在常用桌面尺寸间缩放，检查课程大厅、课表、Dashboard 和 Preview 的视觉手感。

## 12. 自动化测试与实际验证

JDK 21 clean build、Course DAO/Service/transaction、排课冲突、学生时间冲突、规则组、CSP、
Dashboard SQL、CSV、Socket E2E、JavaFX programmatic smoke 和并发 benchmark 均已实际执行。

关键测试入口：

```text
CourseDAOTest / SelectCourseDAOTest / TeachingClassDAOTest / CourseScheduleDAOTest
CourseServerSrvTest / TeachingClassServerSrvTest / CourseScheduleServerSrvTest
StudentScheduleConflictServerSrvTest / CourseRequirementGroupServerSrvTest
CourseAutoSchedulerTest / CourseAutoScheduleServerSrvTest
CourseDashboardDAOTest / CourseCsvExporterTest
CourseClientSrvTest / CourseScheduleClientSrvTest
CourseConcurrentEnrollmentBenchmark
```

并发基准使用 100 名隔离测试学生竞争容量 10 的教学班：

```text
clients=100
success=10
rejected=90
capacity_remaining=0
oversold=0
selectedCount=10
residue=0
```

Socket E2E 覆盖规则组、Dashboard、自动排课 Preview，并验证 Preview 前后排课数不变。
无响应服务端故障注入在约 10 秒触发 `SocketTimeoutException`。JavaFX smoke 通过真实统一 Server 构造并
显示学生 3 个 Tab、教师工作台和管理员 4 个 Tab，CSS 加载及布局成功。

六模块 smoke 仅证明 User、Library、Student、Hospital、Shop、Course 可共同编译并由统一 Server 路由，
不代表其他五个模块的全部业务完成了 Course 开发者人工 UAT。

## 13. 本 PR 修复的关键问题

- 将单层 Course 重构为 Course → TeachingClass → 多段 Schedule，兼容旧数据 migration。
- 服务端补齐学生课表冲突、同 Course 跨班互斥、容量与事务一致性。
- Reality seed 改为无时间冲突的多组合数据。
- Hospital 图片按 classpath 复制/加载并在缺图时降级。
- Windows 中文 SQL 初始化改为 MySQL `SOURCE`。
- 排课测试与 demo/业务数据隔离，测试前后已有数据不变。
- 退课后统一刷新大厅、我的课程和课表，避免漏刷或重复请求。
- 课表在常用桌面尺寸下响应式填充，避免多余水平滚动。
- JavaFX module-path 与团队 `lib/javafx/lib` 布局统一。
- 新增 `CHOOSE_ONE` 课程组、自动排课、Dashboard、CSV 和并发基准。
- Course 客户端增加局部 connect/read timeout，不影响其他模块 Socket 实现。

## 14. 已知架构边界

- 公共 Socket 协议没有可信 server session/token；UI 角色分流不能替代服务端身份认证。本分支未重构六模块认证体系。
- 高并发排课仍可能出现 MySQL `1213` deadlock；当前完整回滚并返回错误，没有自动事务重试。
- 排课尚未独立建模学期、单双周、离散教学周和临时调课，周次只支持连续区间。
- 教师身份仍按用户姓名匹配 `TeachingClass.teacher`，尚无教师账号外键。
- 自动排课候选域来自现有时间模式和教室集合，未建立完整教室资源表。
- 培养方案组当前只实现 `CHOOSE_ONE`，只录入能从现有课程名称明确判断的中文/全英文等价课程。

## 15. Git / PR 状态

最终功能分支为 `feat/course-realistic-model`。各阶段按语义提交保存，不 rebase、不 force、
不直接修改或推送 `main`。最终以本分支最新 HEAD 和 `origin/feat/course-realistic-model` 一致为交付条件。

PR 结论：完成最终门禁并推送后，可交给组长 review / merge。
# Course Final Sprint: 成绩流程

执行 `sql/course/migration_course_score.sql` 创建 `tblCourseScore`，再执行
`sql/course/seed_course_score_demo.sql` 加载王老师的三个演示教学班及成绩样本。

成绩通过 Socket 进入服务端：教师提交后均为 `PENDING`；管理员审核为
`APPROVED` 后，学生端查询才会返回该成绩。教师提交时服务端同时校验教学班
归属与学生实际选课记录。
