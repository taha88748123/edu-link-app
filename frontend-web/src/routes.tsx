import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom';

import App from './App';
import { RequireAuth } from './auth/RequireAuth';
import { RequireRole } from './auth/RequireRole';
import { useAuth } from './auth/useAuth';
import { AppShell } from './components/layout/AppShell';
import { PublicShell } from './components/layout/PublicShell';

import LandingPage from './pages/public/LandingPage';
import LoginPage from './pages/public/LoginPage';
import RegisterPage from './pages/public/RegisterPage';
import VerifyOtpPage from './pages/public/VerifyOtpPage';
import PasswordResetRequestPage from './pages/public/PasswordResetRequestPage';
import PasswordResetConfirmPage from './pages/public/PasswordResetConfirmPage';

import StudentDashboardPage from './pages/protected/student/StudentDashboardPage';
import ProfDashboardPage from './pages/protected/prof/ProfDashboardPage';
import AdminDashboardPage from './pages/protected/admin/AdminDashboardPage';
import NotFoundPage from './pages/protected/NotFoundPage';
import ForbiddenPage from './pages/protected/ForbiddenPage';

/**
 * Dispatches authenticated users to the right home depending on their role.
 */
function RoleHomeRedirect() {
  const { role } = useAuth();
  switch (role) {
    case 'ADMIN':
      return <Navigate to="/admin" replace />;
    case 'PROF':
      return <Navigate to="/prof" replace />;
    case 'STUDENT':
    case 'PARENT':
    default:
      return <Navigate to="/student" replace />;
  }
}

function RoleHome() {
  return (
    <RequireAuth>
      <RoleHomeRedirect />
    </RequireAuth>
  );
}

export const router = createBrowserRouter([
  {
    element: <App />,
    children: [
      // --- Public routes (PublicShell with locale switcher only).
      {
        element: (
          <PublicShell>
            <Outlet />
          </PublicShell>
        ),
        children: [
          { path: '/', element: <LandingPage /> },
          { path: '/login', element: <LoginPage /> },
          { path: '/register', element: <RegisterPage /> },
          { path: '/auth/verify-otp', element: <VerifyOtpPage /> },
          { path: '/auth/forgot-password', element: <PasswordResetRequestPage /> },
          { path: '/auth/reset-password', element: <PasswordResetConfirmPage /> },
        ],
      },

      // --- Authenticated home dispatcher (decides /student vs /prof vs /admin).
      { path: '/home', element: <RoleHome /> },

      // --- Protected routes wrapped by the AppShell.
      {
        element: (
          <RequireAuth>
            <AppShell>
              <Outlet />
            </AppShell>
          </RequireAuth>
        ),
        children: [
          {
            path: '/student',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>
                <StudentDashboardPage />
              </RequireRole>
            ),
          },
          {
            path: '/prof',
            element: (
              <RequireRole allow={['PROF']}>
                <ProfDashboardPage />
              </RequireRole>
            ),
          },
          {
            path: '/admin',
            element: (
              <RequireRole allow={['ADMIN']}>
                <AdminDashboardPage />
              </RequireRole>
            ),
          },
          { path: '/forbidden', element: <ForbiddenPage /> },
        ],
      },

      // --- 404 fallback.
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);
