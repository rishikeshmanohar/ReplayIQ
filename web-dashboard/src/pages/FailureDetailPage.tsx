import { Activity, ArrowLeft, Play, RotateCcw } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getFailure, getFailureAnalysis, replayFailure, runFailureAnalysis } from "../api";
import StatusCode from "../components/StatusCode";
import type { FailureAnalysis, FailureEvent } from "../types";
import { formatDateTime, formatDuration, formatJson } from "../utils/format";

function FailureDetailPage() {
  const { id } = useParams();
  const [failure, setFailure] = useState<FailureEvent | null>(null);
  const [analysis, setAnalysis] = useState<FailureAnalysis | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isAnalysisLoading, setIsAnalysisLoading] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isReplaying, setIsReplaying] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [analysisError, setAnalysisError] = useState<string | null>(null);
  const [replayError, setReplayError] = useState<string | null>(null);
  const [targetBaseUrl, setTargetBaseUrl] = useState("http://localhost:8081");
  const [allowPaymentReplay, setAllowPaymentReplay] = useState(false);

  useEffect(() => {
    if (!id) {
      return;
    }
    void loadDetail(id);
  }, [id]);

  async function loadDetail(failureId: string) {
    setIsLoading(true);
    setError(null);
    setAnalysisError(null);
    setReplayError(null);
    try {
      setFailure(await getFailure(failureId));
      await loadAnalysis(failureId);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load failure");
    } finally {
      setIsLoading(false);
    }
  }

  async function replay(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!id) {
      return;
    }

    setIsReplaying(true);
    setReplayError(null);
    try {
      const attempt = await replayFailure(id, targetBaseUrl, allowPaymentReplay);
      setFailure((current) => current ? { ...current, replayAttempts: [attempt, ...(current.replayAttempts ?? [])] } : current);
    } catch (err) {
      setReplayError(err instanceof Error ? err.message : "Failed to replay failure");
    } finally {
      setIsReplaying(false);
    }
  }

  async function loadAnalysis(failureId: string) {
    setIsAnalysisLoading(true);
    try {
      setAnalysis(await getFailureAnalysis(failureId));
    } catch (err) {
      setAnalysis(null);
      setAnalysisError(err instanceof Error ? err.message : "Root cause analysis is unavailable");
    } finally {
      setIsAnalysisLoading(false);
    }
  }

  async function analyze() {
    if (!id) {
      return;
    }

    setIsAnalyzing(true);
    setAnalysisError(null);
    try {
      setAnalysis(await runFailureAnalysis(id));
    } catch (err) {
      setAnalysisError(err instanceof Error ? err.message : "Failed to run root cause analysis");
    } finally {
      setIsAnalyzing(false);
    }
  }

  if (isLoading) {
    return <PageState title="Loading failure..." />;
  }

  if (error || !failure) {
    return <PageState title="Could not load failure" detail={error ?? "Failure was not found"} />;
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
        <div>
          <Link className="inline-flex items-center gap-2 text-sm font-medium text-brand hover:text-[#0f4a5d]" to="/failures">
            <ArrowLeft size={16} />
            <span>Back to failures</span>
          </Link>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <StatusCode code={failure.statusCode} />
            <span className="rounded bg-slate-100 px-2 py-1 font-mono text-xs text-slate-700">{failure.httpMethod}</span>
            <span className="text-sm text-slate-600">{failure.serviceName}</span>
          </div>
          <h2 className="mt-2 break-words text-2xl font-semibold">{failure.path}</h2>
        </div>
        <div className="flex flex-wrap gap-2">
          <button className="icon-button" onClick={() => id && void loadDetail(id)} type="button">
            <RotateCcw size={17} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      <section className="rounded-md border border-line bg-white">
        <div className="border-b border-line px-4 py-3">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-600">Request info</h3>
        </div>
        <div className="grid gap-4 p-4 md:grid-cols-2 xl:grid-cols-4">
          <Info label="Occurred" value={formatDateTime(failure.occurredAt)} />
          <Info label="Environment" value={failure.environment ?? "Not captured"} />
          <Info label="Latency" value={formatDuration(failure.latencyMs)} />
          <Info label="Trace ID" value={failure.traceId ?? "Not captured"} />
          <Info label="Span ID" value={failure.spanId ?? "Not captured"} />
          <Info label="Query string" value={failure.queryString ?? "None"} />
          <Info label="Exception" value={failure.exceptionType ?? "None captured"} />
          <Info label="Message" value={failure.exceptionMessage ?? "None captured"} />
        </div>
      </section>

      <div className="grid gap-5 xl:grid-cols-[1fr_380px]">
        <section className="space-y-5">
          <PayloadSection title="Request headers" value={failure.requestHeaders} />
          <PayloadSection title="Request body" value={failure.requestBody} />
          <PayloadSection title="Response headers" value={failure.responseHeaders} />
          <PayloadSection title="Response body" value={failure.responseBody} />
          <PayloadSection title="Stack trace" value={failure.stackTrace} minHeight="min-h-[240px]" />
        </section>

        <aside className="space-y-5">
          <section className="rounded-md border border-line bg-white">
            <div className="flex items-center justify-between gap-3 border-b border-line px-4 py-3">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-600">Root cause analysis</h3>
              <button className="primary-button h-9" onClick={analyze} disabled={isAnalyzing} type="button">
                <Activity size={16} />
                <span>{isAnalyzing ? "Running" : "Analyze"}</span>
              </button>
            </div>
            <div className="space-y-4 p-4">
              {isAnalysisLoading ? (
                <p className="text-sm text-slate-600">Loading analysis...</p>
              ) : analysis ? (
                <>
                  <AnalysisBlock label="Summary" value={analysis.summary} />
                  <AnalysisBlock label="Likely cause" value={analysis.likelyCause} />
                  <AnalysisBlock label="Suggested fix" value={analysis.suggestedFix ?? analysis.suggestedAction} />
                  <div className="grid grid-cols-2 gap-3">
                    <Info label="Confidence" value={analysis.confidence == null ? "Not scored" : `${Math.round(analysis.confidence * 100)}%`} />
                    <Info label="Analyzed" value={formatDateTime(analysis.analyzedAt)} />
                  </div>
                </>
              ) : (
                <p className="text-sm leading-6 text-slate-600">No root cause analysis has been generated for this failure.</p>
              )}
              {analysisError ? <p className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800">{analysisError}</p> : null}
            </div>
          </section>

          <section className="rounded-md border border-line bg-white">
            <div className="border-b border-line px-4 py-3">
              <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-600">Safe replay</h3>
            </div>
            <form className="space-y-4 p-4" onSubmit={replay}>
              <label className="field-label block">
                <span>Target base URL</span>
                <input
                  className="field-input"
                  value={targetBaseUrl}
                  onChange={(event) => setTargetBaseUrl(event.target.value)}
                  placeholder="http://localhost:8081"
                  required
                  type="url"
                />
              </label>
              <label className="flex items-start gap-2 text-sm font-medium text-slate-700">
                <input
                  className="mt-1 h-4 w-4 rounded border-line text-brand focus:ring-brand"
                  checked={allowPaymentReplay}
                  onChange={(event) => setAllowPaymentReplay(event.target.checked)}
                  type="checkbox"
                />
                <span>Allow payment path replay</span>
              </label>
              <button className="primary-button w-full" disabled={isReplaying} type="submit">
                <Play size={16} />
                <span>{isReplaying ? "Replaying" : "Replay"}</span>
              </button>
              {replayError ? <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{replayError}</p> : null}
            </form>
            <div className="border-t border-line px-4 py-3">
              <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">Replay attempts</h4>
              <div className="mt-3 space-y-3">
                {(failure.replayAttempts ?? []).length === 0 ? (
                  <p className="text-sm text-slate-600">No replay attempts yet.</p>
                ) : (
                  failure.replayAttempts.map((attempt) => (
                    <div className="rounded-md border border-line bg-slate-50 p-3" key={attempt.id}>
                      <div className="flex items-center justify-between gap-2">
                        <StatusCode code={attempt.statusCode} />
                        <span className="text-xs text-slate-500">{formatDateTime(attempt.replayedAt)}</span>
                      </div>
                      <div className="mt-2 grid grid-cols-2 gap-2 text-sm">
                        <ReplayMeta label="Latency" value={formatDuration(attempt.latencyMs)} />
                        <ReplayMeta label="Attempt" value={`#${attempt.id}`} />
                      </div>
                      <pre className="mt-3 max-h-40 overflow-auto rounded bg-[#101820] p-3 text-xs leading-5 text-slate-100">
                        {formatJson(attempt.responseBody)}
                      </pre>
                    </div>
                  ))
                )}
              </div>
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0 rounded-md border border-line bg-slate-50 p-3">
      <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-1 break-words text-sm font-medium text-ink">{value}</div>
    </div>
  );
}

function PayloadSection({ title, value, minHeight = "min-h-[150px]" }: { title: string; value: unknown; minHeight?: string }) {
  return (
    <section className="rounded-md border border-line bg-white">
      <div className="border-b border-line px-4 py-3">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-600">{title}</h3>
      </div>
      <pre className={`${minHeight} overflow-auto bg-[#101820] p-4 text-sm leading-6 text-slate-100`}>
        {formatJson(value)}
      </pre>
    </section>
  );
}

function AnalysisBlock({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div>
      <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</div>
      <p className="mt-1 text-sm leading-6 text-slate-700">{value || "Not available"}</p>
    </div>
  );
}

function ReplayMeta({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-0.5 break-words font-medium text-ink">{value}</div>
    </div>
  );
}

function PageState({ title, detail }: { title: string; detail?: string }) {
  return (
    <div className="rounded-md border border-line bg-white px-5 py-12 text-center">
      <h2 className="text-lg font-semibold">{title}</h2>
      {detail ? <p className="mt-2 text-sm text-slate-600">{detail}</p> : null}
    </div>
  );
}

export default FailureDetailPage;
