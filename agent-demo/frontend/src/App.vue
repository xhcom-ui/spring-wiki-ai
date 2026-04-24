<template>
    <div class="container">
        <header class="header">
            <h1>🤖 手搓最小 Agent</h1>
            <p class="subtitle">按文章思路实现的 Agent Loop 演示</p>
        </header>

        <div class="content">
            <section class="config-section">
                <h3>配置</h3>
                <div class="config-grid">
                    <div class="config-item">
                        <label>工作区路径</label>
                        <input v-model="workspace" type="text" placeholder="./workspace">
                    </div>
                    <div class="config-item">
                        <label>模型</label>
                        <select v-model="model">
                            <option value="gpt-4o">GPT-4o</option>
                            <option value="gpt-3.5-turbo">GPT-3.5 Turbo</option>
                        </select>
                    </div>
                    <div class="config-item">
                        <label>最大步数</label>
                        <input v-model.number="maxSteps" type="number" min="1" max="20">
                    </div>
                </div>
            </section>

            <section class="input-section">
                <h3>任务描述</h3>
                <textarea
                    v-model="task"
                    placeholder="描述你要让 Agent 做什么...
例如：
- 列出当前目录下的所有文件
- 读取 README.md 的内容
- 查看项目结构"
                    rows="4"
                ></textarea>
                <button
                    @click="runAgent"
                    :disabled="loading || !task.trim()"
                    class="submit-btn"
                >
                    {{ loading ? '运行中...' : '🚀 运行 Agent' }}
                </button>
            </section>

            <div v-if="result" class="result-section">
                <h3>结果</h3>
                <div class="answer-box">
                    <h4>💡 Agent 回答</h4>
                    <div class="answer-content">{{ result.answer }}</div>
                </div>
                
                <div class="stats">
                    <span>步数: {{ result.steps }}</span>
                    <span>工具调用: {{ result.tool_calls }}</span>
                </div>

                <div class="logs-box">
                    <h4>📋 执行日志</h4>
                    <div class="logs">
                        <div v-for="log in result.logs" :key="log.step" class="log-item">
                            <span class="log-step">[Step {{ log.step }}]</span>
                            <span class="log-message">{{ log.message }}</span>
                            <pre v-if="log.data" class="log-data">{{ JSON.stringify(log.data, null, 2) }}</pre>
                        </div>
                    </div>
                </div>
            </div>

            <div v-if="error" class="error-box">
                <h4>❌ 错误</h4>
                <p>{{ error }}</p>
            </div>

            <section class="tools-section">
                <h3>可用工具</h3>
                <div class="tools-grid">
                    <div class="tool-card">
                        <div class="tool-name">📂 list_files</div>
                        <p>列出目录文件</p>
                    </div>
                    <div class="tool-card">
                        <div class="tool-name">📖 read_file</div>
                        <p>读取文件内容</p>
                    </div>
                    <div class="tool-card">
                        <div class="tool-name">⚡ run_command</div>
                        <p>运行白名单命令</p>
                    </div>
                </div>
            </section>

            <section class="concepts-section">
                <h3>核心概念</h3>
                <div class="concepts-grid">
                    <div class="concept-card">
                        <h4>🔄 Agent Loop</h4>
                        <p>读任务 → 选工具 → 执行 → 喂回结果 → 继续</p>
                    </div>
                    <div class="concept-card">
                        <h4>🛡️ Harness</h4>
                        <p>边界控制：步数限制、输出裁剪、日志</p>
                    </div>
                    <div class="concept-card">
                        <h4>🔧 Tools</h4>
                        <p>路径边界、命令白名单、安全限制</p>
                    </div>
                </div>
            </section>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const task = ref('')
const workspace = ref('./workspace')
const model = ref('gpt-4o')
const maxSteps = ref(8)
const loading = ref(false)
const result = ref(null)
const error = ref('')

async function runAgent() {
    if (!task.value.trim()) return
    
    loading.value = true
    error.value = ''
    result.value = null
    
    try {
        const response = await axios.post('/api/agent/run', {
            task: task.value,
            workspace: workspace.value,
            model: model.value,
            maxSteps: maxSteps.value
        })
        
        result.value = response.data
    } catch (err) {
        error.value = err.response?.data?.detail || err.message || '发生未知错误'
    } finally {
        loading.value = false
    }
}
</script>

