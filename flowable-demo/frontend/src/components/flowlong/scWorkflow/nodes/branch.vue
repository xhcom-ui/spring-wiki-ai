<template>
  <div class="branch-wrap">
    <div class="branch-box-wrap">
      <div class="branch-box">
        <el-button class="add-branch" type="success" plain round :disabled="isReadonly" @click="addTerm">添加条件</el-button>
        <div v-for="(item, index) in nodeConfig.conditionNodes" :key="item.nodeKey || index" class="col-box">
          <div class="condition-node">
            <div class="condition-node-box">
              <div class="auto-judge" @click="show(index)">
                <div
                  v-if="index !== 0 && !item.properties?.isDefault"
                  class="sort-left"
                  @click.stop="arrTransfer(index, -1)"
                >
                  <el-icon><ArrowLeft /></el-icon>
                </div>
                <div class="title">
                  <span class="node-title">{{ item.nodeName }}</span>
                  <span class="priority-title">{{ item.properties?.isDefault ? '默认分支' : `条件 ${index + 1}` }}</span>
                  <button
                    v-if="!isReadonly"
                    type="button"
                    class="branch-action"
                    @click.stop="delTerm(index)"
                  >
                    <el-icon><Close /></el-icon>
                  </button>
                </div>
                <div class="content">
                  <span v-if="toText(item)">{{ toText(item) }}</span>
                  <span v-else class="placeholder">请设置条件</span>
                </div>
                <div
                  v-if="index !== nodeConfig.conditionNodes.length - 1 && !item.properties?.isDefault"
                  class="sort-right"
                  @click.stop="arrTransfer(index)"
                >
                  <el-icon><ArrowRight /></el-icon>
                </div>
              </div>
              <add-node v-model="item.childNode"></add-node>
            </div>
          </div>
          <slot v-if="item.childNode" :node="item"></slot>
          <div v-if="index === 0" class="top-left-cover-line"></div>
          <div v-if="index === 0" class="bottom-left-cover-line"></div>
          <div v-if="index === nodeConfig.conditionNodes.length - 1" class="top-right-cover-line"></div>
          <div v-if="index === nodeConfig.conditionNodes.length - 1" class="bottom-right-cover-line"></div>
        </div>
      </div>
      <add-node v-model="nodeConfig.childNode"></add-node>
    </div>

    <el-drawer v-model="drawer" title="条件分支设置" destroy-on-close append-to-body :size="560">
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
            <el-form-item>
              <el-checkbox v-model="form.properties.isDefault" :disabled="isReadonly" @change="handleDefaultChange">设为默认分支</el-checkbox>
            </el-form-item>

            <el-form-item label="条件描述">
              <el-input
                v-model="form.properties.conditionSummary"
                :disabled="isReadonly || form.properties.isDefault"
                placeholder="例如：请假天数 > 3"
              />
            </el-form-item>

            <el-form-item label="条件表达式">
              <el-input
                v-model="form.properties.conditionExpression"
                type="textarea"
                :rows="4"
                :disabled="isReadonly || form.properties.isDefault"
                placeholder="${days > 3}"
              />
            </el-form-item>

            <el-alert
              :title="form.properties.isDefault ? '默认分支不需要条件表达式，当前分支会承接其余未命中的路由。' : '建议条件描述和表达式保持一致，便于后端版本对比和任务摘要展示。'"
              type="info"
              :closable="false"
            />
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
import addNode from './addNode.vue'

