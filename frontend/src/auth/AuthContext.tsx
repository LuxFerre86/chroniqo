import { createContext, useCallback, useContext, useEffect, useMemo } from 'react';
import type { ReactNode } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError, setUnauthorizedHandler } from '../api/http';
import type { UserProfile } from '../api/types';
import { queryKeys } from '../lib/queries';

interface AuthContextValue {
  /** The signed-in user, or null when anonymous. Undefined while loading. */
  user: UserProfile | null | undefined;
  isLoading: boolean;
  login: (email: string, password: string, rememberMe: boolean) => Promise<void>;
  logout: () => Promise<void>;
  /** Re-reads the profile from the server (e.g. after verification auto-login or profile changes). */
  refreshUser: () => Promise<UserProfile | null>;
  /** Drops all cached data and marks the session as ended (after logout/account deletion). */
  clearSession: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

async function fetchCurrentUser(): Promise<UserProfile | null> {
  try {
    return await api.getMe();
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    throw error;
  }
}

// 'me' must survive removeQueries: the mounted observer would keep pointing at the removed query
// and never see the following setQueryData/fetchQuery result.
const PERSISTENT_KEYS = new Set(['me', 'config', 'countries', 'subdivisions']);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();

  const { data: user, isLoading } = useQuery({
    queryKey: queryKeys.me,
    queryFn: fetchCurrentUser,
    staleTime: 60_000,
    retry: false,
  });

  const clearSession = useCallback(() => {
    queryClient.removeQueries({ predicate: (q) => !PERSISTENT_KEYS.has(String(q.queryKey[0])) });
    queryClient.setQueryData(queryKeys.me, null);
  }, [queryClient]);

  useEffect(() => {
    setUnauthorizedHandler(clearSession);
    return () => setUnauthorizedHandler(null);
  }, [clearSession]);

  const refreshUser = useCallback(async () => {
    const profile = await queryClient.fetchQuery({ queryKey: queryKeys.me, queryFn: fetchCurrentUser, staleTime: 0 });
    return profile;
  }, [queryClient]);

  const login = useCallback(
    async (email: string, password: string, rememberMe: boolean) => {
      await api.login(email, password, rememberMe);
      queryClient.removeQueries({ predicate: (q) => !PERSISTENT_KEYS.has(String(q.queryKey[0])) });
      await refreshUser();
    },
    [queryClient, refreshUser],
  );

  const logout = useCallback(async () => {
    try {
      await api.logout();
    } finally {
      clearSession();
    }
  }, [clearSession]);

  const value = useMemo<AuthContextValue>(
    () => ({ user, isLoading, login, logout, refreshUser, clearSession }),
    [user, isLoading, login, logout, refreshUser, clearSession],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
