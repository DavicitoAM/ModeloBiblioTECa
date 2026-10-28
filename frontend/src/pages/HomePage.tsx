import { Box, Container, Typography } from '@mui/material'
import ServiceCard from '../components/ServiceCard'
import { tecPalette } from '../theme/tokens'

export default function HomePage() {
  return (
    <Box component="main">
      <Container maxWidth="xl" sx={{ pt: { xs: 6, md: 9 }, pb: 4 }}>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', lg: '1.05fr .95fr' },
            gap: { xs: 4, lg: 8 },
            alignItems: 'center',
          }}
        >
          <Box>
            <Typography
              sx={{
                color: tecPalette.blue[600],
                fontWeight: 800,
                letterSpacing: '.11em',
                textTransform: 'uppercase',
                fontSize: 13,
                mb: 2,
              }}
            >
              Biblioteca · Comunidad TecNM
            </Typography>

            <Typography
              variant="h1"
              sx={{
                fontSize: { xs: 44, sm: 58, md: 68 },
                lineHeight: 1.04,
                maxWidth: 720,
                color: tecPalette.blue[900],
              }}
            >
              Tu biblioteca,
              <Box component="span" sx={{ color: tecPalette.blue[600] }}>
                {' '}más simple de usar.
              </Box>
            </Typography>

            <Typography
              sx={{
                mt: 3,
                color: tecPalette.gray[600],
                fontSize: { xs: 17, md: 19 },
                lineHeight: 1.7,
                maxWidth: 660,
              }}
            >
              Registra tu acceso, consulta servicios y utiliza los recursos
              de la biblioteca desde una interfaz pensada para estudiantes,
              docentes, personal y visitantes.
            </Typography>
          </Box>

          <Box
            sx={{
              borderRadius: '34px',
              bgcolor: tecPalette.blue[600],
              color: tecPalette.white,
              p: { xs: 3, sm: 4.5 },
              minHeight: 300,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              boxShadow: '0 26px 70px rgba(27,57,106,.23)',
              backgroundImage:
                'linear-gradient(135deg, rgba(255,255,255,.08), transparent 55%)',
            }}
          >
            <Box>
              <Typography
                sx={{
                  fontSize: 13,
                  textTransform: 'uppercase',
                  letterSpacing: '.12em',
                  fontWeight: 800,
                  opacity: .72,
                }}
              >
                Acceso a biblioteca
              </Typography>
              <Typography
                variant="h3"
                sx={{
                  mt: 1.3,
                  fontSize: { xs: 31, sm: 40 },
                  maxWidth: 460,
                }}
              >
                Registra tu entrada en pocos pasos
              </Typography>
            </Box>

            <Typography sx={{ opacity: .84, maxWidth: 500, lineHeight: 1.65 }}>
              El sistema adapta el proceso según seas estudiante, docente,
              personal de la institución o visitante.
            </Typography>
          </Box>
        </Box>
      </Container>

      <Container maxWidth="xl" sx={{ py: { xs: 4, md: 6 } }}>
        <Typography
          variant="h4"
          sx={{ color: tecPalette.blue[900], mb: 1 }}
        >
          ¿Qué necesitas hacer?
        </Typography>
        <Typography sx={{ color: tecPalette.gray[600], mb: 3.5 }}>
          Selecciona un servicio para continuar.
        </Typography>

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: {
              xs: '1fr',
              sm: 'repeat(2, 1fr)',
              xl: 'repeat(4, 1fr)',
            },
            gap: 2.5,
          }}
        >
          <ServiceCard
            title="Registrar acceso"
            description="Entrada y salida para estudiantes, docentes, personal y visitantes."
            icon="login"
            to="/acceso"
            primary
          />
          <ServiceCard
            title="Catálogo"
            description="Consulta material bibliográfico y disponibilidad."
            icon="book"
            to="/catalogo"
          />
          <ServiceCard
            title="Préstamos"
            description="Consulta servicios de circulación, préstamos y devoluciones."
            icon="loan"
            to="/circulacion"
          />
          <ServiceCard
            title="Espacios"
            description="Información de salas, cubículos y espacios de la biblioteca."
            icon="space"
            to="/espacios"
          />
        </Box>
      </Container>
    </Box>
  )
}
