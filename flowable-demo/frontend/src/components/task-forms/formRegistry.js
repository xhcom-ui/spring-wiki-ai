import GeneralApprovalForm from './GeneralApprovalForm.vue'
import GenericTaskForm from './GenericTaskForm.vue'
import LeaveReviewForm from './LeaveReviewForm.vue'
import ManagerApprovalForm from './ManagerApprovalForm.vue'

function buildSummaryField(label, getter) {
  return { label, getter }
}

const leaveSummaryFields = [
  buildSummaryField('申请人', (task) => task.applicant || '-'),
  buildSummaryField('请假天数', (task) => task.days || '-'),
  buildSummaryField('开始时间', (task) => task.startDate || null),
  buildSummaryField('结束时间', (task) => task.endDate || null)
]

export const taskFormRegistry = [
  {
    id: 'leave-review',
    label: '请假业务表单',
    componentKey: 'LEAVE_START',
    component: LeaveReviewForm,
    summaryFields: leaveSummaryFields
  },
  {
    id: 'manager-review',
    label: '经理审批表单',
    componentKey: 'MANAGER_APPROVAL',
    component: ManagerApprovalForm,
    summaryFields: leaveSummaryFields
  },
  {
    id: 'general-review',
    label: '最终审批表单',
    componentKey: 'GENERAL_APPROVAL',
    component: GeneralApprovalForm,
    summaryFields: leaveSummaryFields
  }
]

const genericRegistration = {
  id: 'generic',
  label: '通用表单',
  componentKey: 'GENERIC_TASK',
  component: GenericTaskForm,
  summaryFields: []
}

const legacyFormKeyAliases = {
  'leave-form': 'LEAVE_START',
  'leave-review': 'LEAVE_START',
  'leave-request': 'LEAVE_START',
  'leave-apply': 'LEAVE_START',
  'manager-approval': 'MANAGER_APPROVAL',
  'manager-review': 'MANAGER_APPROVAL',
  'dept-manager-approval': 'MANAGER_APPROVAL',
  'dept-review': 'MANAGER_APPROVAL',
  'leave-approve-form': 'MANAGER_APPROVAL',
  'general-approval': 'GENERAL_APPROVAL',
  'general-review': 'GENERAL_APPROVAL',
  'final-approval': 'GENERAL_APPROVAL',
  'ceo-approval': 'GENERAL_APPROVAL'
}

export function resolveTaskFormRegistration(task) {
  const componentKey = String(task?.componentKey || '').trim().toUpperCase()
  const formKey = String(task?.formKey || '').trim().toLowerCase()
  const effectiveComponentKey = componentKey || legacyFormKeyAliases[formKey] || ''
  if (!effectiveComponentKey) {
    return genericRegistration
  }
  return taskFormRegistry.find((item) => item.componentKey === effectiveComponentKey) || genericRegistration
}

export function resolveTaskFormSummary(task) {
  return resolveTaskFormRegistration(task).summaryFields
    .map((item) => ({
      label: item.label,
      value: item.getter(task)
    }))
    .filter((item) => item.value !== null && item.value !== undefined && item.value !== '')
}
