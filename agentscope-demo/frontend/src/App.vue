<template>
  <div class="app">
    <!-- 登录/注册页面 -->
    <div v-if="!isLogin" class="login-container">
      <!-- 登录表单 -->
      <div v-if="!showRegister" class="login-form">
        <h1>AgentScope 智能问答系统</h1>
        <div class="form-group">
          <label for="login-username">用户名</label>
          <input type="text" id="login-username" v-model="loginForm.username" placeholder="请输入用户名" />
        </div>
        <div class="form-group">
          <label for="login-password">密码</label>
          <input type="password" id="login-password" v-model="loginForm.password" placeholder="请输入密码" />
        </div>
        <button @click="login" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <p class="register-link" @click="showRegister = true">没有账号？点击注册</p>
        <div v-if="error" class="error-message">{{ error }}</div>
      </div>

      <!-- 注册表单 -->
      <div v-if="showRegister" class="register-form">
        <h1>注册账号</h1>
        <div class="form-group">
          <label for="register-username">用户名</label>
          <input type="text" id="register-username" v-model="registerForm.username" placeholder="请输入用户名" />
        </div>
        <div class="form-group">
          <label for="register-password">密码</label>
          <input type="password" id="register-password" v-model="registerForm.password" placeholder="请输入密码" />
        </div>
        <div class="form-group">
          <label for="register-nickname">昵称</label>
          <input type="text" id="register-nickname" v-model="registerForm.nickname" placeholder="请输入昵称" />
        </div>
        <div class="form-group">
          <label for="register-email">邮箱</label>
          <input type="email" id="register-email" v-model="registerForm.email" placeholder="请输入邮箱" />
        </div>
        <div class="form-group">
          <label for="register-phone">电话</label>
          <input type="tel" id="register-phone" v-model="registerForm.phone" placeholder="请输入电话" />
        </div>
        <button @click="register" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <p class="login-link" @click="showRegister = false">已有账号？点击登录</p>
        <div v-if="error" class="error-message">{{ error }}</div>
      </div>
    </div>

    <!-- 主页面 -->
    <div v-else>
      <h1>AgentScope 智能问答系统</h1>
      
      <!-- 创建对话 -->
      <div class="create-conversation">
        <h2>创建新对话</h2>
        <form @submit.prevent="createConversation">
          <input type="text" v-model="newConversationTitle" placeholder="请输入对话标题" required />
          <button type="submit" :disabled="loading">创建对话</button>
        </form>
      </div>

      <!-- 对话列表 -->
      <div class="conversation-list">
        <h2>对话列表</h2>
        <div v-for="conversation in conversations" :key="conversation.id" class="conversation-item" @click="selectConversation(conversation)">
          <h3>{{ conversation.title }}</h3>
          <p>创建时间: {{ formatDate(conversation.createdAt) }}</p>
        </div>
        <div v-if="conversations.length === 0" class="error-message">暂无对话，请创建新对话</div>
      </div>

      <!-- 聊天界面 -->
      <div v-if="selectedConversation" class="chat-container">
        <div class="chat-header">
          {{ selectedConversation.title }}
        </div>
        <div class="chat-messages">
          <div v-for="message in messages" :key="message.id" :class="['message', message.role === 'user' ? 'user-message' : 'agent-message']">
            {{ message.content }}
          </div>
          <div v-if="loading" class="message agent-message">
            <div class="loading"></div>
          </div>
        </div>
        <div class="chat-input">
          <input type="text" v-model="inputMessage" placeholder="请输入您的问题" @keyup.enter="sendMessage" />
          <button @click="sendMessage" :disabled="loading || !inputMessage">发送</button>
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
      // 登录注册
      isLogin: false,
      showRegister: false,
      loginForm: {
        username: '',
        password: ''
      },
      registerForm: {
        username: '',
        password: '',
        nickname: '',
        email: '',
        phone: ''
      },
      loading: false,
      error: null,
      // 对话相关
      conversations: [],
      selectedConversation: null,
      newConversationTitle: '',
      messages: [],
      inputMessage: '',
      currentUser: null
    };
  },
  mounted() {
    this.checkLogin();
  },
  methods: {
    // 检查登录状态
    async checkLogin() {
      try {
        // 这里应该检查本地存储中的 token
        const token = localStorage.getItem('token');
        if (token) {
          this.isLogin = true;
          await this.getConversations();
        }
      } catch (error) {
        console.error('检查登录状态失败:', error);
      }
    },

    // 登录
    async login() {
      if (!this.loginForm.username || !this.loginForm.password) {
        this.error = '请输入用户名和密码';
        return;
      }
      
      this.loading = true;
      this.error = null;
      
      try {
        const response = await axios.post('/api/auth/login', this.loginForm);
        this.currentUser = response.data.user;
        localStorage.setItem('token', 'mock-token'); // 模拟 token
        this.isLogin = true;
        await this.getConversations();
      } catch (error) {
        this.error = error.response?.data?.error || '登录失败';
      } finally {
        this.loading = false;
      }
    },

    // 注册
    async register() {
      if (!this.registerForm.username || !this.registerForm.password) {
        this.error = '请输入用户名和密码';
        return;
      }
      
      this.loading = true;
      this.error = null;
      
      try {
        const response = await axios.post('/api/auth/register', this.registerForm);
        this.error = '注册成功，请登录';
        this.showRegister = false;
      } catch (error) {
        this.error = error.response?.data?.error || '注册失败';
      } finally {
        this.loading = false;
      }
    },

    // 获取对话列表
    async getConversations() {
      try {
        const response = await axios.get('/api/conversations/user/1'); // 模拟用户 ID
        this.conversations = response.data;
      } catch (error) {
        console.error('获取对话列表失败:', error);
      }
    },

    // 创建对话
    async createConversation() {
      if (!this.newConversationTitle) {
        this.error = '请输入对话标题';
        return;
      }
      
      this.loading = true;
      this.error = null;
      
      try {
        const response = await axios.post('/api/conversations', {
          userId: 1, // 模拟用户 ID
          title: this.newConversationTitle
        });
        this.conversations.push(response.data);
        this.newConversationTitle = '';
        this.selectConversation(response.data);
      } catch (error) {
        console.error('创建对话失败:', error);
      } finally {
        this.loading = false;
      }
    },

    // 选择对话
    async selectConversation(conversation) {
      this.selectedConversation = conversation;
      await this.getMessages(conversation.id);
    },

    // 获取消息列表
    async getMessages(conversationId) {
      try {
        const response = await axios.get(`/api/messages/conversation/${conversationId}`);
        this.messages = response.data;
      } catch (error) {
        console.error('获取消息列表失败:', error);
      }
    },

    // 发送消息
    async sendMessage() {
      if (!this.inputMessage || !this.selectedConversation) {
        return;
      }
      
      const message = this.inputMessage;
      this.inputMessage = '';
      
      // 添加用户消息
      this.messages.push({
        id: Date.now(),
        role: 'user',
        content: message
      });
      
      this.loading = true;
      
      try {
        // 保存用户消息
        await axios.post('/api/messages', {
          conversationId: this.selectedConversation.id,
          role: 'user',
          content: message,
          type: 'text'
        });
        
        // 调用 Agent 处理请求
        const response = await axios.post('/api/agent/process', {
          query: message,
          history: this.messages.filter(m => m.role === 'user').map(m => m.content)
        });
        
        // 添加 Agent 回复
        const agentMessage = {
          id: Date.now() + 1,
          role: 'assistant',
          content: response.data.response
        };
        this.messages.push(agentMessage);
        
        // 保存 Agent 消息
        await axios.post('/api/messages', {
          conversationId: this.selectedConversation.id,
          role: 'assistant',
          content: response.data.response,
          type: 'text'
        });
      } catch (error) {
        console.error('发送消息失败:', error);
        this.messages.push({
          id: Date.now() + 1,
          role: 'assistant',
          content: '抱歉，处理您的请求时出现错误，请稍后重试。'
        });
      } finally {
        this.loading = false;
      }
    },

    // 格式化日期
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleString('zh-CN');
    }
  }
};
</script>
