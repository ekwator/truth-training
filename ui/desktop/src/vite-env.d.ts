/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE?: string;
  readonly VITE_APP_NAME?: string;
  readonly VITE_APP_VERSION?: string;
  readonly VITE_DEBUG?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}


