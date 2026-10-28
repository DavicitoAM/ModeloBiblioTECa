import {
  Box,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { getRecentAccessRecords } from '../../api/library'
import AppIcon from '../../components/AppIcon'

function formatDateTime(value: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('es-MX', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

export default function RecordsPage() {
  const query = useQuery({ queryKey: ['access-records', 'recent'], queryFn: getRecentAccessRecords })
  const [search, setSearch] = useState('')
  const [type, setType] = useState('ALL')
  const [status, setStatus] = useState('ALL')

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase()
    return (query.data ?? []).filter((record) => {
      const textMatch = !term || `${record.person.fullName} ${record.person.institutionalIdentifier ?? ''} ${record.reason?.name ?? ''}`.toLowerCase().includes(term)
      const typeMatch = type === 'ALL' || record.person.typeCode === type
      const statusMatch = status === 'ALL' || (status === 'OPEN' ? record.open : !record.open)
      return textMatch && typeMatch && statusMatch
    })
  }, [query.data, search, status, type])

  return (
    <Stack spacing={2.4}>
      <Box>
        <Typography variant="overline" sx={{ color: 'primary.main', fontWeight: 850, letterSpacing: '.12em' }}>
          TRAZABILIDAD
        </Typography>
        <Typography variant="h3" sx={{ fontSize: { xs: 29, md: 36 } }}>
          Historial de accesos
        </Typography>
        <Typography color="text.secondary" sx={{ mt: .8 }}>
          Consulta las últimas entradas y salidas registradas por el sistema.
        </Typography>
      </Box>

      <Card>
        <CardContent sx={{ p: 2.3 }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1.6fr .7fr .7fr' }, gap: 1.2 }}>
            <TextField placeholder="Buscar nombre, identificador o motivo" value={search} onChange={(e) => setSearch(e.target.value)} />
            <TextField select label="Perfil" value={type} onChange={(e) => setType(e.target.value)}>
              <MenuItem value="ALL">Todos</MenuItem>
              <MenuItem value="STUDENT">Estudiantes</MenuItem>
              <MenuItem value="TEACHER">Docentes</MenuItem>
              <MenuItem value="STAFF">Personal</MenuItem>
              <MenuItem value="VISITOR">Visitantes</MenuItem>
            </TextField>
            <TextField select label="Estado" value={status} onChange={(e) => setStatus(e.target.value)}>
              <MenuItem value="ALL">Todos</MenuItem>
              <MenuItem value="OPEN">Dentro</MenuItem>
              <MenuItem value="CLOSED">Salida registrada</MenuItem>
            </TextField>
          </Box>
        </CardContent>
      </Card>

      <Card>
        <CardContent sx={{ p: 0 }}>
          {query.isLoading ? (
            <Box sx={{ py: 8, display: 'grid', placeItems: 'center' }}><CircularProgress /></Box>
          ) : filtered.length === 0 ? (
            <Box sx={{ py: 7, textAlign: 'center', color: 'text.secondary' }}>No hay registros para los filtros seleccionados.</Box>
          ) : (
            <Box sx={{ overflowX: 'auto' }}>
              <Box component="table" sx={{ width: '100%', minWidth: 900, borderCollapse: 'collapse' }}>
                <Box component="thead" sx={{ bgcolor: '#F5F7F9' }}>
                  <Box component="tr">
                    {['Persona', 'Perfil', 'Entrada', 'Salida', 'Motivo / destino', 'Estado'].map((head) => (
                      <Box component="th" key={head} sx={{ px: 2.2, py: 1.5, textAlign: 'left', fontSize: 11, color: '#7B8794', letterSpacing: '.06em' }}>{head}</Box>
                    ))}
                  </Box>
                </Box>
                <Box component="tbody">
                  {filtered.map((record) => (
                    <Box component="tr" key={record.id} sx={{ borderTop: '1px solid #EDF0F3' }}>
                      <Box component="td" sx={{ px: 2.2, py: 1.7 }}>
                        <Stack direction="row" spacing={1.2} sx={{ alignItems: 'center' }}>
                          <Box sx={{ width: 36, height: 36, borderRadius: 2, bgcolor: '#EDF2F7', color: 'primary.main', display: 'grid', placeItems: 'center' }}><AppIcon name="people" size={18} /></Box>
                          <Box>
                            <Typography sx={{ fontWeight: 750, fontSize: 13.5 }}>{record.person.fullName}</Typography>
                            <Typography variant="caption" color="text.secondary">{record.person.institutionalIdentifier ?? 'Sin identificador'}</Typography>
                          </Box>
                        </Stack>
                      </Box>
                      <Box component="td" sx={{ px: 2.2, py: 1.7, fontSize: 13 }}>{record.person.typeName}</Box>
                      <Box component="td" sx={{ px: 2.2, py: 1.7, fontSize: 13 }}>{formatDateTime(record.checkedInAt)}</Box>
                      <Box component="td" sx={{ px: 2.2, py: 1.7, fontSize: 13 }}>{formatDateTime(record.checkedOutAt)}</Box>
                      <Box component="td" sx={{ px: 2.2, py: 1.7 }}>
                        <Typography sx={{ fontSize: 13 }}>{record.reason?.name ?? '—'}</Typography>
                        <Typography variant="caption" color="text.secondary">{record.destination ?? ''}</Typography>
                      </Box>
                      <Box component="td" sx={{ px: 2.2, py: 1.7 }}>
                        <Chip
                          size="small"
                          label={record.open ? 'Dentro' : 'Finalizado'}
                          sx={{ bgcolor: record.open ? '#E8EEF6' : '#F0F1F2', color: record.open ? 'primary.main' : 'secondary.dark' }}
                        />
                      </Box>
                    </Box>
                  ))}
                </Box>
              </Box>
            </Box>
          )}
        </CardContent>
      </Card>
    </Stack>
  )
}
