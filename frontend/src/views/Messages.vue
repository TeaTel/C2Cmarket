<template>
  <div class="messages-page">
    <!-- 顶部标题栏 -->
    <header class="page-header">
      <h1 class="page-title">消息</h1>
      <span v-if="unreadTotal > 0" class="unread-badge">{{ unreadTotal > 99 ? '99+' : unreadTotal }}</span>
    </header>

    <!-- 消息列表 -->
    <main class="messages-list">
      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="skeleton-item" v-for="i in 5" :key="i">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-content">
            <div class="skeleton-line long"></div>
            <div class="skeleton-line short"></div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="conversations.length === 0" class="empty-state">
        <div class="empty-icon">💬</div>
        <h3 class="empty-title">暂无消息</h3>
        <p class="empty-desc">去商品详情页联系卖家吧~</p>
        <button @click="$router.push('/')" class="empty-btn">去逛逛</button>
      </div>

      <!-- 会话列表 -->
      <div v-else class="conversation-list">
        <div
          v-for="conv in conversations"
          :key="conv.userId"
          @click="openChat(conv)"
          class="conversation-item"
        >
          <!-- 头像 -->
          <div class="avatar-wrapper">
            <img
              :src="conv.avatar || defaultAvatar"
              :alt="conv.username"
              class="avatar"
            />
            <!-- 在线状态指示器 -->
            <span v-if="conv.isOnline" class="online-indicator"></span>
          </div>

          <!-- 内容区 -->
          <div class="conv-content">
            <div class="conv-header">
              <h4 class="username">{{ conv.username }}</h4>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>

            <div class="conv-body">
              <p class="last-message line-clamp-1">{{ conv.lastMessage || '暂无消息' }}</p>
              <!-- 未读数量 -->
              <span v-if="conv.unreadCount > 0" class="unread-count">
                {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
              </span>
            </div>
          </div>

          <!-- 商品预览卡片（如果有） -->
          <div v-if="conv.productInfo" class="product-preview">
            <img :src="conv.productInfo.imageUrl" alt="" class="product-thumb" />
            <div class="product-info-mini">
              <p class="product-name-mini">{{ conv.productInfo.name }}</p>
              <span class="product-price-mini">¥{{ conv.productInfo.price }}</span>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { messageApi } from '../services/api'

const router = useRouter()

// 数据状态
const conversations = ref([])
const loading = ref(true)
const unreadTotal = ref(0)

// 默认头像
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI0UwRTBFRCIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTciIHI9IjgiIGZpbGw9IndoaXRlIi8+PC9zdmc+'

onMounted(async () => {
  await loadConversations()
})

// 加载会话列表
async function loadConversations() {
  try {
    loading.value = true

    const response = await messageApi.getContacts()

    if (response.code === 200) {
      const data = response.data || []

      // 转换数据格式并添加模拟数据用于演示
      conversations.value = data.length > 0 ? data : generateMockConversations()

      // 计算总未读数
      unreadTotal.value = conversations.value.reduce(
        (total, conv) => total + (conv.unreadCount || 0),
        0
      )
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
    // 使用模拟数据作为fallback
    conversations.value = generateMockConversations()
  } finally {
    loading.value = false
  }
}

// 打开聊天室
function openChat(conv) {
  router.push({
    path: `/chat/${conv.userId}`,
    query: {
      productId: conv.productId || undefined
    }
  })
}

// 格式化时间
function formatTime(timestamp) {
  if (!timestamp) return ''

  const date = new Date(timestamp)
  const now = new Date()
  const diffMs = now - date
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)

  if (diffMin < 1) {
    return '刚刚'
  } else if (diffMin < 60) {
    return `${diffMin}分钟前`
  } else if (diffHour < 24) {
    return `${diffHour}小时前`
  } else if (diffDay < 7) {
    return `${diffDay}天前`
  } else {
    return date.toLocaleDateString('zh-CN', {
      month: 'numeric',
      day: 'numeric'
    })
  }
}

// 生成模拟数据（用于演示）
function generateMockConversations() {
  return [
    {
      userId: 1001,
      username: '数码达人小王',
      avatar: null,
      lastMessage: '这个iPhone成色很新，还在保，价格可以再商量~',
      lastMessageTime: new Date(Date.now() - 300000).toISOString(),
      unreadCount: 2,
      isOnline: true,
      productInfo: {
        id: 1,
        name: 'iPhone 13 Pro 256G 蓝色',
        price: '4299',
        imageUrl: 'https://via.placeholder.com/60x60/e8f5e9/999999?text=iPhone'
      }
    },
    {
      userId: 1002,
      username: '书虫小李',
      avatar: null,
      lastMessage: '好的，那我们明天下午在图书馆门口见',
      lastMessageTime: new Date(Date.now() - 3600000).toISOString(),
      unreadCount: 0,
      isOnline: false,
      productInfo: null
    },
    {
      userId: 1003,
      username: '运动健将张三',
      avatar: null,
      lastMessage: '这双鞋穿过两次，基本全新，原价599入的',
      lastMessageTime: new Date(Date.now() - 7200000).toISOString(),
      unreadCount: 1,
      isOnline: true,
      productInfo: {
        id: 5,
        name: 'Nike Air Max 270 白色 42码',
        price: '280',
        imageUrl: 'https://via.placeholder.com/60x60/e3f2fd/999999?text=Shoes'
      }
    },
    {
      userId: 1004,
      username: '美妆博主Luna',
      avatar: null,
      lastMessage: '这套护肤品是正品，有购买记录的',
      lastMessageTime: new Date(Date.now() - 86400000).toISOString(),
      unreadCount: 0,
      isOnline: false,
      productInfo: null
    },
    {
      userId: 1005,
      username: '宿舍神器店长',
      avatar: null,
      lastMessage: '亲，这款台灯已经卖出去啦，不过还有同款的',
      lastMessageTime: new Date(Date.now() - 172800000).toISOString(),
      unreadCount: 0,
      isOnline: true,
      productInfo: null
    }
  ]
}
</script>

<style scoped>
.messages-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* ============================================
   页面头部
   ============================================ */
.page-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 16px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.unread-badge {
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  background: linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%);
  color: white;
  font-size: 12px;
  font-weight: 600;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
}

