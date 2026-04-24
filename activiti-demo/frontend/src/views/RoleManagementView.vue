<template>
  <div class="page-grid">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Role Matrix</p>
          <h3>角色管理</h3>
        </div>
        <div class="management-meta">
          <span class="meta-pill">总角色 {{ total }}</span>
          <span class="meta-pill">启用 {{ enabledCount }}</span>
          <span class="meta-pill">关联菜单 {{ boundMenuCount }}</span>
        </div>
      </div>

      <div class="management-toolbar">
        <div class="management-filters">
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
        </div>

        <div class="management-actions">
          <button type="button" class="ghost-btn" @click="searchRoles">搜索</button>
          <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
          <button type="button" class="primary-btn" @click="openCreate">新增角色</button>
        </div>
      </div>

      <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
      <p v-if="errorMessage || listError" class="feedback error">{{ errorMessage || listError }}</p>

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
            <p class="eyebrow">Role Editor</p>
            <h3>{{ form.id ? '编辑角色' : '新增角色' }}</h3>
          </div>
          <button type="button" class="ghost-btn" @click="closeDialog">关闭</button>
        </div>

        <form class="form-stack" @submit.prevent="submitRole">
          <div class="form-grid">
            <label>
              <span>角色名称</span>
              <input v-model.trim="form.name" type="text" />
            </label>
            <label>
              <span>角色编码</span>
              <input v-model.trim="form.code" type="text" />
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
            <span>描述</span>
            <textarea v-model.trim="form.description" rows="4"></textarea>
          </label>

          <label>
            <span>可访问菜单</span>
            <div class="role-menu-panel">
              <div class="role-menu-toolbar">
                <span class="role-menu-summary">已选 {{ form.menuIds.length }} / {{ menus.length }}</span>
              </div>
              <div class="role-menu-list">
                <label v-for="menu in menus" :key="menu.id" class="role-menu-option">
                  <input v-model="form.menuIds" type="checkbox" :value="menu.id" />
                  <span class="role-menu-option-copy">
                    <strong>{{ menu.name }}</strong>
                    <small>{{ menu.path || '-' }} · {{ menu.status === 1 ? '启用' : '禁用' }}</small>
                  </span>
                </label>
                <div v-if="!menus.length" class="empty-state mini-empty-state">暂无可分配菜单。</div>
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
  const { data } = await http.get('/menus/options')
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
    await refreshCurrentUser()
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
.role-menu-panel {
  border: 1px solid #dbe3ee;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  overflow: hidden;
}

.role-menu-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 14px;
  border-bottom: 1px solid #e7edf4;
  background: #f8fafc;
}

.role-menu-summary {
  font-size: 12px;
  color: #66788f;
  font-weight: 700;
}

.role-menu-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 14px;
  max-height: 320px;
  overflow: auto;
}

.role-menu-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid rgba(82, 103, 132, 0.1);
  background: rgba(255, 255, 255, 0.86);
}

.role-menu-option input {
  width: auto;
  margin-top: 2px;
  accent-color: #2f7de1;
}

.role-menu-option-copy {
  display: grid;
  gap: 3px;
}

.role-menu-option-copy strong,
.role-menu-option-copy small {
  margin: 0;
}

.role-menu-option-copy strong {
  color: #243754;
}

.role-menu-option-copy small {
  color: #7d8a99;
}

.mini-empty-state {
  grid-column: 1 / -1;
}

@media (max-width: 900px) {
  .role-menu-list {
    grid-template-columns: 1fr;
  }
}
</style>
