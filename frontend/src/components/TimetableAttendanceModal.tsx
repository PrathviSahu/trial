import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
    X, Users, CheckCircle, XCircle, Clock, AlertTriangle,
    BookOpen, MapPin, User, Save, RefreshCw,
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import attendanceService, { AttendanceRecord, SlotAttendanceRecord } from '../services/attendanceService';
import { studentService } from '../services/studentService';

// ─── Types ────────────────────────────────────────────────────────────────────
interface SlotInfo {
    id: string;
    subject?: string;
    subjectAbbrev?: string;
    faculty?: string;
    classroom?: string;
    startTime?: string;
    endTime?: string;
    day?: string;
    department?: string;
    year?: number;
    batch?: string;
}

interface StudentRow {
    id: number;
    firstName: string;
    lastName: string;
    ienNumber: string;
    rollNumber?: number;
    status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED' | null;
}

interface Props {
    slot: SlotInfo;
    onClose: () => void;
}

// ─── Status pill config ───────────────────────────────────────────────────────
const STATUS_OPTIONS: {
    value: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';
    label: string;
    color: string;
    bg: string;
    border: string;
    icon: React.ReactNode;
}[] = [
        {
            value: 'PRESENT',
            label: 'Present',
            color: 'text-emerald-700 dark:text-emerald-300',
            bg: 'bg-emerald-100 dark:bg-emerald-900/40',
            border: 'border-emerald-400 dark:border-emerald-500',
            icon: <CheckCircle className="w-3.5 h-3.5" />,
        },
        {
            value: 'ABSENT',
            label: 'Absent',
            color: 'text-red-700 dark:text-red-300',
            bg: 'bg-red-100 dark:bg-red-900/40',
            border: 'border-red-400 dark:border-red-500',
            icon: <XCircle className="w-3.5 h-3.5" />,
        },
        {
            value: 'LATE',
            label: 'Late',
            color: 'text-amber-700 dark:text-amber-300',
            bg: 'bg-amber-100 dark:bg-amber-900/40',
            border: 'border-amber-400 dark:border-amber-500',
            icon: <Clock className="w-3.5 h-3.5" />,
        },
        {
            value: 'EXCUSED',
            label: 'Excused',
            color: 'text-blue-700 dark:text-blue-300',
            bg: 'bg-blue-100 dark:bg-blue-900/40',
            border: 'border-blue-400 dark:border-blue-500',
            icon: <AlertTriangle className="w-3.5 h-3.5" />,
        },
    ];

