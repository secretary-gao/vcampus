-- ============================================================
-- 迁移脚本：给已存在的 tblGoods 表补上 imageUrl（商品图片地址）字段
-- 用法：mysql -u root -p vCampus < sql/migration_add_goods_image.sql
-- 适用场景：之前已执行过 sql/shop/vcampus_shop.sql 建好了 tblGoods，
--   现在只需要补这一个新字段，不用重新建表、不会丢已有数据。
-- 如果提示 "Duplicate column name 'imageUrl'"，说明已执行过，忽略即可。
-- ============================================================

USE vCampus;

ALTER TABLE tblGoods
    ADD COLUMN imageUrl VARCHAR(255) NULL COMMENT '商品图片地址（可空）' AFTER stock;

UPDATE tblGoods SET imageUrl = CASE goodsId
    WHEN 'G001' THEN 'Client/src/vcampus/client/view/assets/store/G001.jpg'
    WHEN 'G002' THEN 'Client/src/vcampus/client/view/assets/store/G002.jpg'
    WHEN 'G003' THEN 'Client/src/vcampus/client/view/assets/store/G003.jpg'
    WHEN 'G004' THEN 'Client/src/vcampus/client/view/assets/store/G004.jpg'
    WHEN 'G005' THEN 'Client/src/vcampus/client/view/assets/store/G005.jpg'
    WHEN 'G006' THEN 'Client/src/vcampus/client/view/assets/store/G006.jpg'
    WHEN 'G007' THEN 'Client/src/vcampus/client/view/assets/store/G007.jpg'
    ELSE imageUrl END
WHERE goodsId IN ('G001','G002','G003','G004','G005','G006','G007');

SELECT goodsId, goodsName, imageUrl FROM tblGoods ORDER BY goodsId;
