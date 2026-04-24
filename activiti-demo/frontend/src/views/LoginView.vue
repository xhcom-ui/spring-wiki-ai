<template>
  <section class="auth-screen">
    <div class="auth-panel auth-panel-extended">
      <div class="hero-copy auth-hero">
        <div class="auth-hero-head">
          <div>
            <p class="eyebrow">Activiti Studio</p>
            <h1>把流程设计、审批流转、运行时页面和监控统一到一套业务工作台。</h1>
          </div>
          <span class="auth-env-pill">Engine + BPMN + RBAC</span>
        </div>

        <p class="hero-text">
          登录后可进入首页、流程设计中心、请假发起、待办任务、流程编排、业务页目录、权限管理、版本对比和流程监控中心。
          当前前端已经按业务入口、审批动作、运行时页面和监控反馈做过统一编排，适合直接扩业务。
        </p>

        <div class="auth-feature-grid">
          <article class="auth-feature-card">
            <span class="auth-feature-kicker">Process Studio</span>
            <strong>流程设计与版本管理</strong>
            <p>在动态画布中维护 BPMN，直接发布、激活和回看定义版本。</p>
          </article>
          <article class="auth-feature-card">
            <span class="auth-feature-kicker">Approval Desk</span>
            <strong>发起申请与待办审批</strong>
            <p>把业务发起、待办处理和流程变量查看放到一条业务链路里。</p>
          </article>
          <article class="auth-feature-card">
            <span class="auth-feature-kicker">Runtime Catalog</span>
            <strong>业务页目录与运行时页面</strong>
            <p>统一管理待办页、完成页入口，并把页面配置接到审批运行时。</p>
          </article>
        </div>

        <div class="auth-journey">
          <article class="journey-step">
            <span>01</span>
            <div>
              <strong>进入工作台</strong>
              <p>登录后优先看到首页入口和今日处理重点。</p>
            </div>
          </article>
          <article class="journey-step">
            <span>02</span>
            <div>
              <strong>设计或发起流程</strong>
              <p>根据权限进入设计台、业务申请或待办处理页。</p>
            </div>
          </article>
          <article class="journey-step">
            <span>03</span>
            <div>
              <strong>联通运行时页面</strong>
              <p>在目录页配置待办页和完成页，再回到任务中心验证链路。</p>
            </div>
          </article>
        </div>
      </div>

      <div class="auth-card auth-card-elevated">
        <div class="auth-card-head">
          <div>
            <p class="eyebrow">Access Portal</p>
            <h3>{{ mode === 'login' ? '登录进入流程工作台' : '注册新账号' }}</h3>
          </div>
          <span class="auth-card-note">{{ mode === 'login' ? '已有账号可直接登录' : '注册后会自动切回登录' }}</span>
        </div>

        <div class="segmented">
          <button
            type="button"
            :class="['segment-btn', { active: mode === 'login' }]"
            @click="switchMode('login')"
          >
            登录
          </button>
          <button
            type="button"
            :class="['segment-btn', { active: mode === 'register' }]"
            @click="switchMode('register')"
          >
            注册
          </button>
        </div>

        <div class="auth-inline-hints">
          <span class="meta-pill">首页入口统一工作台</span>
          <span class="meta-pill">流程设计页支持动态画布</span>
          <span class="meta-pill">监控中心支持实例详情查看</span>
        </div>

        <div v-if="mode === 'login'" class="auth-account-panel">
          <div class="auth-account-head">
            <div>
              <span class="auth-account-kicker">Default Accounts</span>
              <strong>默认体验账号</strong>
            </div>
            <span class="auth-account-password">统一密码：123456</span>
          </div>

          <div class="auth-account-list">
            <button
              v-for="account in defaultAccounts"
              :key="account.username"
              type="button"
              class="auth-account-item"
              @click="fillDefaultAccount(account)"
            >
              <div>
                <strong>{{ account.username }}</strong>
                <p>{{ account.label }}</p>
              </div>
              <span>一键填充</span>
            </button>
          </div>
        </div>

        <form v-if="mode === 'login'" class="auth-form" @submit.prevent="login">
          <label>
            <span>用户名</span>
            <input v-model.trim="loginForm.username" type="text" placeholder="请输入用户名" />
          </label>
          <label>
            <span>密码</span>
            <input v-model="loginForm.password" type="password" placeholder="请输入密码" />
          </label>
          <button type="submit" class="primary-btn block-btn" :disabled="loading">
            {{ loading ? '登录中...' : '进入工作台' }}
          </button>
        </form>

        <form v-else class="auth-form auth-form-register" @submit.prevent="register">
          <label>
            <span>用户名</span>
            <input v-model.trim="registerForm.username" type="text" placeholder="例如 admin" />
          </label>
          <label>
            <span>昵称</span>
            <input v-model.trim="registerForm.nickname" type="text" placeholder="例如 系统管理员" />
          </label>
          <label>
            <span>邮箱</span>
            <input v-model.trim="registerForm.email" type="email" placeholder="name@example.com" />
          </label>
          <label>
            <span>手机号</span>
            <input v-model.trim="registerForm.phone" type="text" placeholder="请输入手机号" />
          </label>
          <label>
            <span>密码</span>
            <input v-model="registerForm.password" type="password" placeholder="请输入密码" />
          </label>
          <button type="submit" class="primary-btn block-btn" :disabled="loading">
            {{ loading ? '提交中...' : '完成注册' }}
          </button>
        </form>

        <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
        <p v-if="routeTip" class="feedback error">{{ routeTip }}</p>
        <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { clearCurrentUser, clearToken, http, saveCurrentUser, saveToken } from '../api/http'

