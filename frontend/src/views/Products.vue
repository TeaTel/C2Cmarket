<template>
  <div class="products-page">
    <!-- 顶部搜索栏 -->
    <header class="search-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>

      <div class="search-bar">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <circle cx="11" cy="11" r="8"/>
          <path d="M21 21l-4.35-4.35"/>
        </svg>
        <input
          type="text"
          v-model="searchKeyword"
          class="search-input"
          placeholder="搜索商品..."
          @keyup.enter="handleSearch"
        />
      </div>
    </header>

    <!-- 筛选器 -->
    <section class="filter-section">
      <div class="filter-tabs">
        <button
          v-for="tab in filterTabs"
          :key="tab.key"
          @click="activeFilter = tab.key"
          :class="['filter-tab', { active: activeFilter === tab.key }]"
        >
          {{ tab.label }}
          <svg
            v-if="tab.sortable"
            class="sort-icon"
            :class="{ desc: sortOrder === 'desc' }"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <polyline points="6,9 12,3 18,9"/>
            <polyline points="6,15 12,21 18,15"/>
          </svg>
        </button>
      </div>

      <!-- 分类筛选下拉 -->
      <div v-if="showCategoryFilter" class="category-filter">
        <button
          v-for="cat in categories"
          :key="cat.id"
          @click="selectCategory(cat.id)"
          :class="['category-chip', { active: selectedCategoryId === cat.id }]"
        >
          {{ cat.name }}
        </button>
      </div>

      <!-- 价格筛选 -->
      <div v-if="showPriceFilter" class="price-filter">
        <div class="price-inputs">
          <input
            type="number"
            v-model.number="priceMin"
            placeholder="最低价"
            class="price-input"
          />
          <span class="price-separator">-</span>
          <input
            type="number"
            v-model.number="priceMax"
            placeholder="最高价"
            class="price-input"
          />
        </div>
        <button @click="applyPriceFilter" class="apply-btn">确定</button>
      </div>

      <!-- 成色筛选 -->
      <div v-if="showConditionFilter" class="condition-filter">
        <button
          v-for="cond in conditions"
          :key="cond.value"
          @click="selectedCondition = cond.value; applyFilters()"
          :class="['condition-chip', { active: selectedCondition === cond.value }]"
        >
          {{ cond.label }}
        </button>
      </div>
    </section>

    <!-- 商品网格 -->
    <main class="products-grid-container">
      <!-- 加载状态 -->
      <div v-if="loading && products.length === 0" class="loading-state">
        <div class="skeleton-grid">
          <div v-for="i in 4" :key="i" class="skeleton-card">
            <div class="skeleton-image"></div>
            <div class="skeleton-info">
              <div class="skeleton-text"></div>
              <div class="skeleton-price"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading && products.length === 0" class="empty-state">
        <div class="empty-icon">🔍</div>
        <p class="empty-text">没有找到相关商品</p>
        <button @click="resetFilters" class="reset-btn">重置筛选条件</button>
      </div>

      <!-- 商品列表 -->
      <div v-else class="products-grid">
        <div
          v-for="product in products"
          :key="product.id"
          @click="$router.push(`/products/${product.id}`)"
          class="product-card"
        >
          <div class="card-image-wrapper">
            <img
              :src="product.imageUrl || getDefaultImage()"
              :alt="product.name"
              class="card-image"
              loading="lazy"
            />
            <span v-if="product.condition" class="condition-badge">{{ product.condition }}</span>
          </div>

          <div class="card-info">
            <h4 class="card-title line-clamp-2">{{ product.name }}</h4>
            <div class="card-price-row">
              <span class="current-price">
                <small>¥</small>{{ formatPrice(product.price) }}
              </span>
              <span v-if="product.originalPrice > product.price" class="original-price">
                ¥{{ formatPrice(product.originalPrice) }}
              </span>
            </div>
            <div class="card-footer">
              <span class="seller-name">{{ product.sellerName || '匿名卖家' }}</span>
              <span class="publish-time">{{ getTimeAgo(product.createdAt) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 加载更多指示器 -->
      <div v-if="loadingMore" class="load-more-indicator">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>

      <!-- 已到底部 -->
      <div v-if="!hasMore && products.length > 0" class="end-reached">
        - 已经到底啦 -
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi } from '../services/api'

const route = useRoute()
const router = useRouter()

// 数据状态
const products = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)

// 搜索和筛选参数
const searchKeyword = ref('')
const activeFilter = ref('default')
const selectedCategoryId = ref(null)
const selectedCondition = ref('')
const priceMin = ref(null)
const priceMax = ref(null)
const sortOrder = ref('desc')

