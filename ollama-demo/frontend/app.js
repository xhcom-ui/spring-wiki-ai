const { createApp } = Vue

createApp({
    data() {
        return {
            models: [],
            selectedModel: '',
            generatePrompt: '',
            generateResponse: '',
            chatMessage: '',
            chatMessages: [],
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
                const response = await fetch('http://localhost:8080/api/ollama/models');
                if (response.ok) {
                    const result = await response.json();
                    if (result.models) {
                        this.models = result.models;
                        if (this.models.length > 0) {
                            this.selectedModel = this.models[0].name;
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
        
        // 生成文本
        async generateText() {
            if (!this.generatePrompt) {
                alert('请输入 prompt');
                return;
            }
            
            this.loading = true;
            try {
                const response = await fetch('http://localhost:8080/api/ollama/generate', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        prompt: this.generatePrompt,
                        model: this.selectedModel
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    if (result.response) {
                        this.generateResponse = result.response;
                    } else if (result.error) {
                        alert('生成失败: ' + result.error);
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
            if (!this.chatMessage) {
                alert('请输入消息');
                return;
            }
            
            // 添加用户消息到聊天记录
            this.chatMessages.push({
                role: 'user',
                content: this.chatMessage
            });
            
            const message = this.chatMessage;
            this.chatMessage = '';
            this.loading = true;
            
            try {
                const response = await fetch('http://localhost:8080/api/ollama/chat', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        message: message,
                        model: this.selectedModel
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    if (result.response) {
                        // 添加助手消息到聊天记录
                        this.chatMessages.push({
                            role: 'assistant',
                            content: result.response
                        });
                        // 滚动到底部
                        this.scrollToBottom();
                    } else if (result.error) {
                        alert('发送失败: ' + result.error);
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