/* ============================================
   加载状态骨架屏
   ============================================ */
.loading-state {
  padding: 16px;
  background-color: #fff;
}

.skeleton-item {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.skeleton-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}

.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.skeleton-line {
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-line.long {
  width: 70%;
}

.skeleton-line.short {
  width: 45%;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ============================================
   空状态
   ============================================ */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 32px;
  text-align: center;
}

.empty-icon {
  font-size: 72px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #666;
  margin: 0 0 8px 0;
}

.empty-desc {
  font-size: 14px;
  color: #999;
  margin: 0 0 28px 0;
}

.empty-btn {
  background: linear-gradient(135deg, #FF6A00 0%, #FF8533 100%);
  color: white;
  padding: 12px 36px;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.25s ease;
}

.empty-btn:active {
  transform: scale(0.95);
}

/* ============================================
   会话列表
   ============================================ */
.conversation-list {
  background-color: #fff;
}

.conversation-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
  transition: background-color 0.15s ease;
  cursor: pointer;
  position: relative;
}

.conversation-item:active {
  background-color: #fafafa;
}

/* 头像区域 */
.avatar-wrapper {
  position: relative;
  width: 52px;
  height: 52px;
  flex-shrink: 0;
}

.avatar {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  background-color: #e0e0e0;
}

.online-indicator {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background-color: #52c41a;
  border: 2px solid #fff;
  border-radius: 50%;
}

/* 内容区 */
.conv-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.conv-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.username {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 65%;
}

.conv-time {
  font-size: 12px;
  color: #bbb;
  flex-shrink: 0;
}

.conv-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.last-message {
  font-size: 14px;
  color: #999;
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.unread-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background-color: #FF4D4F;
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

/* 商品预览卡片 */
.product-preview {
  display: flex;
  gap: 10px;
  padding: 10px;
  background-color: #fafafa;
  border-radius: 10px;
  margin-left: 64px; /* 与头像对齐 */
}

.product-thumb {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  object-fit: cover;
  background-color: #e0e0e0;
  flex-shrink: 0;
}

.product-info-mini {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  min-width: 0;
}

.product-name-mini {
  font-size: 13px;
  color: #666;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price-mini {
  font-size: 14px;
  font-weight: 700;
  color: #FF4D4F;
}
</style>
