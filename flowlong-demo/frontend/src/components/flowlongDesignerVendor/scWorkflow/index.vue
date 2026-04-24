<!--
 * @Descripttion: 仿钉钉流程设计器
 * @version: 1.3
 * @Author: sakuya
 * @Date: 2021年9月14日08:38:35
 * @LastEditors: sakuya
 * @LastEditTime: 2022年5月14日19:43:46
-->

<template>
  <div class="sc-workflow-design">
    <div class="box-scale">
      <node-wrap
        v-if="nodeConfig"
        v-model="nodeConfig"></node-wrap>
      <div class="end-node">
        <div class="end-node-circle"></div>
        <div class="end-node-text">流程结束</div>
      </div>
    </div>
    <use-select
      v-if="selectVisible"
      ref="useselect"
      @closed="selectVisible = false"></use-select>
  </div>
</template>

<script>
import nodeWrap from './nodeWrap.vue'
import useSelect from './select.vue'

export default {
  provide() {
    return {
      select: this.selectHandle
    }
  },
  props: {
    modelValue: { type: Object, default: () => {} }
  },
  components: {
    nodeWrap,
    useSelect
  },
  data() {
    return {
      nodeConfig: this.modelValue,
      selectVisible: false
    }
  },
  watch: {
    modelValue(val) {
      this.nodeConfig = val
    },
    nodeConfig(val) {
      this.$emit('update:modelValue', val)
    }
  },
  mounted() {},
  methods: {
    selectHandle(type, data) {
      this.selectVisible = true
      this.$nextTick(() => {
        this.$refs.useselect.open(type, data)
      })
    }
  }
}
</script>

<style lang="scss">
.sc-workflow-design {
  --workflow-line-color: #d5deea;
  --workflow-card-border: #dce5ef;
  --workflow-card-shadow: 0 8px 18px rgba(24, 39, 58, 0.06);
  --workflow-card-hover-shadow: 0 10px 24px rgba(50, 105, 185, 0.12);
  --workflow-stage-fill: #f8fbff;
  width: 100%;
}
.sc-workflow-design .box-scale {
  display: inline-block;
  position: relative;
  width: 100%;
  align-items: flex-start;
  justify-content: center;
  flex-wrap: wrap;
  min-width: min-content;
}

