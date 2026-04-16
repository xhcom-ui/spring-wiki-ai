const { createApp } = Vue

createApp({
    data() {
        return {
            // 当前菜单
            currentMenu: 'knowledge',
            
            // 菜单项
            menuItems: [
                { id: 'home', name: '首页', icon: '🏠' },
                { id: 'log', name: '日志分析', icon: '📊' },
                { id: 'knowledge', name: '知识库', icon: '📚' }
            ],
            
            // 服务地址
            serviceUrl: 'http://localhost:8080',
            
            // 文件上传
            selectedFile: null,
            selectedFileName: '',
            uploading: false,
            uploadMessage: null,
            
            // 知识库列表
            knowledgeBases: [],
            
            // 内容查看弹窗
            showContentModal: false,
            currentFile: null,
            
            // 统计信息
            stats: {
                totalFiles: 0,
                totalSize: 0
            }
        }
    },
    
    mounted() {
        this.loadKnowledgeBases();
    },
    
    methods: {
        // 处理文件选择
        handleFileChange(event) {
            const file = event.target.files[0];
            if (file) {
                this.selectedFile = file;
                this.selectedFileName = file.name;
                this.uploadMessage = null;
            }
        },
        
        // 上传文件
        async uploadFile() {
            if (!this.selectedFile) {
                this.showMessage('请先选择文件', 'error');
                return;
            }
            
            this.uploading = true;
            this.uploadMessage = null;
            
            const formData = new FormData();
            formData.append('file', this.selectedFile);
            formData.append('serviceUrl', this.serviceUrl);
            
            try {
                const response = await fetch(`${this.serviceUrl}/api/knowledge/upload`, {
                    method: 'POST',
                    body: formData
                });
                
                const result = await response.json();
                
                if (result.success) {
                    this.showMessage('文件上传成功', 'success');
                    this.clearForm();
                    this.loadKnowledgeBases();
                } else {
                    this.showMessage(result.message || '上传失败', 'error');
                }
            } catch (error) {
                console.error('Upload error:', error);
                this.showMessage('上传失败: ' + error.message, 'error');
            } finally {
                this.uploading = false;
            }
        },
        
        // 清空表单
        clearForm() {
            this.selectedFile = null;
            this.selectedFileName = '';
            this.uploadMessage = null;
            if (this.$refs.fileInput) {
                this.$refs.fileInput.value = '';
            }
        },
        
        // 显示消息
        showMessage(text, type) {
            this.uploadMessage = { text, type };
            setTimeout(() => {
                this.uploadMessage = null;
            }, 5000);
        },
        
        // 加载知识库列表
        async loadKnowledgeBases() {
            try {
                const response = await fetch(`${this.serviceUrl}/api/knowledge/list`);
                const result = await response.json();
                
                if (result.success) {
                    this.knowledgeBases = result.data;
                    this.updateStats();
                }
            } catch (error) {
                console.error('Load knowledge bases error:', error);
            }
        },
        
        // 更新统计信息
        updateStats() {
            this.stats.totalFiles = this.knowledgeBases.length;
            const totalBytes = this.knowledgeBases.reduce((sum, item) => sum + (item.fileSize || 0), 0);
            this.stats.totalSize = (totalBytes / (1024 * 1024)).toFixed(2);
        },
        
        // 删除文件
        async deleteFile(id) {
            if (!confirm('确定要删除这个文件吗？')) {
                return;
            }
            
            try {
                const response = await fetch(`${this.serviceUrl}/api/knowledge/${id}`, {
                    method: 'DELETE'
                });
                
                const result = await response.json();
                
                if (result.success) {
                    this.showMessage('删除成功', 'success');
                    this.loadKnowledgeBases();
                } else {
                    this.showMessage(result.message || '删除失败', 'error');
                }
            } catch (error) {
                console.error('Delete error:', error);
                this.showMessage('删除失败: ' + error.message, 'error');
            }
        },
        
        // 查看文件内容
        viewContent(item) {
            this.currentFile = item;
            this.showContentModal = true;
        },
        
        // 格式化文件大小
        formatFileSize(bytes) {
            if (bytes === 0) return '0 Bytes';
            const k = 1024;
            const sizes = ['Bytes', 'KB', 'MB', 'GB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
        }
    }
}).mount('#app')
