-- ============================================================
-- 数据库迁移脚本: v1 (旧) -> v2.0 (闲鱼风格重构)
-- 安全升级：添加新列、创建新表、保留旧数据
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------
-- 1. users 表升级
-- -----------------------------------------------------------
ALTER TABLE users ADD COLUMN IF NOT EXISTS nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称' AFTER email;
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL' AFTER nickname;
ALTER TABLE users ADD COLUMN IF NOT EXISTS gender TINYINT(1) DEFAULT NULL COMMENT '性别:0未知,1男,2女' AFTER avatar;
ALTER TABLE users ADD COLUMN IF NOT EXISTS school VARCHAR(100) DEFAULT NULL COMMENT '学校名称' AFTER gender;
ALTER TABLE users ADD COLUMN IF NOT EXISTS major VARCHAR(100) DEFAULT NULL COMMENT '专业' AFTER school;
ALTER TABLE users ADD COLUMN IF NOT EXISTS grade VARCHAR(20) DEFAULT NULL COMMENT '年级' AFTER major;
ALTER TABLE users MODIFY COLUMN wechat VARCHAR(50) DEFAULT NULL COMMENT '微信号';
ALTER TABLE users MODIFY COLUMN qq VARCHAR(20) DEFAULT NULL COMMENT 'QQ号';
ALTER TABLE users ADD COLUMN IF NOT EXISTS bio VARCHAR(255) DEFAULT NULL COMMENT '个人简介' AFTER qq;
ALTER TABLE users MODIFY COLUMN is_student TINYINT(1) DEFAULT 1 COMMENT '是否在校学生';
ALTER TABLE users MODIFY COLUMN status TINYINT(1) DEFAULT 1 COMMENT '状态:0禁用,1正常,2封禁';
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间' AFTER status;
-- 删除旧的不需要的列（如果存在）
ALTER TABLE users DROP COLUMN IF EXISTS created_at;
ALTER TABLE users ADD COLUMN created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
ALTER TABLE users DROP COLUMN IF EXISTS updated_at;
ALTER TABLE users ADD COLUMN updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';

-- -----------------------------------------------------------
-- 2. products 表升级
-- -----------------------------------------------------------
ALTER TABLE products ADD COLUMN IF NOT EXISTS original_price DECIMAL(10,2) DEFAULT NULL COMMENT '原价/全新价格' AFTER price;
ALTER TABLE products ADD COLUMN IF NOT EXISTS condition_level TINYINT DEFAULT NULL COMMENT '成色等级:1全新,2几乎全新,3轻微使用痕迹,4明显使用痕迹,5一般' AFTER category_id;
ALTER TABLE products ADD COLUMN IF NOT EXISTS image_urls JSON DEFAULT NULL COMMENT '商品图片URL列表(JSON数组)' AFTER condition_level;
ALTER TABLE products ADD COLUMN IF NOT EXISTS cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL' AFTER image_urls;
ALTER TABLE products ADD COLUMN IF NOT EXISTS view_count INT DEFAULT 0 COMMENT '浏览次数' AFTER status;
ALTER TABLE products ADD COLUMN IF NOT EXISTS like_count INT DEFAULT 0 COMMENT '收藏/喜欢次数' AFTER view_count;
ALTER TABLE products ADD COLUMN IF NOT EXISTS location VARCHAR(100) DEFAULT NULL COMMENT '交易地点' AFTER like_count;
ALTER TABLE products ADD COLUMN IF NOT EXISTS delivery_method TINYINT(1) DEFAULT NULL COMMENT '交付方式:1自提,2快递,3均可' AFTER location;
-- 删除旧的contact字段（已移到用户资料和聊天中协商）
ALTER TABLE products DROP COLUMN IF EXISTS contact_phone;
ALTER TABLE products DROP COLUMN IF EXISTS contact_wechat;
-- 添加全文索引（如果不存在）
-- ALTER TABLE products ADD FULLTEXT INDEX ft_name_desc (name, description);

