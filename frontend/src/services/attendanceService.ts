export interface AttendanceRecord {
  id?: number;
  studentId: number;
  student?: {
    id: number;
    firstName: string;
    lastName: string;
    ienNumber: string;
    department: string;
  };
  checkInTime: string;
  status: 'PRESENT' | 'LATE' | 'ABSENT' | 'EXCUSED';
  markingMethod: 'FACE_RECOGNITION' | 'MANUAL';
  faceConfidence?: number;
  createdAt?: string;
  updatedAt?: string;
  subject?: string;
  classId?: string;
}

export interface AttendanceStats {
  presentCount: number;
  absentCount: number;
  lateCount: number;
  excusedCount: number;
  unmarkedCount: number;
  totalStudents: number;
}

export interface SlotAttendanceRecord {
  studentId: number;
  status: 'PRESENT' | 'ABSENT' | 'LATE' | 'EXCUSED';
  subject?: string;
}

class AttendanceService {
  private baseUrl = 'http://localhost:8080/api';

  // Mark attendance for a student (face recognition flow)
  async markAttendance(studentId: number, confidence: number = 1.0): Promise<boolean> {
    try {
      const todayAttendance = await this.getTodayAttendance();
      const alreadyMarked = todayAttendance.find(record =>
        record.student?.id === studentId || record.studentId === studentId
      );

      if (alreadyMarked) {
        console.log('⚠️ Attendance already marked for student:', studentId);
        return false;
      }

      const attendanceData = {
        studentId,
        markingMethod: 'FACE_RECOGNITION',
        faceConfidence: confidence,
        checkInTime: new Date().toISOString(),
        status: 'PRESENT',
      };

      const response = await fetch(`${this.baseUrl}/attendance`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(attendanceData),
      });

      if (response.ok) {
        const result = await response.json();
        console.log('✅ Attendance marked successfully:', result);
        return true;
      } else {
        const error = await response.json();
        console.error('❌ Failed to mark attendance:', error.message || 'Unknown error');
        return false;
      }
    } catch (error) {
      console.error('❌ Error marking attendance:', error);
      return false;
    }
  }

  // Get today's attendance statistics
  async getTodayStats(): Promise<AttendanceStats> {
    try {
      const response = await fetch(`${this.baseUrl}/attendance/stats/today`);
      if (response.ok) {
        const result = await response.json();
        return result.data || {
          presentCount: 0, absentCount: 0, lateCount: 0,
          excusedCount: 0, unmarkedCount: 0, totalStudents: 0,
        };
      }
    } catch (error) {
      console.error('❌ Error getting today stats:', error);
    }
    return { presentCount: 0, absentCount: 0, lateCount: 0, excusedCount: 0, unmarkedCount: 0, totalStudents: 0 };
  }

  // Get today's attendance records
  async getTodayAttendance(): Promise<AttendanceRecord[]> {
    try {
      const response = await fetch(`${this.baseUrl}/attendance/today?size=50`);
      if (response.ok) {
        const result = await response.json();
        return result.data?.content || result.data || [];
      }
    } catch (error) {
      console.error('❌ Error getting today attendance:', error);
    }
    return [];
  }

  // Reset all attendance data (for testing)
  async resetAllAttendance(): Promise<boolean> {
    try {
      const response = await fetch(`${this.baseUrl}/attendance/reset-all`, {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
      });
      if (response.ok) {
        console.log('🗑️ All attendance data reset');
        return true;
      }
    } catch (error) {
      console.error('❌ Error resetting attendance:', error);
    }
    return false;
  }

  // Manual attendance marking (single student)
  async markManualAttendance(
    studentId: number,
    status: 'PRESENT' | 'LATE' | 'ABSENT' | 'EXCUSED'
  ): Promise<boolean> {
    try {
      const attendanceData = {
        studentId,
        markingMethod: 'MANUAL',
        checkInTime: new Date().toISOString(),
        status,
      };

      const response = await fetch(`${this.baseUrl}/attendance`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(attendanceData),
      });

      if (response.ok) {
        console.log('✅ Manual attendance marked');
        return true;
      }
    } catch (error) {
      console.error('❌ Error marking manual attendance:', error);
    }
    return false;
  }

  // ── Timetable-slot-wise attendance ──────────────────────────────────────────

  // Bulk mark attendance for an entire class for a specific timetable slot
  async markSlotAttendance(
    slotId: string,
    subject: string,
    records: SlotAttendanceRecord[]
  ): Promise<{ success: boolean; saved?: number; message?: string }> {
    try {
      const response = await fetch(`${this.baseUrl}/attendance/bulk`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ slotId, subject, records }),
      });
      const result = await response.json();
      return result;
    } catch (error) {
      console.error('❌ Error marking slot attendance:', error);
      return { success: false, message: 'Network error' };
    }
  }

  // Fetch attendance records for a specific slot/subject on a given date
  async getSlotAttendance(subject: string, date?: string): Promise<AttendanceRecord[]> {
    try {
      const dateParam = date || new Date().toISOString().split('T')[0];
      const response = await fetch(
        `${this.baseUrl}/attendance/slot?subject=${encodeURIComponent(subject)}&date=${dateParam}`
      );
      if (response.ok) {
        const result = await response.json();
        return result.data || [];
      }
    } catch (error) {
      console.error('❌ Error fetching slot attendance:', error);
    }
    return [];
  }
}

const attendanceService = new AttendanceService();
export default attendanceService;
