import { useState } from 'react';
import { X } from 'lucide-react';
import { toast } from 'sonner';

interface BookingRecordPageProps {
  onBack: () => void;
}

interface BookingRecord {
  id: number;
  name: string;
  date: string;
  time: string;
  status: 'completed' | 'pending' | 'expired' | 'verified';
  statusText: string;
  statusColor: string;
  borderColor?: string;
}

export default function BookingRecordPage({ onBack }: BookingRecordPageProps) {
  const [showQRCode, setShowQRCode] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState<BookingRecord | null>(null);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [cancelingRecordId, setCancelingRecordId] = useState<number | null>(null);

  const [records, setRecords] = useState<BookingRecord[]>([
    {
      id: 1,
      name: '张三',
      date: '2024年1月15日',
      time: '上午 09:00-11:00',
      status: 'completed',
      statusText: '已核销',
      statusColor: 'bg-blue-100 text-blue-600',
      borderColor: 'border-l-blue-500',
    },
    {
      id: 2,
      name: '李四',
      date: '2024年1月20日',
      time: '下午 14:00-16:00',
      status: 'pending',
      statusText: '待核销',
      statusColor: 'bg-cyan-100 text-cyan-600',
      borderColor: 'border-l-cyan-500',
    },
    {
      id: 3,
      name: '王五',
      date: '2024年1月18日',
      time: '上午 10:00-12:00',
      status: 'pending',
      statusText: '待核销',
      statusColor: 'bg-cyan-100 text-cyan-600',
      borderColor: 'border-l-cyan-500',
    },
    {
      id: 4,
      name: '赵六',
      date: '2024年1月10日',
      time: '下午 15:00-17:00',
      status: 'expired',
      statusText: '已逾期',
      statusColor: 'bg-gray-100 text-gray-600',
      borderColor: 'border-l-gray-400',
    },
    {
      id: 5,
      name: '钱七',
      date: '2024年1月12日',
      time: '上午 09:30-11:30',
      status: 'verified',
      statusText: '已核销',
      statusColor: 'bg-green-100 text-green-600',
      borderColor: 'border-l-green-500',
    },
  ]);

  const handleShowQRCode = (record: BookingRecord) => {
    setSelectedRecord(record);
    setShowQRCode(true);
  };

  const handleCancelClick = (recordId: number) => {
    setCancelingRecordId(recordId);
    setShowCancelModal(true);
  };

  const handleConfirmCancel = () => {
    // 删除预约记录
    setRecords(records.filter(r => r.id !== cancelingRecordId));
    
    toast.success('取消预约成功', {
      duration: 2000,
    });
    
    setShowCancelModal(false);
    setCancelingRecordId(null);
  };

  return (
    <div className="min-h-screen bg-gray-50 overflow-y-auto">
      {/* 顶部导航栏 */}
      <div className="bg-white px-4 py-4 flex items-center shadow-sm sticky top-0 z-10">
        <button onClick={onBack} className="mr-4">
          <svg className="w-6 h-6 text-gray-800" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h1 className="text-lg font-semibold text-gray-900">预约记录</h1>
      </div>

      {/* 预约记录列表 */}
      <div className="p-4 space-y-4">
        {records.map((record) => (
          <div
            key={record.id}
            className={`bg-white rounded-2xl p-4 shadow-sm border-l-4 ${record.borderColor}`}
          >
            <div className="flex items-start justify-between mb-3">
              <div className="flex-1">
                <h3 className="font-semibold text-gray-900 text-lg mb-2">{record.name}</h3>
                <p className="text-sm text-gray-600">{record.date}</p>
                <p className="text-sm text-gray-600">{record.time}</p>
              </div>
              <span className={`px-3 py-1 rounded-full text-xs font-medium ${record.statusColor}`}>
                {record.statusText}
              </span>
            </div>

            {/* 待核销状态显示操作按钮 */}
            {record.status === 'pending' && (
              <div className="flex gap-3 mt-4">
                <button
                  onClick={() => handleShowQRCode(record)}
                  className="flex-1 bg-green-500 text-white py-2.5 rounded-lg font-medium hover:bg-green-600 transition-colors"
                >
                  显示预约二维码
                </button>
                <button
                  onClick={() => handleCancelClick(record.id)}
                  className="flex-1 border-2 border-red-500 text-red-500 py-2.5 rounded-lg font-medium hover:bg-red-50 transition-colors"
                >
                  取消预约
                </button>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* 二维码弹窗 */}
      {showQRCode && selectedRecord && (
        <div 
          className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4"
          onClick={() => setShowQRCode(false)}
        >
          <div 
            className="bg-white rounded-2xl p-6 w-full max-w-sm"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold text-gray-900">预约二维码</h3>
              <button 
                onClick={() => setShowQRCode(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="w-6 h-6" />
              </button>
            </div>

            <div className="text-center mb-6">
              <p className="text-sm text-gray-600 mb-2">{selectedRecord.name}</p>
              <p className="text-sm text-gray-600 mb-4">
                {selectedRecord.date} {selectedRecord.time}
              </p>

              {/* 二维码占位 */}
              <div className="w-64 h-64 mx-auto bg-gray-100 rounded-lg flex items-center justify-center mb-4">
                <div className="w-48 h-48 bg-white border-4 border-gray-900 flex items-center justify-center">
                  <div className="text-center">
                    <div className="text-6xl mb-2">📱</div>
                    <p className="text-xs text-gray-500">预约二维码</p>
                  </div>
                </div>
              </div>

              <p className="text-xs text-gray-500">请在入场时出示此二维码</p>
            </div>

            <button
              onClick={() => setShowQRCode(false)}
              className="w-full bg-blue-500 text-white py-3 rounded-lg font-medium hover:bg-blue-600 transition-colors"
            >
              关闭
            </button>
          </div>
        </div>
      )}

      {/* 取消预约确认弹窗 */}
      {showCancelModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl p-6 w-full max-w-sm">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">确认取消预约</h3>
            <p className="text-gray-600 mb-6">确定要取消此预约吗？取消后将无法恢复。</p>
            
            <div className="flex gap-3">
              <button
                onClick={() => setShowCancelModal(false)}
                className="flex-1 py-3 border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition-colors"
              >
                返回
              </button>
              <button
                onClick={handleConfirmCancel}
                className="flex-1 py-3 bg-red-500 text-white rounded-lg font-medium hover:bg-red-600 transition-colors"
              >
                确认取消
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}