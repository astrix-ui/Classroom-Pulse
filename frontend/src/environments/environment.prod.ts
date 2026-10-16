export const environment = {
  production: true,
  // In production, fallback to relative host or deployment origin
  apiBaseUrl: typeof window !== 'undefined' ? `${window.location.origin}/api` : '/api',
  webSocketUrl: typeof window !== 'undefined' 
    ? `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws` 
    : 'wss://localhost:8080/ws'
};

