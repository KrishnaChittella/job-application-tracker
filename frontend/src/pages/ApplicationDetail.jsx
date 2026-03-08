import { useState, useEffect } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import { getApplicationById, deleteApplication } from '../api';

const STATUS_LABELS = {
  APPLIED: 'Applied',
  ASSESSMENT: 'Assessment',
  INTERVIEW: 'Interview',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
  ARCHIVED: 'Archived',
};

function formatDate(iso) {
  if (!iso) return '—';
  try {
    return new Date(iso).toLocaleDateString();
  } catch {
    return iso;
  }
}

export default function ApplicationDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [app, setApp] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    getApplicationById(id)
      .then((res) => setApp(res.data))
      .catch((err) => setError(err.response?.data?.message || err.message || 'Failed to load'))
      .finally(() => setLoading(false));
  }, [id]);

  function handleDelete() {
    if (!window.confirm('Delete this application?')) return;
    setDeleting(true);
    deleteApplication(id)
      .then(() => navigate('/applications'))
      .catch((err) => setError(err.response?.data?.message || err.message || 'Delete failed'))
      .finally(() => setDeleting(false));
  }

  if (loading) return <div className="container"><p>Loading…</p></div>;
  if (error && !app) return <div className="container"><div className="alert alert-error">{error}</div></div>;
  if (!app) return null;

  return (
    <div className="container">
      <div className="page-header">
        <h1>{app.companyName} – {app.roleTitle}</h1>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <Link to={`/applications/${id}/edit`} className="btn btn-secondary">Edit</Link>
          <button type="button" className="btn btn-danger" disabled={deleting} onClick={handleDelete}>
            {deleting ? 'Deleting…' : 'Delete'}
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="card">
        <dl style={{ margin: 0, display: 'grid', gap: '0.75rem' }}>
          <div><dt style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Status</dt><dd>{STATUS_LABELS[app.currentStatus] || app.currentStatus}</dd></div>
          <div><dt style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Source</dt><dd>{app.source || '—'}</dd></div>
          <div><dt style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Job link</dt><dd>{app.jobLink ? <a href={app.jobLink} target="_blank" rel="noopener noreferrer">Open</a> : '—'}</dd></div>
          <div><dt style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Location</dt><dd>{app.location || '—'}</dd></div>
          <div><dt style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Applied date</dt><dd>{formatDate(app.appliedDate)}</dd></div>
          <div><dt style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Follow-up date</dt><dd>{formatDate(app.followUpDate)}</dd></div>
          <div><dt style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>Notes</dt><dd style={{ whiteSpace: 'pre-wrap' }}>{app.notes || '—'}</dd></div>
        </dl>
      </div>
    </div>
  );
}
