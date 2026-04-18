import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus, Edit, Trash2, Save, AlertTriangle,
  MapPin, User, X, Settings, Eye, Printer, Grid,
} from 'lucide-react';
import { toast } from 'react-hot-toast';
import { DEPARTMENTS, Department, CLASS_TYPE, ClassType } from '../config/api';
import { apiService } from '../services/api';
import LoadingSpinner from '../components/LoadingSpinner';
import TimetableAttendanceModal from '../components/TimetableAttendanceModal';

// ─── Interfaces ──────────────────────────────────────────────────────────────
interface TimeSlot {
  id: string;
  startTime: string;
  endTime: string;
  subject?: string;
  subjectAbbrev?: string;
  faculty?: string;
  facultyAbbrev?: string;
  classroom?: string;
  type?: ClassType;
  department: Department;
  year: number;
  semester: number;
  section?: string;
  batch?: string;
  spanAllPeriods?: boolean;
}
interface TimetableEntry { id: string; day: string; timeSlots: TimeSlot[]; }
interface InstituteSettings {
  name: string; location: string; programName: string;
  yearOfProcess: string; wefDate: string; verNo: string; issueNo: string;
  preparedBy: string; preparedByTitle: string;
  verifiedBy: string; verifiedByTitle: string;
  proposedBy: string; proposedByTitle: string;
  approvedBy: string; approvedByTitle: string;
}
interface BatchConfig { id: string; name: string; rollFrom: number; rollTo: number; }
interface NomenclatureItem { id: string; abbrev: string; fullName: string; }
interface FacultyDetail { id: string; abbrev: string; fullName: string; }

// ─── Constants ────────────────────────────────────────────────────────────────
const DAYS = ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'];
const TIME_PERIODS = [
  { id: 1, start: '09:15', end: '10:15', display: '9:15-10:15' },
  { id: 2, start: '10:15', end: '11:15', display: '10:15-11:15' },
  { id: 3, start: '11:30', end: '12:30', display: '11:30-12:30' },
  { id: 4, start: '12:30', end: '13:30', display: '12:30-1:30' },
  { id: 5, start: '14:00', end: '15:00', display: '2:00-3:00' },
  { id: 6, start: '15:00', end: '16:00', display: '3:00-4:00' },
  { id: 7, start: '16:00', end: '17:00', display: '4:00-5:00' },
];
const DEFAULT_INSTITUTE: InstituteSettings = {
  name: 'NEW HORIZON INSTITUTE OF TECHNOLOGY & MANAGEMENT',
  location: 'THANE',
  programName: 'Computer Science & Design, Sem VIII, 2025-26',
  yearOfProcess: 'Fourth Year Engineering',
  wefDate: '05 January 2026',
  verNo: '001', issueNo: '01',
  preparedBy: '', preparedByTitle: 'Class In-charge',
  verifiedBy: '', verifiedByTitle: 'Head of Department',
  proposedBy: '', proposedByTitle: 'Vice-Principal & Dean Academics',
  approvedBy: '', approvedByTitle: 'Principal',
};
const DEFAULT_BATCHES: BatchConfig[] = [
  { id: 'B1', name: 'Batch B1', rollFrom: 1, rollTo: 21 },
  { id: 'B2', name: 'Batch B2', rollFrom: 22, rollTo: 42 },
  { id: 'B3', name: 'Batch B3', rollFrom: 43, rollTo: 63 },
];
const DEFAULT_NOMENCLATURE: NomenclatureItem[] = [
  { id: '1', abbrev: 'DC', fullName: 'Distributed Computing' },
  { id: '2', abbrev: 'ADS', fullName: 'Dept. Optional Course 5: Applied Data Science' },
  { id: '3', abbrev: 'SMA', fullName: 'Dept. Optional Course 6: Social Media Analytics' },
  { id: '4', abbrev: 'PM', fullName: 'Dept. Optional Course: Project Management' },
  { id: '5', abbrev: 'DBM', fullName: 'Dept. Optional Course: Digital Business Management' },
  { id: '6', abbrev: 'EM', fullName: 'Dept. Optional Course: Environmental Management' },
  { id: '7', abbrev: 'MAJOR PROJECT 1', fullName: 'Major Project Phase 1' },
];
const DEFAULT_FACULTY: FacultyDetail[] = [
  { id: '1', abbrev: 'IPJ', fullName: 'Ms. Indira Joshi' },
  { id: '2', abbrev: 'PAY', fullName: 'Ms. Prajakta Yadav' },
  { id: '3', abbrev: 'ASD', fullName: 'Ms. Avanti Dhakane' },
  { id: '4', abbrev: 'SLB', fullName: 'Ms. Swati Bhangale' },
  { id: '5', abbrev: 'PPS', fullName: 'Dr. Prasenkumar Saklecha' },
  { id: '6', abbrev: 'PDD', fullName: 'Mr. Pratap Deshmukh' },
  { id: '7', abbrev: 'SSP', fullName: 'Dr. Sunanda Pandita' },
];

const SUBJECTS_BY_DEPT: Record<string, string[]> = {
  CE: ['Data Structures', 'Computer Networks', 'Database Systems', 'Software Engineering', 'Operating Systems'],
  CSD: ['Distributed Computing', 'Applied Data Science', 'Social Media Analytics', 'Project Management', 'Digital Business Management', 'Environmental Management', 'Major Project 1'],
  AIDS: ['Artificial Intelligence', 'Data Mining', 'Big Data Analytics', 'IoT', 'Robotics'],
  MECHATRONICS: ['Control Systems', 'Robotics', 'Automation', 'Sensors', 'Actuators'],
  CIVIL: ['Structural Engineering', 'Geotechnical', 'Transportation', 'Environmental', 'Construction'],
  IT: ['Web Development', 'Mobile Computing', 'Cloud Computing', 'Cybersecurity', 'DevOps'],
};
const CLASSROOMS = ['SF101', 'SF102', 'SF103', 'SF104', 'SF211', 'SF212', 'SF213', 'SF214', 'FF101', 'FF102', 'FF103', 'FF104', 'Lab 1', 'Lab 2', 'Lab 3', 'Lab 4', 'Lab 5'];

