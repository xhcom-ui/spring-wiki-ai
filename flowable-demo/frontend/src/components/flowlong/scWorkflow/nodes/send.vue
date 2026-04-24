<template>
  <div class="node-wrap">
    <div class="node-wrap-box" @click="show">
      <div class="title copy-title">
        <el-icon class="icon"><Promotion /></el-icon>
        <span>{{ nodeConfig.nodeName }}</span>
        <el-icon class="close" @click.stop="delNode"><Close /></el-icon>
      </div>
      <div class="content">
        <span v-if="summaryText">{{ summaryText }}</span>
        <span v-else class="placeholder">请选择抄送人</span>
      </div>
    </div>
    <add-node v-model="nodeConfig.childNode"></add-node>
    <el-drawer v-model="drawer" title="抄送节点设置" destroy-on-close append-to-body :size="520">
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
            <el-form-item label="选择抄送人">
              <el-button type="primary" round :disabled="isReadonly" @click="selectHandle(1, form.properties.copyUserList)">
                选择人员
              </el-button>
              <div v-if="form.properties.copyUserList.length" class="tags-list">
                <el-tag
                  v-for="(user, index) in form.properties.copyUserList"
                  :key="user.id || `${user.name}-${index}`"
                  :closable="!isReadonly"
                  @close="removeUser(index)"
                >
                  {{ user.name || user.id }}
                </el-tag>
              </div>
            </el-form-item>
            <el-form-item label="抄送人表达式">
              <el-input
                v-model="form.properties.copyUsers"
                type="textarea"
                :rows="3"
                :disabled="isReadonly"
                placeholder="userA,userB 或 ${copyUsers}"
              />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="form.properties.allowInitiatorSelect" :disabled="isReadonly">允许发起人自选抄送人</el-checkbox>
            </el-form-item>
            <el-alert
              title="抄送节点当前仅参与设计与回显，不会生成审批待办任务。"
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
import { summarizeFlowlongNode } from '../../../../utils/flowlongAdapter'
import addNode from './addNode.vue'

export default {
  inject: ['select', 'flowlongReadonly'],
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
      }
    }
  },
  computed: {
    isReadonly() {
      return Boolean(this.flowlongReadonly?.value ?? this.flowlongReadonly)
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
      this.form.properties.copyUserList = Array.isArray(this.form.properties.copyUserList) ? this.form.properties.copyUserList : []
      this.drawer = true
      this.isEditTitle = false
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
    syncCopyUsers() {
      const values = (this.form.properties.copyUserList || [])
        .map((item) => item.id || item.name)
        .filter(Boolean)
      this.form.properties.copyUsers = values.join(',')
    },
    selectHandle(type, data) {
      if (this.isReadonly) {
        return
      }
      this.select(type, data)
    },
    removeUser(index) {
      if (this.isReadonly) {
        return
      }
      this.form.properties.copyUserList.splice(index, 1)
      this.syncCopyUsers()
    },
    save() {
      if (this.isReadonly) {
        this.drawer = false
        return
      }
      this.syncCopyUsers()
      this.$emit('update:modelValue', this.form)
      this.drawer = false
    },
    delNode() {
      if (this.isReadonly) {
        return
      }
      this.$emit('update:modelValue', this.nodeConfig.childNode)
    }
  }
}
</script>

<style scoped>
.copy-title {
  background: linear-gradient(135deg, #4f8ef7, #2473d6);
}
</style>
