import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  Divider,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import {
  checkIn,
  createVisitor,
  findPersonByIdentifier,
  getApiErrorMessage,
  getPersonTypes,
  getVisitReasons,
} from '../../api/library'
import type { AccessRecord, Person, PersonType } from '../../api/types'
import AppIcon, { type AppIconName } from '../../components/AppIcon'

const iconByType: Record<string, AppIconName> = {
  STUDENT: 'student',
  TEACHER: 'teacher',
  STAFF: 'staff',
  VISITOR: 'visitor',
}

const helperByType: Record<string, string> = {
  STUDENT: 'Número de control',
  TEACHER: 'Clave o número de empleado',
  STAFF: 'Clave institucional',
  VISITOR: 'Captura manual de datos',
}

function PersonTypeCard({
  type,
  selected,
  onSelect,
}: {
  type: PersonType
  selected: boolean
  onSelect: () => void
}) {
  return (
    <Card
      component="button"
      type="button"
      onClick={onSelect}
      sx={{
        width: '100%',
        p: 0,
        textAlign: 'left',
        cursor: 'pointer',
        font: 'inherit',
        borderWidth: selected ? 2 : 1,
        borderColor: selected ? 'primary.main' : '#E2E7ED',
        bgcolor: selected ? '#F1F5FA' : 'background.paper',
        transition: 'transform .18s ease, border-color .18s ease, box-shadow .18s ease',
        '&:hover': {
          transform: 'translateY(-2px)',
          borderColor: 'primary.main',
          boxShadow: '0 15px 35px rgba(27,57,106,.12)',
        },
      }}
    >
      <CardContent sx={{ p: 2.2, '&:last-child': { pb: 2.2 } }}>
        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
          <Box
            sx={{
              width: 44,
              height: 44,
              borderRadius: 2.5,
              display: 'grid',
              placeItems: 'center',
              bgcolor: selected ? 'primary.main' : '#EBF0F6',
              color: selected ? 'common.white' : 'primary.main',
            }}
          >
            <AppIcon name={iconByType[type.code] ?? 'people'} />
          </Box>
          <Box sx={{ minWidth: 0 }}>
            <Typography sx={{ fontWeight: 800 }}>{type.name}</Typography>
            <Typography variant="caption" color="text.secondary">
              {helperByType[type.code] ?? type.description}
            </Typography>
          </Box>
        </Stack>
      </CardContent>
    </Card>
  )
}

