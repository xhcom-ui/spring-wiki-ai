<template>
  <div class="add-node-btn-box">
    <div class="add-node-btn">
      <el-popover
        placement="right-start"
        :width="292"
        trigger="click"
        :hide-after="0"
        :show-after="0"
        :disabled="isReadonly"
      >
        <template #reference>
          <el-button type="primary" class="add-node-trigger" circle :disabled="isReadonly">
            <el-icon><Plus /></el-icon>
          </el-button>
        </template>
        <div class="add-node-popover-body">
          <button type="button" class="add-node-option" @click="addType(1)">
            <span class="add-node-icon add-node-icon-approver">
              <el-icon><UserFilled /></el-icon>
            </span>
            <span>
              <strong>审批节点</strong>
              <small>配置办理人表达式与审批表单</small>
            </span>
          </button>
          <button type="button" class="add-node-option" @click="addType(2)">
            <span class="add-node-icon add-node-icon-copy">
              <el-icon><Promotion /></el-icon>
            </span>
            <span>
              <strong>抄送节点</strong>
              <small>仅用于抄送通知，不进入审批待办链路</small>
            </span>
          </button>
          <button type="button" class="add-node-option" @click="addType(4)">
            <span class="add-node-icon add-node-icon-branch">
              <el-icon><Share /></el-icon>
            </span>
            <span>
              <strong>条件分支</strong>
              <small>按表达式路由到不同审批链路</small>
            </span>
          </button>
        </div>
      </el-popover>
    </div>
  </div>
</template>

<script>
export default {
  inject: ['flowlongReadonly'],
  props: {
    modelValue: { type: Object, default: () => {} }
  },
  computed: {
    isReadonly() {
      return Boolean(this.flowlongReadonly?.value ?? this.flowlongReadonly)
    }
  },
  methods: {
    getNodeKey(prefix = 'flk') {
      return `${prefix}_${Math.random().toString(36).slice(2, 10)}`
    },
    addType(type) {
      if (this.isReadonly) {
        return
      }
      let node = {}
      if (type === 1) {
        node = {
          nodeName: '审批节点',
          nodeKey: this.getNodeKey('task'),
          type: 1,
          properties: {
            assignmentMode: 'assignee',
            assignee: '',
            candidateUsers: '',
            candidateGroups: '',
            assigneeList: [],
            candidateUserList: [],
            candidateGroupList: [],
            formKey: '',
            formLabel: '',
            pageLabel: '',
            listeners: []
          },
          childNode: this.modelValue
        }
      } else if (type === 2) {
        node = {
          nodeName: '抄送节点',
          nodeKey: this.getNodeKey('copy'),
          type: 2,
          properties: {
            copyUsers: '',
            copyUserList: [],
            allowInitiatorSelect: true
          },
          childNode: this.modelValue
        }
      } else if (type === 4) {
        node = {
          nodeName: '条件分支',
          nodeKey: this.getNodeKey('gateway'),
          type: 4,
          properties: {
            gatewayMode: 'serial'
          },
          conditionNodes: [
            {
              nodeName: '条件 1',
              nodeKey: this.getNodeKey('branch'),
              type: 3,
              properties: {
                conditionSummary: '条件 1',
                conditionExpression: '',
                isDefault: false
              }
            },
            {
              nodeName: '默认条件',
              nodeKey: this.getNodeKey('branch'),
              type: 3,
              properties: {
                conditionSummary: '默认分支',
                conditionExpression: '',
                isDefault: true
              }
            }
          ],
          childNode: this.modelValue
        }
      }
      this.$emit('update:modelValue', node)
    }
  }
}
</script>

<style scoped>
.add-node-trigger {
  border: 1px solid rgba(80, 112, 255, 0.2);
  background: linear-gradient(135deg, #eef3ff, #ffffff);
  color: #2850d8;
  box-shadow: 0 10px 24px rgba(40, 80, 216, 0.16);
}

.add-node-popover-body {
  display: grid;
  gap: 10px;
}

.add-node-option {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 12px;
  align-items: center;
  padding: 14px 12px;
  border: 1px solid rgba(132, 148, 176, 0.16);
  border-radius: 16px;
  background: rgba(248, 250, 255, 0.9);
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.add-node-option:hover {
  transform: translateY(-1px);
  border-color: rgba(80, 112, 255, 0.26);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.add-node-option strong,
.add-node-option small {
  display: block;
}

.add-node-option small {
  margin-top: 4px;
  color: #667085;
}

.add-node-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  font-size: 18px;
}

.add-node-icon-approver {
  background: rgba(255, 148, 62, 0.14);
  color: #e07a22;
}

.add-node-icon-branch {
  background: rgba(21, 188, 131, 0.14);
  color: #0f9b69;
}

.add-node-icon-copy {
  background: rgba(50, 150, 250, 0.14);
  color: #2473d6;
}
</style>
