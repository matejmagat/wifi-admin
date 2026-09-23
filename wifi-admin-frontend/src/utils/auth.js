const AUTH_STORAGE_KEY = "wifi-admin.basic-auth";

export function getStoredCredentials() {
    const rawCredentials = sessionStorage.getItem(AUTH_STORAGE_KEY);

    if (!rawCredentials) {
        return null;
    }

    try {
        const credentials = JSON.parse(rawCredentials);

        if (!credentials.username || !credentials.password) {
            return null;
        }

        return credentials;
    } catch {
        sessionStorage.removeItem(AUTH_STORAGE_KEY);
        return null;
    }
}

export function saveCredentials(username, password) {
    sessionStorage.setItem(
        AUTH_STORAGE_KEY,
        JSON.stringify({ username, password })
    );
}

export function clearCredentials() {
    sessionStorage.removeItem(AUTH_STORAGE_KEY);
}

export function createBasicAuthHeader(username, password) {
    return `Basic ${btoa(`${username}:${password}`)}`;
}