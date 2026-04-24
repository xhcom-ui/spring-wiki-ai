<template>
	<div class="node-wrap">
		<div class="node-wrap-box" @click="show">
			<div class="title" style="background: #3296fa;">
				<el-icon class="icon"><Promotion /></el-icon>
				<span>{{ nodeConfig.nodeName }}</span>
				<el-icon class="close" @click.stop="delNode()"><Close /></el-icon>
			</div>
			<div class="content">
				<span v-if="toText(nodeConfig)">{{ toText(nodeConfig) }}</span>
				<span v-else class="placeholder">请选择人员</span>
			</div>
		</div>
		<add-node v-model="nodeConfig.childNode"></add-node>
		<el-drawer title="抄送人设置" v-model="drawer" destroy-on-close append-to-body :size="540" class="workflow-node-drawer">
			<template #header>
				<div class="node-wrap-drawer__title workflow-node-drawer__title">
          <div class="workflow-node-drawer__title-main">
					<label @click="editTitle" v-if="!isEditTitle">{{form.nodeName}}<el-icon class="node-wrap-drawer__title-edit"><Edit /></el-icon></label>
					<el-input v-if="isEditTitle" ref="nodeTitle" v-model="form.nodeName" clearable @blur="saveTitle" @keyup.enter="saveTitle"></el-input>
            <span class="workflow-node-drawer__subtitle">Copy Node</span>
          </div>
				</div>
			</template>
			<el-container class="workflow-node-drawer__layout">
				<el-main class="workflow-node-drawer__main">
          <div class="workflow-node-drawer__intro">
            <strong>抄送节点设置</strong>
            <span>维护固定抄送人名单，并决定是否允许发起人在提交时追加通知对象。</span>
          </div>
					<el-form label-position="top">
						<el-form-item label="选择要抄送的人员">
							<el-button type="primary" class="workflow-node-picker-btn" @click="selectHandle(1, form.nodeAssigneeList)">
                <el-icon><Plus /></el-icon>
                选择人员
              </el-button>
							<div class="tags-list">
								<el-tag v-for="(user, index) in form.nodeAssigneeList" :key="user.id" closable @close="delUser(index)">{{user.name}}</el-tag>
							</div>
						</el-form-item>
						<el-form-item label="">
							<el-checkbox v-model="form.userSelectFlag" label="允许发起人自选抄送人"></el-checkbox>
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
  import { Close, Edit, Plus, Promotion } from '@element-plus/icons-vue'

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
      Promotion
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
			selectHandle(type, data){
				this.select(type, data)
			},
			toText(nodeConfig){
				if (nodeConfig.nodeAssigneeList && nodeConfig.nodeAssigneeList.length>0) {
					const users = nodeConfig.nodeAssigneeList.map(item=>item.name).join("、")
					return users
				}else{
					if(nodeConfig.userSelectFlag){
						return "发起人自选"
					}else{
						return false
					}

				}
			}
		}
	}
</script>

<style>
</style>
