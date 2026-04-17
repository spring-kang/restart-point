import { useState, useEffect } from 'react';
import {
  CreditCard,
  Plus,
  Edit2,
  Trash2,
  ChevronDown,
  DollarSign,
  Package,
  Users,
  CheckCircle,
  XCircle,
  Clock,
  AlertCircle,
} from 'lucide-react';
import adminService from '../services/adminService';

interface PricingPlan {
  id: number;
  seasonId: number;
  name: string;
  description: string;
  price: number;
  originalPrice: number | null;
  discountPercentage: number | null;
  features: string[];
  includeMentoring: boolean;
  includeAiCoaching: boolean;
  includeGrowthReport: boolean;
  maxTeamParticipation: number;
  displayOrder: number;
  recommended: boolean;
  active: boolean;
}

interface Order {
  id: number;
  orderNumber: string;
  userId: number;
  userName: string;
  userEmail: string;
  planName: string;
  amount: number;
  status: 'PENDING' | 'COMPLETED' | 'CANCELLED' | 'REFUNDED';
  paymentMethod: string | null;
  createdAt: string;
}

interface Subscription {
  id: number;
  userId: number;
  userName: string;
  userEmail: string;
  planName: string;
  seasonTitle: string;
  status: 'ACTIVE' | 'CANCELLED' | 'EXPIRED';
  startDate: string;
  endDate: string;
  hasMentoring: boolean;
}

interface Season {
  id: number;
  title: string;
  status: string;
}

const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING: '결제 대기',
  COMPLETED: '결제 완료',
  CANCELLED: '취소됨',
  REFUNDED: '환불됨',
};

const ORDER_STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-100 text-yellow-700',
  COMPLETED: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-gray-100 text-gray-600',
  REFUNDED: 'bg-red-100 text-red-700',
};

const SUBSCRIPTION_STATUS_LABELS: Record<string, string> = {
  ACTIVE: '활성',
  CANCELLED: '취소됨',
  EXPIRED: '만료됨',
};

const SUBSCRIPTION_STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'bg-green-100 text-green-700',
  CANCELLED: 'bg-gray-100 text-gray-600',
  EXPIRED: 'bg-red-100 text-red-700',
};

type TabType = 'plans' | 'orders' | 'subscriptions';

