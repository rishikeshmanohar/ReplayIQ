import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#17212b",
        surface: "#f6f7f9",
        line: "#d8dee6",
        brand: "#145c72",
        accent: "#b85c38"
      }
    }
  },
  plugins: []
} satisfies Config;
