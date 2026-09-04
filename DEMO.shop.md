# Vcampus 商店模块 —— 演示流程

给老师验收商店链路（商品查询 + 购买事务 + 库存扣减 + 购买记录 + 管理员商品管理）时，照这份清单走。

> 对应五步走的第 4 步（JavaFX 界面 + 主菜单点进模块）与第 5 步联调成果。
> 商店模块分支：`dev/shop`，负责人：孙志平。

---

## 一、演示前准备（提前 5 分钟做好）

1. 确认 **MySQL 服务在跑**，且已执行过以下三个脚本（任选一个终端，`cd` 到项目根目录后执行；`-p` 后回车输入你的 MySQL 密码）：
   ```bat
   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 < sql\vcampus_schema.sql
   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 < sql\seed_demo_data.sql
   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p --default-character-set=utf8mb4 < sql\shop\vcampus_shop.sql
   ```
2. 确认 **`Server\db.properties`** 已配置好本地 MySQL 密码（`jdbc.password`），且 URL 带 `allowPublicKeyRetrieval=true`。
3. 打开两个 PowerShell 终端，都 `cd` 到项目根目录：
   ```powershell
   cd C:\Users\22548\Desktop\vcampus
   ```
4. 编译一次全部代码（任选一个终端跑即可，看到**没有红字报错**就算成功）：
   ```powershell
   javac -encoding UTF-8 --module-path "lib\javafx\lib" --add-modules javafx.controls,javafx.fxml -d bin -cp "lib\mysql-connector-j-9.7.0.jar" (Get-ChildItem -Recurse -Path Common\src,Server\src,Client\src -Filter *.java).FullName
   ```
   > 若出现"注：使用了未经检查或不安全的操作"，那是泛型强转的**警告，不是错误**，可忽略。

---

## 二、启动服务器（终端①）

```powershell
java -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.server.srv.Server
```

预期输出（这个终端会一直挂住不退出，这是正常的，它在循环监听）：
```
Vcampus 服务器启动中...
服务器已启动，正在监听端口：8888
```

**给老师讲一句**："服务器主线程在 8888 端口循环等待客户端连接，每来一个新连接就分配一个独立线程去处理（一个客户端一个线程），商店模块的请求按消息名路由到对应的模块处理器。"

---

## 三、启动客户端（终端②）

```powershell
java --module-path "lib\javafx\lib" --add-modules javafx.controls,javafx.fxml -cp "bin;lib\mysql-connector-j-9.7.0.jar" vcampus.client.view.LoginFrame
```

会弹出"东南大学 Vcampus 身份认证中心"登录窗口（账号 / 密码 / 角色下拉框 + 登录 / 注册按钮）。

---

## 四、演示脚本（按顺序做，每步讲一句）

### A. 学生视角（商品查询 + 购买 + 记录）

| 步骤 | 操作 | 预期结果 | 讲给老师听 |
|---|---|---|---|
| 1 | 登录ID填 `09010101`，密码 `123456`（角色选"学生"），点【登录】 | 弹窗："登录成功，欢迎 09010101！" | "客户端用 MD5 把密码摘要后封装进 Message 走 Socket 发给服务器，服务器查库比对密码摘要，一致才放行。" |
| 2 | 在主界面功能分区点【商店】卡片上的【进入】 | 弹出"虚拟商店"窗口（商品表 + 购买栏 + 记录页签） | "商店是客户端的一个独立 JavaFX 窗口，业务请求全部经过 IStoreClientSrv → Socket → 服务器，客户端不直接连数据库。" |
| 3 | 在商店搜索框输入 `饮料`，点【查询】 | 表格只剩农夫山泉、可口可乐 | "服务器按关键字做名称/类别模糊查询，把结果列表序列化后经 Socket 返回。" |
| 4 | 清空关键字点【刷新】恢复全部 → 点选中"农夫山泉(G001)" → 购买数量填 `2` → 点【购买】 | 弹窗"购买成功：订单号 ORDER… / 总价 4.00 元"；再回看表格，G001 库存从 **100 → 98** | "服务器开启事务：先 SELECT…FOR UPDATE 锁住商品行防止并发超卖 → 校验库存 → 插入订单 + 原子扣减库存 → 提交。库存变化是这次购买的硬证据。" |
| 5 | 点【我的购买记录】页签 | 表格出现刚那笔订单（订单号/商品/数量/总价/下单时间） | "服务器按购买人查询 tblPurchase，只返回本人记录。" |
| 6 | 切到终端①看服务器日志 | 能看到 `收到请求：…name='shopPurchase'…` 和 `已返回响应：…statusCode='200'…` | "服务器确实收到、处理并回复了，不是客户端自己伪造的弹窗。" |

