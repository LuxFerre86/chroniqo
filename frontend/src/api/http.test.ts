import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, readCookie, request, setUnauthorizedHandler } from './http';

const fetchMock = vi.fn<typeof fetch>();

function json(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

function setXsrfCookie(value: string | null) {
  document.cookie = value === null ? 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/' : `XSRF-TOKEN=${value}; path=/`;
}

function lastInit(index = -1): RequestInit {
  const calls = fetchMock.mock.calls;
  return calls.at(index)![1] as RequestInit;
}

function headersOf(init: RequestInit): Record<string, string> {
  return init.headers as Record<string, string>;
}

beforeEach(() => {
  fetchMock.mockReset();
  vi.stubGlobal('fetch', fetchMock);
  setXsrfCookie(null);
});

afterEach(() => {
  setUnauthorizedHandler(null);
  vi.unstubAllGlobals();
  setXsrfCookie(null);
});

describe('readCookie', () => {
  it('returns null for unknown cookies', () => {
    expect(readCookie('does-not-exist')).toBeNull();
  });

  it('decodes cookie values', () => {
    document.cookie = 'XSRF-TOKEN=a%2Fb%3D; path=/';
    expect(readCookie('XSRF-TOKEN')).toBe('a/b=');
  });
});

describe('request – success paths', () => {
  it('sends GET requests without CSRF header and with same-origin credentials', async () => {
    setXsrfCookie('token-1');
    fetchMock.mockResolvedValueOnce(json(200, { ok: true }));

    const result = await request<{ ok: boolean }>('GET', '/api/me');

    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][0]).toBe('/api/me');
    const init = lastInit();
    expect(init.method).toBe('GET');
    expect(init.credentials).toBe('same-origin');
    expect(headersOf(init)['X-XSRF-TOKEN']).toBeUndefined();
    expect(headersOf(init).Accept).toBe('application/json');
  });

  it('builds query strings and skips undefined values', async () => {
    fetchMock.mockResolvedValueOnce(json(200, []));

    await request('GET', '/api/summary', { query: { from: '2026-01-01', to: '2026-01-31', extra: undefined } });

    expect(fetchMock.mock.calls[0][0]).toBe('/api/summary?from=2026-01-01&to=2026-01-31');
  });

  it('sends the XSRF cookie as header on mutations and serializes JSON bodies', async () => {
    setXsrfCookie('token-1');
    fetchMock.mockResolvedValueOnce(json(200, { id: 1 }));

    await request('PUT', '/api/me', { body: { firstName: 'Ada' } });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = lastInit();
    expect(init.method).toBe('PUT');
    expect(headersOf(init)['X-XSRF-TOKEN']).toBe('token-1');
    expect(headersOf(init)['Content-Type']).toBe('application/json');
    expect(init.body).toBe(JSON.stringify({ firstName: 'Ada' }));
  });

  it('sends form payloads url-encoded', async () => {
    setXsrfCookie('token-1');
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));

    await request('POST', '/api/auth/login', { form: { username: 'a@b.de', password: 'p w' } });

    const init = lastInit();
    expect(headersOf(init)['Content-Type']).toBe('application/x-www-form-urlencoded');
    expect(init.body).toBe('username=a%40b.de&password=p+w');
  });

  it('returns undefined for 204 responses', async () => {
    setXsrfCookie('token-1');
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(request('DELETE', '/api/time-entries/1')).resolves.toBeUndefined();
  });
});

