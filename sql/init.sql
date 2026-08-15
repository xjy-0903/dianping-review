CREATE DATABASE IF NOT EXISTS dianping DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dianping;

DROP TABLE IF EXISTS tb_user;
CREATE TABLE tb_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    phone       VARCHAR(11)     NOT NULL COMMENT '手机号',
    password    VARCHAR(64)     NOT NULL COMMENT '密码(MD5)',
    nick_name   VARCHAR(32)     DEFAULT NULL COMMENT '昵称',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE = InnoDB COMMENT '用户表';

DROP TABLE IF EXISTS tb_shop;
CREATE TABLE tb_shop (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(64)     NOT NULL COMMENT '商户名称',
    type_id     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '类型id(1美食 2饮品 3快餐)',
    images      VARCHAR(1024)   DEFAULT NULL COMMENT '图片',
    address     VARCHAR(128)    DEFAULT NULL COMMENT '地址',
    x           DECIMAL(10, 6)  NOT NULL COMMENT '经度',
    y           DECIMAL(10, 6)  NOT NULL COMMENT '纬度',
    avg_price   BIGINT UNSIGNED DEFAULT 0 COMMENT '人均价格(分)',
    sold        INT UNSIGNED    DEFAULT 0 COMMENT '销量',
    comments    INT UNSIGNED    DEFAULT 0 COMMENT '评论数',
    score       DECIMAL(2, 1)   DEFAULT 5.0 COMMENT '评分',
    open_hours  VARCHAR(64)     DEFAULT NULL COMMENT '营业时间',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT '商户表';

DROP TABLE IF EXISTS tb_voucher;
CREATE TABLE tb_voucher (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    shop_id      BIGINT UNSIGNED NOT NULL COMMENT '商户id',
    title        VARCHAR(64)     NOT NULL COMMENT '标题',
    sub_title    VARCHAR(256)    DEFAULT NULL COMMENT '副标题',
    rules        VARCHAR(1024)   DEFAULT NULL COMMENT '使用规则',
    pay_value    BIGINT UNSIGNED NOT NULL COMMENT '支付金额(分)',
    actual_value BIGINT UNSIGNED NOT NULL COMMENT '抵扣金额(分)',
    type         TINYINT         NOT NULL DEFAULT 0 COMMENT '0-普通券 1-秒杀券',
    stock        INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '库存',
    begin_time   DATETIME        DEFAULT NULL COMMENT '生效时间',
    end_time     DATETIME        DEFAULT NULL COMMENT '失效时间',
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_shop (shop_id)
) ENGINE = InnoDB COMMENT '优惠券表';

DROP TABLE IF EXISTS tb_voucher_order;
CREATE TABLE tb_voucher_order (
    id          BIGINT          NOT NULL COMMENT '主键(雪花id,由Redis生成)',
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户id',
    voucher_id  BIGINT UNSIGNED NOT NULL COMMENT '优惠券id',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0-未支付 1-已支付 2-已核销 3-已取消',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    pay_time    DATETIME        DEFAULT NULL COMMENT '支付时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_voucher (user_id, voucher_id) COMMENT '一人一单数据库层兜底'
) ENGINE = InnoDB COMMENT '优惠券订单表';

-- 演示用户(密码均为 123456 的 MD5)
INSERT INTO tb_user (phone, password, nick_name) VALUES
('13800138000', 'e10adc3949ba59abbe56e057f20f883e', '张三'),
('13800138001', 'e10adc3949ba59abbe56e057f20f883e', '李四'),
('13800138002', 'e10adc3949ba59abbe56e057f20f883e', '王五');

-- 北京地区商户(经纬度用于 Redis GEO 检索演示)
INSERT INTO tb_shop (name, type_id, images, address, x, y, avg_price, sold, comments, score, open_hours) VALUES
('全聚德烤鸭(前门店)', 1, '', '前门大街30号',        116.399300, 39.898200, 16800, 12000, 2300, 4.6, '11:00-21:00'),
('海底捞火锅(西单店)', 1, '', '西单北大街131号',      116.374500, 39.913900, 12800,  8500, 1900, 4.8, '10:00-24:00'),
('瑞幸咖啡(国贸店)',   2, '', '建国门外大街1号',      116.460600, 39.909300,  1900, 32000, 4100, 4.4, '07:00-22:00'),
('喜茶(三里屯店)',     2, '', '三里屯路19号',        116.455100, 39.937200,  2800, 15000, 2600, 4.5, '10:00-22:00'),
('麦当劳(王府井店)',   3, '', '王府井大街255号',      116.411200, 39.914700,  3500, 26000, 3300, 4.2, '00:00-24:00'),
('星巴克(中关村店)',   2, '', '中关村大街15号',       116.315700, 39.983600,  4200,  9800, 1200, 4.3, '07:00-23:00'),
('胡大饭馆(簋街店)',   1, '', '东直门内大街233号',    116.420200, 39.938700,  9800, 21000, 3700, 4.7, '11:00-02:00'),
('庆丰包子铺(月坛店)', 3, '', '月坛北街26号',        116.346000, 39.913400,  1500, 43000, 5100, 4.1, '06:00-20:00');

-- 普通券
INSERT INTO tb_voucher (shop_id, title, sub_title, rules, pay_value, actual_value, type, stock, begin_time, end_time) VALUES
(1, '88元代100元代金券', '全场通用,可叠加使用', '仅限堂食使用;每桌限用1张;不可与其它优惠同享', 8800, 10000, 0, 500, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY);

-- 秒杀券
INSERT INTO tb_voucher (shop_id, title, sub_title, rules, pay_value, actual_value, type, stock, begin_time, end_time) VALUES
(2, '50元代100元秒杀券', '海底捞火锅专属秒杀', '仅限海底捞(西单店)使用;每人限购1张', 5000, 10000, 1, 200, NOW() - INTERVAL 1 HOUR, NOW() + INTERVAL 7 DAY),
(3, '1元秒杀大杯拿铁', '瑞幸咖啡专属秒杀', '仅限瑞幸(国贸店)使用;每人限购1张', 100, 1900, 1, 100, NOW() - INTERVAL 1 HOUR, NOW() + INTERVAL 7 DAY);
