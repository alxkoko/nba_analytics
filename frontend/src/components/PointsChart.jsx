import { useMemo } from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts'

const STAT_LABELS = {
  pts: 'Points',
  reb: 'Rebounds',
  ast: 'Assists',
  fg3m: '3PM',
  pts_reb: 'Pts+Reb',
  pts_ast: 'Pts+Ast',
  reb_ast: 'Reb+Ast',
  pts_reb_ast: 'Pts+Reb+Ast',
}

function getGameValue(g, stat) {
  const p = g.pts ?? 0
  const r = g.reb ?? 0
  const a = g.ast ?? 0
  const f3 = g.fg3m ?? 0
  switch (stat) {
    case 'pts': return p
    case 'reb': return r
    case 'ast': return a
    case 'fg3m': return f3
    case 'pts_reb': return p + r
    case 'pts_ast': return p + a
    case 'reb_ast': return r + a
    case 'pts_reb_ast': return p + r + a
    default: return p
  }
}

export default function PointsChart({ games, lastN = 20, stat = 'pts', threshold }) {
  const statLabel = STAT_LABELS[stat] || 'Points'
  const data = useMemo(() => {
    if (!games?.length) return []
    const slice = games.slice(0, lastN).reverse()
    return slice.map((g, i) => {
      const value = getGameValue(g, stat)
      return {
        index: i + 1,
        game: `Game ${i + 1}`,
        date: g.gameDate ? new Date(g.gameDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) : '',
        value,
      }
    })
  }, [games, lastN, stat])

  const overThreshold = threshold != null ? threshold : (stat === 'pts' ? 25 : null)

  if (!data.length) {
    return (
      <section style={{ marginBottom: '2rem' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>{statLabel} (last {lastN} games)</h2>
        <p style={{ color: '#666' }}>No data.</p>
      </section>
    )
  }

  return (
    <section style={{ marginBottom: '2rem' }}>
      <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>{statLabel} (last {Math.min(lastN, data.length)} games)</h2>
      <div style={{ height: 280, background: '#fff', borderRadius: 8, padding: '1rem', boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={data} margin={{ top: 5, right: 5, left: 5, bottom: 5 }}>
            <XAxis dataKey="game" tick={{ fontSize: 11 }} />
            <YAxis tick={{ fontSize: 11 }} allowDecimals={false} />
            <Tooltip
              formatter={(value) => [value, statLabel]}
              labelFormatter={(_, payload) => payload?.[0]?.payload?.date ? `Date: ${payload[0].payload.date}` : ''}
            />
            <Bar dataKey="value" fill="#c41e3a" radius={[2, 2, 0, 0]}>
              {data.map((entry, i) => (
                <Cell
                  key={i}
                  fill={overThreshold != null && entry.value >= overThreshold ? '#c41e3a' : '#888'}
                />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
    </section>
  )
}
