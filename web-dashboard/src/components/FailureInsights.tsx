import { ArrowUpRight, Clock3, Filter, Gauge, Server, ShieldAlert } from "lucide-react";
import StatusCode from "./StatusCode";
import type { FailureEvent, FailureFilters } from "../types";
import { formatDateTime, formatDuration } from "../utils/format";

type FailureInsightsProps = {
  failures: FailureEvent[];
  filters: FailureFilters;
  isLoading: boolean;
  onApplyFilter: (filters: FailureFilters) => void;
  onOpenFailure: (id: number) => void;
};

type CountItem = {
  label: string;
  count: number;
};

function FailureInsights({ failures, filters, isLoading, onApplyFilter, onOpenFailure }: FailureInsightsProps) {
  const insights = getInsights(failures);

  return (
    <section className="space-y-3">
      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
        <InsightCard
          icon={<ShieldAlert size={18} />}
          label="Current failures"
          value={isLoading ? "..." : String(insights.total)}
          detail={`${insights.recentCount} in the last 24 hours`}
        />
        <InsightCard
          icon={<Gauge size={18} />}
          label="5xx rate"
          value={isLoading ? "..." : `${insights.serverErrorRate}%`}
          detail={`${insights.serverErrors} server errors in this view`}
        />
        <InsightCard
          icon={<Server size={18} />}
          label="Affected services"
          value={isLoading ? "..." : String(insights.serviceCount)}
          detail={insights.topService ? `${insights.topService.label} has ${insights.topService.count}` : "No services in view"}
        />
        <InsightCard
          icon={<Clock3 size={18} />}
          label="Average latency"
          value={isLoading ? "..." : formatDuration(insights.averageLatency)}
          detail={insights.slowest ? `Slowest: ${formatDuration(insights.slowest.latencyMs)}` : "Latency was not captured"}
        />
      </div>

      <div className="grid gap-3 lg:grid-cols-[1fr_340px]">
        <div className="rounded-md border border-line bg-white p-4">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-600">Quick triage filters</h3>
              <p className="mt-1 text-sm text-slate-600">Jump to the busiest services and status codes in the current result set.</p>
            </div>
            <div className="inline-flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500">
              <Filter size={14} />
              <span>{activeFilterCount(filters)} active</span>
            </div>
          </div>

          <div className="mt-4 flex flex-wrap gap-2">
            {isLoading ? (
              <span className="text-sm text-slate-500">Loading filter suggestions...</span>
            ) : insights.topServices.length === 0 && insights.statusCounts.length === 0 ? (
              <span className="text-sm text-slate-500">No suggestions available until failures load.</span>
            ) : (
              <>
                {insights.topServices.map((service) => {
                  const isActive = filters.serviceName?.trim() === service.label;
                  return (
                    <button
                      className={chipClassName(isActive)}
                      key={service.label}
                      onClick={() => onApplyFilter({ serviceName: isActive ? "" : service.label })}
                      type="button"
                    >
                      <Server size={14} />
                      <span>{service.label}</span>
                      <span className={countBadgeClassName(isActive)}>{service.count}</span>
                    </button>
                  );
                })}
                {insights.statusCounts.map((status) => {
                  const isActive = filters.statusCode?.trim() === status.label;
                  return (
                    <button
                      className={chipClassName(isActive)}
                      key={status.label}
                      onClick={() => onApplyFilter({ statusCode: isActive ? "" : status.label })}
                      type="button"
                    >
                      <StatusCode code={Number(status.label)} />
                      <span>{status.count}</span>
                    </button>
                  );
                })}
              </>
            )}
          </div>
        </div>

        <div className="rounded-md border border-line bg-white p-4">
          <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-600">Slowest failure</h3>
          {isLoading ? (
            <p className="mt-3 text-sm text-slate-500">Finding the slowest request...</p>
          ) : insights.slowest ? (
            <button
              className="mt-3 flex w-full items-start justify-between gap-3 rounded-md border border-line bg-slate-50 p-3 text-left transition hover:border-brand hover:bg-white"
              onClick={() => onOpenFailure(insights.slowest!.id)}
              type="button"
            >
              <span className="min-w-0">
                <span className="flex flex-wrap items-center gap-2">
                  <StatusCode code={insights.slowest.statusCode} />
                  <span className="font-mono text-xs text-slate-600">{insights.slowest.httpMethod}</span>
                  <span className="text-sm font-semibold text-ink">{formatDuration(insights.slowest.latencyMs)}</span>
                </span>
                <span className="mt-2 block truncate font-mono text-xs text-slate-700">{formatPath(insights.slowest)}</span>
                <span className="mt-2 block text-xs text-slate-500">{formatDateTime(insights.slowest.occurredAt)}</span>
              </span>
              <ArrowUpRight className="mt-0.5 shrink-0 text-brand" size={17} />
            </button>
          ) : (
            <p className="mt-3 text-sm text-slate-500">No latency values were captured for this result set.</p>
          )}
        </div>
      </div>
    </section>
  );
}

