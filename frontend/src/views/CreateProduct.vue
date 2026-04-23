<template>
  <div class="create-product-page">
    <!-- 顶部导航 -->
    <header class="page-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <h1 class="header-title">发布商品</h1>
      <button
        @click="handleSubmit"
        :disabled="submitting || !isFormValid"
        class="publish-btn"
        :class="{ active: isFormValid }"
      >
        {{ submitting ? '发布中...' : '发布' }}
      </button>
    </header>

    <!-- 表单内容 -->
    <main class="form-content">
      <!-- 图片上传区 -->
      <section class="upload-section">
        <div class="image-grid">
          <div
            v-for="(img, index) in imageList"
            :key="index"
            class="image-item"
          >
            <img :src="img" alt="" class="preview-image" />
            <button @click="removeImage(index)" class="remove-btn">
              <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <!-- 上传按钮 -->
          <label
            v-if="imageList.length < 9"
            class="upload-trigger"
          >
            <input
              type="file"
              accept="image/*"
              multiple
              @change="handleImageUpload"
              hidden
            />
            <div class="upload-icon-wrapper">
              <svg viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <circle cx="8.5" cy="8.5" r="1.5"/>
                <polyline points="21,15 16,10 5,21"/>
              </svg>
            </div>
            <span class="upload-text">{{ imageList.length }}/9</span>
          </label>
        </div>
        <p class="upload-tip">第一张为封面图，最多上传9张</p>
      </section>

      <!-- 商品信息 -->
      <section class="form-section">
        <div class="form-group">
          <label class="form-label required">商品标题</label>
          <input
            type="text"
            v-model="formData.name"
            placeholder="请输入商品标题（5-30字）"
            maxlength="30"
            class="form-input"
          />
          <span class="char-count">{{ formData.name.length }}/30</span>
        </div>

        <div class="form-group">
          <label class="form-label required">商品描述</label>
          <textarea
            v-model="formData.description"
            placeholder="描述一下商品的成色、购买时间、使用情况等..."
            rows="4"
            maxlength="500"
            class="form-textarea"
          ></textarea>
          <span class="char-count">{{ formData.description.length }}/500</span>
        </div>

        <div class="form-row">
          <div class="form-group half">
            <label class="form-label required">价格（元）</label>
            <div class="price-input-wrapper">
              <span class="price-prefix">¥</span>
              <input
                type="number"
                v-model.number="formData.price"
                placeholder="0.00"
                min="0"
                step="0.01"
                class="form-input price-input"
              />
            </div>
          </div>

          <div class="form-group half">
            <label class="form-label">原价（元）</label>
            <div class="price-input-wrapper">
              <span class="price-prefix">¥</span>
              <input
                type="number"
                v-model.number="formData.originalPrice"
                placeholder="选填"
                min="0"
                step="0.01"
                class="form-input price-input"
              />
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label required">分类</label>
          <select v-model="formData.categoryId" class="form-select">
            <option value="">请选择分类</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label required">成色</label>
          <div class="condition-options">
            <button
              v-for="cond in conditions"
              :key="cond.value"
              @click="formData.condition = cond.value"
              :class="['condition-option', { active: formData.condition === cond.value }]"
            >
              {{ cond.label }}
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">交易地点</label>
          <input
            type="text"
            v-model="formData.location"
            placeholder="如：图书馆门口、宿舍楼大厅等"
            class="form-input"
          />
        </div>
      </section>

      <!-- 发布须知 -->
      <section class="notice-section">
        <h3 class="notice-title">发布须知</h3>
        <ul class="notice-list">
          <li>请确保商品信息真实有效，禁止发布违禁品</li>
          <li>图片需清晰展示商品实际状况</li>
          <li>建议使用平台聊天功能沟通，保障交易安全</li>
          <li>平台有权下架违规或虚假商品</li>
        </ul>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { productApi } from '../services/api'

const router = useRouter()
const authStore = useAuthStore()

// 表单数据
const formData = ref({
  name: '',
  description: '',
  price: null,
  originalPrice: null,
  categoryId: '',
  condition: '几乎全新',
  location: ''
})

const imageList = ref([])
const submitting = ref(false)

// 分类选项
const categories = [
  { id: 1, name: '数码电子' },
  { id: 2, name: '书籍教材' },
  { id: 3, name: '生活日用' },
  { id: 4, name: '服饰鞋包' },
  { id: 5, name: '美妆护肤' },
  { id: 6, name: '运动户外' }
]

const conditions = [
  { value: '全新', label: '全新' },
  { value: '几乎全新', label: '几乎全新' },
  { value: '轻微使用痕迹', label: '轻微使用痕迹' },
  { value: '明显使用痕迹', label: '明显使用痕迹' }
]

// 表单验证
const isFormValid = computed(() => {
  return (
    formData.value.name.trim().length >= 5 &&
    formData.value.description.trim().length > 0 &&
    formData.value.price > 0 &&
    formData.value.categoryId !== '' &&
    formData.value.condition !== '' &&
    imageList.value.length > 0
  )
})

