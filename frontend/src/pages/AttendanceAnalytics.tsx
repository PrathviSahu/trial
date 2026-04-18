import React, { useState, useEffect, useMemo } from 'react';
import { motion } from 'framer-motion';
import {
    BarChart3,
    TrendingUp,
    Calendar,
    RefreshCw,
    Users,
} from 'lucide-react';
import {
    BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
    PieChart, Pie, Cell, Legend, LineChart, Line, AreaChart, Area,
} from 'recharts';
import { toast } from 'react-hot-toast';

interface AttendanceRecord {
    id: number;
    student: { id: number; firstName: string; lastName: string; department: string };
    timestamp: string;
    subject: string;
    method: string;
    status: string;
}

const COLORS = ['#6366f1', '#06b6d4', '#f59e0b', '#ef4444', '#10b981', '#8b5cf6', '#ec4899'];
const DAYS = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];

const AttendanceAnalytics: React.FC = () => {
    const [records, setRecords] = useState<AttendanceRecord[]>([]);
    const [students, setStudents] = useState<any[]>([]);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        loadData();
    }, []);

    const loadData = async () => {
        setIsLoading(true);
        try {
            const [attRes, studRes] = await Promise.all([
                fetch('http://localhost:8080/api/attendance'),
                fetch('http://localhost:8080/api/students?size=1000'),
            ]);
            const attData = await attRes.json();
            const studData = await studRes.json();
            if (attData.success) setRecords(attData.data || []);
            if (studData.success) setStudents(studData.data?.content || studData.data || []);
        } catch { toast.error('Failed to load analytics data'); }
        finally { setIsLoading(false); }
    };

    // --- Derived analytics ---

    // Day-of-week distribution
    const dayOfWeekData = useMemo(() => {
        const counts: Record<string, number> = {};
        DAYS.forEach(d => { counts[d] = 0; });
        records.forEach(r => {
            const day = DAYS[new Date(r.timestamp).getDay()];
            counts[day] = (counts[day] || 0) + 1;
        });
        return DAYS.map(d => ({ day: d.slice(0, 3), count: counts[d] }));
    }, [records]);

    // Subject distribution
    const subjectData = useMemo(() => {
        const counts: Record<string, number> = {};
        records.forEach(r => {
            const subj = r.subject || 'General';
            counts[subj] = (counts[subj] || 0) + 1;
        });
        return Object.entries(counts)
            .map(([name, value]) => ({ name, value }))
            .sort((a, b) => b.value - a.value)
            .slice(0, 8);
    }, [records]);

    // Department distribution
    const deptData = useMemo(() => {
        const counts: Record<string, number> = {};
        records.forEach(r => {
            const dept = r.student?.department || 'Unknown';
            counts[dept] = (counts[dept] || 0) + 1;
        });
        return Object.entries(counts).map(([name, value]) => ({ name, value }));
    }, [records]);

    // Method distribution (Face vs QR vs Manual)
    const methodData = useMemo(() => {
        const counts: Record<string, number> = {};
        records.forEach(r => {
            const method = r.method || 'UNKNOWN';
            counts[method] = (counts[method] || 0) + 1;
        });
        return Object.entries(counts).map(([name, value]) => ({ name: name.replace(/_/g, ' '), value }));
    }, [records]);

    // Last 30 days trend
    const trendData = useMemo(() => {
        const last30 = Array.from({ length: 30 }, (_, i) => {
            const d = new Date();
            d.setDate(d.getDate() - (29 - i));
            return d.toISOString().split('T')[0];
        });
        return last30.map(date => {
            const count = records.filter(r =>
                new Date(r.timestamp).toISOString().split('T')[0] === date
            ).length;
            return { date: date.slice(5), count };
        });
    }, [records]);

    // Heatmap data (day vs hour)
    const heatmapData = useMemo(() => {
        const grid: number[][] = Array.from({ length: 7 }, () => Array(24).fill(0));
        records.forEach(r => {
            const d = new Date(r.timestamp);
            grid[d.getDay()][d.getHours()]++;
        });
        // Flatten for display
        const cells: { day: string; hour: number; count: number }[] = [];
        for (let day = 0; day < 7; day++) {
            for (let hour = 6; hour <= 20; hour++) {
                cells.push({ day: DAYS[day].slice(0, 3), hour, count: grid[day][hour] });
            }
        }
        return cells;
    }, [records]);

    const maxHeat = Math.max(...heatmapData.map(c => c.count), 1);

    // Top performers
    const topPerformers = useMemo(() => {
        const counts: Record<string, { name: string; dept: string; count: number }> = {};
        records.forEach(r => {
            const sid = String(r.student?.id);
            if (!counts[sid]) {
                counts[sid] = { name: `${r.student.firstName} ${r.student.lastName}`, dept: r.student.department, count: 0 };
            }
            counts[sid].count++;
        });
        return Object.values(counts).sort((a, b) => b.count - a.count).slice(0, 5);
    }, [records]);

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-500" />
            </div>
        );
    }

    return (
        <div className="space-y-6">
            {/* Header */}
            <motion.div
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-gradient-to-r from-violet-500 to-fuchsia-600 rounded-xl p-6 text-white"
            >
                <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3">
                        <BarChart3 className="w-8 h-8" />
                        <div>
                            <h1 className="text-2xl font-bold">Attendance Analytics</h1>
                            <p className="text-violet-100 text-sm">{records.length} total records • {students.length} students</p>
                        </div>
                    </div>
                    <button onClick={loadData} className="bg-white/20 hover:bg-white/30 p-2 rounded-lg transition-colors">
                        <RefreshCw className="w-5 h-5" />
                    </button>
                </div>
            </motion.div>

            {/* Row 1: 30-day trend + day-of-week */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}
                    className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
                >
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                        <TrendingUp className="w-5 h-5 text-violet-500" /> 30-Day Attendance Trend
                    </h3>
                    <ResponsiveContainer width="100%" height={250}>
                        <AreaChart data={trendData}>
                            <defs>
                                <linearGradient id="trendGrad" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.8} />
                                    <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0.1} />
                                </linearGradient>
                            </defs>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="date" tick={{ fontSize: 10 }} />
                            <YAxis />
                            <Tooltip />
                            <Area type="monotone" dataKey="count" name="Records" stroke="#8b5cf6" fill="url(#trendGrad)" />
                        </AreaChart>
                    </ResponsiveContainer>
                </motion.div>

                <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.2 }}
                    className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
                >
                    <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                        <Calendar className="w-5 h-5 text-cyan-500" /> Day-of-Week Distribution
                    </h3>
                    <ResponsiveContainer width="100%" height={250}>
                        <BarChart data={dayOfWeekData}>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis dataKey="day" />
                            <YAxis />
                            <Tooltip />
                            <Bar dataKey="count" name="Records" fill="#06b6d4" radius={[6, 6, 0, 0]} />
                        </BarChart>
                    </ResponsiveContainer>
                </motion.div>
            </div>

            {/* Row 2: Subject + Department + Method pie charts */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {[
                    { title: 'By Subject', data: subjectData, delay: 0.3 },
                    { title: 'By Department', data: deptData, delay: 0.4 },
                    { title: 'By Method', data: methodData, delay: 0.5 },
                ].map(({ title, data, delay }) => (
                    <motion.div key={title} initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay }}
                        className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
                    >
                        <h3 className="text-sm font-semibold text-gray-900 dark:text-white mb-3">{title}</h3>
                        {data.length === 0 ? (
                            <p className="text-center text-gray-400 py-8 text-sm">No data</p>
                        ) : (
                            <ResponsiveContainer width="100%" height={200}>
                                <PieChart>
                                    <Pie data={data} cx="50%" cy="50%" innerRadius={40} outerRadius={70} paddingAngle={2} dataKey="value" label={(props: any) => `${props.name} ${(props.percent * 100).toFixed(0)}%`}>
                                        {data.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                                    </Pie>
                                    <Tooltip />
                                </PieChart>
                            </ResponsiveContainer>
                        )}
                    </motion.div>
                ))}
            </div>

            {/* Row 3: Heatmap */}
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.6 }}
                className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
            >
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4">Attendance Heatmap (Day × Hour)</h3>
                <div className="overflow-x-auto">
                    <div className="inline-grid gap-1" style={{ gridTemplateColumns: `60px repeat(15, 1fr)` }}>
                        {/* Header row */}
                        <div />
                        {Array.from({ length: 15 }, (_, i) => i + 6).map(h => (
                            <div key={h} className="text-xs text-center text-gray-400 pb-1">{h}:00</div>
                        ))}
                        {/* Data rows */}
                        {DAYS.map(day => (
                            <React.Fragment key={day}>
                                <div className="text-xs text-gray-500 dark:text-gray-400 flex items-center">{day.slice(0, 3)}</div>
                                {Array.from({ length: 15 }, (_, i) => i + 6).map(hour => {
                                    const cell = heatmapData.find(c => c.day === day.slice(0, 3) && c.hour === hour);
                                    const intensity = cell ? cell.count / maxHeat : 0;
                                    return (
                                        <div
                                            key={`${day}-${hour}`}
                                            title={`${day} ${hour}:00 — ${cell?.count || 0} records`}
                                            className="w-8 h-8 rounded cursor-pointer transition-colors"
                                            style={{
                                                backgroundColor: intensity > 0
                                                    ? `rgba(139, 92, 246, ${0.15 + intensity * 0.85})`
                                                    : 'rgba(0,0,0,0.05)',
                                            }}
                                        />
                                    );
                                })}
                            </React.Fragment>
                        ))}
                    </div>
                    <div className="flex items-center gap-2 mt-3 text-xs text-gray-400">
                        <span>Less</span>
                        {[0.1, 0.3, 0.5, 0.7, 1].map(v => (
                            <div key={v} className="w-4 h-4 rounded" style={{ backgroundColor: `rgba(139, 92, 246, ${0.15 + v * 0.85})` }} />
                        ))}
                        <span>More</span>
                    </div>
                </div>
            </motion.div>

            {/* Row 4: Top performers */}
            <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.7 }}
                className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg border border-gray-200 dark:border-gray-700"
            >
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white mb-4 flex items-center gap-2">
                    <Users className="w-5 h-5 text-green-500" /> Top 5 Most Present Students
                </h3>
                <div className="space-y-3">
                    {topPerformers.map((s, i) => (
                        <div key={i} className="flex items-center gap-4">
                            <div className={`w-8 h-8 rounded-full flex items-center justify-center text-white font-bold text-sm ${i === 0 ? 'bg-yellow-500' : i === 1 ? 'bg-gray-400' : i === 2 ? 'bg-amber-600' : 'bg-gray-300'
                                }`}>
                                {i + 1}
                            </div>
                            <div className="flex-1">
                                <p className="font-medium text-gray-900 dark:text-white">{s.name}</p>
                                <p className="text-xs text-gray-500">{s.dept}</p>
                            </div>
                            <div className="text-right">
                                <p className="font-bold text-indigo-600">{s.count}</p>
                                <p className="text-xs text-gray-400">records</p>
                            </div>
                            <div className="w-32 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                                <div
                                    className="h-2 rounded-full bg-gradient-to-r from-indigo-500 to-purple-500"
                                    style={{ width: `${topPerformers[0]?.count ? (s.count / topPerformers[0].count) * 100 : 0}%` }}
                                />
                            </div>
                        </div>
                    ))}
                    {topPerformers.length === 0 && (
                        <p className="text-center text-gray-400 py-4">No attendance data yet</p>
                    )}
                </div>
            </motion.div>
        </div>
    );
};

export default AttendanceAnalytics;
