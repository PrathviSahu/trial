import { API_CONFIG, apiUrl } from '../config/api';

export class HttpTimeoutError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'HttpTimeoutError';
  }
}

export async function fetchJson<T>(path: string, init: RequestInit = {}, timeoutMs: number = API_CONFIG.TIMEOUT): Promise<T> {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);

  try {
    const response = await fetch(apiUrl(path), {
      ...init,
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    return await response.json() as T;
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new HttpTimeoutError('The server took too long to respond. It may still be waking up on Render.');
    }

    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
}
