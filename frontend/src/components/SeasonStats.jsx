export default function SeasonStats({ stats }) {
  if (!stats || stats.gamesPlayed === 0) {
    return (
      <section style={{ marginBottom: '2rem' }}>
        <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>Season stats</h2>
        <p style={{ color: '#666' }}>No games for this season.</p>
      </section>
    )
  }

  return (
    <section style={{ marginBottom: '2rem' }}>
      <h2 style={{ fontSize: '1.25rem', marginBottom: '0.75rem' }}>Season stats ({stats.season})</h2>
      <div style={{
        display: 'grid',
        gridTemplateColumns: 'repeat(auto-fill, minmax(100px, 1fr))',
        gap: '0.75rem',
        padding: '1rem',
        background: '#fff',
        borderRadius: 8,
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
      }}>
        <Stat label="GP" value={stats.gamesPlayed} />
        <Stat label="PPG" value={stats.ptsAvg?.toFixed(1)} />
        <Stat label="RPG" value={stats.rebAvg?.toFixed(1)} />
        <Stat label="APG" value={stats.astAvg?.toFixed(1)} />
        <Stat label="SPG" value={stats.stlAvg?.toFixed(1)} />
        <Stat label="BPG" value={stats.blkAvg?.toFixed(1)} />
        <Stat label="TOV" value={stats.tovAvg?.toFixed(1)} />
      </div>
    </section>
  )
}

function Stat({ label, value }) {
  return (
    <div>
      <span style={{ fontSize: '0.75rem', color: '#666', display: 'block' }}>{label}</span>
      <span style={{ fontWeight: 700, fontSize: '1.1rem' }}>{value}</span>
    </div>
  )
}
