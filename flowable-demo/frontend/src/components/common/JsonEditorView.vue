<template>
  <div class="json-editor-shell" :class="{ fullscreen: isFullScreen }">
    <div ref="editorRef" class="json-editor-view"></div>
    <button
      type="button"
      class="json-editor-fullscreen"
      :class="{ visible: mountedReady }"
      @click="toggleFullScreen"
    ></button>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import JSONEditorModule from 'jsoneditor'
import 'jsoneditor/dist/jsoneditor.min.css'

const JSONEditor = JSONEditorModule?.default || JSONEditorModule

const props = defineProps({
  modelValue: {
    type: [Object, Array, String, Number, Boolean, null],
    default: null
  },
  mode: {
    type: String,
    default: 'view'
  },
  modes: {
    type: Array,
    default: () => ['tree', 'code', 'form', 'text', 'view']
  },
  language: {
    type: String,
    default: 'zh-CN'
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const editorRef = ref(null)
const mountedReady = ref(false)
const isFullScreen = ref(false)
let editor = null
let silentUpdate = false

function buildOptions() {
  return {
    mode: props.mode,
    modes: props.modes,
    language: props.language,
    indentation: 2,
    onChange() {
      if (!editor) {
        return
      }
      const text = editor.getText()
      if (!text) {
        emit('update:modelValue', '')
        emit('change', '')
        return
      }
      try {
        const json = editor.get()
        emit('update:modelValue', json)
        emit('change', json)
      } catch {
      }
    }
  }
}

function setEditorValue(value) {
  if (!editor) {
    return
  }
  silentUpdate = true
  try {
    if (value === undefined || value === null || value === '') {
      editor.setText('')
    } else {
      editor.set(value)
    }
  } catch {
    editor.setText(typeof value === 'string' ? value : JSON.stringify(value ?? '', null, 2))
  }
  nextTick(() => {
    silentUpdate = false
    if (editor?.getMode && ['tree', 'view', 'form'].includes(editor.getMode())) {
      editor.expandAll?.()
    }
  })
}

function toggleFullScreen() {
  isFullScreen.value = !isFullScreen.value
  nextTick(() => {
    window.dispatchEvent(new Event('resize'))
  })
}

watch(
  () => props.modelValue,
  (value) => {
    if (silentUpdate || !editor) {
      return
    }
    setEditorValue(value)
  },
  { deep: true }
)

onMounted(() => {
  editor = new JSONEditor(editorRef.value, buildOptions(), props.modelValue)
  mountedReady.value = true
})

onBeforeUnmount(() => {
  editor?.destroy?.()
  editor = null
})
</script>

<style scoped>
.json-editor-shell {
  position: relative;
  width: 100%;
  height: 100%;
}

.json-editor-shell.fullscreen {
  position: fixed;
  inset: 0;
  z-index: 999999;
  background: #fff;
}

.json-editor-view {
  width: 100%;
  height: 100%;
}

.json-editor-fullscreen {
  position: absolute;
  top: 8px;
  right: 110px;
  display: none;
  width: 22px;
  height: 22px;
  border: 0;
  border-radius: 2px;
  cursor: pointer;
  background-size: cover;
  background-color: transparent;
  background-image: url("data:image/svg+xml,%3Csvg t='1635416254060' viewBox='0 0 1024 1024' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M240.8 196l178.4 178.4-45.6 45.6-177.6-179.2-68 68V128h180.8l-68 68z m133.6 408.8L196 783.2 128 715.2V896h180.8l-68-68 178.4-178.4-44.8-44.8zM715.2 128l68 68-178.4 178.4 45.6 45.6 178.4-178.4 68 68V128H715.2z m-65.6 476.8l-45.6 45.6 178.4 178.4-68 68H896V715.2l-68 68-178.4-178.4z' fill='%23ffffff'/%3E%3C/svg%3E");
}

.json-editor-shell.fullscreen .json-editor-fullscreen {
  background-image: url("data:image/svg+xml,%3Csvg t='1635416420969' viewBox='0 0 1024 1024' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M142.4 96.8l-44.8 44.8 173.6 174.4-68 68H384V203.2l-67.2 67.2zM752.8 316l173.6-174.4-44.8-44.8-174.4 173.6-67.2-67.2V384h180.8zM270.4 707.2l-169.6 170.4 44.8 49.6 170.4-174.4 68 68V640H203.2zM820.8 640H640v180.8l68-68 170.4 174.4 44.8-49.6-169.6-170.4z' fill='%23ffffff'/%3E%3C/svg%3E");
}

.json-editor-fullscreen.visible {
  display: block;
}

.json-editor-fullscreen:hover {
  border: 1px solid #d7e6fe;
}
</style>
