import { lazy, Suspense, type ComponentType, type ReactElement } from 'react';
import { createBrowserRouter, Navigate, Outlet } from 'react-router-dom';

import App from './App';
import { RequireAuth } from './auth/RequireAuth';
import { RequireRole } from './auth/RequireRole';
import { useAuth } from './auth/useAuth';
import { AppShell } from './components/layout/AppShell';
import { PublicShell } from './components/layout/PublicShell';
import { LoadingScreen } from './components/layout/LoadingScreen';

/**
 * Every page route is code-split via {@link lazy} so the initial bundle only
 * loads the auth + shell + landing chunk. Heavy modules (LiveKit, admin tools)
 * are fetched on demand the first time the user opens that route.
 *
 * IMPORTANT: lazy imports MUST point at a single page file with a default
 * export — bundlers cannot statically analyse re-export barrels.
 */
const LandingPage = lazy(() => import('./pages/public/LandingPage'));
const LoginPage = lazy(() => import('./pages/public/LoginPage'));
const RegisterPage = lazy(() => import('./pages/public/RegisterPage'));
const VerifyOtpPage = lazy(() => import('./pages/public/VerifyOtpPage'));
const PasswordResetRequestPage = lazy(() => import('./pages/public/PasswordResetRequestPage'));
const PasswordResetConfirmPage = lazy(() => import('./pages/public/PasswordResetConfirmPage'));

const StudentDashboardPage = lazy(() => import('./pages/protected/student/StudentDashboardPage'));
const DiscoverPage = lazy(() => import('./pages/protected/student/DiscoverPage'));
const ProfPublicPage = lazy(() => import('./pages/protected/student/ProfPublicPage'));
const BookingCreatePage = lazy(() => import('./pages/protected/student/BookingCreatePage'));
const WalletPage = lazy(() => import('./pages/protected/student/WalletPage'));
const TopupsPage = lazy(() => import('./pages/protected/student/TopupsPage'));

const ProfDashboardPage = lazy(() => import('./pages/protected/prof/ProfDashboardPage'));
const CalendarPage = lazy(() => import('./pages/protected/prof/CalendarPage'));
const PricingPage = lazy(() => import('./pages/protected/prof/PricingPage'));
const ProfWalletPage = lazy(() => import('./pages/protected/prof/ProfWalletPage'));

const BookingsListPage = lazy(() => import('./pages/protected/bookings/BookingsListPage'));
const BookingDetailPage = lazy(() => import('./pages/protected/bookings/BookingDetailPage'));

const ChatListPage = lazy(() => import('./pages/protected/chat/ChatListPage'));
const ChatDetailPage = lazy(() => import('./pages/protected/chat/ChatDetailPage'));
const VideoLobbyPage = lazy(() => import('./pages/protected/video/VideoLobbyPage'));
const VideoRoomPage = lazy(() => import('./pages/protected/video/VideoRoomPage'));

const AdminDashboardPage = lazy(() => import('./pages/protected/admin/AdminDashboardPage'));
const AdminValidationsPage = lazy(() => import('./pages/protected/admin/AdminValidationsPage'));
const AdminTopupsPage = lazy(() => import('./pages/protected/admin/AdminTopupsPage'));
const AdminPayoutsPage = lazy(() => import('./pages/protected/admin/AdminPayoutsPage'));
const AdminDisputesPage = lazy(() => import('./pages/protected/admin/AdminDisputesPage'));
const AdminAuditPage = lazy(() => import('./pages/protected/admin/AdminAuditPage'));
const AdminBalanceAdjustPage = lazy(() => import('./pages/protected/admin/AdminBalanceAdjustPage'));

const NotFoundPage = lazy(() => import('./pages/protected/NotFoundPage'));
const ForbiddenPage = lazy(() => import('./pages/protected/ForbiddenPage'));

/**
 * Wraps a lazily-loaded page element in a {@link Suspense} boundary so the
 * shell stays mounted while the chunk loads (no full-screen flash).
 */
function withSuspense(node: ReactElement): ReactElement {
  return <Suspense fallback={<LoadingScreen />}>{node}</Suspense>;
}

