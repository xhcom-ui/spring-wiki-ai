<template>
  <div class="container">
    <h1>PageIndex 演示</h1>
    
    <!-- 文件上传部分 -->
    <section class="upload-section">
      <h2>上传文档</h2>
      <input type="file" ref="fileInput" @change="handleFileChange" accept=".pdf,.doc,.docx,.txt" />
      <button @click="uploadFile" :disabled="!selectedFile">上传并创建索引</button>
      <div v-if="uploadMessage" class="message">{{ uploadMessage }}</div>
    </section>
    
    <!-- 索引列表部分 -->
    <section class="index-section">
      <h2>索引列表</h2>
      <button @click="listIndexes">刷新索引列表</button>
      <ul v-if="indexes.length > 0">
        <li v-for="index_id in indexes" :key="index_id">
          {{ index_id }}
          <button @click="selectIndex(index_id)">选择</button>
          <button @click="deleteIndex(index_id)">删除</button>
        </li>
      </ul>
      <div v-else class="message">暂无索引</div>
    </section>
    
    <!-- 查询部分 -->
    <section class="query-section">
      <h2>查询索引</h2>
      <div v-if="selectedIndex">
        <p>当前索引: {{ selectedIndex }}</p>
        <input type="text" v-model="query" placeholder="输入查询内容" />
        <button @click="performQuery" :disabled="!query">查询</button>
      </div>
      <div v-else class="message">请先选择一个索引</div>
    </section>
    
    <!-- 结果显示部分 -->
    <section class="results-section" v-if="results.length > 0">
      <h2>查询结果</h2>
      <div v-for="(result, index) in results" :key="index" class="result-item">
        <h3>{{ result.title }}</h3>
        <p>{{ result.content }}</p>
        <p class="score">相关性: {{ result.score }}</p>
      </div>
    </section>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      selectedFile: null,
      uploadMessage: '',
      indexes: [],
      selectedIndex: null,
      query: '',
      results: []
    }
  },
  methods: {
    handleFileChange(event) {
      this.selectedFile = event.target.files[0]
    },
    async uploadFile() {
      if (!this.selectedFile) return
      
      const formData = new FormData()
      formData.append('file', this.selectedFile)
      
      try {
        const response = await fetch('http://localhost:8000/api/index/create', {
          method: 'POST',
          body: formData
        })
        
        if (response.ok) {
          const data = await response.json()
          this.uploadMessage = `索引创建成功，ID: ${data.index_id}`
          await this.listIndexes()
        } else {
          const error = await response.json()
          this.uploadMessage = `上传失败: ${error.detail}`
        }
      } catch (error) {
        this.uploadMessage = `上传失败: ${error.message}`
      }
    },
    async listIndexes() {
      try {
        const response = await fetch('http://localhost:8000/api/index/list')
        if (response.ok) {
          const data = await response.json()
          this.indexes = data.indexes
        } else {
          console.error('获取索引列表失败')
        }
      } catch (error) {
        console.error('获取索引列表失败:', error)
      }
    },
    selectIndex(index_id) {
      this.selectedIndex = index_id
      this.results = []
    },
    async deleteIndex(index_id) {
      try {
        const response = await fetch(`http://localhost:8000/api/index/${index_id}`, {
          method: 'DELETE'
        })
        if (response.ok) {
          await this.listIndexes()
          if (this.selectedIndex === index_id) {
            this.selectedIndex = null
            this.results = []
          }
        } else {
          console.error('删除索引失败')
        }
      } catch (error) {
        console.error('删除索引失败:', error)
      }
    },
    async performQuery() {
      if (!this.selectedIndex || !this.query) return
      
      try {
        const response = await fetch('http://localhost:8000/api/index/query', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({
            index_id: this.selectedIndex,
            query: this.query
          })
        })
        
        if (response.ok) {
          const data = await response.json()
          this.results = data.results
        } else {
          const error = await response.json()
          console.error('查询失败:', error.detail)
        }
      } catch (error) {
        console.error('查询失败:', error)
      }
    }
  },
  mounted() {
    this.listIndexes()
  }
}
</script>

<style>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: Arial, sans-serif;
}

h1 {
  text-align: center;
  color: #333;
}

section {
  margin: 30px 0;
  padding: 20px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background-color: #f9f9f9;
}

h2 {
  color: #555;
  margin-bottom: 15px;
}

input[type="file"], input[type="text"] {
  margin: 10px 0;
  padding: 8px;
  width: 100%;
  max-width: 400px;
}

button {
  margin: 5px;
  padding: 8px 16px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:hover {
  background-color: #45a049;
}

button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  margin: 10px 0;
  padding: 10px;
  background-color: #fff;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-item {
  margin: 10px 0;
  padding: 15px;
  background-color: #fff;
  border-radius: 4px;
  border-left: 4px solid #4CAF50;
}

.score {
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

.message {
  margin: 10px 0;
  padding: 10px;
  background-color: #e3f2fd;
  border-radius: 4px;
  color: #1976d2;
}
</style>
