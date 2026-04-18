import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import {
    Award,
    Users,
    Printer,
    RefreshCw,
    ChevronDown,
    CheckCircle,
    Download,
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import LoadingSpinner from '../components/LoadingSpinner';

interface Student {
    id: number;
    firstName: string;
    lastName: string;
    ienNumber: string;
    department: string;
    year: number;
    semester: number;
    rollNumber?: string;
    branch?: string;
}

const AttendanceCertificate: React.FC = () => {
    const [students, setStudents] = useState<Student[]>([]);
    const [selectedStudentId, setSelectedStudentId] = useState<number | ''>('');
    const [isLoadingStudents, setIsLoadingStudents] = useState(true);
    const [attendanceData, setAttendanceData] = useState<any>(null);
    const [isFetching, setIsFetching] = useState(false);
    const [showCertificate, setShowCertificate] = useState(false);

    const today = new Date().toLocaleDateString('en-IN', {
        day: 'numeric', month: 'long', year: 'numeric',
    });

    useEffect(() => {
        loadStudents();
    }, []);

    const loadStudents = async () => {
        try {
            setIsLoadingStudents(true);
            const res = await fetch('http://localhost:8080/api/students?size=1000');
            const data = await res.json();
            if (data.success) {
                const list: Student[] = data.data?.content || data.data || [];
                setStudents(list);
            }
        } catch {
            toast.error('Failed to load students');
        } finally {
            setIsLoadingStudents(false);
        }
    };

    const generateCertificate = async () => {
        if (!selectedStudentId) {
            toast.error('Please select a student');
            return;
        }
        setIsFetching(true);
        setShowCertificate(false);
        try {
            // Fetch attendance records for this student via all attendance records
            const res = await fetch('http://localhost:8080/api/attendance');
            const data = await res.json();

            let presentCount = 0;
            if (data.success && data.data) {
                presentCount = (data.data as any[]).filter(
                    (r: any) => r.student?.id === selectedStudentId
                ).length;
            }

            // Also get student's total for percentage context
            const studentsRes = await fetch('http://localhost:8080/api/students?size=1000');
            const studentsData = await studentsRes.json();
            const totalStudents = studentsData.data?.content?.length || 0;

            setAttendanceData({ presentCount });
            setShowCertificate(true);
            toast.success('Certificate generated!');
        } catch {
            toast.error('Failed to fetch attendance data');
        } finally {
            setIsFetching(false);
        }
    };

    const handlePrint = () => {
        window.print();
    };

    const selectedStudent = students.find(s => s.id === selectedStudentId);
    const totalClasses = 100; // configurable default
    const attendancePercent = attendanceData
        ? Math.min(Math.round((attendanceData.presentCount / totalClasses) * 100), 100)
        : 0;
    const status =
        attendancePercent >= 75 ? 'SATISFACTORY' : 'BELOW REQUIRED';

    return (
        <div className="space-y-6">
            {/* Print styles */}
            <style>{`
        @media print {
          .no-print { display: none !important; }
          .print-area { 
            position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background: white; z-index: 9999; padding: 40px;
            display: flex; flex-direction: column; align-items: center; justify-content: center;
          }
          body > * { display: none; }
          .print-area { display: flex !important; }
        }
      `}</style>

            {/* Header - no-print */}
            <motion.div
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="no-print bg-gradient-to-r from-amber-500 to-orange-600 rounded-xl p-6 text-white"
            >
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <Award className="w-8 h-8" />
                        <div>
                            <h1 className="text-2xl font-bold">Attendance Certificate</h1>
                            <p className="text-amber-100 text-sm">Generate official attendance certificates for students</p>
                        </div>
                    </div>
                    <button
                        onClick={loadStudents}
                        className="bg-white/20 hover:bg-white/30 p-2 rounded-lg transition-colors"
                    >
                        <RefreshCw className="w-5 h-5" />
                    </button>
                </div>
            </motion.div>

            {/* Controls - no-print */}
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="no-print bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
            >
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                    <Users className="w-5 h-5 text-amber-500" />
                    Select Student
                </h2>

                <div className="flex flex-col md:flex-row gap-4">
                    <div className="flex-1">
                        {isLoadingStudents ? (
                            <div className="flex items-center gap-2 py-3 text-gray-500 text-sm">
                                <LoadingSpinner size="sm" /> Loading students...
                            </div>
                        ) : (
                            <div className="relative">
                                <select
                                    value={selectedStudentId}
                                    onChange={e => {
                                        setSelectedStudentId(e.target.value ? Number(e.target.value) : '');
                                        setShowCertificate(false);
                                    }}
                                    className="w-full px-3 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-amber-500 appearance-none pr-10"
                                >
                                    <option value="">— Select a student —</option>
                                    {students.map(s => (
                                        <option key={s.id} value={s.id}>
                                            {s.firstName} {s.lastName} ({s.ienNumber}) — {s.department}
                                        </option>
                                    ))}
                                </select>
                                <ChevronDown className="absolute right-3 top-3 w-4 h-4 text-gray-400 pointer-events-none" />
                            </div>
                        )}
                    </div>

                    <button
                        onClick={generateCertificate}
                        disabled={isFetching || !selectedStudentId}
                        className="flex items-center justify-center gap-2 px-6 py-2.5 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg font-medium transition-colors"
                    >
                        {isFetching ? <LoadingSpinner size="sm" /> : <Award className="w-4 h-4" />}
                        Generate Certificate
                    </button>

                    {showCertificate && (
                        <button
                            onClick={handlePrint}
                            className="flex items-center justify-center gap-2 px-6 py-2.5 bg-gray-800 dark:bg-gray-600 hover:bg-gray-900 text-white rounded-lg font-medium transition-colors"
                        >
                            <Printer className="w-4 h-4" />
                            Print / Save PDF
                        </button>
                    )}
                </div>

                {selectedStudent && (
                    <motion.div
                        initial={{ opacity: 0, height: 0 }}
                        animate={{ opacity: 1, height: 'auto' }}
                        className="mt-4 p-3 bg-amber-50 dark:bg-amber-900/20 rounded-lg border border-amber-200 dark:border-amber-800 flex flex-wrap gap-4 text-sm"
                    >
                        <span className="text-amber-700 dark:text-amber-300 font-medium">
                            {selectedStudent.firstName} {selectedStudent.lastName}
                        </span>
                        <span className="text-gray-500">IEN: {selectedStudent.ienNumber}</span>
                        <span className="text-gray-500">Dept: {selectedStudent.department}</span>
                        <span className="text-gray-500">Year: {selectedStudent.year}</span>
                        <span className="text-gray-500">Semester: {selectedStudent.semester}</span>
                    </motion.div>
                )}
            </motion.div>

            {/* Certificate Preview */}
            {showCertificate && selectedStudent && (
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="print-area"
                >
                    {/* Certificate Paper */}
                    <div className="bg-white rounded-2xl shadow-2xl border-4 border-amber-400 max-w-3xl mx-auto overflow-hidden">
                        {/* Top decorative bar */}
                        <div className="bg-gradient-to-r from-amber-500 via-orange-500 to-amber-600 h-4" />

                        <div className="p-10">
                            {/* College Header */}
                            <div className="text-center mb-8">
                                <div className="w-20 h-20 mx-auto mb-4 bg-amber-100 rounded-full flex items-center justify-center border-4 border-amber-400">
                                    <Award className="w-10 h-10 text-amber-600" />
                                </div>
                                <h1 className="text-2xl font-bold text-gray-800 uppercase tracking-widest">
                                    New Horizon Institute of Technology &amp; Management
                                </h1>
                                <p className="text-gray-500 text-sm mt-1">Thane, Maharashtra</p>
                            </div>

                            {/* Certificate Title */}
                            <div className="text-center mb-8">
                                <div className="inline-block border-t-2 border-b-2 border-amber-400 py-2 px-8">
                                    <h2 className="text-3xl font-bold text-amber-700 tracking-wide uppercase">
                                        Certificate of Attendance
                                    </h2>
                                </div>
                            </div>

                            {/* Body Text */}
                            <div className="text-center text-gray-700 text-base leading-relaxed space-y-3 mb-8">
                                <p>This is to certify that</p>
                                <p className="text-3xl font-bold text-gray-900">
                                    {selectedStudent.firstName} {selectedStudent.lastName}
                                </p>
                                <p>
                                    <strong>IEN Number:</strong> {selectedStudent.ienNumber} &nbsp;|&nbsp;
                                    <strong>Department:</strong> {selectedStudent.department}
                                </p>
                                <p>
                                    <strong>Year:</strong> {selectedStudent.year} &nbsp;|&nbsp;
                                    <strong>Semester:</strong> {selectedStudent.semester}
                                </p>
                                <p className="mt-4">
                                    has attended{' '}
                                    <span className="text-2xl font-bold text-amber-700">
                                        {attendanceData.presentCount}
                                    </span>{' '}
                                    out of {totalClasses} scheduled classes with an attendance percentage of
                                </p>
                                <p className="text-5xl font-bold text-amber-600">{attendancePercent}%</p>
                            </div>

                            {/* Status badge */}
                            <div className="flex justify-center mb-8">
                                <div className={`flex items-center gap-2 px-6 py-2 rounded-full text-sm font-semibold border-2 ${attendancePercent >= 75
                                        ? 'bg-green-50 text-green-700 border-green-400'
                                        : 'bg-red-50 text-red-700 border-red-400'
                                    }`}>
                                    <CheckCircle className="w-4 h-4" />
                                    Attendance Status: {status}
                                </div>
                            </div>

                            {/* Date + Signatures */}
                            <div className="border-t border-gray-200 pt-6 mt-4">
                                <div className="flex justify-between items-end">
                                    <div className="text-center">
                                        <div className="h-14 border-b border-gray-400 w-40 mb-1" />
                                        <p className="text-xs text-gray-500">Class Coordinator</p>
                                    </div>
                                    <div className="text-center text-sm text-gray-500">
                                        <p>Date of Issue:</p>
                                        <p className="font-semibold text-gray-700">{today}</p>
                                    </div>
                                    <div className="text-center">
                                        <div className="h-14 border-b border-gray-400 w-40 mb-1" />
                                        <p className="text-xs text-gray-500">Head of Department</p>
                                    </div>
                                </div>
                                <p className="text-center text-xs text-gray-400 mt-4">
                                    * This certificate is computer-generated and valid without physical signature.
                                </p>
                            </div>
                        </div>

                        {/* Bottom decorative bar */}
                        <div className="bg-gradient-to-r from-amber-500 via-orange-500 to-amber-600 h-4" />
                    </div>
                </motion.div>
            )}

            {/* Empty state */}
            {!showCertificate && !isFetching && (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.3 }}
                    className="no-print text-center py-16 text-gray-400 dark:text-gray-500"
                >
                    <Award className="w-16 h-16 mx-auto mb-4 opacity-30" />
                    <p className="text-lg font-medium">No certificate generated yet</p>
                    <p className="text-sm mt-1">Select a student above and click "Generate Certificate"</p>
                </motion.div>
            )}
        </div>
    );
};

export default AttendanceCertificate;
