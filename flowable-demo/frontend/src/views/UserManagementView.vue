<template>
  <div class="page-grid">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">User Directory</p>
          <h3>用户管理</h3>
        </div>
        <div class="management-meta">
          <span class="meta-pill">总用户 {{ total }}</span>
          <span class="meta-pill">启用 {{ enabledCount }}</span>
          <span class="meta-pill">禁用 {{ disabledCount }}</span>
        </div>
      </div>

      <div class="management-toolbar">
        <div class="management-filters">
          <label class="filter-grow">
            <span>搜索用户</span>
            <input
              v-model.trim="keyword"
              type="text"
              placeholder="搜索用户名、昵称、邮箱或手机号"
              @keyup.enter="searchUsers"
            />
          </label>
          <label class="compact-field">
            <span>状态</span>
            <select v-model="statusFilter">
              <option value="">全部状态</option>
              <option value="1">启用</option>
              <option value="0">禁用</option>
            </select>
          </label>
        </div>

        <div class="management-actions">
          <button type="button" class="ghost-btn" @click="searchUsers">搜索</button>
          <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
          <button type="button" class="primary-btn" @click="openCreate">新增用户</button>
        </div>
      </div>

      <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
      <p v-if="errorMessage || listError" class="feedback error">{{ errorMessage || listError }}</p>

      <table class="simple-table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>昵称</th>
            <th>角色</th>
            <th>状态</th>
            <th>联系方式</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.username }}</td>
            <td>{{ user.nickname || '-' }}</td>
            <td>{{ (user.roleNames || []).join(' / ') || '-' }}</td>
            <td>
              <span :class="['status-pill', user.status === 1 ? 'success-pill' : 'muted-pill']">
                {{ user.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td>{{ user.phone || user.email || '-' }}</td>
            <td>
              <div class="inline-actions">
                <button type="button" class="secondary-btn mini-btn" @click="editUser(user)">编辑</button>
                <button type="button" class="danger-btn mini-btn" @click="removeUser(user)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!users.length">
            <td colspan="6" class="empty-row">
              <div class="table-empty-state">
                <strong>没有匹配的用户数据</strong>
                <p>可以调整筛选条件，或者直接新增用户。</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <AppPagination
        :page="page"
        :size="size"
        :total="total"
        :disabled="loading"
        @update:page="changePage"
        @update:size="changeSize"
      />
    </section>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeDialog">
      <div class="modal-panel form-modal">
        <div class="panel-head">
          <div>
            <p class="eyebrow">User Editor</p>
            <h3>{{ form.id ? '编辑用户' : '新增用户' }}</h3>
          </div>
          <button type="button" class="ghost-btn" @click="closeDialog">关闭</button>
        </div>

        <form class="form-stack" @submit.prevent="submitUser">
          <div class="form-grid">
            <label>
              <span>用户名</span>
              <input v-model.trim="form.username" type="text" :disabled="Boolean(form.id)" />
            </label>
            <label>
              <span>昵称</span>
              <input v-model.trim="form.nickname" type="text" />
            </label>
            <label>
              <span>邮箱</span>
              <input v-model.trim="form.email" type="email" />
            </label>
            <label>
              <span>手机号</span>
              <input v-model.trim="form.phone" type="text" />
            </label>
            <label>
              <span>密码</span>
              <input v-model="form.password" type="password" :placeholder="form.id ? '留空则不修改' : '默认 123456'" />
            </label>
            <label>
              <span>状态</span>
              <select v-model.number="form.status">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </label>
          </div>

          <label>
            <span>角色</span>
            <div class="user-role-panel">
              <div class="user-role-toolbar">
                <span class="user-role-summary">已选 {{ form.roleIds.length }} / {{ roleOptions.length }}</span>
                <button type="button" class="ghost-btn mini-btn" @click="toggleAllRoles">
                  {{ isAllRolesChecked ? '取消全选' : '全选角色' }}
                </button>
              </div>
              <div class="user-role-list">
                <label v-for="role in roleOptions" :key="role.value" class="user-role-option">
                  <input v-model="form.roleIds" type="checkbox" :value="role.value" />
                  <span class="user-role-option-copy">
                    <strong>{{ role.name }}</strong>
                    <small>{{ role.code }}</small>
                  </span>
                </label>
              </div>
            </div>
          </label>

          <div class="action-row">
            <button type="submit" class="primary-btn" :disabled="loading">
              {{ loading ? '保存中...' : form.id ? '保存更新' : '创建用户' }}
            </button>
            <button type="button" class="secondary-btn" @click="resetForm">重置</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getCurrentUser, http, refreshCurrentUser } from '../api/http'
import AppPagination from '../components/common/AppPagination.vue'
import { usePagedQuery } from '../composables/usePagedQuery'

const dialogVisible = ref(false)
const roles = ref([])
const statusMessage = ref('')
const errorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('')
const {
  items: users,
  loading,
  error: listError,
  total,
  page,
  size,
  load: fetchUsers,
  search: searchUsersQuery,
  changePage,
  changeSize
} = usePagedQuery(async ({ page, size, keyword, status }) => {
  const { data } = await http.get('/users/query', {
    params: {
      page,
      size,
      keyword,
      status
    }
  })
  return data
}, {
  page: 1,
  size: 8
})

const form = reactive({
  id: null,
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  status: 1,
  roleIds: []
})

const enabledCount = computed(() => users.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => users.value.filter((item) => item.status !== 1).length)
const roleOptions = computed(() =>
  roles.value.map((role) => ({
    value: Number(role.id),
    name: role.name,
    code: role.code
  }))
)
const isAllRolesChecked = computed(() => {
  if (!roleOptions.value.length) {
    return false
  }
  return roleOptions.value.every((role) => form.roleIds.includes(role.value))
})

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function resetForm() {
  form.id = null
  form.username = ''
  form.password = ''
  form.nickname = ''
  form.email = ''
  form.phone = ''
  form.status = 1
  form.roleIds = []
}

function closeDialog() {
  dialogVisible.value = false
  resetForm()
}

function openCreate() {
  resetMessages()
  resetForm()
  dialogVisible.value = true
}

function toggleAllRoles() {
  form.roleIds = isAllRolesChecked.value ? [] : roleOptions.value.map((role) => role.value)
}

function editUser(user) {
  resetMessages()
  form.id = user.id
  form.username = user.username
  form.password = ''
  form.nickname = user.nickname || ''
  form.email = user.email || ''
  form.phone = user.phone || ''
  form.status = user.status ?? 1
  form.roleIds = [...(user.roleIds || [])]
  dialogVisible.value = true
}

async function fetchRoles() {
  const { data } = await http.get('/roles/options')
  roles.value = data || []
}

async function searchUsers() {
  try {
    await searchUsersQuery({
      keyword: keyword.value || undefined,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value)
    })
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  }
}

