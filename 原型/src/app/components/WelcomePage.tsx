interface WelcomePageProps {
  onNavigateToLogin: () => void;
  onNavigateToRegister: () => void;
}

export default function WelcomePage({ onNavigateToLogin, onNavigateToRegister }: WelcomePageProps) {
  return (
    <div className="min-h-screen relative overflow-hidden" style={{ background: 'linear-gradient(135deg, #f5f0e8 0%, #e8dfd0 100%)' }}>
      {/* 宣纸质感背景纹理 */}
      <div className="absolute inset-0 opacity-30" style={{
        backgroundImage: `url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' /%3E%3C/filter%3E%3Crect width='100' height='100' filter='url(%23noise)' opacity='0.05'/%3E%3C/svg%3E")`,
      }}></div>
      
      {/* 故宫建筑线性轮廓剪影 */}
      <div className="absolute inset-0 flex items-center justify-center opacity-10">
        <svg viewBox="0 0 400 300" className="w-full max-w-2xl">
          {/* 主殿 */}
          <path d="M 50 200 L 100 150 L 150 150 L 200 100 L 250 150 L 300 150 L 350 200 L 350 250 L 50 250 Z" 
                stroke="#8B0000" strokeWidth="2" fill="none" opacity="0.3"/>
          {/* 屋檐 */}
          <path d="M 30 200 L 200 80 L 370 200" 
                stroke="#8B0000" strokeWidth="2" fill="none" opacity="0.3"/>
          {/* 装饰檐角 */}
          <path d="M 30 200 L 20 210 M 370 200 L 380 210" 
                stroke="#8B0000" strokeWidth="2" fill="none" opacity="0.3"/>
        </svg>
      </div>

      <div className="relative z-10 min-h-screen flex flex-col items-center justify-center p-6">
        {/* Logo和标题 */}
        <div className="text-center mb-16">
          <h1 className="text-4xl font-serif mb-4" style={{ 
            color: '#8B0000',
            fontFamily: '"KaiTi", "楷体", serif',
            letterSpacing: '0.2em'
          }}>
            博物馆预约系统
          </h1>
          <p className="text-lg" style={{ 
            color: '#8B4513',
            fontFamily: '"KaiTi", "楷体", serif',
            letterSpacing: '0.15em'
          }}>
            探索千年文化 · 感悟历史变革
          </p>
        </div>

        {/* 按钮区域 */}
        <div className="w-full max-w-sm space-y-4">
          <button
            onClick={onNavigateToLogin}
            className="w-full py-4 rounded-lg font-semibold shadow-lg hover:shadow-xl transition-all text-white"
            style={{ 
              background: '#8B0000',
              fontFamily: '"KaiTi", "楷体", serif',
              letterSpacing: '0.2em'
            }}
          >
            登录
          </button>
          
          <button
            onClick={onNavigateToRegister}
            className="w-full py-4 rounded-lg font-semibold hover:bg-red-50 transition-colors"
            style={{ 
              border: '2px solid #8B0000',
              color: '#8B0000',
              background: 'transparent',
              fontFamily: '"KaiTi", "楷体", serif',
              letterSpacing: '0.2em'
            }}
          >
            注册
          </button>
        </div>

        {/* 底部版本信息 */}
        <div className="absolute bottom-6 text-sm" style={{ color: '#8B4513', opacity: 0.6 }}>
          版本 2.4.12 • 2025
        </div>
      </div>
    </div>
  );
}