import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getApplications } from '../api';

const STATUS_OPTIONS = [
  { value: '', label: 'All statuses' },
  { value: 'APPLIED', label: 'Applied' },
  { value: 'ASSESSMENT', label: 'Assessment' },
  { value: 'INTERVIEW', label: 'Interview' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'ARCHIVED', label: 'Archived' },
];

const STATUS_LABELS = Object.fromEntries(STATUS_OPTIONS.filter((o) => o.value).map((o) => [o.value, o.label]));

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

export default function ApplicationList() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  function load() {
    setLoading(true);
    getApplications({
      status: status || undefined,
      search: search || undefined,
      page,
      size: 20,
    })
      .then((res) => {
        setItems(res.data.content ?? res.data);
        setTotalPages(res.data.totalPages ?? 0);
      })
      .catch((err) => setError(err.response?.data?.message || err.message || 'Failed to load'))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, [page, status]);

  function handleSearchSubmit(e) {
    e.preventDefault();
    setPage(0);
    load();
  }

  return (
    <div className="container">
      <div className="page-header">
        <h1>Applications</h1>
        <Link to="/applications/new" className="btn btn-primary">Add application</Link>
      </div>

      <form onSubmit={handleSearchSubmit} className="filters-bar">
        <input
          type="text"
          className="form-control"
          placeholder="Search company or role..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select
          className="form-control"
          value={status}
          onChange={(e) => { setStatus(e.target.value); setPage(0); }}
        >
          {STATUS_OPTIONS.map((o) => (
            <option key={o.value || 'all'} value={o.value}>{o.label}</option>
          ))}
        </select>
        <button type="submit" className="btn btn-secondary">Search</button>
      </form>

      {error && <div className="alert alert-error">{error}</div>}

      {loading ? (
        <p>Loading…</p>
      ) : (
        <>
          <div className="card">
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Company</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Source</th>
                    <th>Applied</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {items.length === 0 ? (
                    <tr><td colSpan={6} className="empty-state">No applications match. <Link to="/applications/new">Add one</Link>.</td></tr>
                  ) : (
                    items.map((app) => (
                      <tr key={app.id}>
                        <td>{app.companyName}</td>
                        <td>{app.roleTitle}</td>
                        <td><span className={statusClass(app.currentStatus)}>{STATUS_LABELS[app.currentStatus] || app.currentStatus}</span></td>
                        <td>{app.source || '—'}</td>
                        <td>{formatDate(app.appliedDate)}</td>
                        <td>
                          <Link to={`/applications/${app.id}`} className="btn btn-sm btn-secondary">Edit</Link>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
          {totalPages > 1 && (
            <div className="pagination">
              <button type="button" className="btn btn-secondary btn-sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</button>
              <span>Page {page + 1} of {totalPages}</span>
              <button type="button" className="btn btn-secondary btn-sm" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
