export class HttpError extends Error {
  constructor(status, body) { super(body.error ?? `HTTP ${status}`); this.status = status; this.body = body; }
}
export async function request(token, method, path, params = {}, base = '') {
  const options = { method, headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(10000) };
  if (method === 'POST') {
    options.headers['Content-Type'] = 'application/x-www-form-urlencoded';
    options.body = new URLSearchParams(params).toString();
  }
  const response = await fetch(`${base}${path}`, options);
  const body = await response.json();
  if (!response.ok) throw new HttpError(response.status, body);
  return { status: response.status, body };
}
