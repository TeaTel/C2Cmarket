<template>
  <div class="chat-room">
    <!-- 顶部导航栏 -->
    <header class="chat-header">
      <button @click="goBack" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>

      <div class="header-info">
        <h3 class="contact-name">{{ contactInfo.username || '用户' }}</h3>
        <span v-if="isOnline" class="online-status">在线</span>
      </div>

      <button @click="showMoreOptions = !showMoreOptions" class="more-btn">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <circle cx="12" cy="5" r="1.5"/>
          <circle cx="12" cy="12" r="1.5"/>
          <circle cx="12" cy="19" r="1.5"/>
        </svg>
      </button>

      <!-- 更多选项菜单 -->
      <div v-if="showMoreOptions" class="more-menu">
        <button @click="viewContactProfile" class="menu-item">
          查看资料
        </button>
        <button @click="clearMessages" class="menu-item danger">
          清空聊天记录
        </button>
      </div>
    </header>

    <!-- 商品信息卡片（如果有） -->
    <div v-if="productInfo" class="product-card-inline">
      <img :src="productInfo.imageUrl" :alt="productInfo.name" class="product-thumb" />
      <div class="product-detail">
        <h4 class="product-name">{{ productInfo.name }}</h4>
        <p class="product-price">¥{{ productInfo.price }}</p>
      </div>
    </div>

    <!-- 聊天消息列表 -->
    <main class="messages-container" ref="messagesContainer">
      <!-- 加载历史消息 -->
      <div v-if="loadingHistory" class="loading-more-messages">
        <div class="mini-spinner"></div>
        <span>加载中...</span>
      </div>

      <!-- 时间分隔线 -->
      <div v-for="(timeGroup, date) in groupedMessages" :key="date" class="message-group">
        <div class="date-divider">
          <span class="date-text">{{ formatDate(date) }}</span>
        </div>

        <div
          v-for="message in timeGroup"
          :key="message.id"
          :class="['message-item', message.senderId === currentUserId ? 'sent' : 'received']"
        >
          <!-- 对方头像 -->
          <div v-if="message.senderId !== currentUserId" class="avatar-wrapper">
            <img
              :src="contactInfo.avatar || defaultAvatar"
              :alt="contactInfo.username"
              class="avatar"
            />
          </div>

          <!-- 消息气泡 -->
          <div class="bubble-wrapper">
            <div class="message-bubble">
              <p class="message-text">{{ message.content }}</p>
            </div>
            <div class="message-meta">
              <span class="message-time">{{ formatTime(message.createdAt) }}</span>
              <span v-if="message.senderId === currentUserId" class="read-status">
                {{ message.isRead ? '已读' : '未读' }}
              </span>
            </div>
          </div>

          <!-- 自己头像 -->
          <div v-if="message.senderId === currentUserId" class="avatar-wrapper self">
            <img
              :src="currentUserAvatar || defaultAvatar"
              alt="我"
              class="avatar"
            />
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!loadingHistory && messages.length === 0" class="empty-chat">
        <div class="empty-icon">💬</div>
        <p>发送消息开始聊天吧~</p>
      </div>
    </main>

    <!-- 底部输入区域 -->
    <footer class="input-area">
      <!-- 工具栏 -->
      <div class="toolbar">
        <button @click="toggleEmojiPicker" class="tool-btn" :class="{ active: showEmojiPicker }">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
            <line x1="9" y1="9" x2="9.01" y2="9"/>
            <line x1="15" y1="9" x2="15.01" y2="9"/>
          </svg>
        </button>

        <button @click="showImagePicker = true" class="tool-btn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
            <circle cx="8.5" cy="8.5" r="1.5"/>
            <polyline points="21,15 16,10 5,21"/>
          </svg>
        </button>
      </div>

      <!-- 输入框 -->
      <div class="input-wrapper">
        <textarea
          ref="inputRef"
          v-model="newMessage"
          class="message-input"
          placeholder="输入消息..."
          rows="1"
          @input="autoResize"
          @keydown.enter.exact.prevent="sendMessage"
        ></textarea>
        <button
          @click="sendMessage"
          :disabled="!newMessage.trim() || sending"
          class="send-btn"
          :class="{ active: newMessage.trim() }"
        >
          <svg viewBox="0 0 24 24" fill="currentColor">
            <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
          </svg>
        </button>
      </div>

      <!-- 表情选择器 -->
      <div v-if="showEmojiPicker" class="emoji-picker">
        <div class="emoji-grid">
          <span
            v-for="emoji in commonEmojis"
            :key="emoji"
            @click="insertEmoji(emoji)"
            class="emoji-item"
          >{{ emoji }}</span>
        </div>
      </div>
    </footer>

    <!-- 图片选择弹窗 -->
    <div v-if="showImagePicker" class="modal-overlay" @click="showImagePicker = false">
      <div class="image-picker-panel" @click.stop>
        <h3 class="panel-title">选择图片</h3>
        <div class="picker-options">
          <label class="picker-option">
            <input type="file" accept="image/*" @change="handleImageSelect" hidden />
            <span class="option-icon">📷</span>
            <span class="option-text">相册</span>
          </label>
          <label class="picker-option">
            <span class="option-icon">📸</span>
            <span class="option-text">拍照</span>
          </label>
        </div>
        <button @click="showImagePicker = false" class="cancel-picker">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { messageApi, wsManager, productApi } from '../services/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 基础数据
