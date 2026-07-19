import { useEffect, useState } from 'react';
import ErrorMessage from './components/ErrorMessage';
import WifiForm from './components/WifiForm';
import WifiLookup from './components/WifiLookup';
import { useWifiConfiguration } from './hooks/useWifiConfiguration';
import { useWifiMetadata } from './hooks/useWifiMetadata';

const emptyForm = {
  cpeId: '',
  encryptionType: '',
  password: '',
  ssid: '',
  wifiBandType: '',
};

export default function App() {
  const { data, error, loading, fetchConfig, saveConfig } = useWifiConfiguration();
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
      encryptionType: prev.encryptionType || encryptionTypes[0] || '',
      wifiBandType: prev.wifiBandType || wifiBandTypes[0] || '',
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
    }
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
    } catch (_) {
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
      <div style={{ padding: 24, maxWidth: 520 }}>
        <h1>Wi-Fi Admin</h1>

        <WifiLookup
            cpeId={cpeId}
            onChange={setCpeId}
            onFetch={handleFetch}
            disabled={loading || metaLoading}
            loading={loading}
        />

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

        {data && (
            <pre style={{ marginTop: 24 }}>
          {JSON.stringify(data, null, 2)}
        </pre>
        )}
      </div>
  );
}