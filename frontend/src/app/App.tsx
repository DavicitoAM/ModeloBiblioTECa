import { BrowserRouter, Route, Routes } from 'react-router-dom'

import UserLayout from '../layout/UserLayout'
import AccessLayout from '../layout/AccessLayout'
import AdminLayout from '../layout/AdminLayout'

import HomePage from '../pages/HomePage'
import CatalogPage from '../pages/CatalogPage'
import CirculationPage from '../pages/CirculationPage'
import SpacesPage from '../pages/SpacesPage'
import NotFoundPage from '../pages/NotFoundPage'

import AccessGatewayPage from '../features/access/AccessGatewayPage'
import InstitutionalAccessPage from '../features/access/InstitutionalAccessPage'
import VisitorAccessPage from '../features/access/VisitorAccessPage'
import CheckOutPage from '../features/access/CheckOutPage'
import { institutionalProfiles } from '../features/access/profileConfig'

import AdminDashboardPage from '../features/admin/AdminDashboardPage'
import AdminRecordsPage from '../features/admin/AdminRecordsPage'
import AdminPlaceholderPage from '../features/admin/AdminPlaceholderPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        {/* Experiencia pública / usuario. No incluye navegación administrativa. */}
        <Route element={<UserLayout />}>
          <Route index element={<HomePage />} />
          <Route path="catalogo" element={<CatalogPage />} />
          <Route path="circulacion" element={<CirculationPage />} />
          <Route path="espacios" element={<SpacesPage />} />
        </Route>

        {/* Flujo de acceso aislado: pensado para kiosco/autoservicio. */}
        <Route element={<AccessLayout />}>
          <Route path="acceso" element={<AccessGatewayPage />} />
          <Route
            path="acceso/estudiante"
            element={
              <InstitutionalAccessPage
                config={institutionalProfiles.estudiante}
              />
            }
          />
          <Route
            path="acceso/docente"
            element={
              <InstitutionalAccessPage
                config={institutionalProfiles.docente}
              />
            }
          />
          <Route
            path="acceso/personal"
            element={
              <InstitutionalAccessPage
                config={institutionalProfiles.personal}
              />
            }
          />
          <Route path="acceso/visitante" element={<VisitorAccessPage />} />
          <Route path="salida" element={<CheckOutPage />} />
        </Route>

        {/* Administración separada. No existe enlace a /admin en la UI pública.
            La protección real requerirá autenticación/autorización en backend. */}
        <Route path="admin" element={<AdminLayout />}>
          <Route index element={<AdminDashboardPage />} />
          <Route path="registros" element={<AdminRecordsPage />} />
          <Route
            path="personas"
            element={
              <AdminPlaceholderPage
                title="Personas"
                description="Gestión administrativa de personas. Pendiente de endpoints CRUD específicos."
              />
            }
          />
          <Route
            path="catalogos"
            element={
              <AdminPlaceholderPage
                title="Catálogos"
                description="Administración de carreras, motivos y otros catálogos. Pendiente de endpoints de escritura."
              />
            }
          />
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}
