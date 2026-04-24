<template>
  <div class="page-grid">
    <section class="panel-card studio-filter-panel studio-plain-panel">
      <div class="studio-filter-grid">
        <label>
          <span>分类编码</span>
          <input v-model.trim="filters.processKey" type="text" placeholder="leave-process" />
        </label>
        <label>
          <span>分类名称</span>
          <input v-model.trim="filters.processName" type="text" placeholder="请假审批流程" />
        </label>
        <label>
          <span>版本范围</span>
          <select v-model="filters.status">
            <option value="">全部版本</option>
            <option value="ACTIVE">激活版本</option>
            <option value="DRAFT">草稿/历史</option>
          </select>
        </label>
        <div class="studio-filter-actions">
          <button type="button" class="primary-btn studio-search-btn" @click="applyFilters">查询</button>
          <button type="button" class="secondary-btn" @click="resetFilters">重置</button>
        </div>
      </div>
    </section>

    <section class="panel-card studio-catalog-panel studio-plain-panel">
      <div class="panel-head">
        <div>
          <p class="eyebrow">流程分类</p>
          <h3>流程分类管理</h3>
        </div>
        <div class="management-meta">
          <span class="meta-pill">分类 {{ processCatalogCards.length }}</span>
          <span class="meta-pill">版本 {{ filteredDefinitions.length }}</span>
          <span class="meta-pill">激活 {{ activeCatalogCount }}</span>
        </div>
        <div class="inline-actions">
          <button type="button" class="primary-btn studio-new-btn" @click="openStudio()">新增</button>
        </div>
      </div>

      <div v-if="statusMessage" class="feedback success">{{ statusMessage }}</div>
      <div v-if="errorMessage" class="feedback error">{{ errorMessage }}</div>

      <div class="studio-definition-grid">
        <button
          v-for="catalog in processCatalogCards"
          :key="catalog.processKey"
          type="button"
          class="studio-definition-tile"
          @click="openStudio(catalog.entryDefinitionId)"
        >
          <div class="studio-definition-main">
            <div class="studio-definition-tile-head">
              <div class="studio-definition-title-block">
                <strong>{{ catalog.processName }}</strong>
                <p>{{ catalog.processKey }}</p>
              </div>
              <span :class="['status-pill', catalog.hasActive ? 'success-pill' : 'muted-pill']">
                {{ catalog.hasActive ? '已发布' : '未发布' }}
              </span>
            </div>

            <div class="studio-definition-matrix">
              <div>
                <span>流程版本</span>
                <strong>{{ catalog.versionSummary }}</strong>
              </div>
              <div>
                <span>流程数量</span>
                <strong>{{ catalog.definitionCount }}</strong>
              </div>
              <div>
                <span>激活版本</span>
                <strong>{{ catalog.activeVersionLabel }}</strong>
              </div>
              <div>
                <span>更新时间</span>
                <strong>{{ formatDate(catalog.updatedAt) }}</strong>
              </div>
            </div>

            <div class="studio-definition-actions">
              <span class="studio-inline-label">{{ catalog.designerLabel }}</span>
            </div>
          </div>

          <div class="studio-card-tools">
            <button type="button" class="secondary-btn mini-btn" @click.stop="openStudio(catalog.entryDefinitionId)">打开</button>
            <button
              type="button"
              class="danger-btn mini-btn"
              :disabled="loading"
              @click.stop="deleteCatalogEntry(catalog)"
            >
              删除
            </button>
          </div>
        </button>
        <div v-if="!processCatalogCards.length && !loading" class="empty-state studio-grid-empty">当前没有符合条件的流程分类。</div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { http } from '../api/http'

const router = useRouter()
const loading = ref(false)
const processDefinitions = ref([])
const statusMessage = ref('')
const errorMessage = ref('')

const filters = reactive({
  processKey: '',
  processName: '',
  status: ''
})

const filteredDefinitions = computed(() =>
  processDefinitions.value.filter((item) => {
    if (filters.status === 'ACTIVE' && item.status !== 'ACTIVE') {
      return false
    }
    if (filters.status === 'DRAFT' && item.status === 'ACTIVE') {
      return false
    }
    if (filters.processKey && !String(item.processKey || '').toLowerCase().includes(filters.processKey.toLowerCase())) {
      return false
    }
    if (filters.processName && !String(item.processName || '').toLowerCase().includes(filters.processName.toLowerCase())) {
      return false
    }
    return true
  })
)

