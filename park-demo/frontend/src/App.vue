<template>
  <div class="app">
    <el-container>
      <el-header height="60px">
        <div class="header">
          <h1>智慧停车诱导系统</h1>
        </div>
      </el-header>
      <el-container>
        <el-aside width="200px" class="aside">
          <el-menu
            :default-active="activeIndex"
            class="el-menu-vertical-demo"
            router
          >
            <el-menu-item index="/">
              <el-icon><House /></el-icon>
              <span>首页</span>
            </el-menu-item>
            <el-menu-item index="/parking-lots">
              <el-icon><OfficeBuilding /></el-icon>
              <span>停车场管理</span>
            </el-menu-item>
            <el-menu-item index="/parking-spots">
              <el-icon><Position /></el-icon>
              <span>车位管理</span>
            </el-menu-item>
            <el-menu-item index="/areas">
              <el-icon><Grid /></el-icon>
              <span>区域管理</span>
            </el-menu-item>
            <el-menu-item index="/sensors">
              <el-icon><Monitor /></el-icon>
              <span>传感器管理</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main>
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script>
import { House, OfficeBuilding, Position, Grid, Monitor } from '@element-plus/icons-vue'

export default {
  name: 'App',
  components: {
    House,
    OfficeBuilding,
    Position,
    Grid,
    Monitor
  },
  computed: {
    activeIndex() {
      return this.$route.path
    }
  }
}
</script>

<style>
.app {
  height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background-color: #409EFF;
  color: white;
}

.header h1 {
  margin: 0;
  font-size: 24px;
}

.aside {
  background-color: #f0f2f5;
}

.el-menu-vertical-demo:not(.el-menu--collapse) {
  width: 200px;
}
</style>
