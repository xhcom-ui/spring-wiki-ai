const { createApp } = Vue

createApp({
    data() {
        return {
            memory: {
                content: '',
                category: '',
                importance: 3
            },
            scope: 'user_1', // 默认作用域
            inputObservation: '用户操作', // 默认输入观测
            memories: [],
            events: [],
            policies: [],
            editingMemory: false,
            categoryFilter: '',
            importanceFilter: 0
        }
    },
    mounted() {
        this.loadMemories();
        this.loadPolicies();
    },
    computed: {
        filteredMemories() {
            let result = this.memories;
            
            // 按类别过滤
            if (this.categoryFilter) {
                result = result.filter(memory => 
                    memory.category.toLowerCase().includes(this.categoryFilter.toLowerCase())
                );
            }
            
            // 按重要性过滤
            if (this.importanceFilter > 0) {
                result = result.filter(memory => 
                    memory.importance >= this.importanceFilter
                );
            }
            
            return result;
        }
    },
    methods: {
        // 加载所有记忆
        async loadMemories() {
            try {
                const response = await fetch(`http://localhost:8080/api/memories?scope=${this.scope}`);
                if (response.ok) {
                    this.memories = await response.json();
                } else {
                    console.error('Failed to load memories');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            }
        },
        
        // 加载事件
        async loadEvents() {
            try {
                const response = await fetch(`http://localhost:8080/api/memories/events/scope/${this.scope}`);
                if (response.ok) {
                    this.events = await response.json();
                } else {
                    console.error('Failed to load events');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            }
        },
        

        
        // 加载策略
        async loadPolicies() {
            try {
                const response = await fetch('http://localhost:8080/api/memories/policies');
                if (response.ok) {
                    this.policies = await response.json();
                } else {
                    console.error('Failed to load policies');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            }
        },
        
        // 保存记忆
        async saveMemory() {
            if (!this.memory.content || !this.memory.category) {
                alert('请输入记忆内容和类别');
                return;
            }
            
            try {
                let response;
                const memoryRequest = {
                    memory: this.memory,
                    scope: this.scope,
                    inputObservation: this.inputObservation
                };
                
                if (this.editingMemory) {
                    response = await fetch(`http://localhost:8080/api/memories/${this.memory.id}`, {
                        method: 'PUT',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(memoryRequest)
                    });
                } else {
                    response = await fetch('http://localhost:8080/api/memories', {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json'
                        },
                        body: JSON.stringify(memoryRequest)
                    });
                }
                
                if (response.ok) {
                    alert(this.editingMemory ? '记忆更新成功' : '记忆创建成功');
                    this.resetForm();
                    this.loadMemories();
                    this.loadEvents();
                } else {
                    alert('操作失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            }
        },
        
        // 编辑记忆
        editMemory(memory) {
            this.memory = { ...memory };
            this.editingMemory = true;
        },
        
        // 取消编辑
        cancelEdit() {
            this.resetForm();
        },
        
        // 重置表单
        resetForm() {
            this.memory = {
                content: '',
                category: '',
                importance: 3
            };
            this.editingMemory = false;
        },
        
        // 删除记忆
        async deleteMemory(id) {
            if (confirm('确定要删除这个记忆吗？')) {
                try {
                    const response = await fetch(`http://localhost:8080/api/memories/${id}?scope=${this.scope}&inputObservation=${encodeURIComponent(this.inputObservation)}`, {
                        method: 'DELETE'
                    });
                    
                    if (response.ok) {
                        alert('记忆删除成功');
                        this.loadMemories();
                        this.loadEvents();
                    } else {
                        alert('删除失败');
                    }
                } catch (error) {
                    console.error('Error:', error);
                    alert('连接后端失败，请确保后端服务已启动');
                }
            }
        }
    }
}).mount('#app')
