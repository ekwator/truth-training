import React from 'react';
import { ErrorBoundary } from '@/components/system/ErrorBoundary';
import { ToastProvider } from '@/components/system/Toaster';
import { ThemeProvider } from '@/components/system/ThemeProvider';
import { Dashboard } from '@/pages/Dashboard';

export const App: React.FC = () => {
  return (
    <ErrorBoundary>
      <ThemeProvider>
        <ToastProvider>
          <Dashboard />
        </ToastProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
};
