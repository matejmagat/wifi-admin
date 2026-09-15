export default function WifiLookup({
                                       cpeId,
                                       onChange,
                                       onFetch,
                                       disabled,
                                       loading,
                                   }) {
    function handleSubmit(event) {
        event.preventDefault();
        onFetch();
    }

    return (
        <form className="lookup-form" onSubmit={handleSubmit}>
            <label className="field-label" htmlFor="lookup-cpe-id">
                CPE ID
            </label>

            <div className="lookup-form__controls">
                <input
                    id="lookup-cpe-id"
                    value={cpeId}
                    onChange={(event) => onChange(event.target.value)}
                    placeholder="e.g. CPE_001"
                    autoComplete="off"
                    disabled={disabled}
                />

                <button className="button button--secondary" type="submit" disabled={disabled}>
                    {loading ? (
                        <>
                            <span className="spinner" aria-hidden="true" />
                            Loading
                        </>
                    ) : (
                        'Fetch'
                    )}
                </button>
            </div>
        </form>
    );
}