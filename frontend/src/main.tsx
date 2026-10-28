import ReactDOM from 'react-dom/client'
import { HashRouter } from 'react-router-dom'
import '@fontsource/noto-sans/latin-400.css'
import '@fontsource/noto-sans/latin-500.css'
import '@fontsource/noto-sans/latin-600.css'
import '@fontsource/noto-sans/latin-700.css'
import { SessionProvider } from './lib/session'
import App from './App'
import { preparePlugins } from './lib/plugins'
import './design.css'

preparePlugins().catch(error => console.error('Complementos de interfaz:', error)).finally(() => {
  ReactDOM.createRoot(document.getElementById('root')!).render(<HashRouter><SessionProvider><App /></SessionProvider></HashRouter>)
})
