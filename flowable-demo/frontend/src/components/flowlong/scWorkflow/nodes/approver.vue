<template>
  <div class="node-wrap">
    <div class="node-wrap-box" @click="show">
      <div class="title approver-title">
        <el-icon class="icon"><UserFilled /></el-icon>
        <span>{{ nodeConfig.nodeName }}</span>
        <el-icon class="close" @click.stop="delNode"><Close /></el-icon>
      </div>
      <div class="content">
        <span v-if="summaryText">{{ summaryText }}</span>
        <span v-else class="placeholder">请选择审批人表达式或候选角色</span>
      </div>
    </div>
    <add-node v-model="nodeConfig.childNode"></add-node>
    <el-drawer v-model="drawer" title="审批节点设置" destroy-on-close append-to-body :size="560">
      <template #header>
        <div class="node-wrap-drawer__title">
          <label v-if="!isEditTitle" @click="editTitle">
            {{ form.nodeName }}
            <el-icon class="node-wrap-drawer__title-edit"><Edit /></el-icon>
          </label>
          <el-input
            v-else
            ref="nodeTitle"
            v-model="form.nodeName"
            clearable
            @blur="saveTitle"
            @keyup.enter="saveTitle"
          />
        </div>
      </template>
      <el-container>
        <el-main style="padding: 0 20px 20px 20px">
          <el-form label-position="top">
            <el-form-item label="审批模式">
              <el-select v-model="form.properties.assignmentMode" :disabled="isReadonly" @change="handleAssignmentModeChange">
                <el-option label="单一办理人" value="assignee" />
                <el-option label="候选用户" value="candidateUsers" />
                <el-option label="候选角色/组" value="candidateGroups" />
              </el-select>
            </el-form-item>

            <el-form-item v-if="form.properties.assignmentMode === 'assignee'" label="办理人表达式">
              <el-input v-model="form.properties.assignee" :disabled="isReadonly" placeholder="${deptManager}" />
            </el-form-item>
            <el-form-item v-if="form.properties.assignmentMode === 'assignee'" label="选择办理人">
              <el-button type="primary" round :disabled="isReadonly" @click="selectHandle(1, form.properties.assigneeList)">选择人员</el-button>
              <div v-if="form.properties.assigneeList.length" class="tags-list">
                <el-tag
                  v-for="(user, index) in form.properties.assigneeList"
                  :key="user.id || `${user.name}-${index}`"
                  :closable="!isReadonly"
                  @close="removeSelected('assigneeList', index)"
                >
                  {{ user.name || user.id }}
                </el-tag>
              </div>
            </el-form-item>

            <el-form-item v-if="form.properties.assignmentMode === 'candidateUsers'" label="候选用户">
              <el-input
                v-model="form.properties.candidateUsers"
                type="textarea"
                :rows="3"
                :disabled="isReadonly"
                placeholder="userA,userB 或 ${candidateUsers}"
              />
            </el-form-item>
            <el-form-item v-if="form.properties.assignmentMode === 'candidateUsers'" label="选择候选用户">
              <el-button type="primary" round :disabled="isReadonly" @click="selectHandle(1, form.properties.candidateUserList)">选择人员</el-button>
              <div v-if="form.properties.candidateUserList.length" class="tags-list">
                <el-tag
                  v-for="(user, index) in form.properties.candidateUserList"
                  :key="user.id || `${user.name}-${index}`"
                  :closable="!isReadonly"
                  @close="removeSelected('candidateUserList', index)"
                >
                  {{ user.name || user.id }}
                </el-tag>
              </div>
            </el-form-item>

            <el-form-item v-if="form.properties.assignmentMode === 'candidateGroups'" label="候选角色/组">
              <el-input
                v-model="form.properties.candidateGroups"
                type="textarea"
                :rows="3"
                :disabled="isReadonly"
                placeholder="hr,finance 或 ${candidateGroups}"
              />
            </el-form-item>
            <el-form-item v-if="form.properties.assignmentMode === 'candidateGroups'" label="选择候选角色">
              <el-button type="primary" round :disabled="isReadonly" @click="selectHandle(2, form.properties.candidateGroupList)">选择角色</el-button>
              <div v-if="form.properties.candidateGroupList.length" class="tags-list">
                <el-tag
                  v-for="(role, index) in form.properties.candidateGroupList"
                  :key="role.id || `${role.name}-${index}`"
                  :closable="!isReadonly"
                  @close="removeSelected('candidateGroupList', index)"
                >
                  {{ role.name || role.id }}
                </el-tag>
              </div>
            </el-form-item>

            <el-form-item label="审批表单">
              <el-select v-model="form.properties.formKey" :disabled="isReadonly" placeholder="请选择审批表单" filterable @change="syncFormCatalog">
                <el-option label="不绑定表单" value="" />
                <el-option
                  v-for="item in taskCatalogs"
                  :key="item.id || item.formKey"
                  :label="`${item.formName} · ${item.formKey}`"
                  :value="item.formKey"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="业务页面">
              <el-input v-model="form.properties.pageLabel" :disabled="isReadonly" placeholder="待办审批页" />
            </el-form-item>

            <el-form-item label="表单名称">
              <el-input v-model="form.properties.formLabel" :disabled="isReadonly" placeholder="部门经理审批单" />
            </el-form-item>

            <el-form-item label="任务监听器">
              <el-input
                v-model="listenersText"
                type="textarea"
                :rows="4"
                :disabled="isReadonly"
                placeholder="一行一个，格式：create=com.demo.TaskCreateListener"
              />
            </el-form-item>
          </el-form>
        </el-main>
        <el-footer>
          <el-button v-if="!isReadonly" type="primary" @click="save">保存</el-button>
          <el-button @click="drawer = false">取消</el-button>
        </el-footer>
      </el-container>
    </el-drawer>
  </div>
