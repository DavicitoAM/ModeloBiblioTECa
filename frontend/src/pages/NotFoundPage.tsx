import { Box, Button, Typography } from '@mui/material'
import { Link } from 'react-router-dom'

export default function NotFoundPage() {
  return (
    <Box sx={{ p: 6, textAlign: 'center' }}>
      <Typography variant="h3">Página no encontrada</Typography>
      <Button component={Link} to="/" sx={{ mt: 3 }}>
        Volver al inicio
      </Button>
    </Box>
  )
}
