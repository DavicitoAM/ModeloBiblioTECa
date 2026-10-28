import {
  Box,
  ButtonBase,
  Drawer,
  Typography,
} from '@mui/material'
import { NavLink, Outlet } from 'react-router-dom'
import Brand from '../components/Brand'
import AppIcon, { type AppIconName } from '../components/AppIcon'
import { tecPalette } from '../theme/tokens'

const drawerWidth = 255

const items: Array<[string, string, AppIconName]> = [
  ['Resumen', '/admin', 'home'],
  ['Registros', '/admin/registros', 'records'],
  ['Personas', '/admin/personas', 'people'],
  ['Catálogos', '/admin/catalogos', 'settings'],
]

export default function AdminLayout() {
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: tecPalette.blue[50] }}>
      <Drawer
        variant="permanent"
        sx={{
          width: drawerWidth,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: drawerWidth,
            boxSizing: 'border-box',
            borderRight: `1px solid ${tecPalette.blue[100]}`,
            p: 2,
          },
        }}
      >
        <Box sx={{ px: 1, py: 1.5 }}>
          <Brand compact />
          <Typography
            sx={{
              mt: 2.5,
              fontSize: 12,
              textTransform: 'uppercase',
              letterSpacing: '.12em',
              color: tecPalette.gray[500],
              fontWeight: 800,
            }}
          >
            Administración interna
          </Typography>
        </Box>

        <Box component="nav" sx={{ mt: 1 }}>
          {items.map(([label, to, icon]) => (
            <ButtonBase
              key={to}
              component={NavLink}
              to={to}
              end={to === '/admin'}
              sx={{
                width: '100%',
                borderRadius: '14px',
                px: 1.5,
                py: 1.2,
                my: 0.35,
                justifyContent: 'flex-start',
                gap: 1.4,
                color: tecPalette.gray[700],
                '&.active': {
                  bgcolor: tecPalette.blue[600],
                  color: tecPalette.white,
                },
              }}
            >
              <AppIcon name={icon} size={20} />
              <Typography sx={{ fontWeight: 700, fontSize: 14 }}>
                {label}
              </Typography>
            </ButtonBase>
          ))}
        </Box>
      </Drawer>

      <Box sx={{ ml: `${drawerWidth}px`, minHeight: '100vh', p: 4 }}>
        <Outlet />
      </Box>
    </Box>
  )
}
