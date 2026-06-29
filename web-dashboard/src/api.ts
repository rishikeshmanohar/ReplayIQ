import type { FailureAnalysis, FailureEvent, FailureFilters, ReplayAttempt, RootCauseAnalysisResponse } from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
const API_USERNAME = import.meta.env.VITE_API_USERNAME ?? "failframe";
const API_PASSWORD = import.meta.env.VITE_API_PASSWORD ?? "failframe";

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options.headers
    }
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }

  return response.json() as Promise<T>;
}

async function errorMessage(response: Response) {
  try {
    const payload = await response.clone().json() as { detail?: string; title?: string; message?: string };
    return payload.detail || payload.message || payload.title || `FailFrame API returned ${response.status}`;
  } catch {
    return `FailFrame API returned ${response.status}`;
  }
}

function basicAuthHeaders(): HeadersInit {
  return {
    Authorization: `Basic ${window.btoa(`${API_USERNAME}:${API_PASSWORD}`)}`
  };
}

export function listFailures(filters: FailureFilters): Promise<FailureEvent[]> {
  const params = new URLSearchParams();
  addParam(params, "serviceName", filters.serviceName);
  addParam(params, "statusCode", filters.statusCode);
  addParam(params, "environment", filters.environment);
  addParam(params, "fromDate", toIsoDate(filters.fromDate));
  addParam(params, "toDate", toIsoDate(filters.toDate));

  const query = params.toString();
  return request<FailureEvent[]>(`/api/v1/events/failures${query ? `?${query}` : ""}`, {
    headers: basicAuthHeaders()
  });
}

export function getFailure(id: string): Promise<FailureEvent> {
  return request<FailureEvent>(`/api/v1/events/failures/${id}`, {
    headers: basicAuthHeaders()
  });
}

export function replayFailure(id: string, targetBaseUrl: string, allowPaymentReplay: boolean): Promise<ReplayAttempt> {
  return request<ReplayAttempt>(`/api/v1/events/failures/${id}/replay`, {
    method: "POST",
    headers: basicAuthHeaders(),
    body: JSON.stringify({
      targetBaseUrl,
      allowPaymentReplay
    })
  });
}

export async function getFailureAnalysis(id: string): Promise<FailureAnalysis | null> {
  const response = await request<LegacyFailureDetail>(`/api/failures/${id}`, {
    headers: basicAuthHeaders()
  });

  if (!response.analysisSummary && !response.likelyCause && !response.suggestedFix && !response.suggestedAction) {
    return null;
  }

  return {
    summary: response.analysisSummary,
    likelyCause: response.likelyCause,
    suggestedFix: response.suggestedFix,
    suggestedAction: response.suggestedAction,
    confidence: response.confidence,
    analyzedAt: response.analyzedAt
  };
}

export async function runFailureAnalysis(id: string): Promise<FailureAnalysis> {
  const response = await request<RootCauseAnalysisResponse>(`/api/failures/${id}/analysis`, {
    method: "POST",
    headers: basicAuthHeaders()
  });

  return {
    summary: response.summary,
    likelyCause: response.likelyCause ?? null,
    suggestedFix: response.suggestedFix ?? null,
    suggestedAction: response.suggestedAction ?? response.suggestedFix ?? null,
    confidence: response.confidence,
    analyzedAt: response.analyzedAt ?? response.createdAt ?? null
  };
}

function addParam(params: URLSearchParams, key: string, value?: string | null) {
  if (value && value.trim()) {
    params.set(key, value.trim());
  }
}

function toIsoDate(value?: string) {
  if (!value) {
    return undefined;
  }
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toISOString();
}

type LegacyFailureDetail = {
  analysisSummary: string | null;
  likelyCause: string | null;
  suggestedFix: string | null;
  suggestedAction: string | null;
  confidence: number | null;
  analyzedAt: string | null;
};