</template>

<script>
import { summarizeFlowlongNode } from '../../../../utils/flowlongAdapter'
import addNode from './addNode.vue'

export default {
  inject: ['flowlongFormCatalogs', 'select', 'flowlongReadonly'],
  props: {
    modelValue: { type: Object, default: () => ({}) }
  },
  components: {
    addNode
  },
  data() {
    return {
      nodeConfig: {},
      drawer: false,
      isEditTitle: false,
      form: {
        properties: {}
      },
      listenersText: ''
    }
  },
  computed: {
    isReadonly() {
      return Boolean(this.flowlongReadonly?.value ?? this.flowlongReadonly)
    },
    taskCatalogs() {
      const catalogs = this.flowlongFormCatalogs?.value || this.flowlongFormCatalogs || []
      return catalogs.filter((item) => String(item.scope || '').toUpperCase() !== 'START')
    },
    summaryText() {
      return summarizeFlowlongNode(this.nodeConfig)
    }
  },
  watch: {
    modelValue() {
      this.nodeConfig = this.modelValue
    }
  },
  mounted() {
    this.nodeConfig = this.modelValue
  },
  methods: {
    show() {
      this.form = JSON.parse(JSON.stringify(this.nodeConfig))
      this.form.properties = this.form.properties || {}
      this.form.properties.assigneeList = Array.isArray(this.form.properties.assigneeList) ? this.form.properties.assigneeList : []
      this.form.properties.candidateUserList = Array.isArray(this.form.properties.candidateUserList) ? this.form.properties.candidateUserList : []
      this.form.properties.candidateGroupList = Array.isArray(this.form.properties.candidateGroupList) ? this.form.properties.candidateGroupList : []
      this.listenersText = (this.form.properties.listeners || [])
        .map((item) => `${item.listenerType}=${item.listenerPath}`)
        .join('\n')
      this.isEditTitle = false
      this.drawer = true
    },
    editTitle() {
      this.isEditTitle = true
      this.$nextTick(() => {
        this.$refs.nodeTitle.focus()
      })
    },
    saveTitle() {
      this.isEditTitle = false
    },
    delNode() {
      if (this.isReadonly) {
        return
      }
      this.$emit('update:modelValue', this.nodeConfig.childNode)
    },
    syncFormCatalog() {
      const current = this.taskCatalogs.find((item) => item.formKey === this.form.properties.formKey)
      if (!current) {
        return
      }
      this.form.properties.formLabel = current.formName || this.form.properties.formLabel || ''
      this.form.properties.pageLabel = current.pageLabel || this.form.properties.pageLabel || ''
    },
    handleAssignmentModeChange() {
      if (this.form.properties.assignmentMode === 'assignee') {
        this.form.properties.candidateUsers = ''
        this.form.properties.candidateGroups = ''
      } else if (this.form.properties.assignmentMode === 'candidateUsers') {
        this.form.properties.assignee = ''
        this.form.properties.candidateGroups = ''
      } else if (this.form.properties.assignmentMode === 'candidateGroups') {
        this.form.properties.assignee = ''
        this.form.properties.candidateUsers = ''
      }
    },
    selectHandle(type, data) {
      if (this.isReadonly) {
        return
      }
      this.select(type, data)
    },
    removeSelected(field, index) {
      if (this.isReadonly) {
        return
      }
      this.form.properties[field].splice(index, 1)
      this.syncAssignmentExpression()
    },
    syncAssignmentExpression() {
      const selectedAssignee = this.form.properties.assigneeList?.[0]
      if (this.form.properties.assignmentMode === 'assignee') {
        this.form.properties.assignee = selectedAssignee?.id || this.form.properties.assignee
      } else if (this.form.properties.assignmentMode === 'candidateUsers') {
        const ids = (this.form.properties.candidateUserList || []).map((item) => item.id || item.name).filter(Boolean)
        this.form.properties.candidateUsers = ids.join(',')
      } else if (this.form.properties.assignmentMode === 'candidateGroups') {
        const ids = (this.form.properties.candidateGroupList || []).map((item) => item.id || item.name).filter(Boolean)
        this.form.properties.candidateGroups = ids.join(',')
      }
    },
    save() {
      if (this.isReadonly) {
        this.drawer = false
        return
      }
      this.syncAssignmentExpression()
      this.form.properties.listeners = this.listenersText
        .split('\n')
        .map((line) => line.trim())
        .filter(Boolean)
        .map((line) => {
          const [listenerType, ...rest] = line.split('=')
          return {
            listenerType: (listenerType || '').trim(),
            listenerPath: rest.join('=').trim()
          }
        })
        .filter((item) => item.listenerType && item.listenerPath)
      this.$emit('update:modelValue', this.form)
      this.drawer = false
    }
  }
}
</script>

<style scoped>
.approver-title {
  background: linear-gradient(135deg, #ff9b4f, #f17b1c);
}
</style>
