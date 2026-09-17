USE vcampus;
-- 药品表增加库存字段
ALTER TABLE tblMedicine ADD COLUMN stock INT NOT NULL DEFAULT 100 COMMENT '药品库存数量';

-- 给8种模拟药品初始化库存
UPDATE tblMedicine SET stock=100 WHERE medicineId='M001';
UPDATE tblMedicine SET stock=80 WHERE medicineId='M002';
UPDATE tblMedicine SET stock=60 WHERE medicineId='M003';
UPDATE tblMedicine SET stock=120 WHERE medicineId='M004';
UPDATE tblMedicine SET stock=70 WHERE medicineId='M005';
UPDATE tblMedicine SET stock=90 WHERE medicineId='M006';
UPDATE tblMedicine SET stock=150 WHERE medicineId='M007';
UPDATE tblMedicine SET stock=55 WHERE medicineId='M008';

-- 处方表增加是否已经取药标记 isTake 0未取药，1已取药
ALTER TABLE tblPrescription ADD COLUMN isTake TINYINT NOT NULL DEFAULT 0 COMMENT '0未取药，1已取药';
