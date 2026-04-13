import { useState, useEffect } from 'react';
import { CheckCircle, XCircle, ExternalLink } from 'lucide-react';
import adminService from '../services/adminService';
import type { User } from '../types';

export default function CertificationsPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    loadCertifications();
  }, []);

  const loadCertifications = async () => {
    try {
      const data = await adminService.getPendingCertifications();
      setUsers(data);
    } catch (error) {
      console.error('Failed to load certifications:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleApprove = async (user: User) => {
    if (!confirm(`${user.name}님의 인증을 승인하시겠습니까?`)) return;

    setIsProcessing(true);
    try {
      await adminService.approveCertification(user.id);
      loadCertifications();
    } catch (error) {
      console.error('Failed to approve:', error);
      alert('승인 처리에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleReject = async (user: User) => {
    if (!confirm(`${user.name}님의 인증을 거절하시겠습니까?`)) return;

    setIsProcessing(true);
    try {
      await adminService.rejectCertification(user.id);
      loadCertifications();
    } catch (error) {
      console.error('Failed to reject:', error);
      alert('거절 처리에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">수료 인증 관리</h1>
        <p className="text-gray-500 mt-1">
          수료 인증 요청을 검토하고 승인/거절합니다.
        </p>
      </div>

      {/* Stats */}
      <div className="card bg-yellow-50 border-yellow-200">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-yellow-100 rounded-lg">
            <CheckCircle className="w-5 h-5 text-yellow-600" />
          </div>
          <div>
            <p className="text-sm text-yellow-700">대기 중인 인증 요청</p>
            <p className="text-2xl font-bold text-yellow-900">{users.length}건</p>
          </div>
        </div>
      </div>

      {/* User List */}
      {users.length === 0 ? (
        <div className="card text-center py-12">
          <CheckCircle className="w-12 h-12 text-green-500 mx-auto mb-4" />
          <p className="text-gray-500">대기 중인 인증 요청이 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {users.map((user) => (
            <div key={user.id} className="card">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="text-lg font-semibold text-gray-900">{user.name}</h3>
                    <span className="px-2 py-1 rounded text-xs font-medium bg-yellow-100 text-yellow-700">
                      대기 중
                    </span>
                  </div>
                  <p className="text-gray-600 mb-3">{user.email}</p>
                  <div className="grid grid-cols-2 md:grid-cols-5 gap-4 text-sm">
                    <div>
                      <p className="text-gray-500">부트캠프</p>
                      <p className="text-gray-900 font-medium">
                        {user.bootcampName || '-'}
                      </p>
                    </div>
                    <div>
                      <p className="text-gray-500">기수</p>
                      <p className="text-gray-900 font-medium">
                        {user.bootcampGeneration || '-'}
                      </p>
                    </div>
                    <div>
                      <p className="text-gray-500">수료일</p>
                      <p className="text-gray-900 font-medium">
                        {user.graduationDate || '-'}
                      </p>
                    </div>
                    <div>
                      <p className="text-gray-500">가입일</p>
                      <p className="text-gray-900 font-medium">
                        {new Date(user.createdAt).toLocaleDateString('ko-KR')}
                      </p>
                    </div>
                    <div>
                      <p className="text-gray-500">수료증</p>
                      {user.certificateUrl ? (
                        <a
                          href={user.certificateUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center gap-1 text-blue-600 hover:text-blue-800 font-medium"
                        >
                          <ExternalLink className="w-4 h-4" />
                          확인하기
                        </a>
                      ) : (
                        <p className="text-gray-900 font-medium">-</p>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center gap-2 ml-4">
                  <button
                    onClick={() => handleApprove(user)}
                    disabled={isProcessing}
                    className="btn-success flex items-center gap-1"
                  >
                    <CheckCircle className="w-4 h-4" />
                    승인
                  </button>
                  <button
                    onClick={() => handleReject(user)}
                    disabled={isProcessing}
                    className="btn-danger flex items-center gap-1"
                  >
                    <XCircle className="w-4 h-4" />
                    거절
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