### B. 管理员视角（商品管理）

| 步骤 | 操作 | 预期结果 | 讲给老师听 |
|---|---|---|---|
| 7 | 关掉商店/主界面，重新用 `admin` / `123456`（角色选"管理员"）登录，点【商店】【进入】 | 商店窗口**多出"商品管理"页签** | "管理员表单是否可见由服务器返回的用户角色决定；即使前端隐藏按钮，真正的新增/删除仍由服务器端校验角色，防止伪造报文。" |
| 8 | 在【商品管理】填 编号 `ST001`、名称 `零食礼包`、类别 `食品`、单价 `20`、库存 `30`，点【新增】 | 提示成功，列表刷新出现 ST001 | "服务器校验编号唯一性后插入 tblGoods。" |
| 9 | 选中刚加的 ST001 → 改库存为 `50` → 点【修改】 | 提示成功，列表库存变 50 | "编号不可修改，其余字段可改。" |
| 10 | 选一个**还没卖过**的商品（如 G007）点【删除】→ 确认 | 删除成功 | "没有购买记录的商品可删。" |
| 11 | 选**卖过的**商品（如 G001，刚刚学生买过）点【删除】→ 确认 | 弹窗/"该商品已有购买记录，无法删除" | "这是防止历史订单丢失引用完整性的硬规则，即使在数据库层也做了防护。" |

> 加分项：保持终端②窗口不关，再开一个终端③跑同样的客户端命令，弹第二个登录窗口，两个窗口分别用学生/管理员登录，互不影响——体现"一个客户端一个线程"的多线程并发。

---

## 五、可能被问到的问题 + 怎么答

- **购买怎么保证数据一致性？** 服务器用 JDBC 事务：`setAutoCommit(false)` → `SELECT…FOR UPDATE` 锁行 → 校验库存 → 插入订单 + 扣减库存 → 成功 `commit`、异常 `rollback`，两步要么都成要么都回滚。
- **怎么防止超卖/库存减成负数？** 事务内先 `SELECT…FOR UPDATE` 加行锁，且扣库存用 `UPDATE … SET stock = stock + ? WHERE goodsId = ? AND stock + ? >= 0`，双保险。
- **订单号谁生成？** 服务器端生成 `ORDER + 时间戳 + 随机数`，客户端不参与，保证全局唯一。
- **管理员权限怎么保证？** 前端只是"看不到按钮"，真正的新增/修改/删除都在服务器端按用户角色校验；并且"有购买记录的商品禁止删除"是在数据库层强制执行的业务规则。
- **客户端为什么不直接连数据库？** C/S 架构下所有数据请求都经 Socket 转发到服务器，避免数据库账号/ SQL 暴露在客户端，职责也更清晰。
- **代码是怎么分层的？** 每模块按 view（JavaFX 界面）/ biz 或 srv（业务服务）/ vo（实体类）/ dao（数据访问）分包；商店模块的请求处理通过 `StoreModuleHandler` 接入服务器的 `ModuleHandler` 路由，新增模块不用再改 `ServerThread`。

---

## 六、演示完怎么收尾

1. 关闭客户端窗口，两个终端 `Ctrl+C` 停止（服务器终端手动关闭即可）。
2. 若要**恢复初始演示数据**，重新执行第一节的三个 SQL 脚本即可（幂等，可重复跑）。
3. 如果想把演示产生的订单清理掉，可执行：
   ```sql
   USE vCampus;
   DELETE FROM tblPurchase;
   UPDATE tblGoods SET stock = 300 WHERE goodsId = 'G005';  -- 若演示中买过 G005
   ```

---

> **命名说明**：为避免与用户模块的根目录 `DEMO.md` 合并冲突，本模块的演示文档命名为 `DEMO.shop.md`。建议各模块统一采用 `DEMO.<模块名>.md`（如 `DEMO.user.md`、`DEMO.shop.md`），使各模块文档互不冲突；集成到 `main` 时可再汇成一份总演示文档。
