import { Box, ButtonBase, Typography } from '@mui/material'
import { Link } from 'react-router-dom'
import AppIcon, { type AppIconName } from './AppIcon'
import { tecPalette } from '../theme/tokens'

interface Props {
  title: string
  description: string
  icon: AppIconName
  to: string
  primary?: boolean
}

export default function ServiceCard({
  title,
  description,
  icon,
  to,
  primary = false,
}: Props) {
  return (
    <ButtonBase
      component={Link}
      to={to}
      sx={{
        display: 'block',
        width: '100%',
        textAlign: 'left',
        borderRadius: '26px',
      }}
    >
      <Box
        sx={{
          minHeight: 190,
          p: { xs: 2.5, sm: 3 },
          borderRadius: '26px',
          border: `1px solid ${
            primary ? tecPalette.blue[600] : tecPalette.blue[100]
          }`,
          background: primary
            ? `linear-gradient(145deg, ${tecPalette.blue[700]}, ${tecPalette.blue[600]})`
            : tecPalette.white,
          color: primary ? tecPalette.white : tecPalette.gray[900],
          boxShadow: primary
            ? '0 20px 46px rgba(27,57,106,.22)'
            : '0 14px 34px rgba(13,29,54,.06)',
          transition: 'transform .18s ease, box-shadow .18s ease',
          '&:hover': {
            transform: 'translateY(-3px)',
            boxShadow: primary
              ? '0 24px 54px rgba(27,57,106,.28)'
              : '0 18px 42px rgba(13,29,54,.10)',
          },
        }}
      >
        <Box
          sx={{
            width: 48,
            height: 48,
            display: 'grid',
            placeItems: 'center',
            borderRadius: '15px',
            bgcolor: primary
              ? 'rgba(255,255,255,.14)'
              : tecPalette.blue[100],
            color: primary ? 'inherit' : tecPalette.blue[700],
            mb: 2.5,
          }}
        >
          <AppIcon name={icon} size={25} />
        </Box>

        <Typography variant="h6" sx={{ mb: 0.75 }}>
          {title}
        </Typography>

        <Typography
          sx={{
            color: primary
              ? 'rgba(255,255,255,.82)'
              : tecPalette.gray[600],
            fontSize: 14.5,
            lineHeight: 1.55,
            maxWidth: 330,
          }}
        >
          {description}
        </Typography>

        <Box
          sx={{
            mt: 2.5,
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            color: primary ? 'inherit' : tecPalette.blue[700],
            fontWeight: 750,
            fontSize: 14,
          }}
        >
          Abrir
          <AppIcon name="arrow" size={18} />
        </Box>
      </Box>
    </ButtonBase>
  )
}
