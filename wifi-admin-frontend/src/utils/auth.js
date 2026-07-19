export function createBasicAuthHeader(username, password) {
    return `Basic ${btoa(`${username}:${password}`)}`;
}