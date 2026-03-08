import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { outlookCallback } from '../api';

/**
 * Handles redirect from Microsoft OAuth: reads code + state from URL,
 * sends to backend to exchange for tokens and store for user, then redirects to /outlook.
 */
export default function OutlookCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  useEffect(() => {
    const code = searchParams.get('code');
    const state = searchParams.get('state');
    if (!code || !state) {
      setError('Missing code or state from Microsoft.');
      setDone(true);
      return;
    }
    outlookCallback(code, state)
      .then(() => {
        setDone(true);
        navigate('/outlook', { replace: true });
      })
      .catch((err) => {
        setError(err.response?.data?.error || err.message || 'Failed to connect Outlook.');
        setDone(true);
      });
  }, [searchParams, navigate]);

  if (!done) return <div className="container" style={{ padding: '2rem', textAlign: 'center' }}>Connecting Outlook…</div>;
  if (error) return <div className="container"><div className="alert alert-error">{error}</div><button type="button" className="btn btn-secondary" onClick={() => navigate('/outlook')}>Back to Outlook</button></div>;
  return null;
}
