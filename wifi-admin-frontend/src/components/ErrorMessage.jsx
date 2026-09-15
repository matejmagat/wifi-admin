export default function ErrorMessage({ error }) {
    if (!error) return null;

    return (
        <div className="error-message" role="alert">
      <span className="error-message__icon" aria-hidden="true">
        !
      </span>

            <div>
                <p className="error-message__title">Request failed</p>
                <p className="error-message__text">
                    {error.code ? `${error.code}: ` : ''}
                    {error.message || String(error)}
                </p>
            </div>
        </div>
    );
}