// 分页
const currentPage = ref(1)
const pageSize = 20

// 筛选器显示控制
const showCategoryFilter = ref(activeFilter.value === 'category')
const showPriceFilter = ref(activeFilter.value === 'price')
const showConditionFilter = ref(activeFilter.value === 'condition')

// 筛选选项配置
const filterTabs = [
  { key: 'default', label: '综合', sortable: false },
  { key: 'price', label: '价格', sortable: true },
  { key: 'time', label: '最新', sortable: true },
  { key: 'category', label: '分类', sortable: false },
  { key: 'condition', label: '成色', sortable: false }
]

const categories = [
  { id: null, name: '全部' },
  { id: 1, name: '数码电子' },
  { id: 2, name: '书籍教材' },
  { id: 3, name: '生活日用' },
  { id: 4, name: '服饰鞋包' },
  { id: 5, name: '美妆护肤' },
  { id: 6, name: '运动户外' }
]

const conditions = [
  { value: '', label: '全部' },
  { value: '全新', label: '全新' },
  { value: '几乎全新', label: '几乎全新' },
  { value: '轻微使用痕迹', label: '轻微使用痕迹' },
  { value: '明显使用痕迹', label: '明显使用痕迹' }
]

onMounted(async () => {
  // 从URL查询参数初始化筛选条件
  if (route.query.keyword) {
    searchKeyword.value = route.query.keyword
  }
  if (route.query.categoryId) {
    selectedCategoryId.value = parseInt(route.query.categoryId)
    activeFilter.value = 'category'
  }

  await loadProducts()

  // 监听滚动实现无限加载
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

// 监听筛选器切换
watch(activeFilter, (newVal) => {
  showCategoryFilter.value = newVal === 'category'
  showPriceFilter.value = newVal === 'price'
  showConditionFilter.value = newVal === 'condition'
})

// 加载商品
async function loadProducts(isLoadMore = false) {
  try {
    if (isLoadMore) {
      loadingMore.value = true
    } else {
      loading.value = true
    }

    const params = {
      page: currentPage.value,
      size: pageSize.value,
      status: 1 // 只获取在售商品
    }

    // 应用筛选条件
    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }
    if (selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
    }
    if (selectedCondition.value) {
      params.condition = selectedCondition.value
    }
    if (priceMin.value) {
      params.minPrice = priceMin.value
    }
    if (priceMax.value) {
      params.maxPrice = priceMax.value
    }

    // 排序
    if (activeFilter.value === 'price') {
      params.orderBy = 'price'
      params.sortOrder = sortOrder.value
    } else if (activeFilter.value === 'time') {
      params.orderBy = 'createdAt'
      params.sortOrder = 'desc'
    }

    const response = await productApi.getProducts(params)

    if (response.code === 200) {
      const newProducts = response.data?.records || []

      if (isLoadMore) {
        products.value = [...products.value, ...newProducts]
      } else {
        products.value = newProducts
      }

      const total = response.data?.total || 0
      hasMore.value = products.value.length < total
    }
  } catch (error) {
    console.error('加载商品失败:', error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  loadProducts()
}

function selectCategory(categoryId) {
  selectedCategoryId.value = categoryId
  applyFilters()
}

function applyPriceFilter() {
  applyFilters()
}

function applyFilters() {
  currentPage.value = 1
  loadProducts()
}

function resetFilters() {
  searchKeyword.value = ''
  activeFilter.value = 'default'
  selectedCategoryId.value = null
  selectedCondition.value = ''
  priceMin.value = null
  priceMax.value = null
  sortOrder.value = 'desc'
  currentPage.value = 1
  loadProducts()
}

// 无限滚动
function handleScroll() {
  if (loadingMore.value || !hasMore.value) return

  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  const scrollHeight = document.documentElement.scrollHeight
  const clientHeight = document.documentElement.clientHeight

  if (scrollTop + clientHeight >= scrollHeight - 150) {
    currentPage.value++
    loadProducts(true)
  }
}

// 工具函数
function formatPrice(price) {
  return Number(price).toLocaleString('zh-CN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  })
}

function getDefaultImage() {
  return 'https://via.placeholder.com/400x300/f5f5f5/999999?text=No+Image'
}

function getTimeAgo(timestamp) {
  if (!timestamp) return ''
  const diffMs = Date.now() - new Date(timestamp).getTime()
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)

  if (diffHour < 24) return `${diffHour}h前`
  if (diffDay < 30) return `${diffDay}天前`
  return new Date(timestamp).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
}
</script>

<style scoped>
.products-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* ============================================
   顶部搜索栏
   ============================================ */
.search-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  border-radius: 50%;
  flex-shrink: 0;
}

.back-btn svg { width: 22px; height: 22px; }

.search-bar {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  background-color: #f5f5f5;
  border-radius: 18px;
  padding: 8px 14px;
}

.search-icon {
  width: 18px;
  height: 18px;
  color: #999;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
  border: none;
  background: none;
  font-size: 14px;
  color: #333;
  outline: none;
}

/* ============================================
   筛选器
   ============================================ */
.filter-section {
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.filter-tabs {
  display: flex;
  gap: 4px;
  padding: 10px 16px;
  overflow-x: auto;
  scrollbar-width: none;
}

.filter-tabs::-webkit-scrollbar { display: none; }

.filter-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #666;
  background-color: #f5f5f5;
  border-radius: 16px;
  white-space: nowrap;
  transition: all 0.2s ease;
  border: none;
  cursor: pointer;
}

.filter-tab.active {
  background: linear-gradient(135deg, #FF6A00 0%, #FF8533 100%);
  color: white;
}

.filter-tab .sort-icon {
  width: 12px;
  height: 12px;
  opacity: 0.7;
  transition: transform 0.25s ease;
}

.filter-tab .sort-icon.desc {
  transform: rotate(180deg);
}

/* 分类筛选 */
.category-filter {
  padding: 12px 16px;
  border-top: 1px solid #f5f5f5;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.category-chip {
  padding: 6px 14px;
  font-size: 13px;
  color: #666;
  background-color: #f5f5f5;
  border-radius: 14px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-chip.active {
  background-color: #FFF7E6;
  color: #FA8C16;
  font-weight: 600;
}

/* 价格筛选 */
.price-filter {
  padding: 12px 16px;
  border-top: 1px solid #f5f5f5;
  display: flex;
  align-items: center;
  gap: 10px;
}

.price-inputs {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.price-input {
  width: 100px;
  padding: 8px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  text-align: center;
  outline: none;
  transition: border-color 0.2s ease;
}

.price-input:focus {
  border-color: #FF6A00;
}

.price-separator {
  color: #ccc;
  font-size: 14px;
}

.apply-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #FF6A00 0%, #FF8533 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

/* 成色筛选 */
.condition-filter {
  padding: 12px 16px;
  border-top: 1px solid #f5f5f5;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.condition-chip {
  padding: 6px 14px;
  font-size: 13px;
  color: #666;
  background-color: #f5f5f5;
  border-radius: 14px;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.condition-chip.active {
  background-color: #E6F7FF;
  color: #1890FF;
  font-weight: 600;
}

/* ============================================
   商品网格
   ============================================ */
.products-grid-container {
  padding: 12px 16px;
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.product-card {
  background-color: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.25s ease;
  cursor: pointer;
}

.product-card:active {
  transform: scale(0.97);
}

.card-image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background-color: #f5f5f5;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.product-card:hover .card-image {
  transform: scale(1.05);
}

.condition-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 3px 8px;
  background-color: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(4px);
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  color: #666;
}

.card-info {
  padding: 10px 12px;
}

.card-title {
  font-size: 13px;
  font-weight: 500;
  color: #333;
  line-height: 1.4;
  margin: 0 0 8px 0;
  min-height: 36px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 6px;
}

.current-price {
  font-size: 17px;
  font-weight: 700;
  color: #FF4D4F;
}

.current-price small {
  font-size: 12px;
  font-weight: 600;
}

.original-price {
  font-size: 11px;
  color: #bbb;
  text-decoration: line-through;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.seller-name {
  font-size: 11px;
  color: #999;
  max-width: 55%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.publish-time {
  font-size: 11px;
  color: #ccc;
}

/* ============================================
   加载和空状态
   ============================================ */
.loading-state { padding-top: 12px; }

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.skeleton-card {
  background-color: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.skeleton-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}

.skeleton-info { padding: 10px 12px; }

.skeleton-text {
  height: 36px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
  margin-bottom: 10px;
}

.skeleton-price {
  height: 20px;
  width: 70px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.empty-state {
  text-align: center;
  padding: 80px 32px;
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-text {
  font-size: 15px;
  color: #999;
  margin: 0 0 20px 0;
}

.reset-btn {
  background: linear-gradient(135deg, #FF6A00 0%, #FF8533 100%);
  color: white;
  padding: 10px 28px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
}

.load-more-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: #999;
  font-size: 13px;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 2px solid #e0e0e0;
  border-top-color: #FF6A00;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.end-reached {
  text-align: center;
  padding: 28px 0;
  color: #ccc;
  font-size: 13px;
}
</style>
