import { Calendar } from "lucide-react";

interface AnnouncementPageProps {
  onBack: () => void;
  onAnnouncementClick: (announcementId: number) => void;
}

interface Announcement {
  id: number;
  tag: string;
  tagColor: string;
  title: string;
  content: string;
  date: string;
}

export default function AnnouncementPage({
  onBack,
  onAnnouncementClick,
}: AnnouncementPageProps) {
  const announcements: Announcement[] = [
    {
      id: 1,
      tag: "紧急",
      tagColor: "bg-red-50 text-red-600 border-red-200",
      title: "春节期间开放时间调整通知",
      content:
        "尊敬的游客朋友们，为配合春节假期安排，我馆于2月10日-2月17日延长开放时间至19:00，届时大家欢迎前来参观。",
      date: "2024年2月1日",
    },
    {
      id: 2,
      tag: "通知",
      tagColor: "bg-blue-50 text-blue-600 border-blue-200",
      title: "3月份临时容装制时段参观",
      content:
        '为确保参观体验，自3月1日起全面实行分时段预约参观制度。请游客朋友们提前在"我馆官网"或小程序上进行预约，人数预计达人数。',
      date: "2024年2月20日",
    },
    {
      id: 3,
      tag: "活动",
      tagColor: "bg-green-50 text-green-600 border-green-200",
      title: "文物保护主题讲座将于开启",
      content:
        '日常清洁文物修复专业课程"博物馆保护与保修技术"专题讲座将于3月8日在我馆举办，感兴趣的朋友欢迎参加，届时将邀请3月1日至下午2点。',
      date: "2024年2月25日",
    },
    {
      id: 4,
      tag: "新闻",
      tagColor:
        "bg-purple-50 text-purple-600 border-purple-200",
      title: "在我馆举办新东韩越郊公营",
      content:
        "为庆祝成为中央，我馆将于3月5日至3月22日在关闭在B区00进行升级改造，期间可能影响您的观展体验，敬请谅解。",
      date: "2024年2月8日",
    },
    {
      id: 5,
      tag: "展览",
      tagColor:
        "bg-yellow-50 text-yellow-700 border-yellow-200",
      title: "数据艺术展延期至5月底",
      content:
        '由于"大家欢迎参观"4月期限时，观看量不断攀升，应广大观众要求，决定将展期延长至5月31日，欢迎大家持续前来观赏。',
      date: "2024年3月15日",
    },
  ];

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
          公告通知
        </h1>
      </div>

      {/* 公告列表 */}
      <div className="p-4 space-y-3">
        {announcements.map((announcement) => (
          <div
            key={announcement.id}
            onClick={() => onAnnouncementClick(announcement.id)}
            className="bg-white rounded-2xl p-4 shadow-sm hover:shadow-md transition-shadow cursor-pointer"
          >
            {/* 标签 */}
            <div className="mb-2"></div>

            {/* 标题 */}
            <h3 className="font-semibold text-gray-900 mb-2">
              {announcement.title}
            </h3>

            {/* 内容 */}
            <p className="text-sm text-gray-600 leading-relaxed mb-3">
              {announcement.content}
            </p>

            {/* 日期 */}
            <div className="flex items-center text-xs text-gray-400">
              <Calendar className="w-3.5 h-3.5 mr-1" />
              {announcement.date}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}