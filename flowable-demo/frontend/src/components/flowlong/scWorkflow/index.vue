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
  width: 100%;
  min-height: 100%;
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
    width: 240px;
    min-height: 88px;
    flex-shrink: 0;
    background: rgba(255, 255, 255, 0.94);
    border: 1px solid rgba(129, 146, 173, 0.14);
    border-radius: 20px;
    cursor: pointer;
    box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  }
  .node-wrap-box::before {
    content: '';
    position: absolute;
    top: -12px;
    left: 50%;
    transform: translateX(-50%);
    width: 0px;
    border-style: solid;
    border-width: 8px 6px 4px;
    border-color: rgb(202, 202, 202) transparent transparent;
  }
  .node-wrap-box.start-node:before {
    content: none;
  }
  .node-wrap-box .title {
    min-height: 34px;
    color: #fff;
    padding: 10px 16px;
    padding-right: 34px;
    border-radius: 20px 20px 0 0;
    position: relative;
    display: flex;
    align-items: center;
    font-weight: 600;
  }
  .node-wrap-box .title .icon {
    margin-right: 5px;
  }
  .node-wrap-box .title .close {
    width: 28px;
    height: 28px;
    font-size: 15px;
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    right: 10px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    border-radius: 999px;
    opacity: 0;
    pointer-events: none;
    color: rgba(255, 255, 255, 0.88);
    transition: opacity 0.18s ease, background-color 0.18s ease;
  }
  .node-wrap-box .content {
    position: relative;
    padding: 16px;
    color: #1f2937;
    line-height: 1.55;
  }
  .node-wrap-box .content .placeholder {
    color: #94a3b8;
  }
  .node-wrap-box:hover .close {
    opacity: 1;
    pointer-events: auto;
  }
  .node-wrap-box .title .close:hover {
    background: rgba(255, 255, 255, 0.16);
  }
  .add-node-btn-box {
    width: 260px;
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
    background-color: rgb(202, 202, 202);
  }
  .add-node-btn {
    user-select: none;
    width: 260px;
    padding: 22px 0px 34px;
    display: flex;
    justify-content: center;
    flex-shrink: 0;
    flex-grow: 1;
  }
  .add-node-btn span {
  }
  .add-branch {
    justify-content: center;
    padding: 0px 10px;
    position: absolute;
    top: -16px;
    left: 50%;
    transform: translateX(-50%);
    transform-origin: center center;
    z-index: 1;
    display: inline-flex;
    align-items: center;
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
    border-bottom: 2px solid rgba(148, 163, 184, 0.34);
    border-top: 2px solid rgba(148, 163, 184, 0.34);
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
    background-color: rgb(202, 202, 202);
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
    background-color: rgb(202, 202, 202);
  }
  .auto-judge {
    position: relative;
    width: 240px;
    min-height: 88px;
    background: rgba(255, 255, 255, 0.94);
    border: 1px solid rgba(129, 146, 173, 0.14);
    border-radius: 20px;
    padding: 16px 18px;
    cursor: pointer;
    box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  }
  .auto-judge::before {
    content: '';
    position: absolute;
    top: -12px;
    left: 50%;
    transform: translateX(-50%);
    width: 0px;
    border-style: solid;
    border-width: 8px 6px 4px;
    border-color: rgb(202, 202, 202) transparent transparent;
    background: rgb(239, 239, 239);
  }
  .auto-judge .title {
    display: flex;
    align-items: center;
    gap: 8px;
    min-height: 28px;
    line-height: 16px;
  }
  .auto-judge .title .node-title {
    color: #15bc83;
    font-weight: 700;
  }
  .auto-judge .title .priority-title {
    margin-left: auto;
    color: #64748b;
    font-size: 12px;
    font-weight: 600;
    transition: opacity 0.18s ease;
  }
  .auto-judge .branch-action {
    margin-left: auto;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    border: 0;
    border-radius: 999px;
    background: rgba(148, 163, 184, 0.12);
    color: #475569;
    cursor: pointer;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.18s ease, transform 0.18s ease, background-color 0.18s ease;
  }
  .auto-judge .branch-action:hover {
    background: rgba(239, 68, 68, 0.12);
    color: #dc2626;
    transform: scale(1.04);
  }
  .auto-judge .content {
    position: relative;
    padding-top: 14px;
    color: #1f2937;
    line-height: 1.6;
  }
  .auto-judge .content .placeholder {
    color: #999;
  }
  .auto-judge:hover {
    .branch-action {
      opacity: 1;
      pointer-events: auto;
    }
  }
  .auto-judge:hover .priority-title,
  .auto-judge:focus-within .priority-title {
    opacity: 0.45;
  }
  .top-left-cover-line,
  .top-right-cover-line {
    position: absolute;
    height: 3px;
    width: 50%;
    background-color: #efefef;
    top: -2px;
  }
  .bottom-left-cover-line,
  .bottom-right-cover-line {
    position: absolute;
    height: 3px;
    width: 50%;
    background-color: #efefef;
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
    color: rgba(25, 31, 37, 0.4);
    text-align: left;
  }
  .end-node-circle {
    width: 14px;
    height: 14px;
    margin: auto;
    border-radius: 50%;
    background: linear-gradient(135deg, #94a3b8, #64748b);
  }
  .end-node-text {
    margin-top: 5px;
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
    background: rgba(148, 163, 184, 0.12);
    color: #0f172a;
    border-radius: 12px;
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
    border-radius: 20px;
    transition: all 0.1s;
  }
  .auto-judge:hover:after {
    border: 1px solid #3296fa;
    box-shadow: 0 0 6px 0 rgba(50, 150, 250, 0.3);
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
    border-radius: 20px;
    transition: all 0.1s;
  }
  .node-wrap-box:hover:after {
    border: 1px solid #3296fa;
    box-shadow: 0 0 6px 0 rgba(50, 150, 250, 0.3);
  }
}

.tags-list {
  margin-top: 15px;
  width: 100%;
}
.add-node-popover-body {
}
.add-node-popover-body li {
  display: inline-block;
  width: 80px;
  text-align: center;
  padding: 10px 0;
}
.add-node-popover-body li i {
  border: 1px solid var(--el-border-color-light);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  text-align: center;
  line-height: 38px;
  font-size: 18px;
  cursor: pointer;
}
.add-node-popover-body li i:hover {
  border: 1px solid #3296fa;
  background: #3296fa;
  color: #fff !important;
}
.add-node-popover-body li p {
  font-size: 12px;
  margin-top: 5px;
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
