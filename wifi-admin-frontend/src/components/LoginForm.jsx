import { useState } from "react";
import { saveCredentials } from "../utils/auth";
// import "./LoginForm.css";

export default function LoginForm({ onAuthenticated }) {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [showPassword, setShowPassword] = useState(false);

    function handleSubmit(event) {
        event.preventDefault();

        const trimmedUsername = username.trim();

        if (!trimmedUsername || !password) {
            return;
        }

        saveCredentials(trimmedUsername, password);
        onAuthenticated?.();
    }

    return (
        <main className="login-page">
            <section className="login-card" aria-labelledby="login-title">
                <div className="login-card__header">
                    <p className="login-card__eyebrow">Wi-Fi Administration</p>
                    <h1 id="login-title">Sign in</h1>
                    <p className="login-card__description">
                        Enter the credentials used by the Wi-Fi administration service.
                    </p>
                </div>

                <form className="login-form" onSubmit={handleSubmit}>
                    <label className="form-field">
                        <span>Username</span>
                        <input
                            autoComplete="username"
                            autoFocus
                            name="username"
                            onChange={(event) => setUsername(event.target.value)}
                            placeholder="Administrator username"
                            required
                            type="text"
                            value={username}
                        />
                    </label>

                    <label className="form-field">
                        <span>Password</span>
                        <div className="password-field">
                            <input
                                autoComplete="current-password"
                                name="password"
                                onChange={(event) => setPassword(event.target.value)}
                                placeholder="Password"
                                required
                                type={showPassword ? "text" : "password"}
                                value={password}
                            />
                            <button
                                aria-label={showPassword ? "Hide password" : "Show password"}
                                className="password-field__toggle"
                                onClick={() => setShowPassword((current) => !current)}
                                type="button"
                            >
                                {showPassword ? "Hide" : "Show"}
                            </button>
                        </div>
                    </label>

                    <button className="login-form__submit" type="submit">
                        Log in
                    </button>
                </form>
            </section>
        </main>
    );
}