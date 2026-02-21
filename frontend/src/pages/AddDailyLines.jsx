import { useState } from 'react'
import { Link } from 'react-router-dom'
import { api, getCurrentSeason } from '../api/client'

export default function AddDailyLines() {
  const [date, setDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [text, setText] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    const lines = text
      .trim()
      .split('\n')
      .map((line) => {
        const parts = line.split(',').map((p) => p.trim())
        if (parts.length < 3) return null
        const player = parts[0]
        const stat = parts[1]
        const lineVal = parseFloat(parts[2])
        if (Number.isNaN(lineVal)) return null
        return { player, stat, line: lineVal }
      })
      .filter(Boolean)
    if (lines.length === 0) {
      setError('Enter at least one line. Format: Player Name, stat, line (one per row). Example: Kon Knueppel, fg3m, 3')
      return
    }
    setError(null)
    setResult(null)
    setLoading(true)
    try {
      const res = await api.addDailyLines({
        date,
        season: getCurrentSeason(),
        lines,
      })
      setResult(res)
      setText('')
    } catch (err) {
      setError(err?.message || 'Failed to add lines.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <p style={{ marginBottom: '1rem' }}>
        <Link to="/">← Back to search</Link>
      </p>
      <h1 style={{ marginTop: 0, marginBottom: '0.5rem', fontSize: '1.5rem' }}>Add today&apos;s lines</h1>
      <p style={{ color: '#666', fontSize: '0.9rem', marginBottom: '1rem' }}>
        One line per row: <strong>Player Name, stat, line</strong>. Stat: pts, reb, ast, fg3m, pts_reb, pts_ast, reb_ast, pts_reb_ast.
      </p>
      <form onSubmit={handleSubmit}>
        <label style={{ display: 'block', marginBottom: '0.5rem' }}>
          Date: <input type="date" value={date} onChange={(e) => setDate(e.target.value)} style={{ padding: '0.35rem', borderRadius: 4, border: '1px solid #ccc' }} />
        </label>
        <label style={{ display: 'block', marginBottom: '0.5rem' }}>
          Lines (paste from your book — 30, 60, or more; no limit):
        </label>
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder={"Kon Knueppel, fg3m, 3\nDamian Lillard, pts, 25.5\n..."}
          rows={18}
          style={{ width: '100%', maxWidth: 500, padding: '0.5rem', borderRadius: 6, border: '1px solid #ccc', fontFamily: 'inherit', fontSize: '0.9rem' }}
        />
        <div style={{ marginTop: '0.75rem' }}>
          <button type="submit" disabled={loading} style={{ padding: '0.5rem 1rem', background: '#c41e3a', color: '#fff', border: 'none', borderRadius: 6, fontWeight: 600 }}>
            {loading ? 'Adding…' : 'Add lines'}
          </button>
        </div>
      </form>
      {error && <p style={{ color: '#c41e3a', marginTop: '1rem' }}>{error}</p>}
      {result && <p style={{ color: '#0a0', marginTop: '1rem' }}>Saved {result.saved} picks for {result.date}.</p>}
    </div>
  )
}
