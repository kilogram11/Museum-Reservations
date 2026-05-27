import {
  ChevronRight,
  Calendar,
  Users,
  Settings,
  MessageSquare,
  LogOut,
} from "lucide-react";

interface ProfilePageProps {
  onBack: () => void;
  onNavigateToVisitorRegistration: () => void;
  onNavigateToBookingRecord: () => void;
  onNavigateToEditProfile: () => void;
  onLogout: () => void;
  username: string;
  avatar: string;
}

export default function ProfilePage({
  onBack,
  onNavigateToVisitorRegistration,
  onNavigateToBookingRecord,
  onNavigateToEditProfile,
  onLogout,
  username,
  avatar,
}: ProfilePageProps) {
  return (
    <div
      className="min-h-screen overflow-y-auto pb-20"
      style={{
        background:
          "linear-gradient(to bottom, #f5f0e8 0%, #ffffff 100%)",
      }}
    >
      {/* 顶部个人信息卡片 - 故宫红渐变背景 */}
      <div
        className="pt-12 pb-20 px-6 rounded-b-[2rem] relative overflow-hidden"
        style={{
          background:
            "linear-gradient(135deg, #8B0000 0%, #A52A2A 100%)",
        }}
      >
        {/* 装饰性纹理 */}
        <div
          className="absolute inset-0 opacity-10"
          style={{
            backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M30 0l10 10-10 10-10-10z M30 20l10 10-10 10-10-10z M30 40l10 10-10 10-10-10z M10 10l10 10-10 10-10-10z M10 30l10 10-10 10-10-10z M50 10l10 10-10 10-10-10z M50 30l10 10-10 10-10-10z' fill='white' fill-rule='evenodd'/%3E%3C/svg%3E")`,
          }}
        ></div>

        <h1
          className="relative z-10 text-center text-x3 font-semibold text-white mb-8"
          style={{
            fontFamily: '"KaiTi", "楷体", serif',
            letterSpacing: "0.2em",
          }}
        >
          个人中心
        </h1>

        <button
          onClick={onNavigateToEditProfile}
          className="relative z-10 flex items-center w-full hover:opacity-90 transition-opacity"
        >
          {/* 头像 - 金色边框圆环 */}
          <div className="relative w-20 h-20 mr-4">
            {/* 金色外圈 */}
            <div
              className="absolute inset-0 rounded-full"
              style={{
                background:
                  "linear-gradient(135deg, #FFD700 0%, #FFA500 100%)",
                padding: "3px",
              }}
            >
              {/* 白色内圈 */}
              <div className="w-full h-full rounded-full bg-white flex items-center justify-center overflow-hidden">
                <div className="text-4xl">{avatar}</div>
              </div>
            </div>
            {/* 金色光晕效果 */}
            <div
              className="absolute inset-0 rounded-full"
              style={{
                background:
                  "radial-gradient(circle, rgba(255,215,0,0.3) 0%, transparent 70%)",
                transform: "scale(1.2)",
              }}
            ></div>
          </div>

          {/* 用户信息 */}
          <div className="text-left">
            <h2
              className="text-xl font-semibold text-white mb-1"
              style={{
                fontFamily: '"KaiTi", "楷体", serif',
              }}
            >
              {username}
            </h2>
            <p
              className="text-white/80 text-sm"
              style={{
                fontFamily: '"KaiTi", "楷体", serif',
              }}
            >
              点击编辑个人信息
            </p>
          </div>
        </button>
      </div>

      {/* 功能菜单 */}
      <div className="px-4 -mt-12 relative z-10">
        <div
          className="bg-white rounded-2xl shadow-lg overflow-hidden"
          style={{ border: "1px solid #D2B48C" }}
        >
          {/* 预约记录 */}
          <button
            onClick={onNavigateToBookingRecord}
            className="w-full flex items-center justify-between px-5 py-4 hover:bg-gray-50 transition-colors"
            style={{ borderBottom: "1px solid #F5F5DC" }}
          >
            <div className="flex items-center">
              <div
                className="w-10 h-10 rounded-full flex items-center justify-center mr-3"
                style={{ background: "#FFF8DC" }}
              >
                <Calendar
                  className="w-5 h-5"
                  style={{ color: "#8B0000" }}
                />
              </div>
              <span
                style={{
                  color: "#8B4513",
                  fontFamily: '"KaiTi", "楷体", serif',
                }}
              >
                预约记录
              </span>
            </div>
            <div className="flex items-center">
              <span
                className="text-sm mr-1"
                style={{ color: "#8B4513", opacity: 0.6 }}
              >
                3条
              </span>
              <ChevronRight
                className="w-5 h-5"
                style={{ color: "#D2B48C" }}
              />
            </div>
          </button>

          {/* 预约人登记 */}
          <button
            onClick={onNavigateToVisitorRegistration}
            className="w-full flex items-center justify-between px-5 py-4 hover:bg-gray-50 transition-colors"
          >
            <div className="flex items-center">
              <div
                className="w-10 h-10 rounded-full flex items-center justify-center mr-3"
                style={{ background: "#FFE4E1" }}
              >
                <Users
                  className="w-5 h-5"
                  style={{ color: "#8B0000" }}
                />
              </div>
              <span
                style={{
                  color: "#8B4513",
                  fontFamily: '"KaiTi", "楷体", serif',
                }}
              >
                预约人登记
              </span>
            </div>
            <ChevronRight
              className="w-5 h-5"
              style={{ color: "#D2B48C" }}
            />
          </button>
        </div>

        {/* 退出登录按钮 */}
        <div className="mt-6 mb-8">
          <button
            onClick={onLogout}
            className="w-full py-4 rounded-xl font-medium shadow-lg hover:shadow-xl transition-all flex items-center justify-center text-white"
            style={{
              background:
                "linear-gradient(135deg, #8B0000 0%, #A52A2A 100%)",
              fontFamily: '"KaiTi", "楷体", serif',
              letterSpacing: "0.2em",
            }}
          >
            <LogOut className="w-5 h-5 mr-2" />
            退出登录
          </button>
        </div>

        {/* 版本信息 */}
        <p
          className="text-center text-sm pb-6"
          style={{ color: "#8B4513", opacity: 0.5 }}
        >
          当前版本：2.4.12 • 2025年12月24日
        </p>
      </div>

      {/* 底部导航栏 */}
      <div
        className="fixed bottom-0 left-0 right-0 bg-white px-4 py-3 safe-area-inset-bottom shadow-lg"
        style={{ borderTop: "1px solid #D2B48C" }}
      >
        <div className="grid grid-cols-2 gap-4 max-w-md mx-auto">
          <button
            onClick={onBack}
            className="flex flex-col items-center justify-center py-2"
          >
            <div className="w-6 h-6 mb-1 flex items-center justify-center">
              <svg
                className="w-5 h-5"
                style={{ color: "#8B4513" }}
                viewBox="0 0 24 24"
                fill="currentColor"
              >
                <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z" />
              </svg>
            </div>
            <span
              className="text-xs"
              style={{ color: "#8B4513" }}
            >
              首页
            </span>
          </button>
          <button className="flex flex-col items-center justify-center py-2">
            <div
              className="w-6 h-6 mb-1 rounded-full flex items-center justify-center"
              style={{ background: "#8B0000" }}
            >
              <svg
                className="w-4 h-4 text-white"
                viewBox="0 0 24 24"
                fill="currentColor"
              >
                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
              </svg>
            </div>
            <span
              className="text-xs font-medium"
              style={{ color: "#8B0000" }}
            >
              我的
            </span>
          </button>
        </div>
      </div>
    </div>
  );
}