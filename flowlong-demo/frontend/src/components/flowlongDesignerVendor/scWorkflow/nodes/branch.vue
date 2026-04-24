<template>
  <div class="branch-wrap">
    <div class="branch-box-wrap">
      <div class="branch-box">
        <el-button
          class="add-branch"
          type="success"
          plain
          round
          @click="addTerm">
          添加条件
        </el-button>
        <div
          class="col-box"
          v-for="(item, index) in nodeConfig.conditionNodes"
          :key="index">
          <div class="condition-node">
            <div class="condition-node-box">
              <div
                class="auto-judge"
                @click="show(index)">
                <div
                  class="sort-left"
                  v-if="index != 0"
                  @click.stop="arrTransfer(index, -1)">
                  <el-icon><ArrowLeft /></el-icon>
                </div>
                <div class="title">
                  <span class="node-title">{{ item.nodeName }}</span>
                  <span class="priority-title">优先级{{ item.priorityLevel }}</span>
                  <el-icon
                    class="close"
                    @click.stop="delTerm(index)">
                    <Close />
                  </el-icon>
                </div>
                <div class="content">
                  <span v-if="toText(nodeConfig, index)">{{ toText(nodeConfig, index) }}</span>
                  <span
                    v-else
                    class="placeholder">
                    请设置条件
                  </span>
                </div>
                <div
                  class="sort-right"
                  v-if="index != nodeConfig.conditionNodes.length - 1"
                  @click.stop="arrTransfer(index)">
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
              <add-node v-model="item.childNode"></add-node>
            </div>
          </div>
          <slot
            v-if="item.childNode"
            :node="item"></slot>
          <div
            class="top-left-cover-line"
            v-if="index == 0"></div>
          <div
            class="bottom-left-cover-line"
            v-if="index == 0"></div>
          <div
            class="top-right-cover-line"
            v-if="index == nodeConfig.conditionNodes.length - 1"></div>
          <div
            class="bottom-right-cover-line"
            v-if="index == nodeConfig.conditionNodes.length - 1"></div>
        </div>
      </div>
      <add-node v-model="nodeConfig.childNode"></add-node>
    </div>
    <el-drawer
      title="条件设置"
      v-model="drawer"
      destroy-on-close
      append-to-body
      :size="680"
      class="workflow-node-drawer">
      <template #header>
        <div class="node-wrap-drawer__title workflow-node-drawer__title">
          <div class="workflow-node-drawer__title-main">
            <label
              @click="editTitle"
              v-if="!isEditTitle">
              {{ form.nodeName }}
              <el-icon class="node-wrap-drawer__title-edit"><Edit /></el-icon>
            </label>
            <el-input
              v-if="isEditTitle"
              ref="nodeTitle"
              v-model="form.nodeName"
              clearable
              @blur="saveTitle"
              @keyup.enter="saveTitle"></el-input>
            <span class="workflow-node-drawer__subtitle">Condition Branch</span>
          </div>
        </div>
      </template>
      <el-container class="workflow-node-drawer__layout">
        <el-main class="workflow-node-drawer__main workflow-node-branch-main">
          <div class="workflow-node-drawer__intro">
            <strong>条件分支设置</strong>
            <span>按条件组编排路由命中规则，组内为“且”，组间为“或”。</span>
          </div>
          <div class="top-tips">满足以下条件时进入当前分支</div>
          <template v-for="(conditionGroup, conditionGroupIdx) in form.conditionList">
            <div
              class="or-branch-link-tip"
              v-if="conditionGroupIdx != 0">
              或满足
            </div>
            <div class="condition-group-editor">
              <div class="header">
                <span>条件组 {{ conditionGroupIdx + 1 }}</span>
                <div @click="deleteConditionGroup(conditionGroupIdx)">
                  <el-icon class="branch-delete-icon"><Delete /></el-icon>
                </div>
              </div>

              <div class="main-content">
                <!-- 单个条件 -->
                <div class="condition-content-box cell-box">
                  <div>描述</div>
                  <div>条件字段</div>
                  <div>运算符</div>
                  <div>值</div>
                </div>
                <div
                  class="condition-content"
                  v-for="(condition, idx) in conditionGroup">
                  <div class="condition-relation">
                    <span>{{ idx == 0 ? '当' : '且' }}</span>
                    <div @click="deleteConditionList(conditionGroup, idx)">
                      <el-icon class="branch-delete-icon"><Delete /></el-icon>
                    </div>
                  </div>
                  <div class="condition-content">
                    <div class="condition-content-box">
                      <el-input
                        v-model="condition.label"
                        placeholder="描述" />
                      <el-input
                        v-model="condition.field"
                        placeholder="条件字段" />
                      <el-select
                        v-model="condition.operator"
                        placeholder="Select">
                        <el-option
                          label="等于"
                          value="=="></el-option>
                        <el-option
                          label="不等于"
                          value="!="></el-option>
                        <el-option
                          label="大于"
                          value=">"></el-option>
                        <el-option
                          label="大于等于"
                          value=">="></el-option>
                        <el-option
                          label="小于"
                          value="<"></el-option>
                        <el-option
                          label="小于等于"
                          value="<="></el-option>
                        <el-option
                          label="包含"
                          value="include"></el-option>
                        <el-option
                          label="不包含"
                          value="notinclude"></el-option>
                      </el-select>
                      <el-input
                        v-model="condition.value"
                        placeholder="值" />
                    </div>
                  </div>
                </div>
              </div>
              <div class="sub-content">
                <el-button
                  link
                  type="primary"
                  class="workflow-node-branch-link"
                  @click="addConditionList(conditionGroup)">
                  <el-icon><Plus /></el-icon>
                  添加条件
                </el-button>
              </div>
            </div>
          </template>
          <el-button
            class="workflow-node-branch-add-group"
            type="info"
            text
            bg
            @click="addConditionGroup">
            <el-icon><Plus /></el-icon>
            添加条件组
          </el-button>
        </el-main>
        <el-footer class="workflow-node-drawer-footer">
          <el-button
            type="primary"
            @click="save">
            保存
          </el-button>
          <el-button @click="drawer = false">取消</el-button>
        </el-footer>
      </el-container>
    </el-drawer>
  </div>
