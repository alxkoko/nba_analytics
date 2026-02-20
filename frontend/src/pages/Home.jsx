import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'

const SUGGEST_DEBOUNCE_MS = 200
const MIN_QUERY_LENGTH = 2

const DOC_TITLE = 'NBA Player Analytics'

// Same CDN as PlayerDetail; images load from NBA, not your hosting
const HEADSHOT_URL = (nbaId) => `https://cdn.nba.com/headshots/nba/latest/260x190/${nbaId}.png`

function PlayerAvatar({ nbaPlayerId, size = 36 }) {
  const [show, setShow] = useState(true)
  if (!nbaPlayerId || !show) return <span style={{ width: size, height: size, flexShrink: 0 }} />
  return (
    <img
      src={HEADSHOT_URL(nbaPlayerId)}
      alt=""
      width={size}
      height={size}
      style={{ objectFit: 'cover', borderRadius: 6, background: '#eee', flexShrink: 0 }}
      onError={() => setShow(false)}
    />
  )
}

export default function Home() {
  const [query, setQuery] = useState('')
  const [players, setPlayers] = useState([])
  const [suggestions, setSuggestions] = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [loading, setLoading] = useState(false)
  const [suggestLoading, setSuggestLoading] = useState(false)
  const [error, setError] = useState(null)
  const navigate = useNavigate()
  const suggestRef = useRef(null)
  const [todayPicks, setTodayPicks] = useState([])

  useEffect(() => {
    document.title = DOC_TITLE
    return () => { document.title = DOC_TITLE }
  }, [])

  useEffect(() => {
    api.getTodayPicks().then(setTodayPicks).catch(() => setTodayPicks([]))
  }, [])

  // Debounced search-as-you-type for suggestions
  useEffect(() => {
    if (!query.trim() || query.trim().length < MIN_QUERY_LENGTH) {
      setSuggestions([])
      setShowSuggestions(false)
      setSuggestLoading(false)
      return
    }
    setSuggestLoading(true)
    setShowSuggestions(true)
    const t = setTimeout(async () => {
      try {
        const data = await api.searchPlayers(query.trim())
        const list = Array.isArray(data) ? data.slice(0, 8) : []
        setSuggestions(list)
        setShowSuggestions(list.length > 0)
      } catch {
        setSuggestions([])
        setShowSuggestions(false)
      } finally {
        setSuggestLoading(false)
      }
    }, SUGGEST_DEBOUNCE_MS)
    return () => clearTimeout(t)
  }, [query])

  // Close suggestions when clicking outside
  useEffect(() => {
    function handleClickOutside(e) {
      if (suggestRef.current && !suggestRef.current.contains(e.target)) {
        setShowSuggestions(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  async function handleSearch(e) {
    e?.preventDefault()
    if (!query.trim()) return
    setShowSuggestions(false)
    setLoading(true)
    setError(null)
    try {
      const data = await api.searchPlayers(query.trim())
      setPlayers(Array.isArray(data) ? data : [])
    } catch (err) {
      setError(err?.message || 'Something went wrong. Check your connection and try again.')
      setPlayers([])
    } finally {
      setLoading(false)
    }
  }

  function pickSuggestion(p) {
    setQuery(p.fullName)
    setSuggestions([])
    setShowSuggestions(false)
    navigate(`/players/${p.id}`)
  }

  return (
    <div>
      <h1 style={{ marginTop: 0, marginBottom: '1rem', fontSize: '1.5rem' }}>Search players</h1>
      <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', flexWrap: 'wrap' }}>
        <div ref={suggestRef} style={{ position: 'relative' }}>
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onFocus={() => query.trim().length >= MIN_QUERY_LENGTH && suggestions.length > 0 && setShowSuggestions(true)}
            placeholder="e.g. LeBron, Curry"
            style={{
              padding: '0.5rem 0.75rem',
              border: '1px solid #ccc',
              borderRadius: 6,
              minWidth: 260,
            }}
          />
          {showSuggestions && (suggestLoading ? (
            <div style={{ position: 'absolute', top: '100%', left: 0, right: 0, padding: '0.75rem 1rem', background: '#fff', border: '1px solid #ccc', borderRadius: 6, boxShadow: '0 4px 12px rgba(0,0,0,0.15)', zIndex: 10, color: '#666', fontSize: '0.9rem' }}>
              Searching…
            </div>
          ) : suggestions.length > 0 ? (
            <ul
              style={{
                position: 'absolute',
                top: '100%',
                left: 0,
                right: 0,
                margin: 0,
                padding: 0,
                listStyle: 'none',
                background: '#fff',
                border: '1px solid #ccc',
                borderRadius: 6,
                boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                zIndex: 10,
                maxHeight: 280,
                overflowY: 'auto',
              }}
            >
              {suggestions.map((p) => (
                <li key={p.id}>
                  <button
                    type="button"
                    onClick={() => pickSuggestion(p)}
                    style={{
                      width: '100%',
                      padding: '0.6rem 0.75rem',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.5rem',
                      textAlign: 'left',
                      border: 'none',
                      background: 'none',
                      cursor: 'pointer',
                      fontFamily: 'inherit',
                      fontSize: '1rem',
                    }}
                    onMouseEnter={(e) => { e.currentTarget.style.background = '#f0f0f0' }}
                    onMouseLeave={(e) => { e.currentTarget.style.background = 'none' }}
                  >
                    <PlayerAvatar nbaPlayerId={p.nbaPlayerId} size={32} />
                    {p.fullName}
                  </button>
                </li>
              ))}
            </ul>
          ) : null)}
        </div>
        <button
          type="submit"
          disabled={loading}
          style={{
            padding: '0.5rem 1rem',
            background: '#c41e3a',
            color: '#fff',
            border: 'none',
            borderRadius: 6,
            fontWeight: 600,
          }}
        >
          {loading ? 'Searching…' : 'Search'}
        </button>
      </form>

      {todayPicks.length > 0 && (
        <section style={{ marginBottom: '2rem' }}>
          <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>Today&apos;s picks</h2>
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {todayPicks.map((pick, idx) => (
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
                  <span style={{ fontWeight: 600 }}>
                    {idx + 1}. {pick.playerName} {pick.suggestion} {pick.line} {pick.statLabel}
                  </span>
                  <span style={{ color: '#666', fontSize: '0.9rem' }}>({pick.reason})</span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {error && (
        <div style={{ marginBottom: '1rem', padding: '0.75rem 1rem', background: '#fff2f2', border: '1px solid #f5c6cb', borderRadius: 8 }}>
          <p style={{ color: '#c41e3a', margin: '0 0 0.5rem 0' }}>Something went wrong.</p>
          <p style={{ color: '#666', fontSize: '0.9rem', margin: 0 }}>We couldn’t load results. Check your connection and try again.</p>
          <button
            type="button"
            onClick={() => { setError(null); setPlayers([]) }}
            style={{ marginTop: '0.75rem', padding: '0.4rem 0.75rem', background: '#c41e3a', color: '#fff', border: 'none', borderRadius: 6, fontWeight: 500 }}
          >
            Try again
          </button>
        </div>
      )}
      {players.length > 0 && (
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {players.map((p) => (
            <li key={p.id} style={{ marginBottom: '0.5rem' }}>
              <Link
                to={`/players/${p.id}`}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                  padding: '0.75rem 1rem',
                  background: '#fff',
                  borderRadius: 8,
                  boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                  color: '#1a1a1a',
                  fontWeight: 500,
                }}
              >
                <PlayerAvatar nbaPlayerId={p.nbaPlayerId} size={40} />
                {p.fullName}
              </Link>
            </li>
          ))}
        </ul>
      )}
      {!loading && players.length === 0 && query.trim() && !error && (
        <p style={{ color: '#666' }}>No players found. Try another name.</p>
      )}
    </div>
  )
}
