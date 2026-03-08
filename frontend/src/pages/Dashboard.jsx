import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getDashboard } from '../api';

const STATUS_LABELS = {
  APPLIED: 'Applied',
  ASSESSMENT: 'Assessment',
  INTERVIEW: 'Interview',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
  ARCHIVED: 'Archived',
};

function statusClass(s) {
  return 'badge badge-' + (s ? s.toLowerCase() : '');
}

function formatDate(iso) {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString();
  } catch {
    return iso;
  }
}

export default function Dashboard() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getDashboard()
      .then((res) => setData(res.data))
      .catch((err) => setError(err.response?.data?.message || err.message || 'Failed to load dashboard'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="container"><p>Loading dashboard…</p></div>;
  if (error) return <div className="container"><div className="alert alert-error">{error}</div></div>;
  if (!data) return null;

  const { totalApplications, countsByStatus, recentApplications } = data;

  return (
    <div className="container">
      <div className="page-header">
        <h1>Dashboard</h1>
        <Link to="/applications/new" className="btn btn-primary">Add application</Link>
      </div>

      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-value">{totalApplications}</div>
          <div className="stat-label">Total</div>
        </div>
        {Object.entries(countsByStatus || {}).map(([status, count]) => (
          <div key={status} className="stat-card">
            <div className="stat-value">{count}</div>
            <div className="stat-label">{STATUS_LABELS[status] || status}</div>
          </div>
        ))}
      </div>

      <div className="card">
        <h2 className="card-title">Recent applications</h2>
        {recentApplications?.length ? (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Company</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Applied</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {recentApplications.map((app) => (
                  <tr key={app.id}>
                    <td>{app.companyName}</td>
                    <td>{app.roleTitle}</td>
                    <td><span className={statusClass(app.currentStatus)}>{STATUS_LABELS[app.currentStatus] || app.currentStatus}</span></td>
                    <td>{formatDate(app.appliedDate)}</td>
                    <td><Link to={`/applications/${app.id}`} className="btn btn-sm btn-secondary">View</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <p className="empty-state">No applications yet. <Link to="/applications/new">Add one</Link> or connect Outlook to sync from email.</p>
        )}
      </div>
    </div>
  );
}
