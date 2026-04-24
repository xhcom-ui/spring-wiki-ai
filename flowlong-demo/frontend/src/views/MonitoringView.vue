<template>
  <div class="page-grid monitoring-shell">
    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Monitor Center</p>
          <h3>流程监控中心</h3>
        </div>
        <div class="management-meta">
          <span class="meta-pill">运行中 {{ runningTotal }}</span>
          <span class="meta-pill">已完成 {{ completedTotal }}</span>
          <span class="meta-pill">当前任务 {{ taskTotal }}</span>
        </div>
      </div>

      <div class="management-toolbar monitoring-toolbar-compact">
        <div class="management-filters monitoring-filters-compact">
          <label class="filter-grow monitoring-search-field">
            <span>搜索流程</span>
            <input
              v-model.trim="keyword"
              type="text"
              placeholder="搜索实例 ID、定义、业务键、申请人、节点标签或任务名"
              @keyup.enter="applyFilters"
            />
          </label>
          <label class="compact-field monitoring-compact-field">
            <span>任务分配</span>
            <select v-model="assigneeFilter">
              <option value="all">全部</option>
              <option value="assigned">已分配</option>
              <option value="unassigned">未分配</option>
            </select>
          </label>
          <label class="compact-field monitoring-compact-field">
            <span>历史面板</span>
            <select v-model="historyScope">
              <option value="running">运行中</option>
              <option value="completed">已完成</option>
            </select>
          </label>
        </div>

        <div class="management-actions">
          <button type="button" class="ghost-btn" :disabled="pageLoading" @click="applyFilters">搜索</button>
          <button type="button" class="secondary-btn" :disabled="pageLoading" @click="resetFilters">重置</button>
          <button type="button" class="ghost-btn" :disabled="pageLoading" @click="refreshAll">刷新</button>
        </div>
      </div>

      <div class="monitor-filter-summary">
        <span class="meta-pill">当前筛选命中 {{ totalMatchedCount }}</span>
        <span class="meta-pill">运行中 {{ runningTotal }}</span>
        <span class="meta-pill">任务 {{ taskTotal }}</span>
        <span class="meta-pill">已完成 {{ completedTotal }}</span>
        <span class="meta-pill">{{ keywordSummary }}</span>
        <span class="meta-pill">任务分配 {{ assigneeFilterLabel }}</span>
        <span class="meta-pill">历史面板 {{ historyScopeLabel }}</span>
      </div>

      <p v-if="pageError" class="feedback error">{{ pageError }}</p>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <span class="muted-text">运行中流程</span>
        <strong>{{ statistics.runningProcessCount || 0 }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">已完成流程</span>
        <strong>{{ statistics.completedProcessCount || 0 }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">已分配任务</span>
        <strong>{{ statistics.assignedTaskCount || 0 }}</strong>
      </article>
      <article class="stat-card">
        <span class="muted-text">未分配任务</span>
        <strong>{{ statistics.unassignedTaskCount || 0 }}</strong>
      </article>
    </section>

    <section class="content-grid two-column">
      <DataTableCard
        eyebrow="Runtime Instances"
        title="运行中流程"
        :count-label="`共 ${runningTotal} 条`"
        :page="runningPage"
        :size="runningSize"
        :total="runningTotal"
        :disabled="runningLoading"
        @update:page="changeRunningPage"
        @update:size="changeRunningSize"
      >
        <table class="simple-table">
          <thead>
            <tr>
              <th>实例 ID</th>
              <th>定义 Key</th>
              <th>当前节点</th>
              <th>申请人</th>
              <th>开始时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="instance in filteredRunningProcesses" :key="instance.id">
              <td>{{ instance.id }}</td>
              <td>{{ instance.processDefinitionKey }}</td>
              <td>{{ instance.currentNodeName || '-' }}</td>
              <td>{{ instance.applicant || '-' }}</td>
              <td>{{ formatDate(instance.startTime) }}</td>
              <td>
                <div class="inline-actions">
                  <button type="button" class="secondary-btn mini-btn" @click="loadHistory(instance.id)">历史</button>
                  <button type="button" class="primary-btn mini-btn" @click="openDetail(instance.id)">详情</button>
                </div>
              </td>
            </tr>
            <tr v-if="runningLoading">
              <td colspan="6" class="empty-row">运行中流程加载中...</td>
            </tr>
            <tr v-else-if="runningError">
              <td colspan="6" class="empty-row">{{ runningError }}</td>
            </tr>
            <tr v-else-if="!filteredRunningProcesses.length">
              <td colspan="6" class="empty-row">
                <div class="table-empty-state">
                  <strong>没有匹配的运行中实例</strong>
                  <p>可以调整关键词或切换筛选条件。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </DataTableCard>

      <DataTableCard
        eyebrow="Task Pool"
        title="当前任务"
        :count-label="`共 ${taskTotal} 条`"
        :page="taskPage"
        :size="taskSize"
        :total="taskTotal"
        :disabled="taskLoading"
        @update:page="changeTaskPage"
        @update:size="changeTaskSize"
      >
        <table class="simple-table">
          <thead>
            <tr>
              <th>节点标签</th>
              <th>处理人</th>
              <th>流程实例</th>
              <th>申请人</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in filteredTasks" :key="task.id">
              <td>{{ task.nodeLabel || task.name }}</td>
              <td>{{ task.assignee || '未分配' }}</td>
              <td>{{ task.processInstanceId }}</td>
              <td>{{ task.applicant || '-' }}</td>
              <td>{{ formatDate(task.createTime) }}</td>
              <td>
                <button
                  v-if="task.processInstanceId"
                  type="button"
                  class="secondary-btn mini-btn"
                  @click="openDetail(task.processInstanceId)"
                >
                  查看实例
                </button>
              </td>
            </tr>
            <tr v-if="taskLoading">
              <td colspan="6" class="empty-row">当前任务加载中...</td>
            </tr>
            <tr v-else-if="taskError">
              <td colspan="6" class="empty-row">{{ taskError }}</td>
            </tr>
            <tr v-else-if="!filteredTasks.length">
              <td colspan="6" class="empty-row">
                <div class="table-empty-state">
                  <strong>没有匹配的任务数据</strong>
                  <p>可以切换任务分配状态或重新搜索。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </DataTableCard>
    </section>

    <section class="content-grid two-column">
      <DataTableCard
        eyebrow="Completed"
        title="已完成流程"
        :count-label="`共 ${completedTotal} 条`"
        :page="completedPage"
        :size="completedSize"
        :total="completedTotal"
        :disabled="completedLoading"
        @update:page="changeCompletedPage"
        @update:size="changeCompletedSize"
      >
        <table class="simple-table">
          <thead>
            <tr>
              <th>实例 ID</th>
              <th>定义名称</th>
              <th>实际走过节点</th>
              <th>申请人</th>
              <th>结束时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="instance in filteredCompletedProcesses" :key="instance.id">
              <td>{{ instance.id }}</td>
              <td>{{ instance.processDefinitionName }}</td>
              <td>{{ joinLabels(instance.passedNodeLabels) }}</td>
              <td>{{ instance.applicant || '-' }}</td>
              <td>{{ formatDate(instance.endTime) }}</td>
              <td>
                <div class="inline-actions">
                  <button type="button" class="secondary-btn mini-btn" @click="loadHistory(instance.id)">历史</button>
                  <button type="button" class="primary-btn mini-btn" @click="openDetail(instance.id)">详情</button>
                </div>
              </td>
            </tr>
            <tr v-if="completedLoading">
              <td colspan="6" class="empty-row">已完成流程加载中...</td>
            </tr>
            <tr v-else-if="completedError">
              <td colspan="6" class="empty-row">{{ completedError }}</td>
            </tr>
            <tr v-else-if="!filteredCompletedProcesses.length">
              <td colspan="6" class="empty-row">
                <div class="table-empty-state">
                  <strong>没有匹配的已完成实例</strong>
                  <p>可以调整搜索条件，或者等待新的流程归档。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </DataTableCard>

      <DataTableCard
        eyebrow="Instance Timeline"
        title="流程历史"
        :count-label="selectedHistoryLabel"
        :pagination="false"
      >
        <table class="simple-table">
          <thead>
            <tr>
              <th>节点标签</th>
              <th>处理人</th>
              <th>意见</th>
              <th>开始</th>
              <th>结束</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in history" :key="item.id">
              <td>{{ item.nodeLabel || item.name }}</td>
              <td>{{ item.operatorName || item.assignee || '-' }}</td>
              <td>{{ formatAuditComment(item) }}</td>
              <td>{{ formatDate(item.createTime) }}</td>
              <td>{{ formatDate(item.endTime) }}</td>
            </tr>
            <tr v-if="historyLoading">
              <td colspan="5" class="empty-row">流程历史加载中...</td>
            </tr>
            <tr v-else-if="historyError">
              <td colspan="5" class="empty-row">{{ historyError }}</td>
            </tr>
            <tr v-else-if="!history.length">
              <td colspan="5" class="empty-row">
                <div class="table-empty-state">
                  <strong>尚未选择实例历史</strong>
                  <p>点击运行中或已完成列表里的“历史”即可查看。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </DataTableCard>
    </section>

    <div v-if="detailVisible" class="modal-mask" @click.self="closeDetail">
      <div class="modal-panel detail-modal compact-detail-modal">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Process Inspector</p>
            <h3>流程实例详情</h3>
          </div>
          <div class="inline-actions">
            <button
              v-if="canOpenLeave && detail?.leave?.applicant"
              type="button"
              class="secondary-btn"
              @click="openLeaveFromDetail"
            >
              查看申请页
            </button>
            <button
              v-if="canOpenTasks && detail?.processInstanceId"
              type="button"
              class="secondary-btn"
              @click="openTasksFromDetail"
            >
              查看待办页
            </button>
            <span v-if="detail?.status" :class="['status-pill', detail.active ? 'success-pill' : 'muted-pill']">
              {{ detail.active ? '运行中' : '已完成' }}
            </span>
            <button type="button" class="ghost-btn" @click="closeDetail">关闭</button>
          </div>
        </div>

        <p v-if="detailError" class="feedback error">{{ detailError }}</p>
        <p v-else-if="detailLoading" class="muted-text">实例详情加载中...</p>

        <template v-else-if="detail">
          <section class="detail-grid compact-detail-grid">
            <article class="detail-item">
              <span class="detail-key">实例 ID</span>
              <strong class="detail-value">{{ detail.processInstanceId }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">流程定义</span>
              <strong class="detail-value">{{ detail.processDefinitionName || '-' }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">业务键</span>
              <strong class="detail-value">{{ detail.businessKey || '-' }}</strong>
            </article>
            <article class="detail-item">
              <span class="detail-key">状态</span>
              <strong class="detail-value">{{ detail.status || '-' }}</strong>
            </article>
          </section>

          <div class="detail-tabs">
            <button
              v-for="tab in detailTabs"
              :key="tab.value"
              type="button"
              :class="['tab-btn', { active: detailTab === tab.value }]"
              @click="detailTab = tab.value"
            >
              {{ tab.label }}
            </button>
          </div>

          <section v-if="detailTab === 'overview'" class="page-grid">
            <section class="detail-grid">
              <article class="detail-item">
                <span class="detail-key">定义 Key</span>
                <strong class="detail-value">{{ detail.processDefinitionKey || '-' }}</strong>
              </article>
              <article class="detail-item">
                <span class="detail-key">部署 ID</span>
                <strong class="detail-value">{{ detail.deploymentId || '-' }}</strong>
              </article>
              <article class="detail-item">
                <span class="detail-key">开始时间</span>
                <strong class="detail-value">{{ formatDate(detail.startTime) }}</strong>
              </article>
              <article class="detail-item">
                <span class="detail-key">结束时间</span>
                <strong class="detail-value">{{ formatDate(detail.endTime) }}</strong>
              </article>
            </section>

            <section class="content-grid two-column">
              <article class="panel-card">
                <div class="panel-head">
                  <div>
                    <p class="eyebrow">Business Snapshot</p>
                    <h3>业务快照</h3>
                  </div>
                </div>
                <table class="simple-table">
                  <tbody>
                    <tr>
                      <th>申请人</th>
                      <td>{{ detail.leave?.applicant || detail.variables?.applicant || '-' }}</td>
                    </tr>
                    <tr>
                      <th>部门审批人</th>
                      <td>{{ detail.leave?.deptManager || detail.variables?.deptManager || '-' }}</td>
                    </tr>
                    <tr>
                      <th>总经理审批人</th>
                      <td>{{ detail.leave?.generalManager || detail.variables?.generalManager || '-' }}</td>
                    </tr>
                    <tr>
                      <th>请假天数</th>
                      <td>{{ detail.leave?.days || detail.variables?.days || '-' }}</td>
                    </tr>
                    <tr>
                      <th>实际走过节点</th>
                      <td>{{ joinLabels(detail.passedNodeLabels) }}</td>
                    </tr>
                  </tbody>
                </table>
              </article>

              <article class="panel-card">
                <div class="panel-head">
                  <div>
                    <p class="eyebrow">Current Tasks</p>
                    <h3>当前任务</h3>
                  </div>
                </div>
                <table class="simple-table">
                  <thead>
                    <tr>
                      <th>节点标签</th>
                      <th>处理人</th>
                      <th>创建时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="task in detail.currentTasks || []" :key="task.id">
                      <td>{{ task.nodeLabel || task.name }}</td>
                      <td>{{ task.assignee || '未分配' }}</td>
                      <td>{{ formatDate(task.createTime) }}</td>
                    </tr>
                    <tr v-if="!(detail.currentTasks || []).length">
                      <td colspan="3" class="empty-row">当前没有活动任务</td>
                    </tr>
                  </tbody>
                </table>
              </article>
            </section>
          </section>

          <article v-else-if="detailTab === 'variables'" class="panel-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">Variables</p>
                <h3>当前变量与快照时间线</h3>
              </div>
            </div>
            <section class="content-grid two-column">
              <article class="panel-card">
                <div class="panel-head">
                  <div>
                    <p class="eyebrow">Current Variables</p>
                    <h3>当前变量</h3>
                  </div>
                </div>
                <table class="simple-table">
                  <thead>
                    <tr>
                      <th>变量名</th>
                      <th>变量值</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in detailVariables" :key="item.key">
                      <td>{{ item.key }}</td>
                      <td>{{ item.value }}</td>
                    </tr>
                    <tr v-if="!detailVariables.length">
                      <td colspan="2" class="empty-row">没有可展示的流程变量</td>
                    </tr>
                  </tbody>
                </table>
              </article>

              <article class="panel-card">
                <div class="panel-head">
                  <div>
                    <p class="eyebrow">Snapshots</p>
                    <h3>变量快照</h3>
                  </div>
                </div>
                <div class="stack-list">
                  <div v-for="item in detail.variableSnapshots || []" :key="`${item.actionType}-${item.createdAt}-${item.nodeLabel}`" class="list-item rich-list-item">
                    <div>
                      <strong>{{ item.nodeLabel || item.actionType }}</strong>
                      <p>{{ item.operatorName || '-' }} · {{ formatDate(item.createdAt) }}</p>
                    </div>
                    <span class="status-pill">{{ item.actionType }}</span>
                  </div>
                  <div v-if="!(detail.variableSnapshots || []).length" class="empty-state">当前实例还没有变量快照。</div>
                </div>
              </article>
            </section>
          </article>

          <article v-else-if="detailTab === 'trajectory'" class="panel-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">Trajectory</p>
                <h3>BPMN 高亮轨迹图</h3>
              </div>
            </div>
            <div class="management-meta">
              <span class="meta-pill">已完成节点 {{ (detail.completedActivityIds || []).length }}</span>
              <span class="meta-pill">活动节点 {{ (detail.activeActivityIds || []).length }}</span>
              <span class="meta-pill">已走过网关 {{ (detail.highlightedGatewayIds || []).length }}</span>
              <span class="meta-pill">高亮连线 {{ (detail.highlightedSequenceFlowIds || []).length }}</span>
            </div>
            <BpmnTraceViewer
              v-if="detail.bpmnXml"
              :xml="detail.bpmnXml"
              :highlighted-node-ids="detail.highlightedNodeIds || []"
              :completed-activity-ids="detail.completedActivityIds || []"
              :active-activity-ids="detail.activeActivityIds || []"
              :gateway-ids="detail.highlightedGatewayIds || []"
              :current-activity-id="detail.currentActivityId || ''"
              :selected-node-id="selectedTraceNodeId"
              :sequence-flow-ids="detail.highlightedSequenceFlowIds || []"
              @node-click="handleTraceNodeClick"
            />
            <div v-else class="empty-state">当前实例缺少可展示的 BPMN 定义，无法渲染高亮轨迹图。</div>
          </article>

          <article v-else class="panel-card">
            <div class="panel-head">
              <div>
                <p class="eyebrow">Execution Audit</p>
                <h3>节点审计与快照</h3>
              </div>
            </div>
            <section class="content-grid two-column">
              <article class="panel-card">
                <table class="simple-table">
                  <thead>
                    <tr>
                      <th>节点标签</th>
                      <th>动作</th>
                      <th>处理人</th>
                      <th>时间</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="item in detail.auditLogs || []"
                      :key="item.id"
                      :class="{ 'selected-history-row': selectedAuditLog?.id === item.id }"
                      @click="selectAuditLog(item)"
                    >
                      <td>{{ item.nodeLabel || item.taskName || '-' }}</td>
                      <td>{{ item.actionType || '-' }}</td>
                      <td>{{ item.operatorName || '-' }}</td>
                      <td>{{ formatDate(item.createdAt) }}</td>
                    </tr>
                    <tr v-if="!(detail.auditLogs || []).length">
                      <td colspan="4" class="empty-row">暂无节点审计数据</td>
                    </tr>
                  </tbody>
                </table>
              </article>

              <article class="panel-card">
                <div class="panel-head">
                  <div>
                    <p class="eyebrow">Node Detail</p>
                    <h3>{{ selectedAuditLog?.nodeLabel || '选择左侧节点审计记录' }}</h3>
                  </div>
                </div>
                <template v-if="selectedAuditLog">
                  <section class="detail-grid">
                    <article class="detail-item">
                      <span class="detail-key">业务页</span>
                      <strong class="detail-value">{{ selectedAuditLog.pageLabel || '-' }}</strong>
                    </article>
                    <article class="detail-item">
                      <span class="detail-key">表单</span>
                      <strong class="detail-value">{{ selectedAuditLog.formLabel || selectedAuditLog.formKey || '-' }}</strong>
                    </article>
                    <article class="detail-item">
                      <span class="detail-key">审批动作</span>
                      <strong class="detail-value">{{ selectedAuditLog.actionType || '-' }}</strong>
                    </article>
                    <article class="detail-item">
                      <span class="detail-key">处理人</span>
                      <strong class="detail-value">{{ selectedAuditLog.operatorName || '-' }}</strong>
                    </article>
                  </section>

                  <article class="detail-item node-detail-block">
                    <span class="detail-key">意见摘要</span>
                    <strong class="detail-value">{{ formatAuditComment(selectedAuditLog) }}</strong>
                  </article>

                  <section class="detail-grid">
                    <article class="detail-item">
                      <span class="detail-key">审批意见</span>
                      <strong class="detail-value">{{ selectedAuditLog.approvalComment || '-' }}</strong>
                    </article>
                    <article class="detail-item">
                      <span class="detail-key">退回原因</span>
                      <strong class="detail-value">{{ selectedAuditLog.rejectReason || '-' }}</strong>
                    </article>
                    <article class="detail-item">
                      <span class="detail-key">系统备注</span>
                      <strong class="detail-value">{{ selectedAuditLog.systemRemark || '-' }}</strong>
                    </article>
                    <article class="detail-item">
                      <span class="detail-key">快照表单</span>
                      <strong class="detail-value">{{ selectedAuditFormConfig.title }}</strong>
                    </article>
                  </section>

                  <section class="content-grid two-column">
                    <article class="panel-card">
                      <div class="panel-head">
                        <div>
                          <p class="eyebrow">Variable Snapshot</p>
                          <h3>变量快照</h3>
                        </div>
                      </div>
                      <ReadonlyFormSnapshot
                        :form-key="selectedAuditLog.formKey"
                        :source="selectedAuditLog.variableSnapshot || {}"
                        :include-rest="true"
                        :extra-labels="snapshotExtraLabels"
                        empty-text="没有变量快照"
                      />
                    </article>

                    <article class="panel-card">
                      <div class="panel-head">
                        <div>
                          <p class="eyebrow">Form Snapshot</p>
                          <h3>表单快照</h3>
                        </div>
                      </div>
                      <ReadonlyFormSnapshot
                        :form-key="selectedAuditLog.formKey"
                        :source="selectedAuditLog.formSnapshot || {}"
                        :include-rest="true"
                        :extra-labels="snapshotExtraLabels"
                        empty-text="没有表单快照"
                      />
                    </article>
                  </section>
                </template>
                <div v-else class="empty-state">点击左侧某个节点审计记录后，这里会展示审批意见、变量快照和表单快照。</div>
              </article>
            </section>
          </article>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BpmnTraceViewer from '../components/BpmnTraceViewer.vue'
import DataTableCard from '../components/common/DataTableCard.vue'
import ReadonlyFormSnapshot from '../components/ReadonlyFormSnapshot.vue'
import { canAccessPath, http } from '../api/http'
import { usePagedQuery } from '../composables/usePagedQuery'
import { ensureRemoteFormRegistryLoaded, resolveFormConfig } from '../utils/businessForms'

const route = useRoute()
const router = useRouter()
const statistics = ref({})
const history = ref([])
const keyword = ref('')
const assigneeFilter = ref('all')
const historyScope = ref('running')
const selectedHistoryId = ref(null)
const historyLoading = ref(false)
const historyError = ref('')
const pageError = ref('')
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref(null)
const detailTab = ref('overview')
const selectedAuditLog = ref(null)
const selectedTraceNodeId = ref('')
const canOpenLeave = canAccessPath('/leave', 'leave:submit')
const canOpenTasks = canAccessPath('/tasks', 'task:approve')

const detailTabs = [
  { value: 'overview', label: '概览' },
  { value: 'variables', label: '变量' },
  { value: 'trajectory', label: '轨迹' },
  { value: 'history', label: '节点审计' }
]

function formatDate(value) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString()
}

function stringifyValue(value) {
  if (value == null) {
    return '-'
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

function joinLabels(labels) {
  return Array.isArray(labels) && labels.length ? labels.join(' -> ') : '-'
}

const {
  items: runningProcesses,
  loading: runningLoading,
  error: runningError,
  total: runningTotal,
  page: runningPage,
  size: runningSize,
  load: loadRunningProcesses,
  search: searchRunningProcesses,
  changePage: changeRunningPageQuery,
  changeSize: changeRunningSizeQuery
} = usePagedQuery(async ({ page, size, keyword }) => {
  const { data } = await http.get('/monitoring/processes/running/query', {
    params: {
      page,
      size,
      keyword
    }
  })
  return data
}, {
  page: 1,
  size: 8,
  filters: {
    keyword: undefined
  }
})

const {
  items: completedProcesses,
  loading: completedLoading,
  error: completedError,
  total: completedTotal,
  page: completedPage,
  size: completedSize,
  load: loadCompletedProcesses,
  search: searchCompletedProcesses,
  changePage: changeCompletedPageQuery,
  changeSize: changeCompletedSizeQuery
} = usePagedQuery(async ({ page, size, keyword }) => {
  const { data } = await http.get('/monitoring/processes/completed/query', {
    params: {
      page,
      size,
      keyword
    }
  })
  return data
}, {
  page: 1,
  size: 8,
  filters: {
    keyword: undefined
  }
})

const {
  items: tasks,
  loading: taskLoading,
  error: taskError,
  total: taskTotal,
  page: taskPage,
  size: taskSize,
  load: loadTasks,
  search: searchTasks,
  changePage: changeTaskPageQuery,
  changeSize: changeTaskSizeQuery
} = usePagedQuery(async ({ page, size, keyword, assigneeFilter }) => {
  const { data } = await http.get('/monitoring/tasks/query', {
    params: {
      page,
      size,
      keyword,
      assigneeFilter
    }
  })
  return data
}, {
  page: 1,
  size: 8,
  filters: {
    keyword: undefined,
    assigneeFilter: 'all'
  }
})

const filteredRunningProcesses = computed(() => runningProcesses.value)
const filteredCompletedProcesses = computed(() => completedProcesses.value)
const filteredTasks = computed(() => tasks.value)
const pageLoading = computed(() => runningLoading.value || completedLoading.value || taskLoading.value)
const totalMatchedCount = computed(() => runningTotal.value + taskTotal.value + completedTotal.value)
const keywordSummary = computed(() => keyword.value ? `关键词 ${keyword.value}` : '关键词 全部')
const assigneeFilterLabel = computed(() => {
  if (assigneeFilter.value === 'assigned') {
    return '已分配'
  }
  if (assigneeFilter.value === 'unassigned') {
    return '未分配'
  }
  return '全部'
})
const historyScopeLabel = computed(() => (historyScope.value === 'completed' ? '已完成' : '运行中'))

const selectedHistoryLabel = computed(() => {
  if (!selectedHistoryId.value) {
    return '点击实例历史后展示'
  }
  return `实例 ${selectedHistoryId.value} 的执行轨迹`
})

const detailVariables = computed(() =>
  Object.entries(detail.value?.variables || {}).map(([key, value]) => ({
    key,
    value: stringifyValue(value)
  }))
)
const selectedAuditFormConfig = computed(() => resolveFormConfig(selectedAuditLog.value?.formKey))
const snapshotExtraLabels = {
  approved: '审批结果',
  comment: '意见摘要',
  approvalComment: '审批意见',
  rejectReason: '退回原因',
  systemRemark: '系统备注',
  pageLabel: '业务页标签',
  formLabel: '表单标签',
  formKey: '表单 Key'
}

async function resetFilters() {
  keyword.value = ''
  assigneeFilter.value = 'all'
  historyScope.value = 'running'
  await applyFilters()
}

function openLeaveFromDetail() {
  router.push({
    path: '/leave',
    query: {
      applicant: detail.value?.leave?.applicant || '',
      processInstanceId: detail.value?.processInstanceId || ''
    }
  })
}

function openTasksFromDetail() {
  router.push({
    path: '/tasks',
    query: {
      processInstanceId: detail.value?.processInstanceId || ''
    }
  })
}

function closeDetail() {
  detailVisible.value = false
  detailLoading.value = false
  detailError.value = ''
  detail.value = null
  detailTab.value = 'overview'
  selectedAuditLog.value = null
  selectedTraceNodeId.value = ''
}

function selectAuditLog(item) {
  selectedAuditLog.value = item
}

function handleTraceNodeClick(event) {
  const elementId = event?.id
  if (!elementId) {
    return
  }
  selectedTraceNodeId.value = elementId
  if (!detail.value?.auditLogs?.length) {
    return
  }
  const matched = detail.value.auditLogs.find((item) => item.taskKey === elementId)
  if (!matched) {
    return
  }
  detailTab.value = 'history'
  selectedAuditLog.value = matched
}

function formatAuditComment(item) {
  if (!item) {
    return '-'
  }
  return item.approvalComment || item.rejectReason || item.systemRemark || item.comment || '-'
}

async function refreshStatistics() {
  const { data } = await http.get('/monitoring/statistics')
  statistics.value = data || {}
}

async function syncHistorySource() {
  const source = historyScope.value === 'completed'
    ? filteredCompletedProcesses.value[0] || completedProcesses.value[0]
    : filteredRunningProcesses.value[0] || runningProcesses.value[0]
  if (source?.id) {
    await loadHistory(source.id)
  } else {
    selectedHistoryId.value = null
    history.value = []
  }
}

async function applyFilters() {
  pageError.value = ''
  try {
    await Promise.all([
      searchRunningProcesses({
        keyword: keyword.value || undefined
      }),
      searchCompletedProcesses({
        keyword: keyword.value || undefined
      }),
      searchTasks({
        keyword: keyword.value || undefined,
        assigneeFilter: assigneeFilter.value
      })
    ])
    await syncHistorySource()
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '监控列表加载失败'
  }
}

async function refreshAll() {
  pageError.value = ''
  try {
    await Promise.all([
      refreshStatistics(),
      loadRunningProcesses(),
      loadCompletedProcesses(),
      loadTasks()
    ])
    await syncHistorySource()
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '监控中心刷新失败'
  }
}

async function applyRouteContext() {
  const applicant = String(route.query.applicant || '')
  const businessKey = String(route.query.businessKey || '')
  const processInstanceId = String(route.query.processInstanceId || '')
  const targetTab = String(route.query.tab || '')

  if (applicant) {
    keyword.value = applicant
  } else if (businessKey) {
    keyword.value = businessKey
  }

  if (applicant || businessKey) {
    await applyFilters()
  }

  if (processInstanceId) {
    await loadHistory(processInstanceId)
    if (route.query.openDetail === '1') {
      await openDetail(processInstanceId)
      if (targetTab && detailTabs.some((item) => item.value === targetTab)) {
        detailTab.value = targetTab
      }
    }
  }
}

async function loadHistory(processInstanceId) {
  selectedHistoryId.value = processInstanceId
  historyLoading.value = true
  historyError.value = ''
  try {
    const { data } = await http.get(`/monitoring/processes/${processInstanceId}/history`)
    history.value = data || []
  } catch (error) {
    history.value = []
    historyError.value = error.normalizedMessage || error.message || '流程历史加载失败'
  } finally {
    historyLoading.value = false
  }
}

async function changeRunningPage(nextPage) {
  try {
    await changeRunningPageQuery(nextPage)
    if (historyScope.value === 'running') {
      await syncHistorySource()
    }
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '运行中流程翻页失败'
  }
}

async function changeRunningSize(nextSize) {
  try {
    await changeRunningSizeQuery(nextSize)
    if (historyScope.value === 'running') {
      await syncHistorySource()
    }
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '运行中流程分页大小更新失败'
  }
}

async function changeTaskPage(nextPage) {
  try {
    await changeTaskPageQuery(nextPage)
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '任务列表翻页失败'
  }
}

async function changeTaskSize(nextSize) {
  try {
    await changeTaskSizeQuery(nextSize)
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '任务列表分页大小更新失败'
  }
}

async function changeCompletedPage(nextPage) {
  try {
    await changeCompletedPageQuery(nextPage)
    if (historyScope.value === 'completed') {
      await syncHistorySource()
    }
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '已完成流程翻页失败'
  }
}

async function changeCompletedSize(nextSize) {
  try {
    await changeCompletedSizeQuery(nextSize)
    if (historyScope.value === 'completed') {
      await syncHistorySource()
    }
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '已完成流程分页大小更新失败'
  }
}

async function openDetail(processInstanceId) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = null
  detailTab.value = 'overview'
  selectedAuditLog.value = null
  selectedTraceNodeId.value = ''
  try {
    const { data } = await http.get(`/monitoring/processes/${processInstanceId}/detail`)
    detail.value = data || null
    selectedAuditLog.value = detail.value?.auditLogs?.[0] || null
  } catch (error) {
    detailError.value = error.normalizedMessage
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  try {
    await ensureRemoteFormRegistryLoaded().catch(() => {})
    await refreshAll()
    await applyRouteContext()
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '监控中心初始化失败'
  }
})

watch(historyScope, async () => {
  try {
    await syncHistorySource()
  } catch (error) {
    historyError.value = error.normalizedMessage || error.message || '流程历史切换失败'
  }
})

watch(
  () => route.fullPath,
  async () => {
    try {
      await applyRouteContext()
    } catch (error) {
      pageError.value = error.normalizedMessage || error.message || '路由上下文同步失败'
    }
  }
)

watch(
  selectedAuditLog,
  (item) => {
    selectedTraceNodeId.value = item?.taskKey || ''
  },
  { immediate: true }
)
</script>

<style scoped>
.selected-history-row {
  background: rgba(155, 107, 47, 0.08);
}

.node-detail-block {
  margin: 14px 0 18px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 251, 244, 0.84);
  border: 1px solid rgba(87, 69, 44, 0.08);
}

.monitoring-toolbar-compact {
  padding-bottom: 0;
  gap: 10px 14px;
}

.monitoring-filters-compact {
  gap: 10px;
  flex: 1 1 auto;
}

.monitoring-search-field {
  flex: 1 1 320px;
}

.monitoring-compact-field {
  width: 148px;
}

.monitor-filter-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 12px;
}
</style>
