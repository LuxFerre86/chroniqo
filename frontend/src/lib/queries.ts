import { useQuery, type QueryClient } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { DEFAULT_PASSWORD_POLICY } from './password';
import type { AppConfig } from '../api/types';

export const queryKeys = {
  me: ['me'] as const,
  config: ['config'] as const,
  countries: ['countries'] as const,
  subdivisions: (code: string) => ['subdivisions', code] as const,
  dashboard: ['dashboard'] as const,
  summary: (from: string, to: string) => ['summary', from, to] as const,
  timeEntries: (from: string, to: string) => ['time-entries', from, to] as const,
  absences: (from: string, to: string) => ['absences', from, to] as const,
  vacation: (year: number) => ['vacation', year] as const,
};

/** Call after any mutation of time entries, absences or the user profile. */
export function invalidateTimeData(queryClient: QueryClient): Promise<unknown> {
  return Promise.all([
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
    queryClient.invalidateQueries({ queryKey: ['summary'] }),
    queryClient.invalidateQueries({ queryKey: ['time-entries'] }),
    queryClient.invalidateQueries({ queryKey: ['absences'] }),
    queryClient.invalidateQueries({ queryKey: ['vacation'] }),
  ]);
}

const FALLBACK_CONFIG: AppConfig = {
  appName: 'chroniqo',
  version: null,
  registrationEnabled: false,
  passwordPolicy: DEFAULT_PASSWORD_POLICY,
};

/** Public application configuration; falls back to defaults while loading or on failure. */
export function useAppConfig(): AppConfig {
  const { data } = useQuery({
    queryKey: queryKeys.config,
    queryFn: api.getConfig,
    staleTime: Infinity,
  });
  return data ?? FALLBACK_CONFIG;
}

export function useCountries() {
  return useQuery({ queryKey: queryKeys.countries, queryFn: api.getCountries, staleTime: Infinity });
}

export function useSubdivisions(countryCode: string | null) {
  return useQuery({
    queryKey: queryKeys.subdivisions(countryCode ?? ''),
    queryFn: () => api.getSubdivisions(countryCode as string),
    enabled: !!countryCode,
    staleTime: Infinity,
  });
}
