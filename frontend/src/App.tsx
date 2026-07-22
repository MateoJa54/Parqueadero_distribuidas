import { Route, Routes } from 'react-router-dom';
import { RequireAuth, RequirePermiso, HomeRedirect } from '@/auth/guards';
import { AppLayout } from '@/layout/AppLayout';
import { PortalLayout } from '@/layout/PortalLayout';

import { LoginPage } from '@/pages/auth/LoginPage';
import { RegistroPage } from '@/pages/auth/RegistroPage';
import { ForbiddenPage, NotFoundPage } from '@/pages/ErrorPages';

import { DashboardPage } from '@/pages/admin/DashboardPage';
import { PersonasPage } from '@/pages/admin/PersonasPage';
import { UsuariosPage } from '@/pages/admin/UsuariosPage';
import { RolesPage } from '@/pages/admin/RolesPage';
import { AsignacionRolesPage } from '@/pages/admin/AsignacionRolesPage';
import { ZonasPage } from '@/pages/admin/ZonasPage';
import { EspaciosPage } from '@/pages/admin/EspaciosPage';
import { VehiculosPage } from '@/pages/admin/VehiculosPage';
import { AsignacionesVehiculosPage } from '@/pages/admin/AsignacionesVehiculosPage';
import { TicketsPage } from '@/pages/admin/TicketsPage';
import { AuditoriaPage } from '@/pages/admin/AuditoriaPage';
import { ConfiguracionPage } from '@/pages/admin/ConfiguracionPage';
import { DiagnosticoPage } from '@/pages/admin/DiagnosticoPage';

import { PerfilPage } from '@/pages/portal/PerfilPage';
import { MisVehiculosPage } from '@/pages/portal/MisVehiculosPage';
import { MisTicketsPage } from '@/pages/portal/MisTicketsPage';
import { DisponibilidadPage } from '@/pages/portal/DisponibilidadPage';

export function App() {
  return (
    <Routes>
      {/* Públicas */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/registro" element={<RegistroPage />} />
      <Route path="/403" element={<ForbiddenPage />} />

      {/* Raíz → home según rol */}
      <Route path="/" element={<HomeRedirect />} />

      {/* Panel de gestión (staff). Cada rama exige el permiso concreto. */}
      <Route element={<RequireAuth />}>
        <Route path="/app" element={<AppLayout />}>
          <Route element={<RequirePermiso permiso="dashboard" />}>
            <Route index element={<DashboardPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="personas" />}>
            <Route path="personas" element={<PersonasPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="usuarios" />}>
            <Route path="usuarios" element={<UsuariosPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="roles" />}>
            <Route path="roles" element={<RolesPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="asignacion-roles" />}>
            <Route path="asignacion-roles" element={<AsignacionRolesPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="zonas" />}>
            <Route path="zonas" element={<ZonasPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="espacios" />}>
            <Route path="espacios" element={<EspaciosPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="vehiculos" />}>
            <Route path="vehiculos" element={<VehiculosPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="asignaciones-vehiculos" />}>
            <Route path="asignaciones-vehiculos" element={<AsignacionesVehiculosPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="tickets:ver" />}>
            <Route path="tickets" element={<TicketsPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="auditoria" />}>
            <Route path="auditoria" element={<AuditoriaPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="configuracion" />}>
            <Route path="configuracion" element={<ConfiguracionPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="diagnostico" />}>
            <Route path="diagnostico" element={<DiagnosticoPage />} />
          </Route>
        </Route>
      </Route>

      {/* Portal cliente. Disponibilidad es la landing del portal. */}
      <Route element={<RequireAuth />}>
        <Route path="/portal" element={<PortalLayout />}>
          <Route element={<RequirePermiso permiso="portal:perfil" />}>
            <Route index element={<PerfilPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="portal:mis-vehiculos" />}>
            <Route path="vehiculos" element={<MisVehiculosPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="portal:mis-tickets" />}>
            <Route path="tickets" element={<MisTicketsPage />} />
          </Route>
          <Route element={<RequirePermiso permiso="portal:disponibilidad" />}>
            <Route path="disponibilidad" element={<DisponibilidadPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
