import {
  Alert,
  Box,
  Button,
  ButtonBase,
  CircularProgress,
  TextField,
  Typography,
} from '@mui/material'
import { useState } from 'react'
import {
  checkOut,
  getOpenAccessRecords,
  getPersonByIdentifier,
} from '../../api/library'
import { apiErrorMessage } from '../../api/errors'
import type { AccessRecord } from '../../api/types'
import AppIcon from '../../components/AppIcon'
import { tecPalette } from '../../theme/tokens'
import AccessSuccess from './AccessSuccess'

type Mode = 'institutional' | 'visitor'

export default function CheckOutPage() {
  const [mode, setMode] = useState<Mode>('institutional')
  const [query, setQuery] = useState('')
  const [matches, setMatches] = useState<AccessRecord[]>([])
  const [selected, setSelected] = useState<AccessRecord | null>(null)
  const [success, setSuccess] = useState<AccessRecord | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSearch() {
    const value = query.trim()
    if (!value) {
      setError(
        mode === 'institutional'
          ? 'Escribe tu identificador institucional.'
          : 'Escribe tu nombre para localizar tu entrada.',
      )
      return
    }

    setLoading(true)
    setError(null)
    setMatches([])
    setSelected(null)

    try {
      const open = await getOpenAccessRecords()

      if (mode === 'institutional') {
        const person = await getPersonByIdentifier(value)
        const found = open.find((record) => record.person.id === person.id)

        if (!found) {
          setError('No encontramos una entrada abierta para esta persona.')
          return
        }

        setMatches([found])
        setSelected(found)
      } else {
        if (value.length < 4) {
          setError('Escribe al menos 4 caracteres de tu nombre.')
          return
        }

        const normalized = value.toLocaleLowerCase('es-MX')
        const found = open.filter(
          (record) =>
            record.person.typeCode === 'VISITOR' &&
            record.person.fullName
              .toLocaleLowerCase('es-MX')
              .includes(normalized),
        )

        if (found.length === 0) {
          setError('No encontramos una visita abierta con ese nombre.')
          return
        }

        setMatches(found)
        if (found.length === 1) setSelected(found[0])
      }
    } catch (err) {
      setError(apiErrorMessage(err, 'No fue posible localizar tu entrada.'))
    } finally {
      setLoading(false)
    }
  }

  async function handleCheckOut() {
    if (!selected) return

    setLoading(true)
    setError(null)

    try {
      const result = await checkOut(selected.id)
      setSuccess(result)
    } catch (err) {
      setError(apiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  function reset() {
    setQuery('')
    setMatches([])
    setSelected(null)
    setSuccess(null)
    setError(null)
  }

  if (success) {
    return (
      <AccessSuccess
        access={success}
        action="check-out"
        onAnother={reset}
      />
    )
  }

  return (
    <Box sx={{ maxWidth: 780, mx: 'auto' }}>
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
          <AppIcon name="logout" size={29} />
        </Box>

        <Typography
          variant="h3"
          sx={{
            mt: 2.5,
            color: tecPalette.blue[900],
            fontSize: { xs: 32, sm: 40 },
          }}
        >
          Registrar salida
        </Typography>

        <Typography
          sx={{ mt: 1, color: tecPalette.gray[600], lineHeight: 1.6 }}
        >
          Identifícate para localizar únicamente tu entrada abierta.
        </Typography>

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(2, 1fr)',
            gap: 1,
            bgcolor: tecPalette.blue[50],
            p: .8,
            borderRadius: '16px',
            mt: 3,
          }}
        >
          {([
            ['institutional', 'Comunidad institucional'],
            ['visitor', 'Visitante'],
          ] as Array<[Mode, string]>).map(([value, label]) => (
            <ButtonBase
              key={value}
              onClick={() => {
                setMode(value)
                setQuery('')
                setMatches([])
                setSelected(null)
                setError(null)
              }}
              sx={{
                borderRadius: '12px',
                py: 1.2,
                px: 1,
                bgcolor:
                  mode === value ? tecPalette.white : 'transparent',
                color:
                  mode === value
                    ? tecPalette.blue[700]
                    : tecPalette.gray[600],
                boxShadow:
                  mode === value
                    ? '0 4px 14px rgba(13,29,54,.08)'
                    : 'none',
                fontWeight: 750,
              }}
            >
              {label}
            </ButtonBase>
          ))}
        </Box>

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: '1fr auto' },
            gap: 1.5,
            mt: 3,
          }}
        >
          <TextField
            label={
              mode === 'institutional'
                ? 'Número de control o clave institucional'
                : 'Nombre del visitante'
            }
            value={query}
            autoFocus
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') handleSearch()
            }}
          />
          <Button
            variant="contained"
            onClick={handleSearch}
            disabled={loading}
            startIcon={
              loading
                ? <CircularProgress color="inherit" size={18} />
                : <AppIcon name="search" size={18} />
            }
          >
            Buscar
          </Button>
        </Box>

        {error && <Alert severity="error" sx={{ mt: 2.5 }}>{error}</Alert>}

        {matches.length > 1 && (
          <Box sx={{ mt: 3 }}>
            <Typography sx={{ fontWeight: 750, mb: 1.5 }}>
              Selecciona tu registro
            </Typography>
            <Box sx={{ display: 'grid', gap: 1.2 }}>
              {matches.map((record) => (
                <ButtonBase
                  key={record.id}
                  onClick={() => setSelected(record)}
                  sx={{ display: 'block', textAlign: 'left', borderRadius: '16px' }}
                >
                  <Box
                    sx={{
                      p: 2,
                      borderRadius: '16px',
                      border: `1px solid ${
                        selected?.id === record.id
                          ? tecPalette.blue[600]
                          : tecPalette.blue[100]
                      }`,
                      bgcolor:
                        selected?.id === record.id
                          ? tecPalette.blue[50]
                          : tecPalette.white,
                    }}
                  >
                    <Typography sx={{ fontWeight: 750 }}>
                      {record.person.fullName}
                    </Typography>
                    <Typography sx={{ fontSize: 13, color: tecPalette.gray[600] }}>
                      Entrada {new Date(record.checkedInAt).toLocaleString('es-MX')}
                    </Typography>
                  </Box>
                </ButtonBase>
              ))}
            </Box>
          </Box>
        )}

        {selected && (
          <Box
            sx={{
              mt: 3,
              p: 2.5,
              borderRadius: '20px',
              bgcolor: tecPalette.blue[50],
              border: `1px solid ${tecPalette.blue[100]}`,
            }}
          >
            <Typography variant="h6">{selected.person.fullName}</Typography>
            <Typography sx={{ mt: .4, color: tecPalette.gray[600] }}>
              {selected.person.typeName} · Entrada:{' '}
              {new Date(selected.checkedInAt).toLocaleString('es-MX')}
            </Typography>

            <Button
              variant="contained"
              fullWidth
              sx={{ mt: 2.5 }}
              onClick={handleCheckOut}
              disabled={loading}
              startIcon={<AppIcon name="logout" size={20} />}
            >
              Confirmar mi salida
            </Button>
          </Box>
        )}
      </Box>
    </Box>
  )
}
