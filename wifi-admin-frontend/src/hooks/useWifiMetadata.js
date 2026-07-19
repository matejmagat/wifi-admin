import { useEffect, useState } from 'react';
import { getEncryptionTypes, getWifiBandTypes } from '../api/wifiApi';

export function useWifiMetadata() {
    const [encryptionTypes, setEncryptionTypes] = useState([]);
    const [wifiBandTypes, setWifiBandTypes] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        async function loadMetadata() {
            setLoading(true);
            setError(null);

            try {
                const [encryptionData, wifiBandData] = await Promise.all([
                    getEncryptionTypes(),
                    getWifiBandTypes(),
                ]);

                setEncryptionTypes(encryptionData);
                setWifiBandTypes(wifiBandData);
            } catch (err) {
                setError(err);
            } finally {
                setLoading(false);
            }
        }

        loadMetadata();
    }, []);

    return {
        encryptionTypes,
        wifiBandTypes,
        loading,
        error,
    };
}