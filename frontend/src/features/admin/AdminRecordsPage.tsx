import {
  Box,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { useEffect, useState } from 'react'
import { getRecentAccessRecords } from '../../api/library'
import type { AccessRecord } from '../../api/types'
import { tecPalette } from '../../theme/tokens'

export default function AdminRecordsPage() {
  const [records, setRecords] = useState<AccessRecord[] | null>(null)

  useEffect(() => {
    getRecentAccessRecords().then(setRecords).catch(() => setRecords([]))
  }, [])

  return (
    <Box>
      <Typography variant="h3" sx={{ color: tecPalette.blue[900] }}>
        Registros
      </Typography>
      <Typography sx={{ color: tecPalette.gray[600], mt: 1, mb: 3 }}>
        Consulta administrativa de los accesos recientes.
      </Typography>

      {records === null ? (
        <CircularProgress />
      ) : (
        <Box
          sx={{
            overflow: 'auto',
            bgcolor: tecPalette.white,
            borderRadius: '22px',
            border: `1px solid ${tecPalette.blue[100]}`,
          }}
        >
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Persona</TableCell>
                <TableCell>Tipo</TableCell>
                <TableCell>Entrada</TableCell>
                <TableCell>Salida</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {records.map((record) => (
                <TableRow key={record.id}>
                  <TableCell>{record.person.fullName}</TableCell>
                  <TableCell>{record.person.typeName}</TableCell>
                  <TableCell>
                    {new Date(record.checkedInAt).toLocaleString('es-MX')}
                  </TableCell>
                  <TableCell>
                    {record.checkedOutAt
                      ? new Date(record.checkedOutAt).toLocaleString('es-MX')
                      : 'Dentro'}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      )}
    </Box>
  )
}
