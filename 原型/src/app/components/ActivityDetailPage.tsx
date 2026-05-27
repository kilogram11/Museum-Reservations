import { MoreHorizontal, Users } from "lucide-react";

interface ActivityDetailPageProps {
  activityId: number;
  onBack: () => void;
}

export default function ActivityDetailPage({
  activityId,
  onBack,
}: ActivityDetailPageProps) {
  return (
    <div className="h-screen overflow-y-auto bg-white">
      {/* 顶部导航栏 */}
      <div className="sticky top-0 z-10 bg-white border-b border-gray-100">
        <div className="max-w-md mx-auto px-4 py-3 flex items-center justify-between">
          <button onClick={onBack}>
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
                d="M6 18L18 6M6 6l12 12"
              />
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
          艺术@苏美 | 萌"鹿"登场，画中有"韵"——苏绣体验活动
        </h1>

        {/* 元信息 */}
        <div className="text-gray-500 mb-6 pb-4 border-b border-gray-100">
          <span>苏州美术馆</span>
          <span className="mx-2">·</span>
          <span>2024年11月20日 14:22</span>
          <span className="mx-2">·</span>
          <span>浙江</span>
        </div>

        {/* 顶部图片 */}
        <div className="mb-6">
          <div className="w-full h-48 rounded-lg overflow-hidden bg-gradient-to-br from-pink-100 via-purple-100 to-blue-100">
            {/* 渐变色占位背景 */}
          </div>
        </div>

        {/* 正文内容 - 纯文字 */}
        <div className="space-y-4 text-gray-700 leading-relaxed">
          <p>
            苏绣，是中国传统的刺绣工艺品之一。苏绣针法细腻，色彩明快，质感逼真，被誉予中国"锦绣之花"。它与湖南湘绣、广东粤绣、四川蜀绣一起，被誉为我国的四大名绣。两宋时期，苏州成为苏绣的重要发展中心；明代鲁德胜同苏州绣坊生产的苏绣，其针法和色彩皆传承风格；清代刺绣业繁荣；民国时期苏州绣有所创新，并于抗战利后达到了鼎盛。
          </p>

          <p>
            作为苏州的"文化名片"，苏绣以其灵动的针法和温婉的气质，成为江南刺绣艺术的代表之作。苏绣的图案设计融合了传统花鸟图案和独特的艺术风格，展现了中国传统文化的深厚底蕴。
          </p>

          <p>
            本次体验活动将带您深入了解苏绣的历史文化，学习基础的刺绣技法，亲手制作属于自己的苏绣作品。在专业老师的指导下，您将感受针线在指尖流转的魅力，体会传统手工艺的独特魅力。
          </p>

          <p>活动时间：2024年11月30日 14:00-16:30</p>

          <p>活动地点：苏州美术馆一楼活动室</p>

          <p>报名方式：关注公众号在线报名</p>

          <p>
            我们期待与您一起，在针线交织间感受传统文化的魅力，在苏绣艺术中品味江南的温婉与细腻。让我们携手传承和发扬这一宝贵的非物质文化遗产。
          </p>
        </div>
      </div>
    </div>
  );
}