describe('request – CSRF handling', () => {
  it('primes the cookie via GET /api/config before a mutation when it is missing', async () => {
    fetchMock.mockImplementationOnce(async () => {
      setXsrfCookie('fresh-token');
      return json(200, {});
    });
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));

    await request('POST', '/api/time-entries', { body: {} });

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(fetchMock.mock.calls[0][0]).toBe('/api/config');
    expect(fetchMock.mock.calls[1][0]).toBe('/api/time-entries');
    expect(headersOf(lastInit(1))['X-XSRF-TOKEN']).toBe('fresh-token');
  });

  it('does not prime the cookie for GET requests', async () => {
    fetchMock.mockResolvedValueOnce(json(200, {}));

    await request('GET', '/api/dashboard');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock.mock.calls[0][0]).toBe('/api/dashboard');
  });

  it('retries once with a fresh token on 403', async () => {
    setXsrfCookie('stale');
    fetchMock.mockResolvedValueOnce(json(403, { detail: 'Forbidden' }));
    fetchMock.mockImplementationOnce(async () => {
      setXsrfCookie('renewed');
      return json(200, {});
    });
    fetchMock.mockResolvedValueOnce(json(200, { saved: true }));

    const result = await request('PUT', '/api/absences', { body: {} });

    expect(result).toEqual({ saved: true });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(headersOf(lastInit(0))['X-XSRF-TOKEN']).toBe('stale');
    expect(fetchMock.mock.calls[1][0]).toBe('/api/config');
    expect(headersOf(lastInit(2))['X-XSRF-TOKEN']).toBe('renewed');
  });

  it('gives up after a single retry and reports the 403', async () => {
    setXsrfCookie('stale');
    fetchMock.mockResolvedValueOnce(json(403, { detail: 'Forbidden' }));
    fetchMock.mockResolvedValueOnce(json(200, {}));
    fetchMock.mockResolvedValueOnce(json(403, { detail: 'Still forbidden' }));

    await expect(request('POST', '/api/time-entries', { body: {} })).rejects.toMatchObject({
      status: 403,
      message: 'Still forbidden',
    });
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it('does not retry GET requests on 403', async () => {
    fetchMock.mockResolvedValueOnce(json(403, { detail: 'No access' }));

    await expect(request('GET', '/api/me')).rejects.toMatchObject({ status: 403 });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});

describe('request – error mapping', () => {
  beforeEach(() => setXsrfCookie('token-1'));

  it('maps problem+json responses to ApiError including code, field and errors', async () => {
    fetchMock.mockResolvedValueOnce(
      json(400, {
        detail: 'Validation failed',
        code: 'VALIDATION',
        field: 'email',
        errors: { email: 'Email is invalid', password: 'Too short' },
      }),
    );

    const error = await request('POST', '/api/auth/register', { body: {} }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(400);
    expect(apiError.message).toBe('Validation failed');
    expect(apiError.code).toBe('VALIDATION');
    expect(apiError.field).toBe('email');
    expect(apiError.errors).toEqual({ email: 'Email is invalid', password: 'Too short' });
  });

  it('falls back to a generic message for non-JSON error bodies', async () => {
    fetchMock.mockResolvedValueOnce(new Response('<html>oops</html>', { status: 500 }));

    await expect(request('POST', '/api/time-entries', { body: {} })).rejects.toMatchObject({
      status: 500,
      message: 'Request failed (500)',
      code: undefined,
    });
  });

  it('maps network failures to ApiError with status 0', async () => {
    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    const error = await request('GET', '/api/me').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 0,
      code: 'NETWORK',
      message: 'The server could not be reached. Please check your connection.',
    });
  });
});

describe('request – 401 handling', () => {
  beforeEach(() => setXsrfCookie('token-1'));

  it('calls the unauthorized handler and still throws', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    fetchMock.mockResolvedValueOnce(json(401, { detail: 'Unauthorized' }));

    await expect(request('GET', '/api/dashboard')).rejects.toMatchObject({ status: 401 });
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('skips the handler when suppressUnauthorizedHandler is set', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    fetchMock.mockResolvedValueOnce(json(401, { detail: 'Bad credentials' }));

    await expect(
      request('POST', '/api/auth/login', { form: { username: 'a', password: 'b' }, suppressUnauthorizedHandler: true }),
    ).rejects.toMatchObject({ status: 401, message: 'Bad credentials' });
    expect(handler).not.toHaveBeenCalled();
  });

  it('works without a registered handler', async () => {
    fetchMock.mockResolvedValueOnce(json(401, {}));

    await expect(request('GET', '/api/dashboard')).rejects.toMatchObject({ status: 401 });
  });

  it('does not call the handler for other error statuses', async () => {
    const handler = vi.fn();
    setUnauthorizedHandler(handler);
    fetchMock.mockResolvedValueOnce(json(400, { detail: 'Nope' }));

    await expect(request('GET', '/api/dashboard')).rejects.toMatchObject({ status: 400 });
    expect(handler).not.toHaveBeenCalled();
  });
});
