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
    const isOpenNetwork = form.encryptionType === 'OPEN';
    const isDisabled = loading || metaLoading;

    return (
        <form className="wifi-form" onSubmit={onSubmit}>
            <div className="form-grid">
                <div className="field-group">
                    <label className="field-label" htmlFor="config-cpe-id">
                        CPE ID
                    </label>
                    <input
                        id="config-cpe-id"
                        value={form.cpeId}
                        onChange={(event) => onFieldChange('cpeId', event.target.value)}
                        placeholder="CPE ID"
                        required
                        disabled={isDisabled}
                    />
                </div>

                <div className="field-group">
                    <label className="field-label" htmlFor="ssid">
                        Network name <span className="field-label__hint">(SSID)</span>
                    </label>
                    <input
                        id="ssid"
                        value={form.ssid}
                        onChange={(event) => onFieldChange('ssid', event.target.value)}
                        placeholder="e.g. Home Wi‑Fi"
                        required
                        disabled={isDisabled}
                    />
                </div>

                <div className="field-group">
                    <label className="field-label" htmlFor="encryption-type">
                        Security type
                    </label>
                    <select
                        id="encryption-type"
                        value={form.encryptionType}
                        onChange={(event) => onFieldChange('encryptionType', event.target.value)}
                        disabled={metaLoading || encryptionTypes.length === 0 || loading}
                    >
                        {encryptionTypes.map((type) => (
                            <option key={type} value={type}>
                                {formatEnumLabel(type)}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="field-group">
                    <label className="field-label" htmlFor="wifi-band-type">
                        Wi‑Fi band
                    </label>
                    <select
                        id="wifi-band-type"
                        value={form.wifiBandType}
                        onChange={(event) => onFieldChange('wifiBandType', event.target.value)}
                        disabled={metaLoading || wifiBandTypes.length === 0 || loading}
                    >
                        {wifiBandTypes.map((band) => (
                            <option key={band} value={band}>
                                {formatEnumLabel(band)}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="field-group field-group--full">
                    <label className="field-label" htmlFor="wifi-password">
                        Password
                        {isOpenNetwork && (
                            <span className="field-label__hint">Not needed for open networks</span>
                        )}
                    </label>

                    <input
                        id="wifi-password"
                        type="password"
                        value={form.password}
                        onChange={(event) => onFieldChange('password', event.target.value)}
                        placeholder={isOpenNetwork ? 'No password required' : 'Enter network password'}
                        autoComplete="new-password"
                        disabled={isDisabled || isOpenNetwork}
                        required={!isOpenNetwork}
                    />
                </div>
            </div>

            <div className="form-footer">
                <p className="form-footer__note">
                    Changes are applied to the selected CPE device.
                </p>

                <button className="button button--primary" type="submit" disabled={isDisabled}>
                    {loading ? (
                        <>
                            <span className="spinner" aria-hidden="true" />
                            Saving changes
                        </>
                    ) : (
                        'Save configuration'
                    )}
                </button>
            </div>
        </form>
    );
}