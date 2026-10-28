import {
  Alert,
  Box,
  Button,
  CircularProgress,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  checkIn,
  createVisitor,
  getVisitReasons,
} from '../../api/library'
import { apiErrorMessage } from '../../api/errors'
import type { AccessRecord, VisitReason } from '../../api/types'
import AppIcon from '../../components/AppIcon'
import { tecPalette } from '../../theme/tokens'
import AccessSuccess from './AccessSuccess'

const initialForm = {
  firstName: '',
  paternalSurname: '',
  maternalSurname: '',
  email: '',
  phone: '',
  destination: '',
  notes: '',
}

export default function VisitorAccessPage() {
  const [form, setForm] = useState(initialForm)
  const [reasons, setReasons] = useState<VisitReason[]>([])
  const [reasonId, setReasonId] = useState<number | ''>('')
  const [success, setSuccess] = useState<AccessRecord | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getVisitReasons()
      .then(setReasons)
      .catch(() => setError('No fue posible cargar los motivos de visita.'))
  }, [])

  function update(
    field: keyof typeof initialForm,
    value: string,
  ) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function handleSubmit() {
    if (!form.firstName.trim() || !form.paternalSurname.trim()) {
      setError('Nombre y apellido paterno son obligatorios.')
      return
    }

    if (reasonId === '') {
      setError('Selecciona el motivo de tu visita.')
      return
    }

    if (!form.destination.trim()) {
      setError('Indica el área o destino que visitas.')
      return
    }

    setLoading(true)
    setError(null)

    try {
      const visitor = await createVisitor({
        firstName: form.firstName.trim(),
        paternalSurname: form.paternalSurname.trim(),
        maternalSurname: form.maternalSurname.trim() || null,
        email: form.email.trim() || null,
        phone: form.phone.trim() || null,
      })

      const access = await checkIn({
        personId: visitor.id,
        visitReasonId: reasonId,
        destination: form.destination.trim(),
        notes: form.notes.trim() || null,
      })

      setSuccess(access)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  function reset() {
    setForm(initialForm)
    setReasonId('')
    setSuccess(null)
    setError(null)
  }

  if (success) {
    return (
      <AccessSuccess
        access={success}
        action="check-in"
        onAnother={reset}
      />
    )
  }

  return (
    <Box sx={{ maxWidth: 830, mx: 'auto' }}>
      <Button component={Link} to="/acceso" sx={{ mb: 2.5 }}>
        ← Cambiar tipo de usuario
      </Button>

      <Box
        sx={{
          bgcolor: tecPalette.white,
          borderRadius: '30px',
          border: `1px solid ${tecPalette.blue[100]}`,
          p: { xs: 3, sm: 4.5 },
          boxShadow: '0 20px 52px rgba(13,29,54,.08)',
        }}
      >
        <Box
          sx={{
            width: 58,
            height: 58,
            display: 'grid',
            placeItems: 'center',
            borderRadius: '18px',
            bgcolor: tecPalette.blue[100],
            color: tecPalette.blue[700],
          }}
        >
          <AppIcon name="visitor" size={29} />
        </Box>

        <Typography
          variant="h3"
          sx={{
            mt: 2.5,
            color: tecPalette.blue[900],
            fontSize: { xs: 32, sm: 40 },
          }}
        >
          Registro de visitante
        </Typography>
        <Typography
          sx={{ mt: 1, color: tecPalette.gray[600], lineHeight: 1.6 }}
        >
          Captura tus datos y la información básica de la visita. Esta
          pantalla no muestra funciones administrativas.
        </Typography>

        {error && <Alert severity="error" sx={{ mt: 2.5 }}>{error}</Alert>}

        <Typography variant="h6" sx={{ mt: 4, mb: 2 }}>
          Datos personales
        </Typography>

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)' },
            gap: 1.7,
          }}
        >
          <TextField
            required
            label="Nombre"
            value={form.firstName}
            onChange={(e) => update('firstName', e.target.value)}
          />
          <TextField
            required
            label="Apellido paterno"
            value={form.paternalSurname}
            onChange={(e) => update('paternalSurname', e.target.value)}
          />
          <TextField
            label="Apellido materno"
            value={form.maternalSurname}
            onChange={(e) => update('maternalSurname', e.target.value)}
          />
          <TextField
            label="Teléfono (opcional)"
            value={form.phone}
            onChange={(e) => update('phone', e.target.value)}
          />
          <TextField
            label="Correo (opcional)"
            type="email"
            value={form.email}
            onChange={(e) => update('email', e.target.value)}
            sx={{ gridColumn: { sm: '1 / -1' } }}
          />
        </Box>

        <Typography variant="h6" sx={{ mt: 4, mb: 2 }}>
          Información de la visita
        </Typography>

        <Box sx={{ display: 'grid', gap: 1.7 }}>
          <FormControl fullWidth required>
            <InputLabel id="visitor-reason-label">Motivo</InputLabel>
            <Select
              labelId="visitor-reason-label"
              value={reasonId}
              label="Motivo"
              onChange={(e) => setReasonId(e.target.value as number)}
            >
              {reasons.map((reason) => (
                <MenuItem key={reason.id} value={reason.id}>
                  {reason.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <TextField
            required
            label="Área, persona o destino que visitas"
            value={form.destination}
            onChange={(e) => update('destination', e.target.value)}
          />

          <TextField
            label="Observaciones (opcional)"
            multiline
            minRows={3}
            value={form.notes}
            onChange={(e) => update('notes', e.target.value)}
          />
        </Box>

        <Button
          variant="contained"
          size="large"
          fullWidth
          sx={{ mt: 3 }}
          onClick={handleSubmit}
          disabled={loading}
          startIcon={
            loading
              ? <CircularProgress color="inherit" size={18} />
              : <AppIcon name="login" size={20} />
          }
        >
          Registrar mi entrada
        </Button>
      </Box>
    </Box>
  )
}
