import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Toaster } from 'react-hot-toast';

// Contexts
import { ThemeProvider } from './contexts/ThemeContext';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { NotificationProvider } from './contexts/NotificationContext';

// Components
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import StudentManagement from './pages/StudentManagement';
import AttendanceMarking from './pages/AttendanceMarking';
import FaceRecognitionHub from './components/FaceRecognitionHub';
import DatabaseStatus from './pages/DatabaseStatus';
import TimetableBuilder from './pages/TimetableBuilder';
import Reports from './pages/Reports';
import PredictionsDashboard from './pages/PredictionsDashboard';
import ProfessionalReports from './pages/ProfessionalReports';
import EmailNotifications from './pages/EmailNotifications';
import GuardianPortal from './pages/GuardianPortal';

import AttendanceCertificate from './pages/AttendanceCertificate';
import SMSNotifications from './pages/SMSNotifications';
import LeaveManagement from './pages/LeaveManagement';
import AttendanceAnalytics from './pages/AttendanceAnalytics';
import Layout from './components/Layout';
import LoadingSpinner from './components/LoadingSpinner';
import { FEATURES } from './config/features';
import { fetchJson, HttpTimeoutError } from './utils/http';

// Protected Route Component
const ProtectedRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-primary-100">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

// Public Route Component (redirect if authenticated)
const PublicRoute: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-primary-100">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return <>{children}</>;
};

// App Routes Component
const AppRoutes: React.FC = () => {
  return (
    <AnimatePresence mode="wait">
      <Routes>
        {/* Public Routes */}
        <Route
          path="/login"
          element={
            <PublicRoute>
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{ duration: 0.3 }}
              >
                <LoginPage />
              </motion.div>
            </PublicRoute>
          }
        />

        {/* Guardian Portal - Public Route */}
        <Route
          path="/guardian-portal"
          element={
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              transition={{ duration: 0.3 }}
            >
              <GuardianPortal />
            </motion.div>
          }
        />

        {/* Protected Routes */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <Layout />
            </ProtectedRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />

          <Route
            path="dashboard"
            element={
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.3 }}
              >
                <Dashboard />
              </motion.div>
            }
          />

          <Route
            path="students"
            element={
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.3 }}
              >
                <StudentManagement />
              </motion.div>
            }
          />


          <Route
            path="attendance"
            element={
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.3 }}
              >
                <AttendanceMarking />
              </motion.div>
            }
          />

          <Route
            path="advanced-face-recognition"
            element={
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.3 }}
              >
                <FaceRecognitionHub />
              </motion.div>
            }
          />


          <Route
            path="database-status"
            element={
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.3 }}
              >
                <DatabaseStatus />
              </motion.div>
            }
          />

          <Route
            path="timetable"
            element={
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.3 }}
              >
                <TimetableBuilder />
              </motion.div>
            }
          />

          <Route
            path="reports"
            element={
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -20 }}
                transition={{ duration: 0.3 }}
              >
                <Reports />
              </motion.div>
            }
          />

          {FEATURES.predictions && (
            <Route
              path="predictions"
              element={
                <motion.div
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.3 }}
                >
                  <PredictionsDashboard />
                </motion.div>
              }
            />
          )}

          {FEATURES.emailNotifications && (
            <Route
              path="email-notifications"
              element={
                <motion.div
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.3 }}
                >
                  <EmailNotifications />
                </motion.div>
              }
            />
          )}



          {FEATURES.certificates && (
            <Route
              path="attendance-certificate"
              element={
                <motion.div
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.3 }}
                >
                  <AttendanceCertificate />
                </motion.div>
              }
            />
          )}

          {FEATURES.smsNotifications && (
            <Route
              path="sms-notifications"
              element={
                <motion.div
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.3 }}
                >
                  <SMSNotifications />
                </motion.div>
              }
            />
          )}

          {FEATURES.leaveManagement && (
            <Route
              path="leave-management"
              element={
                <motion.div
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.3 }}
                >
                  <LeaveManagement />
                </motion.div>
              }
            />
          )}

          {FEATURES.attendanceAnalytics && (
            <Route
              path="attendance-analytics"
              element={
                <motion.div
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.3 }}
                >
                  <AttendanceAnalytics />
                </motion.div>
              }
            />
          )}

        </Route>

        {/* Catch all route */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </AnimatePresence>
  );
};

// Main App Component
const App: React.FC = () => {
  useEffect(() => {
    const HEARTBEAT_INTERVAL_MS = 8 * 60 * 1000;

    const pingBackend = async () => {
      if (typeof document !== 'undefined' && document.visibilityState === 'hidden') {
        return;
      }

      try {
        await fetchJson('/health');
        console.log('💓 Backend heartbeat ok');
      } catch (error) {
        if (error instanceof HttpTimeoutError) {
          console.log('💓 Backend still waking up');
          return;
        }
        console.warn('💓 Backend heartbeat failed:', error);
      }
    };

    void pingBackend();
    const intervalId = window.setInterval(pingBackend, HEARTBEAT_INTERVAL_MS);

    const handleVisibilityChange = () => {
      if (document.visibilityState === 'visible') {
        void pingBackend();
      }
    };

    window.addEventListener('focus', pingBackend);
    document.addEventListener('visibilitychange', handleVisibilityChange);

    return () => {
      window.clearInterval(intervalId);
      window.removeEventListener('focus', pingBackend);
      document.removeEventListener('visibilitychange', handleVisibilityChange);
    };
  }, []);

  return (
    <ThemeProvider>
      <AuthProvider>
        <NotificationProvider>
          <Router>
            <div className="App min-h-screen bg-background text-foreground transition-colors duration-300">
              <AppRoutes />
              {/* Toast Notifications - Liquid Glass Theme */}
              <Toaster
                position="top-right"
                toastOptions={{
                  duration: 2500,
                  style: {
                    background: 'rgba(255, 255, 255, 0.85)',
                    backdropFilter: 'blur(12px)',
                    WebkitBackdropFilter: 'blur(12px)',
                    color: '#1f2937',
                    padding: '14px 18px',
                    borderRadius: '12px',
                    boxShadow: '0 4px 20px rgba(0,0,0,0.12)',
                    border: '1px solid rgba(255, 255, 255, 0.3)',
                    fontWeight: '500',
                    fontSize: '14px',
                  },
                  success: {
                    duration: 2000,
                    iconTheme: { primary: '#10b981', secondary: '#fff' },
                    style: {
                      background: 'rgba(16, 185, 129, 0.12)',
                      backdropFilter: 'blur(12px)',
                      border: '1px solid rgba(16, 185, 129, 0.25)',
                      color: '#065f46',
                    },
                  },
                  error: {
                    duration: 3000,
                    iconTheme: { primary: '#ef4444', secondary: '#fff' },
                    style: {
                      background: 'rgba(239, 68, 68, 0.12)',
                      backdropFilter: 'blur(12px)',
                      border: '1px solid rgba(239, 68, 68, 0.25)',
                      color: '#991b1b',
                    },
                  },
                }}
              />
            </div>
          </Router>
        </NotificationProvider>
      </AuthProvider>
    </ThemeProvider>
  );
};

export default App;
