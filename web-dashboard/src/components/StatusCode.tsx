type StatusCodeProps = {
  code: number;
};

function StatusCode({ code }: StatusCodeProps) {
  const className =
    code >= 500
      ? "bg-red-100 text-red-700"
      : code >= 400
        ? "bg-amber-100 text-amber-800"
        : "bg-slate-100 text-slate-700";

  return <span className={`inline-flex rounded px-2 py-1 text-xs font-semibold ${className}`}>{code}</span>;
}

export default StatusCode;