const DEPT_FULL_NAMES: Record<string, string> = {
  CE: 'Computer Engineering',
  CSD: 'Computer Science & Design',
  AIDS: 'Artificial Intelligence & Data Science',
  MECHATRONICS: 'Mechatronics Engineering',
  CIVIL: 'Civil Engineering',
  IT: 'Information Technology',
};
const YEAR_SUFFIX = ['', 'First', 'Second', 'Third', 'Fourth'];

// ─── Component ────────────────────────────────────────────────────────────────
const TimetableBuilder: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'builder' | 'view' | 'settings'>('builder');
  const [selectedDepartment, setSelectedDepartment] = useState<Department>('CSD');
  const [selectedYear, setSelectedYear] = useState(4);
  const [selectedSemester, setSelectedSemester] = useState(8);
  const [selectedSection, setSelectedSection] = useState('A');
  const [timetable, setTimetable] = useState<TimetableEntry[]>([]);
  const [showAddModal, setShowAddModal] = useState(false);
  const [editingSlot, setEditingSlot] = useState<TimeSlot | null>(null);
  const [selectedDay, setSelectedDay] = useState('');
  const [selectedPeriodId, setSelectedPeriodId] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [conflicts, setConflicts] = useState<string[]>([]);
  const [instituteSettings, setInstituteSettings] = useState<InstituteSettings>(DEFAULT_INSTITUTE);
  const [batches, setBatches] = useState<BatchConfig[]>(DEFAULT_BATCHES);
  const [nomenclature, setNomenclature] = useState<NomenclatureItem[]>(DEFAULT_NOMENCLATURE);
  const [facultyDetails, setFacultyDetails] = useState<FacultyDetail[]>(DEFAULT_FACULTY);
  const [formData, setFormData] = useState({
    subject: '', subjectAbbrev: '', faculty: '', facultyAbbrev: '',
    classroom: '', type: 'LECTURE' as ClassType, batch: 'ALL', spanAllPeriods: false,
  });
  const [attendanceSlot, setAttendanceSlot] = useState<TimeSlot | null>(null);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => { loadTimetable(); }, [selectedDepartment, selectedYear, selectedSemester, selectedSection]);

  const loadTimetable = async () => {
    setIsLoading(true);
    try {
      const response = await apiService.get<{ success: boolean; data: any[] }>(
        `/timetable/schedule/weekly?department=${encodeURIComponent(selectedDepartment)}&year=${selectedYear}&semester=${selectedSemester}&section=${encodeURIComponent(selectedSection)}`
      );
      const slots = (response as any)?.data || [];
      const byDay = new Map<string, TimeSlot[]>();
      for (const day of DAYS) byDay.set(day, []);
      for (const slot of slots) {
        const normalizedDay = DAYS.find(d => d.toLowerCase() === String(slot.dayOfWeek || '').toLowerCase());
        if (!normalizedDay) continue;
        byDay.get(normalizedDay)?.push({
          id: String(slot.id),
          startTime: String(slot.startTime || '').slice(0, 5),
          endTime: String(slot.endTime || '').slice(0, 5),
          subject: `${slot.subjectCode} - ${slot.subjectName}`,
          subjectAbbrev: slot.subjectCode || '',
          faculty: slot.faculty || '',
          facultyAbbrev: slot.facultyAbbrev || '',
          classroom: slot.classroom || '',
          type: (slot.type as ClassType) || 'LECTURE',
          department: selectedDepartment, year: selectedYear,
          semester: selectedSemester, section: selectedSection,
          batch: slot.batch || 'ALL',
          spanAllPeriods: slot.spanAllPeriods || false,
        });
      }
      const next: TimetableEntry[] = DAYS.map(day => ({
        id: day, day,
        timeSlots: (byDay.get(day) || []).sort((a, b) => a.startTime.localeCompare(b.startTime)),
      }));
      setTimetable(next);
      checkConflicts(next);
    } catch { toast.error('Failed to load timetable'); }
    finally { setIsLoading(false); }
  };

  const checkConflicts = (tt: TimetableEntry[]) => {
    const list: string[] = [];
    const usedFaculty = new Map<string, boolean>();
    const usedRoom = new Map<string, boolean>();
    tt.forEach(day => day.timeSlots.forEach(slot => {
      const fk = `${day.day}-${slot.startTime}-${slot.faculty}`;
      if (slot.faculty && usedFaculty.has(fk)) list.push(`Faculty conflict: ${slot.faculty} on ${day.day} at ${slot.startTime}`);
      if (slot.faculty) usedFaculty.set(fk, true);
      const rk = `${day.day}-${slot.startTime}-${slot.classroom}`;
      if (slot.classroom && usedRoom.has(rk)) list.push(`Room conflict: ${slot.classroom} on ${day.day} at ${slot.startTime}`);
      if (slot.classroom) usedRoom.set(rk, true);
    }));
    setConflicts(list);
  };

  const resetForm = () => {
    setFormData({ subject: '', subjectAbbrev: '', faculty: '', facultyAbbrev: '', classroom: '', type: 'LECTURE', batch: 'ALL', spanAllPeriods: false });
    setSelectedDay(''); setSelectedPeriodId(''); setEditingSlot(null);
  };

  const addTimeSlot = () => {
    if (!selectedDay || !selectedPeriodId) { toast.error('Please select day and period'); return; }
    const period = TIME_PERIODS.find(p => p.id === parseInt(selectedPeriodId));
    if (!period) return;
    const newSlot: TimeSlot = {
      id: Date.now().toString(), startTime: period.start, endTime: period.end,
      subject: formData.subject, subjectAbbrev: formData.subjectAbbrev,
      faculty: formData.faculty, facultyAbbrev: formData.facultyAbbrev,
      classroom: formData.classroom, type: formData.type,
      department: selectedDepartment, year: selectedYear, semester: selectedSemester, section: selectedSection,
      batch: formData.batch, spanAllPeriods: formData.spanAllPeriods,
    };
    const updated = timetable.map(day => {
      if (day.day !== selectedDay) return day;
      const exists = day.timeSlots.find(s => s.startTime === period.start && s.batch === formData.batch);
      if (exists) { toast.error('Slot already exists for this batch'); return day; }
      return { ...day, timeSlots: [...day.timeSlots, newSlot].sort((a, b) => a.startTime.localeCompare(b.startTime)) };
    });
    setTimetable(updated); checkConflicts(updated);
    setShowAddModal(false); resetForm(); toast.success('Slot added!');
  };

  const editTimeSlot = (slot: TimeSlot) => {
    setEditingSlot(slot);
    setFormData({ subject: slot.subject || '', subjectAbbrev: slot.subjectAbbrev || '', faculty: slot.faculty || '', facultyAbbrev: slot.facultyAbbrev || '', classroom: slot.classroom || '', type: slot.type || 'LECTURE', batch: slot.batch || 'ALL', spanAllPeriods: slot.spanAllPeriods || false });
    const dayEntry = timetable.find(d => d.timeSlots.some(s => s.id === slot.id));
    setSelectedDay(dayEntry?.day || '');
    const period = TIME_PERIODS.find(p => p.start === slot.startTime);
    setSelectedPeriodId(period?.id.toString() || '');
    setShowAddModal(true);
  };

  const updateTimeSlot = () => {
    if (!editingSlot) return;
    const updated = timetable.map(day => ({ ...day, timeSlots: day.timeSlots.map(s => s.id === editingSlot.id ? { ...s, ...formData } : s) }));
    setTimetable(updated); checkConflicts(updated);
    setShowAddModal(false); setEditingSlot(null); resetForm();
    toast.success('Slot updated!'); saveTimetable();
  };

  const deleteTimeSlot = (slotId: string) => {
    const numId = Number(slotId);
    if (!isNaN(numId) && numId > 0) apiService.delete(`/timetable/${numId}`).catch(() => { });
    const updated = timetable.map(day => ({ ...day, timeSlots: day.timeSlots.filter(s => s.id !== slotId) }));
    setTimetable(updated); checkConflicts(updated); toast.success('Slot deleted!');
  };

  const saveTimetable = async () => {
    try {
      setIsLoading(true);
      const payload = timetable.flatMap(day => day.timeSlots.map(slot => {
        const [code, ...rest] = String(slot.subject || '').split(' - ');
        return {
          id: slot.id && !isNaN(Number(slot.id)) ? Number(slot.id) : undefined,
          department: selectedDepartment, year: selectedYear, semester: selectedSemester, section: selectedSection,
          dayOfWeek: day.day.toUpperCase(),
          startTime: `${slot.startTime}:00`, endTime: `${slot.endTime}:00`,
          subjectCode: slot.subjectAbbrev || code || 'SUB',
          subjectName: rest.join(' - ') || slot.subject || 'Subject',
          faculty: slot.faculty || null, facultyAbbrev: slot.facultyAbbrev || null,
          classroom: slot.classroom || null, type: slot.type || 'LECTURE',
          batch: slot.batch !== 'ALL' ? slot.batch : null,
          spanAllPeriods: slot.spanAllPeriods || false,
        };
      }));
      await apiService.post('/timetable/bulk', payload);
      toast.success('Timetable saved!'); await loadTimetable();
    } catch { toast.error('Failed to save timetable'); }
    finally { setIsLoading(false); }
  };

  const getSlotsForCell = (day: string, periodStart: string) =>
    timetable.find(d => d.day === day)?.timeSlots.filter(s => s.startTime === periodStart) || [];

  const getSpanSlot = (day: string) =>
    timetable.find(d => d.day === day)?.timeSlots.find(s => s.spanAllPeriods);



  // ── Formal Cell ─────────────────────────────────────────────────────────────
  const FormalCell = ({ day, periodStart }: { day: string; periodStart: string }) => {
    const slots = getSlotsForCell(day, periodStart);
    if (slots.length === 0) return <td className="border border-gray-400 px-1 py-1 min-w-[80px] h-16" />;
    const batchGroups: Record<string, TimeSlot[]> = {};
    slots.forEach(s => { const b = s.batch || 'ALL'; if (!batchGroups[b]) batchGroups[b] = []; batchGroups[b].push(s); });
    return (
      <td className="border border-gray-400 px-1 py-1 min-w-[80px] h-16 align-top">
        {Object.entries(batchGroups).map(([, bSlots], idx) => (
          <div key={idx} className={idx > 0 ? 'border-t border-dashed border-gray-300 pt-0.5 mt-0.5' : ''}>
            {bSlots.map((slot, j) => (
              <div key={j} className="leading-tight">
                <div className="text-red-600 font-bold text-[10px]">
                  {slot.subjectAbbrev || slot.subject}{slot.batch && slot.batch !== 'ALL' ? `(${slot.batch})` : ''}
                </div>
                {slot.facultyAbbrev && <div className="text-blue-700 text-[10px]">{slot.facultyAbbrev}</div>}
                {slot.classroom && <div className="text-green-700 text-[10px]">{slot.classroom}</div>}
              </div>
            ))}
          </div>
        ))}
      </td>
    );
  };

  // ── Formal View ─────────────────────────────────────────────────────────────
  const renderFormalView = () => (
    <div>
      <div className="flex justify-end mb-4 print:hidden gap-3">
        <button onClick={() => window.print()}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
          <Printer className="w-4 h-4" /> Print / Export PDF
        </button>
      </div>
      <div id="print-area" className="bg-white text-black overflow-x-auto">
        <table className="w-full border-collapse border border-gray-500 text-xs">
          <thead>
            <tr>
              <td colSpan={11} className="border border-gray-500 text-center font-bold text-base py-2 px-2">
                {instituteSettings.name}, {instituteSettings.location}
              </td>
            </tr>
            <tr>
              <td colSpan={11} className="border border-gray-500 text-center font-bold text-red-600 text-sm py-1">
                CLASS TIME TABLE
              </td>
            </tr>
            <tr>
              <td colSpan={5} className="border border-gray-500 px-2 py-1 text-xs">
                <b>Name of process:</b> Time Table of {instituteSettings.yearOfProcess}
              </td>
              <td colSpan={4} className="border border-gray-500 text-center font-bold px-2 py-1 text-xs">
                PROGRAM: {DEPT_FULL_NAMES[selectedDepartment] || selectedDepartment}, Sem {selectedSemester}, 2025-26
              </td>
              <td className="border border-gray-500 px-2 py-1 text-xs">Ver. No.: {instituteSettings.verNo}</td>
              <td className="border border-gray-500 px-2 py-1 text-xs">Issue No: {instituteSettings.issueNo}</td>
            </tr>
            <tr>
              <td colSpan={11} className="border border-gray-500 px-2 py-1 text-xs">
                W.e.f.: {instituteSettings.wefDate}
              </td>
            </tr>
            <tr className="bg-gray-100">
              <th className="border border-gray-500 px-2 py-2 text-center text-xs font-bold">DAYS</th>
              {TIME_PERIODS.map((p, i) => (
                <React.Fragment key={p.id}>
                  {i === 2 && (
                    <th className="border border-gray-500 w-6 text-center text-[9px] font-medium text-gray-600 bg-amber-50" style={{ writingMode: 'vertical-rl', letterSpacing: 2 }}>
                      TEA BREAK
                    </th>
                  )}
                  {i === 4 && (
                    <th className="border border-gray-500 w-6 text-center text-[9px] font-medium text-gray-600 bg-green-50" style={{ writingMode: 'vertical-rl', letterSpacing: 2 }}>
                      LUNCH BREAK
                    </th>
                  )}
                  <th className="border border-gray-500 px-1 py-2 text-center text-xs">
                    <div className="font-bold">{p.id}</div>
                    <div className="text-gray-500 text-[9px]">{p.display}</div>
                  </th>
                </React.Fragment>
              ))}
            </tr>
          </thead>
          <tbody>
            {DAYS.map(day => {
              const spanSlot = getSpanSlot(day);
              return (
                <tr key={day}>
                  <td className="border border-gray-500 px-2 py-1 font-bold text-center uppercase text-xs whitespace-nowrap">
                    {day.toUpperCase()}
                  </td>
                  {spanSlot ? (
                    <td colSpan={9} className="border border-gray-500 text-center font-bold text-sm bg-yellow-50 py-3">
                      {spanSlot.subjectAbbrev || spanSlot.subject}
                    </td>
                  ) : (
                    TIME_PERIODS.map((p, i) => (
                      <React.Fragment key={p.id}>
                        {i === 2 && <td className="border border-gray-500 bg-amber-50 w-6" />}
                        {i === 4 && <td className="border border-gray-500 bg-green-50 w-6" />}
                        <FormalCell day={day} periodStart={p.start} />
                      </React.Fragment>
                    ))
                  )}
                </tr>
              );
            })}
          </tbody>
        </table>

        {/* Nomenclature + Faculty */}
        <table className="w-full border-collapse border border-t-0 border-gray-500 text-xs">
          <tbody>
            <tr>
              <td className="border border-gray-500 px-2 py-1 font-bold w-1/2">Nomenclature of the Course</td>
              <td className="border border-gray-500 px-2 py-1 font-bold">Faculty Details</td>
            </tr>
            {Array.from({ length: Math.max(nomenclature.length, facultyDetails.length) }).map((_, i) => (
              <tr key={i}>
                <td className="border border-gray-500 px-2 py-0.5 text-[10px]">
                  {nomenclature[i] ? `${nomenclature[i].abbrev} : ${nomenclature[i].fullName}` : ''}
                </td>
                <td className="border border-gray-500 px-2 py-0.5 text-[10px]">
                  {facultyDetails[i] ? `${facultyDetails[i].abbrev} : ${facultyDetails[i].fullName}` : ''}
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Legend + Batch */}
        <table className="w-full border-collapse border border-t-0 border-gray-500 text-xs">
          <tbody>
            <tr>
              <td className="border border-gray-500 px-2 py-1 w-1/2">
                <div className="text-red-600 text-[10px]">* Indicates Subject Name</div>
                <div className="text-blue-700 text-[10px]">* Indicates Faculty Name</div>
                <div className="text-green-700 text-[10px]">* Indicates Classroom</div>
              </td>
              <td className="border border-gray-500 px-2 py-1">
                {batches.map(b => (
                  <div key={b.id} className="text-red-600 text-[10px]">
                    {b.name} - Roll No. {String(b.rollFrom).padStart(2, '0')} to {String(b.rollTo).padStart(2, '0')}
                  </div>
                ))}
              </td>
            </tr>
          </tbody>
        </table>

        {/* Signatures */}
        <table className="w-full border-collapse border border-t-0 border-gray-500 text-xs">
          <tbody>
            <tr>
              {[
                { name: instituteSettings.preparedBy, title: instituteSettings.preparedByTitle },
                { name: instituteSettings.verifiedBy, title: instituteSettings.verifiedByTitle },
              ].map((sig, i) => (
                <td key={i} className="border border-gray-500 px-4 py-3 text-center align-bottom w-1/5">
                  <div className="h-8 border-b border-gray-400 mb-1" />
                  <div className="font-medium text-[10px]">{sig.name || '_______________'}</div>
                  <div className="font-bold text-[10px]">{sig.title}</div>
                </td>
              ))}
              <td className="border border-gray-500 px-4 py-3 text-center align-middle w-1/5">
                <div className="text-[10px] text-gray-500">College Seal</div>
                <div className="w-14 h-14 border-2 border-dashed border-gray-400 rounded-full mx-auto mt-1" />
              </td>
              {[
                { name: instituteSettings.proposedBy, title: instituteSettings.proposedByTitle },
                { name: instituteSettings.approvedBy, title: instituteSettings.approvedByTitle },
              ].map((sig, i) => (
                <td key={i} className="border border-gray-500 px-4 py-3 text-center align-bottom w-1/5">
                  <div className="h-8 border-b border-gray-400 mb-1" />
                  <div className="font-medium text-[10px]">{sig.name || '_______________'}</div>
                  <div className="font-bold text-[10px]">{sig.title}</div>
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );

  // ── Settings Tab ────────────────────────────────────────────────────────────
  const renderSettings = () => (
    <div className="space-y-6">
      {/* Institute Info */}
      <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow border border-gray-200 dark:border-gray-700">
        <h3 className="text-lg font-bold text-gray-900 dark:text-white mb-4">Institute Information</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {([
            ['name', 'College Name'], ['location', 'Location'], ['programName', 'Program Name'],
            ['yearOfProcess', 'Year of Process'], ['wefDate', 'W.e.f. Date'],
            ['verNo', 'Version No.'], ['issueNo', 'Issue No.'],
            ['preparedBy', 'Prepared By (Name)'], ['preparedByTitle', 'Prepared By (Title)'],
            ['verifiedBy', 'Verified By (Name)'], ['verifiedByTitle', 'Verified By (Title)'],
            ['proposedBy', 'Proposed By (Name)'], ['proposedByTitle', 'Proposed By (Title)'],
            ['approvedBy', 'Approved By (Name)'], ['approvedByTitle', 'Approved By (Title)'],
          ] as [keyof InstituteSettings, string][]).map(([key, label]) => (
            <div key={key}>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">{label}</label>
              <input value={instituteSettings[key]} onChange={e => setInstituteSettings(prev => ({ ...prev, [key]: e.target.value }))}
                className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500" />
            </div>
          ))}
        </div>
      </div>

      {/* Batches */}
      <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow border border-gray-200 dark:border-gray-700">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-bold text-gray-900 dark:text-white">Batch Configuration</h3>
          <button onClick={() => setBatches(prev => [...prev, { id: `B${prev.length + 1}`, name: `Batch B${prev.length + 1}`, rollFrom: 1, rollTo: 20 }])}
            className="flex items-center gap-1 px-3 py-1.5 bg-primary-500 text-white rounded-lg text-sm">
            <Plus className="w-3 h-3" /> Add Batch
          </button>
        </div>
        <div className="space-y-3">
          {batches.map((batch, i) => (
            <div key={batch.id} className="flex items-center gap-3">
              <input value={batch.id} onChange={e => setBatches(prev => prev.map((b, j) => j === i ? { ...b, id: e.target.value } : b))}
                className="w-20 px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" placeholder="ID" />
              <input value={batch.name} onChange={e => setBatches(prev => prev.map((b, j) => j === i ? { ...b, name: e.target.value } : b))}
                className="flex-1 px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" placeholder="Batch Name" />
              <span className="text-xs text-gray-500">Roll:</span>
              <input type="number" value={batch.rollFrom} onChange={e => setBatches(prev => prev.map((b, j) => j === i ? { ...b, rollFrom: +e.target.value } : b))}
                className="w-16 px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
              <span className="text-xs text-gray-500">to</span>
              <input type="number" value={batch.rollTo} onChange={e => setBatches(prev => prev.map((b, j) => j === i ? { ...b, rollTo: +e.target.value } : b))}
                className="w-16 px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
              <button onClick={() => setBatches(prev => prev.filter((_, j) => j !== i))} className="text-red-500 hover:text-red-700">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Nomenclature */}
      <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow border border-gray-200 dark:border-gray-700">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-bold text-gray-900 dark:text-white">Subject Nomenclature</h3>
          <button onClick={() => setNomenclature(prev => [...prev, { id: Date.now().toString(), abbrev: '', fullName: '' }])}
            className="flex items-center gap-1 px-3 py-1.5 bg-primary-500 text-white rounded-lg text-sm">
            <Plus className="w-3 h-3" /> Add
          </button>
        </div>
        <div className="space-y-2">
          {nomenclature.map((item, i) => (
            <div key={item.id} className="flex items-center gap-3">
              <input value={item.abbrev} onChange={e => setNomenclature(prev => prev.map((n, j) => j === i ? { ...n, abbrev: e.target.value } : n))}
                className="w-28 px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" placeholder="Abbrev." />
              <input value={item.fullName} onChange={e => setNomenclature(prev => prev.map((n, j) => j === i ? { ...n, fullName: e.target.value } : n))}
                className="flex-1 px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" placeholder="Full Name" />
              <button onClick={() => setNomenclature(prev => prev.filter((_, j) => j !== i))} className="text-red-500 hover:text-red-700">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Faculty Details */}
      <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow border border-gray-200 dark:border-gray-700">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-bold text-gray-900 dark:text-white">Faculty Details</h3>
          <button onClick={() => setFacultyDetails(prev => [...prev, { id: Date.now().toString(), abbrev: '', fullName: '' }])}
            className="flex items-center gap-1 px-3 py-1.5 bg-primary-500 text-white rounded-lg text-sm">
            <Plus className="w-3 h-3" /> Add
          </button>
        </div>
        <div className="space-y-2">
          {facultyDetails.map((item, i) => (
            <div key={item.id} className="flex items-center gap-3">
              <input value={item.abbrev} onChange={e => setFacultyDetails(prev => prev.map((f, j) => j === i ? { ...f, abbrev: e.target.value } : f))}
                className="w-20 px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" placeholder="Abbrev." />
              <input value={item.fullName} onChange={e => setFacultyDetails(prev => prev.map((f, j) => j === i ? { ...f, fullName: e.target.value } : f))}
                className="flex-1 px-2 py-1.5 border border-gray-300 dark:border-gray-600 rounded text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" placeholder="Full Name" />
              <button onClick={() => setFacultyDetails(prev => prev.filter((_, j) => j !== i))} className="text-red-500 hover:text-red-700">
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      </div>
    </div>
  );

  // ── Builder Tab ─────────────────────────────────────────────────────────────
  const renderBuilder = () => {
    // Slot background helper
    const slotBg = (slots: TimeSlot[]) => {
      if (!slots.length) return '';
      const s = slots[0];
      if (s.subject?.toLowerCase().includes('major project')) return 'bg-violet-100 dark:bg-violet-900/30 border-violet-300 dark:border-violet-600';
      if (s.type === 'PRACTICAL' || s.type === 'SEMINAR') return 'bg-amber-50 dark:bg-amber-900/20 border-amber-300 dark:border-amber-600';
      return 'bg-primary-50 dark:bg-primary-900/20 border-primary-200 dark:border-primary-700';
    };

    // For a given day + period: get all slots, combine into one cell display
    const getCellContent = (day: string, period: typeof TIME_PERIODS[0]) => {
      return getSlotsForCell(day, period.start);
    };

    // Is this a "full-day" slot (major project) on this day?
    const isFullDayDay = (day: string) => {
      return DAYS.some(() => {
        const entry = timetable.find(e => e.day.toLowerCase() === day.toLowerCase());
        if (!entry) return false;
        const allSlots = entry.timeSlots;
        return allSlots.some(s =>
          (s.subject?.toLowerCase().includes('major project') || s.spanAllPeriods) &&
          s.startTime === '09:15'
        );
      });
    };

    const getFullDaySlot = (day: string): TimeSlot | null => {
      const entry = timetable.find(e => e.day.toLowerCase() === day.toLowerCase());
      if (!entry) return null;
      return entry.timeSlots.find(s =>
        s.subject?.toLowerCase().includes('major project') || s.spanAllPeriods
      ) ?? null;
    };

    return (
      <div className="space-y-4">
        {conflicts.length > 0 && (
          <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }}
            className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-xl p-4">
            <div className="flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-red-600 mt-0.5" />
              <div>
                <h4 className="font-semibold text-red-800 dark:text-red-200 mb-1">Conflicts Detected</h4>
                {conflicts.map((c, i) => <div key={i} className="text-sm text-red-700 dark:text-red-300">• {c}</div>)}
              </div>
            </div>
          </motion.div>
        )}

        <div className="bg-white dark:bg-gray-800 rounded-xl shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div className="overflow-x-auto overflow-y-auto max-h-[75vh]">
            <table className="border-collapse text-[11px] w-full">
              {/* ── Header row ── */}
              <thead>
                <tr className="bg-gray-100 dark:bg-gray-700">
                  {/* Days column header */}
                  <th className="sticky left-0 z-20 bg-gray-100 dark:bg-gray-700 border border-gray-300 dark:border-gray-600 px-3 py-2 text-center font-bold text-gray-700 dark:text-gray-200 uppercase tracking-wide text-[10px] w-[80px]">
                    DAYS
                  </th>
                  {/* Period columns with tea/lunch breaks */}
                  {TIME_PERIODS.map((p, idx) => (
                    <React.Fragment key={p.id}>
                      {/* TEA BREAK between period 2 and 3 */}
                      {idx === 2 && (
                        <th className="border border-gray-300 dark:border-gray-600 bg-amber-50 dark:bg-amber-900/20 text-amber-700 dark:text-amber-400 font-bold text-[9px] w-[36px] px-0.5 py-1 text-center align-middle" rowSpan={1}>
                          <div style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)', whiteSpace: 'nowrap' }}>TEA BREAK<br />11:15-11:30</div>
                        </th>
                      )}
                      {/* LUNCH BREAK between period 4 and 5 */}
                      {idx === 4 && (
                        <th className="border border-gray-300 dark:border-gray-600 bg-orange-50 dark:bg-orange-900/20 text-orange-700 dark:text-orange-400 font-bold text-[9px] w-[36px] px-0.5 py-1 text-center" rowSpan={1}>
                          <div style={{ writingMode: 'vertical-rl', transform: 'rotate(180deg)', whiteSpace: 'nowrap' }}>LUNCH<br />1:30-2:00</div>
                        </th>
                      )}
                      <th className="border border-gray-300 dark:border-gray-600 px-1 py-1.5 text-center font-bold text-gray-700 dark:text-gray-200 bg-gray-100 dark:bg-gray-700 min-w-[115px]">
                        <div className="text-[11px] font-bold">{p.id}</div>
                        <div className="text-[9px] font-normal text-gray-500 dark:text-gray-400">{p.display}</div>
                      </th>
                    </React.Fragment>
                  ))}
                </tr>
              </thead>

              {/* ── Body: one row per day ── */}
              <tbody>
                {DAYS.map((day) => {
                  const fullDay = getFullDaySlot(day);
                  return (
                    <tr key={day} className="hover:bg-gray-50/50 dark:hover:bg-gray-700/20 transition-colors">
                      {/* Day label — sticky */}
                      <td className="sticky left-0 z-10 bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 px-2 py-2 text-center font-bold text-gray-800 dark:text-gray-100 uppercase text-[10px] tracking-wide whitespace-nowrap">
                        {day}
                      </td>

                      {/* Full-day slot (Major Project / span all) */}
                      {fullDay ? (
                        <td colSpan={TIME_PERIODS.length + 2} /* +2 for tea+lunch */
                          className="border border-gray-300 dark:border-gray-600 bg-violet-100 dark:bg-violet-900/30 text-center py-3 px-2">
                          <div className="group relative inline-flex flex-col items-center gap-0.5">
                            <span className="font-bold text-violet-700 dark:text-violet-300 text-sm tracking-wide">
                              {fullDay.subjectAbbrev ?? fullDay.subject}
                            </span>
                            {fullDay.faculty && (
                              <span className="text-[10px] text-violet-600 dark:text-violet-400">{fullDay.faculty}</span>
                            )}
                            <div className="absolute top-0 right-0 opacity-0 group-hover:opacity-100 transition-opacity flex gap-1 -mt-1 -mr-1">
                              <button onClick={() => editTimeSlot(fullDay)} className="p-0.5 bg-white dark:bg-gray-700 rounded shadow text-blue-500 hover:text-blue-700"><Edit className="w-3 h-3" /></button>
                              <button onClick={() => deleteTimeSlot(fullDay.id)} className="p-0.5 bg-white dark:bg-gray-700 rounded shadow text-red-500 hover:text-red-700"><Trash2 className="w-3 h-3" /></button>
                            </div>
                          </div>
                        </td>
                      ) : (
                        /* Normal periods */
                        TIME_PERIODS.map((period, idx) => {
                          const slots = getCellContent(day, period);
                          return (
                            <React.Fragment key={period.id}>
                              {/* Tea break cell */}
                              {idx === 2 && (
                                <td className="border border-gray-300 dark:border-gray-600 bg-amber-50 dark:bg-amber-900/10 w-[36px]" />
                              )}
                              {/* Lunch break cell */}
                              {idx === 4 && (
                                <td className="border border-gray-300 dark:border-gray-600 bg-orange-50 dark:bg-orange-900/10 w-[36px]" />
                              )}

                              <td className={`border border-gray-300 dark:border-gray-600 px-1 py-1 min-w-[115px] align-top`}>
                                {slots.length > 0 ? (
                                  <div className={`group relative rounded p-1.5 border ${slotBg(slots)} h-full`}>
                                    {/* Subject line — combined for multi-batch */}
                                    <div className="font-bold text-[10px] text-primary-700 dark:text-primary-300 leading-tight">
                                      {slots.map((s, i) => (
                                        <span key={s.id}>
                                          {i > 0 && <span className="text-gray-400">/ </span>}
                                          {s.subjectAbbrev ?? s.subject}
                                          {s.batch && s.batch !== 'ALL' && (
                                            <span className="text-amber-600 dark:text-amber-400 font-normal">({s.batch})</span>
                                          )}
                                        </span>
                                      ))}
                                    </div>
                                    {/* Faculty line */}
                                    <div className="text-[9px] text-emerald-700 dark:text-emerald-400 leading-tight mt-0.5">
                                      {slots.map((s, i) => (
                                        <span key={s.id}>
                                          {i > 0 && <span className="text-gray-400">/ </span>}
                                          {/* Faculty abbrev from name (last 3 uppercase chars) or show name */}
                                          {s.faculty?.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 3) || s.faculty}
                                        </span>
                                      ))}
                                    </div>
                                    {/* Classroom line */}
                                    <div className="text-[9px] text-blue-600 dark:text-blue-400 leading-tight mt-0.5">
                                      {slots.map((s, i) => (
                                        <span key={s.id}>
                                          {i > 0 && <span className="text-gray-400">/ </span>}
                                          {s.classroom}
                                        </span>
                                      ))}
                                    </div>
                                    {/* Hover actions */}
                                    <div className="absolute top-0.5 right-0.5 opacity-0 group-hover:opacity-100 transition-opacity flex gap-0.5">
                                      {slots.slice(0, 1).map(slot => (
                                        <React.Fragment key={slot.id}>
                                          <button onClick={() => editTimeSlot(slot)} className="p-0.5 bg-white dark:bg-gray-700 rounded shadow-sm text-blue-500 hover:text-blue-700"><Edit className="w-2.5 h-2.5" /></button>
                                          <button onClick={() => deleteTimeSlot(slot.id)} className="p-0.5 bg-white dark:bg-gray-700 rounded shadow-sm text-red-500 hover:text-red-700"><Trash2 className="w-2.5 h-2.5" /></button>
                                        </React.Fragment>
                                      ))}
                                    </div>
                                  </div>
                                ) : (
                                  <div
                                    onClick={() => { setSelectedDay(day); setSelectedPeriodId(period.id.toString()); setShowAddModal(true); }}
                                    className="h-14 border border-dashed border-gray-200 dark:border-gray-600 rounded flex items-center justify-center text-gray-300 dark:text-gray-600 hover:border-primary-400 hover:text-primary-400 cursor-pointer transition-colors"
                                  >
                                    <Plus className="w-3 h-3" />
                                  </div>
                                )}
                              </td>
                            </React.Fragment>
                          );
                        })
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Legend */}
          <div className="px-4 py-2 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/80 flex items-center gap-4 flex-wrap">
            <span className="text-[10px] font-semibold text-gray-500 dark:text-gray-400">Legend:</span>
            {[
              { bg: 'bg-primary-100 dark:bg-primary-900/30 border-primary-300', label: 'Theory' },
              { bg: 'bg-amber-100 dark:bg-amber-900/30 border-amber-400', label: 'Lab / Practical' },
              { bg: 'bg-violet-100 dark:bg-violet-900/30 border-violet-400', label: 'Major Project' },
            ].map(({ bg, label }) => (
              <span key={label} className="flex items-center gap-1 text-[10px] text-gray-600 dark:text-gray-400">
                <span className={`w-3 h-3 rounded border ${bg}`} /> {label}
              </span>
            ))}
            <span className="text-[9px] text-gray-400 ml-auto">Hover a slot to Edit / Mark Attendance / Delete</span>
          </div>
        </div>
      </div>
    );
  };


  if (isLoading && !timetable.length) return (
    <div className="flex items-center justify-center min-h-[60vh]">
      <div className="text-center"><LoadingSpinner size="lg" /><p className="mt-4 text-gray-500">Loading timetable...</p></div>
    </div>
  );

  return (
    <div className="space-y-6">
      {/* Print CSS */}
      <style>{`@media print { .print\\:hidden { display: none !important; } #print-area { font-size: 10px; } }`}</style>

      {/* Header */}
      <div className="flex items-center justify-between print:hidden">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Timetable Builder</h1>
          <p className="text-gray-500 dark:text-gray-400 text-sm">Create and manage class schedules</p>
        </div>
        <div className="flex gap-3">
          {activeTab === 'builder' && (
            <>
              <button onClick={() => setShowAddModal(true)}
                className="flex items-center gap-2 px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-lg text-sm">
                <Plus className="w-4 h-4" /> Add Class
              </button>
              <button onClick={saveTimetable} disabled={isLoading}
                className="flex items-center gap-2 px-4 py-2 bg-green-500 hover:bg-green-600 disabled:opacity-60 text-white rounded-lg text-sm">
                <Save className="w-4 h-4" /> Save
              </button>
            </>
          )}
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white dark:bg-gray-800 rounded-xl p-4 shadow border border-gray-200 dark:border-gray-700 print:hidden">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {/* Department */}
          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Department</label>
            <select value={selectedDepartment} onChange={e => setSelectedDepartment(e.target.value as Department)}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500">
              {Object.values(DEPARTMENTS).map(d => <option key={d} value={d}>{d}</option>)}
            </select>
          </div>
          {/* Year */}
          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Year</label>
            <select value={selectedYear} onChange={e => {
              const yr = +e.target.value;
              setSelectedYear(yr);
              setSelectedSemester(yr * 2 - 1); // auto-snap to first sem of that year
            }}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500">
              {[1, 2, 3, 4].map(y => <option key={y} value={y}>Year {y}</option>)}
            </select>
          </div>
          {/* Semester — only shows 2 options based on year */}
          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Semester</label>
            <select value={selectedSemester} onChange={e => setSelectedSemester(+e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500">
              {[selectedYear * 2 - 1, selectedYear * 2].map(s => (
                <option key={s} value={s}>Sem {s}</option>
              ))}
            </select>
          </div>
          {/* Division */}
          <div>
            <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">Division</label>
            <select value={selectedSection} onChange={e => setSelectedSection(e.target.value)}
              className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary-500">
              {['A', 'B', 'C'].map(s => <option key={s} value={s}>Div {s}</option>)}
            </select>
          </div>
        </div>
      </div>

      {/* Tab Bar */}
      <div className="flex gap-1 bg-gray-100 dark:bg-gray-800 p-1 rounded-xl w-fit print:hidden">
        {([
          { id: 'builder', label: 'Builder', icon: Grid },
          { id: 'view', label: 'Institutional View', icon: Eye },
          { id: 'settings', label: 'Settings', icon: Settings },
        ] as { id: 'builder' | 'view' | 'settings'; label: string; icon: any }[]).map(tab => (
          <button key={tab.id} onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all ${activeTab === tab.id ? 'bg-white dark:bg-gray-700 text-primary-600 dark:text-primary-400 shadow' : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-white'}`}>
            <tab.icon className="w-4 h-4" /> {tab.label}
          </button>
        ))}
      </div>

      {/* Tab Content */}
      {activeTab === 'builder' && renderBuilder()}
      {activeTab === 'view' && renderFormalView()}
      {activeTab === 'settings' && renderSettings()}

      {/* Add/Edit Modal */}
      <AnimatePresence>
        {showAddModal && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
            <motion.div initial={{ scale: 0.9, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.9, opacity: 0 }}
              className="bg-white dark:bg-gray-800 rounded-xl p-6 w-full max-w-lg shadow-xl max-h-[90vh] overflow-y-auto">
              <div className="flex items-center justify-between mb-5">
                <h2 className="text-lg font-bold text-gray-900 dark:text-white">{editingSlot ? 'Edit Class' : 'Add New Class'}</h2>
                <button onClick={() => { setShowAddModal(false); resetForm(); }} className="p-1.5 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"><X className="w-5 h-5" /></button>
              </div>
              <div className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Day</label>
                    <select value={selectedDay} onChange={e => setSelectedDay(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white">
                      <option value="">Select Day</option>
                      {DAYS.map(d => <option key={d} value={d}>{d}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Period</label>
                    <select value={selectedPeriodId} onChange={e => setSelectedPeriodId(e.target.value)}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white">
                      <option value="">Select Period</option>
                      {TIME_PERIODS.map(p => <option key={p.id} value={p.id}>Period {p.id} ({p.display})</option>)}
                    </select>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Subject</label>
                    <select value={formData.subject} onChange={e => setFormData(p => ({ ...p, subject: e.target.value }))}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white">
                      <option value="">Select Subject</option>
                      {(SUBJECTS_BY_DEPT[selectedDepartment] || []).map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Subject Abbrev. <span className="text-red-500">(for formal view)</span></label>
                    <input value={formData.subjectAbbrev} onChange={e => setFormData(p => ({ ...p, subjectAbbrev: e.target.value }))}
                      placeholder="e.g. DC, ADS" className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Faculty Name</label>
                    <input value={formData.faculty} onChange={e => setFormData(p => ({ ...p, faculty: e.target.value }))}
                      list="faculty-list" placeholder="Faculty name"
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
                    <datalist id="faculty-list">{facultyDetails.map(f => <option key={f.id} value={f.fullName} />)}</datalist>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Faculty Abbrev.</label>
                    <input value={formData.facultyAbbrev} onChange={e => setFormData(p => ({ ...p, facultyAbbrev: e.target.value }))}
                      list="abbrev-list" placeholder="e.g. IPJ, PAY"
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
                    <datalist id="abbrev-list">{facultyDetails.map(f => <option key={f.id} value={f.abbrev} />)}</datalist>
                  </div>
                </div>
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Classroom</label>
                    <input value={formData.classroom} onChange={e => setFormData(p => ({ ...p, classroom: e.target.value }))}
                      list="room-list" placeholder="Room code"
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white" />
                    <datalist id="room-list">{CLASSROOMS.map(r => <option key={r} value={r} />)}</datalist>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Batch</label>
                    <select value={formData.batch} onChange={e => setFormData(p => ({ ...p, batch: e.target.value }))}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white">
                      <option value="ALL">All Batches</option>
                      {batches.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-gray-700 dark:text-gray-300 mb-1">Type</label>
                    <select value={formData.type} onChange={e => setFormData(p => ({ ...p, type: e.target.value as ClassType }))}
                      className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-sm bg-white dark:bg-gray-700 text-gray-900 dark:text-white">
                      {Object.values(CLASS_TYPE).map(t => <option key={t} value={t}>{t}</option>)}
                    </select>
                  </div>
                </div>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input type="checkbox" checked={formData.spanAllPeriods} onChange={e => setFormData(p => ({ ...p, spanAllPeriods: e.target.checked }))}
                    className="rounded border-gray-300 text-primary-500" />
                  <span className="text-sm text-gray-700 dark:text-gray-300">Span entire day (e.g. Major Project)</span>
                </label>
              </div>
              <div className="flex justify-end gap-3 mt-6">
                <button onClick={() => { setShowAddModal(false); resetForm(); }}
                  className="px-4 py-2 text-sm text-gray-700 dark:text-gray-300 bg-gray-100 dark:bg-gray-700 hover:bg-gray-200 dark:hover:bg-gray-600 rounded-lg">
                  Cancel
                </button>
                <button onClick={editingSlot ? updateTimeSlot : addTimeSlot}
                  className="px-4 py-2 text-sm bg-primary-500 hover:bg-primary-600 text-white rounded-lg">
                  {editingSlot ? 'Update' : 'Add Class'}
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Timetable-wise Attendance Modal — temporarily disabled, re-enable later */}
    </div>
  );
};

export default TimetableBuilder;

