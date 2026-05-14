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
import DiscoverPage from './pages/protected/student/DiscoverPage';
import ProfPublicPage from './pages/protected/student/ProfPublicPage';
import BookingCreatePage from './pages/protected/student/BookingCreatePage';
import WalletPage from './pages/protected/student/WalletPage';
import TopupsPage from './pages/protected/student/TopupsPage';

import ProfDashboardPage from './pages/protected/prof/ProfDashboardPage';
import CalendarPage from './pages/protected/prof/CalendarPage';
import PricingPage from './pages/protected/prof/PricingPage';
import ProfWalletPage from './pages/protected/prof/ProfWalletPage';

import BookingsListPage from './pages/protected/bookings/BookingsListPage';
import BookingDetailPage from './pages/protected/bookings/BookingDetailPage';

import ChatListPage from './pages/protected/chat/ChatListPage';
import ChatDetailPage from './pages/protected/chat/ChatDetailPage';
import VideoLobbyPage from './pages/protected/video/VideoLobbyPage';
import VideoRoomPage from './pages/protected/video/VideoRoomPage';

import AdminDashboardPage from './pages/protected/admin/AdminDashboardPage';
import AdminValidationsPage from './pages/protected/admin/AdminValidationsPage';
import AdminTopupsPage from './pages/protected/admin/AdminTopupsPage';
import AdminPayoutsPage from './pages/protected/admin/AdminPayoutsPage';
import AdminDisputesPage from './pages/protected/admin/AdminDisputesPage';
import AdminAuditPage from './pages/protected/admin/AdminAuditPage';
import AdminBalanceAdjustPage from './pages/protected/admin/AdminBalanceAdjustPage';

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
          // STUDENT (and PARENT) routes
          {
            path: '/student',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>
                <StudentDashboardPage />
              </RequireRole>
            ),
          },
          {
            path: '/student/discover',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>
                <DiscoverPage />
              </RequireRole>
            ),
          },
          {
            path: '/student/profs/:profId',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>
                <ProfPublicPage />
              </RequireRole>
            ),
          },
          {
            path: '/student/profs/:profId/book',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>
                <BookingCreatePage />
              </RequireRole>
            ),
          },
          {
            path: '/student/bookings',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>
                <BookingsListPage basePath="/student" />
              </RequireRole>
            ),
          },
          {
            path: '/student/bookings/:id',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>
                <BookingDetailPage />
              </RequireRole>
            ),
          },
          {
            path: '/student/wallet',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>
                <WalletPage />
              </RequireRole>
            ),
          },
          {
            path: '/student/topups',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>
                <TopupsPage />
              </RequireRole>
            ),
          },

          // PROF routes
          {
            path: '/prof',
            element: (
              <RequireRole allow={['PROF']}>
                <ProfDashboardPage />
              </RequireRole>
            ),
          },
          {
            path: '/prof/calendar',
            element: (
              <RequireRole allow={['PROF']}>
                <CalendarPage />
              </RequireRole>
            ),
          },
          {
            path: '/prof/pricing',
            element: (
              <RequireRole allow={['PROF']}>
                <PricingPage />
              </RequireRole>
            ),
          },
          {
            path: '/prof/bookings',
            element: (
              <RequireRole allow={['PROF']}>
                <BookingsListPage basePath="/prof" />
              </RequireRole>
            ),
          },
          {
            path: '/prof/bookings/:id',
            element: (
              <RequireRole allow={['PROF']}>
                <BookingDetailPage />
              </RequireRole>
            ),
          },
          {
            path: '/prof/wallet',
            element: (
              <RequireRole allow={['PROF']}>
                <ProfWalletPage />
              </RequireRole>
            ),
          },

          // CHAT (STUDENT, PARENT, PROF — backend enforces participant scoping)
          {
            path: '/chat',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT', 'PROF']}>
                <ChatListPage />
              </RequireRole>
            ),
          },
          {
            path: '/chat/:conversationId',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT', 'PROF']}>
                <ChatDetailPage />
              </RequireRole>
            ),
          },

          // VIDEO ROOM (STUDENT, PARENT, PROF)
          {
            path: '/video/sessions/:sessionId/lobby',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT', 'PROF']}>
                <VideoLobbyPage />
              </RequireRole>
            ),
          },
          {
            path: '/video/sessions/:sessionId',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT', 'PROF']}>
                <VideoRoomPage />
              </RequireRole>
            ),
          },

          // ADMIN routes
          {
            path: '/admin',
            element: (
              <RequireRole allow={['ADMIN']}>
                <AdminDashboardPage />
              </RequireRole>
            ),
          },
          {
            path: '/admin/validations',
            element: (
              <RequireRole allow={['ADMIN']}>
                <AdminValidationsPage />
              </RequireRole>
            ),
          },
          {
            path: '/admin/topups',
            element: (
              <RequireRole allow={['ADMIN']}>
                <AdminTopupsPage />
              </RequireRole>
            ),
          },
          {
            path: '/admin/payouts',
            element: (
              <RequireRole allow={['ADMIN']}>
                <AdminPayoutsPage />
              </RequireRole>
            ),
          },
          {
            path: '/admin/disputes',
            element: (
              <RequireRole allow={['ADMIN']}>
                <AdminDisputesPage />
              </RequireRole>
            ),
          },
          {
            path: '/admin/audit',
            element: (
              <RequireRole allow={['ADMIN']}>
                <AdminAuditPage />
              </RequireRole>
            ),
          },
          {
            path: '/admin/balance-adjust',
            element: (
              <RequireRole allow={['ADMIN']}>
                <AdminBalanceAdjustPage />
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
