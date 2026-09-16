-- 药品表
CREATE TABLE tblMedicine(
    medicineId VARCHAR(32) PRIMARY KEY COMMENT '药品编号',
    medicineName VARCHAR(100) NOT NULL COMMENT '药品名称',
    price DECIMAL(10,2) COMMENT '模拟单价'
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品表';


INSERT IGNORE INTO tblMedicine(medicineId,medicineName,price) VALUES
('M001','布洛芬缓释胶囊',25.50),
('M002','连花清瘟胶囊',18.80),
('M003','阿莫西林胶囊',12.30),
('M004','复方甘草片',8.50),
('M005','氯雷他定片',22.00),
('M006','健胃消食片',10.20),
('M007','维生素C片',6.80),
('M008','蒙脱石散',14.60);


CREATE TABLE tblPrescription(
    presId VARCHAR(32) PRIMARY KEY COMMENT '处方主键ID',
    appointId VARCHAR(32) NOT NULL COMMENT '关联预约记录ID',
    doctorId VARCHAR(32) NOT NULL COMMENT '开药医生ID',
    userId VARCHAR(32) NOT NULL COMMENT '就诊用户ID',
    medicineId VARCHAR(32) NOT NULL COMMENT '药品ID',
    medicineName VARCHAR(100) NOT NULL COMMENT '药品名称(冗余)',
    medicineNum INT NOT NULL DEFAULT 1 COMMENT '开药数量',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '开处方时间'
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生处方开药表';
