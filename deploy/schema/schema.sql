-- ============================================
-- 电商RAG系统 - 完整数据库表结构
-- 数据库名: e_commerce_server
-- 创建时间: 2024年
-- ============================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS `e_commerce_server`
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

USE `e_commerce_server`;

-- ============================================
-- 1. 用户表 (users)
-- ============================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
                         `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                         `identifier` varchar(100) NOT NULL COMMENT '用户账号或者临时标识',
                         `password` varchar(128) DEFAULT NULL COMMENT '用户密码',
                         `phone` varchar(11) DEFAULT NULL COMMENT '用户联系电话',
                         `type` int NOT NULL COMMENT '用户类型，（0表示临时用户，1表示授权用户，2表示后台管理只读权限用户，3表示后台管理可编辑用户，4表示知识增量补充负责人）',
                         `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '用户创建时间',
                         `updated_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '用户修改时间',
                         `last_active` datetime DEFAULT NULL COMMENT '上次活跃的时间',
                         `email` varchar(50) DEFAULT NULL,
                         PRIMARY KEY (`id`),
                         KEY `identifier_index` (`identifier`)
) ENGINE=InnoDB AUTO_INCREMENT=323 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

-- ============================================
-- 2. 数据集表 (dataset)
-- ============================================
DROP TABLE IF EXISTS `dataset`;
CREATE TABLE `dataset` (
                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '数据集ID',
                           `user_id` BIGINT NOT NULL COMMENT '用户ID',
                           `name` VARCHAR(255) NOT NULL COMMENT '数据集名称',
                           `description` TEXT COMMENT '数据集描述',
                           `app_count` INT DEFAULT '0' COMMENT '应用次数',
                           `doc_count` INT DEFAULT '0' COMMENT '文档数量',
                           `disabled` TINYINT(1) DEFAULT '1' COMMENT '是否禁用：1-启用，0-禁用',
                           `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           `disabled_at` DATETIME DEFAULT NULL COMMENT '禁用时间',
                           PRIMARY KEY (`id`),
                           KEY `idx_user_id` (`user_id`),
                           KEY `idx_disabled` (`disabled`),
                           KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据集表';