export default {
  inject: ['flowlongReadonly'],
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
      index: 0,
      form: {
        properties: {}
      }
    }
  },
  computed: {
    isReadonly() {
      return Boolean(this.flowlongReadonly?.value ?? this.flowlongReadonly)
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
    createNodeKey(prefix = 'branch') {
      return `${prefix}_${Math.random().toString(36).slice(2, 10)}`
    },
    normalizeConditionNodes() {
      const list = Array.isArray(this.nodeConfig.conditionNodes) ? this.nodeConfig.conditionNodes : []
      const defaultNodes = list.filter((item) => item?.properties?.isDefault)
      const normalNodes = list.filter((item) => !item?.properties?.isDefault)
      normalNodes.forEach((item, index) => {
        item.properties = item.properties || {}
        item.properties.isDefault = false
        item.nodeName = `条件 ${index + 1}`
        if (!item.properties.conditionSummary) {
          item.properties.conditionSummary = item.nodeName
        }
      })
      if (defaultNodes.length) {
        const fallback = defaultNodes[defaultNodes.length - 1]
        fallback.properties = fallback.properties || {}
        fallback.properties.isDefault = true
        fallback.properties.conditionSummary = '默认分支'
        fallback.properties.conditionExpression = ''
        fallback.nodeName = '默认条件'
        this.nodeConfig.conditionNodes = [...normalNodes, fallback]
        return
      }
      if (normalNodes.length) {
        const fallback = normalNodes.pop()
        fallback.properties = fallback.properties || {}
        fallback.properties.isDefault = true
        fallback.properties.conditionSummary = '默认分支'
        fallback.properties.conditionExpression = ''
        fallback.nodeName = '默认条件'
        this.nodeConfig.conditionNodes = [...normalNodes, fallback]
        return
      }
      this.nodeConfig.conditionNodes = normalNodes
    },
    show(index) {
      if (!this.nodeConfig.conditionNodes?.[index]) {
        return
      }
      this.index = index
      this.form = JSON.parse(JSON.stringify(this.nodeConfig.conditionNodes[index]))
      this.form.properties = this.form.properties || {}
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
    save() {
      if (this.isReadonly) {
        this.drawer = false
        return
      }
      if (this.form.properties.isDefault) {
        this.nodeConfig.conditionNodes.forEach((item, index) => {
          item.properties = item.properties || {}
          item.properties.isDefault = index === this.index
          if (item.properties.isDefault) {
            item.properties.conditionSummary = '默认分支'
            item.properties.conditionExpression = ''
            item.nodeName = this.form.nodeName || '默认条件'
          }
        })
      }
      this.nodeConfig.conditionNodes[this.index] = this.form
      this.normalizeConditionNodes()
      this.$emit('update:modelValue', this.nodeConfig)
      this.drawer = false
    },
    addTerm() {
      if (this.isReadonly) {
        return
      }
      const branchCount = this.nodeConfig.conditionNodes.filter((item) => !item.properties?.isDefault).length + 1
      const defaultIndex = this.nodeConfig.conditionNodes.findIndex((item) => item.properties?.isDefault)
      const branch = {
        nodeName: `条件 ${branchCount}`,
        nodeKey: this.createNodeKey(),
        type: 3,
        properties: {
          conditionSummary: `条件 ${branchCount}`,
          conditionExpression: '',
          isDefault: false
        }
      }
      if (defaultIndex >= 0) {
        this.nodeConfig.conditionNodes.splice(defaultIndex, 0, branch)
      } else {
        this.nodeConfig.conditionNodes.push(branch)
      }
      this.normalizeConditionNodes()
      this.$emit('update:modelValue', this.nodeConfig)
    },
    appendChildChain(node, nextNode) {
      if (!node) {
        return nextNode || null
      }
      if (!node.childNode) {
        node.childNode = nextNode || null
        return node
      }
      this.appendChildChain(node.childNode, nextNode)
      return node
    },
    delTerm(index) {
      if (this.isReadonly) {
        return
      }
      const removed = this.nodeConfig.conditionNodes.splice(index, 1)[0]
      if (!removed) {
        return
      }
      if (this.nodeConfig.conditionNodes.length === 1) {
        const remaining = this.nodeConfig.conditionNodes[0]
        let replacement = remaining?.childNode || null
        if (replacement && this.nodeConfig.childNode) {
          replacement = this.appendChildChain(replacement, this.nodeConfig.childNode)
        } else if (!replacement) {
          replacement = this.nodeConfig.childNode || null
        }
        this.$emit('update:modelValue', replacement)
        return
      }
      if (removed?.properties?.isDefault && this.nodeConfig.conditionNodes.length) {
        const last = this.nodeConfig.conditionNodes[this.nodeConfig.conditionNodes.length - 1]
        last.properties = last.properties || {}
        last.properties.isDefault = true
        last.properties.conditionSummary = '默认分支'
        last.properties.conditionExpression = ''
      }
      this.normalizeConditionNodes()
      this.$emit('update:modelValue', this.nodeConfig)
    },
    arrTransfer(index, type = 1) {
      if (this.isReadonly) {
        return
      }
      if (this.nodeConfig.conditionNodes[index]?.properties?.isDefault) {
        return
      }
      if (this.nodeConfig.conditionNodes[index + type]?.properties?.isDefault) {
        return
      }
      this.nodeConfig.conditionNodes[index] = this.nodeConfig.conditionNodes.splice(index + type, 1, this.nodeConfig.conditionNodes[index])[0]
      this.normalizeConditionNodes()
      this.$emit('update:modelValue', this.nodeConfig)
    },
    handleDefaultChange() {
      if (this.form.properties.isDefault) {
        this.form.properties.conditionSummary = '默认分支'
        this.form.properties.conditionExpression = ''
      }
    },
    toText(node) {
      if (node.properties?.isDefault) {
        return '其他条件进入此分支'
      }
      return node.properties?.conditionSummary || node.properties?.conditionExpression || ''
    }
  }
}
</script>

<style scoped lang="scss">
.top-tips {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: #646a73;
}

.condition-group-editor {
  user-select: none;
  border-radius: 4px;
  border: 1px solid #e4e5e7;
  position: relative;
  margin-bottom: 16px;
}
</style>
