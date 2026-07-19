export default function ErrorMessage({ error }) {
    if (!error) return null;

    return (
        <div style={{ color: 'crimson', marginBottom: 16 }}>
            {error.code ? `${error.code}: ` : ''}
            {error.message || String(error)}
        </div>
    );
}