const messages = ref([])
const loadingHistory = ref(false)
const sending = ref(false)
const newMessage = ref('')
const showEmojiPicker = ref(false)
const showImagePicker = ref(false)
const showMoreOptions = ref(false)
const isOnline = ref(true)

// DOM引用
const messagesContainer = ref(null)
const inputRef = ref(null)

// 用户信息
const currentUserId = computed(() => authStore.user?.id)
const currentUserAvatar = computed(() => authStore.user?.avatar)
const contactInfo = ref({})
const productInfo = ref(null)
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI0UwRTBFRCIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTciIHI9IjgiIGZpbGw9IndoaXRlIi8+PC9zdmc+'

// 常用表情
const commonEmojis = [
  '😀', '😂', '🥰', '😎', '🤔', '😢', '😡', '👍',
  '❤️', '🎉', '🔥', '✨', '👏', '🙏', '💪', '🤝',
  '💰', '📚', '🎁', '☀️', '🌙', '⭐', '🌈', '🎈'
]

// 将消息按日期分组
const groupedMessages = computed(() => {
  const groups = {}
  messages.value.forEach(msg => {
    const date = new Date(msg.createdAt).toDateString()
    if (!groups[date]) {
      groups[date] = []
    }
    groups[date].push(msg)
  })
  return groups
})

onMounted(async () => {
  await Promise.all([
    loadContactInfo(),
    loadProductInfo(),
    loadMessages()
  ])

  // 监听WebSocket实时消息
  wsManager.on('chat_message', handleNewMessage)

  // 自动聚焦输入框
  nextTick(() => {
    inputRef.value?.focus()
  })
})

onUnmounted(() => {
  wsManager.off('chat_message', handleNewMessage)
})

// 加载联系人信息
async function loadContactInfo() {
  try {
    const userId = route.params.userId
    // 这里可以调用API获取联系人详细信息
    contactInfo.value = {
      id: userId,
      username: `用户${userId}`,
      avatar: null,
      isOnline: Math.random() > 0.5
    }
    isOnline.value = contactInfo.value.isOnline
  } catch (error) {
    console.error('加载联系人信息失败:', error)
  }
}

// 加载商品信息（如果从商品详情页进入）
async function loadProductInfo() {
  const productId = route.query.productId
  if (!productId) return

  try {
    const response = await productApi.getProductDetail(productId)
    if (response.code === 200 && response.data) {
      productInfo.value = response.data
    }
  } catch (error) {
    console.error('加载商品信息失败:', error)
  }
}

