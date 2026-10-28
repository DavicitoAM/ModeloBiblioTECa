import { Box, Typography } from '@mui/material'
import { tecPalette } from '../../theme/tokens'

interface Props {
  title: string
  description: string
}

export default function AdminPlaceholderPage({
  title,
  description,
}: Props) {
  return (
    <Box>
      <Typography variant="h3" sx={{ color: tecPalette.blue[900] }}>
        {title}
      </Typography>
      <Typography sx={{ mt: 1, color: tecPalette.gray[600] }}>
        {description}
      </Typography>
    </Box>
  )
}
