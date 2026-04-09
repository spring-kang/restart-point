import { useEffect, useState } from 'react';
import { adminService, type UserSearchParams } from '../services/adminService';
import { useAuthStore } from '../stores/authStore';
import type { User, UserRole, CertificationStatus, Page } from '../types';
import {
  USER_ROLE_LABELS,
  USER_ROLE_COLORS,
  CERTIFICATION_STATUS_LABELS,
  CERTIFICATION_STATUS_COLORS,
} from '../types';
import axios from 'axios';

export default function UsersPage() {
  const { user: currentUser } = useAuthStore();
  const [users, setUsers] = useState<User[]>([]);
  const [page, setPage] = useState<Page<User> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  // 검색/필터 상태
  const [keyword, setKeyword] = useState('');
  const [roleFilter, setRoleFilter] = useState<UserRole | ''>('');
  const [certificationFilter, setCertificationFilter] = useState<CertificationStatus | ''>('');
  const [currentPage, setCurrentPage] = useState(0);

  // 모달 상태
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [isRoleModalOpen, setIsRoleModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  const fetchUsers = async () => {
    setIsLoading(true);
    setError('');
    try {
      const params: UserSearchParams = {
        page: currentPage,
        size: 20,
      };
      if (keyword) params.keyword = keyword;
      if (roleFilter) params.role = roleFilter;
      if (certificationFilter) params.certificationStatus = certificationFilter;

      const response = await adminService.getUsers(params);
      setUsers(response.content);
      setPage(response);
    } catch (err) {
      setError('회원 목록을 불러오는데 실패했습니다.');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [currentPage, roleFilter, certificationFilter]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(0);
    fetchUsers();
  };

  const handleRoleChange = async (newRole: UserRole) => {
    if (!selectedUser) return;
    setIsProcessing(true);
    try {
      await adminService.updateUserRole(selectedUser.id, newRole);
      setIsRoleModalOpen(false);
      setSelectedUser(null);
      fetchUsers();
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        alert(err.response.data.message);
      } else {
        alert('역할 변경에 실패했습니다.');
      }
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedUser) return;
    setIsProcessing(true);
    try {
      await adminService.deleteUser(selectedUser.id);
      setIsDeleteModalOpen(false);
      setSelectedUser(null);
      fetchUsers();
    } catch (err) {
      console.error(err);
      if (axios.isAxiosError(err) && err.response?.data?.message) {
        alert(err.response.data.message);
      } else {
        alert('회원 삭제에 실패했습니다.');
      }
    } finally {
      setIsProcessing(false);
    }
  };

  // 자기 자신인지 확인
  const isSelf = (user: User) => currentUser?.id === user.id;

  const openRoleModal = (user: User) => {
    setSelectedUser(user);
    setIsRoleModalOpen(true);
  };

  const openDeleteModal = (user: User) => {
    setSelectedUser(user);
    setIsDeleteModalOpen(true);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">회원 관리</h1>
        <span className="text-sm text-gray-500">
          총 {page?.totalElements || 0}명
        </span>
      </div>

      {/* 검색 및 필터 */}
      <div className="card">
        <form onSubmit={handleSearch} className="flex flex-wrap gap-4">
          <div className="flex-1 min-w-64">
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="이름 또는 이메일로 검색..."
              className="input"
            />
          </div>
          <select
            value={roleFilter}
            onChange={(e) => setRoleFilter(e.target.value as UserRole | '')}
            className="input w-40"
          >
            <option value="">전체 역할</option>
            <option value="USER">일반 사용자</option>
            <option value="ADMIN">관리자</option>
          </select>
          <select
            value={certificationFilter}
            onChange={(e) => setCertificationFilter(e.target.value as CertificationStatus | '')}
            className="input w-40"
          >
            <option value="">전체 인증상태</option>
            <option value="NONE">미신청</option>
            <option value="PENDING">대기 중</option>
            <option value="APPROVED">승인됨</option>
            <option value="REJECTED">거절됨</option>
          </select>
          <button type="submit" className="btn-primary">
            검색
          </button>
        </form>
      </div>

      {error && (
        <div className="p-4 rounded-lg bg-red-50 text-red-700">
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-4 border-primary-600 border-t-transparent"></div>
          <p className="mt-2 text-gray-500">로딩 중...</p>
        </div>
      ) : users.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          회원이 없습니다.
        </div>
      ) : (
        <>
          {/* 회원 목록 테이블 */}
          <div className="card overflow-hidden p-0">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">ID</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">이름</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">이메일</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">역할</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">인증상태</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">부트캠프</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">가입일</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-700">작업</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {users.map((user) => (
                  <tr key={user.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm text-gray-900">{user.id}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 font-medium">{user.name}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{user.email}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${USER_ROLE_COLORS[user.role]}`}>
                        {USER_ROLE_LABELS[user.role]}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${CERTIFICATION_STATUS_COLORS[user.certificationStatus]}`}>
                        {CERTIFICATION_STATUS_LABELS[user.certificationStatus]}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {user.bootcampName ? (
                        <span>{user.bootcampName} {user.bootcampGeneration}</span>
                      ) : (
                        <span className="text-gray-400">-</span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {formatDate(user.createdAt)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        {isSelf(user) ? (
                          <span className="text-xs text-gray-400">(본인)</span>
                        ) : (
                          <>
                            <button
                              onClick={() => openRoleModal(user)}
                              className="btn-secondary text-xs py-1 px-2"
                            >
                              역할 변경
                            </button>
                            <button
                              onClick={() => openDeleteModal(user)}
                              className="btn-danger text-xs py-1 px-2"
                            >
                              삭제
                            </button>
                          </>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* 페이지네이션 */}
          {page && page.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              <button
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                disabled={page.first}
                className="btn-secondary"
              >
                이전
              </button>
              <span className="text-sm text-gray-600">
                {page.number + 1} / {page.totalPages}
              </span>
              <button
                onClick={() => setCurrentPage((p) => p + 1)}
                disabled={page.last}
                className="btn-secondary"
              >
                다음
              </button>
            </div>
          )}
        </>
      )}

      {/* 역할 변경 모달 */}
      {isRoleModalOpen && selectedUser && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-lg font-bold text-gray-900 mb-4">역할 변경</h2>
            <p className="text-sm text-gray-600 mb-4">
              <strong>{selectedUser.name}</strong>님의 역할을 변경합니다.
            </p>
            <p className="text-sm text-gray-500 mb-4">
              현재 역할: <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${USER_ROLE_COLORS[selectedUser.role]}`}>
                {USER_ROLE_LABELS[selectedUser.role]}
              </span>
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => handleRoleChange('USER')}
                disabled={isProcessing || selectedUser.role === 'USER'}
                className="btn-secondary flex-1"
              >
                일반 사용자로 변경
              </button>
              <button
                onClick={() => handleRoleChange('ADMIN')}
                disabled={isProcessing || selectedUser.role === 'ADMIN'}
                className="btn-primary flex-1"
              >
                관리자로 변경
              </button>
            </div>
            <button
              onClick={() => {
                setIsRoleModalOpen(false);
                setSelectedUser(null);
              }}
              className="btn-secondary w-full mt-3"
            >
              취소
            </button>
          </div>
        </div>
      )}

      {/* 삭제 확인 모달 */}
      {isDeleteModalOpen && selectedUser && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-full max-w-md">
            <h2 className="text-lg font-bold text-gray-900 mb-4">회원 삭제</h2>
            <p className="text-sm text-gray-600 mb-4">
              정말로 <strong>{selectedUser.name}</strong>님을 삭제하시겠습니까?
            </p>
            <p className="text-sm text-red-600 mb-4">
              이 작업은 되돌릴 수 없습니다.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => {
                  setIsDeleteModalOpen(false);
                  setSelectedUser(null);
                }}
                className="btn-secondary flex-1"
              >
                취소
              </button>
              <button
                onClick={handleDelete}
                disabled={isProcessing}
                className="btn-danger flex-1"
              >
                {isProcessing ? '삭제 중...' : '삭제'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
