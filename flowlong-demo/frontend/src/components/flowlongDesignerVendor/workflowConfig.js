import { http } from '../../api/http'

function createAllGroup() {
  return [{
    id: '',
    label: '全部成员',
    children: []
  }]
}

function toUserRow(item) {
  const username = String(item?.username || '').trim()
  const nickname = String(item?.nickname || '').trim()
  return {
    id: username,
    user: nickname ? `${nickname} (${username})` : username
  }
}

function toRoleRow(item) {
  const code = String(item?.code || '').trim()
  const name = String(item?.name || '').trim()
  return {
    id: code,
    label: name ? `${name} (${code})` : code
  }
}

export default {
  successCode: 200,
  group: {
    apiObj: {
      async get() {
        return {
          code: 200,
          data: createAllGroup(),
          message: ''
        }
      }
    },
    parseData(res) {
      return {
        rows: Array.isArray(res?.data) ? res.data : createAllGroup(),
        msg: res?.message || '',
        code: res?.code || 200
      }
    },
    props: {
      key: 'id',
      label: 'label',
      children: 'children'
    }
  },
  user: {
    apiObj: {
      async get(params = {}) {
        const { data } = await http.get('/users/lookup')
        const keyword = String(params.keyword || '').trim().toLowerCase()
        const page = Number(params.page || 1)
        const pageSize = Number(params.pageSize || 20)
        const rows = (Array.isArray(data) ? data : [])
          .filter((item) => item?.status === 1)
          .map(toUserRow)
          .filter((item) => !keyword || item.user.toLowerCase().includes(keyword) || item.id.toLowerCase().includes(keyword))
        const start = (page - 1) * pageSize
        return {
          code: 200,
          data: {
            total: rows.length,
            rows: rows.slice(start, start + pageSize)
          },
          message: ''
        }
      }
    },
    pageSize: 20,
    parseData(res) {
      return {
        rows: Array.isArray(res?.data?.rows) ? res.data.rows : [],
        total: Number(res?.data?.total || 0),
        msg: res?.message || '',
        code: res?.code || 200
      }
    },
    props: {
      key: 'id',
      label: 'user'
    },
    request: {
      page: 'page',
      pageSize: 'pageSize',
      groupId: 'groupId',
      keyword: 'keyword'
    }
  },
  role: {
    apiObj: {
      async get() {
        const { data } = await http.get('/roles/options')
        return {
          code: 200,
          data: (Array.isArray(data) ? data : [])
            .filter((item) => item?.status === 1)
            .map(toRoleRow),
          message: ''
        }
      }
    },
    parseData(res) {
      return {
        rows: Array.isArray(res?.data) ? res.data : [],
        msg: res?.message || '',
        code: res?.code || 200
      }
    },
    props: {
      key: 'id',
      label: 'label',
      children: 'children'
    }
  }
}
