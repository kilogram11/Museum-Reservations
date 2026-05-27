Page({
  data: {
    longitude: 0,
    latitude: 0,
    markers: []
  },

  onLoad(options) {
    const lng = Number(options.lng);
    const lat = Number(options.lat);
    const title = options.title || '场馆位置';

    console.log('接收到百度坐标：', lng, lat);

    // 百度坐标(BD-09) 转 火星坐标(GCJ-02)
    const gcj = this.bd09ToGcj02(lng, lat);
    console.log('转换后 GCJ-02 坐标：', gcj);

    this.setData({
      longitude: gcj.longitude,
      latitude: gcj.latitude,
      markers: [
        {
          id: 1,
          longitude: gcj.longitude,
          latitude: gcj.latitude,
          title: title,
          width: 30,  // 必填
          height: 30  // 必填
        }
      ]
    });
  },

  /**
   * 百度坐标 (BD-09) 转 火星坐标 (GCJ-02)
   */
  bd09ToGcj02(bd_lon, bd_lat) {
    var x_pi = 3.14159265358979324 * 3000.0 / 180.0;
    var x = bd_lon - 0.0065;
    var y = bd_lat - 0.006;
    var z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * x_pi);
    var theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * x_pi);
    return {
      longitude: z * Math.cos(theta),
      latitude: z * Math.sin(theta)
    };
  }
});
