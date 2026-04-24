import { http } from '../../api/http'

function buildUserTreeRows(users) {
  return (users || []).map((user) => ({
    id: user.username,
    name: user.nickname || user.username,
    username: user.username,
    status: user.status
  }))
}

function buildRoleTreeRows(roles) {
  return (roles || []).map((role) => ({
    id: role.code,
    label: role.name,
    name: role.name,
    code: role.code
  }))
}

export default {
  successCode: 200,
  group: {
    apiObj: {
      async get() {
        return {
          code: 200,
          data: [
            {
              id: 'all',
              label: '全部人员',
              children: []
            }
          ],
          msg: ''
        }
      }
    },
    parseData(res) {
      return {
        rows: res.data || [],
        msg: res.msg || '',
        code: res.code || 200
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

        const rows = buildUserTreeRows(data).filter((item) => {
          if (!keyword) {
            return true
          }
          return item.name.toLowerCase().includes(keyword) || item.username.toLowerCase().includes(keyword)
        })
        const start = Math.max(0, (page - 1) * pageSize)
        const end = start + pageSize

        return {
          code: 200,
          data: {
            total: rows.length,
            rows: rows.slice(start, end)
          },
          msg: ''
        }
      }
    },
    pageSize: 20,
    parseData(res) {
      return {
        rows: res.data?.rows || [],
        total: res.data?.total || 0,
        msg: res.msg || '',
        code: res.code || 200
      }
    },
    props: {
      key: 'id',
      label: 'name'
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
          data: buildRoleTreeRows(data),
          msg: ''
        }
      }
    },
    parseData(res) {
      return {
        rows: res.data || [],
        msg: res.msg || '',
        code: res.code || 200
      }
    },
    props: {
      key: 'id',
      label: 'label',
      children: 'children'
    }
  }
}
