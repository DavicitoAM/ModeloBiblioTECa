import {
  Avatar,
  Box,
  Divider,
  Drawer,
  IconButton,
  Stack,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material'
import { useMemo, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import tecnmLogo from '../assets/tecnm-logo.png'
import AppIcon, { type AppIconName } from '../components/AppIcon'

const drawerWidth = 278

type NavItem = {
  label: string
  path: string
  icon: AppIconName
}

type NavSection = {
  title: string
  items: NavItem[]
}

const sections: NavSection[] = [
  {
    title: 'GENERAL',
    items: [{ label: 'Resumen', path: '/', icon: 'dashboard' }],
  },
  {
    title: 'CONTROL DE ACCESO',
    items: [
      { label: 'Registrar entrada', path: '/ingreso', icon: 'login' },
      { label: 'Registrar salida', path: '/salida', icon: 'logout' },
      { label: 'Historial', path: '/registros', icon: 'history' },
    ],
  },
  {
    title: 'BIBLIOTECA',
    items: [
      { label: 'Catálogos', path: '/catalogo', icon: 'catalog' },
      { label: 'Circulación', path: '/circulacion', icon: 'book' },
      { label: 'Espacios', path: '/espacios', icon: 'space' },
    ],
  },
  {
    title: 'SISTEMA',
    items: [{ label: 'Administración', path: '/admin', icon: 'admin' }],
  },
]

const routeTitles: Record<string, string> = {
  '/': 'Resumen operativo',
  '/ingreso': 'Registrar entrada',
  '/salida': 'Registrar salida',
  '/registros': 'Historial de accesos',
  '/catalogo': 'Catálogos',
  '/circulacion': 'Circulación',
  '/espacios': 'Espacios',
  '/admin': 'Administración',
}

function SidebarContent({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <Stack sx={{ minHeight: '100%', p: 2.2 }}>
      <Box sx={{ px: 1.4, pt: 1.2, pb: 2.2 }}>
        <img
          src={tecnmLogo}
          alt="Tecnológico Nacional de México"
          style={{ width: '100%', maxWidth: 215, display: 'block' }}
        />
        <Stack direction="row" spacing={1.2} sx={{ mt: 2.2, alignItems: 'center' }}>
          <Box
            sx={{
              width: 42,
              height: 42,
              borderRadius: 2.2,
              bgcolor: 'primary.main',
              color: 'common.white',
              display: 'grid',
              placeItems: 'center',
              fontWeight: 800,
              fontSize: 12,
              letterSpacing: '.04em',
            }}
            aria-label="Espacio reservado para el logotipo institucional"
          >
            ITSPA
          </Box>
          <Box>
            <Typography sx={{ fontWeight: 800, lineHeight: 1.15 }}>
              BiblioTECa
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Control bibliotecario
            </Typography>
          </Box>
        </Stack>
      </Box>

      <Divider />

      <Box component="nav" sx={{ py: 1.6, flexGrow: 1 }}>
        {sections.map((section) => (
          <Box key={section.title} sx={{ mb: 2.1 }}>
            <Typography
              variant="caption"
              sx={{
                display: 'block',
                px: 1.5,
                mb: 0.7,
                color: '#8A94A0',
                fontWeight: 800,
                fontSize: 10,
                letterSpacing: '.13em',
              }}
            >
              {section.title}
            </Typography>

            <Stack spacing={0.4}>
              {section.items.map((item) => (
                <Box
                  key={item.path}
                  component={NavLink}
                  to={item.path}
                  onClick={onNavigate}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1.4,
                    minHeight: 44,
                    px: 1.5,
                    borderRadius: 2.5,
                    color: '#5D6875',
                    textDecoration: 'none',
                    fontWeight: 650,
                    fontSize: 14,
                    transition: 'all .18s ease',
                    '&:hover': {
                      bgcolor: '#F1F5F9',
                      color: 'primary.main',
                    },
                    '&.active': {
                      bgcolor: 'primary.main',
                      color: 'common.white',
                      boxShadow: '0 9px 22px rgba(27,57,106,.18)',
                    },
                  }}
                >
                  <AppIcon name={item.icon} size={20} />
                  <span>{item.label}</span>
                  <Box sx={{ ml: 'auto', display: 'flex' }}>
                    <AppIcon name="arrow" size={15} />
                  </Box>
                </Box>
              ))}
            </Stack>
          </Box>
        ))}
      </Box>

      <Box
        sx={{
          borderRadius: 3,
          bgcolor: '#F2F6FA',
          border: '1px solid #E2E8F0',
          p: 1.7,
        }}
      >
        <Typography variant="caption" sx={{ fontWeight: 800, color: 'primary.main' }}>
          IDENTIDAD INSTITUCIONAL
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: .5 }}>
          TecNM · Instituto Tecnológico Superior de Pátzcuaro
        </Typography>
      </Box>
    </Stack>
  )
}

