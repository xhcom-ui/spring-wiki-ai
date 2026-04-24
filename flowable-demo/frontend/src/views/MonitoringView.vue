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
              <option v-for="item in assigneeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
            </select>
          </label>
          <label class="compact-field monitoring-compact-field">
            <span>历史面板</span>
            <select v-model="historyScope">
              <option v-for="item in historyScopeOptions" :key="item.value" :value="item.value">
                {{ item.label }}
              </option>
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
      <article class="panel-card">
        <div class="panel-head compact-panel-head">
          <div>
            <p class="eyebrow">Runtime Instances</p>
            <h3>运行中流程</h3>
          </div>
          <span class="muted-text">共 {{ runningTotal }} 条</span>
        </div>
        <table class="simple-table monitoring-table">
          <thead>
            <tr>
              <th>实例</th>
              <th>流程定义</th>
              <th>当前节点</th>
              <th>申请人</th>
              <th>启动时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="instance in filteredRunningProcesses" :key="instance.id">
              <td>
                <strong class="table-strong">{{ shorten(instance.id, 16) }}</strong>
                <p class="table-subtext">{{ instance.businessKey || '无业务键' }}</p>
              </td>
              <td>
                <strong class="table-strong">{{ instance.processDefinitionName }}</strong>
                <p class="table-subtext">{{ instance.processDefinitionKey }}</p>
              </td>
              <td>{{ summarizeNodeLabels(instance.currentNodeLabels) }}</td>
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
        <AppPagination
          :page="runningPage"
          :size="runningSize"
          :total="runningTotal"
          :disabled="runningLoading"
          @update:page="changeRunningPage"
          @update:size="changeRunningSize"
        />
      </article>

      <article class="panel-card">
        <div class="panel-head compact-panel-head">
          <div>
            <p class="eyebrow">Task Pool</p>
            <h3>当前任务</h3>
          </div>
          <span class="muted-text">共 {{ taskTotal }} 条</span>
        </div>
        <table class="simple-table monitoring-table">
          <thead>
            <tr>
              <th>任务</th>
              <th>处理人</th>
              <th>流程实例</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="task in filteredTasks" :key="task.id">
              <td>
                <strong class="table-strong">{{ task.name }}</strong>
                <p class="table-subtext">{{ task.applicant || '未绑定申请人' }}</p>
              </td>
              <td>
                <span :class="['status-pill', task.assignee ? 'warning-pill' : 'muted-pill']">
                  {{ task.assignee || '未分配' }}
                </span>
              </td>
              <td class="table-code">{{ shorten(task.processInstanceId, 16) }}</td>
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
              <td colspan="5" class="empty-row">当前任务加载中...</td>
            </tr>
            <tr v-else-if="taskError">
              <td colspan="5" class="empty-row">{{ taskError }}</td>
            </tr>
            <tr v-else-if="!filteredTasks.length">
              <td colspan="5" class="empty-row">
                <div class="table-empty-state">
                  <strong>没有匹配的任务数据</strong>
                  <p>可以切换任务分配状态或重新搜索。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <AppPagination
          :page="taskPage"
          :size="taskSize"
          :total="taskTotal"
          :disabled="taskLoading"
          @update:page="changeTaskPage"
          @update:size="changeTaskSize"
        />
      </article>
    </section>

    <section class="content-grid two-column monitoring-lower-grid">
      <article class="panel-card">
        <div class="panel-head compact-panel-head">
          <div>
            <p class="eyebrow">Completed</p>
            <h3>已完成流程</h3>
          </div>
          <span class="muted-text">共 {{ completedTotal }} 条</span>
        </div>
        <table class="simple-table monitoring-table">
          <thead>
            <tr>
              <th>实例</th>
              <th>定义名称</th>
              <th>节点标签</th>
              <th>申请人</th>
              <th>耗时</th>
              <th>结束时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="instance in filteredCompletedProcesses" :key="instance.id">
              <td>
                <strong class="table-strong">{{ shorten(instance.id, 16) }}</strong>
                <p class="table-subtext">{{ instance.businessKey || '无业务键' }}</p>
              </td>
              <td>{{ instance.processDefinitionName }}</td>
              <td>{{ summarizeNodeLabels(instance.currentNodeLabels) }}</td>
              <td>{{ instance.applicant || '-' }}</td>
              <td>{{ formatDuration(instance.duration) }}</td>
              <td>{{ formatDate(instance.endTime) }}</td>
              <td>
                <div class="inline-actions">
                  <button type="button" class="secondary-btn mini-btn" @click="loadHistory(instance.id)">历史</button>
                  <button type="button" class="primary-btn mini-btn" @click="openDetail(instance.id)">详情</button>
                </div>
              </td>
            </tr>
            <tr v-if="completedLoading">
              <td colspan="7" class="empty-row">已完成流程加载中...</td>
            </tr>
            <tr v-else-if="completedError">
              <td colspan="7" class="empty-row">{{ completedError }}</td>
            </tr>
            <tr v-else-if="!filteredCompletedProcesses.length">
              <td colspan="7" class="empty-row">
                <div class="table-empty-state">
                  <strong>没有匹配的已完成实例</strong>
                  <p>可以调整搜索条件，或者等待新的流程归档。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <AppPagination
          :page="completedPage"
          :size="completedSize"
          :total="completedTotal"
          :disabled="completedLoading"
          @update:page="changeCompletedPage"
          @update:size="changeCompletedSize"
        />
      </article>

      <article class="panel-card">
        <div class="panel-head compact-panel-head">
          <div>
            <p class="eyebrow">Instance Timeline</p>
            <h3>流程历史</h3>
          </div>
          <span class="muted-text">{{ selectedHistoryLabel }}</span>
        </div>

        <div class="history-summary">
          <article class="history-chip">
            <span>历史来源</span>
            <strong>{{ historyScope === 'running' ? '运行实例' : '已完成实例' }}</strong>
          </article>
          <article class="history-chip">
            <span>轨迹节点</span>
            <strong>{{ history.length }}</strong>
          </article>
        </div>

        <p v-if="historyError" class="feedback error">{{ historyError }}</p>

        <table class="simple-table monitoring-table">
          <thead>
            <tr>
              <th>任务</th>
              <th>处理人</th>
              <th>开始</th>
              <th>结束</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in history" :key="item.id">
              <td>
                {{ item.name }}
                <p class="table-subtext">{{ [item.pageLabel, item.formLabel].filter(Boolean).join(' / ') || '-' }}</p>
              </td>
              <td>{{ item.assignee || '-' }}</td>
              <td>{{ formatDate(item.createTime) }}</td>
              <td>{{ formatDate(item.endTime) }}</td>
            </tr>
            <tr v-if="historyLoading">
              <td colspan="4" class="empty-row">流程历史加载中...</td>
            </tr>
            <tr v-else-if="!history.length">
              <td colspan="4" class="empty-row">
                <div class="table-empty-state">
                  <strong>尚未选择实例历史</strong>
                  <p>点击运行中或已完成列表里的“历史”即可查看。</p>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </article>
    </section>

    <div v-if="detailVisible" class="modal-mask" @click.self="closeDetail">
      <div class="modal-panel detail-modal compact-detail-modal">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Process Inspector</p>
            <h3>流程实例详情</h3>
          </div>
          <div class="inline-actions">
            <span v-if="detail" :class="['status-pill', detailStateClass]">
              {{ detailStateLabel }}
            </span>
            <button type="button" class="ghost-btn" @click="closeDetail">关闭</button>
          </div>
        </div>

        <p v-if="detailError" class="feedback error">{{ detailError }}</p>
        <p v-else-if="detailLoading" class="muted-text">实例详情加载中...</p>

        <template v-else-if="detail">
          <div class="detail-layout">
            <aside class="detail-sidebar">
              <section class="detail-hero-card">
                <span class="detail-hero-label">流程状态</span>
                <strong>{{ detailStateLabel }}</strong>
                <p>{{ detail.processDefinitionName || detail.processDefinitionKey || '未命名流程' }}</p>
                <div class="detail-hero-meta">
                  <span>{{ detail.processInstanceId }}</span>
                  <span>{{ formatDate(detail.startTime) }}</span>
                </div>
              </section>

              <section class="detail-sidebar-card">
                <p class="eyebrow">Quick Jump</p>
                <div class="detail-tab-stack">
                  <button
                    v-for="tab in detailTabs"
                    :key="tab.value"
                    type="button"
                    :class="['tab-btn', { active: detailTab === tab.value }]"
                    @click="detailTab = tab.value"
                  >
                    <span>{{ tab.label }}</span>
                    <small v-if="tab.value === 'variables'">{{ detailVariables.length }}</small>
                    <small v-else-if="tab.value === 'trajectory'">{{ (detail.highlightedNodeIds || []).length }}</small>
                    <small v-else-if="tab.value === 'comments'">{{ detail.comments?.length || 0 }}</small>
                    <small v-else-if="tab.value === 'history'">{{ detail.history?.length || 0 }}</small>
                    <small v-else>{{ detail.currentTasks?.length || 0 }}</small>
                  </button>
                </div>
              </section>

              <section class="detail-sidebar-card">
                <p class="eyebrow">Instance Snapshot</p>
                <div class="detail-summary-list">
                  <article class="detail-summary-item">
                    <span>业务键</span>
                    <strong>{{ detail.businessKey || '-' }}</strong>
                  </article>
                  <article class="detail-summary-item">
                    <span>申请人</span>
                    <strong>{{ detail.leave?.applicant || detail.variables?.applicant || '-' }}</strong>
                  </article>
                  <article class="detail-summary-item">
                    <span>当前任务</span>
                    <strong>{{ detail.currentTasks?.length || 0 }}</strong>
                  </article>
                  <article class="detail-summary-item">
                    <span>流程变量</span>
                    <strong>{{ detailVariables.length }}</strong>
                  </article>
                </div>
              </section>
            </aside>

            <section class="detail-main">
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
                  <span class="detail-key">定义 Key</span>
                  <strong class="detail-value">{{ detail.processDefinitionKey || '-' }}</strong>
                </article>
                <article class="detail-item">
                  <span class="detail-key">部署 ID</span>
                  <strong class="detail-value">{{ detail.deploymentId || '-' }}</strong>
                </article>
              </section>

              <section v-if="detailTab === 'overview'" class="modal-content-stack">
                <section class="detail-grid compact-detail-grid">
                  <article class="detail-item">
                    <span class="detail-key">业务键</span>
                    <strong class="detail-value">{{ detail.businessKey || '-' }}</strong>
                  </article>
                  <article class="detail-item">
                    <span class="detail-key">开始时间</span>
                    <strong class="detail-value">{{ formatDate(detail.startTime) }}</strong>
                  </article>
                  <article class="detail-item">
                    <span class="detail-key">结束时间</span>
                    <strong class="detail-value">{{ formatDate(detail.endTime) }}</strong>
                  </article>
                  <article class="detail-item">
                    <span class="detail-key">耗时</span>
                    <strong class="detail-value">{{ formatDuration(detail.duration) }}</strong>
                  </article>
                </section>

                <section class="content-grid two-column">
                  <article class="panel-card">
                    <div class="panel-head compact-panel-head">
                      <div>
                        <p class="eyebrow">Business Snapshot</p>
                        <h3>业务快照</h3>
                      </div>
                    </div>
                    <table class="simple-table monitoring-table compact-table">
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
                          <th>已走过节点</th>
                          <td>{{ summarizeNodeLabels(detail.visitedNodeLabels) }}</td>
                        </tr>
                        <tr>
                          <th>当前节点</th>
                          <td>{{ summarizeNodeLabels(detail.currentNodeLabels) }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </article>

                  <article class="panel-card">
                    <div class="panel-head compact-panel-head">
                      <div>
                        <p class="eyebrow">Current Tasks</p>
                        <h3>当前任务</h3>
                      </div>
                    </div>
                    <table class="simple-table monitoring-table compact-table">
                      <thead>
                        <tr>
                          <th>任务名</th>
                          <th>处理人</th>
                          <th>创建时间</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="task in detail.currentTasks || []" :key="task.id">
                          <td>
                            {{ task.name }}
                            <p class="table-subtext">{{ [task.pageLabel, task.formLabel].filter(Boolean).join(' / ') || '-' }}</p>
                          </td>
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
                <div class="panel-head compact-panel-head">
                  <div>
                    <p class="eyebrow">Variables</p>
                    <h3>流程变量</h3>
                  </div>
                </div>
                <table class="simple-table monitoring-table">
                  <thead>
                    <tr>
                      <th>变量名</th>
                      <th>变量值</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in detailVariables" :key="item.key" :class="{ 'trace-highlight-row': isTraceVariableHighlighted(item.key) }">
                      <td class="table-code">{{ item.key }}</td>
                      <td>{{ item.value }}</td>
                    </tr>
                    <tr v-if="!detailVariables.length">
                      <td colspan="2" class="empty-row">没有可展示的流程变量</td>
                    </tr>
                  </tbody>
                </table>
              </article>

              <article v-else-if="detailTab === 'trajectory'" class="panel-card">
                <div class="panel-head compact-panel-head">
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
                  :sequence-flow-ids="detail.highlightedSequenceFlowIds || []"
                  @node-selected="handleTraceNodeSelected"
                />
                <div v-else class="empty-state">当前实例缺少可展示的 BPMN 定义，无法渲染高亮轨迹图。</div>
                <section class="content-grid two-column">
                  <article class="panel-card">
                    <div class="panel-head compact-panel-head">
                      <div>
                        <p class="eyebrow">Node Focus</p>
                        <h3>节点联动摘要</h3>
                      </div>
                      <span class="muted-text">{{ selectedTraceNodeContext?.id || '点击图中节点' }}</span>
                    </div>
                    <div v-if="selectedTraceNodeContext" class="detail-grid trace-focus-grid">
                      <article class="detail-item">
                        <span class="detail-key">节点名称</span>
                        <strong class="detail-value">{{ selectedTraceNodeContext.name }}</strong>
                      </article>
                      <article class="detail-item">
                        <span class="detail-key">节点类型</span>
                        <strong class="detail-value">{{ selectedTraceNodeContext.type || '-' }}</strong>
                      </article>
                      <article class="detail-item">
                        <span class="detail-key">开始时间</span>
                        <strong class="detail-value">{{ formatDate(selectedTraceNodeContext.startedAt) }}</strong>
                      </article>
                      <article class="detail-item">
                        <span class="detail-key">状态</span>
                        <strong class="detail-value">{{ selectedTraceNodeContext.status || '-' }}</strong>
                      </article>
                      <article class="detail-item">
                        <span class="detail-key">业务页标签</span>
                        <strong class="detail-value">{{ selectedTraceNodeContext.pageLabel || '-' }}</strong>
                      </article>
                      <article class="detail-item">
                        <span class="detail-key">表单标签</span>
                        <strong class="detail-value">{{ selectedTraceNodeContext.formLabel || '-' }}</strong>
                      </article>
                      <article class="detail-item">
                        <span class="detail-key">formKey</span>
                        <strong class="detail-value">{{ selectedTraceNodeContext.formKey || '-' }}</strong>
                      </article>
                      <article class="detail-item">
                        <span class="detail-key">分配表达式</span>
                        <strong class="detail-value">{{ selectedTraceNodeContext.assignmentExpression || '-' }}</strong>
                      </article>
                    </div>
                    <div v-else class="empty-state">点击 BPMN 图中的节点后，这里会联动展示评论、变量变更和表单快照。</div>
                    <div class="inline-actions">
                      <button type="button" class="secondary-btn mini-btn" @click="focusTraceVariables" :disabled="!selectedTraceNodeContext">
                        定位变量
                      </button>
                      <button type="button" class="secondary-btn mini-btn" @click="focusTraceHistory" :disabled="!selectedTraceNodeContext">
                        定位历史
                      </button>
                    </div>
                  </article>

                  <article class="panel-card">
                    <div class="panel-head compact-panel-head">
                      <div>
                        <p class="eyebrow">Node Inspector</p>
                        <h3>节点评论 / 变量 / 快照</h3>
                      </div>
                    </div>
                    <div class="trace-side-stack">
                      <section class="trace-side-group">
                        <div class="trace-side-head">
                          <strong>表单快照</strong>
                          <span class="muted-text">{{ selectedTraceSnapshotFields.length }} 项</span>
                        </div>
                        <div v-if="selectedTraceSnapshotSchema.length" class="trace-schema-shell">
                          <SchemaDrivenTaskForm
                            :schema="selectedTraceSnapshotSchema"
                            :form-data="selectedTraceSnapshotData"
                            readonly
                          />
                        </div>
                        <div v-else class="stack-list compact-stack">
                          <div v-for="item in selectedTraceVariables" :key="item.key" class="list-item trace-mini-card">
                            <strong class="table-code">{{ item.label || item.key }}</strong>
                            <span>{{ item.value }}</span>
                          </div>
                          <div v-if="!selectedTraceVariables.length" class="empty-state">当前节点没有可展示的表单快照</div>
                        </div>
                      </section>

                      <section class="trace-side-group">
                        <div class="trace-side-head">
                          <strong>变量变更</strong>
                          <span class="muted-text">{{ selectedTraceVariableUpdates.length }} 条</span>
                        </div>
                        <div class="stack-list compact-stack">
                          <div v-for="item in selectedTraceVariableUpdates" :key="item.id || `${item.variableName}-${item.time}`" class="list-item trace-mini-card">
                            <div>
                              <strong class="table-code">{{ item.variableName }}</strong>
                              <p>{{ item.valueText }}</p>
                            </div>
                            <span class="status-pill muted-pill">{{ formatDate(item.time, true) }}</span>
                          </div>
                          <div v-if="!selectedTraceVariableUpdates.length" class="empty-state">当前节点没有变量变更记录</div>
                        </div>
                      </section>

                      <section class="trace-side-group">
                        <div class="trace-side-head">
                          <strong>审批评论</strong>
                          <span class="muted-text">{{ selectedTraceComments.length }} 条</span>
                        </div>
                        <div class="stack-list compact-stack">
                          <div v-for="item in selectedTraceComments" :key="item.id || `${item.time}-${item.message}`" class="list-item trace-mini-card">
                            <div>
                              <strong>{{ item.userId || '系统' }}</strong>
                              <p>{{ item.message || '-' }}</p>
                            </div>
                            <span class="status-pill muted-pill">{{ formatDate(item.time, true) }}</span>
                          </div>
                          <div v-if="!selectedTraceComments.length" class="empty-state">当前节点没有审批评论</div>
                        </div>
                      </section>

                      <section class="trace-side-group">
                        <div class="trace-side-head">
                          <strong>执行记录</strong>
                          <span class="muted-text">{{ selectedTraceHistory.length + selectedTraceCurrentTasks.length }} 条</span>
                        </div>
                        <div class="stack-list compact-stack">
                          <div v-for="item in selectedTraceCurrentTasks" :key="`active-${item.id}`" class="list-item trace-mini-card active-trace-card">
                            <div>
                              <strong>{{ item.name }}</strong>
                              <p>{{ item.assignee || '未分配' }} · {{ formatDate(item.createTime) }} · {{ [item.pageLabel, item.formLabel].filter(Boolean).join(' / ') || '-' }}</p>
                            </div>
                            <span class="status-pill success-pill">活动中</span>
                          </div>
                          <div v-for="item in selectedTraceHistory" :key="`history-${item.id}`" class="list-item trace-mini-card">
                            <div>
                              <strong>{{ item.name }}</strong>
                              <p>{{ item.assignee || '-' }} · {{ formatDate(item.createTime) }} · {{ [item.pageLabel, item.formLabel].filter(Boolean).join(' / ') || '-' }}</p>
                            </div>
                            <span class="status-pill muted-pill">{{ formatDuration(item.duration) }}</span>
                          </div>
                          <div v-if="!selectedTraceCurrentTasks.length && !selectedTraceHistory.length" class="empty-state">当前节点暂无关联执行记录</div>
                        </div>
                      </section>
                    </div>
                  </article>
                </section>
              </article>

              <article v-else-if="detailTab === 'comments'" class="panel-card">
                <div class="panel-head compact-panel-head">
                  <div>
                    <p class="eyebrow">Approval Comments</p>
                    <h3>审批评论</h3>
                  </div>
                </div>
                <div class="stack-list">
                  <div v-for="item in detail.comments || []" :key="item.id || `${item.time}-${item.message}`" class="list-item rich-list-item">
                    <div>
                      <strong>{{ item.userId || '系统' }}</strong>
                      <p>{{ item.message || '-' }}</p>
                    </div>
                    <span class="status-pill">{{ formatDate(item.time) }}</span>
                  </div>
                  <div v-if="!(detail.comments || []).length" class="empty-state">当前实例还没有审批评论。</div>
                </div>
              </article>

              <article v-else class="panel-card">
                <div class="panel-head compact-panel-head">
                  <div>
                    <p class="eyebrow">Execution History</p>
                    <h3>执行历史</h3>
                  </div>
                </div>
                <table class="simple-table monitoring-table">
                  <thead>
                    <tr>
                      <th>任务</th>
                      <th>处理人</th>
                      <th>开始</th>
                      <th>结束</th>
                      <th>耗时</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in detail.history || []" :key="item.id" :class="{ 'trace-highlight-row': isTraceHistoryHighlighted(item) }">
                      <td>
                        {{ item.name }}
                        <p class="table-subtext">{{ [item.pageLabel, item.formLabel].filter(Boolean).join(' / ') || '-' }}</p>
                      </td>
                      <td>{{ item.assignee || '-' }}</td>
                      <td>{{ formatDate(item.createTime) }}</td>
                      <td>{{ formatDate(item.endTime) }}</td>
                      <td>{{ formatDuration(item.duration) }}</td>
                    </tr>
                    <tr v-if="!(detail.history || []).length">
                      <td colspan="5" class="empty-row">暂无执行历史</td>
                    </tr>
                  </tbody>
                </table>
              </article>
            </section>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import BpmnTraceViewer from '../components/BpmnTraceViewer.vue'
import AppPagination from '../components/common/AppPagination.vue'
import SchemaDrivenTaskForm from '../components/task-forms/SchemaDrivenTaskForm.vue'
import { http } from '../api/http'
import { usePagedQuery } from '../composables/usePagedQuery'

const statistics = ref({})
const history = ref([])
const historyLoading = ref(false)
const historyError = ref('')
const pageLoading = ref(false)
const pageError = ref('')
const keyword = ref('')
const assigneeFilter = ref('all')
const historyScope = ref('running')
const selectedHistoryId = ref(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref(null)
const detailTab = ref('overview')
const selectedTraceNode = ref(null)

const runningQuery = usePagedQuery(async ({ page, size, keyword }) => {
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
  size: 6
})

const completedQuery = usePagedQuery(async ({ page, size, keyword }) => {
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
  size: 6
})

const taskQuery = usePagedQuery(async ({ page, size, keyword, assigneeFilter }) => {
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
  size: 6
})

const filteredRunningProcesses = computed(() => runningQuery.items.value)
const filteredCompletedProcesses = computed(() => completedQuery.items.value)
const filteredTasks = computed(() => taskQuery.items.value)

const runningLoading = computed(() => runningQuery.loading.value)
const runningError = computed(() => runningQuery.error.value)
const runningPage = computed(() => runningQuery.page.value)
const runningSize = computed(() => runningQuery.size.value)
const runningTotal = computed(() => runningQuery.total.value)

const completedLoading = computed(() => completedQuery.loading.value)
const completedError = computed(() => completedQuery.error.value)
const completedPage = computed(() => completedQuery.page.value)
const completedSize = computed(() => completedQuery.size.value)
const completedTotal = computed(() => completedQuery.total.value)

const taskLoading = computed(() => taskQuery.loading.value)
const taskError = computed(() => taskQuery.error.value)
const taskPage = computed(() => taskQuery.page.value)
const taskSize = computed(() => taskQuery.size.value)
const taskTotal = computed(() => taskQuery.total.value)
const totalMatchedCount = computed(() => runningTotal.value + taskTotal.value + completedTotal.value)
const keywordSummary = computed(() => keyword.value ? `关键词 ${keyword.value}` : '关键词 全部')
const assigneeFilterLabel = computed(() => assigneeOptions.find((item) => item.value === assigneeFilter.value)?.label || '全部')
const historyScopeLabel = computed(() => historyScopeOptions.find((item) => item.value === historyScope.value)?.label || '运行中')

const assigneeOptions = [
  { value: 'all', label: '全部' },
  { value: 'assigned', label: '已分配' },
  { value: 'unassigned', label: '未分配' }
]

const historyScopeOptions = [
  { value: 'running', label: '运行中' },
  { value: 'completed', label: '已完成' }
]

const detailTabs = [
  { value: 'overview', label: '概览' },
  { value: 'variables', label: '变量' },
  { value: 'trajectory', label: '轨迹' },
  { value: 'comments', label: '评论' },
  { value: 'history', label: '历史' }
]

function formatDate(value, short = false) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  return short ? date.toLocaleString().slice(0, 16) : date.toLocaleString()
}

function formatDuration(value) {
  const duration = Number(value)
  if (!duration) {
    return '-'
  }
  if (duration < 1000) {
    return `${duration} ms`
  }
  if (duration < 60000) {
    return `${(duration / 1000).toFixed(1)} s`
  }
  const minutes = Math.floor(duration / 60000)
  const seconds = Math.round((duration % 60000) / 1000)
  return `${minutes}m ${seconds}s`
}

function shorten(value, limit = 14) {
  if (!value) {
    return '-'
  }
  return value.length > limit ? `${value.slice(0, limit)}...` : value
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

function summarizeNodeLabels(items) {
  if (!Array.isArray(items) || !items.length) {
    return '-'
  }
  return items
    .map((item) => typeof item === 'string' ? item : item?.summary || item?.nodeName || '')
    .filter(Boolean)
    .join(' | ')
}

function formatNodeFieldValue(value) {
  if (value == null || value === '') {
    return '-'
  }
  if (typeof value === 'boolean') {
    return value ? '是' : '否'
  }
  return stringifyValue(value)
}

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

const selectedTraceNodeContext = computed(() => {
  if (!selectedTraceNode.value) {
    return null
  }
  const byActivity = (detail.value?.trajectoryNodes || []).find((item) => item.activityId === selectedTraceNode.value.id)
  const nodeDetail = detail.value?.nodeDetails?.[selectedTraceNode.value.id] || {}
  return {
    id: selectedTraceNode.value.id,
    name: selectedTraceNode.value.name || byActivity?.activityName || selectedTraceNode.value.id,
    type: selectedTraceNode.value.type || byActivity?.activityType || nodeDetail.nodeType || '',
    pageLabel: byActivity?.pageLabel || nodeDetail.pageLabel || '',
    formLabel: byActivity?.formLabel || nodeDetail.formLabel || '',
    formKey: byActivity?.formKey || nodeDetail.formKey || '',
    assignmentExpression: byActivity?.assignmentExpression || nodeDetail.assignmentExpression || '',
    startedAt: byActivity?.startTime || null,
    endedAt: byActivity?.endTime || null,
    status: byActivity?.status || (detail.value?.currentActivityId === selectedTraceNode.value.id ? 'ACTIVE' : 'COMPLETED')
  }
})

const selectedTraceNodeDetail = computed(() => {
  if (!selectedTraceNodeContext.value?.id) {
    return null
  }
  return detail.value?.nodeDetails?.[selectedTraceNodeContext.value.id] || null
})

const selectedTraceVariableKeys = computed(() => {
  const keys = new Set()
  for (const item of selectedTraceNodeDetail.value?.formSnapshot?.fields || []) {
    if (item?.key) {
      keys.add(item.key)
    }
  }
  for (const item of selectedTraceNodeDetail.value?.variableUpdates || []) {
    if (item?.variableName) {
      keys.add(item.variableName)
    }
  }
  return [...keys]
})

const selectedTraceSnapshotSchema = computed(() =>
  Array.isArray(selectedTraceNodeDetail.value?.formSnapshot?.schema)
    ? selectedTraceNodeDetail.value.formSnapshot.schema
    : []
)

const selectedTraceSnapshotData = computed(() => {
  if (selectedTraceNodeDetail.value?.formSnapshot?.values && typeof selectedTraceNodeDetail.value.formSnapshot.values === 'object') {
    return selectedTraceNodeDetail.value.formSnapshot.values
  }
  return Object.fromEntries(
    (selectedTraceNodeDetail.value?.formSnapshot?.fields || []).map((item) => [item.key, item.value])
  )
})

const selectedTraceSnapshotFields = computed(() => {
  const fields = selectedTraceNodeDetail.value?.formSnapshot?.fields
  return Array.isArray(fields) ? fields : []
})

const selectedTraceVariables = computed(() => {
  const snapshotFields = selectedTraceSnapshotFields.value.map((item) => ({
    key: item.key,
    value: formatNodeFieldValue(item.value),
    label: item.label || item.key
  }))
  if (snapshotFields.length) {
    return snapshotFields
  }
  if (!selectedTraceNodeContext.value) {
    return detailVariables.value.slice(0, 6)
  }
  const matches = detailVariables.value.filter((item) => selectedTraceVariableKeys.value.includes(item.key))
  return matches.length ? matches : detailVariables.value.slice(0, 6)
})

const selectedTraceHistory = computed(() => {
  if (selectedTraceNodeDetail.value?.history) {
    return selectedTraceNodeDetail.value.history
  }
  if (!selectedTraceNodeContext.value) {
    return []
  }
  return (detail.value?.history || []).filter((item) => item.taskDefinitionKey === selectedTraceNodeContext.value.id)
})

const selectedTraceCurrentTasks = computed(() => {
  if (selectedTraceNodeDetail.value?.currentTasks) {
    return selectedTraceNodeDetail.value.currentTasks
  }
  if (!selectedTraceNodeContext.value) {
    return []
  }
  return (detail.value?.currentTasks || []).filter((item) => item.taskDefinitionKey === selectedTraceNodeContext.value.id)
})

const selectedTraceVariableUpdates = computed(() =>
  (selectedTraceNodeDetail.value?.variableUpdates || []).map((item) => ({
    ...item,
    valueText: formatNodeFieldValue(item.value)
  }))
)

const selectedTraceComments = computed(() => selectedTraceNodeDetail.value?.comments || [])

const detailStateLabel = computed(() => {
  if (!detail.value) {
    return '-'
  }
  return detail.value.active ? '运行中' : '已完成'
})

const detailStateClass = computed(() => (detail.value?.active ? 'success-pill' : 'muted-pill'))

function resetTraceSelection() {
  selectedTraceNode.value = null
}

function initializeTraceSelection() {
  const preferredId = detail.value?.currentActivityId
    || detail.value?.activeActivityIds?.[0]
    || detail.value?.completedActivityIds?.[0]
    || ''
  if (!preferredId) {
    resetTraceSelection()
    return
  }
  const activity = (detail.value?.trajectoryNodes || []).find((item) => item.activityId === preferredId)
  selectedTraceNode.value = {
    id: preferredId,
    name: activity?.activityName || preferredId,
    type: activity?.activityType || ''
  }
}

function handleTraceNodeSelected(node) {
  selectedTraceNode.value = node
}

function focusTraceVariables() {
  detailTab.value = 'variables'
}

function focusTraceHistory() {
  detailTab.value = 'history'
}

function isTraceVariableHighlighted(key) {
  return Boolean(selectedTraceNodeContext.value) && selectedTraceVariableKeys.value.includes(key)
}

function isTraceHistoryHighlighted(item) {
  return Boolean(selectedTraceNodeContext.value) && item.taskDefinitionKey === selectedTraceNodeContext.value.id
}

function closeDetail() {
  detailVisible.value = false
  detailLoading.value = false
  detailError.value = ''
  detail.value = null
  detailTab.value = 'overview'
  resetTraceSelection()
}

function currentHistorySource() {
  const sourceItems = historyScope.value === 'completed'
    ? filteredCompletedProcesses.value
    : filteredRunningProcesses.value
  if (!sourceItems.length) {
    return null
  }
  return sourceItems.find((item) => item.id === selectedHistoryId.value) || sourceItems[0]
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
    historyError.value = error.normalizedMessage || error.message || '加载流程历史失败'
  } finally {
    historyLoading.value = false
  }
}

async function syncHistorySelection() {
  const source = currentHistorySource()
  if (source?.id) {
    await loadHistory(source.id)
    return
  }
  selectedHistoryId.value = null
  history.value = []
  historyError.value = ''
}

async function applyFilters() {
  pageLoading.value = true
  pageError.value = ''
  try {
    const normalizedKeyword = keyword.value.trim() || undefined
    const [statisticsRes] = await Promise.all([
      http.get('/monitoring/statistics'),
      runningQuery.search({ keyword: normalizedKeyword }),
      completedQuery.search({ keyword: normalizedKeyword }),
      taskQuery.search({
        keyword: normalizedKeyword,
        assigneeFilter: assigneeFilter.value
      })
    ])
    statistics.value = statisticsRes.data || {}
    await syncHistorySelection()
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '加载监控数据失败'
  } finally {
    pageLoading.value = false
  }
}

async function refreshAll() {
  pageLoading.value = true
  pageError.value = ''
  try {
    const [statisticsRes] = await Promise.all([
      http.get('/monitoring/statistics'),
      runningQuery.load(),
      completedQuery.load(),
      taskQuery.load()
    ])
    statistics.value = statisticsRes.data || {}
    await syncHistorySelection()
  } catch (error) {
    pageError.value = error.normalizedMessage || error.message || '刷新监控数据失败'
  } finally {
    pageLoading.value = false
  }
}

async function resetFilters() {
  keyword.value = ''
  assigneeFilter.value = 'all'
  historyScope.value = 'running'
  await applyFilters()
}

async function changeRunningPage(nextPage) {
  try {
    await runningQuery.changePage(nextPage)
    if (historyScope.value === 'running') {
      await syncHistorySelection()
    }
  } catch (error) {
    pageError.value = error.normalizedMessage
  }
}

async function changeRunningSize(nextSize) {
  try {
    await runningQuery.changeSize(nextSize)
    if (historyScope.value === 'running') {
      await syncHistorySelection()
    }
  } catch (error) {
    pageError.value = error.normalizedMessage
  }
}

async function changeCompletedPage(nextPage) {
  try {
    await completedQuery.changePage(nextPage)
    if (historyScope.value === 'completed') {
      await syncHistorySelection()
    }
  } catch (error) {
    pageError.value = error.normalizedMessage
  }
}

async function changeCompletedSize(nextSize) {
  try {
    await completedQuery.changeSize(nextSize)
    if (historyScope.value === 'completed') {
      await syncHistorySelection()
    }
  } catch (error) {
    pageError.value = error.normalizedMessage
  }
}

async function changeTaskPage(nextPage) {
  try {
    await taskQuery.changePage(nextPage)
  } catch (error) {
    pageError.value = error.normalizedMessage
  }
}

async function changeTaskSize(nextSize) {
  try {
    await taskQuery.changeSize(nextSize)
  } catch (error) {
    pageError.value = error.normalizedMessage
  }
}

async function openDetail(processInstanceId) {
  detailVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = null
  detailTab.value = 'overview'
  try {
    const { data } = await http.get(`/monitoring/processes/${processInstanceId}/detail`)
    detail.value = data || null
    initializeTraceSelection()
  } catch (error) {
    detailError.value = error.normalizedMessage
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  await applyFilters()
})

watch(historyScope, async () => {
  await syncHistorySelection()
})
</script>

<style scoped>
.monitoring-shell {
  gap: 16px;
}

.monitoring-toolbar-compact {
  align-items: flex-end;
}

.monitoring-filters-compact {
  flex: 1;
  min-width: 0;
}

.monitoring-search-field {
  min-width: 0;
}

.monitoring-compact-field {
  max-width: 180px;
}

.monitor-filter-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.compact-panel-head {
  align-items: flex-start;
}

.monitoring-table th,
.monitoring-table td {
  padding: 10px 8px;
}

.table-strong,
.table-subtext {
  margin: 0;
}

.table-strong {
  display: block;
}

.table-subtext,
.table-code {
  color: #7b6549;
  font-size: 12px;
}

.table-code {
  font-family: "SFMono-Regular", "Menlo", monospace;
}

.monitoring-lower-grid {
  align-items: start;
}

.history-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.history-chip {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(89, 67, 43, 0.12);
  background: #fffdf8;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-chip span {
  color: #8a7353;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.history-chip strong {
  font-size: 1.05rem;
}

.detail-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.detail-sidebar,
.detail-main,
.modal-content-stack {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-hero-card,
.detail-sidebar-card {
  border: 1px solid rgba(89, 67, 43, 0.12);
  border-radius: 22px;
  background: #fffdf8;
}

.detail-hero-card {
  padding: 18px;
  background:
    radial-gradient(circle at top left, rgba(183, 119, 39, 0.12), transparent 35%),
    linear-gradient(180deg, rgba(255, 250, 241, 0.98), rgba(250, 244, 233, 0.98));
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-hero-card strong,
.detail-hero-card p {
  margin: 0;
}

.detail-hero-label {
  color: #8a7353;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

.detail-hero-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #6f5c45;
  font-size: 12px;
}

.detail-sidebar-card {
  padding: 16px;
}

.detail-tab-stack {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-tab-stack .tab-btn {
  width: 100%;
  justify-content: space-between;
}

.detail-tab-stack small {
  color: inherit;
  opacity: 0.72;
}

.detail-summary-list {
  display: grid;
  gap: 10px;
}

.detail-summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(89, 67, 43, 0.08);
}

.detail-summary-item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.detail-summary-item span {
  color: #7d694e;
  font-size: 13px;
}

.compact-table th,
.compact-table td {
  padding-top: 9px;
  padding-bottom: 9px;
}

.trace-focus-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.trace-side-stack,
.trace-side-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trace-side-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.compact-stack {
  gap: 10px;
}

.trace-schema-shell {
  padding: 14px;
  border: 1px solid rgba(121, 101, 74, 0.14);
  border-radius: 18px;
  background: rgba(255, 252, 246, 0.72);
}

.trace-mini-card {
  align-items: flex-start;
  padding: 12px 14px;
}

.trace-mini-card p,
.trace-mini-card strong,
.trace-mini-card span {
  margin: 0;
}

.active-trace-card {
  border-color: rgba(47, 125, 50, 0.22);
}

.trace-highlight-row {
  background: rgba(183, 119, 39, 0.08);
}

@media (max-width: 1280px) {
  .monitoring-toolbar-compact {
    align-items: stretch;
  }
}

@media (max-width: 1100px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .detail-sidebar {
    order: 2;
  }
}

@media (max-width: 720px) {
  .history-summary {
    grid-template-columns: 1fr;
  }

  .monitoring-filters-compact {
    width: 100%;
  }

  .monitoring-compact-field {
    max-width: none;
  }
}
</style>