</template>

<script>
import addNode from './addNode.vue'
import { ArrowLeft, ArrowRight, Close, Delete, Edit, Plus } from '@element-plus/icons-vue'

export default {
  props: {
    modelValue: { type: Object, default: () => {} }
  },
  components: {
    addNode,
    ArrowLeft,
    ArrowRight,
    Close,
    Delete,
    Edit,
    Plus
  },
  data() {
    return {
      nodeConfig: {},
      drawer: false,
      isEditTitle: false,
      index: 0,
      form: {}
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
    show(index) {
      this.index = index
      this.form = {}
      this.form = JSON.parse(JSON.stringify(this.nodeConfig.conditionNodes[index]))
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
    save() {
      this.nodeConfig.conditionNodes[this.index] = this.form
      this.$emit('update:modelValue', this.nodeConfig)
      this.drawer = false
    },
    addTerm() {
      let len = this.nodeConfig.conditionNodes.length + 1
      this.nodeConfig.conditionNodes.push({
        nodeName: '条件' + len,
        type: 3,
        priorityLevel: len,
        conditionMode: 1,
        conditionList: []
      })
    },
    delTerm(index) {
      this.nodeConfig.conditionNodes.splice(index, 1)
      if (this.nodeConfig.conditionNodes.length == 1) {
        if (this.nodeConfig.childNode) {
          if (this.nodeConfig.conditionNodes[0].childNode) {
            this.reData(this.nodeConfig.conditionNodes[0].childNode, this.nodeConfig.childNode)
          } else {
            this.nodeConfig.conditionNodes[0].childNode = this.nodeConfig.childNode
          }
        }
        this.$emit('update:modelValue', this.nodeConfig.conditionNodes[0].childNode)
      }
    },
    reData(data, addData) {
      if (!data.childNode) {
        data.childNode = addData
      } else {
        this.reData(data.childNode, addData)
      }
    },
    arrTransfer(index, type = 1) {
      this.nodeConfig.conditionNodes[index] = this.nodeConfig.conditionNodes.splice(index + type, 1, this.nodeConfig.conditionNodes[index])[0]
      this.nodeConfig.conditionNodes.map((item, index) => {
        item.priorityLevel = index + 1
      })
      this.$emit('update:modelValue', this.nodeConfig)
    },
    addConditionList(conditionList) {
      conditionList.push({
        label: '',
        field: '',
        operator: '=',
        value: ''
      })
    },
    deleteConditionList(conditionList, index) {
      conditionList.splice(index, 1)
    },
    addConditionGroup() {
      this.addConditionList(this.form.conditionList[this.form.conditionList.push([]) - 1])
    },
    deleteConditionGroup(index) {
      this.form.conditionList.splice(index, 1)
    },
    toText(nodeConfig, index) {
      var { conditionList } = nodeConfig.conditionNodes[index]
      if (conditionList && conditionList.length == 1) {
        const text = conditionList.map((conditionGroup) => conditionGroup.map((item) => `${item.label}${item.operator}${item.value}`)).join(' 和 ')
        return text
      } else if (conditionList && conditionList.length > 1) {
        return conditionList.length + '个条件，或满足'
      } else {
        if (index == nodeConfig.conditionNodes.length - 1) {
          return '其他条件进入此流程'
        } else {
          return false
        }
      }
    }
  }
}
</script>

<style scoped lang="scss">
.top-tips {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  padding: 0 2px;
  color: #66788d;
  font-size: 13px;
  font-weight: 600;
}

.or-branch-link-tip {
  margin: 12px 0;
  color: #6f8093;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.condition-group-editor {
  user-select: none;
  border-radius: 10px;
  border: 1px solid #dfe7f1;
  position: relative;
  margin-bottom: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbfe 100%);
  box-shadow: 0 8px 18px rgba(24, 39, 58, 0.05);

  .branch-delete-icon {
    font-size: 16px;
    color: #8da0b4;
  }

  .header {
    background: linear-gradient(180deg, #f8fbff 0%, #eef4fb 100%);
    padding: 0 14px;
    font-size: 13px;
    color: #243446;
    font-weight: 700;
    height: 40px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid #e5edf6;

    span {
      flex: 1;
    }
  }

  .main-content {
    padding: 0 14px 4px;

    .condition-relation {
      color: #7b8b9d;
      display: flex;
      align-items: center;
      min-height: 38px;
      display: flex;
      justify-content: space-between;
      padding: 0 2px;
      font-size: 12px;
      font-weight: 700;
    }

    .condition-content-box {
      display: flex;
      justify-content: space-between;
      align-items: center;

      div {
        width: 100%;
        min-width: 120px;
      }

      div:not(:first-child) {
        margin-left: 16px;
      }
    }

    .cell-box {
      div {
        padding: 14px 0 12px;
        width: 100%;
        min-width: 120px;
        color: #7c8c9c;
        font-size: 12px;
        font-weight: 600;
        text-align: center;
        letter-spacing: 0.04em;
      }
    }

    .condition-content {
      display: flex;
      flex-direction: column;

      :deep(.el-input__wrapper) {
        border-top-left-radius: 0;
        border-bottom-left-radius: 0;
      }

      .content {
        flex: 1;
        padding: 0 0 4px 0;
        display: flex;
        align-items: center;
        min-height: 31.6px;
        flex-wrap: wrap;
      }
    }
  }

  .sub-content {
    padding: 12px 14px 14px;
    border-top: 1px solid #eef3f8;
  }
}

.workflow-node-branch-main {
  background: transparent;
}

.workflow-node-branch-link {
  padding: 0;
  font-weight: 700;
}

.workflow-node-branch-add-group {
  width: 100%;
  min-height: 38px;
  border-radius: 8px;
}
</style>
