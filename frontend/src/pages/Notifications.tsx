import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bell, Check, CheckCheck, Trash2 } from 'lucide-react';
import { notificationService } from '../services/notificationService';
import { useAuthStore } from '../stores/authStore';
import type { Notification } from '../types';

export default function NotificationsPage() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    loadNotifications();
  }, [isAuthenticated]);

  const loadNotifications = async (pageNum = 0) => {
    setIsLoading(true);
    try {
      const data = await notificationService.getNotifications(pageNum, 20);
      if (pageNum === 0) {
        setNotifications(data.content);
      } else {
        setNotifications((prev) => [...prev, ...data.content]);
      }
      setHasMore(!data.last);
      setPage(pageNum);
    } catch (err) {
      console.error('Failed to load notifications:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleMarkAsRead = async (notification: Notification) => {
    if (notification.read) return;
    try {
      await notificationService.markAsRead(notification.id);
      setNotifications((prev) =>
        prev.map((n) => (n.id === notification.id ? { ...n, read: true } : n))
      );
    } catch (err) {
      console.error('Failed to mark as read:', err);
    }
  };

  const handleMarkAllAsRead = async () => {
    try {
      await notificationService.markAllAsRead();
      setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
    } catch (err) {
      console.error('Failed to mark all as read:', err);
    }
  };

  const handleDelete = async (notificationId: number) => {
    try {
      await notificationService.deleteNotification(notificationId);
      setNotifications((prev) => prev.filter((n) => n.id !== notificationId));
    } catch (err) {
      console.error('Failed to delete notification:', err);
    }
  };

  const handleNotificationClick = (notification: Notification) => {
    handleMarkAsRead(notification);

    if (notification.resourceType && notification.resourceId) {
      switch (notification.resourceType) {
        case 'TEAM':
          navigate(`/teams/${notification.resourceId}`);
          break;
        case 'PROJECT':
          navigate(`/projects/${notification.resourceId}/growth-report`);
          break;
        case 'POST':
          navigate(`/community/posts/${notification.resourceId}`);
          break;
        case 'SEASON':
          navigate(`/seasons/${notification.resourceId}`);
          break;
      }
    }
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* 헤더 */}
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Bell className="w-6 h-6 text-primary-600" />
          <h1 className="text-2xl font-bold text-neutral-900">알림</h1>
          {unreadCount > 0 && (
            <span className="px-2 py-0.5 text-sm font-medium bg-primary-100 text-primary-700 rounded-full">
              {unreadCount}개 읽지 않음
            </span>
          )}
        </div>
        {unreadCount > 0 && (
          <button
            onClick={handleMarkAllAsRead}
            className="flex items-center gap-2 px-4 py-2 text-sm text-primary-600 hover:bg-primary-50 rounded-lg transition-colors"
          >
            <CheckCheck className="w-4 h-4" />
            모두 읽음 처리
          </button>
        )}
      </div>

      {/* 알림 목록 */}
      <div className="bg-white rounded-lg border border-neutral-200">
        {isLoading && notifications.length === 0 ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500" />
          </div>
        ) : notifications.length === 0 ? (
          <div className="py-12 text-center">
            <Bell className="w-12 h-12 text-neutral-300 mx-auto mb-4" />
            <p className="text-neutral-500">알림이 없습니다</p>
          </div>
        ) : (
          <div className="divide-y divide-neutral-100">
            {notifications.map((notification) => (
              <div
                key={notification.id}
                className={`p-4 hover:bg-neutral-50 transition-colors cursor-pointer ${
                  !notification.read ? 'bg-primary-50/30' : ''
                }`}
                onClick={() => handleNotificationClick(notification)}
              >
                <div className="flex items-start gap-4">
                  <div
                    className={`mt-1 w-2 h-2 rounded-full flex-shrink-0 ${
                      notification.read ? 'bg-transparent' : 'bg-primary-500'
                    }`}
                  />
                  <div className="flex-1 min-w-0">
                    <p
                      className={`font-medium ${
                        notification.read ? 'text-neutral-700' : 'text-neutral-900'
                      }`}
                    >
                      {notification.title}
                    </p>
                    <p className="text-sm text-neutral-500 mt-1">
                      {notification.message}
                    </p>
                    <p className="text-xs text-neutral-400 mt-2">
                      {formatTime(notification.createdAt)}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    {!notification.read && (
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleMarkAsRead(notification);
                        }}
                        className="p-2 rounded-lg hover:bg-neutral-200 text-neutral-400 hover:text-neutral-600"
                        title="읽음 처리"
                      >
                        <Check className="w-4 h-4" />
                      </button>
                    )}
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(notification.id);
                      }}
                      className="p-2 rounded-lg hover:bg-red-100 text-neutral-400 hover:text-red-600"
                      title="삭제"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* 더 보기 버튼 */}
        {hasMore && notifications.length > 0 && (
          <div className="p-4 border-t border-neutral-100 text-center">
            <button
              onClick={() => loadNotifications(page + 1)}
              disabled={isLoading}
              className="px-6 py-2 text-sm text-primary-600 hover:bg-primary-50 rounded-lg transition-colors disabled:opacity-50"
            >
              {isLoading ? '로딩 중...' : '더 보기'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
