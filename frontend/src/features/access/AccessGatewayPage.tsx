import { Box, ButtonBase, Typography } from '@mui/material'
import { Link } from 'react-router-dom'
import AppIcon, { type AppIconName } from '../../components/AppIcon'
import { tecPalette } from '../../theme/tokens'

const profiles: Array<{
  title: string
  subtitle: string
  to: string
  icon: AppIconName
}> = [
  {
    title: 'Estudiante',
    subtitle: 'Identifícate con tu número de control',
    to: '/acceso/estudiante',
    icon: 'student',
  },
  {
    title: 'Docente',
    subtitle: 'Utiliza tu clave institucional',
    to: '/acceso/docente',
    icon: 'teacher',
  },
  {
    title: 'Personal',
    subtitle: 'Personal administrativo o de apoyo',
    to: '/acceso/personal',
    icon: 'staff',
  },
  {
    title: 'Visitante',
    subtitle: 'Registro para personas externas',
    to: '/acceso/visitante',
    icon: 'visitor',
  },
]

export default function AccessGatewayPage() {
  return (
    <Box sx={{ maxWidth: 980, mx: 'auto' }}>
      <Box sx={{ textAlign: 'center', mb: { xs: 4, md: 5 } }}>
        <Typography
          sx={{
            textTransform: 'uppercase',
            letterSpacing: '.12em',
            color: tecPalette.blue[600],
            fontWeight: 800,
            fontSize: 13,
            mb: 1.5,
          }}
        >
          Registro de acceso
        </Typography>
        <Typography
          variant="h2"
          sx={{
            color: tecPalette.blue[900],
            fontSize: { xs: 38, sm: 48 },
          }}
        >
          ¿Quién está ingresando?
        </Typography>
        <Typography
          sx={{
            color: tecPalette.gray[600],
            mt: 1.5,
            fontSize: 17,
          }}
        >
          Selecciona tu perfil para mostrar únicamente los datos que necesitas.
        </Typography>
      </Box>

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)' },
          gap: 2.5,
        }}
      >
        {profiles.map((profile) => (
          <ButtonBase
            key={profile.to}
            component={Link}
            to={profile.to}
            sx={{
              display: 'block',
              textAlign: 'left',
              borderRadius: '26px',
            }}
          >
            <Box
              sx={{
                bgcolor: tecPalette.white,
                border: `1px solid ${tecPalette.blue[100]}`,
                borderRadius: '26px',
                p: { xs: 2.5, sm: 3.25 },
                minHeight: 180,
                boxShadow: '0 14px 38px rgba(13,29,54,.07)',
                transition: 'all .18s ease',
                '&:hover': {
                  transform: 'translateY(-3px)',
                  borderColor: tecPalette.blue[300],
                  boxShadow: '0 20px 46px rgba(13,29,54,.11)',
                },
              }}
            >
              <Box
                sx={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  gap: 2,
                }}
              >
                <Box
                  sx={{
                    width: 54,
                    height: 54,
                    borderRadius: '17px',
                    display: 'grid',
                    placeItems: 'center',
                    bgcolor: tecPalette.blue[100],
                    color: tecPalette.blue[700],
                  }}
                >
                  <AppIcon name={profile.icon} size={27} />
                </Box>
                <Box sx={{ color: tecPalette.blue[500], pt: .7 }}>
                  <AppIcon name="arrow" />
                </Box>
              </Box>

              <Typography
                variant="h5"
                sx={{ mt: 3, color: tecPalette.blue[900] }}
              >
                {profile.title}
              </Typography>
              <Typography
                sx={{
                  mt: .75,
                  color: tecPalette.gray[600],
                  fontSize: 14.5,
                }}
              >
                {profile.subtitle}
              </Typography>
            </Box>
          </ButtonBase>
        ))}
      </Box>

      <ButtonBase
        component={Link}
        to="/salida"
        sx={{
          mt: 3,
          width: '100%',
          borderRadius: '20px',
          display: 'block',
        }}
      >
        <Box
          sx={{
            borderRadius: '20px',
            bgcolor: tecPalette.blue[900],
            color: tecPalette.white,
            p: 2.4,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 1.5,
          }}
        >
          <AppIcon name="logout" size={22} />
          <Typography sx={{ fontWeight: 800 }}>
            Ya estoy dentro · Registrar mi salida
          </Typography>
        </Box>
      </ButtonBase>
    </Box>
  )
}
