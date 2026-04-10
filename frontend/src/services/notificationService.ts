import api from './api';
import type { Notification, Page } from '../types';

export const notificationService = {
  /**
   * 알림 목록 조회
   */
  async getNotifications(page = 0, size = 20): Promise<Page<Notification>> {
    const response = await api.get('/notifications', {
      params: { page, size },
    });
    return response.data.data;
  },

  /**
   * 읽지 않은 알림 개수 조회
   */
  async getUnreadCount(): Promise<number> {
    const response = await api.get('/notifications/unread-count');
    return response.data.data.count;
  },

  /**
   * 알림 읽음 처리
   */
  async markAsRead(notificationId: number): Promise<void> {
    await api.patch(`/notifications/${notificationId}/read`);
  },

  /**
   * 모든 알림 읽음 처리
   */
  async markAllAsRead(): Promise<number> {
    const response = await api.patch('/notifications/read-all');
    return response.data.data;
  },

  /**
   * 알림 삭제
   */
  async deleteNotification(notificationId: number): Promise<void> {
    await api.delete(`/notifications/${notificationId}`);
  },
};
