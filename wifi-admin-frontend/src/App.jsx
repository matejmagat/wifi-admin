import { useEffect, useState } from 'react';
import './App.css';
import ErrorMessage from './components/ErrorMessage';
import LoginForm from './components/LoginForm';
import WifiForm from './components/WifiForm';
import WifiLookup from './components/WifiLookup';
import { useWifiConfiguration } from './hooks/useWifiConfiguration';
import { useWifiMetadata } from './hooks/useWifiMetadata';
import {
  clearCredentials,
  getStoredCredentials,
} from './utils/auth';

const emptyForm = {
  cpeId: '',
  encryptionType: '',
  password: '',
  ssid: '',
  wifiBandType: '',
};

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(
      () => getStoredCredentials() !== null
  );

  function handleLogout() {
    clearCredentials();
    setIsAuthenticated(false);
  }

  if (!isAuthenticated) {
    return (
        <LoginForm onAuthenticated={() => setIsAuthenticated(true)} />
    );
  }

  return <Dashboard onLogout={handleLogout} />;
}

function Dashboard({ onLogout }) {
  const { data, error, loading, fetchConfig, saveConfig } =
      useWifiConfiguration();

  const {
    encryptionTypes,
    wifiBandTypes,
    loading: metaLoading,
    error: metaError,
  } = useWifiMetadata();

  const [cpeId, setCpeId] = useState('CPE_001');
  const [form, setForm] = useState(emptyForm);

  useEffect(() => {
    if (metaLoading) return;

    setForm((prev) => ({
      ...prev,
      encryptionType:
          prev.encryptionType || encryptionTypes[0] || '',
      wifiBandType:
          prev.wifiBandType || wifiBandTypes[0] || '',
    }));
  }, [metaLoading, encryptionTypes, wifiBandTypes]);

  async function handleFetch() {
    try {
      const result = await fetchConfig(cpeId);

      setForm({
        cpeId: result.cpeId ?? '',
        encryptionType: result.encryptionType ?? '',
        password: result.password ?? '',
        ssid: result.ssid ?? '',
        wifiBandType: result.wifiBandType ?? '',
      });
    } catch (_) {
      // The hook exposes the request error through `error`.
    }
  }

  async function handleSave(event) {
    event.preventDefault();

    if (
        form.encryptionType !== 'OPEN' &&
        !form.password.trim()
    ) {
      alert('Password is required for secured encryption types.');
      return;
    }

    try {
      await saveConfig({
        ...form,
        password:
            form.encryptionType === 'OPEN' ? '' : form.password,
      });

      alert('Saved successfully');
    } catch (_) {
      // The hook exposes the request error through `error`.
    }
  }

  function updateField(name, value) {
    setForm((prev) => {
      const next = { ...prev, [name]: value };

      if (name === 'encryptionType' && value === 'OPEN') {
        next.password = '';
      }

      return next;
    });
  }

  return (
      <div className="app">
        <main className="app-shell">
          <header className="page-header">
            <div>
              <p className="eyebrow">Network management</p>
              <h1>Wi-Fi Admin</h1>
              <p className="page-subtitle">
                Retrieve and update wireless configuration for a CPE
                device.
              </p>
            </div>

            <button
                className="button button--secondary auth-button"
                onClick={onLogout}
                type="button"
            >
              Log out
            </button>
          </header>

          <section className="dashboard-grid">
            <aside className="lookup-panel">
              <div className="panel-heading">
                <span className="panel-number">01</span>
                <div>
                  <h2>Find device</h2>
                  <p>Load the current configuration by CPE ID.</p>
                </div>
              </div>

              <WifiLookup
                  cpeId={cpeId}
                  onChange={setCpeId}
                  onFetch={handleFetch}
                  disabled={loading || metaLoading}
                  loading={loading}
              />

              <div className="connection-status">
              <span
                  className={`status-dot ${
                      loading ? 'status-dot--loading' : ''
                  }`}
              />
                <span>
                {loading
                    ? 'Contacting configuration service…'
                    : 'Ready to fetch configuration'}
              </span>
              </div>
            </aside>

            <section className="config-panel">
              <div className="panel-heading">
                <span className="panel-number">02</span>
                <div>
                  <h2>Wireless configuration</h2>
                  <p>
                    Review the loaded values and save your changes.
                  </p>
                </div>
              </div>

              <ErrorMessage error={metaError} />
              <ErrorMessage error={error} />

              <WifiForm
                  form={form}
                  onFieldChange={updateField}
                  onSubmit={handleSave}
                  loading={loading}
                  metaLoading={metaLoading}
                  encryptionTypes={encryptionTypes}
                  wifiBandTypes={wifiBandTypes}
              />
            </section>
          </section>

          {data && (
              <details className="response-debug">
                <summary>Latest API response</summary>
                <pre>{JSON.stringify(data, null, 2)}</pre>
              </details>
          )}
        </main>
      </div>
  );
}