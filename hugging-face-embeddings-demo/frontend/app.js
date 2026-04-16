const { createApp } = Vue

createApp({
    data() {
        return {
            // 导航
            currentTab: 'single',
            tabs: [
                { id: 'single', name: '单个文本嵌入' },
                { id: 'batch', name: '批量文本嵌入' },
                { id: 'similarity', name: '文本相似度' },
                { id: 'most-similar', name: '最相似文本' },
                { id: 'similar', name: '相似文本列表' }
            ],
            
            // 加载状态
            loading: false,
            
            // 单个文本嵌入
            singleText: '',
            singleEmbedding: null,
            
            // 批量文本嵌入
            batchTexts: '',
            batchEmbeddings: null,
            
            // 文本相似度
            text1: '',
            text2: '',
            similarityResult: null,
            
            // 最相似文本
            queryText: '',
            candidateTexts: '',
            mostSimilarResult: null,
            
            // 相似文本列表
            similarityQuery: '',
            similarityCandidates: '',
            similarityThreshold: 0.7,
            similarResult: null
        }
    },
    methods: {
        // 获取单个文本嵌入
        async getSingleEmbedding() {
            if (!this.singleText.trim()) return;
            
            this.loading = true;
            
            try {
                const response = await fetch('http://localhost:8080/api/embedding/single', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ text: this.singleText })
                });
                
                if (response.ok) {
                    this.singleEmbedding = await response.json();
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
        
        // 批量获取文本嵌入
        async getBatchEmbeddings() {
            if (!this.batchTexts.trim()) return;
            
            this.loading = true;
            
            const texts = this.batchTexts.split('\n').filter(text => text.trim() !== '');
            
            try {
                const response = await fetch('http://localhost:8080/api/embedding/batch', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ texts })
                });
                
                if (response.ok) {
                    this.batchEmbeddings = await response.json();
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
        
        // 计算文本相似度
        async calculateSimilarity() {
            if (!this.text1.trim() || !this.text2.trim()) return;
            
            this.loading = true;
            
            try {
                const response = await fetch('http://localhost:8080/api/embedding/similarity', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ text1: this.text1, text2: this.text2 })
                });
                
                if (response.ok) {
                    this.similarityResult = await response.json();
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
        
        // 查找最相似文本
        async findMostSimilar() {
            if (!this.queryText.trim() || !this.candidateTexts.trim()) return;
            
            this.loading = true;
            
            const texts = this.candidateTexts.split('\n').filter(text => text.trim() !== '');
            
            try {
                const response = await fetch('http://localhost:8080/api/embedding/most-similar', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ query: this.queryText, texts })
                });
                
                if (response.ok) {
                    this.mostSimilarResult = await response.json();
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
        
        // 查找相似文本列表
        async findSimilar() {
            if (!this.similarityQuery.trim() || !this.similarityCandidates.trim()) return;
            
            this.loading = true;
            
            const texts = this.similarityCandidates.split('\n').filter(text => text.trim() !== '');
            
            try {
                const response = await fetch('http://localhost:8080/api/embedding/similar', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ 
                        query: this.similarityQuery, 
                        texts, 
                        threshold: this.similarityThreshold 
                    })
                });
                
                if (response.ok) {
                    this.similarResult = await response.json();
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
        
        // 获取相似度解释
        getSimilarityExplanation(similarity) {
            if (similarity >= 0.9) {
                return '极高相似度，两个文本几乎相同';
            } else if (similarity >= 0.8) {
                return '高相似度，两个文本内容非常相似';
            } else if (similarity >= 0.7) {
                return '中等相似度，两个文本内容有较多相似之处';
            } else if (similarity >= 0.6) {
                return '较低相似度，两个文本内容有一定相似性';
            } else {
                return '低相似度，两个文本内容差异较大';
            }
        }
    }
}).mount('#app')
