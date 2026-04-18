import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    FileText,
    Plus,
    CheckCircle,
    XCircle,
    Clock,
    Users,
    Calendar,
    ChevronDown,
    RefreshCw,
    Trash2,
} from 'lucide-react';
import { toast } from 'react-hot-toast';

interface Student {
    id: number;
    firstName: string;
    lastName: string;
    ienNumber: string;
    department: string;
}

interface Leave {
    id: number;
    studentId: number;
    studentName: string;
    ienNumber: string;
    department: string;
    leaveType: string;
    startDate: string;
    endDate: string;
    reason: string;
    status: string;
    approvedBy: string | null;
    adminRemarks: string | null;
    createdAt: string | null;
}

const leaveTypes = ['SICK', 'PERSONAL', 'FAMILY', 'OTHER'];
const statusColors: Record<string, string> = {
    PENDING: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
    APPROVED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
    REJECTED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
};

const LeaveManagement: React.FC = () => {
    const [leaves, setLeaves] = useState<Leave[]>([]);
    const [students, setStudents] = useState<Student[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [showForm, setShowForm] = useState(false);
    const [filter, setFilter] = useState('');
    const [reviewModal, setReviewModal] = useState<Leave | null>(null);
    const [reviewRemarks, setReviewRemarks] = useState('');

    // Form state
    const [formStudentId, setFormStudentId] = useState<number | ''>('');
    const [formType, setFormType] = useState('SICK');
    const [formStart, setFormStart] = useState('');
    const [formEnd, setFormEnd] = useState('');
    const [formReason, setFormReason] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        loadData();
    }, [filter]);

    const loadData = async () => {
        setIsLoading(true);
        try {
            const url = filter
                ? `http://localhost:8080/api/leave?status=${filter}`
                : 'http://localhost:8080/api/leave';
            const [leavesRes, studentsRes] = await Promise.all([
                fetch(url),
                fetch('http://localhost:8080/api/students?size=1000'),
            ]);
            const leavesData = await leavesRes.json();
            const studentsData = await studentsRes.json();
            if (leavesData.success) setLeaves(leavesData.data || []);
            if (studentsData.success) setStudents(studentsData.data?.content || studentsData.data || []);
        } catch { toast.error('Failed to load data'); }
        finally { setIsLoading(false); }
    };

    const submitLeave = async () => {
        if (!formStudentId || !formStart || !formEnd) {
            toast.error('Fill all required fields');
            return;
        }
        setIsSubmitting(true);
        try {
            const res = await fetch('http://localhost:8080/api/leave/apply', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    studentId: formStudentId,
                    leaveType: formType,
                    startDate: formStart,
                    endDate: formEnd,
                    reason: formReason,
                }),
            });
            const data = await res.json();
            if (data.success) {
                toast.success('Leave request submitted');
                setShowForm(false);
                setFormStudentId('');
                setFormType('SICK');
                setFormStart('');
                setFormEnd('');
                setFormReason('');
                loadData();
            } else toast.error(data.message);
        } catch { toast.error('Server error'); }
        finally { setIsSubmitting(false); }
    };

    const reviewLeave = async (leaveId: number, action: 'APPROVED' | 'REJECTED') => {
        try {
            const res = await fetch(`http://localhost:8080/api/leave/${leaveId}/review`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action, remarks: reviewRemarks, approvedBy: 'Admin' }),
            });
            const data = await res.json();
            if (data.success) {
                toast.success(`Leave ${action.toLowerCase()}`);
                setReviewModal(null);
                setReviewRemarks('');
                loadData();
            } else toast.error(data.message);
        } catch { toast.error('Server error'); }
    };

    const deleteLeave = async (id: number) => {
        try {
            await fetch(`http://localhost:8080/api/leave/${id}`, { method: 'DELETE' });
            toast.success('Leave request deleted');
            loadData();
        } catch { toast.error('Error'); }
    };

    const pendingCount = leaves.filter(l => l.status === 'PENDING').length;
    const approvedCount = leaves.filter(l => l.status === 'APPROVED').length;
    const rejectedCount = leaves.filter(l => l.status === 'REJECTED').length;

    return (
        <div className="space-y-6">
            {/* Header */}
            <motion.div
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-gradient-to-r from-indigo-500 to-purple-600 rounded-xl p-6 text-white"
            >
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <FileText className="w-8 h-8" />
                        <div>
                            <h1 className="text-2xl font-bold">Leave Management</h1>
                            <p className="text-indigo-100 text-sm">Manage student leave requests — apply, review, and track</p>
                        </div>
                    </div>
                    <div className="flex items-center gap-2">
                        <button onClick={() => setShowForm(true)} className="flex items-center gap-2 bg-white/20 hover:bg-white/30 px-4 py-2 rounded-lg transition-colors text-sm font-medium">
                            <Plus className="w-4 h-4" /> New Request
                        </button>
                        <button onClick={loadData} className="bg-white/20 hover:bg-white/30 p-2 rounded-lg transition-colors">
                            <RefreshCw className="w-5 h-5" />
                        </button>
                    </div>
                </div>
            </motion.div>

            {/* Stats */}
            <div className="grid grid-cols-3 gap-4">
                {[
                    { label: 'Pending', count: pendingCount, color: 'text-yellow-600', bg: 'bg-yellow-50 dark:bg-yellow-900/20', icon: Clock },
                    { label: 'Approved', count: approvedCount, color: 'text-green-600', bg: 'bg-green-50 dark:bg-green-900/20', icon: CheckCircle },
                    { label: 'Rejected', count: rejectedCount, color: 'text-red-600', bg: 'bg-red-50 dark:bg-red-900/20', icon: XCircle },
                ].map(s => (
                    <div key={s.label} className={`${s.bg} rounded-xl p-4 text-center`}>
                        <s.icon className={`w-6 h-6 mx-auto mb-1 ${s.color}`} />
                        <p className={`text-2xl font-bold ${s.color}`}>{s.count}</p>
                        <p className="text-sm text-gray-500">{s.label}</p>
                    </div>
                ))}
            </div>

            {/* Filter */}
            <div className="flex gap-2">
                {['', 'PENDING', 'APPROVED', 'REJECTED'].map(f => (
                    <button
                        key={f}
                        onClick={() => setFilter(f)}
                        className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${filter === f
                                ? 'bg-indigo-500 text-white'
                                : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-gray-700'
                            }`}
                    >
                        {f || 'All'}
                    </button>
                ))}
            </div>

            {/* Leave List */}
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}
                className="bg-white dark:bg-gray-800 rounded-xl shadow-lg border border-gray-200 dark:border-gray-700 overflow-hidden"
            >
                {isLoading ? (
                    <div className="p-12 text-center text-gray-400">Loading...</div>
                ) : leaves.length === 0 ? (
                    <div className="p-12 text-center text-gray-400">
                        <FileText className="w-12 h-12 mx-auto mb-3 opacity-30" />
                        <p>No leave requests found</p>
                    </div>
                ) : (
                    <div className="divide-y divide-gray-200 dark:divide-gray-700">
                        {leaves.map(leave => (
                            <div key={leave.id} className="p-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
                                <div className="flex items-center justify-between">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-3 mb-1">
                                            <span className="font-semibold text-gray-900 dark:text-white">{leave.studentName}</span>
                                            <span className={`px-2 py-0.5 rounded text-xs font-medium ${statusColors[leave.status]}`}>
                                                {leave.status}
                                            </span>
                                            <span className="px-2 py-0.5 rounded text-xs font-medium bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300">
                                                {leave.leaveType}
                                            </span>
                                        </div>
                                        <p className="text-sm text-gray-500">
                                            <Calendar className="w-3 h-3 inline mr-1" />
                                            {leave.startDate} → {leave.endDate}
                                            {leave.reason && <span className="ml-2">• {leave.reason}</span>}
                                        </p>
                                        {leave.adminRemarks && (
                                            <p className="text-xs text-gray-400 mt-1">Admin: {leave.adminRemarks}</p>
                                        )}
                                    </div>
                                    <div className="flex items-center gap-2 ml-4">
                                        {leave.status === 'PENDING' && (
                                            <>
                                                <button onClick={() => { setReviewModal(leave); setReviewRemarks(''); }}
                                                    className="px-3 py-1 bg-indigo-100 text-indigo-700 rounded text-xs font-medium hover:bg-indigo-200 transition-colors"
                                                >
                                                    Review
                                                </button>
                                            </>
                                        )}
                                        <button onClick={() => deleteLeave(leave.id)} className="text-gray-400 hover:text-red-500 transition-colors">
                                            <Trash2 className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </motion.div>

            {/* Apply Form Modal */}
            <AnimatePresence>
                {showForm && (
                    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4"
                        onClick={() => setShowForm(false)}
                    >
                        <motion.div initial={{ scale: 0.9 }} animate={{ scale: 1 }} exit={{ scale: 0.9 }}
                            className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-lg p-6" onClick={e => e.stopPropagation()}
                        >
                            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Apply for Leave</h3>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Student *</label>
                                    <select value={formStudentId} onChange={e => setFormStudentId(e.target.value ? Number(e.target.value) : '')}
                                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                                    >
                                        <option value="">Select student</option>
                                        {students.map(s => <option key={s.id} value={s.id}>{s.firstName} {s.lastName} ({s.ienNumber})</option>)}
                                    </select>
                                </div>
                                <div className="grid grid-cols-3 gap-3">
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Type</label>
                                        <select value={formType} onChange={e => setFormType(e.target.value)}
                                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                                        >
                                            {leaveTypes.map(t => <option key={t} value={t}>{t}</option>)}
                                        </select>
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">From *</label>
                                        <input type="date" value={formStart} onChange={e => setFormStart(e.target.value)}
                                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                                        />
                                    </div>
                                    <div>
                                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">To *</label>
                                        <input type="date" value={formEnd} onChange={e => setFormEnd(e.target.value)}
                                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
                                        />
                                    </div>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Reason</label>
                                    <textarea value={formReason} onChange={e => setFormReason(e.target.value)} rows={3} placeholder="Explain reason for leave..."
                                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white resize-none"
                                    />
                                </div>
                            </div>
                            <div className="flex justify-end gap-3 mt-6">
                                <button onClick={() => setShowForm(false)} className="px-4 py-2 text-gray-600 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors">Cancel</button>
                                <button onClick={submitLeave} disabled={isSubmitting}
                                    className="px-6 py-2 bg-indigo-500 hover:bg-indigo-600 disabled:bg-gray-300 text-white rounded-lg font-medium transition-colors"
                                >
                                    {isSubmitting ? 'Submitting...' : 'Submit'}
                                </button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>

            {/* Review Modal */}
            <AnimatePresence>
                {reviewModal && (
                    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                        className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4"
                        onClick={() => setReviewModal(null)}
                    >
                        <motion.div initial={{ scale: 0.9 }} animate={{ scale: 1 }} exit={{ scale: 0.9 }}
                            className="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-full max-w-md p-6" onClick={e => e.stopPropagation()}
                        >
                            <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-2">Review Leave Request</h3>
                            <div className="text-sm text-gray-600 dark:text-gray-300 space-y-1 mb-4">
                                <p><strong>Student:</strong> {reviewModal.studentName}</p>
                                <p><strong>Type:</strong> {reviewModal.leaveType}</p>
                                <p><strong>Dates:</strong> {reviewModal.startDate} → {reviewModal.endDate}</p>
                                <p><strong>Reason:</strong> {reviewModal.reason || '—'}</p>
                            </div>
                            <textarea
                                value={reviewRemarks} onChange={e => setReviewRemarks(e.target.value)} rows={2} placeholder="Admin remarks (optional)"
                                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white resize-none mb-4"
                            />
                            <div className="flex justify-end gap-3">
                                <button onClick={() => reviewLeave(reviewModal.id, 'REJECTED')}
                                    className="px-4 py-2 bg-red-500 hover:bg-red-600 text-white rounded-lg text-sm font-medium transition-colors"
                                >Reject</button>
                                <button onClick={() => reviewLeave(reviewModal.id, 'APPROVED')}
                                    className="px-4 py-2 bg-green-500 hover:bg-green-600 text-white rounded-lg text-sm font-medium transition-colors"
                                >Approve</button>
                            </div>
                        </motion.div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
};

export default LeaveManagement;
