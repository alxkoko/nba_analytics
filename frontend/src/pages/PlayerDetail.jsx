import { useState, useEffect } from 'react'
import { useParams, Link } from 'react-router-dom'
import { api, getCurrentSeason } from '../api/client'
import GameLogTable from '../components/GameLogTable'
import SeasonStats from '../components/SeasonStats'
import OverUnderBlock from '../components/OverUnderBlock'
import PointsChart from '../components/PointsChart'

export default function PlayerDetail() {
  const { id } = useParams()
  const [player, setPlayer] = useState(null)
  const [games, setGames] = useState([])
  const [stats, setStats] = useState(null)
  const [overUnder, setOverUnder] = useState(null)
  const [season, setSeason] = useState(() => getCurrentSeason())
  const [seasonOptions, setSeasonOptions] = useState([])
  const [overUnderStat, setOverUnderStat] = useState('pts')
  const [overUnderThreshold, setOverUnderThreshold] = useState(25)
  const [lastN, setLastN] = useState(10)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.getSeasons(5).then(setSeasonOptions).catch(() => setSeasonOptions([getCurrentSeason(), '2023-24', '2022-23']))
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
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }, [id, season, overUnderStat, overUnderThreshold, lastN])

  if (loading && !player) {
    return <p>Loading…</p>
  }
  if (error && !player) {
    return (
      <div>
        <p style={{ color: '#c41e3a' }}>{error}</p>
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
      <h1 style={{ marginTop: 0, marginBottom: '0.5rem', fontSize: '1.75rem' }}>{player.fullName}</h1>
      <p style={{ color: '#666', marginBottom: '1.5rem' }}>NBA ID: {player.nbaPlayerId}</p>

      <label style={{ display: 'block', marginBottom: '0.5rem', fontWeight: 600 }}>Season</label>
      <select
        value={season}
        onChange={(e) => setSeason(e.target.value)}
        style={{ padding: '0.5rem', borderRadius: 6, border: '1px solid #ccc', marginBottom: '1.5rem' }}
      >
        {seasonOptions.length ? seasonOptions.map((s) => (
          <option key={s} value={s}>{s}{s === getCurrentSeason() ? ' (current)' : ''}</option>
        )) : (
          <>
            <option value={getCurrentSeason()}>{getCurrentSeason()} (current)</option>
            <option value="2023-24">2023-24</option>
            <option value="2022-23">2022-23</option>
          </>
        )}
      </select>

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
