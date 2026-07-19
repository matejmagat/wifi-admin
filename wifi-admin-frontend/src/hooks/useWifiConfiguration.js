import { useState } from 'react';
import {
    getWifiConfiguration,
    updateWifiConfiguration,
} from '../api/wifiApi';

export function useWifiConfiguration() {
    const [data, setData] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    async function fetchConfig(cpeId) {
        setLoading(true);
        setError(null);

        try {
            const result = await getWifiConfiguration(cpeId);
            setData(result);
            return result;
        } catch (err) {
            setError(err);
            throw err;
        } finally {
            setLoading(false);
        }
    }

    async function saveConfig(config) {
        setLoading(true);
        setError(null);

        try {
            const result = await updateWifiConfiguration(config);
            setData(result);
            return result;
        } catch (err) {
            setError(err);
            throw err;
        } finally {
            setLoading(false);
        }
    }

    return {
        data,
        error,
        loading,
        fetchConfig,
        saveConfig,
    };
}