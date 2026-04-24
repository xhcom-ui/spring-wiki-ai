<template>
  <div class="page-grid">
    <ManagementPageCard
      eyebrow="Post Directory"
      title="岗位管理"
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
        <span class="meta-pill">总岗位 {{ total }}</span>
        <span class="meta-pill">启用 {{ enabledCount }}</span>
        <span class="meta-pill">覆盖部门 {{ coveredDepartmentCount }}</span>
      </template>

      <template #filters>
        <label class="filter-grow">
          <span>搜索岗位</span>
          <input v-model.trim="keyword" type="text" placeholder="搜索岗位名称、编码、级别或描述" @keyup.enter="searchPosts" />
        </label>
        <label class="compact-field">
          <span>部门</span>
          <select v-model="departmentFilter">
            <option value="">全部部门</option>
            <option v-for="item in departmentOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
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
        <button type="button" class="ghost-btn" @click="searchPosts">搜索</button>
        <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
        <button type="button" class="primary-btn" @click="openCreate">新增岗位</button>
      </template>

      <table class="simple-table">
        <thead>
          <tr>
            <th>岗位名称</th>
            <th>编码</th>
            <th>所属部门</th>
            <th>级别</th>
            <th>状态</th>
            <th>说明</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="post in posts" :key="post.id">
            <td>{{ post.name }}</td>
            <td>{{ post.code }}</td>
            <td>{{ getDepartmentName(post.departmentId) }}</td>
            <td>{{ post.level || '-' }}</td>
            <td>
              <span :class="['status-pill', post.status === 1 ? 'success-pill' : 'muted-pill']">
                {{ post.status === 1 ? '启用' : '禁用' }}
              </span>
            </td>
            <td>{{ post.description || '-' }}</td>
            <td>
              <div class="inline-actions">
                <button type="button" class="secondary-btn mini-btn" @click="editPost(post)">编辑</button>
                <button type="button" class="danger-btn mini-btn" @click="removePostEntry(post)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!posts.length">
            <td colspan="7" class="empty-row">
              <div class="table-empty-state">
                <strong>没有匹配的岗位数据</strong>
                <p>可以先维护部门，再补充岗位数据。</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </ManagementPageCard>

    <FormDialog
      :visible="dialogVisible"
      eyebrow="Post Editor"
      :title="form.id ? '编辑岗位' : '新增岗位'"
      @close="closeDialog"
    >
      <form class="form-stack" @submit.prevent="submitPost">
        <SchemaFormFields :model="form" :fields="postFields" layout="grid" />
        <SchemaFormFields :model="form" :fields="postDescriptionField" />

        <div class="action-row">
          <button type="submit" class="primary-btn" :disabled="loading">
            {{ loading ? '保存中...' : form.id ? '保存更新' : '创建岗位' }}
          </button>
          <button type="button" class="secondary-btn" @click="resetForm">重置</button>
        </div>
      </form>
    </FormDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { createPost, deletePost, fetchDepartmentLookup, queryPosts, updatePost } from '../api/orgAdmin'
import FormDialog from '../components/common/FormDialog.vue'
import ManagementPageCard from '../components/common/ManagementPageCard.vue'
import SchemaFormFields from '../components/common/SchemaFormFields.vue'
import { usePagedQuery } from '../composables/usePagedQuery'

const dialogVisible = ref(false)
const statusMessage = ref('')
const errorMessage = ref('')
const keyword = ref('')
const statusFilter = ref('')
const departmentFilter = ref('')
const departments = ref([])
const departmentOptions = ref([])
const {
  items: posts,
  loading,
  error: listError,
  total,
  page,
  size,
  load: fetchPosts,
  search: searchPostQuery,
  changePage,
  changeSize
} = usePagedQuery(async ({ page, size, keyword, status, departmentId }) => queryPosts({ page, size, keyword, status, departmentId }), {
  page: 1,
  size: 8
})

const form = reactive({
  id: null,
  name: '',
  code: '',
  departmentId: null,
  level: '',
  status: 1,
  description: ''
})

const postFields = computed(() => [
  { key: 'name', label: '岗位名称', type: 'text' },
  { key: 'code', label: '岗位编码', type: 'text' },
  {
    key: 'departmentId',
    label: '所属部门',
    type: 'select',
    cast: 'number',
    options: [
      { label: '请选择部门', value: '' },
      ...departmentOptions.value
    ]
  },
  { key: 'level', label: '岗位级别', type: 'text', placeholder: 'P1 / P2 / P3' },
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

const postDescriptionField = [{ key: 'description', label: '说明', type: 'textarea', rows: 4 }]

const enabledCount = computed(() => posts.value.filter((item) => item.status === 1).length)
const coveredDepartmentCount = computed(() => new Set(posts.value.map((item) => item.departmentId).filter(Boolean)).size)

async function refreshDepartmentData() {
  departments.value = await fetchDepartmentLookup()
  departmentOptions.value = departments.value.map((item) => ({
    label: `${item.name} (${item.code})`,
    value: item.id
  }))
}

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function resetForm() {
  form.id = null
  form.name = ''
  form.code = ''
  form.departmentId = null
  form.level = ''
  form.status = 1
  form.description = ''
}

function closeDialog() {
  dialogVisible.value = false
  resetForm()
}

function openCreate() {
  resetMessages()
  void refreshDepartmentData()
  resetForm()
  dialogVisible.value = true
}

function editPost(item) {
  resetMessages()
  void refreshDepartmentData()
  form.id = item.id
  form.name = item.name
  form.code = item.code
  form.departmentId = item.departmentId
  form.level = item.level || ''
  form.status = item.status ?? 1
  form.description = item.description || ''
  dialogVisible.value = true
}

function getDepartmentName(departmentId) {
  return departments.value.find((item) => item.id === departmentId)?.name || '-'
}

async function searchPosts() {
  try {
    await Promise.all([
      searchPostQuery({
        keyword: keyword.value || undefined,
        status: statusFilter.value === '' ? undefined : Number(statusFilter.value),
        departmentId: departmentFilter.value === '' ? undefined : Number(departmentFilter.value)
      }),
      refreshDepartmentData()
    ])
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '查询失败'
  }
}

async function resetFilters() {
  keyword.value = ''
  statusFilter.value = ''
  departmentFilter.value = ''
  await searchPosts()
}

async function submitPost() {
  resetMessages()
  try {
    const payload = {
      name: form.name,
      code: form.code,
      departmentId: form.departmentId,
      level: form.level,
      status: form.status,
      description: form.description
    }
    if (form.id) {
      await updatePost(form.id, payload)
    } else {
      await createPost(payload)
    }
    statusMessage.value = form.id ? '岗位已更新' : '岗位已创建'
    dialogVisible.value = false
    await refreshDepartmentData()
    await searchPosts()
    resetForm()
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '保存失败'
  }
}

async function removePostEntry(item) {
  resetMessages()
  if (!window.confirm(`确认删除岗位「${item.name}」吗？`)) {
    return
  }
  try {
    await deletePost(item.id)
    statusMessage.value = '岗位已删除'
    await searchPosts()
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '删除失败'
  }
}

onMounted(async () => {
  try {
    await refreshDepartmentData()
    await fetchPosts()
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '岗位管理初始化失败'
  }
})
</script>
