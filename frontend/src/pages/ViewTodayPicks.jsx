import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'

export default function ViewTodayPicks() {
  const [picks, setPicks] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.getTodayPicks(null, 30).then(setPicks).catch((err) => { setError(err?.message); setPicks([]) }).finally(() => setLoading(false))
  }, [])

  return (
    <div>
      <p style={{ marginBottom: '1rem' }}>
        <Link to="/">← Back to search</Link>
        <span style={{ marginLeft: '1rem' }}><Link to="/add-lines">Add lines</Link></span>
      </p>
      <h1 style={{ marginTop: 0, marginBottom: '0.5rem', fontSize: '1.5rem' }}>Top 30 picks (most probable first)</h1>
      <p style={{ color: '#666', fontSize: '0.9rem', marginBottom: '1rem' }}>
        Top 30 lines for the latest day, sorted by confidence (High → Medium → Low).
      </p>
      {loading && <p style={{ color: '#666' }}>Loading…</p>}
      {error && <p style={{ color: '#c41e3a' }}>{error}</p>}
      {!loading && !error && picks.length === 0 && <p style={{ color: '#666' }}>No picks for the latest day. Add lines first.</p>}
      {!loading && !error && picks.length > 0 && picks.length < 30 && <p style={{ color: '#666', fontSize: '0.9rem', marginBottom: '0.5rem' }}>Showing {picks.length} pick{picks.length !== 1 ? 's' : ''} (fewer than 30 saved for this day).</p>}
      {!loading && !error && picks.length > 0 && (
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {picks.map((pick, idx) => (
            <li key={pick.id} style={{ marginBottom: '0.5rem' }}>
              <Link
                to={`/players/${pick.playerId}`}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.5rem',
                  padding: '0.6rem 0.75rem',
                  background: '#fff',
                  borderRadius: 8,
                  boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                  color: '#1a1a1a',
                  textDecoration: 'none',
                }}
              >
                <span style={{ width: 24, height: 24, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', borderRadius: '50%', background: pick.suggestion === 'Over' ? '#0a0' : '#c41e3a', color: '#fff', fontSize: 14 }}>
                  {pick.suggestion === 'Over' ? '✓' : '✕'}
                </span>
                <span style={{ fontWeight: 600, minWidth: '2rem' }}>{idx + 1}.</span>
                <span style={{ fontWeight: 600 }}>
                  {pick.playerName} {pick.suggestion} {pick.line} {pick.statLabel}
                </span>
                <span style={{ fontSize: '0.8rem', color: '#888', marginLeft: '0.25rem' }}>({pick.confidence})</span>
                <span style={{ color: '#666', fontSize: '0.9rem' }}>{pick.reason}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
