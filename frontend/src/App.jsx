import { BrowserRouter, Routes, Route, Navigate, Link } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ProtectedRoute } from './ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import ApplicationList from './pages/ApplicationList';
import ApplicationForm from './pages/ApplicationForm';
import ApplicationDetail from './pages/ApplicationDetail';
import OutlookConnect from './pages/OutlookConnect';
import OutlookCallback from './pages/OutlookCallback';

function Layout({ children }) {
  const { user, logout, isAuthenticated } = useAuth();
  if (!isAuthenticated) return children;
  return (
    <div className="app-layout">
      <nav className="nav">
        <div className="nav-inner">
          <div className="nav-links">
            <Link to="/">Dashboard</Link>
            <Link to="/applications">Applications</Link>
            <Link to="/outlook">Outlook</Link>
          </div>
          <div className="nav-user">
            {user?.displayName || user?.email}
            <button type="button" className="btn btn-sm btn-secondary" style={{ marginLeft: '0.75rem' }} onClick={logout}>Logout</button>
          </div>
        </div>
      </nav>
      <main style={{ flex: 1, padding: '1.5rem 0' }}>{children}</main>
    </div>
  );
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/outlook/callback" element={<OutlookCallback />} />
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <Layout><Dashboard /></Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/applications"
        element={
          <ProtectedRoute>
            <Layout><ApplicationList /></Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/applications/new"
        element={
          <ProtectedRoute>
            <Layout><ApplicationForm /></Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/applications/:id"
        element={
          <ProtectedRoute>
            <Layout><ApplicationDetail /></Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/applications/:id/edit"
        element={
          <ProtectedRoute>
            <Layout><ApplicationForm /></Layout>
          </ProtectedRoute>
        }
      />
      <Route
        path="/outlook"
        element={
          <ProtectedRoute>
            <Layout><OutlookConnect /></Layout>
          </ProtectedRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}
