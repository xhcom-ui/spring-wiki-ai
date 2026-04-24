import {
  findTaskFormDefinition,
  findTaskPageDefinition,
  normalizeCatalogKey
} from './taskPageCatalog'

export function normalizePageKey(value) {
  return normalizeCatalogKey(value)
}

export function resolveRuntimePageKey(task, mode = 'todo') {
  if (!task) {
    return ''
  }
  const page = mode === 'done' ? task.donePage : task.todoPage
  return normalizePageKey(page || task.formKey || task.taskDefinitionKey || task.processDefinitionKey)
}

export function canOpenRuntimePage(task, mode = 'todo') {
  return Boolean(resolveRuntimePageKey(task, mode))
}

export function buildRuntimeRoute(task, mode = 'todo') {
  const pageKey = resolveRuntimePageKey(task, mode)
  return {
    name: 'task-runtime',
    params: { pageKey: pageKey || 'generic-task' },
    query: {
      taskId: task?.id || '',
      mode,
      taskName: task?.name || '',
      processInstanceId: task?.processInstanceId || '',
      processDefinitionKey: task?.processDefinitionKey || '',
      processDefinitionName: task?.processDefinitionName || '',
      taskDefinitionKey: task?.taskDefinitionKey || '',
      formKey: task?.formKey || '',
      todoPage: task?.todoPage || '',
      donePage: task?.donePage || '',
      applicant: task?.applicant || '',
      status: task?.leaveStatus || '',
      comment: task?.variables?.comment || '',
      days: task?.variables?.days || '',
      reason: task?.variables?.reason || '',
      startDate: task?.variables?.startDate || '',
      endDate: task?.variables?.endDate || '',
      deptManager: task?.variables?.deptManager || '',
      generalManager: task?.variables?.generalManager || ''
    }
  }
}

export function resolveRuntimeDescriptor(task, mode = 'todo') {
  const pageKey = resolveRuntimePageKey(task, mode)
  const pageDefinition = findTaskPageDefinition(pageKey, mode)
  if (pageDefinition) {
    return {
      kind: pageDefinition.kind,
      title: pageDefinition.label,
      description: pageDefinition.description
    }
  }

  const formDefinition = findTaskFormDefinition(task?.formKey)
  if (formDefinition?.kind === 'leave') {
    return {
      kind: 'leave',
      title: mode === 'done' ? '请假完成页' : '请假办理页',
      description: mode === 'done' ? '展示请假节点处理完成后的业务回执。' : '按流程设计器配置进入请假业务办理页。'
    }
  }

  return {
    kind: 'generic',
    title: mode === 'done' ? '任务完成页' : '通用办理页',
    description: mode === 'done' ? '当前节点未配置专用完成页，展示通用回执信息。' : '当前节点未配置专用业务页，展示通用运行时办理界面。'
  }
}
