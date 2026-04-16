<template>
  <div class="layout">
    <el-container>
      <el-aside width="200px">
        <div class="logo">
          <h1>数据同步平台</h1>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical-demo"
          @select="handleMenuSelect"
        >
          <el-menu-item index="dashboard">
            <el-icon><Dashboard /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="datasource">
            <el-icon><Database /></el-icon>
            <span>数据源管理</span>
          </el-menu-item>
          <el-menu-item index="mapping">
            <el-icon><Operation /></el-icon>
            <span>映射配置</span>
          </el-menu-item>
          <el-menu-item index="sync-task">
            <el-icon><Timer /></el-icon>
            <span>同步任务</span>
          </el-menu-item>
          <el-menu-item index="task-log">
            <el-icon><Document /></el-icon>
            <span>任务日志</span>
          </el-menu-item>
          <el-menu-item index="data-quality">
            <el-icon><Check /></el-icon>
            <span>数据质量</span>
          </el-menu-item>
          <el-menu-item index="realtime-monitor">
            <el-icon><VideoCamera /></el-icon>
            <span>实时监控</span>
          </el-menu-item>
          <el-menu-item index="codegen">
            <el-icon><Edit /></el-icon>
            <span>代码生成</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header>
          <div class="header">
            <div class="user-info">
              <span>欢迎，{{ username }}</span>
              <el-button type="text" @click="logout">退出</el-button>
            </div>
          </div>
        </el-header>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { Dashboard, Database, Operation, Timer, Document, Check, VideoCamera, Edit } from '@element-plus/icons-vue'

export default {
  name: 'Layout',
  components: {
    Dashboard,
    Database,
    Operation,
    Timer,
    Document,
    Check,
    VideoCamera,
    Edit
  },
  data() {
    return {
      username: 'admin',
      activeMenu: 'dashboard'
    }
  },
  mounted() {
    // 从本地存储获取用户名
    const username = localStorage.getItem('username')
    if (username) {
      this.username = username
    }
    
    // 根据当前路由设置激活菜单
    const path = this.$route.path
    const menuMap = {
      '/home/dashboard': 'dashboard',
      '/home/datasource': 'datasource',
      '/home/mapping': 'mapping',
      '/home/sync-task': 'sync-task',
      '/home/task-log': 'task-log',
      '/home/data-quality': 'data-quality',
      '/home/realtime-monitor': 'realtime-monitor',
      '/home/codegen': 'codegen'
    }
    this.activeMenu = menuMap[path] || 'dashboard'
  },
  methods: {
    handleMenuSelect(key) {
      this.$router.push(`/home/${key}`)
    },
    logout() {
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #409EFF;
  color: white;
}

.logo h1 {
  font-size: 18px;
  margin: 0;
}

.el-menu-vertical-demo {
  height: calc(100vh - 60px);
}

.header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  height: 60px;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-info {
  margin-right: 20px;
}

.user-info span {
  margin-right: 10px;
}
</style>
