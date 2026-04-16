import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 停车场相关API
export const parkingLotApi = {
  getAll: () => api.get('/parking-lots'),
  getById: (parkingId) => api.get(`/parking-lots/${parkingId}`),
  create: (data) => api.post('/parking-lots', data),
  update: (id, data) => api.put(`/parking-lots/${id}`, data),
  delete: (id) => api.delete(`/parking-lots/${id}`)
}

// 车位相关API
export const parkingSpotApi = {
  getAll: () => api.get('/parking-spots'),
  getById: (spotId) => api.get(`/parking-spots/${spotId}`),
  getByParkingId: (parkingId) => api.get(`/parking-spots/parking/${parkingId}`),
  getByParkingIdAndAreaId: (parkingId, areaId) => api.get(`/parking-spots/parking/${parkingId}/area/${areaId}`),
  getByParkingIdAndFloor: (parkingId, floor) => api.get(`/parking-spots/parking/${parkingId}/floor/${floor}`),
  create: (data) => api.post('/parking-spots', data),
  update: (id, data) => api.put(`/parking-spots/${id}`, data),
  delete: (id) => api.delete(`/parking-spots/${id}`)
}

// 区域相关API
export const areaApi = {
  getAll: () => api.get('/areas'),
  getById: (areaId) => api.get(`/areas/${areaId}`),
  getByParkingId: (parkingId) => api.get(`/areas/parking/${parkingId}`),
  getByParkingIdAndFloor: (parkingId, floor) => api.get(`/areas/parking/${parkingId}/floor/${floor}`),
  create: (data) => api.post('/areas', data),
  update: (id, data) => api.put(`/areas/${id}`, data),
  delete: (id) => api.delete(`/areas/${id}`)
}

// 传感器相关API
export const sensorApi = {
  getAll: () => api.get('/sensors'),
  getById: (sensorId) => api.get(`/sensors/${sensorId}`),
  getByParkingId: (parkingId) => api.get(`/sensors/parking/${parkingId}`),
  getByParkingIdAndStatus: (parkingId, status) => api.get(`/sensors/parking/${parkingId}/status/${status}`),
  create: (data) => api.post('/sensors', data),
  update: (id, data) => api.put(`/sensors/${id}`, data),
  delete: (id) => api.delete(`/sensors/${id}`)
}

export default api
