<template>
  <div class="page-grid">
    <section class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Role Permission Binder</p>
            <h3>角色菜单授权</h3>
          </div>
          <div class="management-meta">
            <span class="meta-pill">角色 {{ roles.length }}</span>
            <span class="meta-pill">菜单 {{ allMenus.length }}</span>
            <span class="meta-pill">已选 {{ selectedMenuIds.length }}</span>
          </div>
        </div>

        <div class="form-stack">
          <label>
            <span>选择角色</span>
            <select v-model.number="selectedRoleId" @change="syncRoleMenus">
              <option :value="0">请选择角色</option>
              <option v-for="role in roles" :key="role.id" :value="role.id">
                {{ role.name }} ({{ role.code }})
              </option>
            </select>
          </label>

          <label class="filter-grow">
            <span>筛选菜单</span>
            <input v-model.trim="roleMenuKeyword" type="text" placeholder="按菜单名、路径或权限搜索" />
          </label>

          <label class="role-menu-field">
            <span>角色菜单权限</span>
            <div class="role-menu-panel">
              <div class="role-menu-toolbar">
                <span class="role-menu-summary">已选 {{ selectedMenuIds.length }} / {{ filteredAssignableMenus.length }}</span>
                <button type="button" class="ghost-btn mini-btn" :disabled="!filteredAssignableMenus.length" @click="toggleAssignableMenus">
                  {{ isAllAssignableMenusChecked ? '取消全选' : '全选当前结果' }}
                </button>
              </div>
              <div class="role-menu-list">
                <label v-for="menu in filteredAssignableMenus" :key="menu.id" class="role-menu-option">
                  <input v-model="selectedMenuIds" type="checkbox" :value="menu.id" />
                  <span class="role-menu-option-copy">
                    <strong>{{ menu.name }}</strong>
                    <small>{{ menu.path || '-' }} · {{ menu.status === 1 ? '启用' : '禁用' }}</small>
                  </span>
                </label>
                <div v-if="!filteredAssignableMenus.length" class="empty-state mini-empty-state">没有匹配的菜单项。</div>
              </div>
            </div>
          </label>

          <div class="action-row">
            <button type="button" class="primary-btn" :disabled="!selectedRoleId || loading" @click="saveRoleMenus">
              保存角色菜单
            </button>
          </div>
        </div>
      </article>

      <ManagementPageCard
        eyebrow="Menu Registry"
        title="菜单权限配置"
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
          <span class="meta-pill">总菜单 {{ total }}</span>
          <span class="meta-pill">启用 {{ enabledCount }}</span>
          <span class="meta-pill">禁用 {{ disabledCount }}</span>
        </template>

        <template #filters>
          <label class="filter-grow">
            <span>搜索菜单</span>
            <input
              v-model.trim="keyword"
              type="text"
              placeholder="搜索菜单名、路径或权限标识"
              @keyup.enter="searchMenus"
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
          <button type="button" class="ghost-btn" @click="searchMenus">搜索</button>
          <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
          <button type="button" class="primary-btn" @click="openCreate">新增菜单</button>
        </template>

        <table class="simple-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>路径</th>
              <th>权限</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="menu in menus" :key="menu.id">
              <td>{{ menu.name }}</td>
              <td>{{ menu.path }}</td>
              <td>{{ menu.permission || '-' }}</td>
              <td>
                <span :class="['status-pill', menu.status === 1 ? 'success-pill' : 'muted-pill']">
                  {{ menu.status === 1 ? '启用' : '禁用' }}
                </span>
              </td>
              <td>
                <div class="inline-actions">
                  <button type="button" class="secondary-btn mini-btn" @click="editMenu(menu)">编辑</button>
                  <button type="button" class="danger-btn mini-btn" @click="removeMenu(menu)">删除</button>
                </div>
              </td>
            </tr>
            <tr v-if="!menus.length">
              <td colspan="5" class="empty-row">
                <div class="table-empty-state">
                  <strong>没有匹配的菜单数据</strong>
                  <p>可以新增菜单，或者调整筛选条件后重试。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </ManagementPageCard>
    </section>

    <FormDialog
      :visible="dialogVisible"
      eyebrow="Menu Editor"
      :title="menuForm.id ? '编辑菜单' : '新增菜单'"
      @close="closeDialog"
    >
      <form class="form-stack" @submit.prevent="submitMenu">
        <SchemaFormFields :model="menuForm" :fields="menuFields" layout="grid" />
        <SchemaFormFields :model="menuForm" :fields="menuPermissionField" />

        <div class="action-row">
          <button type="submit" class="primary-btn" :disabled="loading">
            {{ loading ? '保存中...' : menuForm.id ? '保存更新' : '创建菜单' }}
          </button>
          <button type="button" class="secondary-btn" @click="resetMenuForm">重置</button>
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
const allMenus = ref([])
const selectedRoleId = ref(0)
const selectedMenuIds = ref([])
const statusMessage = ref('')
const errorMessage = ref('')
const keyword = ref('')
const roleMenuKeyword = ref('')
const statusFilter = ref('')
const {
  items: menus,
  loading,
  error: listError,
  total,
  page,
  size,
  load: fetchMenus,
  search: searchMenusQuery,
  changePage,
  changeSize
} = usePagedQuery(async ({ page, size, keyword, status }) => {
  const { data } = await http.get('/menus/query', {
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

const menuForm = reactive({
  id: null,
  name: '',
  path: '',
  component: '',
  icon: '',
  parentId: 0,
  sort: 0,
  type: 1,
  permission: '',
  status: 1
})

const menuFields = [
  { key: 'name', label: '名称', type: 'text' },
  { key: 'path', label: '路由路径', type: 'text', placeholder: '/users' },
  { key: 'component', label: '组件名', type: 'text' },
  { key: 'icon', label: '图标编码', type: 'text' },
  { key: 'sort', label: '排序', type: 'number', min: 0, cast: 'number' },
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

const menuPermissionField = [{ key: 'permission', label: '权限标识', type: 'text', placeholder: 'user:manage' }]

const enabledCount = computed(() => menus.value.filter((item) => item.status === 1).length)
const disabledCount = computed(() => menus.value.filter((item) => item.status !== 1).length)
const filteredAssignableMenus = computed(() => {
  const needle = roleMenuKeyword.value.trim().toLowerCase()
  if (!needle) {
    return allMenus.value
  }
  return allMenus.value.filter((menu) =>
    [menu.name, menu.path, menu.permission]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(needle))
  )
})
const isAllAssignableMenusChecked = computed(() => {
  if (!filteredAssignableMenus.value.length) {
    return false
  }
  return filteredAssignableMenus.value.every((menu) => selectedMenuIds.value.includes(menu.id))
})

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function resetMenuForm() {
  menuForm.id = null
  menuForm.name = ''
  menuForm.path = ''
  menuForm.component = ''
  menuForm.icon = ''
  menuForm.parentId = 0
  menuForm.sort = 0
  menuForm.type = 1
  menuForm.permission = ''
  menuForm.status = 1
}

function closeDialog() {
  dialogVisible.value = false
  resetMenuForm()
}

function openCreate() {
  resetMessages()
  resetMenuForm()
  dialogVisible.value = true
}

function editMenu(menu) {
  resetMessages()
  menuForm.id = menu.id
  menuForm.name = menu.name
  menuForm.path = menu.path
  menuForm.component = menu.component
  menuForm.icon = menu.icon
  menuForm.parentId = menu.parentId ?? 0
  menuForm.sort = menu.sort ?? 0
  menuForm.type = menu.type ?? 1
  menuForm.permission = menu.permission || ''
  menuForm.status = menu.status ?? 1
  dialogVisible.value = true
}

function syncRoleMenus() {
  const role = roles.value.find((item) => item.id === selectedRoleId.value)
  selectedMenuIds.value = role ? [...(role.menuIds || [])] : []
}

function toggleAssignableMenus() {
  if (isAllAssignableMenusChecked.value) {
    const filteredIds = new Set(filteredAssignableMenus.value.map((menu) => menu.id))
    selectedMenuIds.value = selectedMenuIds.value.filter((id) => !filteredIds.has(id))
    return
  }
  const merged = new Set(selectedMenuIds.value)
  filteredAssignableMenus.value.forEach((menu) => merged.add(menu.id))
  selectedMenuIds.value = Array.from(merged)
}

async function fetchRoles() {
  const { data } = await http.get('/roles/options')
  roles.value = data || []
  syncRoleMenus()
}

async function fetchAllMenus() {
  const { data } = await http.get('/menus')
  allMenus.value = data || []
}

async function refreshAll() {
  await Promise.all([fetchRoles(), fetchMenus(), fetchAllMenus()])
}

async function searchMenus() {
  try {
    await searchMenusQuery({
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
  await searchMenus()
}

async function saveRoleMenus() {
  resetMessages()
  loading.value = true
  try {
    await http.put(`/roles/${selectedRoleId.value}/menus`, { menuIds: selectedMenuIds.value })
    statusMessage.value = '角色菜单权限已保存'
    await fetchRoles()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function submitMenu() {
  resetMessages()
  loading.value = true
  try {
    const payload = { ...menuForm }
    if (menuForm.id) {
      await http.put(`/menus/${menuForm.id}`, payload)
      statusMessage.value = '菜单已更新'
    } else {
      await http.post('/menus', payload)
      statusMessage.value = '菜单已创建'
    }
    closeDialog()
    await refreshAll()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

async function removeMenu(menu) {
  resetMessages()
  if (!window.confirm(`确认删除菜单「${menu.name}」吗？`)) {
    return
  }
  loading.value = true
  try {
    await http.delete(`/menus/${menu.id}`)
    statusMessage.value = '菜单已删除'
    if ((page.value - 1) * size.value >= Math.max(0, total.value - 1) && page.value > 1) {
      page.value -= 1
    }
    await refreshAll()
  } catch (error) {
    errorMessage.value = error.normalizedMessage
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    await refreshAll()
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '初始化菜单权限数据失败'
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
  max-height: 320px;
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

.mini-empty-state {
  grid-column: 1 / -1;
  min-height: 64px;
}

@media (max-width: 900px) {
  .role-menu-list {
    grid-template-columns: 1fr;
  }
}
</style>
