import { useState, useEffect } from 'react';
import { CheckCircle, XCircle, Eye, X } from 'lucide-react';
import adminService from '../services/adminService';
import type { CertificationRequest } from '../types';
import { JOB_ROLE_LABELS } from '../types';

export default function CertificationsPage() {
  const [certifications, setCertifications] = useState<CertificationRequest[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [selectedCert, setSelectedCert] = useState<CertificationRequest | null>(null);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    loadCertifications();
  }, []);

  const loadCertifications = async () => {
    try {
      const data = await adminService.getPendingCertifications();
      setCertifications(data);
    } catch (error) {
      console.error('Failed to load certifications:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleApprove = async (cert: CertificationRequest) => {
    if (!confirm(`${cert.userName}님의 인증을 승인하시겠습니까?`)) return;

    setIsProcessing(true);
    try {
      await adminService.approveCertification(cert.userId);
      loadCertifications();
      setSelectedCert(null);
    } catch (error) {
      console.error('Failed to approve:', error);
      alert('승인 처리에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const openRejectModal = (cert: CertificationRequest) => {
    setSelectedCert(cert);
    setRejectReason('');
    setShowRejectModal(true);
  };

  const handleReject = async () => {
    if (!selectedCert) return;
    if (!rejectReason.trim()) {
      alert('반려 사유를 입력해주세요.');
      return;
    }

    setIsProcessing(true);
    try {
      await adminService.rejectCertification(selectedCert.userId, rejectReason);
      loadCertifications();
      setShowRejectModal(false);
      setSelectedCert(null);
    } catch (error) {
      console.error('Failed to reject:', error);
      alert('반려 처리에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const openDetailModal = (cert: CertificationRequest) => {
    setSelectedCert(cert);
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
          수료 인증 요청을 검토하고 승인/반려합니다.
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
            <p className="text-2xl font-bold text-yellow-900">{certifications.length}건</p>
          </div>
        </div>
      </div>

      {/* Certification List */}
      {certifications.length === 0 ? (
        <div className="card text-center py-12">
          <CheckCircle className="w-12 h-12 text-green-500 mx-auto mb-4" />
          <p className="text-gray-500">대기 중인 인증 요청이 없습니다.</p>
        </div>
      ) : (
        <div className="space-y-4">
          {certifications.map((cert) => (
            <div key={cert.id} className="card">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-2">
                    <h3 className="text-lg font-semibold text-gray-900">{cert.userName}</h3>
                    <span className="px-2 py-1 rounded text-xs font-medium bg-yellow-100 text-yellow-700">
                      대기 중
                    </span>
                  </div>
                  <p className="text-gray-600 mb-1">{cert.userEmail}</p>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm mt-3">
                    <div>
                      <p className="text-gray-500">부트캠프</p>
                      <p className="text-gray-900 font-medium">{cert.bootcampName}</p>
                    </div>
                    <div>
                      <p className="text-gray-500">기수</p>
                      <p className="text-gray-900 font-medium">{cert.cohort}</p>
                    </div>
                    <div>
                      <p className="text-gray-500">직무</p>
                      <p className="text-gray-900 font-medium">{JOB_ROLE_LABELS[cert.jobRole]}</p>
                    </div>
                    <div>
                      <p className="text-gray-500">수료일</p>
                      <p className="text-gray-900 font-medium">
                        {new Date(cert.completionDate).toLocaleDateString('ko-KR')}
                      </p>
                    </div>
                  </div>
                  <p className="text-xs text-gray-400 mt-3">
                    신청일: {new Date(cert.requestedAt).toLocaleString('ko-KR')}
                  </p>
                </div>

                <div className="flex items-center gap-2 ml-4">
                  {cert.certificateImageUrl && (
                    <button
                      onClick={() => openDetailModal(cert)}
                      className="btn-secondary p-2"
                      title="수료증 보기"
                    >
                      <Eye className="w-4 h-4" />
                    </button>
                  )}
                  <button
                    onClick={() => handleApprove(cert)}
                    disabled={isProcessing}
                    className="btn-success flex items-center gap-1"
                  >
                    <CheckCircle className="w-4 h-4" />
                    승인
                  </button>
                  <button
                    onClick={() => openRejectModal(cert)}
                    disabled={isProcessing}
                    className="btn-danger flex items-center gap-1"
                  >
                    <XCircle className="w-4 h-4" />
                    반려
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Detail Modal (Certificate Image) */}
      {selectedCert && !showRejectModal && (
        <div className="fixed inset-0 bg-gray-900/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
            <div className="sticky top-0 bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
              <h2 className="text-xl font-semibold text-gray-900">수료 인증 상세</h2>
              <button
                onClick={() => setSelectedCert(null)}
                className="p-1 hover:bg-gray-100 rounded"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 space-y-6">
              <div className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <p className="text-gray-500">이름</p>
                  <p className="text-gray-900 font-medium">{selectedCert.userName}</p>
                </div>
                <div>
                  <p className="text-gray-500">이메일</p>
                  <p className="text-gray-900 font-medium">{selectedCert.userEmail}</p>
                </div>
                <div>
                  <p className="text-gray-500">부트캠프</p>
                  <p className="text-gray-900 font-medium">{selectedCert.bootcampName}</p>
                </div>
                <div>
                  <p className="text-gray-500">기수</p>
                  <p className="text-gray-900 font-medium">{selectedCert.cohort}</p>
                </div>
                <div>
                  <p className="text-gray-500">직무</p>
                  <p className="text-gray-900 font-medium">{JOB_ROLE_LABELS[selectedCert.jobRole]}</p>
                </div>
                <div>
                  <p className="text-gray-500">수료일</p>
                  <p className="text-gray-900 font-medium">
                    {new Date(selectedCert.completionDate).toLocaleDateString('ko-KR')}
                  </p>
                </div>
              </div>

              {selectedCert.certificateImageUrl && (
                <div>
                  <p className="text-gray-500 mb-2">수료증</p>
                  <div className="border border-gray-200 rounded-lg overflow-hidden">
                    <img
                      src={selectedCert.certificateImageUrl}
                      alt="수료증"
                      className="w-full h-auto"
                    />
                  </div>
                </div>
              )}

              <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
                <button
                  onClick={() => openRejectModal(selectedCert)}
                  className="btn-danger flex items-center gap-1"
                >
                  <XCircle className="w-4 h-4" />
                  반려
                </button>
                <button
                  onClick={() => handleApprove(selectedCert)}
                  disabled={isProcessing}
                  className="btn-success flex items-center gap-1"
                >
                  <CheckCircle className="w-4 h-4" />
                  승인
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Reject Modal */}
      {showRejectModal && selectedCert && (
        <div className="fixed inset-0 bg-gray-900/50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl shadow-xl w-full max-w-md">
            <div className="px-6 py-4 border-b border-gray-200">
              <h2 className="text-xl font-semibold text-gray-900">인증 반려</h2>
            </div>

            <div className="p-6 space-y-4">
              <p className="text-gray-600">
                <strong>{selectedCert.userName}</strong>님의 인증 요청을 반려합니다.
              </p>

              <div>
                <label className="label">반려 사유 *</label>
                <textarea
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  className="input"
                  rows={4}
                  placeholder="반려 사유를 입력해주세요. 사용자에게 전달됩니다."
                  required
                />
              </div>

              <div className="flex justify-end gap-3 pt-4">
                <button
                  onClick={() => {
                    setShowRejectModal(false);
                    setSelectedCert(null);
                  }}
                  className="btn-secondary"
                >
                  취소
                </button>
                <button
                  onClick={handleReject}
                  disabled={isProcessing || !rejectReason.trim()}
                  className="btn-danger"
                >
                  {isProcessing ? '처리 중...' : '반려'}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
