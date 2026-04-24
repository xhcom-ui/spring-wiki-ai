<template>
  <div class="page-grid">
    <section class="content-grid two-column">
      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Sequential Flow</p>
            <h3>流程编排中心</h3>
          </div>
        </div>

        <form class="form-stack" @submit.prevent="runOrchestration">
          <div class="form-grid">
            <label>
              <span>第一流程</span>
              <select v-model="orchestrationForm.firstProcessKey">
                <option value="">请选择</option>
                <option v-for="definition in activeDefinitions" :key="definition.id" :value="definition.processKey">
                  {{ definition.processName }} ({{ definition.processKey }})
                </option>
              </select>
            </label>
            <label>
              <span>第二流程</span>
              <select v-model="orchestrationForm.secondProcessKey">
                <option value="">请选择</option>
                <option v-for="definition in activeDefinitions" :key="definition.id" :value="definition.processKey">
                  {{ definition.processName }} ({{ definition.processKey }})
                </option>
              </select>
            </label>
            <label>
              <span>第三流程</span>
              <select v-model="orchestrationForm.thirdProcessKey">
                <option value="">请选择</option>
                <option v-for="definition in activeDefinitions" :key="definition.id" :value="definition.processKey">
                  {{ definition.processName }} ({{ definition.processKey }})
                </option>
              </select>
            </label>
          </div>

          <label>
            <span>共享变量 JSON</span>
            <textarea v-model="orchestrationForm.variablesJson" rows="8" placeholder='{"applicant":"admin","deptManager":"manager","generalManager":"boss"}'></textarea>
          </label>

          <div class="action-row">
            <button type="submit" class="primary-btn" :disabled="loading">执行串行编排</button>
          </div>
        </form>
      </article>

      <article class="panel-card">
        <div class="panel-head">
          <div>
            <p class="eyebrow">Callback / Signal</p>
            <h3>回调与信号控制</h3>
          </div>
        </div>

        <form class="form-stack" @submit.prevent="startWithCallback">
          <div class="form-grid">
            <label>
              <span>主流程</span>
              <select v-model="callbackForm.processKey">
                <option value="">请选择</option>
                <option v-for="definition in activeDefinitions" :key="definition.id" :value="definition.processKey">
                  {{ definition.processName }} ({{ definition.processKey }})
                </option>
              </select>
            </label>
            <label>
              <span>回调流程</span>
              <select v-model="callbackForm.callbackProcessKey">
                <option value="">请选择</option>
                <option v-for="definition in activeDefinitions" :key="definition.id" :value="definition.processKey">
                  {{ definition.processName }} ({{ definition.processKey }})
                </option>
              </select>
            </label>
          </div>
          <label>
            <span>变量 JSON</span>
            <textarea v-model="callbackForm.variablesJson" rows="5"></textarea>
          </label>
          <div class="action-row">
            <button type="submit" class="secondary-btn" :disabled="loading">启动带回调流程</button>
          </div>
        </form>

        <form class="form-stack" @submit.prevent="signalProcess">
          <div class="form-grid">
            <label>
              <span>流程实例 ID</span>
              <input v-model.trim="signalForm.processInstanceId" type="text" />
            </label>
            <label>
              <span>信号名称</span>
              <input v-model.trim="signalForm.signalName" type="text" placeholder="resumeFlow" />
            </label>
          </div>
          <label>
            <span>信号变量 JSON</span>
            <textarea v-model="signalForm.variablesJson" rows="4"></textarea>
          </label>
          <div class="action-row">
            <button type="submit" class="secondary-btn" :disabled="loading">发送信号</button>
          </div>
        </form>

        <p v-if="statusMessage" class="feedback success">{{ statusMessage }}</p>
        <p v-if="errorMessage" class="feedback error">{{ errorMessage }}</p>
      </article>
    </section>

    <section class="panel-card">
      <div class="panel-head">
        <div>
          <p class="eyebrow">Execution Result</p>
          <h3>执行结果</h3>
        </div>
      </div>
      <pre class="code-block">{{ executionResult }}</pre>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { http } from '../api/http'

const loading = ref(false)
const activeDefinitions = ref([])
const executionResult = ref('等待执行')
const statusMessage = ref('')
const errorMessage = ref('')

const orchestrationForm = reactive({
  firstProcessKey: '',
  secondProcessKey: '',
  thirdProcessKey: '',
  variablesJson: '{"applicant":"admin","deptManager":"manager","generalManager":"boss"}'
})

const callbackForm = reactive({
  processKey: '',
  callbackProcessKey: '',
  variablesJson: '{"applicant":"admin"}'
})

const signalForm = reactive({
  processInstanceId: '',
  signalName: '',
  variablesJson: '{}'
})

function resetMessages() {
  statusMessage.value = ''
  errorMessage.value = ''
}

function parseVariables(text) {
  if (!text || !text.trim()) {
    return {}
  }
  return JSON.parse(text)
}

async function fetchDefinitions() {
  const { data } = await http.get('/process-definition/active')
  activeDefinitions.value = data || []
}

async function runOrchestration() {
  resetMessages()
  loading.value = true
  try {
    const vars = parseVariables(orchestrationForm.variablesJson)
    const payload = {
      firstProcessKey: orchestrationForm.firstProcessKey || null,
      secondProcessKey: orchestrationForm.secondProcessKey || null,
      thirdProcessKey: orchestrationForm.thirdProcessKey || null,
      firstProcessVariables: vars,
      secondProcessVariables: vars,
      thirdProcessVariables: vars
    }
    const { data } = await http.post('/orchestration/orchestrate', payload)
    executionResult.value = JSON.stringify(data, null, 2)
    statusMessage.value = '流程编排已执行'
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message
  } finally {
    loading.value = false
  }
}

async function startWithCallback() {
  resetMessages()
  loading.value = true
  try {
    const payload = {
      processKey: callbackForm.processKey,
      callbackProcessKey: callbackForm.callbackProcessKey,
      variables: parseVariables(callbackForm.variablesJson)
    }
    const { data } = await http.post('/orchestration/start-with-callback', payload)
    executionResult.value = JSON.stringify(data, null, 2)
    statusMessage.value = '带回调流程已启动'
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message
  } finally {
    loading.value = false
  }
}

async function signalProcess() {
  resetMessages()
  loading.value = true
  try {
    const payload = {
      processInstanceId: signalForm.processInstanceId,
      signalName: signalForm.signalName,
      variables: parseVariables(signalForm.variablesJson)
    }
    const { data } = await http.post('/orchestration/signal', payload)
    executionResult.value = JSON.stringify(data, null, 2)
    statusMessage.value = '流程信号已发送'
  } catch (error) {
    errorMessage.value = error.normalizedMessage || error.message
  } finally {
    loading.value = false
  }
}

onMounted(fetchDefinitions)
</script>
