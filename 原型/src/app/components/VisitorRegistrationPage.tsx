import { useState } from "react";
import { AlertTriangle, X } from "lucide-react";
import { toast } from "sonner";

interface VisitorRegistrationPageProps {
  onBack: () => void;
}

interface Visitor {
  id: number;
  name: string;
  phone: string;
  idCard: string;
  hasWarning?: boolean;
  warningMessage?: string;
}

export default function VisitorRegistrationPage({
  onBack,
}: VisitorRegistrationPageProps) {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [idCard, setIdCard] = useState("");

  // 编辑弹窗状态
  const [showEditModal, setShowEditModal] = useState(false);
  const [editingVisitor, setEditingVisitor] =
    useState<Visitor | null>(null);
  const [editName, setEditName] = useState("");
  const [editPhone, setEditPhone] = useState("");
  const [editIdCard, setEditIdCard] = useState("");

  // 删除确认弹窗
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deletingVisitorId, setDeletingVisitorId] = useState<
    number | null
  >(null);

  const [visitors, setVisitors] = useState<Visitor[]>([
    {
      id: 1,
      name: "张三",
      phone: "138****5678",
      idCard: "320102**********1234",
    },
    {
      id: 2,
      name: "李四",
      phone: "139****9012",
      idCard: "110101**********5678",
    },
    {
      id: 3,
      name: "王五",
      phone: "136****3456",
      idCard: "440103**********9012",
      hasWarning: true,
      warningMessage: "该预约人已被拉入和名单",
    },
  ]);

  const handleAddVisitor = () => {
    if (!name || !phone || !idCard) {
      toast.error("请填写完整信息");
      return;
    }

    // 显示成功提示
    toast.success("添加成功", {
      duration: 2000,
    });

    // 清空表单
    setName("");
    setPhone("");
    setIdCard("");
  };

  // 打开编辑弹窗
  const handleEditClick = (visitor: Visitor) => {
    setEditingVisitor(visitor);
    setEditName(visitor.name);
    setEditPhone(visitor.phone);
    setEditIdCard(visitor.idCard);
    setShowEditModal(true);
  };

  // 确认修改
  const handleConfirmEdit = () => {
    if (!editName || !editPhone || !editIdCard) {
      toast.error("请填写完整信息");
      return;
    }

    // 更新访客信息
    setVisitors(
      visitors.map((v) =>
        v.id === editingVisitor?.id
          ? {
              ...v,
              name: editName,
              phone: editPhone,
              idCard: editIdCard,
            }
          : v,
      ),
    );

    toast.success("修改成功", {
      duration: 2000,
    });

    setShowEditModal(false);
    setEditingVisitor(null);
  };

  // 打开删除确认弹窗
  const handleDeleteClick = (visitorId: number) => {
    setDeletingVisitorId(visitorId);
    setShowDeleteModal(true);
  };

  // 确认删除
  const handleConfirmDelete = () => {
    setVisitors(
      visitors.filter((v) => v.id !== deletingVisitorId),
    );

    toast.success("删除成功", {
      duration: 2000,
    });

    setShowDeleteModal(false);
    setDeletingVisitorId(null);
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
          预约人登记
        </h1>
      </div>

      <div className="p-4">
        {/* 新增预约人表单 */}
        <div className="bg-white rounded-2xl p-5 shadow-sm mb-6">
          <h3 className="text-base font-semibold text-gray-900 mb-4">
            新增预约人
          </h3>

          <div className="space-y-3">
            <input
              type="text"
              placeholder="请输入姓名"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-3 bg-gray-50 rounded-lg border border-transparent focus:border-blue-500 focus:bg-white outline-none transition-colors"
            />

            <input
              type="tel"
              placeholder="请输入11位手机号"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              maxLength={11}
              className="w-full px-4 py-3 bg-gray-50 rounded-lg border border-transparent focus:border-blue-500 focus:bg-white outline-none transition-colors"
            />

            <input
              type="text"
              placeholder="请输入18位身份证号"
              value={idCard}
              onChange={(e) => setIdCard(e.target.value)}
              maxLength={18}
              className="w-full px-4 py-3 bg-gray-50 rounded-lg border border-transparent focus:border-blue-500 focus:bg-white outline-none transition-colors"
            />

            <button
              onClick={handleAddVisitor}
              className="w-full bg-blue-500 text-white py-3 rounded-lg font-medium hover:bg-blue-600 transition-colors"
            >
              确认添加
            </button>
          </div>
        </div>

        {/* 已登记的人 */}
        <div className="bg-white rounded-2xl p-5 shadow-sm">
          <h3 className="text-base font-semibold text-gray-900 mb-4">
            已登记的人（{visitors.length}人）
          </h3>

          <div className="space-y-3">
            {visitors.map((visitor) => (
              <div
                key={visitor.id}
                className="border border-gray-200 rounded-xl p-4"
              >
                <div className="flex items-start justify-between mb-2">
                  <div className="flex-1">
                    <h4 className="font-semibold text-gray-900 mb-1">
                      {visitor.name}
                    </h4>
                    <p className="text-sm text-gray-500">
                      {visitor.phone}
                    </p>
                    <p className="text-sm text-gray-500">
                      {visitor.idCard}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => handleEditClick(visitor)}
                      className="text-blue-500 text-sm px-3 py-1"
                    >
                      修改
                    </button>
                    <button
                      onClick={() =>
                        handleDeleteClick(visitor.id)
                      }
                      className="text-red-500 text-sm px-3 py-1"
                    >
                      删除
                    </button>
                  </div>
                </div>

                {visitor.hasWarning && (
                  <div className="flex items-start gap-2 mt-2 text-red-500 text-xs">
                    <AlertTriangle className="w-4 h-4 flex-shrink-0 mt-0.5" />
                    <span>{visitor.warningMessage}</span>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* 编辑弹窗 */}
      {showEditModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-sm">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold text-gray-900">
                预约人信息修改
              </h3>
              <button
                onClick={() => setShowEditModal(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-6 h-6" />
              </button>
            </div>

            <div className="space-y-4">
              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  姓名
                </label>
                <input
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  电话号码
                </label>
                <input
                  type="tel"
                  value={editPhone}
                  onChange={(e) => setEditPhone(e.target.value)}
                  maxLength={11}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none"
                />
              </div>

              <div>
                <label className="block text-sm text-gray-700 mb-2">
                  身份证号
                </label>
                <input
                  type="text"
                  value={editIdCard}
                  onChange={(e) =>
                    setEditIdCard(e.target.value)
                  }
                  maxLength={18}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none"
                />
              </div>

              <button
                onClick={handleConfirmEdit}
                className="w-full bg-blue-500 text-white py-3 rounded-lg font-medium hover:bg-blue-600 transition-colors mt-6"
              >
                确认修改
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 删除确认弹窗 */}
      {showDeleteModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-sm">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">
              确认删除
            </h3>
            <p className="text-gray-600 mb-6">
              确定要删除该预约人信息吗？此操作无法撤销。
            </p>

            <div className="flex gap-3">
              <button
                onClick={() => setShowDeleteModal(false)}
                className="flex-1 py-3 border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition-colors"
              >
                取消
              </button>
              <button
                onClick={handleConfirmDelete}
                className="flex-1 py-3 bg-red-500 text-white rounded-lg font-medium hover:bg-red-600 transition-colors"
              >
                确认删除
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}