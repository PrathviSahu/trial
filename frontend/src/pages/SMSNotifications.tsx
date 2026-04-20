import { apiUrl } from '../config/api';
import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
    MessageSquare,
    Send,
    Trash2,
    Phone,
    Users,
    AlertTriangle,
    RefreshCw,
    ChevronDown,
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import LoadingSpinner from '../components/LoadingSpinner';

interface SMSLog {
    to: string;
    studentName: string;
    message: string;
    type: string;
    sentAt: string;
    status: string;
}

interface Student {
    id: number;
    firstName: string;
    lastName: string;
    ienNumber: string;
    phoneNumber?: string;
}

const SMSNotifications: React.FC = () => {
    const [history, setHistory] = useState<SMSLog[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isSending, setIsSending] = useState(false);
    const [students, setStudents] = useState<Student[]>([]);
    const [selectedStudentId, setSelectedStudentId] = useState<number | ''>('');
    const [customMessage, setCustomMessage] = useState('');
    const [threshold, setThreshold] = useState(75);
    const [adminPhone, setAdminPhone] = useState('');

    useEffect(() => {
        loadHistory();
        loadStudents();
    }, []);

    const loadHistory = async () => {
        try {
            const res = await fetch(apiUrl("/sms/history"));
            const data = await res.json();
            if (data.success) setHistory(data.messages || []);
        } catch { toast.error('Failed to load SMS history'); }
    };

    const loadStudents = async () => {
        try {
            const res = await fetch(apiUrl("/students?size=1000"));
            const data = await res.json();
            if (data.success) setStudents(data.data?.content || data.data || []);
        } catch { }
    };

    const sendLowAttendanceAlerts = async () => {
        setIsSending(true);
        try {
            const res = await fetch(apiUrl(`/sms/alerts/low-attendance?threshold=${threshold}`), { method: 'POST' });
            const data = await res.json();
            if (data.success) {
                toast.success(`${data.smsSent} SMS alerts sent`);
                loadHistory();
            } else toast.error(data.message);
        } catch { toast.error('Server error'); }
        finally { setIsSending(false); }
    };

    const sendDailySummary = async () => {
        if (!adminPhone.trim()) { toast.error('Enter admin phone number'); return; }
        setIsSending(true);
        try {
            const res = await fetch(apiUrl(`/sms/summary/daily?phone=${encodeURIComponent(adminPhone)}`), { method: 'POST' });
            const data = await res.json();
            if (data.success) { toast.success('Daily summary SMS sent'); loadHistory(); }
            else toast.error(data.message);
        } catch { toast.error('Server error'); }
        finally { setIsSending(false); }
    };

    const sendCustomSMS = async () => {
        if (!selectedStudentId || !customMessage.trim()) { toast.error('Select student and enter message'); return; }
        setIsSending(true);
        try {
            const res = await fetch(apiUrl("/sms/send"), {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ studentId: selectedStudentId, message: customMessage }),
            });
            const data = await res.json();
            if (data.success) {
                toast.success(data.message);
                setCustomMessage('');
                setSelectedStudentId('');
                loadHistory();
            } else toast.error(data.message);
        } catch { toast.error('Server error'); }
        finally { setIsSending(false); }
    };

    const clearHistory = async () => {
        try {
            await fetch(apiUrl("/sms/history"), { method: 'DELETE' });
            setHistory([]);
            toast.success('SMS history cleared');
        } catch { toast.error('Error'); }
    };

    const typeColors: Record<string, string> = {
        LOW_ATTENDANCE_ALERT: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
        DAILY_SUMMARY: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
        CUSTOM: 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400',
    };

    return (
        <div className="space-y-6">
            {/* Header */}
            <motion.div
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-gradient-to-r from-green-500 to-teal-600 rounded-xl p-6 text-white"
            >
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <MessageSquare className="w-8 h-8" />
                        <div>
                            <h1 className="text-2xl font-bold">SMS / WhatsApp Notifications</h1>
                            <p className="text-green-100 text-sm">Send attendance alerts to parents and guardians</p>
                        </div>
                    </div>
                    <button onClick={loadHistory} className="bg-white/20 hover:bg-white/30 p-2 rounded-lg transition-colors">
                        <RefreshCw className="w-5 h-5" />
                    </button>
                </div>
            </motion.div>

            {/* Action Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* Low Attendance */}
                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
                    className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
                >
                    <div className="flex items-center gap-2 mb-4">
                        <AlertTriangle className="w-5 h-5 text-red-500" />
                        <h3 className="font-semibold text-gray-900 dark:text-white">Low Attendance Alerts</h3>
                    </div>
                    <p className="text-sm text-gray-500 mb-4">Send SMS to parents of students with attendance below threshold</p>
                    <div className="mb-3">
                        <label className="text-xs text-gray-500">Threshold (%)</label>
                        <input
                            type="number" value={threshold} onChange={e => setThreshold(Number(e.target.value))} min={50} max={100}
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"
                        />
                    </div>
                    <button onClick={sendLowAttendanceAlerts} disabled={isSending}
                        className="w-full flex items-center justify-center gap-2 py-2 bg-red-500 hover:bg-red-600 disabled:bg-gray-300 text-white rounded-lg text-sm font-medium transition-colors"
                    >
                        <Send className="w-4 h-4" /> Send Alerts
                    </button>
                </motion.div>

                {/* Daily Summary */}
                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}
                    className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
                >
                    <div className="flex items-center gap-2 mb-4">
                        <Phone className="w-5 h-5 text-blue-500" />
                        <h3 className="font-semibold text-gray-900 dark:text-white">Daily Summary</h3>
                    </div>
                    <p className="text-sm text-gray-500 mb-4">Send today's attendance summary to admin phone</p>
                    <div className="mb-3">
                        <label className="text-xs text-gray-500">Admin Phone</label>
                        <input
                            value={adminPhone} onChange={e => setAdminPhone(e.target.value)} placeholder="+91 9876543210"
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"
                        />
                    </div>
                    <button onClick={sendDailySummary} disabled={isSending}
                        className="w-full flex items-center justify-center gap-2 py-2 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 text-white rounded-lg text-sm font-medium transition-colors"
                    >
                        <Send className="w-4 h-4" /> Send Summary
                    </button>
                </motion.div>

                {/* Custom SMS */}
                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }}
                    className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
                >
                    <div className="flex items-center gap-2 mb-4">
                        <Users className="w-5 h-5 text-purple-500" />
                        <h3 className="font-semibold text-gray-900 dark:text-white">Custom Message</h3>
                    </div>
                    <div className="space-y-3">
                        <select
                            value={selectedStudentId} onChange={e => setSelectedStudentId(e.target.value ? Number(e.target.value) : '')}
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm"
                        >
                            <option value="">Select student</option>
                            {students.map(s => (
                                <option key={s.id} value={s.id}>{s.firstName} {s.lastName}</option>
                            ))}
                        </select>
                        <textarea
                            value={customMessage} onChange={e => setCustomMessage(e.target.value)} placeholder="Type your message..." rows={2}
                            className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white text-sm resize-none"
                        />
                    </div>
                    <button onClick={sendCustomSMS} disabled={isSending}
                        className="w-full mt-3 flex items-center justify-center gap-2 py-2 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-300 text-white rounded-lg text-sm font-medium transition-colors"
                    >
                        <Send className="w-4 h-4" /> Send
                    </button>
                </motion.div>
            </div>

            {/* History */}
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }}
                className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
            >
                <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white">SMS History ({history.length})</h3>
                    {history.length > 0 && (
                        <button onClick={clearHistory} className="text-red-500 hover:text-red-600 text-sm flex items-center gap-1">
                            <Trash2 className="w-4 h-4" /> Clear
                        </button>
                    )}
                </div>
                {history.length === 0 ? (
                    <p className="text-center text-gray-400 py-8">No SMS sent yet</p>
                ) : (
                    <div className="space-y-2 max-h-[400px] overflow-y-auto">
                        {history.map((sms, i) => (
                            <div key={i} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2 mb-1">
                                        <span className="font-medium text-sm text-gray-900 dark:text-white">{sms.studentName}</span>
                                        <span className={`px-2 py-0.5 rounded text-xs font-medium ${typeColors[sms.type] || 'bg-gray-100 text-gray-600'}`}>
                                            {sms.type.replace(/_/g, ' ')}
                                        </span>
                                    </div>
                                    <p className="text-xs text-gray-500 truncate">{sms.message}</p>
                                </div>
                                <div className="text-right ml-4 whitespace-nowrap">
                                    <p className="text-xs text-gray-400">{sms.sentAt}</p>
                                    <p className="text-xs text-green-500">{sms.status}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </motion.div>
        </div>
    );
};

export default SMSNotifications;
