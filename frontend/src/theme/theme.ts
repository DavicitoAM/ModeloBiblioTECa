import { createTheme } from '@mui/material/styles'
import { radius, tecPalette } from './tokens'

export const theme = createTheme({
  palette: {
    primary: {
      main: tecPalette.blue[600],
      dark: tecPalette.blue[800],
      light: tecPalette.blue[300],
      contrastText: tecPalette.white,
    },
    secondary: {
      main: tecPalette.gray[500],
    },
    background: {
      default: tecPalette.blue[50],
      paper: tecPalette.white,
    },
    text: {
      primary: tecPalette.gray[900],
      secondary: tecPalette.gray[600],
    },
    success: {
      main: tecPalette.success,
    },
    warning: {
      main: tecPalette.warning,
    },
    error: {
      main: tecPalette.danger,
    },
  },
  typography: {
    fontFamily: '"Noto Sans", "Segoe UI", Arial, sans-serif',
    h1: {
      fontFamily: '"Patria", "Noto Sans", "Segoe UI", sans-serif',
      fontWeight: 700,
      letterSpacing: '-0.03em',
    },
    h2: {
      fontFamily: '"Patria", "Noto Sans", "Segoe UI", sans-serif',
      fontWeight: 700,
      letterSpacing: '-0.025em',
    },
    h3: {
      fontFamily: '"Patria", "Noto Sans", "Segoe UI", sans-serif',
      fontWeight: 700,
      letterSpacing: '-0.02em',
    },
    h4: {
      fontFamily: '"Patria", "Noto Sans", "Segoe UI", sans-serif',
      fontWeight: 700,
    },
    h5: {
      fontWeight: 750,
    },
    h6: {
      fontWeight: 750,
    },
    button: {
      textTransform: 'none',
      fontWeight: 750,
      letterSpacing: 0,
    },
  },
  shape: {
    borderRadius: radius.md,
  },
  components: {
    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          minHeight: 46,
          borderRadius: 14,
          paddingInline: 20,
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: radius.lg,
          border: `1px solid ${tecPalette.blue[100]}`,
          boxShadow: '0 16px 40px rgba(13, 29, 54, 0.07)',
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
      },
    },
  },
})
