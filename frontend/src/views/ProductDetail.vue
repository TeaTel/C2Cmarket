<template>
  <div class="product-detail-page">
    <!-- 顶部导航 -->
    <header class="detail-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <h1 class="header-title">商品详情</h1>
      <button @click="toggleShareMenu" class="share-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="18" cy="5" r="3"/>
          <circle cx="6" cy="12" r="3"/>
          <circle cx="18" cy="19" r="3"/>
          <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
          <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
        </svg>
      </button>
    </header>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <div class="skeleton-image"></div>
      <div class="skeleton-info">
        <div class="skeleton-line long"></div>
        <div class="skeleton-price"></div>
        <div class="skeleton-line medium"></div>
        <div class="skeleton-line short"></div>
      </div>
    </div>

    <!-- 商品内容 -->
    <main v-else-if="product" class="detail-content">
      <!-- 图片轮播区 -->
      <section class="image-gallery">
        <div class="gallery-main">
          <img
            :src="currentImage"
            :alt="product.name"
            class="main-image"
            @click="previewImage(currentImage)"
          />
          <!-- 图片计数器 -->
          <span class="image-counter">{{ currentImageIndex + 1 }} / {{ product.images?.length || 1 }}</span>
        </div>
        <!-- 缩略图列表 -->
        <div v-if="product.images && product.images.length > 1" class="thumbnail-list">
          <div
            v-for="(img, index) in product.images"
            :key="index"
            @click="currentImageIndex = index"
            :class="['thumbnail-item', { active: currentImageIndex === index }]"
          >
            <img :src="img" alt="" class="thumbnail-img" />
          </div>
        </div>
      </section>

      <!-- 价格和基本信息 -->
      <section class="price-section">
        <div class="price-row">
          <span class="current-price">
            <small>¥</small>{{ formatPrice(product.price) }}
          </span>
          <span v-if="product.originalPrice && product.originalPrice > product.price" class="original-price">
            ¥{{ formatPrice(product.originalPrice) }}
          </span>
          <span v-if="getDiscount(product)" class="discount-tag">{{ getDiscount(product) }}折</span>
        </div>
        <h2 class="product-title">{{ product.name }}</h2>
        <div class="tags-row">
          <span v-if="product.condition" class="info-tag condition">{{ product.condition }}</span>
          <span v-if="product.categoryName" class="info-tag category">{{ product.categoryName }}</span>
          <span class="info-tag time">{{ getTimeAgo(product.createdAt) }}</span>
        </div>
      </section>

      <!-- 卖家信息卡片 -->
      <section class="seller-card" @click="viewSellerProfile">
        <div class="seller-avatar-wrapper">
          <img
            :src="product.sellerAvatar || defaultAvatar"
            :alt="product.sellerName"
            class="seller-avatar"
          />
          <span v-if="product.isOnline" class="online-dot"></span>
        </div>
        <div class="seller-info">
          <h4 class="seller-name">{{ product.sellerName || '匿名卖家' }}</h4>
          <p class="seller-stats">
            信用良好 · 发布{{ product.sellerProductCount || 0 }}件
          </p>
        </div>
        <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="9,18 15,12 9,6"/>
        </svg>
      </section>

      <!-- 商品描述 -->
      <section class="description-section">
        <h3 class="section-heading">商品描述</h3>
        <div class="description-content">
          {{ product.description || '暂无详细描述，请联系卖家了解更多~' }}
        </div>
      </section>

      <!-- 商品详情图片 -->
      <section v-if="product.detailImages && product.detailImages.length > 0" class="detail-images-section">
        <h3 class="section-heading">图文详情</h3>
        <div class="detail-images">
          <img
            v-for="(img, index) in product.detailImages"
            :key="index"
            :src="img"
            alt=""
            class="detail-img"
            @click="previewImage(img)"
          />
        </div>
      </section>

      <!-- 安全提示 -->
      <section class="safety-tips">
        <svg class="tips-icon" viewBox="0 0 24 24" fill="none" stroke="#FF6A00" stroke-width="2">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
          <polyline points="12,8 12,13 15,16"/>
        </svg>
        <div class="tips-content">
          <strong>交易安全提示</strong>
          <p>请使用平台聊天功能沟通，切勿私下转账，谨防诈骗</p>
        </div>
      </section>
    </main>

    <!-- 错误状态 -->
    <div v-else-if="error" class="error-state">
      <div class="error-icon">😕</div>
      <p class="error-text">{{ error }}</p>
      <button @click="loadProductDetail" class="retry-btn">重新加载</button>
    </div>

    <!-- 底部操作栏 -->
    <footer v-if="product" class="bottom-action-bar">
      <div class="action-left">
        <button @click="toggleFavorite" :class="['icon-btn', { active: isFavorited }]">
          <svg v-if="!isFavorited" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="#FF4D4F" stroke="#FF4D4F" stroke-width="2">
            <path d="M20.84 4.61a5.5 5.5 0 00-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 00-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 000-7.78z"/>
          </svg>
          <span>{{ isFavorited ? '已收藏' : '收藏' }}</span>
        </button>
      </div>

      <div class="action-right">
        <button @click="contactSeller" class="contact-btn secondary">
          聊一聊
        </button>
        <button @click="wantIt" class="contact-btn primary">
          我想要
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { productApi, messageApi } from '../services/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 数据状态
const product = ref(null)
const loading = ref(true)
const error = ref(null)
const currentImageIndex = ref(0)
const isFavorited = ref(false)

