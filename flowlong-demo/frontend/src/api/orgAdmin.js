import { http } from './http'

function normalizePageResult(data) {
  return {
    items: Array.isArray(data?.items) ? data.items : [],
    total: Number(data?.total || 0),
    page: Number(data?.page || 1),
    size: Number(data?.size || 10)
  }
}

export async function fetchDepartmentLookup() {
  const { data } = await http.get('/departments/lookup')
  return Array.isArray(data) ? data : []
}

export async function queryDepartments(params = {}) {
  const { data } = await http.get('/departments/query', { params })
  return normalizePageResult(data)
}

export async function createDepartment(payload) {
  const { data } = await http.post('/departments', payload)
  return data
}

export async function updateDepartment(id, payload) {
  const { data } = await http.put(`/departments/${id}`, payload)
  return data
}

export async function deleteDepartment(id) {
  const { data } = await http.delete(`/departments/${id}`)
  return data
}

export async function fetchPostLookup() {
  const { data } = await http.get('/posts/lookup')
  return Array.isArray(data) ? data : []
}

export async function queryPosts(params = {}) {
  const { data } = await http.get('/posts/query', { params })
  return normalizePageResult(data)
}

export async function createPost(payload) {
  const { data } = await http.post('/posts', payload)
  return data
}

export async function updatePost(id, payload) {
  const { data } = await http.put(`/posts/${id}`, payload)
  return data
}

export async function deletePost(id) {
  const { data } = await http.delete(`/posts/${id}`)
  return data
}

export async function fetchOrgSummary() {
  const [departments, posts] = await Promise.all([fetchDepartmentLookup(), fetchPostLookup()])
  return {
    departmentCount: departments.length,
    postCount: posts.length
  }
}
