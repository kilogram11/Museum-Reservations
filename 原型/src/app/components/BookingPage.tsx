import { useState } from 'react';
import { Check, User, Lock, AlertTriangle } from 'lucide-react';
import { toast } from 'sonner';

interface BookingPageProps {
  onBack: () => void;
  onBookingSuccess: () => void;
}

export default function BookingPage({ onBack, onBookingSuccess }: BookingPageProps) {
  const [selectedDate, setSelectedDate] = useState('24');
  const [selectedTime, setSelectedTime] = useState('12:00—14:00');
  const [visitors, setVisitors] = useState([
    { id: 1, name: '张建国', phone: '110101198001011234', selected: true, blacklisted: false },
    { id: 2, name: '李明', phone: '110101199001011235', selected: false, blacklisted: false },
    { id: 3, name: '王芳', phone: '110101198501011236', selected: false, blacklisted: true },
  ]);

  const toggleVisitor = (id: number) => {
    // 黑名单用户不可选择
    const visitor = visitors.find(v => v.id === id);
    if (visitor?.blacklisted) return;
    
    setVisitors(visitors.map(v => 
      v.id === id ? { ...v, selected: !v.selected } : v
    ));
  };

  const selectedCount = visitors.filter(v => v.selected).length;

  const dates = [
    { date: '24', day: '周四', month: '12月', capacity: '剩余200位', available: true },
    { date: '25', day: '周四', month: '', capacity: '剩余90位', available: true },
    { date: '26', day: '周五', month: '', capacity: '已满，不可预约', available: false },
    { date: '27', day: '周六', month: '', capacity: '已满，不可预约', available: false },
    { date: '28', day: '周日', month: '', capacity: '已满，不可预约', available: false },
  ];

  const timeSlots = [
    { time: '10:00—12:00', capacity: '剩余100位', available: true },
    { time: '12:00—14:00', capacity: '剩余45位', available: true },
    { time: '14:00—16:00', capacity: '剩余55位', available: true },
    { time: '16:00—18:00', capacity: '已满，不可预约', available: false },
  ];

  const handleBooking = () => {
    // 显示预约成功提示
    toast.success('预约成功！', {
      duration: 1500,
    });
    
    // 延迟后跳转到预约记录页面
    setTimeout(() => {
      onBookingSuccess();
    }, 1500);
  };

  return (
    <div className="h-screen overflow-y-auto bg-gray-50">
      {/* 顶部导航栏 */}
      <div className="bg-white px-4 py-4 flex items-center shadow-sm sticky top-0 z-10">
        <button onClick={onBack} className="mr-4">
          <svg className="w-6 h-6 text-gray-800" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <h1 className="text-lg font-semibold text-gray-900">参观预约</h1>
      </div>

      <div className="p-4 space-y-4 pb-24">
        {/* 博物馆卡片 */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 bg-indigo-50 rounded-full flex items-center justify-center">
              <svg className="w-7 h-7 text-indigo-600" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-3zm0 2.18l7 2.62v4.7c0 4.83-3.13 9.37-7 10.5-3.87-1.13-7-5.67-7-10.5V6.8l7-2.62z"/>
                <path d="M6 9h12v1H6zm0 2h12v1H6zm0 2h12v1H6zm0 2h12v1H6z"/>
              </svg>
            </div>
            <div>
              <h2 className="text-base font-semibold text-gray-900">国家博物馆</h2>
              <p className="text-xs text-gray-500">当前选择：2025年12月24日</p>
            </div>
          </div>
        </div>

        {/* 选择参观日期 */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-gray-900">选择参观日期</h3>
            <span className="text-xs text-blue-600">12月</span>
          </div>
          <div className="flex gap-2 overflow-x-auto pb-2">
            {dates.map((item) => (
              <button
                key={item.date}
                onClick={() => item.available && setSelectedDate(item.date)}
                disabled={!item.available}
                className={`flex-shrink-0 w-16 rounded-xl p-3 text-center transition-all ${
                  selectedDate === item.date && item.available
                    ? 'bg-[#1a237e] text-white shadow-lg scale-105'
                    : item.available
                    ? 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                    : 'bg-gray-50 text-gray-400 cursor-not-allowed'
                }`}
              >
                {item.month && <div className="text-[10px] mb-1">{item.month}</div>}
                <div className="text-2xl font-bold mb-1">{item.date}</div>
                <div className="text-[10px]">{item.day}</div>
                <div className={`text-[9px] mt-1 ${!item.available ? 'text-red-500' : ''}`}>
                  {item.capacity}
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* 选择入场时段 */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">选择入场时段</h3>
          <div className="space-y-2">
            {timeSlots.map((slot) => (
              <button
                key={slot.time}
                onClick={() => slot.available && setSelectedTime(slot.time)}
                disabled={!slot.available}
                className={`w-full rounded-xl p-4 text-center transition-all flex items-center justify-center ${
                  selectedTime === slot.time && slot.available
                    ? 'bg-[#1a237e] text-white shadow-lg'
                    : slot.available
                    ? 'bg-gray-50 text-gray-800 hover:bg-gray-100'
                    : 'bg-gray-50 text-gray-400 cursor-not-allowed'
                }`}
              >
                <span className="font-semibold">{slot.time}</span>
                <span className="ml-auto text-sm">
                  {slot.available ? slot.capacity : (
                    <span className="text-red-500 text-xs">已满，不可预约</span>
                  )}
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* 警告提示 */}
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 flex items-start gap-2">
          <AlertTriangle className="w-4 h-4 text-amber-600 flex-shrink-0 mt-0.5" />
          <p className="text-xs text-amber-800">
            请注意：一个账号最多可预约3人，入馆请携带身份证备查。
          </p>
        </div>

        {/* 选择访客 */}
        <div className="bg-white rounded-2xl p-4 shadow-sm">
          <h3 className="text-sm font-semibold text-gray-900 mb-3">选择访客（{selectedCount}/3）</h3>
          
          {/* 访客列表 */}
          {visitors.map((visitor) => (
            <div key={visitor.id} className="mb-3">
              <button
                onClick={() => toggleVisitor(visitor.id)}
                disabled={visitor.blacklisted}
                className={`w-full rounded-xl p-4 flex items-center justify-between transition-all ${
                  visitor.blacklisted
                    ? 'border-2 border-gray-200 bg-gray-100 cursor-not-allowed opacity-60'
                    : visitor.selected 
                    ? 'border-2 border-[#1a237e] bg-white' 
                    : 'border-2 border-gray-200 bg-gray-50 hover:border-gray-300'
                }`}
              >
                <div className="flex items-center gap-3">
                  <User className={`w-5 h-5 ${visitor.blacklisted ? 'text-gray-400' : visitor.selected ? 'text-gray-600' : 'text-gray-400'}`} />
                  <div className="text-left">
                    <div className={`font-semibold ${visitor.blacklisted ? 'text-gray-500' : visitor.selected ? 'text-gray-900' : 'text-gray-600'}`}>
                      {visitor.name}
                    </div>
                    <div className="text-xs text-gray-500">{visitor.phone}</div>
                  </div>
                </div>
                <div className={`w-6 h-6 rounded-full flex items-center justify-center ${
                  visitor.blacklisted
                    ? 'border-2 border-gray-300'
                    : visitor.selected 
                    ? 'bg-[#1a237e]' 
                    : 'border-2 border-gray-300'
                }`}>
                  {visitor.selected && !visitor.blacklisted && <Check className="w-4 h-4 text-white" />}
                </div>
              </button>
              {visitor.blacklisted && (
                <p className="text-xs text-red-600 mt-1 ml-4">
                  该用户近期有逾期行为，已被拉入黑名单，不可预约
                </p>
              )}
            </div>
          ))}
        </div>

        {/* 底部操作栏 */}
        <div className="bg-white rounded-2xl p-4 shadow-sm flex items-center justify-between">
          <div>
            <div className="text-xs text-gray-500">已选中者</div>
            <div className="text-xl font-bold text-gray-900">共 {selectedCount} 人</div>
          </div>
          <button
            onClick={handleBooking}
            className="bg-[#1a237e] text-white px-8 py-3 rounded-full font-semibold shadow-lg hover:bg-[#0d1642] transition-colors"
          >
            立即预约
          </button>
        </div>
      </div>
    </div>
  );
}