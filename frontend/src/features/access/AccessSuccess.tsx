import { Box, Button, Typography } from '@mui/material'
import { Link } from 'react-router-dom'
import type { AccessRecord } from '../../api/types'
import AppIcon from '../../components/AppIcon'
import { tecPalette } from '../../theme/tokens'

interface Props {
  access: AccessRecord
  action: 'check-in' | 'check-out'
  onAnother?: () => void
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat('es-MX', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

export default function AccessSuccess({
  access,
  action,
  onAnother,
}: Props) {
  const isCheckIn = action === 'check-in'

  return (
    <Box
      sx={{
        maxWidth: 650,
        mx: 'auto',
        bgcolor: tecPalette.white,
        border: `1px solid ${tecPalette.blue[100]}`,
        borderRadius: '30px',
        p: { xs: 3, sm: 5 },
        textAlign: 'center',
        boxShadow: '0 22px 58px rgba(13,29,54,.10)',
      }}
    >
      <Box
        sx={{
          width: 72,
          height: 72,
          borderRadius: '50%',
          display: 'grid',
          placeItems: 'center',
          mx: 'auto',
          bgcolor: tecPalette.successSoft,
          color: tecPalette.success,
        }}
      >
        <AppIcon name="check" size={38} />
      </Box>

      <Typography
        variant="h3"
        sx={{ mt: 3, color: tecPalette.blue[900], fontSize: { xs: 31, sm: 38 } }}
      >
        {isCheckIn ? 'Entrada registrada' : 'Salida registrada'}
      </Typography>

      <Typography variant="h6" sx={{ mt: 2 }}>
        {access.person.fullName}
      </Typography>

      <Typography sx={{ color: tecPalette.gray[600], mt: .6 }}>
        {access.person.typeName}
        {access.person.institutionalIdentifier
          ? ` · ${access.person.institutionalIdentifier}`
          : ''}
      </Typography>

      <Box
        sx={{
          mt: 3,
          borderRadius: '18px',
          bgcolor: tecPalette.blue[50],
          p: 2.2,
          color: tecPalette.gray[700],
        }}
      >
        {isCheckIn
          ? `Entrada: ${formatDate(access.checkedInAt)}`
          : `Salida: ${formatDate(access.checkedOutAt || new Date().toISOString())}`}
      </Box>

      <Box
        sx={{
          mt: 4,
          display: 'flex',
          gap: 1.5,
          justifyContent: 'center',
          flexWrap: 'wrap',
        }}
      >
        {onAnother && (
          <Button variant="outlined" onClick={onAnother}>
            Realizar otro registro
          </Button>
        )}
        <Button variant="contained" component={Link} to="/">
          Finalizar
        </Button>
      </Box>
    </Box>
  )
}