const processCatalogCards = computed(() => {
  const grouped = new Map()
  filteredDefinitions.value.forEach((item) => {
    const key = item.processKey || `definition-${item.id}`
    if (!grouped.has(key)) {
      grouped.set(key, [])
    }
    grouped.get(key).push(item)
  })
  return [...grouped.values()]
    .map((items) => {
      const ordered = [...items].sort((a, b) => (b.version || 0) - (a.version || 0))
      const active = ordered.find((item) => item.status === 'ACTIVE') || null
      const latest = ordered[0]
      return {
        processKey: latest.processKey,
        processName: latest.processName,
        entryDefinitionId: (active || latest).id,
        definitionCount: ordered.length,
        updatedAt: latest.updatedAt || latest.createdAt,
        hasActive: Boolean(active),
        activeVersionLabel: active ? `v${active.version}` : '无',
        versionSummary: ordered.length > 1 ? `v${ordered[0].version} ~ v${ordered[ordered.length - 1].version}` : `v${latest.version}`,
        designerLabel: latest.designerType === 'CUSTOM' ? '可视化设计器' : 'BPMN 设计器'
      }
    })
    .sort((a, b) => String(a.processName || '').localeCompare(String(b.processName || ''), 'zh-CN'))
})

const activeCatalogCount = computed(() => processCatalogCards.value.filter((item) => item.hasActive).length)

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function applyFilters() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function resetFilters() {
  filters.processKey = ''
  filters.processName = ''
  filters.status = ''
  statusMessage.value = ''
  errorMessage.value = ''
}

function openStudio(id) {
  router.push(id ? { path: '/designer/studio', query: { id } } : { path: '/designer/studio' })
}

async function fetchProcessDefinitions() {
  const { data } = await http.get('/process-definition/all')
  processDefinitions.value = data || []
}

async function deleteCatalogEntry(catalog) {
  if (!catalog?.entryDefinitionId) {
    return
  }
  if (!window.confirm(`确认删除流程分类「${catalog.processName}」当前入口版本吗？`)) {
    return
  }
  loading.value = true
  statusMessage.value = ''
  errorMessage.value = ''
  try {
    await http.delete(`/process-definition/delete/${catalog.entryDefinitionId}`)
    await fetchProcessDefinitions()
    statusMessage.value = `已删除 ${catalog.processName}`
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message || '删除失败'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await fetchProcessDefinitions()
  } catch (error) {
    errorMessage.value = error?.normalizedMessage || error?.message || '流程分类加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-grid {
  gap: 12px;
}

.studio-filter-panel,
.studio-catalog-panel {
  padding: 14px;
}

.studio-plain-panel {
  border-radius: 0;
  box-shadow: none;
  border-color: #d8dee8;
  background: #ffffff;
}

.studio-filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
}

.studio-filter-grid label {
  display: grid;
  gap: 6px;
}

.studio-filter-grid span {
  color: #707b8a;
  font-size: 12px;
}

.studio-filter-grid :deep(input),
.studio-filter-grid :deep(select) {
  height: 34px;
  padding: 0 12px;
  border-radius: 2px;
}

.studio-filter-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-start;
}

.studio-search-btn,
.studio-new-btn {
  min-width: 60px;
  height: 34px;
  padding-inline: 14px;
  border-radius: 2px;
}

.studio-definition-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  justify-content: start;
}

.studio-definition-tile {
  border: 1px solid #dde3eb;
  border-radius: 6px;
  background: #fff;
  padding: 14px 16px;
  text-align: left;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  box-shadow: none;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.studio-definition-tile:hover {
  border-color: rgba(47, 118, 246, 0.35);
  background: #fcfdff;
}

.studio-definition-main {
  min-width: 0;
}

.studio-definition-tile-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.studio-definition-title-block {
  min-width: 0;
}

.studio-definition-title-block strong {
  color: #2e3642;
  font-size: 15px;
  font-weight: 500;
}

.studio-definition-title-block p {
  margin: 6px 0 0;
  color: #6f7d90;
  font-size: 12px;
  font-weight: 600;
}

.studio-card-tools {
  display: flex;
  align-items: center;
  gap: 8px;
  align-self: start;
}

.studio-definition-matrix {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.studio-definition-matrix div {
  border: 1px solid #edf1f5;
  border-radius: 4px;
  padding: 9px 10px;
  background: #fafbfd;
  display: grid;
  gap: 4px;
}

.studio-definition-matrix span {
  color: #727d8b;
  font-size: 12px;
  padding: 0;
  border-bottom: 0;
  background: transparent;
}

.studio-definition-matrix strong {
  color: #2f3947;
  font-size: 13px;
  word-break: break-word;
  padding: 0;
}

.studio-definition-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 10px;
}

.studio-inline-label {
  font-size: 12px;
  color: #6d7784;
}

.studio-grid-empty {
  grid-column: 1 / -1;
  padding: 20px;
  border: 1px dashed #d7e1ef;
  border-radius: 6px;
  background: #fafbfd;
}

@media (max-width: 1180px) {
  .studio-filter-grid {
    grid-template-columns: 1fr;
  }

  .studio-definition-tile {
    grid-template-columns: 1fr;
  }

  .studio-definition-matrix {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .studio-definition-matrix {
    grid-template-columns: 1fr;
  }
}
</style>
