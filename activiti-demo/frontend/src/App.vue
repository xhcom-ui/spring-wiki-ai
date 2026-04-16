<template>
  <div class="app">
    <!-- 登录页面 -->
    <div v-if="!isLogin" class="login-page">
      <div class="login-form">
        <h1>工作流系统</h1>
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
        <h1>工作流系统</h1>
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
          <div class="canvas-controls">
            <div class="form-group">
              <label for="process-select">选择流程</label>
              <select id="process-select" v-model="selectedProcessId" @change="loadSelectedProcess">
                <option value="">新建流程</option>
                <option v-for="process in processDefinitions" :key="process.id" :value="process.id">
                  {{ process.processName }} (版本: {{ process.version }})
                </option>
              </select>
            </div>
          </div>
          <div class="canvas-container">
            <ProcessDesigner ref="designerRef" />
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
            <label>上传流程文件</label>
            <input type="file" @change="handleFileUpload" accept=".bpmn" />
          </div>
          <button @click="deployProcessFile" :disabled="loading || !selectedFile">
            {{ loading ? '部署中...' : '部署流程' }}
          </button>
          <div v-if="processDefinitions.length > 0" class="process-list">
            <h3>流程定义列表</h3>
            <table class="process-table">
              <thead>
                <tr>
                  <th>流程名称</th>
                  <th>流程ID</th>
                  <th>版本</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="definition in processDefinitions" :key="definition.id" :class="{ 'active-version': definition.status === 'ACTIVE' }">
                  <td>{{ definition.processName }}</td>
                  <td>{{ definition.processKey }}</td>
                  <td>{{ definition.version }}</td>
                  <td>
                    <span :class="['status-badge', 'status-' + definition.status.toLowerCase()]">{{ definition.status === 'ACTIVE' ? '活跃' : '非活跃' }}</span>
                  </td>
                  <td>
                    <button class="activate-btn" v-if="definition.status !== 'ACTIVE'" @click="activateProcess(definition.id)">激活</button>
                    <button class="delete-btn" @click="deleteProcess(definition.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
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

        <!-- 流程监控 -->
        <div v-if="currentTab === 'monitor'" class="section">
          <h2>流程监控</h2>
          <button @click="getProcessStatistics" :disabled="loading">
            {{ loading ? '加载中...' : '刷新统计' }}
          </button>
          
          <!-- 统计卡片 -->
          <div v-if="processStatistics" class="statistics-cards">
            <div class="stat-card">
              <h3>运行中流程</h3>
              <p class="stat-value">{{ processStatistics.runningProcessCount }}</p>
            </div>
            <div class="stat-card">
              <h3>已完成流程</h3>
              <p class="stat-value">{{ processStatistics.completedProcessCount }}</p>
            </div>
            <div class="stat-card">
              <h3>总任务数</h3>
              <p class="stat-value">{{ processStatistics.totalTaskCount }}</p>
            </div>
            <div class="stat-card">
              <h3>已分配任务</h3>
              <p class="stat-value">{{ processStatistics.assignedTaskCount }}</p>
            </div>
            <div class="stat-card">
              <h3>未分配任务</h3>
              <p class="stat-value">{{ processStatistics.unassignedTaskCount }}</p>
            </div>
          </div>

          <!-- 运行中流程 -->
          <div class="monitor-section">
            <h3>运行中流程</h3>
            <button @click="getRunningProcesses" :disabled="loading">
              {{ loading ? '加载中...' : '刷新' }}
            </button>
            <table v-if="runningProcesses.length > 0" class="monitor-table">
              <thead>
                <tr>
                  <th>流程ID</th>
                  <th>流程名称</th>
                  <th>开始时间</th>
                  <th>业务键</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="process in runningProcesses" :key="process.id">
                  <td>{{ process.id }}</td>
                  <td>{{ process.processDefinitionName }}</td>
                  <td>{{ formatDate(process.startTime) }}</td>
                  <td>{{ process.businessKey || '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else-if="!loading" class="result">
              <h4>暂无运行中流程</h4>
            </div>
          </div>

          <!-- 任务列表 -->
          <div class="monitor-section">
            <h3>任务列表</h3>
            <button @click="getAllTasks" :disabled="loading">
              {{ loading ? '加载中...' : '刷新' }}
            </button>
            <table v-if="allTasks.length > 0" class="monitor-table">
              <thead>
                <tr>
                  <th>任务ID</th>
                  <th>任务名称</th>
                  <th>负责人</th>
                  <th>创建时间</th>
                  <th>到期时间</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in allTasks" :key="task.id">
                  <td>{{ task.id }}</td>
                  <td>{{ task.name }}</td>
                  <td>{{ task.assignee || '未分配' }}</td>
                  <td>{{ formatDate(task.createTime) }}</td>
                  <td>{{ task.dueDate ? formatDate(task.dueDate) : '-' }}</td>
                </tr>
              </tbody>
            </table>
            <div v-else-if="!loading" class="result">
              <h4>暂无任务</h4>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import ProcessDesigner from './components/ProcessDesigner.vue';

export default {
  name: 'App',
  components: {
    ProcessDesigner
  },
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
        { id: 5, name: '请假列表', path: 'leaves' },
        { id: 6, name: '流程监控', path: 'monitor' }
      ],
      currentTab: 'canvas',
      loading: false,
      error: null,
      // 流程画布
      designerRef: null,
      selectedFile: null,
      processDefinitions: [],
      selectedProcessId: '',
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
      leaves: [],
      // 流程监控
      processStatistics: null,
      runningProcesses: [],
      allTasks: []
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
          await this.getProcessDefinitions();
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
        await this.getProcessDefinitions();
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

    // 加载选中的流程
    async loadSelectedProcess() {
      if (this.selectedProcessId) {
        try {
          await this.$refs.designerRef.loadProcessById(this.selectedProcessId);
          this.error = '流程加载成功';
        } catch (error) {
          this.error = '加载流程失败: ' + error.message;
        }
      }
    },

    // 保存流程
    async saveProcess() {
      try {
        const bpmnXml = await this.$refs.designerRef.getBpmnXml();
        console.log('保存流程:', bpmnXml);
        
        // 提取流程信息
        const processName = '请假流程';
        const processKey = 'leaveProcess';
        const userId = this.currentUser?.id || 1;
        
        // 保存到数据库
        const response = await axios.post('/api/process-definition/save', {
          processKey,
          processName,
          bpmnXml,
          userId
        });
        
        this.error = '流程保存成功';
        await this.getProcessDefinitions();
      } catch (error) {
        this.error = '保存流程失败: ' + error.message;
      }
    },

    // 部署流程
    async deployProcess() {
      this.loading = true;
      this.error = null;
      
      try {
        const bpmnXml = await this.$refs.designerRef.getBpmnXml();
        // 创建 Blob 对象
        const blob = new Blob([bpmnXml], { type: 'application/xml' });
        const formData = new FormData();
        formData.append('file', blob, 'process.bpmn');
        
        const response = await axios.post('/api/process/deploy', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });
        this.error = response.data.message;
        await this.getProcessDefinitions();
      } catch (error) {
        this.error = error.response?.data?.error || '部署流程失败';
      } finally {
        this.loading = false;
      }
    },

    // 处理文件上传
    handleFileUpload(event) {
      this.selectedFile = event.target.files[0];
    },

    // 部署流程文件
    async deployProcessFile() {
      if (!this.selectedFile) {
        this.error = '请选择流程文件';
        return;
      }
      
      this.loading = true;
      this.error = null;
      
      try {
        const formData = new FormData();
        formData.append('file', this.selectedFile);
        const response = await axios.post('/api/process/deploy', formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          }
        });
        this.error = response.data.message;
        await this.getProcessDefinitions();
      } catch (error) {
        this.error = error.response?.data?.error || '部署流程失败';
      } finally {
        this.loading = false;
      }
    },

    // 获取流程定义列表
    async getProcessDefinitions() {
      try {
        const response = await axios.get('/api/process-definition/all');
        this.processDefinitions = response.data;
      } catch (error) {
        console.error('获取流程定义列表失败:', error);
      }
    },

    // 删除流程定义
    async deleteProcess(id) {
      try {
        await axios.delete(`/api/process-definition/delete/${id}`);
        await this.getProcessDefinitions();
      } catch (error) {
        console.error('删除流程定义失败:', error);
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
        const response = await axios.post('/api/activiti/leave/start', this.leaveForm);
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
        const response = await axios.get(`/api/activiti/tasks/${this.assignee}`);
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
        await axios.post('/api/activiti/task/complete', {
          taskId,
          variables: {
            deptApproval: 'approved',
            generalApproval: 'approved'
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
        await axios.post('/api/activiti/task/complete', {
          taskId,
          variables: {
            deptApproval: 'rejected',
            generalApproval: 'rejected'
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
        const response = await axios.get('/api/activiti/leaves');
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
    },

    // 获取流程统计
    async getProcessStatistics() {
      this.loading = true;
      this.error = null;
      try {
        const response = await axios.get('/api/monitoring/statistics');
        this.processStatistics = response.data;
      } catch (error) {
        this.error = '获取流程统计失败: ' + error.message;
      } finally {
        this.loading = false;
      }
    },

    // 获取运行中流程
    async getRunningProcesses() {
      this.loading = true;
      this.error = null;
      try {
        const response = await axios.get('/api/monitoring/processes/running');
        this.runningProcesses = response.data;
      } catch (error) {
        this.error = '获取运行中流程失败: ' + error.message;
      } finally {
        this.loading = false;
      }
    },

    // 获取所有任务
    async getAllTasks() {
      this.loading = true;
      this.error = null;
      try {
        const response = await axios.get('/api/monitoring/tasks');
        this.allTasks = response.data;
      } catch (error) {
        this.error = '获取任务列表失败: ' + error.message;
      } finally {
        this.loading = false;
      }
    },

    // 激活流程版本
    async activateProcess(id) {
      this.loading = true;
      this.error = null;
      try {
        const userId = this.currentUser?.id || 1;
        await axios.put(`/api/process-definition/activate/${id}`, { userId });
        this.error = '流程版本激活成功';
        await this.getProcessDefinitions();
      } catch (error) {
        this.error = '激活流程版本失败: ' + error.message;
      } finally {
        this.loading = false;
      }
    }
  }
};
</script>

<style scoped>
.login-page {
  max-width: 400px;
  margin: 100px auto;
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.login-form, .register-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.login-form h1, .register-form h1 {
  text-align: center;
  color: #3498db;
}

.register-link, .login-link {
  text-align: center;
  color: #3498db;
  cursor: pointer;
  font-size: 14px;
}

.register-link:hover, .login-link:hover {
  text-decoration: underline;
}

.error-message {
  color: #e74c3c;
  font-size: 14px;
  text-align: center;
}

.main-page {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background-color: #3498db;
  color: white;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logout-btn {
  background-color: rgba(255, 255, 255, 0.2);
  border: 1px solid white;
  color: white;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.logout-btn:hover {
  background-color: rgba(255, 255, 255, 0.3);
}

.sidebar {
  width: 200px;
  background-color: #f8f9fa;
  border-right: 1px solid #ddd;
  padding: 20px 0;
  position: fixed;
  top: 60px;
  left: 0;
  bottom: 0;
  overflow-y: auto;
}

.menu-list {
  list-style: none;
}

.menu-item {
  padding: 10px 20px;
}

.menu-item a {
  text-decoration: none;
  color: #333;
  display: block;
  transition: all 0.3s ease;
}

.menu-item a:hover {
  color: #3498db;
  background-color: #e9ecef;
}

.content {
  margin-left: 200px;
  padding: 20px;
  flex: 1;
}

.canvas-controls {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border: 1px solid #ddd;
}

.canvas-container {
  margin-top: 20px;
}

.canvas-actions {
  margin-top: 10px;
  display: flex;
  gap: 10px;
}

.process-list {
  margin-top: 20px;
}

.process-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

.process-table th, .process-table td {
  padding: 10px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.process-table th {
  background-color: #f8f9fa;
  font-weight: 600;
}

.delete-btn {
  background-color: #e74c3c;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  margin-right: 5px;
}

.delete-btn:hover {
  background-color: #c0392b;
}

.activate-btn {
  background-color: #27ae60;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  margin-right: 5px;
}

.activate-btn:hover {
  background-color: #229954;
}

.active-version {
  background-color: #e8f5e8;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-active {
  background-color: #d4edda;
  color: #155724;
}

.status-inactive {
  background-color: #f8d7da;
  color: #721c24;
}

/* 流程监控样式 */
.statistics-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin: 20px 0;
}

.stat-card {
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #3498db;
  margin-top: 10px;
}

.monitor-section {
  margin: 30px 0;
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.monitor-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 15px;
}

.monitor-table th, .monitor-table td {
  padding: 10px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.monitor-table th {
  background-color: #f8f9fa;
  font-weight: 600;
}

@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    position: relative;
    top: 0;
    left: 0;
    bottom: auto;
    border-right: none;
    border-bottom: 1px solid #ddd;
  }
  
  .content {
    margin-left: 0;
  }
}
</style>
