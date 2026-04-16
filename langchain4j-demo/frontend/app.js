const { createApp } = Vue

createApp({
    data() {
        return {
            // 导航
            currentTab: 'chat',
            tabs: [
                { id: 'chat', name: '简单聊天' },
                { id: 'memory', name: '记忆聊天' },
                { id: 'documents', name: '文档问答' },
                { id: 'analyze', name: '文本分析' },
                { id: 'summarize', name: '文本摘要' },
                { id: 'translate', name: '文本翻译' },
                { id: 'code', name: '代码生成' },
                { id: 'template', name: 'Prompt模板' }
            ],
            
            // 加载状态
            loading: false,
            
            // 简单聊天
            simpleChatMessages: [],
            simpleChatInput: '',
            
            // 记忆聊天
            sessionId: '',
            memoryChatMessages: [],
            memoryChatInput: '',
            
            // 文档问答
            documentContent: '',
            documentChatMessages: [],
            documentChatInput: '',
            
            // 文本分析
            analysisType: '情感',
            analysisText: '',
            analysisResult: '',
            
            // 文本摘要
            summarizeText: '',
            maxLength: 100,
            summarizeResult: '',
            
            // 文本翻译
            targetLanguage: '英语',
            translateText: '',
            translateResult: '',
            
            // 代码生成
            codeLanguage: 'Java',
            codeDescription: '',
            codeResult: '',
            
            // Prompt Template
            templateText: '',
            templateVariables: {},
            newVariableName: '',
            templateResult: ''
        }
    },
    methods: {
        // 简单聊天
        async sendSimpleChat() {
            if (!this.simpleChatInput.trim()) return;
            
            this.loading = true;
            const message = this.simpleChatInput;
            this.simpleChatInput = '';
            
            this.simpleChatMessages.push({ role: 'user', content: message });
            
            try {
                const response = await fetch('http://localhost:8080/api/langchain4j/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.simpleChatMessages.push({ role: 'assistant', content: result.response });
                    this.scrollToBottom();
                } else {
                    alert('请求失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接失败，请确保后端服务已启动');
            } finally {
                this.loading = false;
            }
        },
        
        // 带记忆的聊天
        async sendMemoryChat() {
            if (!this.memoryChatInput.trim()) return;
            
            this.loading = true;
            const message = this.memoryChatInput;
            this.memoryChatInput = '';
            
            this.memoryChatMessages.push({ role: 'user', content: message });
            
            try {
                const response = await fetch('http://localhost:8080/api/langchain4j/chat/memory', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sessionId: this.sessionId, message })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.sessionId = result.sessionId;
                    this.memoryChatMessages.push({ role: 'assistant', content: result.response });
                    this.scrollToBottom();
                } else {
                    alert('请求失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接失败，请确保后端服务已启动');
            } finally {
                this.loading = false;
            }
        },
        
        // 清除记忆
        async clearMemory() {
            if (!this.sessionId) return;
            
            try {
                const response = await fetch('http://localhost:8080/api/langchain4j/chat/clear', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ sessionId: this.sessionId })
                });
                
                if (response.ok) {
                    this.memoryChatMessages = [];
                    this.sessionId = '';
                    alert('记忆已清除');
                }
            } catch (error) {
                console.error('Error:', error);
            }
        },
        
        // 添加文档
        async addDocument() {
            if (!this.documentContent.trim()) return;
            
            this.loading = true;
            
            try {
                const response = await fetch('http://localhost:8080/api/langchain4j/documents', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ content: this.documentContent })
                });
                
                if (response.ok) {
                    this.documentContent = '';
                    alert('文档添加成功');
                } else {
                    alert('添加失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接失败');
            } finally {
                this.loading = false;
            }
        },
        
        // 基于文档的问答
        async sendDocumentChat() {
            if (!this.documentChatInput.trim()) return;
            
            this.loading = true;
            const message = this.documentChatInput;
            this.documentChatInput = '';
            
            this.documentChatMessages.push({ role: 'user', content: message });
            
            try {
                const response = await fetch('http://localhost:8080/api/langchain4j/chat/documents', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.documentChatMessages.push({ role: 'assistant', content: result.response });
                    this.scrollToBottom();
                } else {
                    alert('请求失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接失败');
            } finally {
                this.loading = false;
            }
        },
        
        // 文本分析
        async analyzeText() {
            if (!this.analysisText.trim()) return;
            
            this.loading = true;
            
            try {
                const response = await fetch('http://localhost:8080/api/langchain4j/analyze', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ 
                        text: this.analysisText, 
                        type: this.analysisType 
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.analysisResult = result.response;
                } else {
                    alert('分析失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接失败');
            } finally {
                this.loading = false;
            }
        },
        
        // 文本摘要
        async summarize() {
            if (!this.summarizeText.trim()) return;
            
            this.loading = true;
            
            try {
                const response = await fetch('http://localhost:8080/api/langchain4j/summarize', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ 
                        text: this.summarizeText, 
                        maxLength: this.maxLength 
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.summarizeResult = result.response;
                } else {
                    alert('摘要失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接失败');
            } finally {
                this.loading = false;
            }
        },
        
        // 文本翻译
        async translate() {
            if (!this.translateText.trim()) return;
            
            this.loading = true;
            
            try {
                const response = await fetch('http://localhost:8080/api/langchain4j/translate', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ 
                        text: this.translateText, 
                        language: this.targetLanguage 
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.translateResult = result.response;
                } else {
                    alert('翻译失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接失败');
            } finally {
                this.loading = false;
            }
        },
        
        // 代码生成
        async generateCode() {
            if (!this.codeDescription.trim()) return;
            
            this.loading = true;
            
            try {
                const response = await fetch('http://localhost:8080/api/langchain4j/generate-code', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ 
                        description: this.codeDescription, 
                        language: this.codeLanguage 
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.codeResult = result.response;
                } else {
                    alert('代码生成失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接失败');
            } finally {
                this.loading = false;
            }
        },
        
        // 添加变量
        addVariable() {
            if (this.newVariableName.trim()) {
                this.templateVariables[this.newVariableName] = '';
                this.newVariableName = '';
            }
        },
        
        // 使用 Prompt Template 生成
        async generateWithTemplate() {
            if (!this.templateText.trim()) return;
            
            this.loading = true;
            
            try {
                const response = await fetch('http://localhost:8080/api/langchain4j/template', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ 
                        template: this.templateText, 
                        variables: this.templateVariables 
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.templateResult = result.response;
                } else {
                    alert('生成失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接失败');
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
