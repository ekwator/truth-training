// Environment configuration
export const config = {
  API_BASE_URL: process.env.VITE_API_BASE || 'http://localhost:8080/api/v1',
  APP_NAME: process.env.VITE_APP_NAME || 'Truth Training',
  APP_VERSION: process.env.VITE_APP_VERSION || '0.1.0',
  DEBUG: process.env.VITE_DEBUG === 'true',
};
