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

          <label>
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

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Menu Registry</p>
            <h3>菜单权限配置</h3>
          </div>
          <div class="management-meta">
            <span class="meta-pill">总菜单 {{ total }}</span>
            <span class="meta-pill">启用 {{ enabledCount }}</span>
            <span class="meta-pill">禁用 {{ disabledCount }}</span>
          </div>
        </div>

        <div class="management-toolbar">
          <div class="management-filters">
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
          </div>

          <div class="management-actions">
            <button type="button" class="ghost-btn" @click="searchMenus">搜索</button>
            <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
            <button type="button" class="primary-btn" @click="openCreate">新增菜单</button>
          </div>
        </div>

        <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
        <p v-if="errorMessage || listError" class="feedback error">{{ errorMessage || listError }}</p>

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

        <AppPagination
          :page="page"
          :size="size"
          :total="total"
          :disabled="loading"
          @update:page="changePage"
          @update:size="changeSize"
        />
      </article>
    </section>

    <div v-if="dialogVisible" class="modal-mask" @click.self="closeDialog">
      <div class="modal-panel form-modal">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Menu Editor</p>
            <h3>{{ menuForm.id ? '编辑菜单' : '新增菜单' }}</h3>
          </div>
          <button type="button" class="ghost-btn" @click="closeDialog">关闭</button>
        </div>

        <form class="form-stack" @submit.prevent="submitMenu">
          <div class="form-grid">
            <label>
              <span>名称</span>
              <input v-model.trim="menuForm.name" type="text" />
            </label>
            <label>
              <span>路由路径</span>
              <input v-model.trim="menuForm.path" type="text" placeholder="/users" />
            </label>
            <label>
              <span>组件名</span>
              <input v-model.trim="menuForm.component" type="text" />
            </label>
            <label>
              <span>图标编码</span>
              <input v-model.trim="menuForm.icon" type="text" />
            </label>
            <label>
              <span>排序</span>
              <input v-model.number="menuForm.sort" type="number" min="0" />
            </label>
            <label>
              <span>状态</span>
              <select v-model.number="menuForm.status">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </label>
          </div>

          <label>
            <span>权限标识</span>
            <input v-model.trim="menuForm.permission" type="text" placeholder="user:manage" />
          </label>

          <div class="action-row">
            <button type="submit" class="primary-btn" :disabled="loading">
              {{ loading ? '保存中...' : menuForm.id ? '保存更新' : '创建菜单' }}
            </button>
            <button type="button" class="secondary-btn" @click="resetMenuForm">重置</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { http, refreshCurrentUser } from '../api/http'
import AppPagination from '../components/common/AppPagination.vue'
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
    await refreshCurrentUser()
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
    await refreshCurrentUser()
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
    await refreshCurrentUser()
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
  gap: 10px;
  padding: 12px;
  max-height: 320px;
  overflow: auto;
}

.role-menu-option {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.role-menu-option input {
  width: auto;
  margin-top: 2px;
}

.role-menu-option-copy {
  display: grid;
  gap: 2px;
}

.role-menu-option-copy strong,
.role-menu-option-copy small {
  margin: 0;
}

.role-menu-option-copy small {
  color: #7d8a99;
}

.mini-empty-state {
  grid-column: 1 / -1;
}
</style>
