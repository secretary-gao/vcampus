-- ======================================================
-- Vcampus 虚拟校园系统 - 医院模块初始化脚本
-- 表：医生表 tblDoctor、挂号预约表 tblAppointment
-- 依赖：必须先执行 vcampus_schema.sql 创建 tblUser
-- 【注意】当前已有数据库不要执行本脚本，会重建表丢失数据！
-- 新环境部署用法：mysql -u root -p vCampus < sql/vcampus_hospital.sql
-- ======================================================
USE vCampus;

-- 医生信息表 tblDoctor
CREATE TABLE IF NOT EXISTS tblDoctor (
    doctorId CHAR(8) PRIMARY KEY COMMENT '医生编号，定长8位',
    name VARCHAR(20) NOT NULL COMMENT '医生姓名',
    department VARCHAR(30) COMMENT '所属科室',
    title VARCHAR(20) COMMENT '职称'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生信息表';

-- 挂号预约记录表 tblAppointment
CREATE TABLE IF NOT EXISTS tblAppointment (
    appointmentId CHAR(12) PRIMARY KEY COMMENT '挂号记录编号，定长12位',
    userId CHAR(8) NOT NULL COMMENT '就诊用户编号，关联tblUser.uId',
    doctorId CHAR(8) NOT NULL COMMENT '预约医生编号，关联tblDoctor.doctorId',
    appointmentTime DATETIME NOT NULL COMMENT '预约就诊时间',
    status ENUM('待就诊','已就诊','已取消') DEFAULT '待就诊' COMMENT '预约状态',
    FOREIGN KEY (userId) REFERENCES tblUser(uId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    FOREIGN KEY (doctorId) REFERENCES tblDoctor(doctorId)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挂号预约记录表';

-- 插入测试样例医生数据，用于DAO测试
INSERT IGNORE INTO tblDoctor(doctorId, name, department, title) VALUES
('D0000001','张医生','内科','主任医师'),
('D0000002','李医生','外科','副主任医师'),
('D0000003','王医生','皮肤科','主治医师'),
('D0000004','赵芳','儿科','主任医师'),
('D0000005','陈明','骨科','副主任医师'),
('D0000006','刘梅','眼科','主治医师'),
('D0000007','周建国','耳鼻喉科','住院医师'),
('D0000008','吴海燕','妇产科','主任医师'),
('D0000009','孙强','急诊科','副主任医师'),
('D0000010','郑晓云','口腔科','主治医师'),
('D0000011','马伟','神经内科','主任医师'),
('D0000012','何丽','消化内科','住院医师'),
('D0000013','黄涛','心血管内科','副主任医师'),
('D0000014','林小燕','呼吸内科','主治医师'),
('D0000015','高建军','泌尿外科','主任医师');
