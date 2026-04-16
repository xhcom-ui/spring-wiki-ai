<template>
  <div class="app">
    <h1>深度研究系统</h1>
    
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

    <!-- 启动研究 -->
    <div v-if="currentTab === 'conduct'" class="section">
      <h2>启动研究</h2>
      <div class="form">
        <input 
          type="text" 
          v-model="researchTopic" 
          placeholder="输入研究主题..."
        />
        <textarea 
          v-model="researchDescription" 
          placeholder="输入研究描述（可选）..."
        ></textarea>
        <button @click="conductResearch" :disabled="loading">
          {{ loading ? '研究中...' : '启动研究' }}
        </button>
        <div v-if="researchResult" class="result">
          <h3>研究结果：</h3>
          <p><strong>摘要：</strong>{{ researchResult.summary }}</p>
          <p><strong>结论：</strong>{{ researchResult.conclusion }}</p>
          <p><strong>参考文献：</strong></p>
          <pre>{{ researchResult.references }}</pre>
        </div>
        <div v-if="error" class="result error">
          <h3>错误：</h3>
          <p>{{ error }}</p>
        </div>
      </div>
    </div>

    <!-- 查看研究主题 -->
    <div v-if="currentTab === 'topics'" class="section">
      <h2>研究主题列表</h2>
      <button @click="getAllTopics" :disabled="loading">
        {{ loading ? '加载中...' : '刷新列表' }}
      </button>
      <ul class="topics-list" v-if="topics.length > 0">
        <li 
          v-for="topic in topics" 
          :key="topic.id"
          class="topic-item"
          @click="getResearchResult(topic.id)"
        >
          <h3>{{ topic.topic }}</h3>
          <p>{{ topic.description || '无描述' }}</p>
          <div class="meta">
            创建时间：{{ new Date(topic.createdAt).toLocaleString() }}
          </div>
        </li>
      </ul>
      <div v-else-if="!loading" class="result">
        <h3>暂无研究主题</h3>
        <p>请先启动一个研究任务</p>
      </div>
      <div v-if="selectedResult" class="result" style="margin-top: 20px;">
        <h3>研究结果详情：</h3>
        <p><strong>摘要：</strong>{{ selectedResult.summary }}</p>
        <p><strong>结论：</strong>{{ selectedResult.conclusion }}</p>
        <p><strong>参考文献：</strong></p>
        <pre>{{ selectedResult.references }}</pre>
      </div>
      <div v-if="error" class="result error">
        <h3>错误：</h3>
        <p>{{ error }}</p>
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
      currentTab: 'conduct',
      tabs: [
        { id: 'conduct', name: '启动研究' },
        { id: 'topics', name: '研究主题' }
      ],
      loading: false,
      error: null,
      // 启动研究
      researchTopic: '',
      researchDescription: '',
      researchResult: null,
      // 研究主题列表
      topics: [],
      selectedResult: null
    };
  },
  mounted() {
    // 页面加载时获取研究主题列表
    this.getAllTopics();
  },
  methods: {
    async conductResearch() {
      if (!this.researchTopic.trim()) {
        this.error = '研究主题不能为空';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.researchResult = null;
      
      try {
        const response = await axios.post('/api/research/conduct', null, {
          params: {
            topic: this.researchTopic,
            description: this.researchDescription
          }
        });
        this.researchResult = response.data;
        // 重新获取研究主题列表
        this.getAllTopics();
      } catch (error) {
        this.error = error.response?.data?.error || '启动研究失败';
      } finally {
        this.loading = false;
      }
    },
    
    async getAllTopics() {
      this.loading = true;
      this.error = null;
      
      try {
        const response = await axios.get('/api/research/topics');
        this.topics = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '获取研究主题失败';
      } finally {
        this.loading = false;
      }
    },
    
    async getResearchResult(topicId) {
      this.loading = true;
      this.error = null;
      this.selectedResult = null;
      
      try {
        const response = await axios.get(`/api/research/result/${topicId}`);
        this.selectedResult = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '获取研究结果失败';
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>