// 图片上传处理
function handleImageUpload(event) {
  const files = Array.from(event.target.files)
  
  files.forEach(file => {
    if (imageList.value.length >= 9) return
    
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      alert('请选择图片文件')
      return
    }

    // 验证文件大小（最大5MB）
    if (file.size > 5 * 1024 * 1024) {
      alert('图片大小不能超过5MB')
      return
    }

    // 创建预览URL
    const reader = new FileReader()
    reader.onload = (e) => {
      imageList.value.push(e.target.result)
    }
    reader.readAsDataURL(file)
  })

  event.target.value = ''
}

function removeImage(index) {
  imageList.value.splice(index, 1)
}

// 提交表单
async function handleSubmit() {
  if (!isFormValid.value || submitting.value) return

  try {
    submitting.value = true

    const productData = {
      ...formData.value,
      images: imageList.value,
      status: 1 // 在售状态
    }

    const response = await productApi.createProduct(productData)

    if (response.code === 200) {
      alert('发布成功！')
      router.push('/profile?tab=my-products')
    } else {
      alert(response.message || '发布失败，请稍后重试')
    }
  } catch (error) {
    console.error('发布失败:', error)
    alert('网络错误，请检查网络连接后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-product-page {
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
  justify-content: space-between;
  padding: 12px 16px;
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
}

.back-btn svg { width: 22px; height: 22px; }

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.publish-btn {
  padding: 8px 20px;
  background-color: #e0e0e0;
  color: #999;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.25s ease;
}

.publish-btn.active {
  background: linear-gradient(135deg, #FF6A00 0%, #FF8533 100%);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 106, 0, 0.35);
}

.publish-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* ============================================
   表单内容
   ============================================ */
.form-content {
  padding-bottom: 40px;
}

/* 图片上传 */
.upload-section {
  background-color: #fff;
  padding: 20px 16px;
  margin-top: 10px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.image-item {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: 12px;
  overflow: hidden;
  background-color: #f5f5f5;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  background-color: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.remove-btn svg { width: 12px; height: 12px; }
.remove-btn:active { background-color: rgba(255, 77, 79, 0.9); }

.upload-trigger {
  aspect-ratio: 1 / 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background-color: #fafafa;
  border: 2px dashed #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.upload-trigger:active {
  background-color: #FFF7E6;
  border-color: #FFD591;
}

.upload-icon-wrapper {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-icon-wrapper svg { width: 32px; height: 32px; }

.upload-text {
  font-size: 13px;
  color: #bbb;
}

.upload-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
  text-align: center;
}

/* 表单区域 */
.form-section {
  margin-top: 10px;
  background-color: #fff;
  padding: 8px 16px 20px;
}

.form-group {
  margin-top: 20px;
  position: relative;
}

.form-group.half {
  flex: 1;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.form-label.required::after {
  content: '*';
  color: #FF4D4F;
  margin-left: 3px;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  font-size: 15px;
  color: #333;
  outline: none;
  transition: all 0.25s ease;
  background-color: #fafafa;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: #FF6A00;
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(255, 106, 0, 0.08);
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #ccc;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
  line-height: 1.6;
}

.form-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='%23999' viewBox='0 0 16 16'%3E%3Cpath d='M8 11L3 6h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 14px center;
  padding-right: 36px;
}

.char-count {
  position: absolute;
  right: 0;
  bottom: -22px;
  font-size: 12px;
  color: #ccc;
}

/* 价格输入框 */
.price-input-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
  background-color: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.25s ease;
}

.price-input-wrapper:focus-within {
  border-color: #FF6A00;
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(255, 106, 0, 0.08);
}

.price-prefix {
  padding-left: 14px;
  font-size: 17px;
  font-weight: 700;
  color: #FF4D4F;
}

.price-input {
  border: none !important;
  background: none !important;
  padding: 12px 14px !important;
  box-shadow: none !important;
  font-weight: 600;
}

/* 成色选项 */
.condition-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.condition-option {
  padding: 9px 18px;
  background-color: #f5f5f5;
  border: 1px solid transparent;
  border-radius: 18px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.condition-option.active {
  background-color: #FFF7E6;
  border-color: #FFD591;
  color: #FA8C16;
  font-weight: 600;
}

.condition-option:active {
  transform: scale(0.95);
}

/* 发布须知 */
.notice-section {
  margin-top: 10px;
  background-color: #fff;
  padding: 20px 16px;
}

.notice-title {
  font-size: 16px;
  font-weight: 700;
  color: #333;
  margin: 0 0 14px 0;
}

.notice-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.notice-list li {
  position: relative;
  padding-left: 16px;
  font-size: 13px;
  color: #888;
  line-height: 2;
}

.notice-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background-color: #FF6A00;
}
</style>
