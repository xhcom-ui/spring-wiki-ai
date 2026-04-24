<template>
	 <div class="add-node-btn-box">
	 	<div class="add-node-btn">
			<el-popover
        placement="right-start"
        :width="232"
        trigger="click"
        :hide-after="0"
        :show-after="0"
        popper-class="flowlong-node-picker"
      >
				<template #reference>
					<el-button type="primary" circle class="flowlong-add-trigger">
            <el-icon><Plus /></el-icon>
          </el-button>
				</template>
				<div class="add-node-popover-body">
          <button type="button" class="flowlong-node-option" @click="addType(1)">
            <span class="flowlong-node-option-icon flowlong-node-option-icon-approve">
              <el-icon><UserFilled /></el-icon>
            </span>
            <span class="flowlong-node-option-copy">
              <strong>审批节点</strong>
              <small>配置办理人</small>
            </span>
          </button>
          <button type="button" class="flowlong-node-option" @click="addType(2)">
            <span class="flowlong-node-option-icon flowlong-node-option-icon-copy">
              <el-icon><Promotion /></el-icon>
            </span>
            <span class="flowlong-node-option-copy">
              <strong>抄送节点</strong>
              <small>抄送通知人</small>
            </span>
          </button>
          <button type="button" class="flowlong-node-option" @click="addType(4)">
            <span class="flowlong-node-option-icon flowlong-node-option-icon-branch">
              <el-icon><Share /></el-icon>
            </span>
            <span class="flowlong-node-option-copy">
              <strong>条件分支</strong>
              <small>配置判断条件</small>
            </span>
          </button>
				</div>
			</el-popover>
	 	</div>
	 </div>
</template>

<script>
  import { Plus, Promotion, Share, UserFilled } from '@element-plus/icons-vue'

	export default {
		props: {
			modelValue: { type: Object, default: () => {} }
		},
    components: {
      Plus,
      Promotion,
      Share,
      UserFilled
    },
		data() {
			return {

			}
		},
		mounted() {

		},
		methods: {
      getNodeKey() {
        return 'flk' + Date.now()
      },
			addType(type){
				var node = {}
				if (type == 1) {
					node = {
						nodeName: "审核人",
            nodeKey: this.getNodeKey(),
            type: 1,			//节点类型
						setType: 1,			//审核人类型 1，选择成员 3，选择角色
            nodeAssigneeList: [],	//审核人员，根据 setType 确定成员还是角色
						examineLevel: 1,	//指定主管层级
						directorLevel: 1,	//自定义连续主管审批层级
						selectMode: 1,		//发起人自选类型
						termAuto: false,	//审批期限超时自动审批
						term: 0,			//审批期限
						termMode: 1,		//审批期限超时后执行类型
						examineMode: 1,		//多人审批时审批方式
						directorMode: 0,	//连续主管审批方式
						childNode: this.modelValue
					}
				}else if(type == 2){
					node = {
						nodeName: "抄送人",
            nodeKey: this.getNodeKey(),
						type: 2,
						userSelectFlag: true,
						nodeAssigneeList: [],
						childNode: this.modelValue
					}

				}else if(type == 4){
					node = {
						nodeName: "条件路由",
            nodeKey: this.getNodeKey(),
						type: 4,
						conditionNodes: [
							{
								nodeName: "条件1",
                nodeKey: this.getNodeKey(),
								type: 3,
								priorityLevel: 1,
								conditionMode: 1,
								conditionList: []
							},
							{
								nodeName: "条件2",
                nodeKey: this.getNodeKey(),
								type: 3,
								priorityLevel: 2,
								conditionMode: 1,
								conditionList: []
							}
						],
						childNode: this.modelValue
					}

				}
				this.$emit("update:modelValue", node)
			}
		}
	}
</script>

<style scoped>
.flowlong-add-trigger {
  width: 32px;
  height: 32px;
  min-height: 32px;
  padding: 0;
  border: 1px solid #b7cdf3;
  border-radius: 50%;
  background: linear-gradient(180deg, #5ea3ff 0%, #3d8bfd 100%);
  box-shadow: 0 6px 12px rgba(61, 139, 253, 0.18);
}

.flowlong-add-trigger:hover,
.flowlong-add-trigger:focus-visible {
  border-color: #2f7df4;
  box-shadow: 0 8px 16px rgba(61, 139, 253, 0.24);
}

.add-node-popover-body {
  display: grid;
  gap: 6px;
  padding: 0;
}

.flowlong-node-option {
  width: 100%;
  min-height: 54px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfdff 100%);
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  align-items: start;
  gap: 10px;
  padding: 9px 11px;
  text-align: left;
  transition: border-color 0.16s ease, background-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.flowlong-node-option:hover {
  border-color: #bad0ef;
  background: #f7fbff;
  box-shadow: 0 8px 18px rgba(27, 52, 81, 0.08);
  transform: translateY(-1px);
}

.flowlong-node-option-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  display: inline-grid;
  place-items: center;
  font-size: 16px;
  border: 1px solid #dde6f0;
  background: #f7faff;
}

.flowlong-node-option-icon-approve {
  color: #f79032;
  background: #fff7ee;
  border-color: #f4dcc2;
}

.flowlong-node-option-icon-copy {
  color: #3e8cff;
  background: #eef5ff;
  border-color: #d5e5ff;
}

.flowlong-node-option-icon-branch {
  color: #20b883;
  background: #edf9f5;
  border-color: #cdebdc;
}

.flowlong-node-option-copy {
  display: grid;
  gap: 1px;
  padding-top: 1px;
}

.flowlong-node-option-copy strong {
  color: #243445;
  font-size: 13px;
  font-weight: 700;
}

.flowlong-node-option-copy small {
  color: #7f8b99;
  font-size: 11px;
  line-height: 1.35;
}
</style>

<style>
.flowlong-node-picker {
  padding: 8px !important;
  border-radius: 10px !important;
  border: 1px solid #d7e0eb !important;
  box-shadow: 0 14px 28px rgba(33, 52, 77, 0.14) !important;
}
</style>