-- ============================================
-- 3. 数据集文件表 (dataset_files)
-- ============================================
DROP TABLE IF EXISTS `dataset_files`;
CREATE TABLE `dataset_files` (
                                 `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
                                 `name` VARCHAR(255) NOT NULL COMMENT '文件名',
                                 `file_path` VARCHAR(512) DEFAULT NULL COMMENT '文件路径',
                                 `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
                                 `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
                                 `dataset_id` BIGINT NOT NULL COMMENT '所属数据集ID',
                                 `user_id` BIGINT NOT NULL COMMENT '上传用户ID',
                                 `disabled` TINYINT(1) DEFAULT '1' COMMENT '状态：1-启用，-1-禁用',
                                 `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                 `disabled_at` DATETIME DEFAULT NULL COMMENT '禁用时间',
                                 `hit_count` INT DEFAULT '0' COMMENT '命中次数',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_dataset_id` (`dataset_id`),
                                 KEY `idx_user_id` (`user_id`),
                                 KEY `idx_disabled` (`disabled`),
                                 KEY `idx_file_type` (`file_type`),
                                 KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据集文件表';

-- ============================================
-- 4. 评价表 (evaluate)
-- ============================================
DROP TABLE IF EXISTS `evaluate`;
CREATE TABLE `evaluate` (
                            `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
                            `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
                            `session_id` VARCHAR(255) DEFAULT NULL COMMENT '会话ID',
                            `message_id` VARCHAR(255) DEFAULT NULL COMMENT '消息ID',
                            `rating` TINYINT(1) DEFAULT NULL COMMENT '评价：1-点赞，-1-点踩',
                            `comment` TEXT COMMENT '反馈内容',
                            `created_at` VARCHAR(50) DEFAULT NULL COMMENT '创建时间',
                            `updated_at` VARCHAR(50) DEFAULT NULL COMMENT '修改时间',
                            PRIMARY KEY (`id`),
                            KEY `idx_user_id` (`user_id`),
                            KEY `idx_session_id` (`session_id`),
                            KEY `idx_message_id` (`message_id`),
                            KEY `idx_rating` (`rating`),
                            KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户反馈评价表';

-- ============================================
-- 5. 聊天日志表 (chat_logs)
-- ============================================
DROP TABLE IF EXISTS `chat_logs`;
CREATE TABLE `chat_logs` (
                             `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                             `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
                             `session_id` VARCHAR(255) DEFAULT NULL COMMENT '会话ID',
                             `message_id` VARCHAR(255) DEFAULT NULL COMMENT '消息ID',
                             `text` LONGTEXT COMMENT '消息内容',
                             `status` TINYINT(1) DEFAULT '1' COMMENT '状态：1-成功，0-被中断',
                             `message_type` VARCHAR(50) DEFAULT NULL COMMENT '消息类型：SYSTEM/USER/ASSISTANT',
                             `metadata` JSON DEFAULT NULL COMMENT '元数据（JSON格式）',
                             `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             PRIMARY KEY (`id`),
                             KEY `idx_user_id` (`user_id`),
                             KEY `idx_session_id` (`session_id`),
                             KEY `idx_message_id` (`message_id`),
                             KEY `idx_status` (`status`),
                             KEY `idx_message_type` (`message_type`),
                             KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='聊天日志表';

-- ============================================
-- 6. 商品主表 (product)
-- ============================================
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
                           `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
                           `product_code` VARCHAR(64) NOT NULL COMMENT '商品编码',
                           `title` VARCHAR(512) NOT NULL COMMENT '商品标题',
                           `brand` VARCHAR(128) DEFAULT NULL COMMENT '品牌名称',
                           `category` VARCHAR(64) DEFAULT NULL COMMENT '一级分类',
                           `sub_category` VARCHAR(64) DEFAULT NULL COMMENT '二级分类',
                           `base_price` DECIMAL(12,2) DEFAULT NULL COMMENT '基础价格',
                           `main_image_url` VARCHAR(512) DEFAULT NULL COMMENT '商品主图OSS URL',
                           `local_image_path` VARCHAR(512) DEFAULT NULL COMMENT '本地图片路径',
                           `status` TINYINT(1) DEFAULT '1' COMMENT '商品状态：0-下架，1-上架',
                           `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                           `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `uk_product_code` (`product_code`),
                           KEY `idx_category` (`category`),
                           KEY `idx_brand` (`brand`),
                           KEY `idx_status` (`status`),
                           KEY `idx_sub_category` (`sub_category`),
                           KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品主表';

-- ============================================
-- 7. SKU规格表 (product_sku)
-- ============================================
DROP TABLE IF EXISTS `product_sku`;
CREATE TABLE `product_sku` (
                               `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
                               `sku_code` VARCHAR(64) NOT NULL COMMENT 'SKU编码',
                               `product_id` BIGINT NOT NULL COMMENT '商品ID',
                               `price` DECIMAL(12,2) DEFAULT NULL COMMENT 'SKU价格',
                               `status` TINYINT(1) DEFAULT '1' COMMENT 'SKU状态：0-下架，1-上架',
                               `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               PRIMARY KEY (`id`),
                               UNIQUE KEY `uk_sku_code` (`sku_code`),
                               KEY `idx_product_id` (`product_id`),
                               KEY `idx_status` (`status`),
                               KEY `idx_price` (`price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品SKU规格表';

-- ============================================
-- 8. SKU属性表 (product_sku_attribute)
-- ============================================
DROP TABLE IF EXISTS `product_sku_attribute`;
CREATE TABLE `product_sku_attribute` (
                                         `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '属性ID',
                                         `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
                                         `attr_name` VARCHAR(64) NOT NULL COMMENT '属性名称（如：容量、颜色、尺码）',
                                         `attr_value` VARCHAR(256) NOT NULL COMMENT '属性值（如：30ml、256GB、黑色）',
                                         `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         PRIMARY KEY (`id`),
                                         KEY `idx_sku_id` (`sku_id`),
                                         KEY `idx_attr_name` (`attr_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SKU属性表';


-- ============================================
-- 购物车相关表结构
-- ============================================

-- ============================================
-- 9. 购物车表 (cart)
-- ============================================
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
                        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车ID',
                        `user_id` BIGINT NOT NULL COMMENT '用户ID',
                        `total_amount` DECIMAL(12,2) DEFAULT '0.00' COMMENT '购物车总金额',
                        `total_items` INT DEFAULT '0' COMMENT '商品总数',
                        `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`id`),
                        UNIQUE KEY `uk_user_id` (`user_id`),
                        KEY `idx_user_id` (`user_id`),
                        KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车表';

-- ============================================
-- 10. 购物车商品项表 (cart_item)
-- ============================================
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item` (
                             `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
                             `cart_id` BIGINT NOT NULL COMMENT '购物车ID',
                             `product_id` BIGINT NOT NULL COMMENT '商品ID',
                             `sku_id` BIGINT NOT NULL COMMENT 'SKU ID',
                             `quantity` INT NOT NULL DEFAULT '1' COMMENT '商品数量',
                             `price` DECIMAL(12,2) NOT NULL COMMENT '商品单价',
                             `subtotal` DECIMAL(12,2) NOT NULL COMMENT '小计金额',
                             `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                             `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                             PRIMARY KEY (`id`),
                             KEY `idx_cart_id` (`cart_id`),
                             KEY `idx_product_id` (`product_id`),
                             KEY `idx_sku_id` (`sku_id`),
                             KEY `idx_created_at` (`created_at`),
                             CONSTRAINT `fk_cart_item_cart` FOREIGN KEY (`cart_id`) REFERENCES `cart` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='购物车商品项表';

-- ============================================
-- 表关系说明
-- ============================================
--
-- 用户表 (users)
--   ↓ 1:N
-- 数据集表 (dataset) - user_id关联
--   ↓ 1:N
-- 数据集文件表 (dataset_files) - dataset_id关联
--
-- 用户表 (users)
--   ↓ 1:N
-- 商品表 (product) - 预留商家关联
--   ↓ 1:N
-- SKU表 (product_sku) - product_id关联
--   ↓ 1:N
-- SKU属性表 (product_sku_attribute) - sku_id关联
--
-- 用户表 (users)
--   ↓ 1:N
-- 聊天日志表 (chat_logs) - user_id关联
--   ↓ 1:N
-- 评价表 (evaluate) - user_id关联

-- ============================================
-- 初始化测试数据（可选）
-- ============================================

-- 插入测试用户
INSERT INTO `users` VALUES (2,'xiaoTnb','b6aab45f109e1ea8851af151e5f01503','13113340545',0,'2025-10-03 15:00:12','2025-10-03 15:00:12',NULL,NULL),(5,'AAA','e0c79caa654708df3fecdf49391f0a33',NULL,0,'2025-10-14 14:07:37','2025-10-14 14:07:37',NULL,NULL),(26,'xiaoliu','b6aab45f109e1ea8851af151e5f01503',NULL,0,'2025-10-18 11:31:53','2025-10-18 11:31:53',NULL,NULL),(83,'00001','18df8e94f642847978774cb61572f4a7',NULL,0,'2025-10-20 08:42:01','2025-10-20 08:42:01',NULL,NULL),(126,'admin1','b6aab45f109e1ea8851af151e5f01503',NULL,1,'2025-10-22 13:25:42','2025-10-22 13:25:42',NULL,NULL),(127,'admin2','b6aab45f109e1ea8851af151e5f01503',NULL,1,'2025-10-22 13:25:58','2025-10-22 13:25:58',NULL,NULL),(128,'admin3','b6aab45f109e1ea8851af151e5f01503',NULL,1,'2025-10-22 13:26:11','2025-10-22 13:26:11',NULL,NULL),(129,'admin4','b6aab45f109e1ea8851af151e5f01503',NULL,1,'2025-10-22 13:26:23','2025-10-22 13:26:23',NULL,NULL),(130,'user1','b6aab45f109e1ea8851af151e5f01503',NULL,2,'2025-10-22 13:26:46','2025-10-22 13:26:46',NULL,NULL),(131,'user2','b6aab45f109e1ea8851af151e5f01503',NULL,2,'2025-10-22 13:26:59','2025-10-22 13:26:59',NULL,NULL),(132,'user3','b6aab45f109e1ea8851af151e5f01503',NULL,2,'2025-10-22 13:27:10','2025-10-22 13:27:10',NULL,NULL),(133,'user4','b6aab45f109e1ea8851af151e5f01503',NULL,2,'2025-10-22 13:27:24','2025-10-22 13:27:31',NULL,NULL),(134,'super1','78bc2f6af9af840ebdf4161d3c0db90f',NULL,0,'2025-10-22 13:29:01','2025-10-22 13:29:01',NULL,NULL),(135,'super2','78bc2f6af9af840ebdf4161d3c0db90f',NULL,0,'2025-10-22 13:29:20','2025-10-22 13:29:20',NULL,NULL),(321,'guest_1773490183625_kbvm4n','','',3,'2026-05-12 14:40:30','2026-05-12 14:40:30','2026-05-12 14:40:30',NULL),(322,'guest_1773490183625_kbvm4n','','',3,'2026-05-12 14:40:30','2026-05-12 14:40:30','2026-05-12 14:40:30',NULL);

-- ============================================
-- 查看所有表
-- ============================================
SHOW TABLES;

-- ============================================
-- 查看表结构示例
-- ============================================
DESC users;
DESC product;
DESC product_sku;