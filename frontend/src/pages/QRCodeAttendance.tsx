import React, { useState, useEffect, useRef, useCallback } from 'react';
import { motion } from 'framer-motion';
import { QRCodeSVG } from 'qrcode.react';
import { Scanner } from '@yudiel/react-qr-scanner';
import {
    QrCode,
    Plus,
    Clock,
    Users,
    CheckCircle,
    RefreshCw,
    ShieldCheck,
    Timer,
    Camera
} from 'lucide-react';
import { toast } from 'react-hot-toast';

interface QRSession {
    sessionCode: string;
    subject: string;
    department: string;
    faculty: string;
    expiresAt: string;
    markedCount?: number;
}

interface Student {
    id: number;
    firstName: string;
    lastName: string;
    ienNumber: string;
    department: string;
}

const ROTATE_INTERVAL = 30; // seconds

const QRCodeAttendance: React.FC = () => {
    const [sessions, setSessions] = useState<QRSession[]>([]);
    const [subject, setSubject] = useState('');
    const [department, setDepartment] = useState('');
    const [faculty, setFaculty] = useState('');
    const [validMinutes, setValidMinutes] = useState(15);
    const [isGenerating, setIsGenerating] = useState(false);
    const [activeQR, setActiveQR] = useState<string | null>(null);
    const [students, setStudents] = useState<Student[]>([]);
    const [selectedStudentId, setSelectedStudentId] = useState<number | ''>('');
    const [scanCode, setScanCode] = useState('');
    const [isMarking, setIsMarking] = useState(false);
    const [tab, setTab] = useState<'generate' | 'scan'>('generate');
    const [countdown, setCountdown] = useState(ROTATE_INTERVAL);
    const [isRotating, setIsRotating] = useState(false);
    const [rotationCount, setRotationCount] = useState(0);
    const countdownRef = useRef<NodeJS.Timeout | null>(null);
    const [isCameraActive, setIsCameraActive] = useState(false);
    const lastScannedCodeRef = useRef<string | null>(null);

    useEffect(() => {
        loadSessions();
        loadStudents();
        const interval = setInterval(loadSessions, 30000);
        return () => clearInterval(interval);
    }, []);

    // Auto-rotate countdown timer
    useEffect(() => {
        if (!activeQR) {
            if (countdownRef.current) clearInterval(countdownRef.current);
            return;
        }

        setCountdown(ROTATE_INTERVAL);

        countdownRef.current = setInterval(() => {
            setCountdown(prev => {
                if (prev <= 1) {
                    rotateQR();
                    return ROTATE_INTERVAL;
                }
                return prev - 1;
            });
        }, 1000);

        return () => {
            if (countdownRef.current) clearInterval(countdownRef.current);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [activeQR, rotationCount]);

    const rotateQR = useCallback(async () => {
        if (!activeQR || isRotating) return;
        setIsRotating(true);
        try {
            const res = await fetch('http://localhost:8080/api/qr-attendance/session/rotate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sessionCode: activeQR }),
            });
            const data = await res.json();
            if (data.success) {
                setActiveQR(data.sessionCode);
                setRotationCount(prev => prev + 1);
                loadSessions();
            }
        } catch { /* silent */ }
        finally { setIsRotating(false); }
    }, [activeQR, isRotating]);

    const loadSessions = async () => {
        try {
            const res = await fetch('http://localhost:8080/api/qr-attendance/sessions');
            const data = await res.json();
            if (data.success) setSessions(data.sessions || []);
        } catch { }
    };

    const loadStudents = async () => {
        try {
            const res = await fetch('http://localhost:8080/api/students?size=1000');
            const data = await res.json();
            if (data.success) setStudents(data.data?.content || data.data || []);
        } catch { }
    };

    const generateSession = async () => {
        if (!subject.trim()) { toast.error('Subject is required'); return; }
        setIsGenerating(true);
        try {
            const res = await fetch('http://localhost:8080/api/qr-attendance/session/generate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ subject, department, faculty, validMinutes }),
            });
            const data = await res.json();
            if (data.success) {
                toast.success(`QR session started — code rotates every ${ROTATE_INTERVAL}s`);
                setActiveQR(data.sessionCode);
                setCountdown(ROTATE_INTERVAL);
                setRotationCount(0);
                setSubject('');
                loadSessions();
            } else {
                toast.error(data.message || 'Failed');
            }
        } catch { toast.error('Server error'); }
        finally { setIsGenerating(false); }
    };

    const markViaQR = async (scannedCodeOverride?: string) => {
        const codeToUse = scannedCodeOverride || scanCode;
        if (!codeToUse.trim() || !selectedStudentId) {
            toast.error('Enter session code and select student');
            return;
        }
        setIsMarking(true);
        try {
            const res = await fetch('http://localhost:8080/api/qr-attendance/mark', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sessionCode: codeToUse.trim().toUpperCase(), studentId: selectedStudentId }),
            });
            const data = await res.json();
            if (data.success) {
                toast.success(data.alreadyMarked ? 'Already marked!' : `✅ ${data.studentName} — attendance marked`);
                setScanCode('');
                if (!scannedCodeOverride) {
                    setSelectedStudentId('');
                }
                loadSessions();
            } else {
                toast.error(data.message || 'Failed');
            }
        } catch { toast.error('Server error'); }
        finally { 
            setIsMarking(false); 
            // Reset throttle after 2 seconds
            setTimeout(() => {
                lastScannedCodeRef.current = null;
            }, 2000);
        }
    };

    const handleScan = (detectedCodes: { rawValue: string }[]) => {
        if (!detectedCodes || detectedCodes.length === 0) return;
        const rawValue = detectedCodes[0].rawValue;
        if (rawValue === lastScannedCodeRef.current) return; // prevent spamming
        lastScannedCodeRef.current = rawValue;

        if (rawValue.startsWith('FACETRACK_QR:')) {
            const extractedCode = rawValue.split(':')[1];
            setScanCode(extractedCode);
            if (!selectedStudentId) {
                toast.error('QR Scanned! Please select your student name to submit.');
            } else {
                markViaQR(extractedCode);
            }
        } else {
            toast.error('Invalid QR Code format.');
        }
    };

    const stopSession = () => {
        setActiveQR(null);
        setCountdown(ROTATE_INTERVAL);
        setRotationCount(0);
        toast.success('QR session stopped');
    };

    const timeLeft = (expiresAt: string) => {
        const diff = new Date(expiresAt).getTime() - Date.now();
        if (diff <= 0) return 'Expired';
        const m = Math.floor(diff / 60000);
        const s = Math.floor((diff % 60000) / 1000);
        return `${m}m ${s}s`;
    };

    // Circular progress for countdown
    const countdownPercent = (countdown / ROTATE_INTERVAL) * 100;
    const circumference = 2 * Math.PI * 40;
    const strokeDashoffset = circumference - (countdownPercent / 100) * circumference;

    return (
        <div className="space-y-6">
            {/* Header */}
            <motion.div
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-gradient-to-r from-cyan-500 to-blue-600 rounded-xl p-6 text-white"
            >
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <QrCode className="w-8 h-8" />
                        <div>
                            <h1 className="text-2xl font-bold">QR Code Attendance</h1>
                            <p className="text-cyan-100 text-sm flex items-center gap-1">
                                <ShieldCheck className="w-4 h-4" />
                                Anti-fraud: Code auto-rotates every {ROTATE_INTERVAL} seconds
                            </p>
                        </div>
                    </div>
                    <button onClick={loadSessions} className="bg-white/20 hover:bg-white/30 p-2 rounded-lg transition-colors">
                        <RefreshCw className="w-5 h-5" />
                    </button>
                </div>
            </motion.div>

            {/* Tabs */}
            <div className="flex gap-2">
                {(['generate', 'scan'] as const).map(t => (
                    <button
                        key={t}
                        onClick={() => setTab(t)}
                        className={`px-5 py-2.5 rounded-lg font-medium text-sm transition-colors ${tab === t
                            ? 'bg-cyan-500 text-white shadow-lg'
                            : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-gray-700'
                            }`}
                    >
                        {t === 'generate' ? '🔐 Generate QR (Teacher)' : '📷 Scan QR (Student)'}
                    </button>
                ))}
            </div>

            {tab === 'generate' && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
                    {/* Generate Form */}
                    {!activeQR && (
                        <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700">
                            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                                <Plus className="w-5 h-5 text-cyan-500" /> New QR Session
                            </h2>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Subject *</label>
                                    <input
                                        value={subject}
                                        onChange={e => setSubject(e.target.value)}
                                        placeholder="e.g. Data Structures"
                                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-cyan-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Department</label>
                                    <input
                                        value={department}
                                        onChange={e => setDepartment(e.target.value)}
                                        placeholder="e.g. CSD"
                                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-cyan-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Faculty</label>
                                    <input
                                        value={faculty}
                                        onChange={e => setFaculty(e.target.value)}
                                        placeholder="e.g. Prof. Sharma"
                                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-cyan-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">Valid for (minutes)</label>
                                    <input
                                        type="number"
                                        value={validMinutes}
                                        onChange={e => setValidMinutes(Number(e.target.value))}
                                        min={1}
                                        max={120}
                                        className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-cyan-500"
                                    />
                                </div>
                            </div>
                            <button
                                onClick={generateSession}
                                disabled={isGenerating}
                                className="mt-4 flex items-center gap-2 px-6 py-2.5 bg-cyan-500 hover:bg-cyan-600 disabled:bg-gray-300 text-white rounded-lg font-medium transition-colors"
                            >
                                <QrCode className="w-4 h-4" /> {isGenerating ? 'Starting...' : 'Start Secure QR Session'}
                            </button>
                        </div>
                    )}

                    {/* Active QR Display with Countdown */}
                    {activeQR && (
                        <motion.div
                            initial={{ opacity: 0, scale: 0.9 }}
                            animate={{ opacity: 1, scale: 1 }}
                            className="bg-white dark:bg-gray-800 rounded-xl p-8 shadow-lg border-2 border-cyan-400"
                        >
                            {/* Anti-fraud banner */}
                            <div className="flex items-center justify-center gap-2 mb-6 px-4 py-2 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-lg">
                                <ShieldCheck className="w-5 h-5 text-green-600" />
                                <span className="text-sm font-medium text-green-700 dark:text-green-300">
                                    🔒 Anti-Fraud Mode Active — Code rotates every {ROTATE_INTERVAL}s • Rotation #{rotationCount + 1}
                                </span>
                            </div>

                            <div className="flex flex-col md:flex-row items-center justify-center gap-12">
                                {/* QR Code */}
                                <div className="text-center">
                                    <motion.div
                                        key={activeQR}
                                        initial={{ opacity: 0, scale: 0.8, rotateY: 90 }}
                                        animate={{ opacity: 1, scale: 1, rotateY: 0 }}
                                        transition={{ duration: 0.3 }}
                                        className="inline-block p-4 bg-white rounded-xl shadow-md border-4 border-gray-100"
                                    >
                                        <QRCodeSVG value={`FACETRACK_QR:${activeQR}`} size={260} />
                                    </motion.div>
                                    <motion.div
                                        key={`code-${activeQR}`}
                                        initial={{ opacity: 0, y: 10 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        className="mt-4"
                                    >
                                        <span className="text-4xl font-mono font-bold tracking-[0.3em] text-cyan-600">{activeQR}</span>
                                    </motion.div>
                                </div>

                                {/* Countdown Ring */}
                                <div className="flex flex-col items-center gap-4">
                                    <div className="relative w-36 h-36">
                                        <svg className="w-36 h-36 -rotate-90" viewBox="0 0 100 100">
                                            <circle cx="50" cy="50" r="40" fill="none" stroke="#e5e7eb" strokeWidth="6" />
                                            <circle
                                                cx="50"
                                                cy="50"
                                                r="40"
                                                fill="none"
                                                stroke={countdown <= 5 ? '#ef4444' : countdown <= 10 ? '#f59e0b' : '#06b6d4'}
                                                strokeWidth="6"
                                                strokeLinecap="round"
                                                strokeDasharray={circumference}
                                                strokeDashoffset={strokeDashoffset}
                                                style={{ transition: 'stroke-dashoffset 1s linear, stroke 0.5s' }}
                                            />
                                        </svg>
                                        <div className="absolute inset-0 flex flex-col items-center justify-center">
                                            <span className={`text-3xl font-bold ${countdown <= 5 ? 'text-red-500' : countdown <= 10 ? 'text-amber-500' : 'text-cyan-600'}`}>
                                                {countdown}s
                                            </span>
                                            <span className="text-[10px] text-gray-400 uppercase tracking-wider">rotate</span>
                                        </div>
                                    </div>
                                    <div className="text-center space-y-1">
                                        <p className="text-sm font-medium text-gray-600 dark:text-gray-300 flex items-center gap-1 justify-center">
                                            <Timer className="w-4 h-4" />
                                            New code in {countdown}s
                                        </p>
                                        <p className="text-xs text-gray-400 max-w-[150px]">
                                            {isRotating ? '⟳ Rotating...' : 'Students must scan code before it changes'}
                                        </p>
                                    </div>
                                </div>
                            </div>

                            {/* Stop button */}
                            <div className="mt-8 flex justify-center border-t border-gray-100 dark:border-gray-700 pt-6">
                                <button
                                    onClick={stopSession}
                                    className="px-6 py-2.5 bg-red-500 hover:bg-red-600 text-white rounded-lg text-sm font-medium transition-colors"
                                >
                                    ⏹ Stop Session
                                </button>
                            </div>
                        </motion.div>
                    )}

                    {/* Active Sessions */}
                    <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700">
                        <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                            <Clock className="w-5 h-5 text-cyan-500" /> Active Sessions ({sessions.length})
                        </h2>
                        {sessions.length === 0 ? (
                            <p className="text-center text-gray-400 py-8">No active sessions stored in database.</p>
                        ) : (
                            <div className="space-y-3">
                                {sessions.map(s => (
                                    <div
                                        key={s.sessionCode}
                                        className="flex items-center justify-between p-4 bg-cyan-50 dark:bg-cyan-900/20 border border-cyan-200 dark:border-cyan-800 rounded-lg"
                                    >
                                        <div>
                                            <p className="font-semibold text-gray-900 dark:text-white">{s.subject}</p>
                                            <p className="text-sm text-gray-500">
                                                {s.department} • {s.faculty} • Code: <span className="font-mono font-bold text-cyan-600">{s.sessionCode}</span>
                                            </p>
                                        </div>
                                        <div className="text-right">
                                            <div className="flex items-center gap-1 text-sm text-green-600 justify-end font-medium">
                                                <Users className="w-4 h-4" /> {s.markedCount || 0} marked
                                            </div>
                                            <p className="text-xs text-gray-400 mt-1">{timeLeft(s.expiresAt)}</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </motion.div>
            )}

            {tab === 'scan' && (
                <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col md:flex-row gap-6 items-start">
                    
                    {/* Left Panel: Camera Scanner */}
                    <div className="w-full md:w-1/2 bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700 flex flex-col h-full">
                        <div className="flex items-center justify-between mb-4">
                            <h2 className="text-lg font-semibold text-gray-900 dark:text-white flex items-center gap-2">
                                <Camera className="w-5 h-5 text-cyan-500" /> Live QR Scanner
                            </h2>
                            <button 
                                onClick={() => setIsCameraActive(!isCameraActive)}
                                className={`px-4 py-1.5 rounded-full text-sm font-medium ${isCameraActive ? 'bg-red-100 text-red-600' : 'bg-green-100 text-green-600'}`}
                            >
                                {isCameraActive ? 'Off' : 'On'}
                            </button>
                        </div>

                        {/* Scanner Component Area */}
                        <div className="flex-1 min-h-[300px] w-full bg-gray-900 rounded-lg overflow-hidden relative flex items-center justify-center">
                            {!isCameraActive ? (
                                <div className="text-center p-6 text-gray-400">
                                    <Camera className="w-12 h-12 mx-auto mb-3 opacity-50" />
                                    <p>Camera is paused.</p>
                                    <button onClick={() => setIsCameraActive(true)} className="mt-3 px-4 py-2 bg-cyan-600 hover:bg-cyan-500 text-white rounded-lg text-sm transition-colors">Start Camera</button>
                                </div>
                            ) : (
                                <div className="w-full h-full relative">
                                    <Scanner 
                                        onScan={handleScan}
                                        onError={(e) => console.log('Scanner error:', e)}
                                        components={{
                                            torch: true,
                                            zoom: false
                                        }}
                                    />
                                    <div className="absolute inset-0 pointer-events-none border-[40px] border-black/40 rounded-lg"></div>
                                    <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-48 h-48 border-2 border-cyan-400 pointer-events-none rounded-xl"></div>
                                </div>
                            )}
                        </div>
                        <p className="text-xs text-gray-500 text-center mt-3">
                            Hold the Teacher's rotating QR code steady within the frame.
                        </p>
                    </div>

                    {/* Right Panel: Manual Code Entry & Student Selection */}
                    <div className="w-full md:w-1/2 flex flex-col gap-6">
                        {/* Status / Instructions */}
                        <div className="bg-gradient-to-br from-cyan-50 to-blue-50 dark:from-cyan-900/20 dark:to-blue-900/20 border border-cyan-100 dark:border-cyan-800 rounded-xl p-5 text-sm text-cyan-800 dark:text-cyan-200">
                            <h3 className="font-semibold text-cyan-900 dark:text-cyan-100 mb-2">How to Mark Attendance:</h3>
                            <ol className="list-decimal pl-4 space-y-1.5 marker:text-cyan-600">
                                <li>Select your name from the Student dropdown below.</li>
                                <li>Turn <strong>ON</strong> the Camera and point it at the teacher's screen.</li>
                                <li>Once scanned, your attendance is <strong>auto-submitted!</strong></li>
                            </ol>
                            <p className="mt-3 italic opacity-80 text-xs">Or you can manually type the 8-character code below.</p>
                        </div>

                        <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700">
                            <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                                <CheckCircle className="w-5 h-5 text-green-500" /> Attendance Details
                            </h2>
                            
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                        Student Profile <span className="text-red-500">*</span>
                                    </label>
                                    <select
                                        value={selectedStudentId}
                                        onChange={e => setSelectedStudentId(e.target.value ? Number(e.target.value) : '')}
                                        className="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-green-500"
                                    >
                                        <option value="">— Select your profile —</option>
                                        {students.map(s => (
                                            <option key={s.id} value={s.id}>{s.firstName} {s.lastName} ({s.ienNumber})</option>
                                        ))}
                                    </select>
                                    {!selectedStudentId && isCameraActive && (
                                        <p className="text-red-500 text-xs mt-1 font-medium">⚠️ Required before scanning.</p>
                                    )}
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                        Manual Session Code
                                    </label>
                                    <input
                                        value={scanCode}
                                        onChange={e => setScanCode(e.target.value.toUpperCase())}
                                        placeholder="e.g. A1B2C3D4"
                                        maxLength={8}
                                        className="w-full px-4 py-2.5 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white font-mono text-lg tracking-widest focus:ring-2 focus:ring-green-500"
                                    />
                                </div>

                                <button
                                    onClick={() => markViaQR()}
                                    disabled={isMarking || !scanCode || !selectedStudentId}
                                    className="w-full mt-2 flex items-center justify-center gap-2 px-6 py-3 bg-green-500 hover:bg-green-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg font-medium transition-colors"
                                >
                                    <CheckCircle className="w-5 h-5" /> {isMarking ? 'Marking...' : 'Manual Mark Present'}
                                </button>
                            </div>
                        </div>
                    </div>
                </motion.div>
            )}
        </div>
    );
};

export default QRCodeAttendance;