async function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  await searchUsers()
}

async function removeUser(user) {
  resetMessages()
  if (!window.confirm(`确认删除用户「${user.username}」吗？`)) {
    return
  }
  loading.value = true
  try {
    await http.delete(`/users/${user.id}`)
    statusMessage.value = '用户已删除'
    if ((page.value - 1) * size.value >= Math.max(0, total.value - 1) && page.value > 1) {
      page.value -= 1
    }
    await fetchUsers()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function submitUser() {
  resetMessages()
  loading.value = true
  try {
    const payload = {
      username: form.username,
      password: form.password,
      nickname: form.nickname,
      email: form.email,
      phone: form.phone,
      status: form.status,
      roleIds: form.roleIds
    }
    if (form.id) {
      await http.put(`/users/${form.id}`, payload)
      if (Number(form.id) === Number(getCurrentUser()?.id)) {
        await refreshCurrentUser()
      }
      statusMessage.value = '用户已更新'
    } else {
      await http.post('/users', payload)
      statusMessage.value = '用户已创建'
    }
    closeDialog()
    await fetchUsers()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    await Promise.all([fetchUsers(), fetchRoles()])
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '初始化用户列表失败'
  }
})
</script>

<style scoped>
.user-role-panel {
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}

.user-role-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border-bottom: 1px solid #e7edf4;
  background: #f8fafc;
}

.user-role-summary {
  font-size: 12px;
  color: #66788f;
}

.user-role-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 12px;
  max-height: 220px;
  overflow: auto;
}

.user-role-option {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.user-role-option input {
  width: auto;
  margin-top: 2px;
}

.user-role-option-copy {
  display: grid;
  gap: 2px;
}

.user-role-option-copy strong,
.user-role-option-copy small {
  margin: 0;
}

.user-role-option-copy small {
  color: #7d8a99;
}
</style>
