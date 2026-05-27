<template>
  <div class="baidu-map-picker">
    <div ref="mapRef" style="width: 100%; height: 400px;"></div>
  </div>
</template>

<script>
import { BMPGL } from '@/utils/bmp.js'

export default {
  name: 'BaiduMapPicker',
  props: {
    longitude: { type: [Number, String], default: 0 },
    latitude:  { type: [Number, String], default: 0 }
  },
  data() {
    return {
      map: null,
      BMap: null
    }
  },
  mounted() {
    console.log('[BaiduMapPicker] mounted - props:', {
      longitude: this.longitude,
      latitude: this.latitude
    })
    
    this.$nextTick(() => {
      // 延迟 300ms 确保容器已渲染（针对弹窗场景）
      setTimeout(() => {
        this.initMap()
      }, 300)
    })
  },
  watch: {
    latitude(newVal, oldVal) {
      // 避免重复触发
      if (newVal === oldVal) return
      this.updateCenter()
    },
    longitude(newVal, oldVal) {
      if (newVal === oldVal) return
      this.updateCenter()
    }
  },
  methods: {
    initMap() {
      const ak = 's7QW1pclY1HNqyTnBCmZzmlMhs4cyQqp'
      console.log('[BaiduMapPicker] 开始初始化地图...')
      
      BMPGL(ak).then((BMap) => {
        // 1. 获取真实的 DOM 元素 (使用 ref)
        const mapContainer = this.$refs.mapRef;
        
        // 2. 安全检查：如果组件被销毁或 DOM 不存在，停止初始化
        if (!mapContainer) {
          console.error('[BaiduMapPicker] 错误：找不到地图容器 DOM，可能组件已卸载');
          return;
        }

        // 3. 检查容器是否有高度 (排查白屏的关键日志)
        console.log('[BaiduMapPicker] 容器尺寸:', {
          width: mapContainer.offsetWidth,
          height: mapContainer.offsetHeight
        });
        
        if (mapContainer.offsetHeight === 0) {
          console.warn('[BaiduMapPicker] 严重警告：容器高度为 0，地图将不可见！请检查父组件是否使用了 v-show 或 display:none');
        }

        this.BMap = BMap
        
        // 4. 初始化地图 (传入 DOM 对象)
        this.map = new BMap.Map(mapContainer)
        
        // 处理坐标
        const lng = Number(this.longitude) || 0
        const lat = Number(this.latitude)  || 0
        
        // 如果有有效坐标就用，否则默认北京天安门
        // 注意：如果是 0,0 坐标，也应该视为无效，使用默认值
        const point = (lng !== 0 && lat !== 0) 
          ? new BMap.Point(lng, lat)
          : new BMap.Point(116.404, 39.915)
        
        console.log('[BaiduMapPicker] 初始化中心点:', point)
        
        this.map.centerAndZoom(point, 15)
        this.map.enableScrollWheelZoom(true)

        // 若已有有效坐标，添加初始标注
        if (lng !== 0 && lat !== 0) {
          this.map.addOverlay(new BMap.Marker(point))
        }

        // 5. 绑定点击事件
        this.map.addEventListener('click', e => {
          const { lng, lat } = e.point
          console.log('[BaiduMapPicker] 地图点击:', { lng, lat })

          // 清除旧标记并添加新标记
          this.map.clearOverlays()
          this.map.addOverlay(new BMap.Marker(e.point))

          // 逆地理编码 (坐标 -> 地址)
          const geocoder = new BMap.Geocoder()
          geocoder.getLocation(e.point, (rs) => {
            const address = rs?.address || ''
            console.log('[BaiduMapPicker] 选中地址:', address)
            
            // 向父组件发送数据
            this.$emit('change', {
              longitude: lng,
              latitude: lat,
              address: address
            })
          })
        })
        
        console.log('[BaiduMapPicker] 地图初始化完成')
      }).catch(err => {
        console.error('[BaiduMapPicker] 地图加载失败:', err)
      })
    },
    
    updateCenter() {
      if (!this.map || !this.BMap) {
        return
      }
      
      const lng = Number(this.longitude) || 0
      const lat = Number(this.latitude)  || 0
      
      // 只有坐标有效且不为0时才更新视图，避免重置到非洲
      if (lng !== 0 && lat !== 0) {
        console.log('[BaiduMapPicker] 更新地图中心:', { lng, lat })
        const point = new this.BMap.Point(lng, lat)
        this.map.panTo(point) // 使用 panTo 动画过渡更平滑
        this.map.clearOverlays()
        this.map.addOverlay(new this.BMap.Marker(point))
      }
    }
  }
}
</script>

<style scoped>
.baidu-map-picker {
  border: 1px solid #eee;
  /* 这里的 min-height 是为了保证外层容器有高度 */
  min-height: 400px;
  width: 100%;
}
</style>