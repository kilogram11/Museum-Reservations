import { useState, useEffect } from 'react';
import museumImage from "figma:asset/c2595921d536f75dff08b819f0e1dec205eeaeb5.png";
import palaceImage from "figma:asset/551327f57d6d018867b46166ec1913d0eb01640a.png";

interface HomePageProps {
  onNavigateToBooking: () => void;
  onNavigateToActivity: () => void;
  onNavigateToAnnouncement: () => void;
  onNavigateToProfile: () => void;
}

export default function HomePage({
  onNavigateToBooking,
  onNavigateToActivity,
  onNavigateToAnnouncement,
  onNavigateToProfile,
}: HomePageProps) {
  const [currentSlide, setCurrentSlide] = useState(0);
  const images = [museumImage, palaceImage];

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % images.length);
    }, 3000);

    return () => clearInterval(timer);
  }, [images.length]);

  return (
    <div className="h-screen overflow-y-auto" style={{ background: 'linear-gradient(to bottom, #f5f0e8 0%, #ffffff 100%)' }}>
      {/* 顶部轮播图区域 - 铺满 */}
      <div className="relative h-72 overflow-hidden">
        {images.map((image, index) => (
          <div
            key={index}
            className={`absolute inset-0 transition-opacity duration-1000 ${
              index === currentSlide ? 'opacity-100' : 'opacity-0'
            }`}
          >
            <img
              src={image}
              alt={`博物馆 ${index + 1}`}
              className="w-full h-full object-cover"
            />
            <div className="absolute inset-0 bg-gradient-to-b from-black/20 to-black/40"></div>
          </div>
        ))}
        
        {/* 轮播指示器 */}
        <div className="absolute bottom-4 left-0 right-0 flex justify-center gap-2 z-10">
          {images.map((_, index) => (
            <button
              key={index}
              onClick={() => setCurrentSlide(index)}
              className={`w-2 h-2 rounded-full transition-all ${
                index === currentSlide 
                  ? 'bg-white w-6' 
                  : 'bg-white/50'
              }`}
            />
          ))}
        </div>
      </div>

      {/* 今日开放状态卡片 */}
      <div className="px-4 mb-6 mt-6">
        <div className="rounded-xl p-4 shadow-sm" style={{ 
          background: 'linear-gradient(135deg, #FFF8DC 0%, #FAEBD7 100%)' 
        }}>
          <div className="flex items-center justify-between">
            <div>
              <h3 className="font-semibold mb-1" style={{ color: '#8B0000' }}>今日开放</h3>
              <p className="text-sm" style={{ color: '#8B4513' }}>12月26日</p>
            </div>
            <div className="text-right">
              <div className="text-xs mb-1" style={{ color: '#8B4513' }}>开放时间</div>
              <div className="font-semibold" style={{ color: '#8B0000' }}>08:30 - 17:00</div>
            </div>
          </div>
        </div>
      </div>

      {/* 功能卡片区域 - 3个卡片 */}
      <div className="px-4 mb-6">
        <div className="grid grid-cols-3 gap-3">
          {/* 参观预约卡片 */}
          <button
            onClick={onNavigateToBooking}
            className="rounded-xl p-4 shadow-md hover:shadow-lg transition-all text-center"
            style={{ 
              background: 'linear-gradient(135deg, #8B0000 0%, #A52A2A 100%)' 
            }}
          >
            <div className="flex flex-col items-center">
              <div className="w-12 h-12 rounded-full bg-white/20 flex items-center justify-center mb-2">
                <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M19 3h-1V1h-2v2H8V1H6v2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V9h14v10zM7 11h5v5H7z"/>
                </svg>
              </div>
              <h4 className="text-white font-semibold text-sm mb-1" style={{ fontFamily: '"KaiTi", "楷体", serif' }}>
                参观预约
              </h4>
              <p className="text-white/80 text-xs">在线预约</p>
            </div>
          </button>

          {/* 活动查看卡片 */}
          <button
            onClick={onNavigateToActivity}
            className="rounded-xl p-4 shadow-md hover:shadow-lg transition-all text-center"
            style={{ 
              background: 'linear-gradient(135deg, #DAA520 0%, #F0E68C 100%)' 
            }}
          >
            <div className="flex flex-col items-center">
              <div className="w-12 h-12 rounded-full bg-white/30 flex items-center justify-center mb-2">
                <svg className="w-6 h-6" style={{ color: '#8B4513' }} viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
                </svg>
              </div>
              <h4 className="font-semibold text-sm mb-1" style={{ color: '#8B4513', fontFamily: '"KaiTi", "楷体", serif' }}>
                活动查看
              </h4>
              <p className="text-xs" style={{ color: '#8B4513', opacity: 0.8 }}>展览活动</p>
            </div>
          </button>

          {/* 公告查看卡片 */}
          <button
            onClick={onNavigateToAnnouncement}
            className="rounded-xl p-4 shadow-md hover:shadow-lg transition-all text-center"
            style={{ 
              background: 'linear-gradient(135deg, #4682B4 0%, #87CEEB 100%)' 
            }}
          >
            <div className="flex flex-col items-center">
              <div className="w-12 h-12 rounded-full bg-white/30 flex items-center justify-center mb-2">
                <svg className="w-6 h-6 text-white" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02z"/>
                </svg>
              </div>
              <h4 className="text-white font-semibold text-sm mb-1" style={{ fontFamily: '"KaiTi", "楷体", serif' }}>
                公告查看
              </h4>
              <p className="text-white/80 text-xs">馆内公告</p>
            </div>
          </button>
        </div>
      </div>

      {/* 故宫博物院信息卡片 */}
      <div className="px-4 mb-6 pb-24">
        <div className="rounded-2xl p-6 shadow-sm" style={{ 
          background: 'linear-gradient(135deg, #FFF8DC 0%, #FAEBD7 100%)',
          border: '1px solid #D2B48C'
        }}>
          <h3 className="text-lg font-semibold mb-4" style={{ 
            color: '#8B0000',
            fontFamily: '"KaiTi", "楷体", serif',
            letterSpacing: '0.1em'
          }}>
            故宫博物院
          </h3>

          <div className="space-y-3">
            <div className="flex items-start">
              <svg className="w-5 h-5 mr-2 mt-0.5" style={{ color: '#8B0000' }} viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
              </svg>
              <div>
                <h4 className="text-sm font-semibold mb-1" style={{ color: '#8B4513' }}>地址</h4>
                <p className="text-sm" style={{ color: '#8B4513', opacity: 0.8 }}>
                  北京市东城区景山前街4号
                </p>
              </div>
            </div>

            <div className="flex items-start">
              <svg className="w-5 h-5 mr-2 mt-0.5" style={{ color: '#8B0000' }} viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C6.5 2 2 6.5 2 12s4.5 10 10 10 10-4.5 10-10S17.5 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm.5-13H11v6l5.2 3.2.8-1.3-4.5-2.7V7z"/>
              </svg>
              <div>
                <h4 className="text-sm font-semibold mb-1" style={{ color: '#8B4513' }}>开放时间</h4>
                <p className="text-sm" style={{ color: '#8B4513', opacity: 0.8 }}>
                  08:30 - 17:00（周二至周日）
                </p>
              </div>
            </div>

            <div className="flex items-start">
              <svg className="w-5 h-5 mr-2 mt-0.5" style={{ color: '#8B0000' }} viewBox="0 0 24 24" fill="currentColor">
                <path d="M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z"/>
              </svg>
              <div>
                <h4 className="text-sm font-semibold mb-1" style={{ color: '#8B4513' }}>咨询电话</h4>
                <p className="text-sm" style={{ color: '#8B4513', opacity: 0.8 }}>
                  010-8500 7421
                </p>
              </div>
            </div>

            <div className="pt-2">
              <p className="text-sm leading-relaxed" style={{ color: '#8B4513', opacity: 0.8 }}>
                故宫博物院建立于1925年，是在明清两朝皇宫及其收藏基础上建立起来的大型综合性博物馆，也是中国最大的古代文化艺术博物馆。
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 底部导航 */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t px-4 py-3 safe-area-inset-bottom shadow-lg" style={{ borderColor: '#D2B48C' }}>
        <div className="grid grid-cols-2 gap-4 max-w-md mx-auto">
          <button className="flex flex-col items-center justify-center py-2">
            <div className="w-6 h-6 mb-1 rounded-full flex items-center justify-center" style={{ background: '#8B0000' }}>
              <svg className="w-4 h-4 text-white" viewBox="0 0 24 24" fill="currentColor">
                <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"/>
              </svg>
            </div>
            <span className="text-xs font-medium" style={{ color: '#8B0000' }}>首页</span>
          </button>
          <button
            onClick={onNavigateToProfile}
            className="flex flex-col items-center justify-center py-2"
          >
            <div className="w-6 h-6 mb-1 flex items-center justify-center">
              <svg className="w-5 h-5" style={{ color: '#8B4513' }} viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
              </svg>
            </div>
            <span className="text-xs" style={{ color: '#8B4513' }}>我的</span>
          </button>
        </div>
      </div>
    </div>
  );
}
