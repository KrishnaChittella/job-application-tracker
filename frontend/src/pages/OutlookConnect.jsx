import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { getOutlookAuthorizeUrl, getOutlookStatus, outlookSync } from '../api';

export default function OutlookConnect() {
  const { token } = useAuth();
  const [status, setStatus] = useState({ configured: false, connected: false });
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    getOutlookStatus()
      .then((res) => setStatus(res.data))
      .catch(() => setStatus({ configured: false, connected: false }))
      .finally(() => setLoading(false));
  }, []);

  function handleConnect() {
    setError('');
    getOutlookAuthorizeUrl(token)
      .then((res) => {
        window.location.href = res.data.authorizeUrl;
      })
      .catch((err) => setError(err.response?.data?.message || err.message || 'Could not get authorize URL'));
  }

  function handleSync() {
    setError('');
    setSyncing(true);
    setSyncResult(null);
    outlookSync()
      .then((res) => setSyncResult(res.data))
      .catch((err) => setError(err.response?.data?.message || err.message || 'Sync failed'))
      .finally(() => setSyncing(false));
  }

  if (loading) return <div className="container"><p>Loading…</p></div>;

  return (
    <div className="container">
      <div className="page-header">
        <h1>Outlook / Microsoft 365</h1>
      </div>

      <div className="outlook-section">
        <div className="card">
          <div>
            <h3 className="card-title">Email sync</h3>
            <p style={{ color: 'var(--text-muted)', margin: '0 0 1rem', fontSize: '0.9rem' }}>
              Connect your Outlook organizational mailbox to detect job-related emails (rejections, interview invites, offers, etc.)
              using rule-based keyword parsing. No paid AI APIs.
            </p>
            {!status.configured && (
              <div className="alert alert-info">
                Admin must configure Azure AD app (client id, secret, tenant, redirect URI). See README for setup.
              </div>
            )}
            {status.configured && !status.connected && (
              <button type="button" className="btn btn-primary" onClick={handleConnect}>
                Connect Outlook
              </button>
            )}
            {status.configured && status.connected && (
              <div>
                <span style={{ color: 'var(--success)', marginRight: '1rem' }}>✓ Connected</span>
                <button type="button" className="btn btn-primary" disabled={syncing} onClick={handleSync}>
                  {syncing ? 'Syncing…' : 'Sync now'}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {syncResult && (
        <div className="card">
          <h3 className="card-title">Last sync result</h3>
          <p>Applications created: <strong>{syncResult.applicationsCreated}</strong></p>
          <p>Applications updated: <strong>{syncResult.applicationsUpdated}</strong></p>
          {syncResult.parsedEmails?.length > 0 && (
            <div className="table-wrap" style={{ marginTop: '1rem' }}>
              <table>
                <thead>
                  <tr>
                    <th>Subject</th>
                    <th>Detected status</th>
                    <th>Company hint</th>
                  </tr>
                </thead>
                <tbody>
                  {syncResult.parsedEmails.map((e) => (
                    <tr key={e.messageId}>
                      <td>{e.subject}</td>
                      <td>{e.status}</td>
                      <td>{e.companyHint}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
