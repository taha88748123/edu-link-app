import { Outlet } from 'react-router-dom';

/**
 * Root layout — currently just a passthrough for the router outlet.
 * Wrapped automatically by RouterProvider / createBrowserRouter.
 */
export default function App() {
  return <Outlet />;
}
