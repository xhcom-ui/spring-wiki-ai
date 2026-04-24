<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <div class="logo">
            <el-icon class="logo-icon"><DataAnalysis /></el-icon>
            <h2>数据同步平台</h2>
          </div>
          <p>请登录系统</p>
        </div>
      </template>
      <el-form 
        :model="loginForm" 
        :rules="rules" 
        ref="loginFormRef" 
        label-width="80px"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入用户名"
            prefix-icon="User"
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="loginForm.remember" label="记住我" />
          <el-link type="primary" :underline="false" style="float: right">忘记密码?</el-link>
        </el-form-item>
        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleLogin" 
            style="width: 100%"
            :loading="loading"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tips">
        <div class="tips-header">
          <el-icon><InfoFilled /></el-icon>
          <span>测试账号</span>
        </div>
        <div class="tips-content">
          <p><strong>管理员:</strong> admin / admin123</p>
          <p><strong>普通用户:</strong> user / user123</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from '../utils/request'
import { ElMessage } from 'element-plus'
import { DataAnalysis, User, Lock, InfoFilled } from '@element-plus/icons-vue'
import { getFirstAccessibleHome } from '../utils/auth'

export default {
  name: 'Login',
  components: {
    DataAnalysis,
    User,
    Lock,
    InfoFilled
  },
  setup() {
    const router = useRouter()
    const loginFormRef = ref(null)
    const loading = ref(false)
    
    const loginForm = reactive({
      username: '',
      password: '',
      remember: false
    })
    
    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 20, message: '用户名长度在 3-20 个字符之间', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' },
        { min: 6, message: '密码长度至少 6 个字符', trigger: 'blur' }
      ]
    }
    
    const handleLogin = async () => {
      if (!loginFormRef.value) return
      
      try {
        await loginFormRef.value.validate()
        
        loading.value = true
        
        // 调用登录API
        const response = await axios.post('/auth/login', {
          username: loginForm.username,
          password: loginForm.password
        })
        
        const token = response.token ?? response.data?.token
        const user = response.user ?? response.data?.user

        // 保存token和用户信息到localStorage
        localStorage.setItem('so-token', token)
        localStorage.setItem('user', JSON.stringify(user))
        
        // 如果勾选了记住我，存储登录状态
        if (loginForm.remember) {
          localStorage.setItem('remember', 'true')
          localStorage.setItem('username', user?.name || user?.username || loginForm.username)
        } else {
          localStorage.removeItem('remember')
          localStorage.removeItem('username')
        }
        
        ElMessage.success('登录成功')
        
        router.push(getFirstAccessibleHome(user))
        
      } catch (error) {
        console.error('登录失败:', error)
        // 错误处理已在request.js中完成
      } finally {
        loading.value = false
      }
    }
    
    // 从localStorage加载记住的用户名
    const loadRememberedUser = () => {
      const remember = localStorage.getItem('remember')
      if (remember === 'true') {
        const username = localStorage.getItem('username')
        if (username) {
          loginForm.username = username
          loginForm.remember = true
        }
      }
    }
    
    onMounted(() => {
      loadRememberedUser()
    })
    
    return {
      loginForm,
      rules,
      loginFormRef,
      loading,
      handleLogin
    }
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  animation: gradient 15s ease infinite;
  background-size: 400% 400%;
}

@keyframes gradient {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.login-card {
  width: 100%;
  max-width: 400px;
  border-radius: 12px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  transition: all 0.3s ease;
}

.login-card:hover {
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  transform: translateY(-5px);
}

.login-header {
  text-align: center;
  padding: 30px 0;
  background-color: rgba(255, 255, 255, 0.95);
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 15px;
}

.logo-icon {
  font-size: 32px;
  color: #409EFF;
  margin-right: 12px;
}

.login-header h2 {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.login-header p {
  margin: 10px 0 0 0;
  color: #909399;
  font-size: 14px;
}

.login-tips {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 6px;
  border-left: 4px solid #409EFF;
}

.tips-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  color: #409EFF;
  font-weight: 500;
}

.tips-header span {
  margin-left: 8px;
}

.tips-content {
  font-size: 13px;
  color: #606266;
}

.tips-content p {
  margin: 5px 0;
}

/* 响应式设计 */
@media screen and (max-width: 480px) {
  .login-card {
    max-width: 100%;
  }
  
  .login-header {
    padding: 20px 0;
  }
  
  .logo-icon {
    font-size: 24px;
  }
  
  .login-header h2 {
    font-size: 20px;
  }
}
</style>
