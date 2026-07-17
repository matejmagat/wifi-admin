const BASE_URL = 'http://localhost:8081';

async function parseResponse(response) {
    const contentType = response.headers.get('content-type') || '';
    const data = contentType.includes('application/json')
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        const error = new Error(
            typeof data === 'object' && data?.message
                ? data.message
                : 'Request failed'
        );

        error.status = response.status;
        error.code = typeof data === 'object' ? data?.code : undefined;
        error.payload = data;
        throw error;
    }

    return data;
}

export async function getWifiConfiguration(cpeId) {
    const response = await fetch(
        `${BASE_URL}/wifi-parameter/${encodeURIComponent(cpeId)}`,
        {
            method: 'GET',
            headers: {
                Accept: 'application/json',
            },
        }
    );

    return parseResponse(response);
}

export async function updateWifiConfiguration(config) {
    const response = await fetch(`${BASE_URL}/wifi-parameter`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            Accept: 'application/json',
        },
        body: JSON.stringify(config),
    });

    return parseResponse(response);
}

export async function getEncryptionTypes() {
    const response = await fetch('http://localhost:8081/wifi-parameter/encryption-types');
    if (!response.ok) throw new Error('Failed to fetch encryption types');
    return response.json();
}

export async function getWifiBandTypes() {
    const response = await fetch('http://localhost:8081/wifi-parameter/wifi-band-types');
    if (!response.ok) throw new Error('Failed to fetch Wi-Fi band types');
    return response.json();
}