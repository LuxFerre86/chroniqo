/**
 * Thin fetch wrapper for the Chroniqo REST API.
 *
 * - sends the session cookie (same-origin) and the CSRF token from the
 *   `XSRF-TOKEN` cookie as `X-XSRF-TOKEN` header on every request,
 * - maps RFC 9457 problem responses to {@link ApiError},
 * - reports 401 responses of protected endpoints through a global handler.
 */

import { getLanguage, translate } from '../i18n';

export class ApiError extends Error {
  readonly status: number;
  readonly code: string | undefined;
  readonly field: string | undefined;
  readonly errors: Record<string, string> | undefined;

  constructor(
    status: number,
    message: string,
    code?: string,
    field?: string,
    errors?: Record<string, string>,
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.field = field;
    this.errors = errors;
  }
}

type UnauthorizedHandler = () => void;
let unauthorizedHandler: UnauthorizedHandler | null = null;

/** Registers the callback invoked when a protected endpoint answers 401. */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler;
}

export function readCookie(name: string): string | null {
  const prefix = `${name}=`;
  for (const part of document.cookie.split(';')) {
    const trimmed = part.trim();
    if (trimmed.startsWith(prefix)) {
      return decodeURIComponent(trimmed.slice(prefix.length));
    }
  }
  return null;
}

interface RequestOptions {
  body?: unknown;
  form?: Record<string, string>;
  query?: Record<string, string | number | undefined>;
  /** 401 on this request is a normal outcome (e.g. login), not a session expiry. */
  suppressUnauthorizedHandler?: boolean;
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  if (!query) return path;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined) params.set(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${path}?${qs}` : path;
}

async function toError(response: Response): Promise<ApiError> {
  let detail = translate('error.requestFailed', { status: response.status });
  let code: string | undefined;
  let field: string | undefined;
  let errors: Record<string, string> | undefined;
  try {
    const data = await response.json();
    if (typeof data?.detail === 'string') detail = data.detail;
    code = data?.code;
    field = data?.field ?? undefined;
    errors = data?.errors;
  } catch {
    // Body is empty or not JSON – keep the generic message.
  }
  return new ApiError(response.status, detail, code, field, errors);
}

async function primeCsrfCookie(): Promise<void> {
  try {
    await fetch('/api/config', { credentials: 'same-origin', headers: { Accept: 'application/json' } });
  } catch {
    // The real request reports connectivity problems.
  }
}

export async function request<T = void>(
  method: 'GET' | 'POST' | 'PUT' | 'DELETE',
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const isMutation = method !== 'GET';
  // The XSRF cookie is (re)issued by any response; make sure we have one before mutating.
  if (isMutation && !readCookie('XSRF-TOKEN')) {
    await primeCsrfCookie();
  }

  let response = await send(method, path, options);
  if (response.status === 403 && isMutation) {
    // Stale CSRF token (e.g. after session rotation): fetch a fresh one and retry once.
    await primeCsrfCookie();
    response = await send(method, path, options);
  }

  if (!response.ok) {
    if (response.status === 401 && !options.suppressUnauthorizedHandler) {
      unauthorizedHandler?.();
    }
    throw await toError(response);
  }

  if (response.status === 204 || response.status === 202 || response.status === 201) {
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }
  return (await response.json()) as T;
}

async function send(method: string, path: string, options: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json', 'Accept-Language': getLanguage() };
  let body: BodyInit | undefined;

  if (options.form) {
    headers['Content-Type'] = 'application/x-www-form-urlencoded';
    body = new URLSearchParams(options.form).toString();
  } else if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(options.body);
  }

  if (method !== 'GET') {
    const token = readCookie('XSRF-TOKEN');
    if (token) headers['X-XSRF-TOKEN'] = token;
  }

  try {
    return await fetch(buildUrl(path, options.query), {
      method,
      headers,
      body,
      credentials: 'same-origin',
    });
  } catch {
    throw new ApiError(0, translate('error.network'), 'NETWORK');
  }
}