/** Helper that lifts a plain page Component into a Suspense-wrapped element. */
function page<P>(C: ComponentType<P>, props?: P): ReactElement {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return withSuspense(<C {...(props as any)} />);
}

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
          { path: '/', element: page(LandingPage) },
          { path: '/login', element: page(LoginPage) },
          { path: '/register', element: page(RegisterPage) },
          { path: '/auth/verify-otp', element: page(VerifyOtpPage) },
          { path: '/auth/forgot-password', element: page(PasswordResetRequestPage) },
          { path: '/auth/reset-password', element: page(PasswordResetConfirmPage) },
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
                {page(StudentDashboardPage)}
              </RequireRole>
            ),
          },
          {
            path: '/student/discover',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>{page(DiscoverPage)}</RequireRole>
            ),
          },
          {
            path: '/student/profs/:profId',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>{page(ProfPublicPage)}</RequireRole>
            ),
          },
          {
            path: '/student/profs/:profId/book',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>{page(BookingCreatePage)}</RequireRole>
            ),
          },
          {
            path: '/student/bookings',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>
                {page(BookingsListPage, { basePath: '/student' as const })}
              </RequireRole>
            ),
          },
          {
            path: '/student/bookings/:id',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT']}>{page(BookingDetailPage)}</RequireRole>
            ),
          },
          {
            path: '/student/wallet',
            element: <RequireRole allow={['STUDENT', 'PARENT']}>{page(WalletPage)}</RequireRole>,
          },
          {
            path: '/student/topups',
            element: <RequireRole allow={['STUDENT', 'PARENT']}>{page(TopupsPage)}</RequireRole>,
          },

          // PROF routes
          {
            path: '/prof',
            element: <RequireRole allow={['PROF']}>{page(ProfDashboardPage)}</RequireRole>,
          },
          {
            path: '/prof/calendar',
            element: <RequireRole allow={['PROF']}>{page(CalendarPage)}</RequireRole>,
          },
          {
            path: '/prof/pricing',
            element: <RequireRole allow={['PROF']}>{page(PricingPage)}</RequireRole>,
          },
          {
            path: '/prof/bookings',
            element: (
              <RequireRole allow={['PROF']}>
                {page(BookingsListPage, { basePath: '/prof' as const })}
              </RequireRole>
            ),
          },
          {
            path: '/prof/bookings/:id',
            element: <RequireRole allow={['PROF']}>{page(BookingDetailPage)}</RequireRole>,
          },
          {
            path: '/prof/wallet',
            element: <RequireRole allow={['PROF']}>{page(ProfWalletPage)}</RequireRole>,
          },

          // CHAT (STUDENT, PARENT, PROF — backend enforces participant scoping)
          {
            path: '/chat',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT', 'PROF']}>{page(ChatListPage)}</RequireRole>
            ),
          },
          {
            path: '/chat/:conversationId',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT', 'PROF']}>
                {page(ChatDetailPage)}
              </RequireRole>
            ),
          },

          // VIDEO ROOM (STUDENT, PARENT, PROF). LiveKit is in its own chunk thanks to
          // the lazy import + manualChunks split below.
          {
            path: '/video/sessions/:sessionId/lobby',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT', 'PROF']}>
                {page(VideoLobbyPage)}
              </RequireRole>
            ),
          },
          {
            path: '/video/sessions/:sessionId',
            element: (
              <RequireRole allow={['STUDENT', 'PARENT', 'PROF']}>{page(VideoRoomPage)}</RequireRole>
            ),
          },

          // ADMIN routes
          {
            path: '/admin',
            element: <RequireRole allow={['ADMIN']}>{page(AdminDashboardPage)}</RequireRole>,
          },
          {
            path: '/admin/validations',
            element: <RequireRole allow={['ADMIN']}>{page(AdminValidationsPage)}</RequireRole>,
          },
          {
            path: '/admin/topups',
            element: <RequireRole allow={['ADMIN']}>{page(AdminTopupsPage)}</RequireRole>,
          },
          {
            path: '/admin/payouts',
            element: <RequireRole allow={['ADMIN']}>{page(AdminPayoutsPage)}</RequireRole>,
          },
          {
            path: '/admin/disputes',
            element: <RequireRole allow={['ADMIN']}>{page(AdminDisputesPage)}</RequireRole>,
          },
          {
            path: '/admin/audit',
            element: <RequireRole allow={['ADMIN']}>{page(AdminAuditPage)}</RequireRole>,
          },
          {
            path: '/admin/balance-adjust',
            element: <RequireRole allow={['ADMIN']}>{page(AdminBalanceAdjustPage)}</RequireRole>,
          },

          { path: '/forbidden', element: page(ForbiddenPage) },
        ],
      },

      // --- 404 fallback.
      { path: '*', element: page(NotFoundPage) },
    ],
  },
]);
