const { createApp } = Vue

createApp({
    data() {
        return {
            tokenType: '',
            tokenCount: 0,
            tokens: [],
            stats: {
                totalTokens: 0,
                totalCost: 0,
                usageCount: 0
            }
        }
    },
    mounted() {
        this.loadTokenUsage();
        this.loadStats();
    },
    methods: {
        // 记录 Token 使用
        async recordTokenUsage() {
            if (!this.tokenType || this.tokenCount <= 0) {
                alert('请输入有效的 Token 类型和数量');
                return;
            }
            
            try {
                const response = await fetch('http://localhost:8080/api/tokens/record', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        type: this.tokenType,
                        count: this.tokenCount
                    })
                });
                
                if (response.ok) {
                    alert('Token 使用记录成功');
                    this.tokenType = '';
                    this.tokenCount = 0;
                    this.loadTokenUsage();
                    this.loadStats();
                } else {
                    alert('记录失败');
                }
            } catch (error) {
                console.error('Error:', error);
                alert('连接后端失败，请确保后端服务已启动');
            }
        },
        
        // 加载 Token 使用记录
        async loadTokenUsage() {
            try {
                const response = await fetch('http://localhost:8080/api/tokens/all');
                if (response.ok) {
                    this.tokens = await response.json();
                } else {
                    console.error('Failed to load token usage');
                }
            } catch (error) {
                console.error('Error:', error);
            }
        },
        
        // 加载统计信息
        async loadStats() {
            try {
                const response = await fetch('http://localhost:8080/api/tokens/stats');
                if (response.ok) {
                    this.stats = await response.json();
                } else {
                    console.error('Failed to load stats');
                }
            } catch (error) {
                console.error('Error:', error);
            }
        }
    }
}).mount('#app')
