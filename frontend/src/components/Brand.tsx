import { Box, Typography } from '@mui/material'
import tecnmLogo from '../assets/tecnm-logo.png'
import { tecPalette } from '../theme/tokens'

interface Props {
  compact?: boolean
}

export default function Brand({ compact = false }: Props) {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: { xs: 1.25, md: 2 },
        minWidth: 0,
      }}
    >
      <Box
        component="img"
        src={tecnmLogo}
        alt="Tecnológico Nacional de México"
        sx={{
          width: compact ? { xs: 150, sm: 190 } : { xs: 170, sm: 230 },
          height: 'auto',
          display: 'block',
          flexShrink: 0,
        }}
      />

      {!compact && (
        <>
          <Box
            sx={{
              width: 1,
              height: 42,
              bgcolor: tecPalette.blue[100],
              display: { xs: 'none', md: 'block' },
            }}
          />
          <Box sx={{ display: { xs: 'none', md: 'block' } }}>
            <Typography
              sx={{
                fontWeight: 800,
                color: tecPalette.blue[800],
                lineHeight: 1.1,
                fontSize: 14,
              }}
            >
              Instituto Tecnológico Superior
            </Typography>
            <Typography
              sx={{
                color: tecPalette.gray[600],
                fontSize: 13,
                mt: 0.4,
              }}
            >
              de Pátzcuaro · Biblioteca
            </Typography>
          </Box>
        </>
      )}
    </Box>
  )
}
