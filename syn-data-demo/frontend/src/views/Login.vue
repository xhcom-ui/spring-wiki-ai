<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="login-header">
          <h2>数据同步平台</h2>
          <p>请登录系统</p>
        </div>
      </template>
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" style="width: 100%">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="login-tips">
        <p>默认管理员账号：admin / admin123</p>
        <p>默认普通用户账号：user / user123</p>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import axios from '../utils/request'
import { ElMessage } from 'element-plus'

export default {
  name: 'Login',
  setup() {
    const router = useRouter()
    const loginFormRef = ref(null)
    
    const loginForm = reactive({
      username: '',
      password: ''
    })
    
    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' }
      ],
      password: [
        { required: true, message: '请输入密码', trigger: 'blur' }
      ]
    }
    
    const handleLogin = async () => {
      if (!loginFormRef.value) return
      
      try {
        await loginFormRef.value.validate()
        
        ElMessage.info('正在登录...')
        
        // 调用登录API
        const response = await axios.post('/api/auth/login', {
          username: loginForm.username,
          password: loginForm.password
        })
        
        // 保存token和用户信息到localStorage
        localStorage.setItem('so-token', response.data.token)
        localStorage.setItem('user', JSON.stringify(response.data.user))
        
        ElMessage.success('登录成功')
        
        // 跳转到首页
        router.push('/dashboard')
        
      } catch (error) {
        console.error('登录失败:', error)
        if (error.response) {
          ElMessage.error('登录失败: ' + error.response.data.message)
        } else {
          ElMessage.error('登录失败: ' + error.message)
        }
      }
    }
    
    return {
      loginForm,
      rules,
      loginFormRef,
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
}

.login-card {
  width: 400px;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.login-header {
  text-align: center;
  padding: 20px 0;
}

.login-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.login-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.login-tips {
  margin-top: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  color: #606266;
}

.login-tips p {
  margin: 5px 0;
}
</style>
