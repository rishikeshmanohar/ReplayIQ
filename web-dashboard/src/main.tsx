import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import "./styles.css";

const routerBasename = normalizeBasename(import.meta.env.VITE_ROUTER_BASENAME);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter basename={routerBasename}>
      <App />
    </BrowserRouter>
  </React.StrictMode>
);

function normalizeBasename(value: string | undefined) {
  if (!value || value === "/") {
    return "/";
  }
  return value.endsWith("/") ? value.slice(0, -1) : value;
}
