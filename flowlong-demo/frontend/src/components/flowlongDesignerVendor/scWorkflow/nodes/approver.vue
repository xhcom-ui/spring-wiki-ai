<template>
	<div class="node-wrap">
		<div class="node-wrap-box" @click="show">
			<div class="title" style="background: #ff943e;">
				<el-icon class="icon"><UserFilled /></el-icon>
				<span>{{ nodeConfig.nodeName }}</span>
				<el-icon class="close" @click.stop="delNode()"><Close /></el-icon>
			</div>
			<div class="content">
				<span v-if="toText(nodeConfig)">{{ toText(nodeConfig) }}</span>
				<span v-else class="placeholder">请选择</span>
			</div>
		</div>
		<add-node v-model="nodeConfig.childNode"></add-node>
		<el-drawer title="审批人设置" v-model="drawer" destroy-on-close append-to-body :size="560" class="workflow-node-drawer">
			<template #header>
				<div class="node-wrap-drawer__title workflow-node-drawer__title">
          <div class="workflow-node-drawer__title-main">
					<label @click="editTitle" v-if="!isEditTitle">{{form.nodeName}}<el-icon class="node-wrap-drawer__title-edit"><Edit /></el-icon></label>
					<el-input v-if="isEditTitle" ref="nodeTitle" v-model="form.nodeName" clearable @blur="saveTitle" @keyup.enter="saveTitle"></el-input>
            <span class="workflow-node-drawer__subtitle">Approver Node</span>
          </div>
				</div>
			</template>
			<el-container class="workflow-node-drawer__layout">
				<el-main class="workflow-node-drawer__main">
          <div class="workflow-node-drawer__intro">
            <strong>审批节点设置</strong>
            <span>设置审批人范围、超时处理和多人审批方式，保持流程执行规则一致。</span>
          </div>
					<el-form label-position="top">

						<el-form-item label="审批人员类型">
							<el-select v-model="form.setType" @change="changeSetType">
								<el-option :value="1" label="指定成员"></el-option>
								<el-option :value="2" label="主管"></el-option>
								<el-option :value="3" label="角色"></el-option>
								<el-option :value="4" label="发起人自选"></el-option>
								<el-option :value="5" label="发起人自己"></el-option>
								<el-option :value="7" label="连续多级主管"></el-option>
							</el-select>
						</el-form-item>

						<el-form-item v-if="form.setType==1" label="选择成员">
							<el-button type="primary" class="workflow-node-picker-btn" @click="selectHandle(1, form.nodeAssigneeList)">
                <el-icon><Plus /></el-icon>
                选择人员
              </el-button>
							<div class="tags-list">
								<el-tag v-for="(user, index) in form.nodeAssigneeList" :key="user.id" closable @close="delUser(index)">{{user.name}}</el-tag>
							</div>
						</el-form-item>

						<el-form-item v-if="form.setType==2" label="指定主管">
              <div class="workflow-node-inline-field">
							<span>发起人的第</span><el-input-number v-model="form.examineLevel" :min="1"/><span>级主管</span>
              </div>
						</el-form-item>

						<el-form-item v-if="form.setType==3" label="选择角色">
							<el-button type="primary" class="workflow-node-picker-btn" @click="selectHandle(2, form.nodeAssigneeList)">
                <el-icon><Plus /></el-icon>
                选择角色
              </el-button>
							<div class="tags-list">
								<el-tag v-for="(role, index) in form.nodeAssigneeList" :key="role.id" type="info" closable @close="delRole(index)">{{role.name}}</el-tag>
							</div>
						</el-form-item>

						<el-form-item v-if="form.setType==4" label="发起人自选">
							<el-radio-group v-model="form.selectMode">
                <div class="workflow-node-radio-row"><el-radio :label="1">自选一个人</el-radio></div>
                <div class="workflow-node-radio-row"><el-radio :label="2">自选多个人</el-radio></div>
							</el-radio-group>
						</el-form-item>

						<el-form-item v-if="form.setType==7" label="连续主管审批终点">
							<el-radio-group v-model="form.directorMode">
                <div class="workflow-node-radio-row"><el-radio :label="0">直到最上层主管</el-radio></div>
                <div class="workflow-node-radio-row"><el-radio :label="1">自定义审批终点</el-radio></div>
							</el-radio-group>
							<div v-if="form.directorMode==1" class="workflow-node-inline-field">
                <span>直到发起人的第</span><el-input-number v-model="form.directorLevel" :min="1"/><span>级主管</span>
              </div>
						</el-form-item>

						<el-divider></el-divider>
						<el-form-item label="">
							<el-checkbox v-model="form.termAuto" label="超时自动审批"></el-checkbox>
						</el-form-item>
						<template v-if="form.termAuto">
							<el-form-item label="审批期限（为 0 则不生效）">
                <div class="workflow-node-inline-field">
								<el-input-number v-model="form.term" :min="0"/><span>小时</span>
                </div>
							</el-form-item>
							<el-form-item label="审批期限超时后执行">
								<el-radio-group v-model="form.termMode">
                  <div class="workflow-node-radio-row"><el-radio :label="0">自动通过</el-radio></div>
                  <div class="workflow-node-radio-row"><el-radio :label="1">自动拒绝</el-radio></div>
								</el-radio-group>
							</el-form-item>
						</template>
						<el-divider></el-divider>
						<el-form-item label="多人审批时审批方式">
							<el-radio-group v-model="form.examineMode">
                <div class="workflow-node-radio-row"><el-radio :label="1">按顺序依次审批</el-radio></div>
                <div class="workflow-node-radio-row"><el-radio :label="2">会签（可同时审批，每个人必须审批通过）</el-radio></div>
                <div class="workflow-node-radio-row"><el-radio :label="3">或签（有一人审批通过即可）</el-radio></div>
							</el-radio-group>
						</el-form-item>
					</el-form>
				</el-main>
				<el-footer class="workflow-node-drawer-footer">
					<el-button type="primary" @click="save">保存</el-button>
					<el-button @click="drawer=false">取消</el-button>
				</el-footer>
			</el-container>
		</el-drawer>
	</div>
