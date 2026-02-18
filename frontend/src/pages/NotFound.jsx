import { useEffect } from 'react'
import { Link } from 'react-router-dom'

export default function NotFound() {
  useEffect(() => {
    document.title = 'Page not found – NBA Player Analytics'
    return () => { document.title = 'NBA Player Analytics' }
  }, [])
  return (
    <div style={{ textAlign: 'center', padding: '2rem 1rem' }}>
      <h1 style={{ fontSize: '1.5rem', marginTop: 0, marginBottom: '0.5rem' }}>Page not found</h1>
      <p style={{ color: '#666', marginBottom: '1.5rem' }}>The page you’re looking for doesn’t exist or has been moved.</p>
      <Link
        to="/"
        style={{
          display: 'inline-block',
          padding: '0.5rem 1rem',
          background: '#c41e3a',
          color: '#fff',
          borderRadius: 6,
          fontWeight: 600,
          textDecoration: 'none',
        }}
      >
        Back to search
      </Link>
    </div>
  )
}
