<template>
  <div class="page-grid">
    <ManagementPageCard
      eyebrow="Role Matrix"
      title="角色管理"
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
        <span class="meta-pill">总角色 {{ total }}</span>
        <span class="meta-pill">启用 {{ enabledCount }}</span>
        <span class="meta-pill">关联菜单 {{ boundMenuCount }}</span>
      </template>

      <template #filters>
        <label class="filter-grow">
          <span>搜索角色</span>
          <input
            v-model.trim="keyword"
            type="text"
            placeholder="搜索角色名称、编码或描述"
            @keyup.enter="searchRoles"
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
        <button type="button" class="ghost-btn" @click="searchRoles">搜索</button>
        <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
        <button type="button" class="primary-btn" @click="openCreate">新增角色</button>
      </template>

      <table class="simple-table">
        <thead>
          <tr>
            <th>角色名称</th>
            <th>编码</th>
            <th>描述</th>
            <th>菜单数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="role in roles" :key="role.id">
            <td>{{ role.name }}</td>
            <td>{{ role.code }}</td>
            <td>{{ role.description || '-' }}</td>
            <td>{{ (role.menuIds || []).length }}</td>
            <td>
              <span :class="['status-pill', role.status === 1 ? 'success-pill' : 'muted-pill']">
                {{ role.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td>
              <div class="inline-actions">
                <button type="button" class="secondary-btn mini-btn" @click="editRole(role)">编辑</button>
                <button type="button" class="danger-btn mini-btn" @click="removeRole(role)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!roles.length">
            <td colspan="6" class="empty-row">
              <div class="table-empty-state">
                <strong>没有匹配的角色数据</strong>
                <p>可以新增角色，或者调整搜索条件重新查看。</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </ManagementPageCard>

    <FormDialog
      :visible="dialogVisible"
      eyebrow="Role Editor"
      :title="form.id ? '编辑角色' : '新增角色'"
      @close="closeDialog"
    >
      <form class="form-stack" @submit.prevent="submitRole">
        <SchemaFormFields :model="form" :fields="roleFields" layout="grid" />
        <SchemaFormFields :model="form" :fields="roleDescriptionField" />
        <label class="role-menu-field">
          <span>可访问菜单</span>
          <div class="role-menu-panel">
            <div class="role-menu-toolbar">
              <span class="role-menu-summary">已选 {{ form.menuIds.length }} / {{ menuOptions.length }}</span>
              <button type="button" class="ghost-btn mini-btn" @click="toggleAllMenus">
                {{ isAllMenusChecked ? '取消全选' : '全选菜单' }}
              </button>
            </div>
            <div class="role-menu-list">
              <label v-for="menu in menuOptions" :key="menu.value" class="role-menu-option">
                <input v-model="form.menuIds" type="checkbox" :value="menu.value" />
                <span class="role-menu-option-copy">
                  <strong>{{ menu.name }}</strong>
                  <small>{{ menu.path }}</small>
                </span>
              </label>
            </div>
          </div>
        </label>

        <div class="action-row">
          <button type="submit" class="primary-btn" :disabled="loading">
            {{ loading ? '保存中...' : form.id ? '保存更新' : '创建角色' }}
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
const menus = ref([])
const statusMessage = ref('')
const errorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('')
const {
  items: roles,
  loading,
  error: listError,
  total,
  page,
  size,
  load: fetchRoles,
  search: searchRolesQuery,
  changePage,
  changeSize
} = usePagedQuery(async ({ page, size, keyword, status }) => {
  const { data } = await http.get('/roles/query', {
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
  name: '',
  code: '',
  description: '',
  status: 1,
  menuIds: []
})

const roleFields = [
  { key: 'name', label: '角色名称', type: 'text' },
  { key: 'code', label: '角色编码', type: 'text' },
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

const roleDescriptionField = [{ key: 'description', label: '描述', type: 'textarea', rows: 4 }]

const menuOptions = computed(() =>
  menus.value.map((menu) => ({
    value: Number(menu.id),
    name: menu.name,
    path: menu.path || '-'
  }))
)
const isAllMenusChecked = computed(() => {
  if (!menuOptions.value.length) {
    return false
  }
  return menuOptions.value.every((menu) => form.menuIds.includes(menu.value))
})

const enabledCount = computed(() => roles.value.filter((item) => item.status === 1).length)
const boundMenuCount = computed(() => roles.value.reduce((sum, item) => sum + ((item.menuIds || []).length > 0 ? 1 : 0), 0))

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function resetForm() {
  form.id = null
  form.name = ''
  form.code = ''
  form.description = ''
  form.status = 1
  form.menuIds = []
}

function closeDialog() {
  dialogVisible.value = false
  resetForm()
}

function toggleAllMenus() {
  form.menuIds = isAllMenusChecked.value ? [] : menuOptions.value.map((menu) => menu.value)
}

function openCreate() {
  resetMessages()
  resetForm()
  dialogVisible.value = true
}

function editRole(role) {
  resetMessages()
  form.id = role.id
  form.name = role.name
  form.code = role.code
  form.description = role.description || ''
  form.status = role.status ?? 1
  form.menuIds = [...(role.menuIds || [])]
  dialogVisible.value = true
}

async function fetchMenus() {
  const { data } = await http.get('/menus')
  menus.value = data || []
}

async function searchRoles() {
  try {
    await searchRolesQuery({
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
  await searchRoles()
}

async function removeRole(role) {
  resetMessages()
  if (!window.confirm(`确认删除角色「${role.name}」吗？`)) {
    return
  }
  loading.value = true
  try {
    await http.delete(`/roles/${role.id}`)
    statusMessage.value = '角色已删除'
    if ((page.value - 1) * size.value >= Math.max(0, total.value - 1) && page.value > 1) {
      page.value -= 1
    }
    await fetchRoles()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function submitRole() {
  resetMessages()
  loading.value = true
  try {
    const payload = {
      name: form.name,
      code: form.code,
      description: form.description,
      status: form.status,
      menuIds: form.menuIds
    }
    if (form.id) {
      await http.put(`/roles/${form.id}`, payload)
      statusMessage.value = '角色已更新'
    } else {
      await http.post('/roles', payload)
      statusMessage.value = '角色已创建'
    }
    closeDialog()
    await fetchRoles()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    await Promise.all([fetchRoles(), fetchMenus()])
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '初始化角色列表失败'
  }
})
</script>

<style scoped>
.role-menu-field {
  display: grid;
  gap: 8px;
}

.role-menu-panel {
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #fff;
  overflow: hidden;
}

.role-menu-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border-bottom: 1px solid #e7edf4;
  background: #f8fafc;
}

.role-menu-summary {
  font-size: 12px;
  color: #66788f;
}

.role-menu-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 12px;
  max-height: 240px;
  padding: 10px;
  overflow: auto;
}

.role-menu-option {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-height: 40px;
  padding: 8px 10px;
  border: 1px solid #e5ebf3;
  border-radius: 6px;
  background: #fff;
}

.role-menu-option input {
  width: 14px;
  height: 14px;
  margin: 2px 0 0;
  flex: 0 0 auto;
}

.role-menu-option-copy {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.role-menu-option-copy strong {
  color: #243445;
  font-size: 13px;
  line-height: 1.3;
  font-weight: 600;
}

.role-menu-option-copy small {
  color: #7b8a9b;
  font-size: 11px;
  line-height: 1.3;
  word-break: break-all;
}

@media (max-width: 900px) {
  .role-menu-list {
    grid-template-columns: 1fr;
  }
}
</style>
