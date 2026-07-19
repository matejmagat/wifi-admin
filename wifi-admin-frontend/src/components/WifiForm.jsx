import { formatEnumLabel } from '../utils/format';

export default function WifiForm({
                                     form,
                                     onFieldChange,
                                     onSubmit,
                                     loading,
                                     metaLoading,
                                     encryptionTypes,
                                     wifiBandTypes,
                                 }) {
    return (
        <form onSubmit={onSubmit} style={{ display: 'grid', gap: 12 }}>
            <input
                value={form.cpeId}
                onChange={(e) => onFieldChange('cpeId', e.target.value)}
                placeholder="CPE ID"
            />

            <input
                value={form.ssid}
                onChange={(e) => onFieldChange('ssid', e.target.value)}
                placeholder="SSID"
            />

            <select
                value={form.encryptionType}
                onChange={(e) => onFieldChange('encryptionType', e.target.value)}
                disabled={metaLoading || encryptionTypes.length === 0}
            >
                {encryptionTypes.map((type) => (
                    <option key={type} value={type}>
                        {formatEnumLabel(type)}
                    </option>
                ))}
            </select>

            <input
                value={form.password}
                onChange={(e) => onFieldChange('password', e.target.value)}
                placeholder="Password"
                disabled={form.encryptionType === 'OPEN'}
            />

            <select
                value={form.wifiBandType}
                onChange={(e) => onFieldChange('wifiBandType', e.target.value)}
                disabled={metaLoading || wifiBandTypes.length === 0}
            >
                {wifiBandTypes.map((band) => (
                    <option key={band} value={band}>
                        {formatEnumLabel(band)}
                    </option>
                ))}
            </select>

            <button type="submit" disabled={loading || metaLoading}>
                {loading ? 'Saving...' : 'Save'}
            </button>
        </form>
    );
}