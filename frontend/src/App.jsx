import { Routes, Route, Link } from 'react-router-dom'
import Home from './pages/Home'
import PlayerDetail from './pages/PlayerDetail'
import NotFound from './pages/NotFound'

function App() {
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <header style={{
        background: '#1a1a1a',
        color: '#fff',
        padding: '0.75rem 1.5rem',
        boxShadow: '0 1px 3px rgba(0,0,0,0.2)',
      }}>
        <Link to="/" style={{ color: '#fff', textDecoration: 'none', fontWeight: 700, fontSize: '1.25rem' }}>
          NBA Player Analytics
        </Link>
      </header>
      <main style={{ flex: 1, padding: '1.5rem', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/players/:id" element={<PlayerDetail />} />
          <Route path="*" element={<NotFound />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
