export default function WifiLookup({
                                       cpeId,
                                       onChange,
                                       onFetch,
                                       disabled,
                                       loading,
                                   }) {
    return (
        <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
            <input
                value={cpeId}
                onChange={(e) => onChange(e.target.value)}
                placeholder="Enter CPE ID"
            />
            <button onClick={onFetch} disabled={disabled}>
                {loading ? 'Loading...' : 'Fetch'}
            </button>
        </div>
    );
}