const router = useRouter()
const route = useRoute()
const mode = ref('login')
const loading = ref(false)
const statusMessage = ref('')
const errorMessage = ref('')
const defaultAccounts = [
  { username: 'admin', password: '123456', label: '系统管理员，适合查看全部菜单与管理功能' },
  { username: 'manager', password: '123456', label: '审批经理，适合处理待办和审批流转' },
  { username: 'employee', password: '123456', label: '普通员工，适合发起申请和查看个人流程' }
]
const routeTip = computed(() => {
  if (route.query.reason === 'expired') {
    return '登录状态已失效，请重新登录后继续操作。'
  }
  if (route.query.denied === '1') {
    return '当前账号没有访问该页面的权限。'
  }
  return ''
})

const loginForm = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: ''
})

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function switchMode(target) {
  resetMessages()
  mode.value = target
}

function fillDefaultAccount(account) {
  resetMessages()
  loginForm.username = account.username
  loginForm.password = account.password
}

async function login() {
  resetMessages()
  loading.value = true
  try {
    const { data } = await http.post('/auth/login', loginForm)
    saveToken(data.token)
    const currentUser = data.user?.permissions && data.user?.menuPaths
      ? data.user
      : (await http.get('/auth/current')).data
    saveCurrentUser(currentUser)
    await router.replace(resolvePostLoginPath(currentUser))
  } catch (error) {
    clearToken()
    clearCurrentUser()
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function register() {
  resetMessages()
  loading.value = true
  try {
    await http.post('/auth/register', registerForm)
    statusMessage.value = '注册成功，请使用新账号登录'
    mode.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = registerForm.password
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

function resolvePostLoginPath(user) {
  const menuPaths = Array.isArray(user?.menuPaths) ? user.menuPaths : []
  if (user?.isAdmin || menuPaths.includes('/dashboard')) {
    return '/dashboard'
  }
  return menuPaths[0] || '/dashboard'
}
</script>

<style scoped>
.auth-panel-extended {
  grid-template-columns: minmax(0, 1.3fr) minmax(380px, 0.75fr);
}

.auth-hero {
  gap: 24px;
  background:
    radial-gradient(circle at top left, rgba(41, 79, 129, 0.16), transparent 28%),
    radial-gradient(circle at right bottom, rgba(47, 125, 50, 0.1), transparent 30%),
    rgba(255, 255, 255, 0.94);
}

.auth-hero-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.auth-env-pill {
  display: inline-flex;
  align-items: center;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(77, 98, 127, 0.12);
  color: #41546b;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.auth-feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.auth-feature-card,
.journey-step {
  border: 1px solid rgba(77, 98, 127, 0.12);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
}

.auth-feature-card {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.auth-feature-card strong,
.auth-feature-card p {
  margin: 0;
}

.auth-feature-kicker {
  color: #6f8299;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.auth-journey {
  display: grid;
  gap: 12px;
}

.journey-step {
  padding: 14px 16px;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.journey-step span {
  width: 44px;
  height: 44px;
  display: inline-grid;
  place-items: center;
  border-radius: 16px;
  background: rgba(41, 79, 129, 0.12);
  color: #2d5f98;
  font-weight: 700;
}

.journey-step strong,
.journey-step p {
  margin: 0;
}

.auth-card-elevated {
  gap: 16px;
  background: rgba(255, 255, 255, 0.96);
}

.auth-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.auth-card-head h3 {
  margin: 0;
}

.auth-card-note {
  color: #6f8299;
  font-size: 13px;
}

.auth-inline-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.auth-account-panel {
  display: grid;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(77, 98, 127, 0.12);
  background: rgba(255, 255, 255, 0.82);
}

.auth-account-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.auth-account-head strong,
.auth-account-head p,
.auth-account-item strong,
.auth-account-item p {
  margin: 0;
}

.auth-account-kicker {
  display: inline-block;
  margin-bottom: 4px;
  color: #6f8299;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.auth-account-password {
  color: #2d5f98;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.auth-account-list {
  display: grid;
  gap: 10px;
}

.auth-account-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid rgba(77, 98, 127, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  color: #24364c;
  text-align: left;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.auth-account-item:hover {
  transform: translateY(-1px);
  border-color: rgba(47, 118, 246, 0.28);
  box-shadow: 0 10px 24px rgba(47, 118, 246, 0.08);
}

.auth-account-item p {
  color: #6f7d90;
  font-size: 13px;
}

.auth-account-item span {
  color: #2f76f6;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.auth-form-register {
  gap: 12px;
}

@media (max-width: 1180px) {
  .auth-panel-extended {
    grid-template-columns: 1fr;
  }

  .auth-feature-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .auth-hero-head,
  .auth-card-head,
  .auth-account-head {
    flex-direction: column;
  }
}
</style>