// 加载聊天记录
async function loadMessages() {
  try {
    loadingHistory.value = true
    const otherUserId = route.params.userId

    const response = await messageApi.getConversation(otherUserId, {
      page: 1,
      size: 50
    })

    if (response.code === 200) {
      messages.value = response.data?.records || response.data || []

      // 标记已读
      await markAsRead()

      // 滚动到底部
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载消息失败:', error)
  } finally {
    loadingHistory.value = false
  }
}

// 发送消息
async function sendMessage() {
  const content = newMessage.value.trim()
  if (!content || sending.value) return

  sending.value = true

  try {
    const otherUserId = route.params.userId

    // 先通过HTTP API发送（确保可靠性）
    const response = await messageApi.sendMessage({
      receiverId: parseInt(otherUserId),
      content,
      productId: route.query.productId || null
    })

    if (response.code === 200) {
      // 添加到本地消息列表（乐观更新）
      const tempMsg = {
        id: Date.now(),
        senderId: currentUserId.value,
        content,
        createdAt: new Date().toISOString(),
        isRead: false
      }

      messages.value.push(tempMsg)
      newMessage.value = ''

      // 同时通过WebSocket发送（实时性）
      if (wsManager.isConnected) {
        wsManager.sendMessage(content, parseInt(otherUserId), route.query.productId)
      }

      // 重置输入框高度
      autoResize()

      // 滚动到底部
      scrollToBottom()
    } else {
      alert(response.message || '发送失败')
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    alert('发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

// 处理WebSocket接收到的消息
function handleNewMessage(data) {
  if (data.senderId !== parseInt(route.params.userId)) return

  // 避免重复添加自己发的消息
  if (data.senderId === currentUserId.value) return

  messages.value.push({
    id: data.id || Date.now(),
    senderId: data.senderId,
    content: data.content,
    createdAt: data.timestamp || new Date().toISOString(),
    isRead: true
  })

  scrollToBottom()
}

// 标记会话为已读
async function markAsRead() {
  try {
    const otherUserId = route.params.userId
    await messageApi.markConversationAsRead(otherUserId)
  } catch (error) {
    console.error('标记已读失败:', error)
  }
}

// 工具方法
function goBack() {
  router.back()
}

function viewContactProfile() {
  showMoreOptions.value = false
  router.push(`/profile?userId=${route.params.userId}`)
}

function clearMessages() {
  if (confirm('确定要清空聊天记录吗？')) {
    messages.value = []
    showMoreOptions.value = false
  }
}

function toggleEmojiPicker() {
  showEmojiPicker.value = !showEmojiPicker.value
  showImagePicker.value = false
}

function insertEmoji(emoji) {
  newMessage.value += emoji
  inputRef.value?.focus()
  showEmojiPicker.value = false
}

function handleImageSelect(event) {
  const file = event.target.files[0]
  if (file) {
    // TODO: 实现图片上传和发送
    alert('图片发送功能开发中...')
  }
  showImagePicker.value = false
}

function autoResize() {
  const textarea = inputRef.value
  if (!textarea) return

  textarea.style.height = 'auto'
  textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px'
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTo({
        top: messagesContainer.value.scrollHeight,
        behavior: 'smooth'
      })
    }
  })
}

function formatDate(dateStr) {
  const date = new Date(dateStr)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  if (date.toDateString() === today.toDateString()) {
    return '今天'
  } else if (date.toDateString() === yesterday.toDateString()) {
    return '昨天'
  } else {
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f5f5;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1001;
}

/* ============================================
   顶部导航栏
   ============================================ */
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  border-radius: 50%;
  transition: background-color 0.2s ease;
}

.back-btn:active {
  background-color: #f5f5f5;
}

.back-btn svg {
  width: 22px;
  height: 22px;
}

.header-info {
  flex: 1;
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.contact-name {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.online-status {
  font-size: 12px;
  color: #52c41a;
  font-weight: 500;
}

.more-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  border-radius: 50%;
  transition: background-color 0.2s ease;
}

.more-btn:active {
  background-color: #f5f5f5;
}

.more-btn svg {
  width: 20px;
  height: 20px;
}

.more-menu {
  position: absolute;
  top: 56px;
  right: 16px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  min-width: 140px;
  animation: slideDown 0.2s ease;
  z-index: 10;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.menu-item {
  width: 100%;
  padding: 14px 20px;
  font-size: 15px;
  color: #333;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.menu-item:active {
  background-color: #f5f5f5;
}

.menu-item.danger {
  color: #ff4d4f;
}

/* ============================================
   商品信息卡片
   ============================================ */
.product-card-inline {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  align-items: center;
}

.product-thumb {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  object-fit: cover;
  background-color: #f5f5f5;
}

.product-detail {
  flex: 1;
}

.product-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin: 0 0 4px 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-price {
  font-size: 16px;
  font-weight: 700;
  color: #FF4D4F;
  margin: 0;
}

/* ============================================
   消息列表区域
   ============================================ */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  overscroll-behavior: contain;
}

.loading-more-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  color: #999;
  font-size: 13px;
}

.mini-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e0e0e0;
  border-top-color: #FF6A00;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 日期分隔线 */
.date-divider {
  text-align: center;
  margin: 20px 0;
}

.date-text {
  display: inline-block;
  padding: 4px 12px;
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  font-size: 12px;
  color: #999;
}

/* 消息项 */
.message-item {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  max-width: 85%;
}

.message-item.sent {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-item.received {
  align-self: flex-start;
}

.avatar-wrapper {
  flex-shrink: 0;
}

.avatar-wrapper.self {
  visibility: hidden; /* 自己的消息隐藏头像 */
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #e0e0e0;
}

.bubble-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-item.sent .bubble-wrapper {
  align-items: flex-end;
}

.message-item.received .bubble-wrapper {
  align-items: flex-start;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 18px;
  max-width: 280px;
  word-break: break-word;
}

.message-item.sent .message-bubble {
  background: linear-gradient(135deg, #FF6A00 0%, #FF8533 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message-item.received .message-bubble {
  background-color: #fff;
  color: #333;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.message-text {
  margin: 0;
  font-size: 15px;
  line-height: 1.5;
}

.message-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #bbb;
  padding: 0 4px;
}

.read-status {
  color: #52c41a;
}

/* 空状态 */
.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
  color: #ccc;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.6;
}

.empty-chat p {
  font-size: 14px;
  margin: 0;
}

/* ============================================
   底部输入区域
   ============================================ */
.input-area {
  background-color: #fff;
  border-top: 1px solid #f0f0f0;
  padding: 10px 16px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.tool-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.tool-btn:active,
.tool-btn.active {
  background-color: #f5f5f5;
  color: #FF6A00;
}

.tool-btn svg {
  width: 22px;
  height: 22px;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background-color: #f5f5f5;
  border-radius: 20px;
  padding: 8px 12px;
}

.message-input {
  flex: 1;
  border: none;
  background: none;
  font-size: 15px;
  color: #333;
  resize: none;
  outline: none;
  max-height: 120px;
  line-height: 1.5;
  font-family: inherit;
}

.message-input::placeholder {
  color: #bbb;
}

.send-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #e0e0e0;
  border-radius: 50%;
  color: #fff;
  transition: all 0.25s ease;
  flex-shrink: 0;
}

.send-btn svg {
  width: 18px;
  height: 18px;
}

.send-btn.active {
  background: linear-gradient(135deg, #FF6A00 0%, #FF8533 100%);
  box-shadow: 0 4px 12px rgba(255, 106, 0, 0.35);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 表情选择器 */
.emoji-picker {
  position: absolute;
  bottom: 80px;
  left: 16px;
  right: 16px;
  background-color: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
  animation: slideUp 0.2s ease;
  z-index: 10;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 8px;
}

.emoji-item {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.emoji-item:active {
  background-color: #f5f5f5;
  transform: scale(0.9);
}

/* 图片选择弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  align-items: flex-end;
}

.image-picker-panel {
  width: 100%;
  background-color: #fff;
  border-radius: 20px 20px 0 0;
  padding: 24px;
  animation: slideUpPanel 0.25s ease;
}

@keyframes slideUpPanel {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.panel-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  text-align: center;
  margin: 0 0 20px 0;
}

.picker-options {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-bottom: 24px;
}

.picker-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px;
  border-radius: 12px;
  background-color: #f5f5f5;
  cursor: pointer;
  transition: all 0.2s ease;
  min-width: 100px;
}

.picker-option:active {
  background-color: #e8e8e8;
  transform: scale(0.95);
}

.option-icon {
  font-size: 32px;
}

.option-text {
  font-size: 14px;
  color: #666;
}

.cancel-picker {
  width: 100%;
  padding: 14px;
  background-color: #f5f5f5;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  color: #666;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.cancel-picker:active {
  background-color: #e0e0e0;
}
</style>