export default function CheckInPage() {
  const queryClient = useQueryClient()
  const typesQuery = useQuery({ queryKey: ['person-types'], queryFn: getPersonTypes })
  const reasonsQuery = useQuery({ queryKey: ['visit-reasons'], queryFn: getVisitReasons })

  const [selectedType, setSelectedType] = useState<string | null>(null)
  const [identifier, setIdentifier] = useState('')
  const [person, setPerson] = useState<Person | null>(null)
  const [reasonId, setReasonId] = useState<number | null>(null)
  const [destination, setDestination] = useState('')
  const [notes, setNotes] = useState('')
  const [visitor, setVisitor] = useState({
    firstName: '',
    paternalSurname: '',
    maternalSurname: '',
    email: '',
    phone: '',
  })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<AccessRecord | null>(null)

  const selectedTypeInfo = useMemo(
    () => typesQuery.data?.find((type) => type.code === selectedType) ?? null,
    [selectedType, typesQuery.data],
  )
  const isVisitor = selectedType === 'VISITOR'

  function chooseType(type: string) {
    setSelectedType(type)
    setPerson(null)
    setIdentifier('')
    setReasonId(null)
    setDestination('')
    setNotes('')
    setError(null)
    setSuccess(null)
  }

  async function handleSearch() {
    if (!selectedType || !identifier.trim()) return
    setBusy(true)
    setError(null)
    try {
      const result = await findPersonByIdentifier(identifier)
      if (result.type.code !== selectedType) {
        setPerson(null)
        setError(`El identificador pertenece a ${result.type.name.toLowerCase()}, no al perfil seleccionado.`)
        return
      }
      setPerson(result)
    } catch (caught) {
      setPerson(null)
      setError(getApiErrorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function handleInstitutionalCheckIn() {
    if (!person) return
    setBusy(true)
    setError(null)
    try {
      const result = await checkIn({
        personId: person.id,
        visitReasonId: reasonId ?? undefined,
        notes: notes.trim() || undefined,
      })
      setSuccess(result)
      await queryClient.invalidateQueries({ queryKey: ['access-records'] })
    } catch (caught) {
      setError(getApiErrorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function handleVisitorCheckIn() {
    if (!visitor.firstName.trim() || !visitor.paternalSurname.trim() || !reasonId || !destination.trim()) {
      setError('Completa nombre, apellido paterno, motivo y destino del visitante.')
      return
    }
    setBusy(true)
    setError(null)
    try {
      const created = await createVisitor({
        firstName: visitor.firstName.trim(),
        paternalSurname: visitor.paternalSurname.trim(),
        maternalSurname: visitor.maternalSurname.trim() || undefined,
        email: visitor.email.trim() || undefined,
        phone: visitor.phone.trim() || undefined,
      })
      const result = await checkIn({
        personId: created.id,
        visitReasonId: reasonId,
        destination: destination.trim(),
        notes: notes.trim() || undefined,
      })
      setSuccess(result)
      await queryClient.invalidateQueries({ queryKey: ['access-records'] })
    } catch (caught) {
      setError(getApiErrorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  function reset() {
    setSelectedType(null)
    setIdentifier('')
    setPerson(null)
    setReasonId(null)
    setDestination('')
    setNotes('')
    setVisitor({ firstName: '', paternalSurname: '', maternalSurname: '', email: '', phone: '' })
    setError(null)
    setSuccess(null)
  }

  if (success) {
    return (
      <Box sx={{ maxWidth: 720, mx: 'auto', py: { md: 4 } }}>
        <Card>
          <CardContent sx={{ p: { xs: 3, md: 5 }, textAlign: 'center' }}>
            <Box
              sx={{
                width: 68,
                height: 68,
                mx: 'auto',
                borderRadius: '50%',
                bgcolor: '#E8EEF6',
                color: 'primary.main',
                display: 'grid',
                placeItems: 'center',
              }}
            >
              <AppIcon name="check" size={34} />
            </Box>
            <Typography variant="h3" sx={{ fontSize: 34, mt: 2.3 }}>
              Entrada registrada
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              El acceso quedó guardado correctamente.
            </Typography>

            <Box sx={{ mt: 3, p: 2.4, borderRadius: 3, bgcolor: '#F3F6F9', textAlign: 'left' }}>
              <Typography sx={{ fontWeight: 800 }}>{success.person.fullName}</Typography>
              <Typography variant="body2" color="text.secondary">
                {success.person.typeName}
                {success.person.institutionalIdentifier
                  ? ` · ${success.person.institutionalIdentifier}`
                  : ''}
              </Typography>
              {success.reason && (
                <Typography variant="body2" sx={{ mt: 1.2 }}>
                  Motivo: <strong>{success.reason.name}</strong>
                </Typography>
              )}
              {success.destination && (
                <Typography variant="body2">
                  Destino: <strong>{success.destination}</strong>
                </Typography>
              )}
            </Box>

            <Button variant="contained" size="large" sx={{ mt: 3 }} onClick={reset}>
              Registrar otra entrada
            </Button>
          </CardContent>
        </Card>
      </Box>
    )
  }

  return (
    <Stack spacing={2.4}>
      <Box>
        <Typography variant="overline" sx={{ color: 'primary.main', fontWeight: 850, letterSpacing: '.12em' }}>
          NUEVO ACCESO
        </Typography>
        <Typography variant="h3" sx={{ fontSize: { xs: 29, md: 36 } }}>
          ¿Quién está ingresando?
        </Typography>
        <Typography color="text.secondary" sx={{ mt: .8 }}>
          Selecciona el perfil para mostrar únicamente la captura que corresponde.
        </Typography>
      </Box>

      {error && <Alert severity="error" onClose={() => setError(null)}>{error}</Alert>}

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(2,1fr)', xl: 'repeat(4,1fr)' },
          gap: 1.6,
        }}
      >
        {(typesQuery.data ?? []).map((type) => (
          <PersonTypeCard
            key={type.id}
            type={type}
            selected={selectedType === type.code}
            onSelect={() => chooseType(type.code)}
          />
        ))}
      </Box>

      {typesQuery.isLoading && (
        <Box sx={{ py: 3, display: 'flex', justifyContent: 'center' }}><CircularProgress size={28} /></Box>
      )}

      {selectedType && (
        <Card>
          <CardContent sx={{ p: { xs: 2.4, md: 3.2 } }}>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center', mb: 2.6 }}>
              <Box
                sx={{
                  width: 46,
                  height: 46,
                  borderRadius: 2.5,
                  display: 'grid',
                  placeItems: 'center',
                  bgcolor: '#EAF0F6',
                  color: 'primary.main',
                }}
              >
                <AppIcon name={iconByType[selectedType] ?? 'people'} />
              </Box>
              <Box>
                <Typography variant="h6">{selectedTypeInfo?.name}</Typography>
                <Typography variant="caption" color="text.secondary">
                  {selectedTypeInfo?.description}
                </Typography>
              </Box>
            </Stack>

            {!isVisitor ? (
              <Stack spacing={2.3}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.2}>
                  <TextField
                    fullWidth
                    label={helperByType[selectedType] ?? 'Identificador institucional'}
                    value={identifier}
                    disabled={Boolean(person) || busy}
                    onChange={(event) => setIdentifier(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' && !person) void handleSearch()
                    }}
                  />
                  {!person && (
                    <Button
                      variant="contained"
                      startIcon={<AppIcon name="search" size={18} />}
                      disabled={!identifier.trim() || busy}
                      onClick={() => void handleSearch()}
                      sx={{ minWidth: 130 }}
                    >
                      Buscar
                    </Button>
                  )}
                </Stack>

                {person && (
                  <>
                    <Box
                      sx={{
                        p: 2.2,
                        borderRadius: 3,
                        bgcolor: '#F2F5F8',
                        border: '1px solid #E3E8ED',
                      }}
                    >
                      <Stack direction={{ xs: 'column', sm: 'row' }} sx={{ justifyContent: 'space-between', gap: 1.5 }}>
                        <Box>
                          <Typography sx={{ fontWeight: 850, fontSize: 18 }}>{person.fullName}</Typography>
                          <Typography variant="body2" color="text.secondary">
                            {person.type.name} · {person.institutionalIdentifier}
                          </Typography>
                          {person.student && (
                            <Typography variant="body2" color="text.secondary" sx={{ mt: .5 }}>
                              {person.student.careerName ?? 'Carrera no asignada'}
                              {person.student.semester ? ` · ${person.student.semester}.º semestre` : ''}
                            </Typography>
                          )}
                        </Box>
                        <Button
                          size="small"
                          onClick={() => {
                            setPerson(null)
                            setReasonId(null)
                          }}
                        >
                          Cambiar persona
                        </Button>
                      </Stack>
                    </Box>

                    <Divider />

                    <Box>
                      <Typography sx={{ fontWeight: 800 }}>Motivo de visita</Typography>
                      <Typography variant="body2" color="text.secondary">
                        Es opcional para perfiles institucionales.
                      </Typography>
                      <Box
                        sx={{
                          display: 'grid',
                          gridTemplateColumns: { xs: '1fr', sm: 'repeat(2,1fr)', lg: 'repeat(3,1fr)' },
                          gap: 1,
                          mt: 1.5,
                        }}
                      >
                        {(reasonsQuery.data ?? []).map((reason) => (
                          <Button
                            key={reason.id}
                            variant={reasonId === reason.id ? 'contained' : 'outlined'}
                            onClick={() => setReasonId(reasonId === reason.id ? null : reason.id)}
                            sx={{ justifyContent: 'flex-start', textAlign: 'left' }}
                          >
                            {reason.name}
                          </Button>
                        ))}
                      </Box>
                    </Box>

                    <TextField
                      label="Observaciones (opcional)"
                      value={notes}
                      multiline
                      minRows={2}
                      onChange={(event) => setNotes(event.target.value)}
                    />

                    <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                      <Button
                        variant="contained"
                        size="large"
                        startIcon={busy ? <CircularProgress size={18} color="inherit" /> : <AppIcon name="login" size={18} />}
                        disabled={busy}
                        onClick={() => void handleInstitutionalCheckIn()}
                      >
                        Registrar entrada
                      </Button>
                    </Box>
                  </>
                )}
              </Stack>
            ) : (
              <Stack spacing={2.3}>
                <Alert severity="info">
                  Este flujo registra un visitante nuevo. La búsqueda de visitantes existentes requerirá un endpoint adicional en el backend.
                </Alert>
                <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2,1fr)' }, gap: 1.5 }}>
                  <TextField label="Nombre *" value={visitor.firstName} onChange={(e) => setVisitor({ ...visitor, firstName: e.target.value })} />
                  <TextField label="Apellido paterno *" value={visitor.paternalSurname} onChange={(e) => setVisitor({ ...visitor, paternalSurname: e.target.value })} />
                  <TextField label="Apellido materno" value={visitor.maternalSurname} onChange={(e) => setVisitor({ ...visitor, maternalSurname: e.target.value })} />
                  <TextField label="Teléfono" value={visitor.phone} onChange={(e) => setVisitor({ ...visitor, phone: e.target.value })} />
                  <TextField label="Correo electrónico" type="email" value={visitor.email} onChange={(e) => setVisitor({ ...visitor, email: e.target.value })} sx={{ gridColumn: { md: '1 / -1' } }} />
                </Box>

                <Divider />

                <Box>
                  <Stack direction="row" sx={{ alignItems: 'center', gap: 1, mb: 1.3 }}>
                    <Typography sx={{ fontWeight: 800 }}>Motivo de visita *</Typography>
                    {reasonId && <Chip size="small" label="Seleccionado" />}
                  </Stack>
                  <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2,1fr)', lg: 'repeat(3,1fr)' }, gap: 1 }}>
                    {(reasonsQuery.data ?? []).map((reason) => (
                      <Button
                        key={reason.id}
                        variant={reasonId === reason.id ? 'contained' : 'outlined'}
                        onClick={() => setReasonId(reason.id)}
                        sx={{ justifyContent: 'flex-start' }}
                      >
                        {reason.name}
                      </Button>
                    ))}
                  </Box>
                </Box>

                <TextField label="Destino o área que visita *" value={destination} onChange={(e) => setDestination(e.target.value)} />
                <TextField label="Observaciones" value={notes} multiline minRows={2} onChange={(e) => setNotes(e.target.value)} />

                <Box sx={{ display: 'flex', justifyContent: 'flex-end' }}>
                  <Button
                    variant="contained"
                    size="large"
                    disabled={busy}
                    startIcon={busy ? <CircularProgress size={18} color="inherit" /> : <AppIcon name="login" size={18} />}
                    onClick={() => void handleVisitorCheckIn()}
                  >
                    Registrar visitante
                  </Button>
                </Box>
              </Stack>
            )}
          </CardContent>
        </Card>
      )}
    </Stack>
  )
}
