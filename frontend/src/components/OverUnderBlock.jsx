import { useState, useEffect } from 'react'

const STAT_OPTIONS = [
  { value: 'pts', label: 'Points' },
  { value: 'reb', label: 'Rebounds' },
  { value: 'ast', label: 'Assists' },
  { value: 'fg3m', label: '3PM' },
  { value: 'pts_reb', label: 'Pts+Reb' },
  { value: 'pts_ast', label: 'Pts+Ast' },
  { value: 'reb_ast', label: 'Reb+Ast' },
  { value: 'pts_reb_ast', label: 'Pts+Reb+Ast' },
]

export default function OverUnderBlock({
  overUnder,
  stat,
  onStatChange,
  threshold,
  onThresholdChange,
  lastN,
  onLastNChange,
}) {
  const [inputThreshold, setInputThreshold] = useState(threshold)
  const [inputLastN, setInputLastN] = useState(lastN ?? '')
  const statLabel = STAT_OPTIONS.find((o) => o.value === (stat || 'pts'))?.label || stat

  useEffect(() => {
    setInputLastN(lastN ?? '')
  }, [lastN])

  function handleApply() {
    const t = Number(inputThreshold)
    const n = inputLastN === '' ? null : Number(inputLastN)
    if (!Number.isNaN(t)) onThresholdChange(t)
    onLastNChange(n)
  }

  if (!overUnder) {
    return (
      <section style={{ marginBottom: '2rem' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>Over/under</h2>
        <p style={{ color: '#666' }}>Loading…</p>
      </section>
    )
  }

  const pctOver = (overUnder.probabilityOver * 100).toFixed(1)
  const pctUnder = (overUnder.probabilityUnder * 100).toFixed(1)

  return (
    <section style={{ marginBottom: '2rem' }}>
      <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>Over/under</h2>
      <div style={{
        padding: '1rem',
        background: '#fff',
        borderRadius: 8,
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
        marginBottom: '1rem',
      }}>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center', marginBottom: '0.75rem' }}>
          <label>
            <span style={{ marginRight: '0.5rem' }}>Stat:</span>
            <select
              value={stat || 'pts'}
              onChange={(e) => onStatChange(e.target.value)}
              style={{ padding: '0.35rem', borderRadius: 4, border: '1px solid #ccc', minWidth: 120 }}
            >
              {STAT_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </select>
          </label>
          <label>
            <span style={{ marginRight: '0.5rem' }}>Threshold:</span>
            <input
              type="number"
              value={inputThreshold}
              onChange={(e) => setInputThreshold(e.target.value)}
              min={0}
              step={0.5}
              style={{ width: 70, padding: '0.35rem', borderRadius: 4, border: '1px solid #ccc' }}
            />
          </label>
          <label>
            <span style={{ marginRight: '0.5rem' }}>Last N games played (blank = all):</span>
            <input
              type="number"
              value={inputLastN}
              onChange={(e) => setInputLastN(e.target.value)}
              min={1}
              placeholder="all"
              style={{ width: 70, padding: '0.35rem', borderRadius: 4, border: '1px solid #ccc' }}
            />
          </label>
          <button
            type="button"
            onClick={handleApply}
            style={{
              padding: '0.35rem 0.75rem',
              background: '#c41e3a',
              color: '#fff',
              border: 'none',
              borderRadius: 6,
              fontWeight: 600,
            }}
          >
            Apply
          </button>
        </div>
        <p style={{ margin: 0, fontSize: '0.75rem', color: '#888', marginBottom: '0.25rem' }}>Over = at or above threshold (≥)</p>
        <p style={{ margin: 0, fontSize: '1.1rem' }}>
          <strong>{statLabel} over {overUnder.threshold}</strong>: {overUnder.gamesOver} of {overUnder.totalGames} games ({pctOver}%)
        </p>
        <p style={{ margin: '0.25rem 0 0', color: '#666' }}>
          <strong>Under {overUnder.threshold}</strong>: {overUnder.gamesUnder} games ({pctUnder}%)
        </p>
        <p style={{ margin: '0.5rem 0 0', fontSize: '0.9rem', color: '#666' }}>
          Based on {overUnder.totalGames} game{overUnder.totalGames !== 1 ? 's' : ''} played
          {overUnder.lastN != null ? ` (last ${overUnder.lastN} games played)` : ' this season'}.
        </p>
        {overUnder.lastN != null && overUnder.totalGames < overUnder.lastN && (
          <p style={{ margin: '0.25rem 0 0', fontSize: '0.85rem', color: '#888', fontStyle: 'italic' }}>
            Player has {overUnder.totalGames} game{overUnder.totalGames !== 1 ? 's' : ''} in this range — may have missed games. Use a larger range or full season for more context.
          </p>
        )}
      </div>
    </section>
  )
}
