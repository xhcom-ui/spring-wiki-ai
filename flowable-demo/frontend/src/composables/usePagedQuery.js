import { reactive, ref } from 'vue'

export function usePagedQuery(fetcher, options = {}) {
  const items = ref([])
  const loading = ref(false)
  const error = ref('')
  const total = ref(0)
  const page = ref(options.page ?? 1)
  const size = ref(options.size ?? 10)
  const filters = reactive({ ...(options.filters || {}) })

  async function load(overrides = {}) {
    loading.value = true
    error.value = ''
    try {
      const payload = await fetcher({
        page: page.value,
        size: size.value,
        ...filters,
        ...overrides
      })
      items.value = payload?.items || []
      total.value = payload?.total || 0
      page.value = payload?.page || page.value
      size.value = payload?.size || size.value
      return payload
    } catch (requestError) {
      items.value = []
      total.value = 0
      error.value = requestError.normalizedMessage || requestError.message || '加载失败'
      throw requestError
    } finally {
      loading.value = false
    }
  }

  function search(nextFilters = null) {
    if (nextFilters && typeof nextFilters === 'object') {
      Object.assign(filters, nextFilters)
    }
    page.value = 1
    return load()
  }

  function changePage(nextPage) {
    page.value = Math.max(1, Number(nextPage) || 1)
    return load()
  }

  function changeSize(nextSize) {
    size.value = Math.max(1, Number(nextSize) || 1)
    page.value = 1
    return load()
  }

  function reset(nextFilters = {}) {
    Object.keys(filters).forEach((key) => {
      delete filters[key]
    })
    Object.assign(filters, nextFilters)
    page.value = 1
    return load()
  }

  return {
    items,
    loading,
    error,
    total,
    page,
    size,
    filters,
    load,
    search,
    changePage,
    changeSize,
    reset
  }
}