// ─── Component ────────────────────────────────────────────────────────────────
const TimetableAttendanceModal: React.FC<Props> = ({ slot, onClose }) => {
    const [students, setStudents] = useState<StudentRow[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [alreadyMarked, setAlreadyMarked] = useState(false);
    const [date] = useState(new Date().toISOString().split('T')[0]);

    const subjectKey = slot.subjectAbbrev
        ? `${slot.subjectAbbrev} - ${slot.subject || slot.subjectAbbrev}`
        : slot.subject || '';

    // Load students and existing attendance records
    const load = useCallback(async () => {
        setIsLoading(true);
        try {
            // 1. Load all students (filter by dept/year if available)
            const allStudentsResp = await studentService.getAllStudents();
            let filtered = allStudentsResp.content || [];
            if (slot.department) {
                filtered = filtered.filter((s: any) => s.department === slot.department);
            }
            if (slot.year) {
                filtered = filtered.filter((s: any) => s.year === slot.year);
            }

            // 2. Load existing attendance for this slot today
            const existing: AttendanceRecord[] = await attendanceService.getSlotAttendance(subjectKey, date);
            const existingMap = new Map<number, 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED'>();
            existing.forEach((r: any) => {
                const sid = r.student?.id ?? r.studentId;
                if (sid) existingMap.set(sid, r.status || 'PRESENT');
            });

            const rows: StudentRow[] = filtered.map((s: any) => ({
                id: s.id,
                firstName: s.firstName || s.first_name || '',
                lastName: s.lastName || s.last_name || '',
                ienNumber: s.ienNumber || s.ien_number || '',
                rollNumber: s.rollNumber || s.roll_number,
                status: existingMap.get(s.id) ?? null,
            }));

            setStudents(rows);
            setAlreadyMarked(existing.length > 0);
        } catch (err) {
            console.error('Failed to load data for attendance modal:', err);
            toast.error('Failed to load students');
        } finally {
            setIsLoading(false);
        }
    }, [slot, subjectKey, date]);

    useEffect(() => { load(); }, [load]);

    // Set a single student's status
    const setStatus = (id: number, status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED') => {
        setStudents(prev => prev.map(s => s.id === id ? { ...s, status } : s));
    };

    // Mark all as a given status
    const markAll = (status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED') => {
        setStudents(prev => prev.map(s => ({ ...s, status })));
    };

    // Save attendance
    const handleSave = async () => {
        const unmarked = students.filter(s => s.status === null);
        if (unmarked.length > 0) {
            toast.error(`${unmarked.length} student(s) still unmarked`);
            return;
        }

        setIsSaving(true);
        try {
            const records: SlotAttendanceRecord[] = students.map(s => ({
                studentId: s.id,
                status: s.status!,
                subject: subjectKey,
            }));

            const result = await attendanceService.markSlotAttendance(slot.id, subjectKey, records);
            if (result.success) {
                toast.success(`Attendance saved for ${result.saved ?? students.length} students`);
                setAlreadyMarked(true);
                onClose();
            } else {
                toast.error(result.message || 'Failed to save attendance');
            }
        } catch (err) {
            toast.error('Failed to save attendance');
        } finally {
            setIsSaving(false);
        }
    };

    // Stats
    const stats = {
        present: students.filter(s => s.status === 'PRESENT').length,
        absent: students.filter(s => s.status === 'ABSENT').length,
        late: students.filter(s => s.status === 'LATE').length,
        excused: students.filter(s => s.status === 'EXCUSED').length,
        unmarked: students.filter(s => s.status === null).length,
    };

    return (
        <AnimatePresence>
            <motion.div
                className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
            >
                <motion.div
                    className="bg-white dark:bg-gray-900 rounded-2xl shadow-2xl w-full max-w-3xl max-h-[90vh] flex flex-col border border-gray-200 dark:border-gray-700 overflow-hidden"
                    initial={{ scale: 0.92, opacity: 0, y: 24 }}
                    animate={{ scale: 1, opacity: 1, y: 0 }}
                    exit={{ scale: 0.92, opacity: 0, y: 24 }}
                    transition={{ type: 'spring', damping: 26, stiffness: 320 }}
                >
                    {/* Header */}
                    <div className="relative bg-gradient-to-r from-violet-600 to-indigo-600 px-6 py-5 flex-shrink-0">
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2 mb-1">
                                    <BookOpen className="w-5 h-5 text-violet-200 flex-shrink-0" />
                                    <h2 className="text-white font-bold text-lg leading-tight truncate">
                                        {slot.subject || slot.subjectAbbrev || 'Untitled Subject'}
                                    </h2>
                                    {slot.subjectAbbrev && (
                                        <span className="text-xs bg-white/20 text-white px-2 py-0.5 rounded-full font-mono flex-shrink-0">
                                            {slot.subjectAbbrev}
                                        </span>
                                    )}
                                </div>
                                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-violet-200 text-sm">
                                    {slot.faculty && (
                                        <span className="flex items-center gap-1">
                                            <User className="w-3.5 h-3.5" /> {slot.faculty}
                                        </span>
                                    )}
                                    {slot.classroom && (
                                        <span className="flex items-center gap-1">
                                            <MapPin className="w-3.5 h-3.5" /> {slot.classroom}
                                        </span>
                                    )}
                                    {slot.startTime && slot.endTime && (
                                        <span className="flex items-center gap-1">
                                            <Clock className="w-3.5 h-3.5" /> {slot.startTime} – {slot.endTime}
                                        </span>
                                    )}
                                    {slot.day && (
                                        <span className="capitalize">{slot.day}</span>
                                    )}
                                </div>
                            </div>
                            <button
                                onClick={onClose}
                                className="text-white/80 hover:text-white hover:bg-white/20 p-1.5 rounded-lg transition-colors flex-shrink-0"
                            >
                                <X className="w-5 h-5" />
                            </button>
                        </div>

                        {/* Already marked banner */}
                        {alreadyMarked && (
                            <div className="mt-3 flex items-center gap-2 bg-emerald-500/30 border border-emerald-400/40 rounded-lg px-3 py-2 text-sm text-white">
                                <CheckCircle className="w-4 h-4 flex-shrink-0" />
                                Attendance already marked today — you can update it
                            </div>
                        )}
                    </div>

                    {/* Stat chips */}
                    <div className="flex items-center gap-2 px-6 py-3 bg-gray-50 dark:bg-gray-800/60 border-b border-gray-200 dark:border-gray-700 flex-shrink-0 flex-wrap">
                        {[
                            { label: 'Present', count: stats.present, color: 'text-emerald-600 dark:text-emerald-400' },
                            { label: 'Absent', count: stats.absent, color: 'text-red-600 dark:text-red-400' },
                            { label: 'Late', count: stats.late, color: 'text-amber-600 dark:text-amber-400' },
                            { label: 'Excused', count: stats.excused, color: 'text-blue-600 dark:text-blue-400' },
                            { label: 'Unmarked', count: stats.unmarked, color: 'text-gray-500 dark:text-gray-400' },
                        ].map(({ label, count, color }) => (
                            <span key={label} className="flex items-center gap-1.5 text-xs font-medium bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 px-3 py-1 rounded-full shadow-sm">
                                <span className={`font-bold text-sm ${color}`}>{count}</span>
                                <span className="text-gray-500 dark:text-gray-400">{label}</span>
                            </span>
                        ))}
                        <div className="ml-auto flex gap-2">
                            <button onClick={load} title="Refresh" className="p-1.5 text-gray-500 hover:text-gray-700 dark:hover:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-700 rounded-lg transition-colors">
                                <RefreshCw className="w-4 h-4" />
                            </button>
                        </div>
                    </div>

                    {/* Bulk actions */}
                    <div className="flex items-center gap-2 px-6 py-2.5 bg-gray-50 dark:bg-gray-800/40 border-b border-gray-200 dark:border-gray-700 flex-shrink-0">
                        <Users className="w-4 h-4 text-gray-500" />
                        <span className="text-xs text-gray-600 dark:text-gray-400 font-medium mr-1">Mark all:</span>
                        {STATUS_OPTIONS.map(opt => (
                            <button
                                key={opt.value}
                                onClick={() => markAll(opt.value)}
                                className={`flex items-center gap-1 px-2.5 py-1 text-xs font-medium rounded-full border transition-all hover:shadow-sm ${opt.color} ${opt.bg} ${opt.border}`}
                            >
                                {opt.icon} {opt.label}
                            </button>
                        ))}
                    </div>

                    {/* Student list */}
                    <div className="flex-1 overflow-y-auto">
                        {isLoading ? (
                            <div className="flex flex-col items-center justify-center h-48 gap-3 text-gray-500">
                                <div className="w-8 h-8 border-4 border-violet-300 border-t-violet-600 rounded-full animate-spin" />
                                <span className="text-sm">Loading students...</span>
                            </div>
                        ) : students.length === 0 ? (
                            <div className="flex flex-col items-center justify-center h-48 gap-3 text-gray-400">
                                <Users className="w-10 h-10 opacity-40" />
                                <span className="text-sm">No students found for this class</span>
                                <p className="text-xs text-center max-w-xs">
                                    Students will appear here if they are registered under the same department &amp; year as this slot.
                                </p>
                            </div>
                        ) : (
                            <div className="divide-y divide-gray-100 dark:divide-gray-800">
                                {students.map((student, idx) => {
                                    const currentOpt = STATUS_OPTIONS.find(o => o.value === student.status);
                                    return (
                                        <motion.div
                                            key={student.id}
                                            initial={{ opacity: 0, x: -10 }}
                                            animate={{ opacity: 1, x: 0 }}
                                            transition={{ delay: Math.min(idx * 0.02, 0.3) }}
                                            className="flex items-center gap-3 px-5 py-3 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                                        >
                                            {/* Serial number */}
                                            <span className="text-xs text-gray-400 w-6 text-right flex-shrink-0">{idx + 1}</span>

                                            {/* Avatar */}
                                            <div
                                                className={`w-9 h-9 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0 ${student.status === 'PRESENT'
                                                    ? 'bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-300'
                                                    : student.status === 'ABSENT'
                                                        ? 'bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-300'
                                                        : student.status === 'LATE'
                                                            ? 'bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-300'
                                                            : student.status === 'EXCUSED'
                                                                ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/50 dark:text-blue-300'
                                                                : 'bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400'
                                                    }`}
                                            >
                                                {student.firstName[0] || '?'}{student.lastName[0] || ''}
                                            </div>

                                            {/* Name & IEN */}
                                            <div className="flex-1 min-w-0">
                                                <div className="text-sm font-medium text-gray-900 dark:text-white truncate">
                                                    {student.firstName} {student.lastName}
                                                </div>
                                                <div className="text-xs text-gray-500 dark:text-gray-400">
                                                    {student.ienNumber}
                                                    {student.rollNumber && ` · Roll ${student.rollNumber}`}
                                                </div>
                                            </div>

                                            {/* Status buttons */}
                                            <div className="flex items-center gap-1.5 flex-shrink-0">
                                                {STATUS_OPTIONS.map(opt => {
                                                    const selected = student.status === opt.value;
                                                    return (
                                                        <button
                                                            key={opt.value}
                                                            onClick={() => setStatus(student.id, opt.value)}
                                                            title={opt.label}
                                                            className={`flex items-center gap-1 px-2 py-1 text-xs font-semibold rounded-full border transition-all duration-150 ${selected
                                                                ? `${opt.bg} ${opt.color} ${opt.border} shadow-sm scale-105`
                                                                : 'bg-transparent border-gray-200 dark:border-gray-600 text-gray-400 hover:border-gray-300 dark:hover:border-gray-500 hover:text-gray-600 dark:hover:text-gray-300'
                                                                }`}
                                                        >
                                                            {opt.icon}
                                                            <span className="hidden sm:inline">{opt.label}</span>
                                                        </button>
                                                    );
                                                })}
                                            </div>

                                            {/* Current status badge (small) */}
                                            {currentOpt ? (
                                                <span
                                                    className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 flex-shrink-0 ${currentOpt.bg} ${currentOpt.color} ${currentOpt.border} border`}
                                                >
                                                    {currentOpt.icon}
                                                </span>
                                            ) : (
                                                <span className="w-6 flex-shrink-0" />
                                            )}
                                        </motion.div>
                                    );
                                })}
                            </div>
                        )}
                    </div>

                    {/* Footer */}
                    <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 flex-shrink-0">
                        <div className="text-sm text-gray-600 dark:text-gray-400">
                            <span className="font-semibold text-gray-900 dark:text-white">{students.length}</span> students ·{' '}
                            {stats.unmarked > 0 ? (
                                <span className="text-amber-600 font-medium">{stats.unmarked} unmarked</span>
                            ) : (
                                <span className="text-emerald-600 font-medium">All marked ✓</span>
                            )}
                        </div>
                        <div className="flex gap-3">
                            <button
                                onClick={onClose}
                                className="px-4 py-2 text-sm font-medium text-gray-600 dark:text-gray-300 bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 rounded-lg transition-colors"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleSave}
                                disabled={isSaving || isLoading || students.length === 0}
                                className="flex items-center gap-2 px-5 py-2 text-sm font-semibold text-white bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed rounded-lg shadow transition-all duration-150 hover:shadow-md"
                            >
                                {isSaving ? (
                                    <><div className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full animate-spin" /> Saving...</>
                                ) : (
                                    <><Save className="w-4 h-4" /> Save Attendance</>
                                )}
                            </button>
                        </div>
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
};

export default TimetableAttendanceModal;
