import { useEffect, useState } from 'react';
import { useWifiConfiguration } from './useWifiConfiguration';
import { getEncryptionTypes, getWifiBandTypes } from './api/wifiApi';

export default function App() {
  const { data, error, loading, fetchConfig, saveConfig } = useWifiConfiguration();

  const [cpeId, setCpeId] = useState('CPE_001');
  const [form, setForm] = useState({
    cpeId: '',
    encryptionType: '',
    password: '',
    ssid: '',
    wifiBandType: '',
  });

  const [encryptionTypes, setEncryptionTypes] = useState([]);
  const [wifiBandTypes, setWifiBandTypes] = useState([]);
  const [metaLoading, setMetaLoading] = useState(true);
  const [metaError, setMetaError] = useState(null);

  useEffect(() => {
    async function loadMetadata() {
      setMetaLoading(true);
      setMetaError(null);

      try {
        const [encryptionTypesData, wifiBandTypesData] = await Promise.all([
          getEncryptionTypes(),
          getWifiBandTypes(),
        ]);

        setEncryptionTypes(encryptionTypesData);
        setWifiBandTypes(wifiBandTypesData);

        setForm((prev) => ({
          ...prev,
          encryptionType: prev.encryptionType || encryptionTypesData[0] || '',
          wifiBandType: prev.wifiBandType || wifiBandTypesData[0] || '',
        }));
      } catch (err) {
        setMetaError(err.message || 'Failed to load selector options');
      } finally {
        setMetaLoading(false);
      }
    }

    loadMetadata();
  }, []);

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
    } catch (_) {}
  }

  async function handleSave(e) {
    e.preventDefault();

    if (form.encryptionType !== 'OPEN' && !form.password.trim()) {
      alert('Password is required for secured encryption types.');
      return;
    }

    try {
      await saveConfig({
        ...form,
        password: form.encryptionType === 'OPEN' ? '' : form.password,
      });
      alert('Saved successfully');
    } catch (_) {}
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
      <div style={{ padding: 24, maxWidth: 520 }}>
        <h1>Wi-Fi Admin</h1>

        <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
          <input
              value={cpeId}
              onChange={(e) => setCpeId(e.target.value)}
              placeholder="Enter CPE ID"
          />
          <button onClick={handleFetch} disabled={loading || metaLoading}>
            {loading ? 'Loading...' : 'Fetch'}
          </button>
        </div>

        {metaError && (
            <div style={{ color: 'crimson', marginBottom: 16 }}>
              {metaError}
            </div>
        )}

        {error && (
            <div style={{ color: 'crimson', marginBottom: 16 }}>
              {error.code ? `${error.code}: ` : ''}
              {error.message}
            </div>
        )}

        <form onSubmit={handleSave} style={{ display: 'grid', gap: 12 }}>
          <input
              value={form.cpeId}
              onChange={(e) => updateField('cpeId', e.target.value)}
              placeholder="CPE ID"
          />

          <input
              value={form.ssid}
              onChange={(e) => updateField('ssid', e.target.value)}
              placeholder="SSID"
          />

          <select
              value={form.encryptionType}
              onChange={(e) => updateField('encryptionType', e.target.value)}
              disabled={metaLoading || encryptionTypes.length === 0}
          >
            {encryptionTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
            ))}
          </select>

          <input
              value={form.password}
              onChange={(e) => updateField('password', e.target.value)}
              placeholder="Password"
              disabled={form.encryptionType === 'OPEN'}
          />

          <select
              value={form.wifiBandType}
              onChange={(e) => updateField('wifiBandType', e.target.value)}
              disabled={metaLoading || wifiBandTypes.length === 0}
          >
            {wifiBandTypes.map((band) => (
                <option key={band} value={band}>
                  {band}
                </option>
            ))}
          </select>

          <button type="submit" disabled={loading || metaLoading}>
            {loading ? 'Saving...' : 'Save'}
          </button>
        </form>

        {data && (
            <pre style={{ marginTop: 24 }}>
          {JSON.stringify(data, null, 2)}
        </pre>
        )}
      </div>
  );
}