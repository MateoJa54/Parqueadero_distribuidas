/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_USUARIOS: string;
  readonly VITE_API_ZONAS: string;
  readonly VITE_API_ASIGNACIONES: string;
  readonly VITE_API_TICKETS: string;
  readonly VITE_API_VEHICULOS: string;
  readonly VITE_API_AUDIT: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