.sc-workflow-design {
  .node-wrap {
    display: inline-flex;
    width: 100%;
    flex-flow: column wrap;
    justify-content: flex-start;
    align-items: center;
    padding: 0px 0px;
    position: relative;
    z-index: 1;
  }
  .node-wrap-box {
    display: inline-flex;
    flex-direction: column;
    position: relative;
    width: 244px;
    min-height: 78px;
    flex-shrink: 0;
    background: #fff;
    border: 1px solid var(--workflow-card-border);
    border-radius: 8px;
    cursor: pointer;
    box-shadow: var(--workflow-card-shadow);
  }
  .node-wrap-box::before {
    content: '';
    position: absolute;
    top: -13px;
    left: 50%;
    transform: translateX(-50%);
    width: 0px;
    border-style: solid;
    border-width: 9px 7px 4px;
    border-color: var(--workflow-line-color) transparent transparent;
  }
  .node-wrap-box.start-node:before {
    content: none;
  }
  .node-wrap-box .title {
    min-height: 34px;
    line-height: 34px;
    color: #fff;
    font-size: 14px;
    font-weight: 600;
    padding-left: 14px;
    padding-right: 34px;
    border-radius: 7px 7px 0 0;
    position: relative;
    display: flex;
    align-items: center;
  }
  .node-wrap-box .title .icon {
    margin-right: 6px;
    font-size: 14px;
  }
  .node-wrap-box .title .close {
    font-size: 14px;
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    right: 11px;
    color: rgba(255, 255, 255, 0.9);
    display: none;
  }
  .node-wrap-box .content {
    position: relative;
    padding: 14px 16px 15px;
    color: #223447;
    font-size: 13px;
    font-weight: 600;
    line-height: 1.55;
    letter-spacing: 0.01em;
  }
  .node-wrap-box .content .placeholder {
    color: #788596;
    font-weight: 600;
  }
  .node-wrap-box:hover .close {
    display: block;
  }
  .add-node-btn-box {
    width: 240px;
    display: inline-flex;
    flex-shrink: 0;
    position: relative;
    z-index: 1;
  }
  .add-node-btn-box:before {
    content: '';
    position: absolute;
    top: 0px;
    left: 0px;
    right: 0px;
    bottom: 0px;
    z-index: -1;
    margin: auto;
    width: 2px;
    height: 100%;
    background-color: var(--workflow-line-color);
  }
  .add-node-btn {
    user-select: none;
    width: 240px;
    padding: 16px 0px 24px;
    display: flex;
    justify-content: center;
    flex-shrink: 0;
    flex-grow: 1;
  }
  .add-node-btn span {
  }
  .add-branch {
    justify-content: center;
    min-height: 28px;
    padding: 0px 12px;
    position: absolute;
    top: -16px;
    left: 50%;
    transform: translateX(-50%);
    transform-origin: center center;
    z-index: 1;
    display: inline-flex;
    align-items: center;
    border-color: #b7cae6;
    color: #315f9a;
    background: #fff;
    box-shadow: 0 4px 10px rgba(24, 39, 58, 0.06);
  }
  .branch-wrap {
    display: inline-flex;
    width: 100%;
  }
  .branch-box-wrap {
    display: flex;
    flex-flow: column wrap;
    align-items: center;
    min-height: 270px;
    width: 100%;
    flex-shrink: 0;
  }
  .col-box {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    position: relative;
  }
  .branch-box {
    display: flex;
    overflow: visible;
    min-height: 180px;
    height: auto;
    border-bottom: 2px solid var(--workflow-line-color);
    border-top: 2px solid var(--workflow-line-color);
    position: relative;
    margin-top: 15px;
  }
  .branch-box .col-box::before {
    content: '';
    position: absolute;
    top: 0px;
    left: 0px;
    right: 0px;
    bottom: 0px;
    z-index: 0;
    margin: auto;
    width: 2px;
    height: 100%;
    background-color: var(--workflow-line-color);
  }
  .condition-node {
    display: inline-flex;
    flex-direction: column;
    min-height: 220px;
  }
  .condition-node-box {
    padding-top: 30px;
    padding-right: 50px;
    padding-left: 50px;
    justify-content: center;
    align-items: center;
    flex-grow: 1;
    position: relative;
    display: inline-flex;
    flex-direction: column;
  }
  .condition-node-box::before {
    content: '';
    position: absolute;
    top: 0px;
    left: 0px;
    right: 0px;
    bottom: 0px;
    margin: auto;
    width: 2px;
    height: 100%;
    background-color: var(--workflow-line-color);
  }
  .auto-judge {
    position: relative;
    width: 244px;
    min-height: 78px;
    background: #fff;
    border: 1px solid var(--workflow-card-border);
    border-radius: 8px;
    padding: 13px 16px 14px;
    cursor: pointer;
    box-shadow: var(--workflow-card-shadow);
  }
  .auto-judge::before {
    content: '';
    position: absolute;
    top: -13px;
    left: 50%;
    transform: translateX(-50%);
    width: 0px;
    border-style: solid;
    border-width: 9px 7px 4px;
    border-color: var(--workflow-line-color) transparent transparent;
  }
  .auto-judge .title {
    line-height: 18px;
    color: #243445;
  }
  .auto-judge .title .node-title {
    color: #13946a;
    font-size: 14px;
    font-weight: 700;
  }
  .auto-judge .title .close {
    font-size: 14px;
    position: absolute;
    top: 13px;
    right: 14px;
    color: #8391a2;
    display: none;
  }
  .auto-judge .title .priority-title {
    position: absolute;
    top: 13px;
    right: 14px;
    color: #8391a2;
    font-size: 12px;
  }
  .auto-judge .content {
    position: relative;
    padding-top: 13px;
    color: #263749;
    font-size: 13px;
    font-weight: 600;
    line-height: 1.55;
  }
  .auto-judge .content .placeholder {
    color: #788596;
    font-weight: 600;
  }
  .auto-judge:hover {
    .close {
      display: block;
    }
    .priority-title {
      display: none;
    }
  }
  .top-left-cover-line,
  .top-right-cover-line {
    position: absolute;
    height: 3px;
    width: 50%;
    background-color: var(--workflow-stage-fill);
    top: -2px;
  }
  .bottom-left-cover-line,
  .bottom-right-cover-line {
    position: absolute;
    height: 3px;
    width: 50%;
    background-color: var(--workflow-stage-fill);
    bottom: -2px;
  }
  .top-left-cover-line {
    left: -1px;
  }
  .top-right-cover-line {
    right: -1px;
  }
  .bottom-left-cover-line {
    left: -1px;
  }
  .bottom-right-cover-line {
    right: -1px;
  }
  .end-node {
    border-radius: 50%;
    font-size: 14px;
    color: rgba(54, 71, 92, 0.66);
    text-align: left;
  }
  .end-node-circle {
    width: 12px;
    height: 12px;
    margin: auto;
    border-radius: 50%;
    background: #bcc8d7;
    box-shadow: 0 0 0 4px rgba(188, 200, 215, 0.18);
  }
  .end-node-text {
    margin-top: 9px;
    text-align: center;
  }
  .auto-judge:hover {
    .sort-left {
      display: flex;
    }
    .sort-right {
      display: flex;
    }
  }
  .auto-judge .sort-left {
    position: absolute;
    top: 0;
    bottom: 0;
    z-index: 1;
    left: 0;
    display: none;
    justify-content: center;
    align-items: center;
    flex-direction: column;
  }
  .auto-judge .sort-right {
    position: absolute;
    top: 0;
    bottom: 0;
    z-index: 1;
    right: 0;
    display: none;
    justify-content: center;
    align-items: center;
    flex-direction: column;
  }
  .auto-judge .sort-left:hover,
  .auto-judge .sort-right:hover {
    background: #f3f7fb;
  }
  .auto-judge:after {
    pointer-events: none;
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 2;
    border-radius: 8px;
    transition: all 0.1s;
  }
  .auto-judge:hover:after {
    border: 1px solid #95b8ef;
    box-shadow: var(--workflow-card-hover-shadow);
  }
  .node-wrap-box:after {
    pointer-events: none;
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    right: 0;
    z-index: 2;
    border-radius: 8px;
    transition: all 0.1s;
  }
  .node-wrap-box:hover:after {
    border: 1px solid #95b8ef;
    box-shadow: var(--workflow-card-hover-shadow);
  }
}

.tags-list {
  margin-top: 15px;
  width: 100%;
}
.node-wrap-drawer__title {
  padding-right: 40px;
}
.node-wrap-drawer__title label {
  cursor: pointer;
}
.node-wrap-drawer__title label:hover {
  border-bottom: 1px dashed #409eff;
}
.node-wrap-drawer__title .node-wrap-drawer__title-edit {
  color: #409eff;
  margin-left: 10px;
  vertical-align: middle;
}

.dark .sc-workflow-design {
  .node-wrap-box,
  .auto-judge {
    background: #2b2b2b;
  }
  .col-box {
    background: var(--el-bg-color);
  }
  .top-left-cover-line,
  .top-right-cover-line,
  .bottom-left-cover-line,
  .bottom-right-cover-line {
    background-color: var(--el-bg-color);
  }
  .node-wrap-box::before,
  .auto-judge::before {
    background-color: var(--el-bg-color);
  }
  .branch-box .add-branch {
    background: var(--el-bg-color);
  }
  .end-node .end-node-text {
    color: #ccc;
  }
  .auto-judge .sort-left:hover,
  .auto-judge .sort-right:hover {
    background: var(--el-bg-color);
  }
}
</style>
