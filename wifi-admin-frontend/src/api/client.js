import {
    createBasicAuthHeader,
    getStoredCredentials,
} from '../utils/auth';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL ?? '/api';

async function parseResponse(response) {
    const contentType = response.headers.get('content-type') || '';
    const data = contentType.includes('application/json')
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        const error = new Error(
            typeof data === 'object' && data?.message
                ? data.message
                : response.status === 401
                    ? 'Unauthorized. Check frontend Basic Auth credentials.'
                    : 'Request failed'
        );

        error.status = response.status;
        error.code = typeof data === 'object' ? data?.code : undefined;
        error.payload = data;
        throw error;
    }

    return data;
}

export async function apiFetch(path, options = {}) {
    // Read the latest credentials for every request.
    // This means credentials saved by the login form are used immediately.
    const credentials = getStoredCredentials();

    const authorizationHeader = credentials
        ? {
            Authorization: createBasicAuthHeader(
                credentials.username,
                credentials.password
            ),
        }
        : {};

    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers: {
            Accept: 'application/json',
            ...authorizationHeader,
            ...(options.body
                ? { 'Content-Type': 'application/json' }
                : {}),
            ...(options.headers || {}),
        },
    });

    return parseResponse(response);
}