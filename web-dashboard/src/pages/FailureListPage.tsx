import { AlertTriangle, RotateCcw, Search } from "lucide-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { listFailures } from "../api";
import FailureInsights from "../components/FailureInsights";
import StatusCode from "../components/StatusCode";
import type { FailureEvent, FailureFilters } from "../types";
import { formatDateTime, formatDuration } from "../utils/format";

function FailureListPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => filtersFromSearchParams(searchParams), [searchParams]);
  const [draftFilters, setDraftFilters] = useState<FailureFilters>(filters);
  const [failures, setFailures] = useState<FailureEvent[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setDraftFilters(filters);
    void loadFailures(filters);
  }, [filters]);

  async function loadFailures(nextFilters: FailureFilters) {
    setIsLoading(true);
    setError(null);
    try {
      setFailures(await listFailures(nextFilters));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load failures");
    } finally {
      setIsLoading(false);
    }
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSearchParams(searchParamsFromFilters(draftFilters));
  }

  function resetFilters() {
    setDraftFilters({});
    setSearchParams({});
  }

  function applyInsightFilters(filterPatch: FailureFilters) {
    const nextFilters = { ...filters, ...filterPatch };
    setDraftFilters(nextFilters);
    setSearchParams(searchParamsFromFilters(nextFilters));
  }

  return (
    <div className="space-y-5">
      <FailureInsights
        failures={failures}
        filters={filters}
        isLoading={isLoading}
        onApplyFilter={applyInsightFilters}
        onOpenFailure={(failureId) => navigate(`/failures/${failureId}`)}
      />

      <section className="rounded-md border border-line bg-white">
        <div className="flex flex-col gap-3 border-b border-line px-4 py-4 md:flex-row md:items-center md:justify-between">
          <div>
            <h2 className="text-lg font-semibold">Failed API requests</h2>
            <p className="mt-1 text-sm text-slate-600">Captured 5xx responses and thrown exceptions from SDK-enabled services.</p>
          </div>
          <button className="icon-button" onClick={() => void loadFailures(filters)} type="button">
            <RotateCcw size={17} />
            <span>Refresh</span>
          </button>
        </div>

        <form className="grid gap-3 border-b border-line bg-slate-50 px-4 py-4 md:grid-cols-[1fr_130px_1fr_190px_190px_auto]" onSubmit={applyFilters}>
          <label className="field-label">
            <span>Service</span>
            <input
              className="field-input"
              value={draftFilters.serviceName ?? ""}
              onChange={(event) => setDraftFilters((current) => ({ ...current, serviceName: event.target.value }))}
              placeholder="checkout-service"
            />
          </label>
          <label className="field-label">
            <span>Status</span>
            <input
              className="field-input"
              inputMode="numeric"
              value={draftFilters.statusCode ?? ""}
              onChange={(event) => setDraftFilters((current) => ({ ...current, statusCode: event.target.value }))}
              placeholder="500"
            />
          </label>
          <label className="field-label">
            <span>Environment</span>
            <input
              className="field-input"
              value={draftFilters.environment ?? ""}
              onChange={(event) => setDraftFilters((current) => ({ ...current, environment: event.target.value }))}
              placeholder="local"
            />
          </label>
          <label className="field-label">
            <span>From</span>
            <input
              className="field-input"
              type="datetime-local"
              value={draftFilters.fromDate ?? ""}
              onChange={(event) => setDraftFilters((current) => ({ ...current, fromDate: event.target.value }))}
            />
          </label>
          <label className="field-label">
            <span>To</span>
            <input
              className="field-input"
              type="datetime-local"
              value={draftFilters.toDate ?? ""}
              onChange={(event) => setDraftFilters((current) => ({ ...current, toDate: event.target.value }))}
            />
          </label>
          <div className="flex items-end gap-2">
            <button className="primary-button" type="submit">
              <Search size={17} />
              <span>Filter</span>
            </button>
            <button className="icon-button" type="button" onClick={resetFilters}>
              <span>Reset</span>
            </button>
          </div>
        </form>

        {error ? (
          <div className="flex items-center gap-2 border-b border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            <AlertTriangle size={17} />
            <span>{error}</span>
          </div>
        ) : null}

        <div className="overflow-x-auto">
          <table className="min-w-[1080px] table-fixed text-left text-sm">
            <thead className="border-b border-line bg-white text-xs font-semibold uppercase tracking-wide text-slate-500">
              <tr>
                <th className="w-[190px] px-4 py-3">Occurred</th>
                <th className="w-[170px] px-4 py-3">Service</th>
                <th className="w-[130px] px-4 py-3">Environment</th>
                <th className="w-[100px] px-4 py-3">Method</th>
                <th className="w-[260px] px-4 py-3">Path</th>
                <th className="w-[110px] px-4 py-3">Status</th>
                <th className="w-[110px] px-4 py-3">Latency</th>
                <th className="w-[220px] px-4 py-3">Exception</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {isLoading ? (
                <tr>
                  <td className="px-4 py-10 text-center text-slate-500" colSpan={8}>
                    Loading failures...
                  </td>
                </tr>
              ) : failures.length === 0 ? (
                <tr>
                  <td className="px-4 py-10 text-center text-slate-500" colSpan={8}>
                    No failures match the current filters.
                  </td>
                </tr>
              ) : (
                failures.map((failure) => (
                  <tr
                    className="cursor-pointer bg-white transition hover:bg-slate-50"
                    key={failure.id}
                    onClick={() => navigate(`/failures/${failure.id}`)}
                    onKeyDown={(event) => {
                      if (event.key === "Enter") {
                        navigate(`/failures/${failure.id}`);
                      }
                    }}
                    tabIndex={0}
                  >
                    <td className="px-4 py-3 text-slate-700">{formatDateTime(failure.occurredAt)}</td>
                    <td className="truncate px-4 py-3 font-medium">{failure.serviceName}</td>
                    <td className="truncate px-4 py-3 text-slate-600">{failure.environment ?? "Not captured"}</td>
                    <td className="px-4 py-3 font-mono text-xs">{failure.httpMethod}</td>
                    <td className="truncate px-4 py-3 font-mono text-xs text-slate-700">{formatPath(failure)}</td>
                    <td className="px-4 py-3">
                      <StatusCode code={failure.statusCode} />
                    </td>
                    <td className="px-4 py-3 text-slate-700">{formatDuration(failure.latencyMs)}</td>
                    <td className="truncate px-4 py-3 text-slate-600">{failure.exceptionType ?? "None captured"}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>
    </div>
  );
}

function filtersFromSearchParams(params: URLSearchParams): FailureFilters {
  return {
    serviceName: params.get("serviceName") ?? "",
    statusCode: params.get("statusCode") ?? "",
    environment: params.get("environment") ?? "",
    fromDate: params.get("fromDate") ?? "",
    toDate: params.get("toDate") ?? ""
  };
}

function searchParamsFromFilters(filters: FailureFilters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value && value.trim()) {
      params.set(key, value.trim());
    }
  });
  return params;
}

function formatPath(failure: FailureEvent) {
  return `${failure.path}${failure.queryString ? `?${failure.queryString}` : ""}`;
}

export default FailureListPage;