export default function PaymentsPage() {
  const [activeTab, setActiveTab] = useState<TabType>('plans');
  const [seasons, setSeasons] = useState<Season[]>([]);
  const [selectedSeasonId, setSelectedSeasonId] = useState<number | null>(null);
  const [plans, setPlans] = useState<PricingPlan[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showPlanModal, setShowPlanModal] = useState(false);
  const [editingPlan, setEditingPlan] = useState<PricingPlan | null>(null);

  // Stats
  const [stats, setStats] = useState({
    totalRevenue: 0,
    activeSubscriptions: 0,
    pendingOrders: 0,
  });

  useEffect(() => {
    loadSeasons();
  }, []);

  useEffect(() => {
    if (selectedSeasonId) {
      loadData();
    }
  }, [selectedSeasonId, activeTab]);

  const loadSeasons = async () => {
    try {
      const data = await adminService.getSeasons();
      setSeasons(data.content);
      if (data.content.length > 0) {
        setSelectedSeasonId(data.content[0].id);
      }
    } catch (err) {
      console.error('Failed to load seasons:', err);
      setError('시즌 목록을 불러오는데 실패했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  const loadData = async () => {
    if (!selectedSeasonId) return;
    setError(null);

    try {
      if (activeTab === 'plans') {
        const data = await adminService.getPricingPlans(selectedSeasonId);
        setPlans(data || []);
      } else if (activeTab === 'orders') {
        const data = await adminService.getOrders(selectedSeasonId);
        setOrders(data || []);
      } else if (activeTab === 'subscriptions') {
        const data = await adminService.getSubscriptions(selectedSeasonId);
        setSubscriptions(data || []);
      }

      // Load stats
      try {
        const statsData = await adminService.getPaymentStats(selectedSeasonId);
        setStats(statsData || { totalRevenue: 0, activeSubscriptions: 0, pendingOrders: 0 });
      } catch {
        // Stats loading failure shouldn't block other data
        console.error('Failed to load stats');
      }
    } catch (err) {
      console.error('Failed to load data:', err);
      setError('데이터를 불러오는데 실패했습니다.');
    }
  };

  const handleCreatePlan = () => {
    setEditingPlan(null);
    setShowPlanModal(true);
  };

  const handleEditPlan = (plan: PricingPlan) => {
    setEditingPlan(plan);
    setShowPlanModal(true);
  };

  const handleDeletePlan = async (planId: number) => {
    if (!confirm('정말 이 가격 정책을 삭제하시겠습니까?')) return;
    try {
      await adminService.deletePricingPlan(planId);
      setPlans((prev) => prev.filter((p) => p.id !== planId));
    } catch (error) {
      console.error('Failed to delete plan:', error);
      alert('삭제에 실패했습니다.');
    }
  };

  const handleRefundOrder = async (orderId: number) => {
    const reason = prompt('환불 사유를 입력해주세요:');
    if (!reason) return;

    try {
      await adminService.refundOrder(orderId, reason);
      loadData();
    } catch (error) {
      console.error('Failed to refund order:', error);
      alert('환불 처리에 실패했습니다.');
    }
  };

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('ko-KR', {
      style: 'currency',
      currency: 'KRW',
    }).format(amount);
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
        <h1 className="text-2xl font-bold text-gray-900">결제/구독 관리</h1>
        <p className="text-gray-500 mt-1">가격 정책, 주문, 구독을 관리합니다.</p>
      </div>

      {/* Error State */}
      {error && (
        <div className="card bg-red-50 border-red-200 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-red-500" />
          <p className="text-red-700">{error}</p>
        </div>
      )}

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="card flex items-center gap-4">
          <div className="p-3 rounded-lg bg-green-100">
            <DollarSign className="w-6 h-6 text-green-600" />
          </div>
          <div>
            <p className="text-sm text-gray-500">총 매출</p>
            <p className="text-xl font-bold text-gray-900">{formatCurrency(stats.totalRevenue)}</p>
          </div>
        </div>
        <div className="card flex items-center gap-4">
          <div className="p-3 rounded-lg bg-blue-100">
            <Users className="w-6 h-6 text-blue-600" />
          </div>
          <div>
            <p className="text-sm text-gray-500">활성 구독</p>
            <p className="text-xl font-bold text-gray-900">{stats.activeSubscriptions}명</p>
          </div>
        </div>
        <div className="card flex items-center gap-4">
          <div className="p-3 rounded-lg bg-yellow-100">
            <Clock className="w-6 h-6 text-yellow-600" />
          </div>
          <div>
            <p className="text-sm text-gray-500">결제 대기</p>
            <p className="text-xl font-bold text-gray-900">{stats.pendingOrders}건</p>
          </div>
        </div>
      </div>

      {/* Season Selector & Tabs */}
      <div className="card">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-4">
            <label className="text-sm font-medium text-gray-700">시즌:</label>
            <div className="relative">
              <select
                value={selectedSeasonId || ''}
                onChange={(e) => setSelectedSeasonId(Number(e.target.value))}
                className="appearance-none bg-white border border-gray-300 rounded-lg px-4 py-2 pr-8 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                {seasons.map((season) => (
                  <option key={season.id} value={season.id}>
                    {season.title}
                  </option>
                ))}
              </select>
              <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-500 pointer-events-none" />
            </div>
          </div>

          <div className="flex gap-1 p-1 bg-gray-100 rounded-lg">
            <button
              onClick={() => setActiveTab('plans')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                activeTab === 'plans'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              가격 정책
            </button>
            <button
              onClick={() => setActiveTab('orders')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                activeTab === 'orders'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              주문 내역
            </button>
            <button
              onClick={() => setActiveTab('subscriptions')}
              className={`px-4 py-2 text-sm font-medium rounded-md transition-colors ${
                activeTab === 'subscriptions'
                  ? 'bg-white text-gray-900 shadow-sm'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              구독 현황
            </button>
          </div>
        </div>
      </div>

      {/* Content */}
      {activeTab === 'plans' && (
        <div className="space-y-4">
          <div className="flex justify-end">
            <button
              onClick={handleCreatePlan}
              className="btn-primary flex items-center gap-2"
            >
              <Plus className="w-4 h-4" />
              가격 정책 추가
            </button>
          </div>

          {plans.length === 0 && !error ? (
            <div className="card text-center py-12">
              <Package className="w-12 h-12 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500">등록된 가격 정책이 없습니다.</p>
            </div>
          ) : plans.length > 0 ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {plans.map((plan) => (
                <div
                  key={plan.id}
                  className={`card relative ${plan.recommended ? 'ring-2 ring-primary-500' : ''}`}
                >
                  {plan.recommended && (
                    <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-1 bg-primary-500 text-white text-xs font-medium rounded-full">
                      추천
                    </div>
                  )}
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <h3 className="font-semibold text-gray-900">{plan.name}</h3>
                      <p className="text-sm text-gray-500">{plan.description}</p>
                    </div>
                    <span
                      className={`px-2 py-1 rounded text-xs font-medium ${
                        plan.active ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600'
                      }`}
                    >
                      {plan.active ? '활성' : '비활성'}
                    </span>
                  </div>

                  <div className="mb-4">
                    <div className="flex items-baseline gap-2">
                      <span className="text-2xl font-bold text-gray-900">
                        {formatCurrency(plan.price)}
                      </span>
                      {plan.originalPrice && plan.originalPrice > plan.price && (
                        <span className="text-sm text-gray-400 line-through">
                          {formatCurrency(plan.originalPrice)}
                        </span>
                      )}
                    </div>
                    {plan.discountPercentage && (
                      <span className="text-sm text-red-500 font-medium">
                        {plan.discountPercentage}% 할인
                      </span>
                    )}
                  </div>

                  <div className="space-y-2 mb-4 text-sm">
                    <div className="flex items-center gap-2">
                      {plan.includeMentoring ? (
                        <CheckCircle className="w-4 h-4 text-green-500" />
                      ) : (
                        <XCircle className="w-4 h-4 text-gray-300" />
                      )}
                      <span className={plan.includeMentoring ? 'text-gray-700' : 'text-gray-400'}>
                        멘토링 포함
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      {plan.includeAiCoaching ? (
                        <CheckCircle className="w-4 h-4 text-green-500" />
                      ) : (
                        <XCircle className="w-4 h-4 text-gray-300" />
                      )}
                      <span className={plan.includeAiCoaching ? 'text-gray-700' : 'text-gray-400'}>
                        AI 코칭
                      </span>
                    </div>
                    <div className="flex items-center gap-2">
                      {plan.includeGrowthReport ? (
                        <CheckCircle className="w-4 h-4 text-green-500" />
                      ) : (
                        <XCircle className="w-4 h-4 text-gray-300" />
                      )}
                      <span className={plan.includeGrowthReport ? 'text-gray-700' : 'text-gray-400'}>
                        성장 리포트
                      </span>
                    </div>
                  </div>

                  <div className="flex gap-2 pt-4 border-t">
                    <button
                      onClick={() => handleEditPlan(plan)}
                      className="flex-1 btn-secondary flex items-center justify-center gap-1"
                    >
                      <Edit2 className="w-4 h-4" />
                      수정
                    </button>
                    <button
                      onClick={() => handleDeletePlan(plan.id)}
                      className="p-2 text-gray-500 hover:text-red-600 hover:bg-gray-100 rounded"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      )}

      {activeTab === 'orders' && (
        <div className="card">
          {orders.length === 0 && !error ? (
            <div className="text-center py-12">
              <CreditCard className="w-12 h-12 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500">주문 내역이 없습니다.</p>
            </div>
          ) : orders.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="text-left text-sm text-gray-500 border-b">
                    <th className="pb-3 font-medium">주문번호</th>
                    <th className="pb-3 font-medium">사용자</th>
                    <th className="pb-3 font-medium">플랜</th>
                    <th className="pb-3 font-medium">금액</th>
                    <th className="pb-3 font-medium">상태</th>
                    <th className="pb-3 font-medium">주문일</th>
                    <th className="pb-3 font-medium">액션</th>
                  </tr>
                </thead>
                <tbody className="text-sm">
                  {orders.map((order) => (
                    <tr key={order.id} className="border-b last:border-0">
                      <td className="py-3 font-mono text-gray-600">{order.orderNumber}</td>
                      <td className="py-3">
                        <div>
                          <p className="font-medium text-gray-900">{order.userName}</p>
                          <p className="text-xs text-gray-500">{order.userEmail}</p>
                        </div>
                      </td>
                      <td className="py-3">{order.planName}</td>
                      <td className="py-3 font-medium">{formatCurrency(order.amount)}</td>
                      <td className="py-3">
                        <span className={`px-2 py-1 rounded text-xs font-medium ${ORDER_STATUS_COLORS[order.status]}`}>
                          {ORDER_STATUS_LABELS[order.status]}
                        </span>
                      </td>
                      <td className="py-3 text-gray-500">
                        {new Date(order.createdAt).toLocaleDateString('ko-KR')}
                      </td>
                      <td className="py-3">
                        {order.status === 'COMPLETED' && (
                          <button
                            onClick={() => handleRefundOrder(order.id)}
                            className="text-sm text-red-600 hover:text-red-700"
                          >
                            환불
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      )}

      {activeTab === 'subscriptions' && (
        <div className="card">
          {subscriptions.length === 0 && !error ? (
            <div className="text-center py-12">
              <Users className="w-12 h-12 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500">구독 현황이 없습니다.</p>
            </div>
          ) : subscriptions.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="text-left text-sm text-gray-500 border-b">
                    <th className="pb-3 font-medium">사용자</th>
                    <th className="pb-3 font-medium">플랜</th>
                    <th className="pb-3 font-medium">시즌</th>
                    <th className="pb-3 font-medium">상태</th>
                    <th className="pb-3 font-medium">멘토링</th>
                    <th className="pb-3 font-medium">시작일</th>
                    <th className="pb-3 font-medium">종료일</th>
                  </tr>
                </thead>
                <tbody className="text-sm">
                  {subscriptions.map((sub) => (
                    <tr key={sub.id} className="border-b last:border-0">
                      <td className="py-3">
                        <div>
                          <p className="font-medium text-gray-900">{sub.userName}</p>
                          <p className="text-xs text-gray-500">{sub.userEmail}</p>
                        </div>
                      </td>
                      <td className="py-3">{sub.planName}</td>
                      <td className="py-3">{sub.seasonTitle}</td>
                      <td className="py-3">
                        <span className={`px-2 py-1 rounded text-xs font-medium ${SUBSCRIPTION_STATUS_COLORS[sub.status]}`}>
                          {SUBSCRIPTION_STATUS_LABELS[sub.status]}
                        </span>
                      </td>
                      <td className="py-3">
                        {sub.hasMentoring ? (
                          <CheckCircle className="w-4 h-4 text-green-500" />
                        ) : (
                          <XCircle className="w-4 h-4 text-gray-300" />
                        )}
                      </td>
                      <td className="py-3 text-gray-500">
                        {new Date(sub.startDate).toLocaleDateString('ko-KR')}
                      </td>
                      <td className="py-3 text-gray-500">
                        {new Date(sub.endDate).toLocaleDateString('ko-KR')}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
        </div>
      )}

      {/* Plan Modal */}
      {showPlanModal && (
        <PlanModal
          plan={editingPlan}
          seasonId={selectedSeasonId!}
          onClose={() => setShowPlanModal(false)}
          onSave={() => {
            setShowPlanModal(false);
            loadData();
          }}
        />
      )}
    </div>
  );
}

// Plan Modal Component
function PlanModal({
  plan,
  seasonId,
  onClose,
  onSave,
}: {
  plan: PricingPlan | null;
  seasonId: number;
  onClose: () => void;
  onSave: () => void;
}) {
  const [formData, setFormData] = useState({
    name: plan?.name || '',
    description: plan?.description || '',
    price: plan?.price || 0,
    originalPrice: plan?.originalPrice || null,
    discountPercentage: plan?.discountPercentage || null,
    features: plan?.features?.join('\n') || '',
    includeMentoring: plan?.includeMentoring ?? false,
    includeAiCoaching: plan?.includeAiCoaching ?? true,
    includeGrowthReport: plan?.includeGrowthReport ?? true,
    maxTeamParticipation: plan?.maxTeamParticipation || 1,
    displayOrder: plan?.displayOrder || 0,
    recommended: plan?.recommended ?? false,
  });
  const [isSaving, setIsSaving] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);

    try {
      const payload = {
        ...formData,
        features: formData.features.split('\n').map((s) => s.trim()).filter(Boolean),
      };

      if (plan) {
        await adminService.updatePricingPlan(plan.id, payload);
      } else {
        await adminService.createPricingPlan(seasonId, payload);
      }
      onSave();
    } catch (error) {
      console.error('Failed to save plan:', error);
      alert('저장에 실패했습니다.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg w-full max-w-lg max-h-[90vh] overflow-y-auto">
        <div className="p-6 border-b border-gray-200">
          <h2 className="text-lg font-semibold">
            {plan ? '가격 정책 수정' : '새 가격 정책'}
          </h2>
        </div>
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              플랜 이름 *
            </label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="input"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              설명
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="input"
              rows={2}
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                가격 *
              </label>
              <input
                type="number"
                value={formData.price}
                onChange={(e) => setFormData({ ...formData, price: Number(e.target.value) })}
                className="input"
                min={0}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                원래 가격
              </label>
              <input
                type="number"
                value={formData.originalPrice || ''}
                onChange={(e) => setFormData({ ...formData, originalPrice: e.target.value ? Number(e.target.value) : null })}
                className="input"
                min={0}
              />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                할인율 (%)
              </label>
              <input
                type="number"
                value={formData.discountPercentage || ''}
                onChange={(e) => setFormData({ ...formData, discountPercentage: e.target.value ? Number(e.target.value) : null })}
                className="input"
                min={0}
                max={100}
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                표시 순서
              </label>
              <input
                type="number"
                value={formData.displayOrder}
                onChange={(e) => setFormData({ ...formData, displayOrder: Number(e.target.value) })}
                className="input"
                min={0}
              />
            </div>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              기능 목록 (줄바꿈으로 구분)
            </label>
            <textarea
              value={formData.features}
              onChange={(e) => setFormData({ ...formData, features: e.target.value })}
              className="input"
              rows={3}
              placeholder="팀 프로젝트 참여&#10;AI 코칭&#10;성장 리포트"
            />
          </div>
          <div className="space-y-2">
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="includeMentoring"
                checked={formData.includeMentoring}
                onChange={(e) => setFormData({ ...formData, includeMentoring: e.target.checked })}
                className="rounded border-gray-300"
              />
              <label htmlFor="includeMentoring" className="text-sm text-gray-700">
                멘토링 포함
              </label>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="includeAiCoaching"
                checked={formData.includeAiCoaching}
                onChange={(e) => setFormData({ ...formData, includeAiCoaching: e.target.checked })}
                className="rounded border-gray-300"
              />
              <label htmlFor="includeAiCoaching" className="text-sm text-gray-700">
                AI 코칭 포함
              </label>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="includeGrowthReport"
                checked={formData.includeGrowthReport}
                onChange={(e) => setFormData({ ...formData, includeGrowthReport: e.target.checked })}
                className="rounded border-gray-300"
              />
              <label htmlFor="includeGrowthReport" className="text-sm text-gray-700">
                성장 리포트 포함
              </label>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="recommended"
                checked={formData.recommended}
                onChange={(e) => setFormData({ ...formData, recommended: e.target.checked })}
                className="rounded border-gray-300"
              />
              <label htmlFor="recommended" className="text-sm text-gray-700">
                추천 플랜으로 표시
              </label>
            </div>
          </div>
          <div className="flex justify-end gap-3 pt-4 border-t">
            <button type="button" onClick={onClose} className="btn-secondary">
              취소
            </button>
            <button type="submit" className="btn-primary" disabled={isSaving}>
              {isSaving ? '저장 중...' : '저장'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
