import {
  Alert,
  Box,
  CircularProgress,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import {
  getOpenAccessRecords,
  getRecentAccessRecords,
} from '../../api/library'
import type { AccessRecord } from '../../api/types'
import { tecPalette } from '../../theme/tokens'

export default function AdminDashboardPage() {
  const [open, setOpen] = useState<AccessRecord[]>([])
  const [recent, setRecent] = useState<AccessRecord[]>([])
  const [loading, setLoading] = useState(true)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    Promise.all([getOpenAccessRecords(), getRecentAccessRecords()])
      .then(([openData, recentData]) => {
        setOpen(openData)
        setRecent(recentData)
      })
      .catch(() => setFailed(true))
      .finally(() => setLoading(false))
  }, [])

  return (
    <Box>
      <Typography
        sx={{
          textTransform: 'uppercase',
          letterSpacing: '.12em',
          color: tecPalette.gray[500],
          fontWeight: 800,
          fontSize: 12,
        }}
      >
        Vista interna
      </Typography>
      <Typography variant="h3" sx={{ color: tecPalette.blue[900], mt: 1 }}>
        Resumen administrativo
      </Typography>
      <Typography sx={{ color: tecPalette.gray[600], mt: 1, mb: 4 }}>
        Este espacio está separado de la experiencia pública de estudiantes,
        docentes, personal y visitantes.
      </Typography>

      {loading && <CircularProgress />}
      {failed && (
        <Alert severity="error">
          No fue posible cargar el resumen.
        </Alert>
      )}

      {!loading && !failed && (
        <>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, minmax(0, 260px))',
              gap: 2,
            }}
          >
            <Box
              sx={{
                bgcolor: tecPalette.white,
                border: `1px solid ${tecPalette.blue[100]}`,
                borderRadius: '22px',
                p: 3,
              }}
            >
              <Typography sx={{ color: tecPalette.gray[600] }}>
                Personas dentro
              </Typography>
              <Typography variant="h3" sx={{ mt: 1, color: tecPalette.blue[700] }}>
                {open.length}
              </Typography>
            </Box>
            <Box
              sx={{
                bgcolor: tecPalette.white,
                border: `1px solid ${tecPalette.blue[100]}`,
                borderRadius: '22px',
                p: 3,
              }}
            >
              <Typography sx={{ color: tecPalette.gray[600] }}>
                Registros recientes cargados
              </Typography>
              <Typography variant="h3" sx={{ mt: 1, color: tecPalette.blue[700] }}>
                {recent.length}
              </Typography>
            </Box>
          </Box>
        </>
      )}
    </Box>
  )
}
