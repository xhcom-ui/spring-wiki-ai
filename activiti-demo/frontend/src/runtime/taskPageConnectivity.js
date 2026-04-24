import router from '../router'
import { normalizeCatalogKey } from './taskPageCatalog'

function buildPageLookup(allItems) {
  return new Map(
    (Array.isArray(allItems) ? allItems : [])
      .filter((item) => item?.itemType === 'PAGE')
      .map((item) => [normalizeCatalogKey(item.itemKey), item])
  )
}

function resolveRuntimeRoute(item) {
  return router.resolve({
    name: 'task-runtime',
    params: {
      pageKey: normalizeCatalogKey(item?.itemKey) || 'generic-task'
    },
    query: {
      mode: item?.pageMode === 'done' ? 'done' : 'todo'
    }
  })
}

function validatePageItem(item) {
  const issues = []
  if (!item?.itemKey) {
    issues.push('页面目录缺少目录 Key')
  }
  if (!item?.pageMode) {
    issues.push('页面目录缺少页面模式')
  }

  const resolved = resolveRuntimeRoute(item)
  if (!resolved?.matched?.length) {
    issues.push('前端没有匹配到 task-runtime 路由')
  }

  return {
    ok: issues.length === 0,
    issues,
    href: resolved?.href || '',
    routeName: resolved?.name || 'task-runtime'
  }
}

export function validateCatalogItemConnectivity(item, allItems) {
  if (!item) {
    return {
      ok: false,
      issues: ['目录项不存在'],
      href: ''
    }
  }

  if (item.itemType === 'PAGE') {
    return validatePageItem(item)
  }

  const pageLookup = buildPageLookup(allItems)
  const todoPage = pageLookup.get(normalizeCatalogKey(item.defaultTodoPage))
  const donePage = pageLookup.get(normalizeCatalogKey(item.defaultDonePage))
  const issues = []

  if (!todoPage) {
    issues.push('默认待办页未在页面目录中注册')
  } else if (todoPage.pageMode !== 'todo') {
    issues.push('默认待办页引用的不是 todo 页面')
  } else {
    issues.push(...validatePageItem(todoPage).issues.map((message) => `待办页: ${message}`))
  }

  if (!donePage) {
    issues.push('默认完成页未在页面目录中注册')
  } else if (donePage.pageMode !== 'done') {
    issues.push('默认完成页引用的不是 done 页面')
  } else {
    issues.push(...validatePageItem(donePage).issues.map((message) => `完成页: ${message}`))
  }

  return {
    ok: issues.length === 0,
    issues,
    todoHref: todoPage ? resolveRuntimeRoute(todoPage).href : '',
    doneHref: donePage ? resolveRuntimeRoute(donePage).href : '',
    href: todoPage ? resolveRuntimeRoute(todoPage).href : ''
  }
}
