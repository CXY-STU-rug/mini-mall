-- ============================================================
-- 迷你商城（mini-mall）数据库建表脚本
-- 数据库：mini_mall
-- 用途：换电脑/重装时一键重建所有表；阅读时一目了然
-- 使用方法：
--   1) mysql -uroot -p123456
--   2) CREATE DATABASE mini_mall DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
--   3) USE mini_mall;
--   4) SOURCE D:/path/to/schema.sql;   （或在 Navicat 里运行整个文件）
-- ============================================================

-- 注意：建表顺序按依赖关系排（被引用的先建，但本项目没建外键，只是逻辑依赖）

-- ------------------------------------------------------------
-- 1. user —— 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT       COMMENT '主键ID',
  `username`    VARCHAR(50)  NOT NULL                       COMMENT '用户名（登录用，唯一）',
  `password`    VARCHAR(100) NOT NULL                       COMMENT '密码（BCrypt 加密后）',
  `nickname`    VARCHAR(50)  DEFAULT NULL                   COMMENT '昵称（展示用）',
  `phone`       VARCHAR(20)  DEFAULT NULL                   COMMENT '手机号',
  `email`       VARCHAR(100) DEFAULT NULL                   COMMENT '邮箱',
  `avatar`      VARCHAR(100) DEFAULT NULL                   COMMENT '头像URL',
  `role`        TINYINT      NOT NULL DEFAULT 0             COMMENT '角色：0=普通用户 1=管理员',
  `status`      TINYINT      NOT NULL DEFAULT 1             COMMENT '状态：0=禁用 1=启用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)                     -- 用户名唯一，注册时防止重名
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. category —— 商品分类表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT       COMMENT '分类ID',
  `name`        VARCHAR(50)  NOT NULL                       COMMENT '分类名',
  `icon`        VARCHAR(255) DEFAULT NULL                   COMMENT '图标URL',
  `sort`        INT          NOT NULL DEFAULT 0             COMMENT '排序值，越小越靠前',
  `status`      TINYINT      NOT NULL DEFAULT 1             COMMENT '状态：0=禁用 1=启用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- ------------------------------------------------------------
-- 3. product —— 商品表
--   外键关系（逻辑上）：category_id → category.id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT      COMMENT '商品ID',
  `category_id` BIGINT        NOT NULL                      COMMENT '分类ID（指向 category.id）',
  `name`        VARCHAR(100)  NOT NULL                      COMMENT '商品名',
  `description` VARCHAR(500)  DEFAULT NULL                  COMMENT '商品简介',
  `detail`      TEXT          DEFAULT NULL                  COMMENT '商品详情（长文本HTML）',
  `price`       DECIMAL(10,2) NOT NULL                      COMMENT '价格（元）',
  `stock`       INT           NOT NULL DEFAULT 0            COMMENT '库存',
  `sales`       INT           NOT NULL DEFAULT 0            COMMENT '销量（累计）',
  `cover_image` VARCHAR(255)  DEFAULT NULL                  COMMENT '封面图URL',
  `status`      TINYINT       NOT NULL DEFAULT 1            COMMENT '状态：0=下架 1=上架',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT       NOT NULL DEFAULT 0            COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`)                     -- 按分类查商品的查询会走这个索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ------------------------------------------------------------
-- 4. address —— 收货地址表
--   逻辑外键：user_id → user.id
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT       COMMENT '主键ID',
  `user_id`     BIGINT       NOT NULL                       COMMENT '用户ID（指向 user.id）',
  `receiver`    VARCHAR(50)  NOT NULL                       COMMENT '收货人姓名',
  `phone`       VARCHAR(20)  NOT NULL                       COMMENT '手机号',
  `province`    VARCHAR(50)  NOT NULL                       COMMENT '省',
  `city`        VARCHAR(50)  NOT NULL                       COMMENT '市',
  `district`    VARCHAR(50)  NOT NULL                       COMMENT '区/县',
  `detail`      VARCHAR(200) NOT NULL                       COMMENT '详细地址',
  `is_default`  TINYINT      NOT NULL DEFAULT 0             COMMENT '是否默认：0否 1是',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)                             -- "我的地址列表"按 user_id 查
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收货地址表';

-- ------------------------------------------------------------
-- 5. cart_item —— 购物车表
--   逻辑外键：user_id → user.id, product_id → product.id
--   关键：(user_id, product_id) 唯一约束 —— 同一用户同一商品只能有一条，加购时累加 quantity
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT          COMMENT '主键ID',
  `user_id`     BIGINT   NOT NULL                          COMMENT '用户ID',
  `product_id`  BIGINT   NOT NULL                          COMMENT '商品ID',
  `quantity`    INT      NOT NULL DEFAULT 1                COMMENT '数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`  TINYINT  NOT NULL DEFAULT 0                COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)   -- 同一用户的同一商品只能有一行（幂等保障）
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='购物车表';

