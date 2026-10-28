import { Box, Card, CardContent, Stack, Typography } from '@mui/material'
import AppIcon, { type AppIconName } from '../components/AppIcon'

const modules: Array<{ title: string; text: string; icon: AppIconName }> = [
  { title: 'Identidad institucional', text: 'Paleta TecNM, tipografía y activos gráficos del sistema.', icon: 'admin' },
  { title: 'Catálogos operativos', text: 'Tipos de persona, carreras y motivos de visita configurados en PostgreSQL.', icon: 'catalog' },
  { title: 'Estado del servicio', text: 'Frontend Vite conectado al backend Spring Boot mediante /api/v1.', icon: 'clock' },
]

export default function AdminPage() {
  return (
    <Stack spacing={2.5}>
      <Box>
        <Typography variant="overline" sx={{ color: 'primary.main', fontWeight: 850, letterSpacing: '.12em' }}>SISTEMA</Typography>
        <Typography variant="h3" sx={{ fontSize: { xs: 29, md: 36 } }}>Administración</Typography>
        <Typography color="text.secondary" sx={{ mt: .8 }}>Vista base para las configuraciones administrativas del proyecto.</Typography>
      </Box>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'repeat(3,1fr)' }, gap: 1.7 }}>
        {modules.map((module) => (
          <Card key={module.title}>
            <CardContent sx={{ p: 2.6 }}>
              <Box sx={{ width: 44, height: 44, borderRadius: 2.4, bgcolor: '#EAF0F6', color: 'primary.main', display: 'grid', placeItems: 'center', mb: 2 }}><AppIcon name={module.icon} /></Box>
              <Typography variant="h6">{module.title}</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: .7 }}>{module.text}</Typography>
            </CardContent>
          </Card>
        ))}
      </Box>
    </Stack>
  )
}
