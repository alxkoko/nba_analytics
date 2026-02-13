export default function GameLogTable({ games }) {
  if (!games?.length) {
    return (
      <section style={{ marginBottom: '2rem' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>Game log</h2>
        <p style={{ color: '#666' }}>No games.</p>
      </section>
    )
  }

  return (
    <section style={{ marginBottom: '2rem' }}>
      <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>Game log</h2>
      <div style={{ overflowX: 'auto', background: '#fff', borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.1)' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.9rem' }}>
          <thead>
            <tr style={{ background: '#f0f0f0', textAlign: 'left' }}>
              <Th>Date</Th>
              <Th>Matchup</Th>
              <Th>Result</Th>
              <Th>MIN</Th>
              <Th>PTS</Th>
              <Th>REB</Th>
              <Th>AST</Th>
              <Th>STL</Th>
              <Th>BLK</Th>
              <Th>TOV</Th>
            </tr>
          </thead>
          <tbody>
            {games.map((g) => (
              <tr key={g.id} style={{ borderBottom: '1px solid #eee' }}>
                <Td>{formatDate(g.gameDate)}</Td>
                <Td>{g.matchup ?? '—'}</Td>
                <Td>{g.wl ?? '—'}</Td>
                <Td>{g.minPlayed ?? '—'}</Td>
                <Td><strong>{g.pts}</strong></Td>
                <Td>{g.reb}</Td>
                <Td>{g.ast}</Td>
                <Td>{g.stl}</Td>
                <Td>{g.blk}</Td>
                <Td>{g.tov}</Td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function Th({ children }) {
  return <th style={{ padding: '0.6rem 0.75rem', fontWeight: 600 }}>{children}</th>
}

function Td({ children }) {
  return <td style={{ padding: '0.6rem 0.75rem' }}>{children}</td>
}

function formatDate(dateStr) {
  if (!dateStr) return '—'
  try {
    const d = new Date(dateStr)
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  } catch {
    return dateStr
  }
}
