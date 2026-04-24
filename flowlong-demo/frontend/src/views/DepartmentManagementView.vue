<template>
  <div class="page-grid">
    <ManagementPageCard
      eyebrow="Department Matrix"
      title="部门管理"
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
        <span class="meta-pill">总部门 {{ total }}</span>
        <span class="meta-pill">启用 {{ enabledCount }}</span>
        <span class="meta-pill">有负责人 {{ leaderBoundCount }}</span>
      </template>

      <template #filters>
        <label class="filter-grow">
          <span>搜索部门</span>
          <input v-model.trim="keyword" type="text" placeholder="搜索部门名称、编码、负责人或描述" @keyup.enter="searchDepartments" />
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
        <button type="button" class="ghost-btn" @click="searchDepartments">搜索</button>
        <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
        <button type="button" class="primary-btn" @click="openCreate">新增部门</button>
      </template>

      <table class="simple-table">
        <thead>
          <tr>
            <th>部门名称</th>
            <th>编码</th>
            <th>负责人</th>
            <th>上级部门</th>
            <th>状态</th>
            <th>说明</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="department in departments" :key="department.id">
            <td>{{ department.name }}</td>
            <td>{{ department.code }}</td>
            <td>{{ department.leader || '-' }}</td>
            <td>{{ getParentName(department.parentId) }}</td>
            <td>
              <span :class="['status-pill', department.status === 1 ? 'success-pill' : 'muted-pill']">
                {{ department.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td>{{ department.description || '-' }}</td>
            <td>
              <div class="inline-actions">
                <button type="button" class="secondary-btn mini-btn" @click="editDepartment(department)">编辑</button>
                <button type="button" class="danger-btn mini-btn" @click="removeDepartmentEntry(department)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!departments.length">
            <td colspan="7" class="empty-row">
              <div class="table-empty-state">
                <strong>没有匹配的部门数据</strong>
                <p>可以新增部门，或调整筛选条件重新查看。</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </ManagementPageCard>

    <FormDialog
      :visible="dialogVisible"
      eyebrow="Department Editor"
      :title="form.id ? '编辑部门' : '新增部门'"
      @close="closeDialog"
    >
      <form class="form-stack" @submit.prevent="submitDepartment">
        <SchemaFormFields :model="form" :fields="departmentFields" layout="grid" />
        <SchemaFormFields :model="form" :fields="departmentDescriptionField" />

        <div class="action-row">
          <button type="submit" class="primary-btn" :disabled="loading">
            {{ loading ? '保存中...' : form.id ? '保存更新' : '创建部门' }}
          </button>
          <button type="button" class="secondary-btn" @click="resetForm">重置</button>
        </div>
      </form>
    </FormDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { createDepartment, deleteDepartment, fetchDepartmentLookup, queryDepartments, updateDepartment } from '../api/orgAdmin'
import FormDialog from '../components/common/FormDialog.vue'
import ManagementPageCard from '../components/common/ManagementPageCard.vue'
import SchemaFormFields from '../components/common/SchemaFormFields.vue'
import { usePagedQuery } from '../composables/usePagedQuery'

const dialogVisible = ref(false)
const statusMessage = ref('')
const errorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('')
const departmentList = ref([])
const {
  items: departments,
  loading,
  error: listError,
  total,
  page,
  size,
  load: fetchDepartments,
  search: searchDepartmentQuery,
  changePage,
  changeSize
} = usePagedQuery(async ({ page, size, keyword, status }) => queryDepartments({ page, size, keyword, status }), {
  page: 1,
  size: 8
})

const form = reactive({
  id: null,
  name: '',
  code: '',
  leader: '',
  parentId: null,
  status: 1,
  description: ''
})

const departmentFields = computed(() => [
  { key: 'name', label: '部门名称', type: 'text' },
  { key: 'code', label: '部门编码', type: 'text' },
  { key: 'leader', label: '负责人', type: 'text', placeholder: 'leader.username' },
  {
    key: 'parentId',
    label: '上级部门',
    type: 'select',
    cast: 'number',
    options: [
      { label: '无', value: '' },
      ...departmentList.value
        .filter((item) => !form.id || item.id !== form.id)
        .map((item) => ({ label: `${item.name} (${item.code})`, value: item.id }))
    ]
  },
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
])

const departmentDescriptionField = [{ key: 'description', label: '说明', type: 'textarea', rows: 4 }]

const enabledCount = computed(() => departments.value.filter((item) => item.status === 1).length)
const leaderBoundCount = computed(() => departments.value.filter((item) => item.leader).length)

async function refreshOptions() {
  departmentList.value = await fetchDepartmentLookup()
}

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function resetForm() {
  form.id = null
  form.name = ''
  form.code = ''
  form.leader = ''
  form.parentId = null
  form.status = 1
  form.description = ''
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

function editDepartment(item) {
  resetMessages()
  form.id = item.id
  form.name = item.name
  form.code = item.code
  form.leader = item.leader || ''
  form.parentId = item.parentId || null
  form.status = item.status ?? 1
  form.description = item.description || ''
  dialogVisible.value = true
}

function getParentName(parentId) {
  if (!parentId) {
    return '-'
  }
  return departmentList.value.find((item) => item.id === parentId)?.name || '-'
}

async function searchDepartments() {
  try {
    await Promise.all([
      searchDepartmentQuery({
        keyword: keyword.value || undefined,
        status: statusFilter.value === '' ? undefined : Number(statusFilter.value)
      }),
      refreshOptions()
    ])
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '查询失败'
  }
}

async function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  await searchDepartments()
}

async function submitDepartment() {
  resetMessages()
  try {
    const payload = {
      name: form.name,
      code: form.code,
      leader: form.leader,
      parentId: form.parentId,
      status: form.status,
      description: form.description
    }
    if (form.id) {
      await updateDepartment(form.id, payload)
    } else {
      await createDepartment(payload)
    }
    statusMessage.value = form.id ? '部门已更新' : '部门已创建'
    dialogVisible.value = false
    await refreshOptions()
    await searchDepartments()
    resetForm()
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '保存失败'
  }
}

async function removeDepartmentEntry(item) {
  resetMessages()
  if (!window.confirm(`确认删除部门「${item.name}」吗？`)) {
    return
  }
  try {
    await deleteDepartment(item.id)
    statusMessage.value = '部门已删除'
    await refreshOptions()
    await searchDepartments()
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '删除失败'
  }
}

onMounted(async () => {
  try {
    await refreshOptions()
    await fetchDepartments()
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '部门管理初始化失败'
  }
})
</script>
