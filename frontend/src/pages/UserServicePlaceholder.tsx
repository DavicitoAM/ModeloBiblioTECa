import { Box, Button, Container, Typography } from '@mui/material'
import { Link } from 'react-router-dom'
import AppIcon, { type AppIconName } from '../components/AppIcon'
import { tecPalette } from '../theme/tokens'

interface Props {
  title: string
  description: string
  icon: AppIconName
}

export default function UserServicePlaceholder({
  title,
  description,
  icon,
}: Props) {
  return (
    <Container maxWidth="md" component="main" sx={{ py: { xs: 6, md: 10 } }}>
      <Box
        sx={{
          bgcolor: tecPalette.white,
          border: `1px solid ${tecPalette.blue[100]}`,
          borderRadius: '30px',
          p: { xs: 3, sm: 5 },
          boxShadow: '0 18px 50px rgba(13,29,54,.07)',
        }}
      >
        <Box
          sx={{
            width: 60,
            height: 60,
            borderRadius: '18px',
            display: 'grid',
            placeItems: 'center',
            bgcolor: tecPalette.blue[100],
            color: tecPalette.blue[700],
            mb: 3,
          }}
        >
          <AppIcon name={icon} size={30} />
        </Box>

        <Typography variant="h3" sx={{ color: tecPalette.blue[900] }}>
          {title}
        </Typography>
        <Typography
          sx={{
            color: tecPalette.gray[600],
            mt: 2,
            lineHeight: 1.7,
            maxWidth: 640,
          }}
        >
          {description}
        </Typography>

        <Box
          sx={{
            bgcolor: tecPalette.blue[50],
            borderRadius: '18px',
            p: 2.5,
            mt: 4,
            color: tecPalette.gray[700],
          }}
        >
          Esta sección ya tiene su lugar dentro de la experiencia del usuario,
          pero todavía necesita los endpoints específicos del backend para ser
          funcional.
        </Box>

        <Button component={Link} to="/" sx={{ mt: 3 }}>
          Volver al inicio
        </Button>
      </Box>
    </Container>
  )
}
