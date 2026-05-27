import { MoreHorizontal, Calendar } from 'lucide-react';

interface AnnouncementDetailPageProps {
  announcementId: number;
  onBack: () => void;
}

export default function AnnouncementDetailPage({ announcementId, onBack }: AnnouncementDetailPageProps) {
  return (
    <div className="h-screen overflow-y-auto bg-white">
      {/* 顶部导航栏 */}
      <div className="sticky top-0 z-10 bg-white border-b border-gray-100">
        <div className="max-w-md mx-auto px-4 py-3 flex items-center justify-between">
          <button onClick={onBack}>
            <svg className="w-6 h-6 text-gray-800" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
          <button>
            <MoreHorizontal className="w-6 h-6 text-gray-800" />
          </button>
        </div>
      </div>

      {/* 文章内容 */}
      <div className="px-5 py-6">
        {/* 标题 */}
        <h1 className="mb-6">
          春节期间开放时间调整通知
        </h1>

        {/* 元信息 */}
        <div className="text-gray-500 mb-6 pb-4 border-b border-gray-100">
          <span>博物馆管理处</span>
          <span className="mx-2">·</span>
          <span>2024年2月1日</span>
          <span className="mx-2">·</span>
          <span>公告通知</span>
        </div>

        {/* 正文内容 - 纯文字 */}
        <div className="space-y-4 text-gray-700 leading-relaxed">
          <p>
            尊敬的游客朋友们：
          </p>

          <p>
            为配合春节假期安排，更好地为广大游客提供参观服务，我馆特对春节期间的开放时间做出如下调整：
          </p>

          <p>
            开放时间调整
          </p>

          <p>
            调整日期：2024年2月10日至2月17日（春节假期）
          </p>
          
          <p>
            开放时间：每日08:30 - 19:00（延长2小时）
          </p>
          
          <p>
            停止入场：18:00
          </p>
          
          <p>
            闭馆时间：除夕（2月9日）全天闭馆
          </p>

          <p>
            春节期间，我馆将正常开放，届时将有精彩的新春特展等您来参观。为保证良好的参观体验，建议您提前通过官方渠道进行预约。
          </p>

          <p>
            温馨提示：
          </p>
          
          <p>
            1. 春节期间参观人数较多，请提前预约
          </p>
          
          <p>
            2. 入馆时请主动出示预约二维码及有效证件
          </p>
          
          <p>
            3. 请自觉遵守参观秩序，文明观展
          </p>
          
          <p>
            4. 馆内禁止吸烟，请爱护文物展品
          </p>

          <p>
            感谢您对我馆工作的理解与支持，恭祝您新春快乐，阖家幸福！如有疑问，请致电咨询热线：010-8500 7421
          </p>

          <p className="text-right">
            博物馆管理处
          </p>
          
          <p className="text-right">
            2024年2月1日
          </p>
        </div>
      </div>
    </div>
  );
}
