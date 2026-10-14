import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  preview: {
    port: 4173,
    strictPort: true,
  },
  build: {
    assetsInlineLimit: 4096,
    rollupOptions: {
      output: {
        entryFileNames: "assets/[name].js",
        chunkFileNames: "assets/[name]-[hash].js",
        assetFileNames: (asset) => asset.names.some((name) => name.endsWith(".css")) ? "assets/index.css" : "assets/[name]-[hash][extname]",
      },
    },
  },
});
