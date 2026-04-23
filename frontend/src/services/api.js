import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL
    ? `${import.meta.env.VITE_API_BASE_URL}/api`
    : 'http://localhost:8080/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加JWT令牌
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器 - 处理错误
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      let message = '请求失败'

      if (data && data.message) {
        message = data.message
      } else if (status === 401) {
        message = '未授权，请重新登录'
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        window.location.href = '/login'
      } else if (status === 403) {
        message = '权限不足'
      } else if (status === 404) {
        message = '请求的资源不存在'
      } else if (status >= 500) {
        message = '服务器内部错误'
      }

      console.error(`请求错误 ${status}:`, message)
      return Promise.reject({ message, status })
    } else if (error.request) {
      console.error('网络错误:', error.message)
      return Promise.reject({ message: '网络连接失败，请检查网络设置' })
    } else {
      console.error('请求配置错误:', error.message)
      return Promise.reject({ message: '请求配置错误' })
    }
  }
)

// ============================================
// 用户相关API
// ============================================
export const userApi = {
  register(data) {
    return api.post('/users/register', data)
  },

  login(data) {
    return api.post('/users/login', data)
  },

  getUserInfo() {
    return api.get('/users/info')
  },

  updateProfile(data) {
    return api.put('/users/profile', data)
  },

  changePassword(data) {
    return api.put('/users/password', data)
  },

  uploadAvatar(formData) {
    return api.post('/users/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

// ============================================
// 商品相关API
// ============================================
export const productApi = {
  getProducts(params) {
    return api.get('/products', { params })
  },

  getProductDetail(id) {
    return api.get(`/products/${id}`)
  },

  createProduct(data) {
    return api.post('/products', data)
  },

  updateProduct(id, data) {
    return api.put(`/products/${id}`, data)
  },

  deleteProduct(id) {
    return api.delete(`/products/${id}`)
  },

  getMyProducts() {
    return api.get('/products/my')
  },

  uploadImage(formData) {
    return api.post('/products/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  searchProducts(keyword) {
    return api.get('/products/search', { params: { keyword } })
  }
}

// ============================================
// 订单相关API
// ============================================
export const orderApi = {
  createOrder(data) {
    return api.post('/orders', data)
  },

  getOrderDetail(id) {
    return api.get(`/orders/${id}`)
  },

  getBuyerOrders(params) {
    return api.get('/orders/buyer', { params })
  },

  getSellerOrders(params) {
    return api.get('/orders/seller', { params })
  },

  updateOrderStatus(id, status) {
    return api.put(`/orders/${id}/status`, { status })
  },

  cancelOrder(id) {
    return api.put(`/orders/${id}/cancel`)
  },

  confirmOrder(id) {
    return api.put(`/orders/${id}/confirm`)
  }
}

// ============================================
// 消息/聊天相关API（增强版）
// ============================================
export const messageApi = {
  // 发送消息
  sendMessage(data) {
    return api.post('/messages', data)
  },

  // 获取对话记录（分页）
  getConversation(otherUserId, params = {}) {
    return api.get(`/messages/conversation/${otherUserId}`, { params })
  },

  // 获取联系人/会话列表
  getContacts() {
    return api.get('/messages/contacts')
  },

  // 获取未读消息列表
  getUnreadMessages() {
    return api.get('/messages/unread')
  },

  // 标记单条消息为已读
  markAsRead(messageId) {
    return api.put(`/messages/${messageId}/read`)
  },

  // 标记整个对话为已读
  markConversationAsRead(senderId) {
    return api.put(`/messages/conversation/${senderId}/read`)
  },

  // 获取未读消息数量
  getUnreadCount() {
    return api.get('/messages/unread/count')
  },

  // 删除消息
  deleteMessage(messageId) {
    return api.delete(`/messages/${messageId}`)
  },

  // 撤回消息（发送后2分钟内）
  recallMessage(messageId) {
    return api.put(`/messages/${messageId}/recall`)
  },

  // 获取最近的消息预览（用于首页展示）
  getRecentMessages(limit = 10) {
    return api.get('/messages/recent', { params: { limit } })
  },

  // 创建会话（如果不存在则创建）
  createConversation(userId, productId) {
    return api.post('/messages/conversation', { userId, productId })
  },

  // 获取与某用户的会话ID
  getConversationId(otherUserId) {
    return api.get(`/messages/conversation-id/${otherUserId}`)
  }
}

// ============================================
// 分类相关API
// ============================================
export const categoryApi = {
  getCategories(params) {
    return api.get('/categories', { params })
  },

  getCategoryTree() {
    return api.get('/categories/tree')
  },

  getCategoryDetail(id) {
    return api.get(`/categories/${id}`)
  },

  createCategory(data) {
    return api.post('/categories', data)
  },

  updateCategory(id, data) {
    return api.put(`/categories/${id}`, data)
  },

  deleteCategory(id) {
    return api.delete(`/categories/${id}`)
  },

  // 获取分类下的商品
  getCategoryProducts(categoryId, params) {
    return api.get(`/categories/${categoryId}/products`, { params })
  }
}

// ============================================
// WebSocket连接管理（用于实时聊天）
// ============================================
class WebSocketManager {
  constructor() {
    this.ws = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.listeners = new Map()
    this.isConnected = false
  }

  connect(token) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.log('WebSocket already connected')
      return
    }

    const wsUrl = import.meta.env.VITE_WS_URL || 
      `ws://${window.location.hostname}:8080/ws/chat`

    try {
      this.ws = new WebSocket(`${wsUrl}?token=${token}`)

      this.ws.onopen = () => {
        console.log('WebSocket connected')
        this.isConnected = true
        this.reconnectAttempts = 0
        this.emit('connected')
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.emit('message', data)

          // 根据消息类型分发事件
          if (data.type) {
            this.emit(data.type, data)
          }
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e)
        }
      }

      this.ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        this.emit('error', error)
      }

      this.ws.onclose = () => {
        console.log('WebSocket disconnected')
        this.isConnected = false
        this.emit('disconnected')

        // 自动重连
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          this.reconnectAttempts++
          console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)
          setTimeout(() => this.connect(token), this.reconnectDelay * this.reconnectAttempts)
        }
      }
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error)
    }
  }

  disconnect() {
    if (this.ws) {
      this.ws.close()
      this.ws = null
      this.isConnected = false
    }
  }

  send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data))
      return true
    }
    console.warn('WebSocket is not connected')
    return false
  }

  // 发送聊天消息
  sendMessage(content, receiverId, productId = null) {
    return this.send({
      type: 'chat',
      content,
      receiverId,
      productId,
      timestamp: Date.now()
    })
  }

  on(event, callback) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event).push(callback)
  }

  off(event, callback) {
    if (this.listeners.has(event)) {
      const callbacks = this.listeners.get(event)
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  emit(event, data) {
    if (this.listeners.has(event)) {
      this.listeners.get(event).forEach(callback => {
        try {
          callback(data)
        } catch (e) {
          console.error(`Error in event handler for ${event}:`, e)
        }
      })
    }
  }
}

// 创建全局WebSocket实例
export const wsManager = new WebSocketManager()

export default api
