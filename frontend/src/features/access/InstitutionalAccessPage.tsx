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
import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  checkIn,
  getOpenAccessRecords,
  getPersonByIdentifier,
  getVisitReasons,
} from '../../api/library'
import { apiErrorMessage } from '../../api/errors'
import type {
  AccessRecord,
  Person,
  VisitReason,
} from '../../api/types'
import AppIcon from '../../components/AppIcon'
import { tecPalette } from '../../theme/tokens'
import AccessSuccess from './AccessSuccess'
import type { InstitutionalProfileConfig } from './profileConfig'

interface Props {
  config: InstitutionalProfileConfig
}

export default function InstitutionalAccessPage({ config }: Props) {
  const [identifier, setIdentifier] = useState('')
  const [person, setPerson] = useState<Person | null>(null)
  const [reasons, setReasons] = useState<VisitReason[]>([])
  const [reasonId, setReasonId] = useState<number | ''>('')
  const [openAccess, setOpenAccess] = useState<AccessRecord | null>(null)
  const [success, setSuccess] = useState<AccessRecord | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getVisitReasons().then(setReasons).catch(() => setReasons([]))
  }, [])

  const studentInfo = useMemo(() => {
    if (!person?.student) return null
    return [
      person.student.careerName,
      person.student.semester
        ? `${person.student.semester}° semestre`
        : null,
    ].filter(Boolean).join(' · ')
  }, [person])

  async function handleSearch() {
    const value = identifier.trim()
    if (!value) {
      setError(`Escribe tu ${config.identifierLabel.toLowerCase()}.`)
      return
    }

    setLoading(true)
    setError(null)
    setPerson(null)
    setOpenAccess(null)

    try {
      const result = await getPersonByIdentifier(value)

      if (result.type.code !== config.code) {
        throw new Error(
          `El identificador corresponde al perfil "${result.type.name}", no a ${config.label}.`,
        )
      }

      const open = await getOpenAccessRecords()

      setPerson(result)
      setOpenAccess(
        open.find((record) => record.person.id === result.id) || null,
      )
    } catch (err) {
      if (err instanceof Error && !('response' in err)) {
        setError(err.message)
      } else {
        setError(
          apiErrorMessage(
            err,
            `No encontramos un ${config.singular} con ese identificador.`,
          ),
        )
      }
    } finally {
      setLoading(false)
    }
  }

  async function handleCheckIn() {
    if (!person) return

    setLoading(true)
    setError(null)

    try {
      const result = await checkIn({
        personId: person.id,
        visitReasonId: reasonId === '' ? null : reasonId,
      })
      setSuccess(result)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  function reset() {
    setIdentifier('')
    setPerson(null)
    setOpenAccess(null)
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
    <Box sx={{ maxWidth: 760, mx: 'auto' }}>
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
          <AppIcon name={config.icon} size={29} />
        </Box>

        <Typography
          variant="h3"
          sx={{
            color: tecPalette.blue[900],
            mt: 2.5,
            fontSize: { xs: 32, sm: 40 },
          }}
        >
          Acceso de {config.label.toLowerCase()}
        </Typography>

        <Typography
          sx={{ mt: 1, color: tecPalette.gray[600], lineHeight: 1.6 }}
        >
          {config.description}
        </Typography>

        <Box
          sx={{
            mt: 4,
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: '1fr auto' },
            gap: 1.5,
          }}
        >
          <TextField
            label={config.identifierLabel}
            placeholder={config.identifierPlaceholder}
            value={identifier}
            autoFocus
            onChange={(event) => {
              setIdentifier(event.target.value)
              setPerson(null)
              setOpenAccess(null)
              setError(null)
            }}
            onKeyDown={(event) => {
              if (event.key === 'Enter') handleSearch()
            }}
          />
          <Button
            variant="contained"
            onClick={handleSearch}
            disabled={loading}
            startIcon={
              loading
                ? <CircularProgress size={18} color="inherit" />
                : <AppIcon name="search" size={18} />
            }
          >
            Buscar
          </Button>
        </Box>

        {error && <Alert severity="error" sx={{ mt: 2.5 }}>{error}</Alert>}

        {person && (
          <Box
            sx={{
              mt: 3.5,
              border: `1px solid ${tecPalette.blue[100]}`,
              bgcolor: tecPalette.blue[50],
              borderRadius: '22px',
              p: { xs: 2.5, sm: 3 },
            }}
          >
            <Typography
              sx={{
                color: tecPalette.gray[500],
                fontSize: 12,
                fontWeight: 800,
                textTransform: 'uppercase',
                letterSpacing: '.1em',
              }}
            >
              Datos encontrados
            </Typography>
            <Typography variant="h5" sx={{ mt: 1 }}>
              {person.fullName}
            </Typography>
            <Typography sx={{ color: tecPalette.gray[600], mt: .5 }}>
              {person.type.name} · {person.institutionalIdentifier}
            </Typography>
            {studentInfo && (
              <Typography sx={{ color: tecPalette.gray[600], mt: .4 }}>
                {studentInfo}
              </Typography>
            )}

            {openAccess ? (
              <Alert severity="info" sx={{ mt: 2.5 }}>
                Ya tienes una entrada activa. Para continuar debes registrar
                tu salida.
                <Button
                  component={Link}
                  to="/salida"
                  size="small"
                  sx={{ ml: 1 }}
                >
                  Ir a salida
                </Button>
              </Alert>
            ) : (
              <>
                <FormControl fullWidth sx={{ mt: 3 }}>
                  <InputLabel id="reason-label">
                    Motivo de visita (opcional)
                  </InputLabel>
                  <Select
                    labelId="reason-label"
                    label="Motivo de visita (opcional)"
                    value={reasonId}
                    onChange={(event) =>
                      setReasonId(event.target.value as number | '')
                    }
                  >
                    <MenuItem value="">Sin especificar</MenuItem>
                    {reasons.map((reason) => (
                      <MenuItem key={reason.id} value={reason.id}>
                        {reason.name}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>

                <Button
                  variant="contained"
                  size="large"
                  fullWidth
                  sx={{ mt: 2 }}
                  onClick={handleCheckIn}
                  disabled={loading}
                  startIcon={<AppIcon name="login" size={20} />}
                >
                  Registrar mi entrada
                </Button>
              </>
            )}
          </Box>
        )}
      </Box>
    </Box>
  )
}
