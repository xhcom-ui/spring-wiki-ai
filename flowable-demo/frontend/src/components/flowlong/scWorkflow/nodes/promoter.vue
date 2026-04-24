<template>
  <div class="node-wrap">
    <div class="node-wrap-box start-node" @click="show">
      <div class="title promoter-title">
        <el-icon class="icon"><UserFilled /></el-icon>
        <span>{{ nodeConfig.nodeName }}</span>
      </div>
      <div class="content">
        <span>{{ summaryText }}</span>
      </div>
    </div>
    <add-node v-model="nodeConfig.childNode"></add-node>
    <el-drawer v-model="drawer" title="发起节点设置" destroy-on-close append-to-body :size="520">
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
            <el-form-item label="发起人说明">
              <el-input v-model="form.properties.initiatorLabel" :disabled="isReadonly" placeholder="申请人 / 发起人" />
            </el-form-item>

            <el-form-item label="选择发起人">
              <el-button type="primary" round :disabled="isReadonly" @click="selectHandle(1, form.properties.initiatorUserList)">选择人员</el-button>
              <div v-if="form.properties.initiatorUserList.length" class="tags-list">
                <el-tag
                  v-for="(user, index) in form.properties.initiatorUserList"
                  :key="user.id || `${user.name}-${index}`"
                  :closable="!isReadonly"
                  @close="removeUser(index)"
                >
                  {{ user.name || user.id }}
                </el-tag>
              </div>
            </el-form-item>

            <el-form-item label="发起人表达式">
              <el-input
                v-model="form.properties.initiatorUsers"
                type="textarea"
                :rows="2"
                :disabled="isReadonly"
                placeholder="userA,userB 或 ${startUsers}"
              />
            </el-form-item>

            <el-form-item label="发起表单">
              <el-select v-model="form.properties.formKey" :disabled="isReadonly" placeholder="请选择发起表单" filterable @change="syncFormCatalog">
                <el-option label="不绑定表单" value="" />
                <el-option
                  v-for="item in startCatalogs"
                  :key="item.id || item.formKey"
                  :label="`${item.formName} · ${item.formKey}`"
                  :value="item.formKey"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="业务页面">
              <el-input v-model="form.properties.pageLabel" :disabled="isReadonly" placeholder="请假发起页" />
            </el-form-item>

            <el-form-item label="表单名称">
              <el-input v-model="form.properties.formLabel" :disabled="isReadonly" placeholder="请假申请单" />
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
      }
    }
  },
  computed: {
    isReadonly() {
      return Boolean(this.flowlongReadonly?.value ?? this.flowlongReadonly)
    },
    startCatalogs() {
      const catalogs = this.flowlongFormCatalogs?.value || this.flowlongFormCatalogs || []
      return catalogs.filter((item) => String(item.scope || '').toUpperCase() === 'START')
    },
    summaryText() {
      return summarizeFlowlongNode(this.nodeConfig) || '发起人'
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
      this.form.properties.initiatorUserList = Array.isArray(this.form.properties.initiatorUserList) ? this.form.properties.initiatorUserList : []
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
    syncFormCatalog() {
      const current = this.startCatalogs.find((item) => item.formKey === this.form.properties.formKey)
      if (!current) {
        return
      }
      this.form.properties.formLabel = current.formName || this.form.properties.formLabel || ''
      this.form.properties.pageLabel = current.pageLabel || this.form.properties.pageLabel || ''
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
      this.form.properties.initiatorUserList.splice(index, 1)
      this.syncInitiatorUsers()
    },
    syncInitiatorUsers() {
      const ids = (this.form.properties.initiatorUserList || []).map((item) => item.id || item.name).filter(Boolean)
      this.form.properties.initiatorUsers = ids.join(',')
    },
    save() {
      if (this.isReadonly) {
        this.drawer = false
        return
      }
      this.syncInitiatorUsers()
      this.$emit('update:modelValue', this.form)
      this.drawer = false
    }
  }
}
</script>

<style scoped>
.promoter-title {
  background: linear-gradient(135deg, #445d8a, #2f446c);
}
</style>
