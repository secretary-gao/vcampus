mysql后首先要修改编码，否则无法插入中文
SET NAMES utf8mb4;

建表/插入方式是用命令行执行类似格式的指令
mysql> source D:\School_Info_System\vcampus\sql\Library\BuildTbl.sql
mysql> source D:\School_Info_System\vcampus\sql\Library\InitTbl.sql

执行完后需切换回编码页，否则命令行会显示不正常
SET NAMES gbk;

执行下面指令以察看建表结果
mysql> SELECT * FROM tblBook;

执行\Server\src\vcampus\server\dao\TestBookBorrowDAO.java以测试DAO，
通过注释掉Test中的删除操作，然后在mysql中执行SELECT * FROM tblBook;确实看到了Test新增出的图书，符合要求