<template>
  <div class="layout" :class="{ 'sidebar-collapsed': isSidebarCollapsed }">
    <el-container>
      <el-aside :width="isSidebarCollapsed ? '64px' : '240px'" class="sidebar">
        <div class="logo" :class="{ 'collapsed': isSidebarCollapsed }">
          <h1 v-if="!isSidebarCollapsed">数据同步平台</h1>
          <div v-else class="logo-icon">
            <el-icon><DataAnalysis /></el-icon>
          </div>
        </div>
        <div class="sidebar-toggle" @click="toggleSidebar">
          <el-icon><component :is="isSidebarCollapsed ? ArrowRight : ArrowLeft" /></el-icon>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical-demo"
          @select="handleMenuSelect"
          :collapse="isSidebarCollapsed"
          background-color="#ffffff"
          text-color="#303133"
          active-text-color="#409EFF"
        >
          <el-menu-item v-for="item in visibleMenuItems" :key="item.key" :index="item.key">
            <el-icon><component :is="iconMap[item.icon]" /></el-icon>
            <span v-if="!isSidebarCollapsed">{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container class="main-container">
        <el-header class="header">
          <div class="header-left">
            <h2 class="page-title">{{ currentPageTitle }}</h2>
          </div>
          <div class="header-right">
            <div class="theme-toggle" @click="toggleTheme">
              <el-icon><component :is="isDarkTheme ? Sunny : Moon" /></el-icon>
            </div>
            <div class="user-info">
              <el-dropdown>
                <span class="user-dropdown">
                  <el-avatar>{{ displayName.charAt(0).toUpperCase() }}</el-avatar>
                  <span class="user-name">{{ username }}</span>
                  <el-tag size="small" :type="isAdminUser ? 'danger' : 'info'" effect="plain">{{ roleLabel }}</el-tag>
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="goToProfile">个人资料</el-dropdown-item>
                    <el-dropdown-item @click="logout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </el-header>
        <el-main class="main-content">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { clearAuthSession, getStoredUser, isAdmin } from '../utils/auth'
import { 
  DataBoard, 
  Connection, 
  Operation, 
  Timer, 
  Document, 
  Check, 
  VideoCamera, 
  Edit, 
  ArrowLeft, 
  ArrowRight, 
  DataAnalysis, 
  Moon, 
  Sunny, 
  ArrowDown 
} from '@element-plus/icons-vue'

export default {
  name: 'Layout',
  components: {
    DataBoard,
    Connection,
    Operation,
    Timer,
    Document,
    Check,
    VideoCamera,
    Edit,
    ArrowLeft,
    ArrowRight,
    DataAnalysis,
    Moon,
    Sunny,
    ArrowDown
  },
  setup() {
    const route = useRoute()
    const router = useRouter()
    const username = ref('admin')
    const currentUser = ref(null)
    const isSidebarCollapsed = ref(false)
    const isDarkTheme = ref(false)
    const menuItems = [
      { key: 'dashboard', title: '仪表盘', icon: 'DataBoard' },
      { key: 'datasource', title: '数据源管理', icon: 'Connection', roles: ['admin'] },
      { key: 'mapping', title: '映射配置', icon: 'Operation', roles: ['admin'] },
      { key: 'sync-task', title: '同步任务', icon: 'Timer' },
      { key: 'task-log', title: '任务日志', icon: 'Document' },
      { key: 'data-quality', title: '数据质量', icon: 'Check' },
      { key: 'realtime-monitor', title: '实时监控', icon: 'VideoCamera', roles: ['admin'] },
      { key: 'codegen', title: 'Watcher 配置', icon: 'Edit', roles: ['admin'] }
    ]
    const iconMap = {
      DataBoard,
      Connection,
      Operation,
      Timer,
      Document,
      Check,
      VideoCamera,
      Edit
    }
    const isAdminUser = computed(() => isAdmin(currentUser.value))
    const visibleMenuItems = computed(() => menuItems.filter(item => !item.roles || item.roles.includes(currentUser.value?.role)))
    
    // 当前页面标题
    const currentPageTitle = computed(() => {
      const current = visibleMenuItems.value.find(item => item.key === activeMenu.value)
      return current?.title || '数据同步平台'
    })
    
    // 激活的菜单
    const activeMenu = computed(() => {
      const path = route.path
      return path.split('/').pop() || 'dashboard'
    })

    const displayName = computed(() => username.value || 'U')
    const roleLabel = computed(() => isAdminUser.value ? '管理员' : '普通用户')
    
    // 切换侧边栏
    const toggleSidebar = () => {
      isSidebarCollapsed.value = !isSidebarCollapsed.value
      localStorage.setItem('sidebarCollapsed', isSidebarCollapsed.value)
    }
    
    // 切换主题
    const toggleTheme = () => {
      isDarkTheme.value = !isDarkTheme.value
      document.documentElement.classList.toggle('dark-theme', isDarkTheme.value)
      localStorage.setItem('darkTheme', isDarkTheme.value)
    }
    
    // 处理菜单选择
    const handleMenuSelect = (key) => {
      router.push(`/home/${key}`)
    }
    
    // 退出登录
    const logout = () => {
      clearAuthSession()
      router.push('/login')
    }
    
    // 跳转到个人资料
    const goToProfile = () => {
      ElMessage.info('个人资料页暂未开放')
    }
    
    // 初始化
    onMounted(() => {
      const storedUsername = localStorage.getItem('username')
      const storedUser = getStoredUser()
      currentUser.value = storedUser
      if (storedUser) {
        username.value = storedUser.name || storedUser.username || username.value
      } else if (storedUsername) {
        username.value = storedUsername
      }
      
      // 从本地存储获取侧边栏状态
      const storedSidebarState = localStorage.getItem('sidebarCollapsed')
      if (storedSidebarState !== null) {
        isSidebarCollapsed.value = storedSidebarState === 'true'
      }
      
      // 从本地存储获取主题状态
      const storedThemeState = localStorage.getItem('darkTheme')
      if (storedThemeState !== null) {
        isDarkTheme.value = storedThemeState === 'true'
        document.documentElement.classList.toggle('dark-theme', isDarkTheme.value)
      }
    })
    
    return {
      username,
      displayName,
      isSidebarCollapsed,
      isDarkTheme,
      ArrowLeft,
      ArrowRight,
      Moon,
      Sunny,
      activeMenu,
      visibleMenuItems,
      iconMap,
      currentPageTitle,
      roleLabel,
      isAdminUser,
      toggleSidebar,
      toggleTheme,
      handleMenuSelect,
      logout,
      goToProfile
    }
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
  transition: all 0.3s ease;
}

.sidebar {
  background-color: #ffffff;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.08);
  position: relative;
  transition: all 0.3s ease;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #409EFF;
  color: white;
  transition: all 0.3s ease;
}

.logo h1 {
  font-size: 18px;
  margin: 0;
  font-weight: 600;
}

.logo.collapsed {
  justify-content: center;
}

.logo-icon {
  font-size: 24px;
}

.sidebar-toggle {
  position: absolute;
  top: 20px;
  right: -12px;
  width: 24px;
  height: 24px;
  background-color: #409EFF;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 10;
  transition: all 0.3s ease;
}

.sidebar-toggle:hover {
  background-color: #66b1ff;
  transform: scale(1.1);
}

.el-menu-vertical-demo {
  height: calc(100vh - 64px);
  border-right: none;
}

.main-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  padding: 0 24px;
  z-index: 100;
}

.header-left .page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.theme-toggle {
  cursor: pointer;
  font-size: 18px;
  color: #606266;
  transition: all 0.3s ease;
}

.theme-toggle:hover {
  color: #409EFF;
  transform: scale(1.1);
}

.user-info {
  position: relative;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.3s ease;
}

.user-dropdown:hover {
  background-color: #f0f9eb;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background-color: #f5f7fa;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式设计 */
@media screen and (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 1000;
    transform: translateX(0);
  }
  
  .sidebar.collapsed {
    transform: translateX(-100%);
  }
  
  .main-content {
    padding: 16px;
  }
  
  .header-left .page-title {
    font-size: 16px;
  }
}

/* 深色主题 */
:global(.dark-theme) {
  --el-bg-color: #1a1a1a;
  --el-bg-color-page: #1a1a1a;
  --el-bg-color-overlay: #2a2a2a;
  --el-text-color-primary: #ffffff;
  --el-text-color-regular: #e0e0e0;
  --el-text-color-secondary: #a0a0a0;
  --el-border-color: #3a3a3a;
  --el-border-color-light: #4a4a4a;
  --el-border-color-lighter: #5a5a5a;
  --el-border-color-extra-light: #6a6a6a;
  --el-fill-color: #2a2a2a;
  --el-fill-color-light: #3a3a3a;
  --el-fill-color-lighter: #4a4a4a;
  --el-fill-color-extra-light: #5a5a5a;
  --el-color-white: #ffffff;
  --el-color-black: #000000;
}

:global(.dark-theme) .sidebar {
  background-color: #2a2a2a;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.3);
}

:global(.dark-theme) .main-content {
  background-color: #1a1a1a;
}

:global(.dark-theme) .header {
  background-color: #2a2a2a;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

:global(.dark-theme) .header-left .page-title {
  color: #ffffff;
}

:global(.dark-theme) .user-name {
  color: #e0e0e0;
}

:global(.dark-theme) .user-dropdown:hover {
  background-color: #3a3a3a;
}
</style>
