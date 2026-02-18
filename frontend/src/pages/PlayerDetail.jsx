import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api, getCurrentSeason } from '../api/client'
import GameLogTable from '../components/GameLogTable'
import SeasonStats from '../components/SeasonStats'
import OverUnderBlock from '../components/OverUnderBlock'
import PointsChart from '../components/PointsChart'

const HEADSHOT_URL = (nbaId) => `https://cdn.nba.com/headshots/nba/latest/260x190/${nbaId}.png`

function PlayerHeadshot({ nbaPlayerId }) {
  const [show, setShow] = useState(true)
  if (!nbaPlayerId || !show) return null
  return (
    <img
      src={HEADSHOT_URL(nbaPlayerId)}
      alt=""
      width={130}
      height={95}
      style={{ objectFit: 'cover', borderRadius: 8, background: '#eee' }}
      onError={() => setShow(false)}
    />
  )
}

export default function PlayerDetail() {
  const { id } = useParams()
  const [player, setPlayer] = useState(null)
  const [games, setGames] = useState([])
  const [stats, setStats] = useState(null)
  const [overUnder, setOverUnder] = useState(null)
  const season = getCurrentSeason()
  const [overUnderStat, setOverUnderStat] = useState('pts')
  const [overUnderThreshold, setOverUnderThreshold] = useState(25)
  const [lastN, setLastN] = useState(10)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    document.title = 'NBA Player Analytics'
    return () => { document.title = 'NBA Player Analytics' }
  }, [])

  useEffect(() => {
    if (!id) return
    if (!player) {
      setLoading(true)
      setError(null)
    }
    Promise.all([
      api.getPlayer(id),
      api.getGames(id, season),
      api.getStats(id, season),
      api.getOverUnder(id, { season, stat: overUnderStat, threshold: overUnderThreshold, lastN }),
    ])
      .then(([p, g, s, ou]) => {
        setPlayer(p)
        setGames(g || [])
        setStats(s)
        setOverUnder(ou)
        if (p?.fullName) document.title = `${p.fullName} – NBA Player Analytics`
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id, season, overUnderStat, overUnderThreshold, lastN, retry])

  if (loading && !player) {
    return <p>Loading…</p>
  }
  if (error && !player) {
    return (
      <div>
        <div style={{ marginBottom: '1rem', padding: '0.75rem 1rem', background: '#fff2f2', border: '1px solid #f5c6cb', borderRadius: 8 }}>
          <p style={{ color: '#c41e3a', margin: '0 0 0.5rem 0' }}>Something went wrong.</p>
          <p style={{ color: '#666', fontSize: '0.9rem', margin: 0 }}>We couldn’t load this player. Check your connection and try again.</p>
          <button
            type="button"
            onClick={() => { setError(null); setLoading(true); setRetry((r) => r + 1); }}
            style={{ marginTop: '0.75rem', padding: '0.4rem 0.75rem', background: '#c41e3a', color: '#fff', border: 'none', borderRadius: 6, fontWeight: 500 }}
          >
            Try again
          </button>
        </div>
        <Link to="/">← Back to search</Link>
      </div>
    )
  }
  if (!player) {
    return (
      <div>
        <p>Player not found.</p>
        <Link to="/">← Back to search</Link>
      </div>
    )
  }

  return (
    <div>
      <p style={{ marginBottom: '1rem' }}>
        <Link to="/">← Back to search</Link>
      </p>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1rem', flexWrap: 'wrap' }}>
        <PlayerHeadshot nbaPlayerId={player.nbaPlayerId} />
        <div>
          <h1 style={{ marginTop: 0, marginBottom: '0.25rem', fontSize: '1.75rem' }}>{player.fullName}</h1>
          <p style={{ color: '#666', margin: 0 }}>NBA ID: {player.nbaPlayerId} · Season: {season}</p>
        </div>
      </div>

      <SeasonStats stats={stats} />
      <OverUnderBlock
        overUnder={overUnder}
        stat={overUnderStat}
        onStatChange={setOverUnderStat}
        threshold={overUnderThreshold}
        onThresholdChange={setOverUnderThreshold}
        lastN={lastN}
        onLastNChange={setLastN}
      />
      <PointsChart
        games={games}
        lastN={lastN ?? 20}
        stat={overUnderStat}
        threshold={overUnderThreshold}
      />
      <GameLogTable games={games} />
    </div>
  )
}
