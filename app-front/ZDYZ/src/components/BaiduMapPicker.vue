<template>
  <div class="map-picker">
    <div ref="mapRef" class="map-container"></div>

    <div class="info">
      <div>📍 地址：{{ address || '请在地图上点击选择' }}</div>
      <div>🌐 经度：{{ longitude }}</div>
      <div>🌐 纬度：{{ latitude }}</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'

const emit = defineEmits(['change'])

const props = defineProps({
  longitude: [String, Number],
  latitude: [String, Number]
})


const mapRef = ref(null)
const map = ref(null)
const marker = ref(null)

const longitude = ref('')
const latitude = ref('')
const address = ref('')

onMounted(() => {
  initMap()

  // 👉 如果父组件传了经纬度（编辑场馆），则回显
  if (props.longitude && props.latitude) {
    const point = new BMap.Point(props.longitude, props.latitude)
    setMarker(point)
    map.value.centerAndZoom(point, 15)

    longitude.value = props.longitude
    latitude.value = props.latitude
  }
})

function initMap() {
  // 默认中心点（北京，可改成你学校/城市）
  const center = new BMap.Point(116.404, 39.915)

  map.value = new BMap.Map(mapRef.value)
  map.value.centerAndZoom(center, 12)
  map.value.enableScrollWheelZoom(true)

  const geocoder = new BMap.Geocoder()

  map.value.addEventListener('click', (e) => {
    const point = e.point
    setMarker(point)

    longitude.value = point.lng
    latitude.value = point.lat

    // 逆地址解析
    geocoder.getLocation(point, (res) => {
      address.value = res?.address || ''
      emitChange()
    })

  })
}

function setMarker(point) {
  if (marker.value) {
    marker.value.setPosition(point)
  } else {
    marker.value = new BMap.Marker(point, { enableDragging: true })
    marker.value.enableDragging()
    map.value.addOverlay(marker.value)

    marker.value.addEventListener('dragend', (e) => {
      longitude.value = e.point.lng
      latitude.value = e.point.lat

      const geocoder = new BMap.Geocoder()
      geocoder.getLocation(e.point, (res) => {
        address.value = res.address
        emitChange()
      })
    })
  }
}

function emitChange() {
  emit('change', {
    longitude: longitude.value,
    latitude: latitude.value,
    address: address.value
  })
}
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 400px;
  border-radius: 6px;
}

.info {
  margin-top: 10px;
  font-size: 14px;
  line-height: 22px;
}
</style>
