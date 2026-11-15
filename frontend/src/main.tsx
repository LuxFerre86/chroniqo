import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { registerSW } from 'virtual:pwa-register';
import { App } from './App';
import { AuthProvider } from './auth/AuthContext';
import { ToastProvider } from './components/Toast';
import { ApiError } from './api/http';
import { I18nProvider } from './i18n';
import './styles/theme.css';
import './styles/components.css';
import './styles/layout.css';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // Replaces the former server push: data is refreshed when the app/tab regains focus.
      refetchOnWindowFocus: true,
      retry: (failureCount, error) => !(error instanceof ApiError && error.status >= 400 && error.status < 500) && failureCount < 2,
    },
  },
});

registerSW({ immediate: true });

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <I18nProvider>
            <ToastProvider>
              <App />
            </ToastProvider>
          </I18nProvider>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
