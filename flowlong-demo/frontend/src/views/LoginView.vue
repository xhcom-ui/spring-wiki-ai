<template>
  <section class="auth-screen">
    <div class="auth-panel auth-panel-extended">
      <div class="hero-copy auth-hero">
        <div class="auth-hero-head">
          <div>
            <p class="eyebrow">FlowLong Studio</p>
            <h1>把流程设计、审批流转和运行监控统一到一套业务工作台。</h1>
          </div>
          <span class="auth-env-pill">Engine + Studio + Monitor</span>
        </div>

        <p class="hero-text">
          登录后可进入首页、流程设计中心、请假发起、待办任务、权限管理、版本对比和流程监控中心。
          当前工作区已经按业务入口、审批动作和监控反馈做过统一编排，适合继续扩业务。
        </p>

        <div class="auth-feature-grid">
          <article class="auth-feature-card">
            <span class="auth-feature-kicker">Process Studio</span>
            <strong>流程设计与版本管理</strong>
            <p>在 FlowLong 设计台里维护 BPMN 与自定义画布版本，并直接发布、激活和回看历史。</p>
          </article>
          <article class="auth-feature-card">
            <span class="auth-feature-kicker">Approval Desk</span>
            <strong>发起申请与待办审批</strong>
            <p>把业务发起、待办处理和流程变量查看放在同一条业务链路里。</p>
          </article>
          <article class="auth-feature-card">
            <span class="auth-feature-kicker">Monitor Center</span>
            <strong>运行实例与轨迹审计</strong>
            <p>统一查看实例详情、变量快照、审批评论和 BPMN 高亮轨迹图。</p>
          </article>
        </div>

        <div class="auth-journey">
          <article class="journey-step">
            <span>01</span>
            <div>
              <strong>进入工作台</strong>
              <p>登录后优先看到首页入口和当前处理重点。</p>
            </div>
          </article>
          <article class="journey-step">
            <span>02</span>
            <div>
              <strong>设计或发起流程</strong>
              <p>根据权限进入设计台、业务申请页或待办处理页。</p>
            </div>
          </article>
          <article class="journey-step">
            <span>03</span>
            <div>
              <strong>监控运行结果</strong>
              <p>在监控中心回看实例、轨迹和节点审计快照。</p>
            </div>
          </article>
        </div>
      </div>

      <div class="auth-card auth-card-elevated">
        <div class="auth-card-head">
          <div>
            <p class="eyebrow">Access Portal</p>
            <h3>{{ mode === 'login' ? '登录进入 FlowLong 工作台' : '注册新账号' }}</h3>
          </div>
          <span class="auth-card-note">{{ mode === 'login' ? '已有账号可直接登录' : '注册完成后会自动切回登录' }}</span>
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
          <span class="meta-pill">首页业务入口统一编排</span>
          <span class="meta-pill">流程设计页支持动态画布</span>
          <span class="meta-pill">监控中心支持实例轨迹审计</span>
        </div>

        <div v-if="mode === 'login'" class="auth-account-tip">
          <strong>测试账号</strong>
          <span>可直接使用 `codex_test / 123456` 登录。</span>
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
    radial-gradient(circle at top left, rgba(61, 126, 255, 0.16), transparent 28%),
    radial-gradient(circle at right bottom, rgba(34, 193, 195, 0.12), transparent 30%),
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
  background: #f7faff;
  border: 1px solid #dce5f2;
  color: #426487;
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
  border: 1px solid #e2ebf6;
  border-radius: 14px;
  background: rgba(250, 252, 255, 0.96);
  padding: 16px 18px;
}

.auth-feature-card strong,
.journey-step strong {
  display: block;
  margin-bottom: 8px;
}

.auth-feature-card p,
.journey-step p {
  margin: 0;
  color: #6b7c93;
}

.auth-feature-kicker {
  display: inline-flex;
  margin-bottom: 10px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #3d7eff;
}

.auth-journey {
  display: grid;
  gap: 12px;
}

.journey-step {
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.journey-step span {
  width: 48px;
  height: 48px;
  display: inline-grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(61, 126, 255, 0.12);
  color: #2f63d6;
  font-weight: 700;
}

.auth-card-elevated {
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.auth-card-elevated::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 160px;
  background: linear-gradient(180deg, rgba(61, 126, 255, 0.08), rgba(61, 126, 255, 0));
  pointer-events: none;
}

.auth-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.auth-card-note {
  color: #7b8da6;
  font-size: 12px;
}

.auth-inline-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.auth-account-tip {
  display: grid;
  gap: 4px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid #d9e7ff;
  background: linear-gradient(135deg, rgba(246, 250, 255, 0.98), rgba(236, 244, 255, 0.94));
  color: #47607d;
}

.auth-account-tip strong {
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #2f63d6;
}

.auth-account-tip span {
  font-size: 13px;
}

.auth-form {
  display: grid;
  gap: 14px;
}

.auth-form label {
  display: grid;
  gap: 8px;
}

.auth-form label span {
  font-size: 13px;
  font-weight: 600;
  color: #42566f;
}

.auth-form :deep(input) {
  min-height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.98);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.auth-form :deep(input:focus) {
  outline: none;
  border-color: rgba(61, 126, 255, 0.6);
  box-shadow: 0 0 0 4px rgba(61, 126, 255, 0.12);
  transform: translateY(-1px);
}

.auth-form .primary-btn {
  margin-top: 6px;
  min-height: 50px;
  border-radius: 12px;
  border: 0;
  background: linear-gradient(135deg, #3d7eff, #2457d6);
  box-shadow: 0 16px 32px rgba(49, 100, 219, 0.22);
  transition: transform 0.18s ease, box-shadow 0.18s ease, filter 0.18s ease;
}

.auth-form .primary-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 18px 36px rgba(49, 100, 219, 0.28);
  filter: saturate(1.05);
}

@media (max-width: 1024px) {
  .auth-panel-extended {
    grid-template-columns: 1fr;
  }

  .auth-feature-grid {
    grid-template-columns: 1fr;
  }
}
</style>