function InsightCard({ icon, label, value, detail }: { icon: JSX.Element; label: string; value: string; detail: string }) {
  return (
    <div className="rounded-md border border-line bg-white p-4">
      <div className="flex items-center justify-between gap-3">
        <div className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</div>
        <div className="flex h-8 w-8 items-center justify-center rounded-md bg-slate-100 text-brand">{icon}</div>
      </div>
      <div className="mt-3 text-2xl font-semibold text-ink">{value}</div>
      <div className="mt-1 truncate text-sm text-slate-600">{detail}</div>
    </div>
  );
}

function getInsights(failures: FailureEvent[]) {
  const total = failures.length;
  const serverErrors = failures.filter((failure) => failure.statusCode >= 500).length;
  const recentCount = failures.filter((failure) => isWithinLastDay(failure.occurredAt)).length;
  const topServices = countTop(failures.map((failure) => failure.serviceName), 4);
  const statusCounts = countTop(failures.map((failure) => String(failure.statusCode)), 5);
  const latencies = failures.filter((failure) => typeof failure.latencyMs === "number");
  const latencyTotal = latencies.reduce((sum, failure) => sum + (failure.latencyMs ?? 0), 0);
  const averageLatency = latencies.length > 0 ? Math.round(latencyTotal / latencies.length) : null;
  const slowest = latencies.reduce<FailureEvent | null>((current, failure) => {
    if (!current || (failure.latencyMs ?? -1) > (current.latencyMs ?? -1)) {
      return failure;
    }
    return current;
  }, null);

  return {
    total,
    serverErrors,
    serverErrorRate: total > 0 ? Math.round((serverErrors / total) * 100) : 0,
    recentCount,
    topServices,
    topService: topServices[0],
    statusCounts,
    serviceCount: new Set(failures.map((failure) => failure.serviceName)).size,
    averageLatency,
    slowest
  };
}

function countTop(values: string[], limit: number): CountItem[] {
  const counts = values.reduce<Map<string, number>>((current, value) => {
    const normalized = value.trim();
    if (!normalized) {
      return current;
    }
    current.set(normalized, (current.get(normalized) ?? 0) + 1);
    return current;
  }, new Map());

  return Array.from(counts.entries())
    .map(([label, count]) => ({ label, count }))
    .sort((left, right) => right.count - left.count || left.label.localeCompare(right.label))
    .slice(0, limit);
}

function isWithinLastDay(value: string) {
  const occurredAt = new Date(value).getTime();
  if (Number.isNaN(occurredAt)) {
    return false;
  }
  return Date.now() - occurredAt <= 24 * 60 * 60 * 1000;
}

function activeFilterCount(filters: FailureFilters) {
  return Object.values(filters).filter((value) => value?.trim()).length;
}

function chipClassName(isActive: boolean) {
  return [
    "inline-flex h-9 max-w-full items-center gap-2 rounded-md border px-3 text-sm font-semibold transition",
    isActive ? "border-brand bg-brand text-white" : "border-line bg-white text-ink hover:border-brand hover:bg-slate-50"
  ].join(" ");
}

function countBadgeClassName(isActive: boolean) {
  return [
    "rounded px-1.5 py-0.5 text-[11px]",
    isActive ? "bg-white/85 text-brand" : "bg-slate-100 text-slate-600"
  ].join(" ");
}

function formatPath(failure: FailureEvent) {
  return `${failure.path}${failure.queryString ? `?${failure.queryString}` : ""}`;
}

export default FailureInsights;
