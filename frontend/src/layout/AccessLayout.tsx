import { Box, Button, Container } from '@mui/material'
import { Link, Outlet } from 'react-router-dom'
import Brand from '../components/Brand'
import AppIcon from '../components/AppIcon'
import { tecPalette } from '../theme/tokens'

export default function AccessLayout() {
  return (
    <Box
      sx={{
        minHeight: '100vh',
        bgcolor: tecPalette.blue[50],
        backgroundImage:
          'radial-gradient(circle at 90% 5%, rgba(129,150,179,.20), transparent 32%), radial-gradient(circle at 10% 92%, rgba(27,57,106,.08), transparent 28%)',
      }}
    >
      <Box
        component="header"
        sx={{
          bgcolor: tecPalette.white,
          borderBottom: `1px solid ${tecPalette.blue[100]}`,
        }}
      >
        <Container
          maxWidth="lg"
          sx={{
            minHeight: 82,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 2,
          }}
        >
          <Brand compact />
          <Button
            component={Link}
            to="/"
            startIcon={<AppIcon name="home" size={18} />}
            sx={{ color: tecPalette.blue[700] }}
          >
            Inicio
          </Button>
        </Container>
      </Box>

      <Container
        maxWidth="lg"
        component="main"
        sx={{ py: { xs: 4, md: 7 } }}
      >
        <Outlet />
      </Container>
    </Box>
  )
}
