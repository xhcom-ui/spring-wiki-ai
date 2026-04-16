// store/modules/sse.js
export default {
    namespaced: true,

    state: {
        messages: [],
        unreadCount: 0,
        connectionStatus: 'disconnected'
    },

    mutations: {
        ADD_MESSAGE(state, message) {
            state.messages.push(message)
            state.unreadCount++
        },

        MARK_AS_READ(state) {
            state.unreadCount = 0
        },

        SET_CONNECTION_STATUS(state, status) {
            state.connectionStatus = status
        }
    },

    actions: {
        // 在组件中处理消息后，提交到store
        handleMessage({ commit }, message) {
            commit('ADD_MESSAGE', message)

            // 根据消息类型分发到不同模块
            switch (message.type) {
                case 'ORDER_UPDATE':
                    commit('order/UPDATE_ORDER', message.content, { root: true })
                    break
                case 'APM_ALERT':
                    commit('monitor/ADD_ALERT', message.content, { root: true })
                    break
            }
        }
    }
}