-- ------------------------------------------------------------
-- 6. orders —— 订单主表
--   注意：表名是 orders（带 s），因为 order 是 MySQL 保留字
--   设计要点：
--     - order_no：业务订单号（如 20260524123456001），对外暴露用这个，不是 id
--     - receiver/phone/address：**快照字段**，下单瞬间拷贝过来，
--       之后用户改地址也不影响历史订单
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT     COMMENT '主键ID',
  `order_no`     VARCHAR(32)   NOT NULL                     COMMENT '订单号（业务唯一）',
  `user_id`      BIGINT        NOT NULL                     COMMENT '用户ID',
  `total_amount` DECIMAL(10,2) NOT NULL                     COMMENT '订单总金额',
  `status`       TINYINT       NOT NULL DEFAULT 0           COMMENT '状态：0待付款 1已付款 2已发货 3已完成 4已取消',
  `receiver`     VARCHAR(50)   NOT NULL                     COMMENT '收货人（快照）',
  `phone`        VARCHAR(20)   NOT NULL                     COMMENT '手机号（快照）',
  `address`      VARCHAR(500)  NOT NULL                     COMMENT '收货地址（快照，省+市+区+详细）',
  `pay_time`     DATETIME      DEFAULT NULL                 COMMENT '支付时间',
  `ship_time`    DATETIME      DEFAULT NULL                 COMMENT '发货时间',
  `finish_time`  DATETIME      DEFAULT NULL                 COMMENT '完成时间',
  `remark`       VARCHAR(200)  DEFAULT NULL                 COMMENT '备注',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`   TINYINT       NOT NULL DEFAULT 0           COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),                    -- 订单号唯一
  KEY `idx_user_id` (`user_id`)                             -- "我的订单"按 user_id 查
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- ------------------------------------------------------------
-- 7. order_item —— 订单明细表
--   一个订单可能有多个商品 → 一行 orders 对应多行 order_item
--   关键设计：product_name / product_image / price 都是**快照**
--             商品改名改价不影响历史订单的展示
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT    COMMENT '主键ID',
  `order_id`      BIGINT        NOT NULL                    COMMENT '订单ID（指向 orders.id）',
  `product_id`    BIGINT        NOT NULL                    COMMENT '商品ID',
  `product_name`  VARCHAR(100)  NOT NULL                    COMMENT '商品名（快照）',
  `product_image` VARCHAR(255)  DEFAULT NULL                COMMENT '商品图（快照）',
  `price`         DECIMAL(10,2) NOT NULL                    COMMENT '成交单价（快照）',
  `quantity`      INT           NOT NULL                    COMMENT '购买数量',
  `subtotal`      DECIMAL(10,2) NOT NULL                    COMMENT '小计 = price * quantity',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)                           -- 按订单查明细
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';


-- ============================================================
-- 📐 表关系图（文字版）
-- ============================================================
--
--   user (1) ────────┬──────── (N) address       一个用户多个地址
--                    │
--                    ├──────── (N) cart_item ──── (1) product   购物车连接用户和商品
--                    │
--                    └──────── (N) orders ─────── (N) order_item ── (1) product
--                                                                 （商品信息是快照）
--   category (1) ──── (N) product       一个分类多个商品
--
-- ============================================================
-- 🔑 设计要点速查
-- ============================================================
--   1. 所有表都有 is_deleted —— 配合 MyBatis-Plus @TableLogic 实现"软删除"
--   2. 所有表都有 create_time/update_time —— 自动维护，看数据生命周期方便
--   3. 金额一律 DECIMAL(10,2) —— 永远不要用 float/double 存钱（精度丢失）
--   4. 字符集 utf8mb4 —— 支持 emoji 和生僻字
--   5. 命名规约：表名/字段名小写下划线（user_id），Java 字段驼峰（userId）
--      由 mybatis-plus 的 map-underscore-to-camel-case: true 自动映射
--   6. 索引原则：where 经常用、order by 经常用的字段加 KEY；唯一字段加 UNIQUE KEY
--   7. 订单/订单明细的"快照字段"：下单瞬间冻结，不跟随商品/地址后续变化
