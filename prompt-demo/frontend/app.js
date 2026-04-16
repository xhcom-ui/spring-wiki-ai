const { createApp } = Vue

createApp({
    data() {
        return {
            prompt: {
                name: '',
                content: '',
                description: ''
            },
            prompts: [],
            editingPrompt: false,
            selectedPromptId: '',
            variables: [],
            generatedText: ''
        }
    },
    mounted() {
        this.loadPrompts();
    },
    computed: {
        selectedPrompt() {
            return this.prompts.find(p => p.id == this.selectedPromptId);
        }
    },
    methods: {
        // 加载所有 Prompt 模板
        async loadPrompts() {
            try {
                const response = await fetch('http://localhost:8080/api/prompts');
                if (response.ok) {
                    this.prompts = await response.json();
                } else {
                    console.error('Failed to load prompts');
                }
            } catch (error) {
                console.error('Error:', error);
            }
        },
        
        // 保存 Prompt 模板
        async savePrompt() {
            if (!this.prompt.name || !this.prompt.content) {
                alert('请输入 Prompt 名称和内容');
                return;
            }
            
            try {
                let response;
                if (this.editingPrompt) {
                    response = await fetch(`http://localhost:8080/api/prompts/${this.prompt.id}`, {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(this.prompt)
                    });
                } else {
                    response = await fetch('http://localhost:8080/api/prompts', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(this.prompt)
                    });
                }
                
                if (response.ok) {
                    alert(this.editingPrompt ? 'Prompt 更新成功' : 'Prompt 创建成功');
                    this.resetForm();
                    this.loadPrompts();
                } else {
                    alert('操作失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            }
        },
        
        // 编辑 Prompt 模板
        editPrompt(prompt) {
            this.prompt = { ...prompt };
            this.editingPrompt = true;
        },
        
        // 取消编辑
        cancelEdit() {
            this.resetForm();
        },
        
        // 重置表单
        resetForm() {
            this.prompt = {
                name: '',
                content: '',
                description: ''
            };
            this.editingPrompt = false;
        },
        
        // 删除 Prompt 模板
        async deletePrompt(id) {
            if (confirm('确定要删除这个 Prompt 模板吗？')) {
                try {
                    const response = await fetch(`http://localhost:8080/api/prompts/${id}`, {
                        method: 'DELETE'
                    });
                    
                    if (response.ok) {
                        alert('Prompt 删除成功');
                        this.loadPrompts();
                    } else {
                        alert('删除失败');
                    }
                } catch (error) {
                    console.error('Error:', error);
                    alert('连接后端失败，请确保后端服务已启动');
                }
            }
        },
        
        // 查看 Prompt 内容
        showPromptContent(prompt) {
            alert(`Prompt 内容：\n${prompt.content}`);
        },
        
        // 添加变量
        addVariable() {
            this.variables.push('');
        },
        
        // 生成 Prompt 文本
        async generatePrompt() {
            if (!this.selectedPrompt) {
                alert('请选择一个 Prompt 模板');
                return;
            }
            
            try {
                const response = await fetch('http://localhost:8080/api/prompts/generate', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        content: this.selectedPrompt.content,
                        variables: this.variables
                    })
                });
                
                if (response.ok) {
                    const result = await response.json();
                    this.generatedText = result.generatedText;
                } else {
                    alert('生成失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            }
        }
    }
}).mount('#app')
