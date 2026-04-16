<template>
  <div class="app">
    <!-- 登录页面 -->
    <div v-if="!isLogin" class="login-page">
      <div class="login-form">
        <h1>FlowLong 工作流系统</h1>
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
          <input type="text" id="register-phone" v-model="registerForm.phone" placeholder="请输入电话" />
        </div>
        <button @click="register" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <p class="login-link" @click="showRegister = false">已有账号？点击登录</p>
        <div v-if="error" class="error-message">{{ error }}</div>
      </div>
    </div>

    <!-- 主页面 -->
    <div v-else class="main-page">
      <!-- 顶部导航 -->
      <div class="top-nav">
        <h1>FlowLong 工作流系统</h1>
        <div class="user-info">
          <span>{{ currentUser?.nickname || currentUser?.username }}</span>
          <button class="logout-btn" @click="logout">登出</button>
        </div>
      </div>

      <!-- 侧边菜单 -->
      <div class="sidebar">
        <ul class="menu-list">
          <li v-for="menu in menus" :key="menu.id" class="menu-item">
            <a href="#" @click="currentTab = menu.path">{{ menu.name }}</a>
          </li>
        </ul>
      </div>

      <!-- 主内容区 -->
      <div class="content">
        <!-- 流程画布 -->
        <div v-if="currentTab === 'canvas'" class="section">
          <h2>流程画布</h2>
          <div class="canvas-container">
            <div style="display: flex; gap: 20px;">
              <div id="bpmn-canvas" style="flex: 1; height: 600px; border: 1px solid #ddd;"></div>
              <div id="properties-panel" style="width: 300px; height: 600px; border: 1px solid #ddd;"></div>
            </div>
            <div class="canvas-actions">
              <button @click="saveProcess">保存流程</button>
              <button @click="deployProcess">部署流程</button>
            </div>
          </div>
        </div>

        <!-- 流程管理 -->
        <div v-if="currentTab === 'process'" class="section">
          <h2>流程管理</h2>
          <div class="form-group">
            <label>流程名称</label>
            <input type="text" v-model="processName" placeholder="请输入流程名称" />
          </div>
          <div class="form-group">
            <label>流程定义</label>
            <textarea v-model="processDefinition" placeholder="请输入流程定义 JSON" rows="10"></textarea>
          </div>
          <button @click="deployProcessFromJson" :disabled="loading || !processName || !processDefinition">
            {{ loading ? '部署中...' : '部署流程' }}
          </button>
        </div>

        <!-- 提交请假申请 -->
        <div v-if="currentTab === 'submit'" class="section">
          <h2>提交请假申请</h2>
          <div class="form">
            <div class="form-group">
              <label for="applicant">申请人</label>
              <input type="text" id="applicant" v-model="leaveForm.applicant" placeholder="请输入申请人姓名" />
            </div>
            <div class="form-group">
              <label for="deptManager">部门经理</label>
              <input type="text" id="deptManager" v-model="leaveForm.deptManager" placeholder="请输入部门经理姓名" />
            </div>
            <div class="form-group">
              <label for="generalManager">总经理</label>
              <input type="text" id="generalManager" v-model="leaveForm.generalManager" placeholder="请输入总经理姓名" />
            </div>
            <div class="form-group">
              <label for="startDate">开始日期</label>
              <input type="date" id="startDate" v-model="leaveForm.startDate" />
            </div>
            <div class="form-group">
              <label for="endDate">结束日期</label>
              <input type="date" id="endDate" v-model="leaveForm.endDate" />
            </div>
            <div class="form-group">
              <label for="days">请假天数</label>
              <input type="number" id="days" v-model="leaveForm.days" placeholder="请输入请假天数" />
            </div>
            <div class="form-group">
              <label for="reason">请假原因</label>
              <textarea id="reason" v-model="leaveForm.reason" placeholder="请输入请假原因"></textarea>
            </div>
            <button @click="submitLeave" :disabled="loading">
              {{ loading ? '提交中...' : '提交申请' }}
            </button>
            <div v-if="submitResult" class="result">
              <h3>提交结果：</h3>
              <p>申请已提交，流程实例ID：{{ submitResult.processInstanceId }}</p>
            </div>
            <div v-if="error" class="result error">
              <h3>错误：</h3>
              <p>{{ error }}</p>
            </div>
          </div>
        </div>

        <!-- 待办任务 -->
        <div v-if="currentTab === 'tasks'" class="section">
          <h2>待办任务</h2>
          <div class="form-group">
            <label for="assignee">任务负责人</label>
            <input type="text" id="assignee" v-model="assignee" placeholder="请输入任务负责人姓名" />
          </div>
          <button @click="getTasks" :disabled="loading">
            {{ loading ? '查询中...' : '查询待办任务' }}
          </button>
          <ul v-if="tasks.length > 0" class="task-list">
            <li v-for="task in tasks" :key="task.id" class="task-item">
              <h4>{{ task.name }}</h4>
              <p>流程实例ID：{{ task.processInstanceId }}</p>
              <p>任务ID：{{ task.id }}</p>
              <div class="task-actions">
                <button class="approval-btn" @click="approveTask(task.id, task.processInstanceId)">
                  批准
                </button>
                <button class="reject-btn" @click="rejectTask(task.id, task.processInstanceId)">
                  拒绝
                </button>
              </div>
            </li>
          </ul>
          <div v-else-if="!loading" class="result">
            <h3>暂无待办任务</h3>
          </div>
          <div v-if="error" class="result error">
            <h3>错误：</h3>
            <p>{{ error }}</p>
          </div>
        </div>

        <!-- 请假列表 -->
        <div v-if="currentTab === 'leaves'" class="section">
          <h2>请假列表</h2>
          <button @click="getLeaves" :disabled="loading">
            {{ loading ? '加载中...' : '刷新列表' }}
          </button>
          <ul v-if="leaves.length > 0" class="leave-list">
            <li v-for="leave in leaves" :key="leave.id" class="leave-item">
              <h4>{{ leave.applicant }} - {{ leave.days }}天</h4>
              <p>开始日期：{{ formatDate(leave.startDate) }}</p>
              <p>结束日期：{{ formatDate(leave.endDate) }}</p>
              <p>请假原因：{{ leave.reason }}</p>
              <p>状态：<span :class="['status-badge', 'status-' + leave.status.toLowerCase()]">{{ leave.status }}</span></p>
              <p>流程实例ID：{{ leave.processInstanceId }}</p>
            </li>
          </ul>
          <div v-else-if="!loading" class="result">
            <h3>暂无请假申请</h3>
          </div>
          <div v-if="error" class="result error">
            <h3>错误：</h3>
            <p>{{ error }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import propertiesPanelModule from 'bpmn-js-properties-panel';
import propertiesProviderModule from 'bpmn-js-properties-panel/lib/provider/camunda';
import camundaModdleDescriptor from 'camunda-bpmn-moddle/resources/camunda';

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
      currentUser: null,
      // 菜单
      menus: [
        { id: 1, name: '流程画布', path: 'canvas' },
        { id: 2, name: '流程管理', path: 'process' },
        { id: 3, name: '提交申请', path: 'submit' },
        { id: 4, name: '待办任务', path: 'tasks' },
        { id: 5, name: '请假列表', path: 'leaves' }
      ],
      currentTab: 'canvas',
      loading: false,
      error: null,
      // 流程管理
      processName: '',
      processDefinition: '',
      // BPMN 模型器
      bpmnModeler: null,
      // 提交请假申请
      leaveForm: {
        applicant: '',
        deptManager: '',
        generalManager: '',
        startDate: '',
        endDate: '',
        days: '',
        reason: ''
      },
      submitResult: null,
      // 待办任务
      assignee: '',
      tasks: [],
      // 请假列表
      leaves: []
    };
  },
  mounted() {
    this.checkLogin();
    this.initBpmnModeler();
  },
  methods: {
    // 检查登录状态
    async checkLogin() {
      try {
        const response = await axios.get('/api/auth/check');
        if (response.data.isLogin) {
          this.isLogin = true;
          await this.getCurrentUser();
        }
      } catch (error) {
        console.error('检查登录状态失败:', error);
      }
    },

    // 获取当前用户信息
    async getCurrentUser() {
      try {
        const response = await axios.get('/api/auth/current');
        this.currentUser = response.data;
      } catch (error) {
        console.error('获取用户信息失败:', error);
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
        localStorage.setItem('token', response.data.token);
        this.isLogin = true;
        this.currentUser = response.data.user;
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

    // 登出
    async logout() {
      try {
        await axios.post('/api/auth/logout');
        localStorage.removeItem('token');
        this.isLogin = false;
        this.currentUser = null;
        this.currentTab = 'canvas';
      } catch (error) {
        console.error('登出失败:', error);
      }
    },

    // 初始化 BPMN 模型器
    initBpmnModeler() {
      const container = document.getElementById('bpmn-canvas');
      if (!container) return;

      this.bpmnModeler = new BpmnModeler({
        container,
        propertiesPanel: {
          parent: '#properties-panel'
        },
        additionalModules: [
          propertiesPanelModule,
          propertiesProviderModule
        ],
        moddleExtensions: {
          camunda: camundaModdleDescriptor
        }
      });

      // 加载默认流程
      const defaultXml = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
             targetNamespace="http://bpmn.io/schema/bpmn">
  <process id="leave-process" name="请假流程" isExecutable="true">
    <startEvent id="startEvent" name="开始" />
    <userTask id="submitTask" name="提交申请" camunda:assignee="${applicant}" />
    <userTask id="deptTask" name="部门经理审批" camunda:assignee="${deptManager}" />
    <userTask id="generalTask" name="总经理审批" camunda:assignee="${generalManager}" />
    <endEvent id="endEvent" name="结束" />
    <sequenceFlow id="flow1" sourceRef="startEvent" targetRef="submitTask" />
    <sequenceFlow id="flow2" sourceRef="submitTask" targetRef="deptTask" />
    <sequenceFlow id="flow3" sourceRef="deptTask" targetRef="generalTask" />
    <sequenceFlow id="flow4" sourceRef="generalTask" targetRef="endEvent" />
  </process>
</definitions>`;

      this.bpmnModeler.importXML(defaultXml, (err) => {
        if (err) {
          console.error('加载默认流程失败:', err);
        }
      });
    },

    // 保存流程
    async saveProcess() {
      if (!this.bpmnModeler) return;

      try {
        const { xml } = await this.bpmnModeler.saveXML({ format: true });
        this.processDefinition = xml;
        this.error = '流程保存成功';
      } catch (error) {
        console.error('保存流程失败:', error);
        this.error = '保存流程失败: ' + error.message;
      }
    },

    // 部署流程
    async deployProcess() {
      if (!this.bpmnModeler) return;

      try {
        const { xml } = await this.bpmnModeler.saveXML({ format: true });
        const response = await axios.post('/api/flowlong/deploy', {
          processName: this.processName || '请假流程',
          modelContent: xml
        });
        this.error = response.data.message;
      } catch (error) {
        console.error('部署流程失败:', error);
        this.error = '部署流程失败: ' + error.message;
      }
    },

    // 从 JSON 部署流程
    async deployProcessFromJson() {
      if (!this.processName || !this.processDefinition) {
        this.error = '请输入流程名称和流程定义';
        return;
      }
      
      this.loading = true;
      this.error = null;
      
      try {
        const response = await axios.post('/api/flowlong/deploy', {
          processName: this.processName,
          modelContent: this.processDefinition
        });
        this.error = response.data.message;
      } catch (error) {
        this.error = error.response?.data?.error || '部署流程失败';
      } finally {
        this.loading = false;
      }
    },

    // 提交请假申请
    async submitLeave() {
      if (!this.leaveForm.applicant || !this.leaveForm.deptManager || !this.leaveForm.generalManager) {
        this.error = '请填写完整的申请信息';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.submitResult = null;
      
      try {
        const response = await axios.post('/api/flowlong/leave/start', this.leaveForm);
        this.submitResult = response.data;
        // 清空表单
        this.leaveForm = {
          applicant: '',
          deptManager: '',
          generalManager: '',
          startDate: '',
          endDate: '',
          days: '',
          reason: ''
        };
      } catch (error) {
        this.error = error.response?.data?.error || '提交申请失败';
      } finally {
        this.loading = false;
      }
    },
    
    // 获取待办任务
    async getTasks() {
      if (!this.assignee) {
        this.error = '请输入任务负责人';
        return;
      }
      
      this.loading = true;
      this.error = null;
      this.tasks = [];
      
      try {
        const response = await axios.get(`/api/flowlong/tasks/${this.assignee}`);
        this.tasks = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '查询任务失败';
      } finally {
        this.loading = false;
      }
    },
    
    // 批准任务
    async approveTask(taskId, processInstanceId) {
      this.loading = true;
      this.error = null;
      
      try {
        await axios.post('/api/flowlong/task/complete', {
          taskId,
          variables: {
            approved: true
          },
          processInstanceId,
          status: 'APPROVED'
        });
        // 重新查询任务
        this.getTasks();
      } catch (error) {
        this.error = error.response?.data?.error || '批准任务失败';
      } finally {
        this.loading = false;
      }
    },
    
    // 拒绝任务
    async rejectTask(taskId, processInstanceId) {
      this.loading = true;
      this.error = null;
      
      try {
        await axios.post('/api/flowlong/task/complete', {
          taskId,
          variables: {
            approved: false
          },
          processInstanceId,
          status: 'REJECTED'
        });
        // 重新查询任务
        this.getTasks();
      } catch (error) {
        this.error = error.response?.data?.error || '拒绝任务失败';
      } finally {
        this.loading = false;
      }
    },
    
    // 获取请假列表
    async getLeaves() {
      this.loading = true;
      this.error = null;
      this.leaves = [];
      
      try {
        const response = await axios.get('/api/flowlong/leaves');
        this.leaves = response.data;
      } catch (error) {
        this.error = error.response?.data?.error || '查询请假列表失败';
      } finally {
        this.loading = false;
      }
    },
    
    // 格式化日期
    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      return date.toLocaleDateString('zh-CN');
    }
  }
};
</script>
