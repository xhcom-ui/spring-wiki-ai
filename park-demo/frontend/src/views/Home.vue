<template>
  <div class="home">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>停车场概览</span>
            </div>
          </template>
          <div class="overview-item">
            <div class="overview-label">停车场数量</div>
            <div class="overview-value">{{ parkingLotCount }}</div>
          </div>
          <div class="overview-item">
            <div class="overview-label">总车位数</div>
            <div class="overview-value">{{ totalSpots }}</div>
          </div>
          <div class="overview-item">
            <div class="overview-label">空闲车位数</div>
            <div class="overview-value">{{ freeSpots }}</div>
          </div>
          <div class="overview-item">
            <div class="overview-label">占用率</div>
            <div class="overview-value">{{ occupancyRate }}%</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>传感器状态</span>
            </div>
          </template>
          <div class="overview-item">
            <div class="overview-label">传感器总数</div>
            <div class="overview-value">{{ sensorCount }}</div>
          </div>
          <div class="overview-item">
            <div class="overview-label">在线传感器</div>
            <div class="overview-value">{{ onlineSensors }}</div>
          </div>
          <div class="overview-item">
            <div class="overview-label">离线传感器</div>
            <div class="overview-value">{{ offlineSensors }}</div>
          </div>
          <div class="overview-item">
            <div class="overview-label">在线率</div>
            <div class="overview-value">{{ onlineRate }}%</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>系统状态</span>
            </div>
          </template>
          <div class="overview-item">
            <div class="overview-label">系统状态</div>
            <div class="overview-value status-ok">正常</div>
          </div>
          <div class="overview-item">
            <div class="overview-label">Kafka状态</div>
            <div class="overview-value status-ok">正常</div>
          </div>
          <div class="overview-item">
            <div class="overview-label">Redis状态</div>
            <div class="overview-value status-ok">正常</div>
          </div>
          <div class="overview-item">
            <div class="overview-label">MQTT状态</div>
            <div class="overview-value status-ok">正常</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>车位状态实时更新</span>
            </div>
          </template>
          <div class="status-list">
            <div v-for="(status, index) in spotStatuses" :key="index" class="status-item">
              <div class="status-info">
                <span class="status-spot">{{ status.spotId }}</span>
                <span class="status-parking">{{ status.parkingId }}</span>
              </div>
              <div class="status-badge" :class="status.status === 0 ? 'status-free' : 'status-occupied'">
                {{ status.status === 0 ? '空闲' : '占用' }}
              </div>
            </div>
            <div v-if="spotStatuses.length === 0" class="empty-status">
              暂无实时数据
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>区域聚合数据</span>
            </div>
          </template>
          <div class="aggregation-list">
            <div v-for="(aggregation, index) in areaAggregations" :key="index" class="aggregation-item">
              <div class="aggregation-info">
                <span class="aggregation-area">{{ aggregation.areaId }}</span>
                <span class="aggregation-parking">{{ aggregation.parkingId }}</span>
              </div>
              <div class="aggregation-stats">
                <span class="aggregation-free">{{ aggregation.freeSpots }} 空闲</span>
                <span class="aggregation-total">/ {{ aggregation.totalSpots }} 总</span>
              </div>
            </div>
            <div v-if="areaAggregations.length === 0" class="empty-status">
              暂无聚合数据
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { parkingLotApi, sensorApi } from '../api'

export default {
  name: 'Home',
  data() {
    return {
      parkingLotCount: 0,
      totalSpots: 0,
      freeSpots: 0,
      occupancyRate: 0,
      sensorCount: 0,
      onlineSensors: 0,
      offlineSensors: 0,
      onlineRate: 0,
      spotStatuses: [],
      areaAggregations: []
    }
  },
  mounted() {
    this.loadOverviewData()
    this.initWebSocket()
  },
  methods: {
    async loadOverviewData() {
      try {
        // 加载停车场数据
        const parkingLotsResponse = await parkingLotApi.getAll()
        this.parkingLotCount = parkingLotsResponse.data.length
        this.totalSpots = parkingLotsResponse.data.reduce((sum, lot) => sum + lot.totalSpots, 0)
        // 这里简化处理，实际项目中应该从Redis获取实时数据
        this.freeSpots = Math.floor(this.totalSpots * 0.7)
        this.occupancyRate = Math.round((this.totalSpots - this.freeSpots) / this.totalSpots * 100)

        // 加载传感器数据
        const sensorsResponse = await sensorApi.getAll()
        this.sensorCount = sensorsResponse.data.length
        this.onlineSensors = sensorsResponse.data.filter(sensor => sensor.status === 1).length
        this.offlineSensors = this.sensorCount - this.onlineSensors
        this.onlineRate = Math.round(this.onlineSensors / this.sensorCount * 100)
      } catch (error) {
        console.error('加载概览数据失败:', error)
      }
    },
    initWebSocket() {
      // 初始化WebSocket连接
      const ws = new WebSocket('ws://localhost:8080/api/ws/parking')
      
      ws.onopen = () => {
        console.log('WebSocket连接已建立')
      }
      
      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          // 判断数据类型
          if (data.spotId) {
            // 车位状态数据
            this.spotStatuses.unshift(data)
            if (this.spotStatuses.length > 10) {
              this.spotStatuses.pop()
            }
          } else if (data.areaId) {
            // 区域聚合数据
            const existingIndex = this.areaAggregations.findIndex(item => item.areaId === data.areaId)
            if (existingIndex > -1) {
              this.areaAggregations[existingIndex] = data
            } else {
              this.areaAggregations.push(data)
            }
          }
        } catch (error) {
          console.error('解析WebSocket消息失败:', error)
        }
      }
      
      ws.onclose = () => {
        console.log('WebSocket连接已关闭')
        // 尝试重连
        setTimeout(() => this.initWebSocket(), 3000)
      }
      
      ws.onerror = (error) => {
        console.error('WebSocket错误:', error)
      }
    }
  }
}
</script>

<style scoped>
.home {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.overview-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.overview-label {
  font-size: 14px;
  color: #606266;
}

.overview-value {
  font-size: 24px;
  font-weight: bold;
  color: #409EFF;
}

.status-ok {
  color: #67C23A;
}

.status-list {
  max-height: 300px;
  overflow-y: auto;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.status-spot {
  font-weight: bold;
  margin-right: 10px;
}

.status-parking {
  font-size: 12px;
  color: #909399;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
}

.status-free {
  background-color: #f0f9eb;
  color: #67C23A;
}

.status-occupied {
  background-color: #fef0f0;
  color: #F56C6C;
}

.aggregation-list {
  max-height: 300px;
  overflow-y: auto;
}

.aggregation-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-bottom: 1px solid #e4e7ed;
}

.aggregation-area {
  font-weight: bold;
  margin-right: 10px;
}

.aggregation-parking {
  font-size: 12px;
  color: #909399;
}

.aggregation-stats {
  font-size: 14px;
}

.aggregation-free {
  color: #67C23A;
  font-weight: bold;
}

.aggregation-total {
  color: #909399;
}

.empty-status {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}
</style>
