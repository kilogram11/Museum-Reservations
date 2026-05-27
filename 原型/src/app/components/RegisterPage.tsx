import { useState } from 'react';
import { Eye, EyeOff, User, Phone, Lock, Upload } from 'lucide-react';
import { toast } from 'sonner';

interface RegisterPageProps {
  onBack: () => void;
  onRegisterSuccess: () => void;
}

export default function RegisterPage({ onBack, onRegisterSuccess }: RegisterPageProps) {
  const [username, setUsername] = useState('');
  const [gender, setGender] = useState<'male' | 'female' | ''>('');
  const [avatar, setAvatar] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const avatarOptions = ['👨', '👩', '👦', '👧', '🧑', '👴', '👵', '🧔', '👨‍💼', '👩‍💼', '👨‍🎓', '👩‍🎓'];

  const handleRegister = () => {
    if (!username) {
      toast.error('请输入用户名');
      return;
    }

    if (!gender) {
      toast.error('请选择性别');
      return;
    }

    if (!avatar) {
      toast.error('请选择头像');
      return;
    }

    if (!phone) {
      toast.error('请输入手机号');
      return;
    }

    if (phone.length !== 11) {
      toast.error('请输入11位手机号');
      return;
    }

    if (!password) {
      toast.error('请输入密码');
      return;
    }

    if (password.length < 6) {
      toast.error('密码长度至少6位');
      return;
    }

    if (!confirmPassword) {
      toast.error('请确认密码');
      return;
    }

    if (password !== confirmPassword) {
      toast.error('两次密码输入不一致');
      return;
    }

    // 注册成功
    toast.success('注册成功！即将跳转到登录页面...', {
      duration: 2000,
    });

    setTimeout(() => {
      onRegisterSuccess();
    }, 2000);
  };

  return (
    <div className="min-h-screen relative overflow-hidden flex flex-col overflow-y-auto" style={{ background: 'linear-gradient(135deg, #f5f0e8 0%, #e8dfd0 100%)' }}>
      {/* 宣纸质感背景纹理 */}
      <div className="absolute inset-0 opacity-30" style={{
        backgroundImage: `url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noise'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' /%3E%3C/filter%3E%3Crect width='100' height='100' filter='url(%23noise)' opacity='0.05'/%3E%3C/svg%3E")`,
      }}></div>

      {/* 顶部返回按钮 */}
      <div className="relative z-10 p-4">
        <button onClick={onBack} style={{ color: '#8B4513' }}>
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
      </div>

      <div className="relative z-10 flex-1 flex flex-col items-center p-6 pb-8">
        {/* 标题 */}
        <div className="text-center mb-6">
          <h1 className="text-2xl mb-2" style={{ 
            color: '#8B0000',
            fontFamily: '"KaiTi", "楷体", serif',
            letterSpacing: '0.2em'
          }}>欢迎来到博物馆</h1>
          <p className="text-sm" style={{ 
            color: '#8B4513',
            fontFamily: '"KaiTi", "楷体", serif'
          }}>创建您的账号</p>
        </div>

        {/* 注册表单 */}
        <div className="w-full max-w-sm space-y-4 pb-8">
          {/* 用户名 */}
          <div className="relative">
            <div className="flex items-center pb-2" style={{ borderBottom: '1px solid #D2B48C' }}>
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

          {/* 性别选择 */}
          <div className="space-y-2">
            <label className="text-sm ml-1" style={{ color: '#8B4513' }}>性别</label>
            <div className="flex gap-2">
              <button
                onClick={() => setGender('male')}
                className={`flex-1 py-3 rounded-lg font-medium transition-all ${
                  gender === 'male'
                    ? 'shadow-md'
                    : 'border'
                }`}
                style={gender === 'male' ? {
                  background: '#8B0000',
                  color: 'white'
                } : {
                  borderColor: '#D2B48C',
                  color: '#8B4513',
                  background: 'transparent'
                }}
              >
                👨 男
              </button>
              <button
                onClick={() => setGender('female')}
                className={`flex-1 py-3 rounded-lg font-medium transition-all ${
                  gender === 'female'
                    ? 'shadow-md'
                    : 'border'
                }`}
                style={gender === 'female' ? {
                  background: '#8B0000',
                  color: 'white'
                } : {
                  borderColor: '#D2B48C',
                  color: '#8B4513',
                  background: 'transparent'
                }}
              >
                👩 女
              </button>
            </div>
          </div>

          {/* 头像选择 */}
          <div className="space-y-2">
            <label className="text-sm ml-1" style={{ color: '#8B4513' }}>选择头像</label>
            <div className="rounded-lg p-3" style={{ border: '1px solid #D2B48C', background: 'rgba(255, 255, 255, 0.3)' }}>
              <div className="grid grid-cols-6 gap-2">
                {avatarOptions.map((ava, index) => (
                  <button
                    key={index}
                    onClick={() => setAvatar(ava)}
                    className={`w-11 h-11 rounded-full flex items-center justify-center text-xl transition-all ${
                      avatar === ava
                        ? 'shadow-md scale-110'
                        : ''
                    }`}
                    style={avatar === ava ? {
                      background: '#8B0000',
                      border: '2px solid #FFD700'
                    } : {
                      background: 'rgba(255, 255, 255, 0.5)'
                    }}
                  >
                    {ava}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* 手机号 */}
          <div className="relative">
            <div className="flex items-center pb-2" style={{ borderBottom: '1px solid #D2B48C' }}>
              <Phone className="w-5 h-5 mr-3" style={{ color: '#8B0000' }} />
              <input
                type="tel"
                placeholder="请输入11位手机号"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                maxLength={11}
                className="flex-1 bg-transparent outline-none"
                style={{ 
                  color: '#8B4513',
                  fontFamily: '"KaiTi", "楷体", serif'
                }}
              />
            </div>
          </div>

          {/* 密码 */}
          <div className="relative">
            <div className="flex items-center pb-2" style={{ borderBottom: '1px solid #D2B48C' }}>
              <Lock className="w-5 h-5 mr-3" style={{ color: '#8B0000' }} />
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="请输入密码（至少6位）"
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

          {/* 确认密码 */}
          <div className="relative">
            <div className="flex items-center pb-2" style={{ borderBottom: '1px solid #D2B48C' }}>
              <Lock className="w-5 h-5 mr-3" style={{ color: '#8B0000' }} />
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                placeholder="请再次输入密码"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="flex-1 bg-transparent outline-none"
                style={{ 
                  color: '#8B4513',
                  fontFamily: '"KaiTi", "楷体", serif'
                }}
              />
              <button
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                style={{ color: '#8B4513' }}
              >
                {showConfirmPassword ? (
                  <EyeOff className="w-5 h-5" />
                ) : (
                  <Eye className="w-5 h-5" />
                )}
              </button>
            </div>
          </div>

          {/* 注册按钮 */}
          <button
            onClick={handleRegister}
            className="w-full py-4 rounded-xl font-semibold shadow-lg hover:shadow-xl transition-all mt-6 text-white"
            style={{ 
              background: '#8B0000',
              fontFamily: '"KaiTi", "楷体", serif',
              letterSpacing: '0.3em'
            }}
          >
            注册
          </button>

          {/* 登录提示 */}
          <div className="text-center pt-2">
            <span className="text-sm" style={{ color: '#8B4513', opacity: 0.7 }}>已有账号？</span>
            <button
              onClick={onRegisterSuccess}
              className="text-sm font-semibold ml-2 hover:underline"
              style={{ color: '#8B0000' }}
            >
              立即登录
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
