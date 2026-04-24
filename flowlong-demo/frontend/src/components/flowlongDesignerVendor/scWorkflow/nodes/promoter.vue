<template>
	<div class="node-wrap">
		<div class="node-wrap-box start-node" @click="show">
			<div class="title" style="background: #576a95;">
				<el-icon class="icon"><UserFilled /></el-icon>
				<span>{{ nodeConfig.nodeName }}</span>
			</div>
			<div class="content">
				<span>{{ toText(nodeConfig) }}</span>
			</div>
		</div>
		<add-node v-model="nodeConfig.childNode"></add-node>
		<el-drawer title="发起人" v-model="drawer" destroy-on-close append-to-body :size="540" class="workflow-node-drawer">
			<template #header>
				<div class="node-wrap-drawer__title workflow-node-drawer__title">
          <div class="workflow-node-drawer__title-main">
					<label @click="editTitle" v-if="!isEditTitle">{{form.nodeName}}<el-icon class="node-wrap-drawer__title-edit"><Edit /></el-icon></label>
					<el-input v-if="isEditTitle" ref="nodeTitle" v-model="form.nodeName" clearable @blur="saveTitle" @keyup.enter="saveTitle"></el-input>
            <span class="workflow-node-drawer__subtitle">Promoter Node</span>
          </div>
				</div>
			</template>
			<el-container class="workflow-node-drawer__layout">
				<el-main class="workflow-node-drawer__main">
          <div class="workflow-node-drawer__intro">
            <strong>发起节点设置</strong>
            <span>控制哪些角色可以发起当前流程，未指定时默认所有人可见。</span>
          </div>
					<el-form label-position="top">
						<el-form-item label="谁可以发起此审批">
							<el-button type="primary" class="workflow-node-picker-btn" @click="selectHandle(2, form.nodeAssigneeList)">
                <el-icon><Plus /></el-icon>
                选择角色
              </el-button>
							<div class="tags-list">
								<el-tag v-for="(role, index) in form.nodeAssigneeList" :key="role.id" type="info" closable @close="delRole(index)">{{role.name}}</el-tag>
							</div>
						</el-form-item>
						<el-alert v-if="form.nodeAssigneeList.length==0" title="不指定则默认所有人都可发起此审批" type="info" :closable="false"/>
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
  import { Edit, Plus, UserFilled } from '@element-plus/icons-vue'

	export default {
		inject: ['select'],
		props: {
			modelValue: { type: Object, default: () => {} }
		},
		components: {
			addNode,
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
				this.isEditTitle = false
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
			selectHandle(type, data){
				this.select(type, data)
			},
			delRole(index){
				this.form.nodeAssigneeList.splice(index, 1)
			},
			save(){
				this.$emit("update:modelValue", this.form)
				this.drawer = false
			},
			toText(nodeConfig){
				if(nodeConfig.nodeAssigneeList && nodeConfig.nodeAssigneeList.length > 0){
					return nodeConfig.nodeAssigneeList.map(item=>item.name).join("、")
				}else{
					return "所有人"
				}
			}
		}
	}
</script>

<style>
</style>