// 默认头像
const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIyMCIgZmlsbD0iI0UwRTBFRCIvPjxjaXJjbGUgY3g9IjIwIiBjeT0iMTciIHI9IjgiIGZpbGw9IndoaXRlIi8+PC9zdmc+'

// 当前显示的图片
const currentImage = computed(() => {
  if (!product.value) return ''
  const images = product.value.images || [product.value.imageUrl]
  return images[currentImageIndex.value] || images[0] || ''
})

onMounted(async () => {
  await loadProductDetail()
})

// 加载商品详情
async function loadProductDetail() {
  try {
    loading.value = true
    error.value = null

    const productId = route.params.id
    const response = await productApi.getProductDetail(productId)

    if (response.code === 200) {
      product.value = response.data

      // 确保images数组存在
      if (product.value.imageUrl && !product.value.images) {
        product.value.images = [product.value.imageUrl]
      }
    } else {
      error.value = response.message || '加载失败'
    }
  } catch (err) {
    console.error('加载商品详情失败:', err)
    error.value = '网络错误，请检查网络连接'
  } finally {
    loading.value = false
  }
}

// 核心功能：联系卖家 / 我想要
function contactSeller() {
  if (!authStore.isAuthenticated) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  router.push({
    path: `/chat/${product.value.sellerId}`,
    query: { productId: product.value.id }
  })
}

function wantIt() {
  if (!authStore.isAuthenticated) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }

  // 直接跳转到聊天，并带上预设消息
  router.push({
    path: `/chat/${product.value.sellerId}`,
    query: {
      productId: product.value.id,
      preMessage: `你好！我对这个【${product.value.name}】很感兴趣，请问还在吗？`
    }
  })
}

function toggleFavorite() {
  isFavorited.value = !isFavorited.value
  // TODO: 调用API保存收藏状态
}

function viewSellerProfile() {
  // TODO: 跳转到卖家主页
  console.log('查看卖家资料')
}

function toggleShareMenu() {
  // TODO: 显示分享菜单
  alert('分享功能开发中...')
}

function previewImage(url) {
  // TODO: 实现图片预览
  console.log('预览图片:', url)
}

// 工具函数
function formatPrice(price) {
  if (!price) return '0'
  return Number(price).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  })
}

function getDiscount(p) {
  if (!p.originalPrice || p.originalPrice <= p.price) return null
  const discount = Math.round((p.price / p.originalPrice) * 10)
  return discount
}

function getTimeAgo(timestamp) {
  if (!timestamp) return ''
  const diffMs = Date.now() - new Date(timestamp).getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)

  if (diffMin < 60) return `${diffMin}分钟前发布`
  if (diffHour < 24) return `${diffHour}小时前发布`
  if (diffDay < 30) return `${diffDay}天前发布`
  return new Date(timestamp).toLocaleDateString('zh-CN')
}
</script>

<style scoped>
.product-detail-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding-bottom: 70px; /* 底部操作栏高度 */
}

/* ============================================
   顶部导航
   ============================================ */
.detail-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.back-btn,
.share-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #333;
  transition: all 0.2s ease;
}

.back-btn:active,
.share-btn:active {
  background-color: #f5f5f5;
}

