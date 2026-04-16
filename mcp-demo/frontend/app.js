const { createApp } = Vue

createApp({
    data() {
        return {
            models: [],
            selectedModel: '',
            sessionId: '',
            message: '',
            messages: [],
            loading: false
        }
    },
    mounted() {
        this.loadModels();
    },
    methods: {
        // 加载模型列表
        async loadModels() {
            try {
                const response = await fetch('http://localhost:8080/api/mcp/models');
                if (response.ok) {
                    const result = await response.json();
                    if (result.models) {
                        this.models = result.models;
                        if (this.models.length > 0) {
                            this.selectedModel = this.models[0].id;
                        }
                    } else if (result.error) {
                        alert('加载模型失败: ' + result.error);
                    }
                } else {
                    alert('连接后端失败，请确保后端服务已启动');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            }
        },
        
        // 创建会话
        async createSession() {
            if (!this.selectedModel) {
                alert('请选择一个模型');
                return;
            }
            
            this.loading = true;
            try {
                const response = await fetch('http://localhost:8080/api/mcp/session', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        model: this.selectedModel
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    if (result.session_id) {
                        this.sessionId = result.session_id;
                        this.messages = [];
                        alert('会话创建成功');
                    } else if (result.error) {
                        alert('创建会话失败: ' + result.error);
                    }
                } else {
                    alert('连接后端失败，请确保后端服务已启动');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            } finally {
                this.loading = false;
            }
        },
        
        // 关闭会话
        async closeSession() {
            if (!this.sessionId) {
                alert('没有活跃的会话');
                return;
            }
            
            this.loading = true;
            try {
                const response = await fetch('http://localhost:8080/api/mcp/session', {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        session_id: this.sessionId
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    if (result.success) {
                        this.sessionId = '';
                        this.messages = [];
                        alert('会话关闭成功');
                    } else if (result.error) {
                        alert('关闭会话失败: ' + result.error);
                    }
                } else {
                    alert('连接后端失败，请确保后端服务已启动');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            } finally {
                this.loading = false;
            }
        },
        
        // 发送消息
        async sendMessage() {
            if (!this.sessionId || !this.message) {
                alert('请先创建会话并输入消息');
                return;
            }
            
            // 添加用户消息到聊天记录
            this.messages.push({
                role: 'user',
                content: this.message
            });
            
            const message = this.message;
            this.message = '';
            this.loading = true;
            
            try {
                // 发送消息
                const sendResponse = await fetch('http://localhost:8080/api/mcp/message', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        session_id: this.sessionId,
                        message: message
                    })
                });
                
                if (sendResponse.ok) {
                    const sendResult = await sendResponse.json();
                    if (sendResult.success) {
                        // 获取响应
                        const responseResponse = await fetch('http://localhost:8080/api/mcp/response', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                session_id: this.sessionId
                            })
                        });
                        
                        if (responseResponse.ok) {
                            const responseResult = await responseResponse.json();
                            if (responseResult.response) {
                                // 添加助手消息到聊天记录
                                this.messages.push({
                                    role: 'assistant',
                                    content: responseResult.response
                                });
                                // 滚动到底部
                                this.scrollToBottom();
                            } else if (responseResult.error) {
                                alert('获取响应失败: ' + responseResult.error);
                            }
                        } else {
                            alert('连接后端失败，请确保后端服务已启动');
                        }
                    } else if (sendResult.error) {
                        alert('发送消息失败: ' + sendResult.error);
                    }
                } else {
                    alert('连接后端失败，请确保后端服务已启动');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            } finally {
                this.loading = false;
            }
        },
        
        // 滚动到底部
        scrollToBottom() {
            setTimeout(() => {
                const chatMessages = document.querySelector('.chat-messages');
                if (chatMessages) {
                    chatMessages.scrollTop = chatMessages.scrollHeight;
                }
            }, 100);
        }
    }
}).mount('#app')
