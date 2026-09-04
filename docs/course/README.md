# Course 模块开发与演示说明

Course 模块已完成课程查询、选课/退课、教务排课、学生课程表及 JavaFX 页面，
并接入当前 `origin/main` 的 User / Library / Student / Shop / Hospital 五模块集成架构。

## 1. 调用链与职责

```text
JavaFX CoursePanel
    -> ICourseClientSrv / CourseClientSrv
    -> Message + 短连接 Socket
    -> ServerThread
    -> CourseHandler (ModuleHandler)
    -> ICourseServerSrv / CourseServerSrv
    -> CourseDAO / SelectCourseDAO / CourseScheduleDAO
    -> MySQL vCampus
```

- JavaFX 只调用 `CourseClientSrv`，不访问 DAO 或数据库。
- `CourseHandler` 复用集成基线的 `ModuleHandler` 扩展点；`ServerThread` 只增加一行注册。
- DAO 只负责 SQL 与行映射；容量、重复选课、排课冲突等规则在 Service。
- 选课和退课分别把记录变化与 `selectedCount` 变化放入同一 JDBC transaction。

## 2. 文件地图

```text
Common/src/vcampus/common/vo/
  Course.java                 课程主数据
  SelectCourse.java           学生选课记录
  CourseSchedule.java         教室/星期/时间段

Server/src/vcampus/server/
  dao/CourseDAO.java
  dao/SelectCourseDAO.java
  dao/CourseScheduleDAO.java
  dao/CourseStudentDAO.java   登录用户到正式学号的只读映射
  srv/CourseServerSrv.java    业务规则与事务
  srv/CourseHandler.java      ModuleHandler Socket 适配器

Client/src/vcampus/client/
  biz/CourseClientSrv.java
  view/course/CoursePanel.java
  view/course/CourseHallPane.java
  view/course/SelectedCoursesPane.java
  view/course/TimetablePane.java
  view/course/ScheduleAdminPane.java
  view/course/CourseFrame.java

sql/course/
  vcampus_course.sql          正式 schema
  seed_course_demo.sql        可选演示数据
```

## 3. 数据库

按以下顺序初始化：

```powershell
Get-Content -Raw sql/vcampus_schema.sql | mysql -u root -p --default-character-set=utf8mb4
Get-Content -Raw sql/Student/BuildTbl.sql | mysql -u root -p --default-character-set=utf8mb4
Get-Content -Raw sql/course/vcampus_course.sql | mysql -u root -p --default-character-set=utf8mb4
Get-Content -Raw sql/seed_demo_data.sql | mysql -u root -p --default-character-set=utf8mb4
Get-Content -Raw sql/course/seed_course_demo.sql | mysql -u root -p --default-character-set=utf8mb4
```

三张业务表：

- `tblCourse`：课程主键、教师、学分、容量和已选人数，使用 `CHECK` 保证正数及容量边界。
- `tblSelectCourse`：选课记录；`UNIQUE(studentId, courseId)` 防止重复选课；分别外键关联
  正式 `tblStudent` 和 `tblCourse`。
- `tblCourseSchedule`：课程、教室、星期与起止时间；外键关联课程，`CHECK(startTime < endTime)`。
  教师不重复存储，始终从 `tblCourse.teacher` 获取。

教室和教师的“时间区间重叠”无法由普通唯一键表达，因此由 Service 在事务内检查。

## 4. Socket API

Course 消息名定义在 `IConstant`：

- `courseQuery`
- `courseSelect`
- `courseDrop`
- `courseSelectedQuery`
- `courseStudentIdQuery`
- `courseScheduleQuery`
- `courseScheduleAdd`
- `courseScheduleUpdate`
- `courseScheduleDelete`
- `studentTimetableQuery`

业务条件不满足返回 `400`，服务器/JDBC 异常返回 `500`，成功返回 `200`。

## 5. Java 21 编译与启动

仓库不使用 Maven。下面命令均在项目根目录执行。PowerShell 中先指定本机 JDK 21 和
JavaFX 21.0.12 Windows SDK：

```powershell
$jdk = 'C:\path\to\jdk-21'
$fx = 'C:\path\to\javafx-sdk-21.0.12'
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;" + $env:Path

javac -encoding UTF-8 `
  --module-path "$fx\lib" --add-modules javafx.controls,javafx.fxml `
  -d bin -cp "lib\mysql-connector-j-9.7.0.jar" `
  (Get-ChildItem -Recurse Common\src,Server\src,Client\src -Filter *.java).FullName
```

终端一启动统一服务器：

```powershell
& "$jdk\bin\java.exe" -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.Server
```

终端二可启动完整登录界面：

```powershell
& "$jdk\bin\java.exe" "-Djava.library.path=$fx\bin" `
  --module-path "$fx\lib" --add-modules javafx.controls,javafx.fxml `
  -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.client.view.LoginFrame
```

也可独立演示 Course 页面：

```powershell
# 管理员
& "$jdk\bin\java.exe" "-Djava.library.path=$fx\bin" `
  --module-path "$fx\lib" --add-modules javafx.controls,javafx.fxml `
  -cp "bin;lib\mysql-connector-j-9.7.0.jar" `
  vcampus.client.view.course.CourseFrame --role=管理员 --user=ADMIN001

# 学生（需要先执行 demo seed）
& "$jdk\bin\java.exe" "-Djava.library.path=$fx\bin" `
  --module-path "$fx\lib" --add-modules javafx.controls,javafx.fxml `
  -cp "bin;lib\mysql-connector-j-9.7.0.jar" `
  vcampus.client.view.course.CourseFrame --role=学生 --user=09010101
```

演示账号密码均为 `123456`。

## 6. 无界面测试

服务器端直接测试：

```powershell
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.dao.CourseDAOTest
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.dao.SelectCourseDAOTest
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.CourseServerSrvTest
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.dao.CourseScheduleDAOTest
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.CourseScheduleServerSrvTest
```

Socket 测试需要先启动统一 Server。测试数据由服务端夹具准备和清理：

```powershell
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.CourseSocketTestFixture setup
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.client.biz.CourseClientSrvTest
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.client.biz.CourseScheduleClientSrvTest
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.CourseSocketTestFixture cleanup
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.CourseSocketTestFixture residue
```

## 7. 演示流程

学生：使用 `09010101 / 123456` 登录，进入“教务”，查询并选择课程；在“我的课程”
查看或退课，在“我的课程表”查看所选课程的教师、教室与时间。

管理员：使用 `ADMIN001 / 123456` 登录，进入“教务”的“排课管理”，新增排课；再尝试
为同一教师或同一教室添加重叠时段，界面会展示 Service 返回的冲突消息；随后可修改、删除。

## 8. 已知边界

- 当前协议是一次请求一个短连接，没有服务端 session；管理员可见性由登录用户角色控制，
  但协议层尚无 token/权限中间件。这是现有项目公共认证架构的边界。
- 排课模型按“星期 + 时间段”表达每周课表，尚未建学期、教学周、单双周和临时调课模型。
- `tblCourse.teacher` 是教师名称，不是教师账号外键；因此冲突按教师名称判断。
- JavaFX 运行时必须使用带 Windows native DLL 的完整 SDK。仅复制 JavaFX JAR 可以编译，
  但会在启动时报 `no suitable pipeline found`。
