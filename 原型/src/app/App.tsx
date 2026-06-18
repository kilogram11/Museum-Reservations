import { useState } from 'react';
import { Toaster } from 'sonner';
import WelcomePage from './components/WelcomePage';
import LoginPage from './components/LoginPage';
import RegisterPage from './components/RegisterPage';
import HomePage from './components/HomePage';
import BookingPage from './components/BookingPage';
import ActivityListPage from './components/ActivityListPage';
import ActivityDetailPage from './components/ActivityDetailPage';
import AnnouncementPage from './components/AnnouncementPage';
import AnnouncementDetailPage from './components/AnnouncementDetailPage';
import ProfilePage from './components/ProfilePage';
import EditProfilePage, { UserProfile } from './components/EditProfilePage';
import VisitorRegistrationPage from './components/VisitorRegistrationPage';
import BookingRecordPage from './components/BookingRecordPage';

type PageType = 'welcome' | 'login' | 'register' | 'home' | 'booking' | 'activity' | 'activityDetail' | 'announcement' | 'announcementDetail' | 'profile' | 'editProfile' | 'visitorRegistration' | 'bookingRecord';

export default function App() {
  const [currentPage, setCurrentPage] = useState<PageType>('welcome');
  const [selectedActivityId, setSelectedActivityId] = useState<number>(1);
  const [selectedAnnouncementId, setSelectedAnnouncementId] = useState<number>(1);
  const [currentUser, setCurrentUser] = useState<string>('');
  const [userProfile, setUserProfile] = useState<UserProfile>({
    username: '查理苏',
    gender: 'male',
    phone: '138****5678',
    avatar: '👨',
  });

  const handleActivityClick = (activityId: number) => {
    setSelectedActivityId(activityId);
    setCurrentPage('activityDetail');
  };
  
  const handleAnnouncementClick = (announcementId: number) => {
    setSelectedAnnouncementId(announcementId);
    setCurrentPage('announcementDetail');
  };

  const handleBookingSuccess = () => {
    setCurrentPage('bookingRecord');
  };
  
  const handleLoginSuccess = (username: string) => {
    setCurrentUser(username);
    setUserProfile({ ...userProfile, username });
    setCurrentPage('home');
  };
  
  const handleRegisterSuccess = () => {
    setCurrentPage('login');
  };
  
  const handleLogout = () => {
    setCurrentUser('');
    setCurrentPage('welcome');
  };
  
  const handleSaveProfile = (profile: UserProfile) => {
    setUserProfile(profile);
    setCurrentUser(profile.username);
  };

  return (
    <div className="size-full bg-gray-50">
      <Toaster position="top-center" richColors />
      <div className="max-w-md mx-auto h-full bg-white shadow-xl overflow-hidden">
        {currentPage === 'welcome' ? (
          <WelcomePage 
            onNavigateToLogin={() => setCurrentPage('login')}
            onNavigateToRegister={() => setCurrentPage('register')}
          />
        ) : currentPage === 'login' ? (
          <LoginPage 
            onBack={() => setCurrentPage('welcome')}
            onLoginSuccess={handleLoginSuccess}
          />
        ) : currentPage === 'register' ? (
          <RegisterPage 
            onBack={() => setCurrentPage('welcome')}
            onRegisterSuccess={handleRegisterSuccess}
          />
        ) : currentPage === 'home' ? (
          <HomePage 
            onNavigateToBooking={() => setCurrentPage('booking')}
            onNavigateToActivity={() => setCurrentPage('activity')}
            onNavigateToAnnouncement={() => setCurrentPage('announcement')}
            onNavigateToProfile={() => setCurrentPage('profile')}
          />
        ) : currentPage === 'booking' ? (
          <BookingPage 
            onBack={() => setCurrentPage('home')}
            onBookingSuccess={handleBookingSuccess}
          />
        ) : currentPage === 'activity' ? (
          <ActivityListPage 
            onBack={() => setCurrentPage('home')}
            onActivityClick={handleActivityClick}
          />
        ) : currentPage === 'activityDetail' ? (
          <ActivityDetailPage 
            activityId={selectedActivityId}
            onBack={() => setCurrentPage('activity')}
          />
        ) : currentPage === 'announcement' ? (
          <AnnouncementPage 
            onBack={() => setCurrentPage('home')}
            onAnnouncementClick={handleAnnouncementClick}
          />
        ) : currentPage === 'announcementDetail' ? (
          <AnnouncementDetailPage 
            announcementId={selectedAnnouncementId}
            onBack={() => setCurrentPage('announcement')}
          />
        ) : currentPage === 'profile' ? (
          <ProfilePage 
            onBack={() => setCurrentPage('home')}
            onNavigateToVisitorRegistration={() => setCurrentPage('visitorRegistration')}
            onNavigateToBookingRecord={() => setCurrentPage('bookingRecord')}
            onNavigateToEditProfile={() => setCurrentPage('editProfile')}
            onLogout={handleLogout}
            username={userProfile.username}
            avatar={userProfile.avatar}
          />
        ) : currentPage === 'editProfile' ? (
          <EditProfilePage 
            onBack={() => setCurrentPage('profile')}
            currentUsername={userProfile.username}
            onSaveProfile={handleSaveProfile}
          />
        ) : currentPage === 'visitorRegistration' ? (
          <VisitorRegistrationPage onBack={() => setCurrentPage('profile')} />
        ) : (
          <BookingRecordPage onBack={() => setCurrentPage('profile')} />
        )}
      </div>
    </div>
  );
}