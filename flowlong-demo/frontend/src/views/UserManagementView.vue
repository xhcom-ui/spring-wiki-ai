<template>
  <div class="page-grid">
    <ManagementPageCard
      eyebrow="User Directory"
      title="用户管理"
      :status-message="statusMessage"
      :error-message="errorMessage || listError"
      :page="page"
      :size="size"
      :total="total"
      :loading="loading"
      @update:page="changePage"
      @update:size="changeSize"
    >
      <template #meta>
        <span class="meta-pill">总用户 {{ total }}</span>
        <span class="meta-pill">启用 {{ enabledCount }}</span>
        <span class="meta-pill">禁用 {{ disabledCount }}</span>
      </template>

      <template #filters>
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
      </template>

      <template #actions>
        <button type="button" class="ghost-btn" @click="searchUsers">搜索</button>
        <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
        <button type="button" class="primary-btn" @click="openCreate">新增用户</button>
      </template>

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
    </ManagementPageCard>

    <FormDialog
      :visible="dialogVisible"
      eyebrow="User Editor"
      :title="form.id ? '编辑用户' : '新增用户'"
      @close="closeDialog"
    >
      <form class="form-stack" @submit.prevent="submitUser">
        <SchemaFormFields :model="form" :fields="userFields" layout="grid" />
        <SchemaFormFields :model="form" :fields="[userRoleField]" />

        <div class="action-row">
          <button type="submit" class="primary-btn" :disabled="loading">
            {{ loading ? '保存中...' : form.id ? '保存更新' : '创建用户' }}
          </button>
          <button type="button" class="secondary-btn" @click="resetForm">重置</button>
        </div>
      </form>
    </FormDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { http } from '../api/http'
import FormDialog from '../components/common/FormDialog.vue'
import ManagementPageCard from '../components/common/ManagementPageCard.vue'
import SchemaFormFields from '../components/common/SchemaFormFields.vue'
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

const userFields = [
  { key: 'username', label: '用户名', type: 'text', disabled: (model) => Boolean(model.id) },
  { key: 'nickname', label: '昵称', type: 'text' },
  { key: 'email', label: '邮箱', type: 'email' },
  { key: 'phone', label: '手机号', type: 'text' },
  { key: 'password', label: '密码', type: 'password', placeholder: '默认 123456' },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    cast: 'number',
    options: [
      { label: '启用', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
]

const userRoleField = computed(() => ({
  key: 'roleIds',
  label: '角色',
  type: 'multiselect',
  size: 7,
  cast: 'number',
  options: roles.value.map((role) => ({
    label: `${role.name} (${role.code})`,
    value: role.id
  }))
}))

const enabledCount = computed(() => users.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => users.value.filter((item) => item.status !== 1).length)

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
