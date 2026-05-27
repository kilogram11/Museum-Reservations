export function BMPGL(ak) {
  return new Promise(function(resolve, reject) {
    console.log('[bmp.js] 开始加载百度地图 API...')
    
    if (typeof window.BMap !== 'undefined') {
      console.log('[bmp.js] BMap 已存在，直接使用')
      resolve(window.BMap)
      return
    }
    
    window.onBMapCallback = function() {
      console.log('[bmp.js] BMap callback 触发，地图加载成功')
      resolve(window.BMap)
    }
    
    const script = document.createElement('script')
    script.type = 'text/javascript'
    // script.src = 'https://api.map.baidu.com/api?v=3.0&ak=' + ak + '&callback=onBMapCallback'
    script.src = '//api.map.baidu.com/api?v=3.0&ak=' + ak + '&callback=onBMapCallback'

    script.onerror = function(err) {
      console.error('[bmp.js] 百度地图脚本加载失败', err)
      reject(new Error('Baidu Map script load error'))
    }
    
    console.log('[bmp.js] 插入脚本标签:', script.src)
    document.head.appendChild(script)
  })
}