<style scoped>
.container {
    background: white;
    border-radius: 16px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    overflow: hidden;
}

.header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 40px;
    text-align: center;
}

.header h1 {
    font-size: 2.5rem;
    margin-bottom: 10px;
}

.subtitle {
    opacity: 0.9;
    font-size: 1.1rem;
}

.content {
    padding: 40px;
}

section {
    margin-bottom: 30px;
}

h3 {
    color: #333;
    margin-bottom: 15px;
    font-size: 1.3rem;
}

.config-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
}

.config-item label {
    display: block;
    margin-bottom: 8px;
    color: #555;
    font-weight: 500;
}

.config-item input,
.config-item select {
    width: 100%;
    padding: 12px;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    font-size: 1rem;
    transition: border-color 0.2s;
}

.config-item input:focus,
.config-item select:focus {
    outline: none;
    border-color: #667eea;
}

textarea {
    width: 100%;
    padding: 16px;
    border: 2px solid #e0e0e0;
    border-radius: 8px;
    font-size: 1rem;
    font-family: inherit;
    resize: vertical;
    transition: border-color 0.2s;
}

textarea:focus {
    outline: none;
    border-color: #667eea;
}

.submit-btn {
    width: 100%;
    margin-top: 15px;
    padding: 16px 32px;
    font-size: 1.1rem;
    font-weight: 600;
    color: white;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
    border-radius: 8px;
    cursor: pointer;
    transition: transform 0.2s, box-shadow 0.2s;
}

.submit-btn:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
}

.submit-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.result-section {
    margin-top: 30px;
}

.answer-box {
    background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 20px;
}

.answer-box h4 {
    color: #667eea;
    margin-bottom: 12px;
}

.answer-content {
    line-height: 1.8;
    color: #333;
    white-space: pre-wrap;
}

.stats {
    display: flex;
    gap: 20px;
    margin-bottom: 20px;
    padding: 12px 20px;
    background: #f5f7fa;
    border-radius: 8px;
}

.stats span {
    color: #667eea;
    font-weight: 600;
}

.logs-box {
    background: #1a1a2e;
    border-radius: 12px;
    padding: 20px;
}

.logs-box h4 {
    color: #a5b4fc;
    margin-bottom: 12px;
}

.logs {
    max-height: 400px;
    overflow-y: auto;
}

.log-item {
    padding: 10px 0;
    border-bottom: 1px solid #2d2d4a;
}

.log-item:last-child {
    border-bottom: none;
}

.log-step {
    color: #a5b4fc;
    font-weight: 600;
    margin-right: 10px;
}

.log-message {
    color: #e4e4e7;
}

.log-data {
    margin-top: 8px;
    color: #888;
    font-size: 0.85rem;
    background: #0f0f1a;
    padding: 10px;
    border-radius: 6px;
    overflow-x: auto;
}

.error-box {
    background: #fef2f2;
    border: 2px solid #fca5a5;
    border-radius: 12px;
    padding: 20px;
    margin-top: 20px;
}

.error-box h4 {
    color: #dc2626;
    margin-bottom: 10px;
}

.error-box p {
    color: #7f1d1d;
}

.tools-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 20px;
}

.tool-card {
    background: #f8fafc;
    border: 2px solid #e2e8f0;
    border-radius: 12px;
    padding: 20px;
    transition: all 0.2s;
}

.tool-card:hover {
    border-color: #667eea;
    transform: translateY(-2px);
}

.tool-name {
    font-weight: 600;
    color: #667eea;
    margin-bottom: 8px;
    font-size: 1.1rem;
}

.tool-card p {
    color: #666;
    font-size: 0.95rem;
}

.concepts-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 20px;
}

.concept-card {
    background: linear-gradient(135deg, #f0f4ff 0%, #f3e8ff 100%);
    border-radius: 12px;
    padding: 24px;
}

.concept-card h4 {
    color: #667eea;
    margin-bottom: 10px;
    font-size: 1.1rem;
}

.concept-card p {
    color: #555;
    line-height: 1.6;
}
</style>