-- -----------------------------------------------------------
-- 3. categories 表 - 基本结构不变，确保有icon_url
-- -----------------------------------------------------------
ALTER TABLE categories ADD COLUMN IF NOT EXISTS icon_url VARCHAR(255) DEFAULT NULL COMMENT '图标URL' AFTER description;

-- -----------------------------------------------------------
-- 4. orders 表升级
-- -----------------------------------------------------------
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_no VARCHAR(32) NOT NULL COMMENT '订单编号';
-- 为现有数据生成order_no
UPDATE orders SET order_no = CONCAT('ORD', DATE_FORMAT(created_at, '%Y%m%d%H%i%s'), LPAD(id, 4, '0')) WHERE order_no IS NULL OR order_no = '';
ALTER TABLE orders MODIFY COLUMN order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单编号';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS remark VARCHAR(255) DEFAULT NULL COMMENT '买家备注' AFTER status;
-- 删除payment_method（不含支付功能）
ALTER TABLE orders DROP COLUMN IF EXISTS payment_method;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS completed_at DATETIME DEFAULT NULL COMMENT '完成时间' AFTER updated_at;

-- -----------------------------------------------------------
-- 5. order_items 表升级：image_url -> product_image
-- -----------------------------------------------------------
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_image VARCHAR(255) DEFAULT NULL COMMENT '商品图片(快照)' AFTER product_name;
-- 如果旧列image_url存在且product_image为空，则迁移数据
UPDATE order_items SET product_image = image_url WHERE product_image IS NULL AND image_url IS NOT NULL;
ALTER TABLE order_items DROP COLUMN IF EXISTS image_url;

-- -----------------------------------------------------------
-- 6. 创建聊天会话表 (全新)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_conversations (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    user1_id        BIGINT       NOT NULL COMMENT '参与者1的用户ID',
    user2_id        BIGINT       NOT NULL COMMENT '参与者2的用户ID',
    product_id      BIGINT       DEFAULT NULL COMMENT '关联的商品ID(可选)',
    last_message    TEXT         DEFAULT NULL COMMENT '最后一条消息内容',
    last_message_at DATETIME     DEFAULT NULL COMMENT '最后消息时间',
    user1_unread    INT          DEFAULT 0 COMMENT 'user1的未读消息数',
    user2_unread    INT          DEFAULT 0 COMMENT 'user2的未读消息数',
    status          TINYINT(1)   DEFAULT 1 COMMENT '状态:0已关闭,1正常',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL,
    UNIQUE INDEX uk_user_pair (LEAST(user1_id, user2_id), GREATEST(user1_id, user2_id)),
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    INDEX idx_product (product_id),
    INDEX idx_last_message_at (last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天会话表';

-- -----------------------------------------------------------
-- 7. 创建聊天消息表 (全新)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_messages (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    conversation_id BIGINT       NOT NULL COMMENT '所属会话ID',
    sender_id       BIGINT       NOT NULL COMMENT '发送者用户ID',
    receiver_id     BIGINT       NOT NULL COMMENT '接收者用户ID',
    content         TEXT         NOT NULL COMMENT '消息内容',
    message_type    VARCHAR(20)  DEFAULT 'text' COMMENT '消息类型:text,image,system',
    is_read         TINYINT(1)   DEFAULT 0 COMMENT '是否已读:0未读,1已读',
    read_at         DATETIME     DEFAULT NULL COMMENT '阅读时间',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    FOREIGN KEY (conversation_id) REFERENCES chat_conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_sender_id (sender_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_created_at (created_at),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='聊天消息表';

-- -----------------------------------------------------------
-- 8. 创建收藏表 (全新)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS favorites (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收藏ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    product_id      BIGINT       NOT NULL COMMENT '商品ID',
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE INDEX uk_user_product (user_id, product_id),
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品收藏表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 迁移完成!
-- 新增: chat_conversations, chat_messages, favorites
-- 升级: users(+9列), products(+8列-2列), orders(+3列-1列), order_items(+1列-1列)
-- ============================================================