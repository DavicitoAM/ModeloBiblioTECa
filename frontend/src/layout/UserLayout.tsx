import { Box, Button, Container } from '@mui/material'
import { Link, NavLink, Outlet } from 'react-router-dom'
import Brand from '../components/Brand'
import { tecPalette } from '../theme/tokens'

const nav = [
  ['Inicio', '/'],
  ['Catálogo', '/catalogo'],
  ['Préstamos', '/circulacion'],
  ['Espacios', '/espacios'],
] as const

export default function UserLayout() {
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: tecPalette.blue[50] }}>
      <Box
        component="header"
        sx={{
          position: 'sticky',
          top: 0,
          zIndex: 20,
          bgcolor: 'rgba(255,255,255,.94)',
          backdropFilter: 'blur(14px)',
          borderBottom: `1px solid ${tecPalette.blue[100]}`,
        }}
      >
        <Container
          maxWidth="xl"
          sx={{
            minHeight: 82,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 2,
          }}
        >
          <Brand />

          <Box
            component="nav"
            aria-label="Servicios de biblioteca"
            sx={{
              display: { xs: 'none', lg: 'flex' },
              alignItems: 'center',
              gap: 0.5,
            }}
          >
            {nav.map(([label, to]) => (
              <Button
                key={to}
                component={NavLink}
                to={to}
                sx={{
                  px: 1.6,
                  color: tecPalette.gray[700],
                  '&.active': {
                    bgcolor: tecPalette.blue[100],
                    color: tecPalette.blue[700],
                  },
                }}
              >
                {label}
              </Button>
            ))}
          </Box>

          <Button
            variant="contained"
            component={Link}
            to="/acceso"
            sx={{ flexShrink: 0 }}
          >
            Registrar acceso
          </Button>
        </Container>
      </Box>

      <Outlet />

      <Box
        component="footer"
        sx={{
          borderTop: `1px solid ${tecPalette.blue[100]}`,
          bgcolor: tecPalette.white,
          mt: 8,
        }}
      >
        <Container
          maxWidth="xl"
          sx={{
            minHeight: 92,
            py: 2,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 2,
            flexWrap: 'wrap',
          }}
        >
          <Brand compact />
          <Box
            sx={{
              fontSize: 13,
              color: tecPalette.gray[600],
              textAlign: { xs: 'left', sm: 'right' },
            }}
          >
            BiblioTECa · Servicios para la comunidad institucional
          </Box>
        </Container>
      </Box>
    </Box>
  )
}
