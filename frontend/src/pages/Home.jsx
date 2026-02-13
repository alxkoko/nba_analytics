import { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'

const SUGGEST_DEBOUNCE_MS = 300
const MIN_QUERY_LENGTH = 2

export default function Home() {
  const [query, setQuery] = useState('')
  const [players, setPlayers] = useState([])
  const [suggestions, setSuggestions] = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const navigate = useNavigate()
  const suggestRef = useRef(null)

  // Debounced search-as-you-type for suggestions
  useEffect(() => {
    if (!query.trim() || query.trim().length < MIN_QUERY_LENGTH) {
      setSuggestions([])
      setShowSuggestions(false)
      return
    }
    const t = setTimeout(async () => {
      try {
        const data = await api.searchPlayers(query.trim())
        const list = Array.isArray(data) ? data.slice(0, 8) : []
        setSuggestions(list)
        setShowSuggestions(list.length > 0)
      } catch {
        setSuggestions([])
        setShowSuggestions(false)
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
      setError(err.message)
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
          {showSuggestions && suggestions.length > 0 && (
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
                    {p.fullName}
                  </button>
                </li>
              ))}
            </ul>
          )}
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
      {error && <p style={{ color: '#c41e3a', marginBottom: '1rem' }}>{error}</p>}
      {players.length > 0 && (
        <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {players.map((p) => (
            <li key={p.id} style={{ marginBottom: '0.5rem' }}>
              <Link
                to={`/players/${p.id}`}
                style={{
                  display: 'block',
                  padding: '0.75rem 1rem',
                  background: '#fff',
                  borderRadius: 8,
                  boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                  color: '#1a1a1a',
                  fontWeight: 500,
                }}
              >
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
