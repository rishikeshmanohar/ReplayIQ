import { Activity, DatabaseZap } from "lucide-react";
import type { ReactNode } from "react";
import { NavLink } from "react-router-dom";

type AppLayoutProps = {
  children: ReactNode;
};

function AppLayout({ children }: AppLayoutProps) {
  return (
    <main className="min-h-screen bg-surface text-ink">
      <header className="border-b border-line bg-white">
        <div className="mx-auto flex max-w-7xl flex-col gap-4 px-5 py-4 md:flex-row md:items-center md:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-md bg-brand text-white">
              <DatabaseZap size={20} />
            </div>
            <div>
              <div className="text-xs font-semibold uppercase tracking-wide text-brand">DebugFlow</div>
              <h1 className="text-xl font-semibold">Failure replay dashboard</h1>
            </div>
          </div>
          <nav className="flex items-center gap-2">
            <NavLink
              className={({ isActive }) => `nav-link ${isActive ? "nav-link-active" : ""}`}
              to="/failures"
            >
              <Activity size={17} />
              <span>Failures</span>
            </NavLink>
          </nav>
        </div>
      </header>
      <div className="mx-auto max-w-7xl px-5 py-5">{children}</div>
    </main>
  );
}

export default AppLayout;
