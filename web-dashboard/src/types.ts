export type HeaderValue = string | string[] | number | boolean | null;

export type HeaderMap = Record<string, HeaderValue>;

export type FailureEvent = {
  id: number;
  serviceName: string;
  environment: string | null;
  traceId: string | null;
  spanId: string | null;
  httpMethod: string;
  path: string;
  queryString: string | null;
  statusCode: number;
  latencyMs: number | null;
  exceptionType: string | null;
  exceptionMessage: string | null;
  stackTrace: string | null;
  requestHeaders: HeaderMap | null;
  requestBody: string | null;
  responseHeaders: HeaderMap | null;
  responseBody: string | null;
  occurredAt: string;
  createdAt: string;
  rootCauseAnalysis?: RootCauseAnalysisResponse | null;
  replayAttempts: ReplayAttempt[];
};

export type FailureFilters = {
  serviceName?: string;
  statusCode?: string;
  environment?: string;
  fromDate?: string;
  toDate?: string;
};

export type FailureAnalysis = {
  summary: string | null;
  likelyCause: string | null;
  suggestedFix: string | null;
  suggestedAction: string | null;
  confidence: number | null;
  analyzedAt: string | null;
};

export type RootCauseAnalysisResponse = {
  id?: number;
  failureId: number;
  summary: string;
  likelyCause?: string | null;
  suggestedFix?: string | null;
  suggestedAction?: string | null;
  confidence: number;
  createdAt?: string;
  analyzedAt?: string;
};

export type ReplayAttempt = {
  id: number;
  failureEventId: number;
  statusCode: number;
  responseHeaders: HeaderMap | null;
  responseBody: string | null;
  latencyMs: number | null;
  replayedAt: string;
};
