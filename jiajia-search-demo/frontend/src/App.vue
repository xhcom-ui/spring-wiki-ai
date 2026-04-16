<template>
  <div class="app">
    <h1>JiaJia-Search 演示</h1>
    
    <!-- 导航标签 -->
    <div class="tabs">
      <button 
        v-for="tab in tabs" 
        :key="tab.id"
        :class="['tab-btn', { active: currentTab === tab.id }]"
        @click="currentTab = tab.id"
      >
        {{ tab.name }}
      </button>
    </div>

    <!-- 生成单个嵌入 -->
    <div v-if="currentTab === 'single-embedding'" class="section">
      <h2>生成单个文本嵌入</h2>
      <div class="form">
        <textarea v-model="singleText" placeholder="输入文本..."></textarea>
        <button @click="generateSingleEmbedding" :disabled="loading">
          {{ loading ? '生成中...' : '生成嵌入' }}
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

    <!-- 批量生成嵌入 -->
    <div v-if="currentTab === 'batch-embedding'" class="section">
      <h2>批量生成文本嵌入</h2>
      <div class="form">
        <textarea v-model="batchTexts" placeholder="输入多个文本，每行一个..."></textarea>
        <button @click="generateBatchEmbeddings" :disabled="loading">
          {{ loading ? '生成中...' : '批量生成嵌入' }}
        </button>
        <div v-if="batchEmbeddings" class="result">
          <h3>嵌入结果：</h3>
          <div v-for="(embedding, index) in batchEmbeddings.embeddings" :key="index">
            <p><strong>文本 {{ index + 1 }}：</strong>{{ batchTexts.split('\n')[index] }}</p>
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

    <!-- 搜索流水线 -->
    <div v-if="currentTab === 'search-pipeline'" class="section">
      <h2>搜索流水线</h2>
      <div class="form">
        <input type="text" v-model="searchQuery" placeholder="输入查询文本..." />
        <textarea v-model="searchDocuments" placeholder="输入文档列表，每行一个..."></textarea>
        <button @click="runSearchPipeline" :disabled="loading">
          {{ loading ? '搜索中...' : '执行搜索' }}
        </button>
        <div v-if="searchResults" class="result">
          <h3>搜索结果：</h3>
          <div v-for="item in searchResults.results" :key="item.rank" class="search-result-item">
            <h4>
              <span>排名 {{ item.rank }}</span>
              <span :class="['relevance', item.relevance.toLowerCase().replace(' ', '-')]">
                {{ item.relevance }}
              </span>
            </h4>
            <p>{{ item.document }}</p>
            <div class="scores">
              <span>最终得分：{{ item.score.toFixed(4) }} | </span>
              <span>重排分：{{ item.rerank_score.toFixed(4) }}</span>
            </div>
          </div>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- 文档管理 -->
    <div v-if="currentTab === 'document'" class="section">
      <h2>文档管理</h2>
      <div class="form">
        <input type="text" v-model="documentTitle" placeholder="文档标题..." />
        <input type="text" v-model="documentType" placeholder="文档类型..." />
        <textarea v-model="documentContent" placeholder="文档内容..."></textarea>
        <button @click="saveDocument" :disabled="loading">
          {{ loading ? '保存中...' : '保存文档' }}
        </button>
        <div v-if="documentSaved" class="result">
          <h3>保存成功：</h3>
          <p>文档已成功保存</p>
        </div>
      </div>
    </div>

    <!-- 文档搜索 -->
    <div v-if="currentTab === 'document-search'" class="section">
      <h2>文档搜索</h2>
      <div class="form">
        <input type="text" v-model="documentSearchQuery" placeholder="输入搜索查询..." />
        <button @click="searchDocuments" :disabled="loading">
          {{ loading ? '搜索中...' : '搜索文档' }}
        </button>
        <div v-if="documentSearchResults" class="result">
          <h3>搜索结果：</h3>
          <div v-for="item in documentSearchResults.results" :key="item.rank" class="search-result-item">
            <h4>
              <span>排名 {{ item.rank }}</span>
              <span :class="['relevance', item.relevance.toLowerCase().replace(' ', '-')]">
                {{ item.relevance }}
              </span>
            </h4>
            <p>{{ item.document }}</p>
            <div class="scores">
              <span>最终得分：{{ item.score.toFixed(4) }} | </span>
              <span>重排分：{{ item.rerank_score.toFixed(4) }}</span>
            </div>
          </div>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- 文档列表 -->
    <div v-if="currentTab === 'document-list'" class="section">
      <h2>文档列表</h2>
      <button @click="getDocuments" :disabled="loading">
        {{ loading ? '加载中...' : '加载文档' }}
      </button>
      <ul v-if="documents.length > 0" class="documents-list">
        <li v-for="doc in documents" :key="doc.id" class="document-item">
          <h3>{{ doc.title }}</h3>
          <p>{{ doc.content.substring(0, 100) }}{{ doc.content.length > 100 ? '...' : '' }}</p>
          <div class="meta">
            <span>类型：{{ doc.type }}</span> | 
            <span>创建时间：{{ new Date(doc.createdAt).toLocaleString() }}</span>
          </div>
        </li>
      </ul>
      <div v-else-if="!loading" class="result">
        <h3>暂无文档</h3>
        <p>请先保存文档</p>
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
      currentTab: 'single-embedding',
      tabs: [
        { id: 'single-embedding', name: '单个文本嵌入' },
        { id: 'batch-embedding', name: '批量文本嵌入' },
        { id: 'search-pipeline', name: '搜索流水线' },
        { id: 'document', name: '保存文档' },
        { id: 'document-search', name: '文档搜索' },
        { id: 'document-list', name: '文档列表' }
      ],
      loading: false,
      error: null,
      // 单个嵌入
      singleText: '这是一个测试文本',
      singleEmbedding: null,
      // 批量嵌入
      batchTexts: '文本 1\n文本 2\n文本 3',
      batchEmbeddings: null,
      // 搜索流水线
      searchQuery: '苹果手机多少钱',
      searchDocuments: 'iPhone 15 售价多少\n苹果手机官方定价\n华为手机最新报价\n小米手机价格查询\n二手苹果手机多少钱',
      searchResults: null,
      // 文档管理
      documentTitle: '',
      documentType: '',
      documentContent: '',
      documentSaved: false,
      // 文档搜索
      documentSearchQuery: '',
      documentSearchResults: null,
      // 文档列表
      documents: []
    };
  },
  methods: {
    async generateSingleEmbedding() {
      if (!this.singleText.trim()) {
        this.error = '文本不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.singleEmbedding = null;
      
      try {
        const response = await axios.post('/api/jiajia/embedding/single', {
          text: this.singleText
        });
        this.singleEmbedding = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '生成嵌入失败';
      } finally {
        this.loading = false;
      }
    },
    
    async generateBatchEmbeddings() {
      const texts = this.batchTexts.split('\n').filter(text => text.trim());
      if (texts.length === 0) {
        this.error = '文本列表不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.batchEmbeddings = null;
      
      try {
        const response = await axios.post('/api/jiajia/embedding/batch', {
          texts
        });
        this.batchEmbeddings = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '批量生成嵌入失败';
      } finally {
        this.loading = false;
      }
    },
    
    async runSearchPipeline() {
      if (!this.searchQuery.trim() || !this.searchDocuments.trim()) {
        this.error = '查询和文档列表不能为空';
        return;
      }
      
      const documents = this.searchDocuments.split('\n').filter(doc => doc.trim());
      if (documents.length === 0) {
        this.error = '文档列表不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.searchResults = null;
      
      try {
        const response = await axios.post('/api/jiajia/search/pipeline', {
          query: this.searchQuery,
          documents
        });
        this.searchResults = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '搜索流水线失败';
      } finally {
        this.loading = false;
      }
    },
    
    async saveDocument() {
      if (!this.documentTitle.trim() || !this.documentContent.trim()) {
        this.error = '标题和内容不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.documentSaved = false;
      
      try {
        await axios.post('/api/jiajia/document/save', {
          title: this.documentTitle,
          content: this.documentContent,
          type: this.documentType || 'unknown'
        });
        this.documentSaved = true;
        // 清空表单
        this.documentTitle = '';
        this.documentType = '';
        this.documentContent = '';
      } catch (error) {
        this.error = error.response?.data?.error || '保存文档失败';
      } finally {
        this.loading = false;
      }
    },
    
    async searchDocuments() {
      if (!this.documentSearchQuery.trim()) {
        this.error = '查询不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.documentSearchResults = null;
      
      try {
        const response = await axios.post('/api/jiajia/search/documents', {
          query: this.documentSearchQuery
        });
        this.documentSearchResults = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '搜索文档失败';
      } finally {
        this.loading = false;
      }
    },
    
    async getDocuments() {
      this.loading = true;
      this.error = null;
      
      try {
        const response = await axios.get('/api/jiajia/documents');
        this.documents = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '获取文档列表失败';
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