export default function MainLayout() {
  const theme = useTheme()
  const desktop = useMediaQuery(theme.breakpoints.up('lg'))
  const [mobileOpen, setMobileOpen] = useState(false)
  const location = useLocation()
  const currentTitle = routeTitles[location.pathname] ?? 'BiblioTECa'

  const dateText = useMemo(
    () =>
      new Intl.DateTimeFormat('es-MX', {
        weekday: 'long',
        day: '2-digit',
        month: 'long',
        year: 'numeric',
      }).format(new Date()),
    [],
  )

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex' }}>
      {desktop ? (
        <Box
          component="aside"
          sx={{
            width: drawerWidth,
            flexShrink: 0,
            position: 'fixed',
            inset: '18px auto 18px 18px',
            height: 'calc(100vh - 36px)',
            bgcolor: 'background.paper',
            borderRadius: '26px 0 0 26px',
            border: '1px solid #DEE5EC',
            overflowY: 'auto',
            zIndex: 10,
          }}
        >
          <SidebarContent />
        </Box>
      ) : (
        <Drawer
          open={mobileOpen}
          onClose={() => setMobileOpen(false)}
          slotProps={{ paper: { sx: { width: drawerWidth } } }}
        >
          <SidebarContent onNavigate={() => setMobileOpen(false)} />
        </Drawer>
      )}

      <Box
        sx={{
          flexGrow: 1,
          ml: { lg: `${drawerWidth + 18}px` },
          minWidth: 0,
          p: { xs: 1.5, sm: 2.2, lg: '18px 18px 18px 0' },
        }}
      >
        <Box
          sx={{
            minHeight: { lg: 'calc(100vh - 36px)' },
            borderRadius: { xs: 4, lg: '0 26px 26px 0' },
            border: '1px solid #DCE4EC',
            overflow: 'hidden',
            background:
              'linear-gradient(135deg, rgba(255,255,255,.94) 0%, rgba(238,244,249,.97) 50%, rgba(226,235,244,.98) 100%)',
            boxShadow: '0 24px 70px rgba(27,57,106,.12)',
          }}
        >
          <Box
            component="header"
            sx={{
              minHeight: 78,
              px: { xs: 2, md: 3.2 },
              display: 'flex',
              alignItems: 'center',
              gap: 2,
              borderBottom: '1px solid rgba(128,126,130,.14)',
              bgcolor: 'rgba(255,255,255,.72)',
              backdropFilter: 'blur(14px)',
            }}
          >
            {!desktop && (
              <IconButton onClick={() => setMobileOpen(true)} aria-label="Abrir menú">
                <AppIcon name="menu" />
              </IconButton>
            )}

            <Box sx={{ minWidth: 0 }}>
              <Typography sx={{ fontWeight: 800, fontSize: { xs: 16, md: 18 } }}>
                {currentTitle}
              </Typography>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ textTransform: 'capitalize' }}
              >
                {dateText}
              </Typography>
            </Box>

            <Stack direction="row" spacing={1.3} sx={{ ml: 'auto', alignItems: 'center' }}>
              <Box
                sx={{
                  display: { xs: 'none', sm: 'flex' },
                  alignItems: 'center',
                  gap: .8,
                  px: 1.4,
                  py: .8,
                  borderRadius: 999,
                  bgcolor: '#F0F4F8',
                  color: 'primary.main',
                  fontSize: 12,
                  fontWeight: 750,
                }}
              >
                <Box sx={{ width: 7, height: 7, borderRadius: '50%', bgcolor: 'primary.main' }} />
                API local · 8081
              </Box>
              <Avatar
                sx={{
                  width: 38,
                  height: 38,
                  bgcolor: 'primary.main',
                  fontSize: 13,
                  fontWeight: 800,
                }}
              >
                BT
              </Avatar>
            </Stack>
          </Box>

          <Box component="main" sx={{ p: { xs: 2, sm: 2.6, md: 3.2 } }}>
            <Outlet />
          </Box>
        </Box>
      </Box>
    </Box>
  )
}