.back-btn svg,
.share-btn svg {
  width: 22px;
  height: 22px;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

/* ============================================
   加载状态
   ============================================ */
.loading-container {
  padding-top: 56px;
}

.skeleton-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-info {
  padding: 20px;
  background-color: #fff;
}

.skeleton-line {
  height: 18px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin-bottom: 14px;
}

.skeleton-line.long { width: 80%; }
.skeleton-line.medium { width: 60%; }
.skeleton-line.short { width: 40%; }

.skeleton-price {
  height: 32px;
  width: 150px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin-bottom: 14px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* ============================================
   详情内容
   ============================================ */
.detail-content {
  padding-top: 56px;
}

/* 图片轮播 */
.image-gallery {
  background-color: #fff;
}

.gallery-main {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background-color: #f5f5f5;
}

.main-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform 0.3s ease;
}

.image-counter {
  position: absolute;
  bottom: 12px;
  right: 12px;
  padding: 4px 10px;
  background-color: rgba(0, 0, 0, 0.55);
  color: white;
  font-size: 12px;
  border-radius: 12px;
  backdrop-filter: blur(4px);
}

.thumbnail-list {
  display: flex;
  gap: 8px;
  padding: 12px;
  overflow-x: auto;
}

.thumbnail-item {
  width: 64px;
  height: 64px;
  border-radius: 8px;
  overflow: hidden;
  opacity: 0.6;
  transition: all 0.2s ease;
  cursor: pointer;
  flex-shrink: 0;
  border: 2px solid transparent;
}

.thumbnail-item.active {
  opacity: 1;
  border-color: #FF6A00;
}

.thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 价格区域 */
.price-section {
  padding: 20px 16px;
  background-color: #fff;
  margin-top: 10px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.current-price {
  font-size: 32px;
  font-weight: 800;
  color: #FF4D4F;
}

.current-price small {
  font-size: 16px;
  font-weight: 700;
  margin-right: 2px;
}

.original-price {
  font-size: 15px;
  color: #bbb;
  text-decoration: line-through;
}

.discount-tag {
  padding: 3px 8px;
  background: linear-gradient(135deg, #FF4D4F 0%, #FF7875 100%);
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
}

.product-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
  line-height: 1.5;
  margin: 0 0 12px 0;
}

.tags-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.info-tag {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.info-tag.condition {
  background-color: #FFF7E6;
  color: #FA8C16;
}

.info-tag.category {
  background-color: #E6F7FF;
  color: #1890FF;
}

.info-tag.time {
  background-color: #F0F0F0;
  color: #999;
}

/* 卖家信息卡片 */
.seller-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background-color: #fff;
  margin-top: 10px;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.seller-card:active {
  background-color: #fafafa;
}

.seller-avatar-wrapper {
  position: relative;
  flex-shrink: 0;
}

.seller-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  background-color: #e0e0e0;
}

.online-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 10px;
  height: 10px;
  background-color: #52c41a;
  border: 2px solid #fff;
  border-radius: 50%;
}

.seller-info {
  flex: 1;
  min-width: 0;
}

.seller-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.seller-stats {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.arrow-icon {
  width: 18px;
  height: 18px;
  color: #ccc;
  flex-shrink: 0;
}

/* 描述区域 */
.description-section,
.detail-images-section {
  padding: 20px 16px;
  background-color: #fff;
  margin-top: 10px;
}

.section-heading {
  font-size: 17px;
  font-weight: 700;
  color: #333;
  margin: 0 0 14px 0;
}

.description-content {
  font-size: 15px;
  line-height: 1.8;
  color: #666;
  white-space: pre-wrap;
}

.detail-images img {
  width: 100%;
  margin-bottom: 8px;
  border-radius: 8px;
}

/* 安全提示 */
.safety-tips {
  display: flex;
  gap: 12px;
  padding: 16px;
  background-color: #FFF7E6;
  margin-top: 10px;
  border-radius: 12px;
  align-items: flex-start;
}

.tips-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  margin-top: 2px;
}

.tips-content strong {
  display: block;
  font-size: 14px;
  color: #FA8C16;
  margin-bottom: 4px;
}

.tips-content p {
  font-size: 13px;
  color: #999;
  margin: 0;
  line-height: 1.5;
}

/* 错误状态 */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 32px;
  text-align: center;
}

.error-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.error-text {
  font-size: 15px;
  color: #999;
  margin: 0 0 24px 0;
}

.retry-btn {
  background: linear-gradient(135deg, #FF6A00 0%, #FF8533 100%);
  color: white;
  padding: 12px 32px;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  cursor: pointer;
}

/* ============================================
   底部操作栏
   ============================================ */
.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  background-color: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.06);
}

.action-left {
  display: flex;
  gap: 8px;
}

.icon-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px 12px;
  color: #666;
  border-radius: 8px;
  transition: all 0.2s ease;
  font-size: 11px;
}

.icon-btn svg {
  width: 22px;
  height: 22px;
}

.icon-btn.active {
  color: #FF4D4F;
}

.action-right {
  display: flex;
  gap: 10px;
}

.contact-btn {
  padding: 12px 28px;
  border-radius: 22px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.25s ease;
}

.contact-btn.secondary {
  background-color: #FFF7E6;
  color: #FA8C16;
  border: 1px solid #FFD591;
}

.contact-btn.secondary:active {
  background-color: #FFE7BA;
}

.contact-btn.primary {
  background: linear-gradient(135deg, #FF6A00 0%, #FF8533 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 106, 0, 0.35);
}

.contact-btn.primary:active {
  transform: scale(0.96);
  box-shadow: 0 2px 8px rgba(255, 106, 0, 0.45);
}
</style>
