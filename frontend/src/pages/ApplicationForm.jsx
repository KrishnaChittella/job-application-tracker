import { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getApplicationById, createApplication, updateApplication } from '../api';

const STATUS_OPTIONS = ['APPLIED', 'ASSESSMENT', 'INTERVIEW', 'OFFER', 'REJECTED', 'ARCHIVED'];

const emptyForm = {
  companyName: '',
  roleTitle: '',
  currentStatus: 'APPLIED',
  source: '',
  jobLink: '',
  location: '',
  appliedDate: '',
  notes: '',
  followUpDate: '',
};

function toForm(app) {
  return {
    companyName: app.companyName || '',
    roleTitle: app.roleTitle || '',
    currentStatus: app.currentStatus || 'APPLIED',
    source: app.source || '',
    jobLink: app.jobLink || '',
    location: app.location || '',
    appliedDate: app.appliedDate || '',
    notes: app.notes || '',
    followUpDate: app.followUpDate || '',
  };
}

function toPayload(form) {
  const p = { ...form };
  if (!p.appliedDate) p.appliedDate = null;
  if (!p.followUpDate) p.followUpDate = null;
  return p;
}

export default function ApplicationForm() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [submitError, setSubmitError] = useState('');

  useEffect(() => {
    if (isEdit) {
      getApplicationById(id)
        .then((res) => setForm(toForm(res.data)))
        .catch((err) => setLoadError(err.response?.data?.message || err.message || 'Failed to load'));
    }
  }, [id, isEdit]);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitError('');
    setLoading(true);
    try {
      if (isEdit) {
        await updateApplication(id, toPayload(form));
        navigate(`/applications/${id}`);
      } else {
        const { data } = await createApplication(toPayload(form));
        navigate(`/applications/${data.id}`);
      }
    } catch (err) {
      setSubmitError(err.response?.data?.message || err.message || 'Save failed');
    } finally {
      setLoading(false);
    }
  }

  if (isEdit && loadError) return <div className="container"><div className="alert alert-error">{loadError}</div></div>;

  return (
    <div className="container">
      <div className="page-header">
        <h1>{isEdit ? 'Edit application' : 'New application'}</h1>
      </div>

      {submitError && <div className="alert alert-error">{submitError}</div>}

      <div className="card">
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="companyName">Company name *</label>
            <input id="companyName" name="companyName" className="form-control" value={form.companyName} onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label htmlFor="roleTitle">Role / title *</label>
            <input id="roleTitle" name="roleTitle" className="form-control" value={form.roleTitle} onChange={handleChange} required />
          </div>
          <div className="form-group">
            <label htmlFor="currentStatus">Status *</label>
            <select id="currentStatus" name="currentStatus" className="form-control" value={form.currentStatus} onChange={handleChange}>
              {STATUS_OPTIONS.map((s) => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="source">Source</label>
            <input id="source" name="source" className="form-control" value={form.source} onChange={handleChange} placeholder="e.g. LinkedIn, company site" />
          </div>
          <div className="form-group">
            <label htmlFor="jobLink">Job link</label>
            <input id="jobLink" name="jobLink" type="url" className="form-control" value={form.jobLink} onChange={handleChange} />
          </div>
          <div className="form-group">
            <label htmlFor="location">Location</label>
            <input id="location" name="location" className="form-control" value={form.location} onChange={handleChange} />
          </div>
          <div className="form-group">
            <label htmlFor="appliedDate">Applied date</label>
            <input id="appliedDate" name="appliedDate" type="date" className="form-control" value={form.appliedDate} onChange={handleChange} />
          </div>
          <div className="form-group">
            <label htmlFor="followUpDate">Follow-up date</label>
            <input id="followUpDate" name="followUpDate" type="date" className="form-control" value={form.followUpDate} onChange={handleChange} />
          </div>
          <div className="form-group">
            <label htmlFor="notes">Notes</label>
            <textarea id="notes" name="notes" className="form-control" value={form.notes} onChange={handleChange} />
          </div>
          <div style={{ display: 'flex', gap: '0.75rem' }}>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'Saving…' : (isEdit ? 'Update' : 'Create')}
            </button>
            <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  );
}
