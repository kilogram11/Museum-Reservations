import { useState } from 'react';
import Slider from 'react-slick';
import 'slick-carousel/slick/slick.css';
import 'slick-carousel/slick/slick-theme.css';
import { MapPin, Clock } from 'lucide-react';

interface ActivityListPageProps {
  onBack: () => void;
  onActivityClick: (activityId: number) => void;
}

export default function ActivityListPage({ onBack, onActivityClick }: ActivityListPageProps) {
  // 轮播图数据
  const carouselItems = [
    {
      id: 1,
      title: '青铜器时代特展',
      subtitle: '探索三千年前的艺术',
      image: 'https://images.unsplash.com/photo-1562064729-6c3f058785fd?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&ixid=M3w3Nzg4Nzd8MHwxfHNlYXJjaHwxfHxtdXNldW0lMjBleGhpYml0aW9ufGVufDF8fHx8MTc2NjU0MTQ5OXww&ixlib=rb-4.1.0&q=80&w=1080',
    },
  ];

  // 活动列表数据
  const activities = [
    {
      id: 1,
      tag: '展览',
      title: '青铜器时代特展',
      date: '2024.11.15-2025.06.30',
      location: '华夏博物馆',
      images: [
        'https://images.unsplash.com/photo-1761724794667-64dd1a9d0527?w=400',
        'https://images.unsplash.com/photo-1562064729-6c3f058785fd?w=400',
        'https://images.unsplash.com/photo-1612284230156-15fad4f9bb6d?w=400',
        'https://images.unsplash.com/photo-1761724794667-64dd1a9d0527?w=400',
      ],
    },
    {
      id: 2,
      tag: '文物',
      title: '春宇陶艺等你来',
      date: '2024.11.17-2025.10.30',
      location: '九宫博物馆',
      images: [],
    },
    {
      id: 3,
      tag: '展出',
      title: '现代科学学宝库',
      date: '2025.01.12-2025.12.30',
      location: '周口一站',
      images: [],
    },
  ];

  const sliderSettings = {
    dots: true,
    infinite: true,
    speed: 500,
    slidesToShow: 1,
    slidesToScroll: 1,
    autoplay: true,
    autoplaySpeed: 3000,
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
        <h1 className="text-lg font-semibold text-gray-900">活动查看</h1>
      </div>

      {/* 轮播图 */}
      <div className="bg-gray-900 relative overflow-hidden">
        <Slider {...sliderSettings}>
          {carouselItems.map((item) => (
            <div key={item.id} className="relative">
              <div className="relative h-56">
                <img
                  src={item.image}
                  alt={item.title}
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
                <div className="absolute bottom-0 left-0 right-0 p-6 text-white">
                  <h2 className="text-2xl font-bold mb-1">{item.title}</h2>
                  <p className="text-sm text-white/90">{item.subtitle}</p>
                </div>
              </div>
            </div>
          ))}
        </Slider>
      </div>

      {/* 近期活动 */}
      <div className="p-4 pb-8">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">近期活动</h3>

        <div className="space-y-4">
          {activities.map((activity) => (
            <div
              key={activity.id}
              onClick={() => onActivityClick(activity.id)}
              className="bg-white rounded-2xl overflow-hidden shadow-sm hover:shadow-md transition-shadow cursor-pointer"
            >
              {/* 活动图片或占位符 */}
              {activity.images.length > 0 ? (
                <div className="relative h-48">
                  <img
                    src={activity.images[0]}
                    alt={activity.title}
                    className="w-full h-full object-cover"
                  />
                </div>
              ) : (
                <div className="h-48 bg-gradient-to-br from-blue-100 to-purple-100 flex items-center justify-center">
                  <span className="text-4xl">🏛️</span>
                </div>
              )}

              {/* 活动信息 */}
              <div className="p-4">
                
                <h4 className="font-semibold text-gray-900 mb-2">{activity.title}</h4>
                <div className="space-y-1">
                  <div className="flex items-center text-sm text-gray-500">
                    <Clock className="w-4 h-4 mr-1" />
                    {activity.date}
                  </div>
                  <div className="flex items-center text-sm text-gray-500">
                    <MapPin className="w-4 h-4 mr-1" />
                    {activity.location}
                  </div>
                </div>

                {/* 图片缩略图 */}
                {activity.images.length > 0 && (
                  <div className="flex gap-2 mt-3 overflow-x-auto pb-2">
                    {activity.images.map((img, idx) => (
                      <img
                        key={idx}
                        src={img}
                        alt={`${activity.title} ${idx + 1}`}
                        className="w-16 h-16 rounded-lg object-cover flex-shrink-0"
                      />
                    ))}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}