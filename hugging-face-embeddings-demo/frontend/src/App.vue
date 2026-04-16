<template>
  <div class="app">
    <h1>Hugging Face Embeddings 演示</h1>
    
    <!-- 导航菜单 -->
    <div class="nav-menu">
      <button 
        v-for="tab in tabs" 
        :key="tab.id"
        :class="['nav-btn', { active: currentTab === tab.id }]"
        @click="currentTab = tab.id"
      >
        {{ tab.name }}
      </button>
    </div>

    <!-- 单个文本嵌入 -->
    <div v-if="currentTab === 'single'" class="section">
      <h2>单个文本嵌入</h2>
      <div class="form">
        <textarea v-model="singleText" placeholder="输入文本..."></textarea>
        <button @click="getSingleEmbedding" :disabled="loading">
          {{ loading ? '处理中...' : '获取嵌入' }}
        </button>
        <div v-if="singleEmbedding" class="result">
          <h3>嵌入结果：</h3>
          <p><strong>维度：</strong>{{ singleEmbedding.dimension }}</p>
          <p><strong>前 10 个值：</strong>{{ singleEmbedding.embedding.slice(0, 10).join(', ') }}</p>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- 批量文本嵌入 -->
    <div v-if="currentTab === 'batch'" class="section">
      <h2>批量文本嵌入</h2>
      <div class="form">
        <textarea v-model="batchTexts" placeholder="输入多个文本，每行一个..."></textarea>
        <button @click="getBatchEmbeddings" :disabled="loading">
          {{ loading ? '处理中...' : '获取嵌入' }}
        </button>
        <div v-if="batchEmbeddings" class="result">
          <h3>嵌入结果：</h3>
          <div v-for="(embedding, index) in batchEmbeddings.embeddings" :key="index" class="embedding-item">
            <p><strong>文本 {{ index + 1 }}：</strong>{{ batchEmbeddings.texts[index] }}</p>
            <p><strong>维度：</strong>{{ embedding.length }}</p>
            <p><strong>前 10 个值：</strong>{{ embedding.slice(0, 10).join(', ') }}</p>
          </div>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- 文本相似度 -->
    <div v-if="currentTab === 'similarity'" class="section">
      <h2>文本相似度</h2>
      <div class="form">
        <textarea v-model="text1" placeholder="输入文本 1..."></textarea>
        <textarea v-model="text2" placeholder="输入文本 2..."></textarea>
        <button @click="calculateSimilarity" :disabled="loading">
          {{ loading ? '计算中...' : '计算相似度' }}
        </button>
        <div v-if="similarityResult" class="result">
          <h3>相似度结果：</h3>
          <p><strong>相似度：</strong>{{ similarityResult.similarity.toFixed(4) }}</p>
          <p><strong>解释：</strong>{{ getSimilarityExplanation(similarityResult.similarity) }}</p>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- 最相似文本 -->
    <div v-if="currentTab === 'most-similar'" class="section">
      <h2>最相似文本</h2>
      <div class="form">
        <textarea v-model="queryText" placeholder="输入查询文本..."></textarea>
        <textarea v-model="candidateTexts" placeholder="输入候选文本，每行一个..."></textarea>
        <button @click="findMostSimilar" :disabled="loading">
          {{ loading ? '搜索中...' : '查找最相似' }}
        </button>
        <div v-if="mostSimilarResult" class="result">
          <h3>最相似文本：</h3>
          <p><strong>查询文本：</strong>{{ mostSimilarResult.query }}</p>
          <p><strong>最相似文本：</strong>{{ mostSimilarResult.mostSimilar }}</p>
          <p><strong>相似度：</strong>{{ mostSimilarResult.similarity.toFixed(4) }}</p>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- 相似文本列表 -->
    <div v-if="currentTab === 'similar'" class="section">
      <h2>相似文本列表</h2>
      <div class="form">
        <textarea v-model="similarityQuery" placeholder="输入查询文本..."></textarea>
        <textarea v-model="similarityCandidates" placeholder="输入候选文本，每行一个..."></textarea>
        <div class="input-group">
          <label>相似度阈值：</label>
          <input type="number" v-model.number="similarityThreshold" min="0" max="1" step="0.05" value="0.7">
        </div>
        <button @click="findSimilar" :disabled="loading">
          {{ loading ? '搜索中...' : '查找相似' }}
        </button>
        <div v-if="similarResult" class="result">
          <h3>相似文本列表：</h3>
          <p><strong>查询文本：</strong>{{ similarResult.query }}</p>
          <p><strong>找到相似文本数量：</strong>{{ similarResult.count }}</p>
          <div v-for="(item, index) in similarResult.similarTexts" :key="index" class="similar-item">
            <p><strong>{{ index + 1 }}. 相似度：</strong>{{ item.similarity.toFixed(4) }}</p>
            <p><strong>文本：</strong>{{ item.text }}</p>
          </div>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- 健康检查 -->
    <div v-if="currentTab === 'health'" class="section">
      <h2>健康检查</h2>
      <div class="form">
        <button @click="checkHealth" :disabled="loading">
          {{ loading ? '检查中...' : '检查健康状态' }}
        </button>
        <div v-if="healthStatus" class="result">
          <h3>健康状态：</h3>
          <p><strong>状态：</strong>{{ healthStatus.status }}</p>
          <p><strong>时间戳：</strong>{{ new Date(healthStatus.timestamp).toLocaleString() }}</p>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- 缓存统计 -->
    <div v-if="currentTab === 'cache'" class="section">
      <h2>缓存统计</h2>
      <div class="form">
        <button @click="getCacheStats" :disabled="loading">
          {{ loading ? '获取中...' : '获取缓存统计' }}
        </button>
        <div v-if="cacheStats" class="result">
          <h3>缓存统计：</h3>
          <p><strong>当前缓存大小：</strong>{{ cacheStats.cacheSize }}</p>
          <p><strong>最大缓存大小：</strong>{{ cacheStats.maxCacheSize }}</p>
          <p><strong>Hugging Face 模型可用：</strong>{{ cacheStats.huggingFaceModelAvailable ? '是' : '否' }}</p>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'App',
  data() {
    return {
      currentTab: 'single',
      tabs: [
        { id: 'single', name: '单个文本嵌入' },
        { id: 'batch', name: '批量文本嵌入' },
        { id: 'similarity', name: '文本相似度' },
        { id: 'most-similar', name: '最相似文本' },
        { id: 'similar', name: '相似文本列表' },
        { id: 'health', name: '健康检查' },
        { id: 'cache', name: '缓存统计' }
      ],
      loading: false,
      error: null,
      // 单个文本嵌入
      singleText: '这是一个测试文本',
      singleEmbedding: null,
      // 批量文本嵌入
      batchTexts: '文本 1\n文本 2\n文本 3',
      batchEmbeddings: null,
      // 文本相似度
      text1: '人工智能是未来的发展方向',
      text2: 'AI 技术将改变世界',
      similarityResult: null,
      // 最相似文本
      queryText: '人工智能的应用',
      candidateTexts: '人工智能在医疗领域的应用\n机器学习的原理\n深度学习的发展\n人工智能在教育中的应用',
      mostSimilarResult: null,
      // 相似文本列表
      similarityQuery: '人工智能的应用',
      similarityCandidates: '人工智能在医疗领域的应用\n机器学习的原理\n深度学习的发展\n人工智能在教育中的应用\n人工智能在金融领域的应用',
      similarityThreshold: 0.7,
      similarResult: null,
      // 健康检查
      healthStatus: null,
      // 缓存统计
      cacheStats: null
    };
  },
  methods: {
    async getSingleEmbedding() {
      if (!this.singleText.trim()) {
        this.error = '文本不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.singleEmbedding = null;
      
      try {
        const response = await axios.post('/api/embedding/single', {
          text: this.singleText
        });
        this.singleEmbedding = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '获取嵌入失败';
      } finally {
        this.loading = false;
      }
    },
    
    async getBatchEmbeddings() {
      if (!this.batchTexts.trim()) {
        this.error = '文本列表不能为空';
        return;
      }
      
      const texts = this.batchTexts.split('\n').filter(text => text.trim());
      if (texts.length === 0) {
        this.error = '文本列表不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.batchEmbeddings = null;
      
      try {
        const response = await axios.post('/api/embedding/batch', {
          texts
        });
        this.batchEmbeddings = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '批量获取嵌入失败';
      } finally {
        this.loading = false;
      }
    },
    
    async calculateSimilarity() {
      if (!this.text1.trim() || !this.text2.trim()) {
        this.error = '两个文本都不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.similarityResult = null;
      
      try {
        const response = await axios.post('/api/embedding/similarity', {
          text1: this.text1,
          text2: this.text2
        });
        this.similarityResult = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '计算相似度失败';
      } finally {
        this.loading = false;
      }
    },
    
    async findMostSimilar() {
      if (!this.queryText.trim() || !this.candidateTexts.trim()) {
        this.error = '查询文本和候选文本都不能为空';
        return;
      }
      
      const texts = this.candidateTexts.split('\n').filter(text => text.trim());
      if (texts.length === 0) {
        this.error = '候选文本列表不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.mostSimilarResult = null;
      
      try {
        const response = await axios.post('/api/embedding/most-similar', {
          query: this.queryText,
          texts
        });
        this.mostSimilarResult = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '查找最相似文本失败';
      } finally {
        this.loading = false;
      }
    },
    
    async findSimilar() {
      if (!this.similarityQuery.trim() || !this.similarityCandidates.trim()) {
        this.error = '查询文本和候选文本都不能为空';
        return;
      }
      
      const texts = this.similarityCandidates.split('\n').filter(text => text.trim());
      if (texts.length === 0) {
        this.error = '候选文本列表不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.similarResult = null;
      
      try {
        const response = await axios.post('/api/embedding/similar', {
          query: this.similarityQuery,
          texts,
          threshold: this.similarityThreshold
        });
        this.similarResult = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '查找相似文本失败';
      } finally {
        this.loading = false;
      }
    },
    
    async checkHealth() {
      this.loading = true;
      this.error = null;
      this.healthStatus = null;
      
      try {
        const response = await axios.get('/api/embedding/health');
        this.healthStatus = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '健康检查失败';
      } finally {
        this.loading = false;
      }
    },
    
    async getCacheStats() {
      this.loading = true;
      this.error = null;
      this.cacheStats = null;
      
      try {
        const response = await axios.get('/api/embedding/cache/stats');
        this.cacheStats = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '获取缓存统计失败';
      } finally {
        this.loading = false;
      }
    },
    
    getSimilarityExplanation(similarity) {
      if (similarity >= 0.9) {
        return '非常相似';
      } else if (similarity >= 0.7) {
        return '比较相似';
      } else if (similarity >= 0.5) {
        return '有些相似';
      } else {
        return '不太相似';
      }
    }
  }
};
</script>

<style scoped>
.error {
  border-left-color: #e74c3c !important;
  background-color: #f8d7da !important;
}

.error h3 {
  color: #721c24 !important;
}
</style>
