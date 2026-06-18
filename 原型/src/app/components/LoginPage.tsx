import { useState } from "react";
import { Eye, EyeOff, User, Lock } from "lucide-react";
import { toast } from "sonner";

interface LoginPageProps {
  onBack: () => void;
  onLoginSuccess: (username: string) => void;
}

export default function LoginPage({
  onBack,
  onLoginSuccess,
}: LoginPageProps) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const handleLogin = () => {
    if (!username) {
      toast.error("请输入用户名");
      return;
    }

    if (!password) {
      toast.error("请输入密码");
      return;
    }

    // 模拟登录验证
    if (username && password) {
      toast.success("登录成功！", {
        duration: 1500,
      });

      setTimeout(() => {
        onLoginSuccess(username);
      }, 1500);
    }
  };

  return (
    <div className="min-h-screen relative overflow-hidden" style={{ background: 'linear-gradient(135deg, #f5f0e8 0%, #e8dfd0 100%)' }}>
      {/* 宣纸质感背景纹理 */}
      <div className="absolute inset-0 opacity-30" style={{
        backgroundImage: `url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' /%3E%3C/filter%3E%3Crect width='100' height='100' filter='url(%23noise)' opacity='0.05'/%3E%3C/svg%3E")`,
      }}></div>

      {/* 顶部返回按钮 */}
      <div className="relative z-10 p-4">
        <button
          onClick={onBack}
          style={{ color: '#8B4513' }}
        >
          <svg
            className="w-6 h-6"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M15 19l-7-7 7-7"
            />
          </svg>
        </button>
      </div>

      <div className="relative z-10 flex-1 flex flex-col items-center justify-center p-6 pt-12">
        {/* 标题 */}
        <h1 className="text-3xl mb-2" style={{ 
          color: '#8B0000',
          fontFamily: '"KaiTi", "楷体", serif',
          letterSpacing: '0.2em'
        }}>
          欢迎来到博物馆
        </h1>
        <p className="mb-12" style={{ 
          color: '#8B4513',
          fontFamily: '"KaiTi", "楷体", serif',
          letterSpacing: '0.1em'
        }}>
          登录您的账号
        </p>

        {/* 登录表单 */}
        <div className="w-full max-w-sm space-y-6">
          {/* 用户名输入 */}
          <div className="relative">
            <div className="flex items-center pb-2" style={{ borderBottom: '1px solid #D2B48C' }}>
              {/* 祥云图标 */}
              <svg className="w-5 h-5 mr-3" style={{ color: '#8B0000' }} viewBox="0 0 24 24" fill="currentColor">
                <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4c-1.48 0-2.85.43-4.01 1.17l1.46 1.46C10.21 6.23 11.08 6 12 6c3.04 0 5.5 2.46 5.5 5.5v.5H19c1.66 0 3 1.34 3 3 0 1.13-.64 2.11-1.56 2.62l1.45 1.45C23.16 18.16 24 16.68 24 15c0-2.64-2.05-4.78-4.65-4.96zM3 5.27l2.75 2.74C2.56 8.15 0 10.77 0 14c0 3.31 2.69 6 6 6h11.73l2 2L21 20.73 4.27 4 3 5.27zM7.73 10l8 8H6c-2.21 0-4-1.79-4-4s1.79-4 4-4h1.73z"/>
              </svg>
              <input
                type="text"
                placeholder="请输入用户名"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="flex-1 bg-transparent outline-none"
                style={{ 
                  color: '#8B4513',
                  fontFamily: '"KaiTi", "楷体", serif'
                }}
              />
            </div>
          </div>

          {/* 密码输入 */}
          <div className="relative">
            <div className="flex items-center pb-2" style={{ borderBottom: '1px solid #D2B48C' }}>
              {/* 古建筑图标 */}
              <svg className="w-5 h-5 mr-3" style={{ color: '#8B0000' }} viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-3zm0 10.99h7c-.53 4.12-3.28 7.79-7 8.94V12H5V6.3l7-3.11v8.8z"/>
              </svg>
              <input
                type={showPassword ? "text" : "password"}
                placeholder="请输入密码"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="flex-1 bg-transparent outline-none"
                style={{ 
                  color: '#8B4513',
                  fontFamily: '"KaiTi", "楷体", serif'
                }}
              />
              <button
                onClick={() => setShowPassword(!showPassword)}
                style={{ color: '#8B4513' }}
              >
                {showPassword ? (
                  <EyeOff className="w-5 h-5" />
                ) : (
                  <Eye className="w-5 h-5" />
                )}
              </button>
            </div>
          </div>

          {/* 登录按钮 */}
          <button
            onClick={handleLogin}
            className="w-full py-4 rounded-xl font-semibold text-lg shadow-lg hover:shadow-xl transition-all text-white mt-8"
            style={{ 
              background: '#8B0000',
              fontFamily: '"KaiTi", "楷体", serif',
              letterSpacing: '0.3em'
            }}
          >
            登录
          </button>

          {/* 注册提示 */}
          <div className="text-center pt-4">
            <span className="text-sm" style={{ color: '#8B4513', opacity: 0.7 }}>
              还没有账号？
            </span>
            <button
              onClick={onBack}
              className="text-sm font-semibold ml-2 hover:underline"
              style={{ color: '#8B0000' }}
            >
              立即注册
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}