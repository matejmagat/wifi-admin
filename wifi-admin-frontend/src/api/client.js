import { createBasicAuthHeader } from '../utils/auth';

const API_BASE_URL = process.env.REACT_APP_API_BASE_URL;
const BASIC_AUTH_PASSWORD = process.env.REACT_APP_BASIC_AUTH_PASSWORD;
const BASIC_AUTH_USERNAME = process.env.REACT_APP_BASIC_AUTH_USERNAME;

const defaultHeaders = {
    Accept: 'application/json',
    Authorization: createBasicAuthHeader(
        BASIC_AUTH_USERNAME,
        BASIC_AUTH_PASSWORD
    ),
};

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
    const response = await fetch(`${API_BASE_URL}${path}`, {
        ...options,
        headers: {
            ...defaultHeaders,
            ...(options.body ? { 'Content-Type': 'application/json' } : {}),
            ...(options.headers || {}),
        },
    });

    return parseResponse(response);
}