</template>

<script>
	import addNode from './addNode.vue'
  import { Close, Edit, Plus, UserFilled } from '@element-plus/icons-vue'

	export default {
		inject: ['select'],
		props: {
			modelValue: { type: Object, default: () => {} }
		},
		components: {
			addNode,
      Close,
      Edit,
      Plus,
      UserFilled
		},
		data() {
			return {
				nodeConfig: {},
				drawer: false,
				isEditTitle: false,
				form: {}
			}
		},
		watch:{
			modelValue(){
				this.nodeConfig = this.modelValue
			}
		},
		mounted() {
			this.nodeConfig = this.modelValue
		},
		methods: {
			show(){
				this.form = {}
				this.form = JSON.parse(JSON.stringify(this.nodeConfig))
				this.drawer = true
			},
			editTitle(){
				this.isEditTitle = true
				this.$nextTick(()=>{
					this.$refs.nodeTitle.focus()
				})
			},
			saveTitle(){
				this.isEditTitle = false
			},
			save(){
				this.$emit("update:modelValue", this.form)
				this.drawer = false
			},
			delNode(){
				this.$emit("update:modelValue", this.nodeConfig.childNode)
			},
			delUser(index){
				this.form.nodeAssigneeList.splice(index, 1)
			},
			delRole(index){
				this.form.nodeAssigneeList.splice(index, 1)
			},
			selectHandle(type, data){
				this.select(type, data)
			},
      changeSetType() {
        this.form.nodeAssigneeList = [];
      },
			toText(nodeConfig){
				if(nodeConfig.setType == 1){
					if (nodeConfig.nodeAssigneeList && nodeConfig.nodeAssigneeList.length>0) {
						const users = nodeConfig.nodeAssigneeList.map(item=>item.name).join("、")
						return users
					}else{
						return false
					}
				}else if (nodeConfig.setType == 2) {
					return nodeConfig.examineLevel == 1 ? '直接主管' : `发起人的第${nodeConfig.examineLevel}级主管`
				}else if (nodeConfig.setType == 3) {
					if (nodeConfig.nodeAssigneeList && nodeConfig.nodeAssigneeList.length>0) {
						const roles = nodeConfig.nodeAssigneeList.map(item=>item.name).join("、")
						return '角色-' + roles
					}else{
						return false
					}
				}else if (nodeConfig.setType == 4) {
					return "发起人自选"
				}else if (nodeConfig.setType == 5) {
					return "发起人自己"
				}else if (nodeConfig.setType == 7) {
					return "连续多级主管"
				}
			}
		}
	}
</script>

<style>

</style>
