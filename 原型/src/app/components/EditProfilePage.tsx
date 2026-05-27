import { useState } from "react";
import {
  User,
  Phone,
  Lock,
  Eye,
  EyeOff,
  X,
} from "lucide-react";
import { toast } from "sonner";

interface EditProfilePageProps {
  onBack: () => void;
  currentUsername: string;
  onSaveProfile: (profile: UserProfile) => void;
}

export interface UserProfile {
  username: string;
  gender: "male" | "female";
  phone: string;
  avatar: string;
}

export default function EditProfilePage({
  onBack,
  currentUsername,
  onSaveProfile,
}: EditProfilePageProps) {
  const [username, setUsername] = useState(currentUsername);
  const [gender, setGender] = useState<"male" | "female">(
    "male",
  );
  const [phone, setPhone] = useState("138****5678");
  const [avatar, setAvatar] = useState("👨");

  // 修改密码相关
  const [showPasswordModal, setShowPasswordModal] =
    useState(false);
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmNewPassword, setConfirmNewPassword] =
    useState("");
  const [showOldPassword, setShowOldPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] =
    useState(false);

  const avatarOptions = [
    "👨",
    "👩",
    "👦",
    "👧",
    "🧑",
    "👴",
    "👵",
    "🧔",
    "👨‍💼",
    "👩‍💼",
    "👨‍🎓",
    "👩‍🎓",
  ];

  const handleSave = () => {
    if (!username) {
      toast.error("请输入用户名");
      return;
    }

    if (!phone) {
      toast.error("请输入手机号");
      return;
    }

    if (phone.length !== 11) {
      toast.error("请输入11位手机号");
      return;
    }

    // 保存个人信息
    onSaveProfile({
      username,
      gender,
      phone,
      avatar,
    });

    toast.success("个人信息修改成功！", {
      duration: 2000,
    });

    setTimeout(() => {
      onBack();
    }, 2000);
  };

  const handleChangePassword = () => {
    if (!oldPassword) {
      toast.error("请输入原密码");
      return;
    }

    if (!newPassword) {
      toast.error("请输入新密码");
      return;
    }

    if (newPassword.length < 6) {
      toast.error("新密码长度至少6位");
      return;
    }

    if (!confirmNewPassword) {
      toast.error("请确认新密码");
      return;
    }

    if (newPassword !== confirmNewPassword) {
      toast.error("两次密码输入不一致");
      return;
    }

    // 模拟验证原密码
    toast.success("密码修改成功！", {
      duration: 2000,
    });

    setShowPasswordModal(false);
    setOldPassword("");
    setNewPassword("");
    setConfirmNewPassword("");
  };

  return (
    <div className="min-h-screen bg-gray-50 overflow-y-auto">
      {/* 顶部导航栏 */}
      <div className="bg-white px-4 py-4 flex items-center shadow-sm sticky top-0 z-10">
        <button onClick={onBack} className="mr-4">
          <svg
            className="w-6 h-6 text-gray-800"
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
        <h1 className="text-lg font-semibold text-gray-900">
          编辑个人信息
        </h1>
      </div>

      <div className="p-4 pb-24">
        {/* 头像选择 */}
        <div className="bg-white rounded-2xl p-5 shadow-sm mb-4">
          <h3 className="text-base font-semibold text-gray-900 mb-4">
            头像
          </h3>
          <div className="grid grid-cols-6 gap-3">
            {avatarOptions.map((ava, index) => (
              <button
                key={index}
                onClick={() => setAvatar(ava)}
                className={`w-14 h-14 rounded-full flex items-center justify-center text-2xl transition-all ${
                  avatar === ava
                    ? "bg-blue-50 ring-2 ring-blue-500 scale-110"
                    : "bg-gray-100 hover:bg-gray-200"
                }`}
              >
                {ava}
              </button>
            ))}
          </div>
        </div>

        {/* 基本信息 */}
        <div className="bg-white rounded-2xl p-5 shadow-sm mb-4">
          <h3 className="text-base font-semibold text-gray-900 mb-4">
            基本信息
          </h3>

          <div className="space-y-4">
            {/* 用户名 */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                用户名
              </label>
              <div className="flex items-center bg-gray-50 rounded-lg px-4 py-3 border border-gray-200 focus-within:border-blue-500 focus-within:ring-1 focus-within:ring-blue-500">
                <User className="w-5 h-5 text-gray-400 mr-3" />
                <input
                  type="text"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className="flex-1 bg-transparent outline-none text-gray-900"
                />
              </div>
            </div>

            {/* 性别 */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                性别
              </label>
              <div className="flex gap-3">
                <button
                  onClick={() => setGender("male")}
                  className={`flex-1 py-3 rounded-lg font-medium transition-all ${
                    gender === "male"
                      ? "bg-blue-500 text-white shadow-md"
                      : "bg-gray-100 text-gray-700 border border-gray-200"
                  }`}
                >
                  👨 男
                </button>
                <button
                  onClick={() => setGender("female")}
                  className={`flex-1 py-3 rounded-lg font-medium transition-all ${
                    gender === "female"
                      ? "bg-blue-500 text-white shadow-md"
                      : "bg-gray-100 text-gray-700 border border-gray-200"
                  }`}
                >
                  👩 女
                </button>
              </div>
            </div>

            {/* 手机号 */}
            <div>
              <label className="block text-sm text-gray-700 mb-2">
                手机号
              </label>
              <div className="flex items-center bg-gray-50 rounded-lg px-4 py-3 border border-gray-200 focus-within:border-blue-500 focus-within:ring-1 focus-within:ring-blue-500">
                <Phone className="w-5 h-5 text-gray-400 mr-3" />
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  maxLength={11}
                  className="flex-1 bg-transparent outline-none text-gray-900"
                />
              </div>
            </div>
          </div>
        </div>

        {/* 修改密码 */}
        <div className="bg-white rounded-2xl p-5 shadow-sm mb-4">
          <h3 className="text-base font-semibold text-gray-900 mb-4">
            账号安全
          </h3>
          <button
            onClick={() => setShowPasswordModal(true)}
            className="w-full flex items-center justify-between px-4 py-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors"
          >
            <div className="flex items-center">
              <Lock className="w-5 h-5 text-gray-600 mr-3" />
              <span className="text-gray-800">修改密码</span>
            </div>
            <svg
              className="w-5 h-5 text-gray-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 5l7 7-7 7"
              />
            </svg>
          </button>
        </div>

        {/* 保存按钮 */}
        <button
          onClick={handleSave}
          className="w-full bg-blue-500 text-white py-4 rounded-full font-semibold shadow-lg hover:bg-blue-600 transition-colors"
        >
          保存修改
        </button>
      </div>

      {/* 修改密码弹窗 */}
      {showPasswordModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-sm max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold text-gray-900">
                修改密码
              </h3>
              <button
                onClick={() => setShowPasswordModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-6 h-6" />
              </button>
            </div>

            <div className="space-y-4">
              {/* 原密码 */}
              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  原密码
                </label>
                <div className="flex items-center bg-gray-50 rounded-lg px-4 py-3 border border-gray-200">
                  <Lock className="w-5 h-5 text-gray-400 mr-3" />
                  <input
                    type={showOldPassword ? "text" : "password"}
                    value={oldPassword}
                    onChange={(e) =>
                      setOldPassword(e.target.value)
                    }
                    placeholder="请输入原密码"
                    className="flex-1 bg-transparent outline-none text-gray-900"
                  />
                  <button
                    onClick={() =>
                      setShowOldPassword(!showOldPassword)
                    }
                    className="text-gray-400 hover:text-gray-600"
                  >
                    {showOldPassword ? (
                      <EyeOff className="w-5 h-5" />
                    ) : (
                      <Eye className="w-5 h-5" />
                    )}
                  </button>
                </div>
              </div>

              {/* 新密码 */}
              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  新密码
                </label>
                <div className="flex items-center bg-gray-50 rounded-lg px-4 py-3 border border-gray-200">
                  <Lock className="w-5 h-5 text-gray-400 mr-3" />
                  <input
                    type={showNewPassword ? "text" : "password"}
                    value={newPassword}
                    onChange={(e) =>
                      setNewPassword(e.target.value)
                    }
                    placeholder="请输入新密码（至少6位）"
                    className="flex-1 bg-transparent outline-none text-gray-900"
                  />
                  <button
                    onClick={() =>
                      setShowNewPassword(!showNewPassword)
                    }
                    className="text-gray-400 hover:text-gray-600"
                  >
                    {showNewPassword ? (
                      <EyeOff className="w-5 h-5" />
                    ) : (
                      <Eye className="w-5 h-5" />
                    )}
                  </button>
                </div>
              </div>

              {/* 确认新密码 */}
              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  确认新密码
                </label>
                <div className="flex items-center bg-gray-50 rounded-lg px-4 py-3 border border-gray-200">
                  <Lock className="w-5 h-5 text-gray-400 mr-3" />
                  <input
                    type={
                      showConfirmPassword ? "text" : "password"
                    }
                    value={confirmNewPassword}
                    onChange={(e) =>
                      setConfirmNewPassword(e.target.value)
                    }
                    placeholder="请再次输入新密码"
                    className="flex-1 bg-transparent outline-none text-gray-900"
                  />
                  <button
                    onClick={() =>
                      setShowConfirmPassword(
                        !showConfirmPassword,
                      )
                    }
                    className="text-gray-400 hover:text-gray-600"
                  >
                    {showConfirmPassword ? (
                      <EyeOff className="w-5 h-5" />
                    ) : (
                      <Eye className="w-5 h-5" />
                    )}
                  </button>
                </div>
              </div>

              <button
                onClick={handleChangePassword}
                className="w-full bg-blue-500 text-white py-3 rounded-lg font-medium hover:bg-blue-600 transition-colors mt-6"
              >
                确认修改
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}