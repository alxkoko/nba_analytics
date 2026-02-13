// In production with split hosting (e.g. Vercel + Railway), set VITE_API_URL to the backend URL (no trailing slash)
const API_BASE = import.meta.env.VITE_API_URL ? `${import.meta.env.VITE_API_URL}/api` : '/api'

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || `HTTP ${res.status}`)
  }
  return res.json()
}

/** NBA season: Oct–June = e.g. 2024-25; July–Sep = previous season. Same logic as backend/ingestion. */
export function getCurrentSeason() {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth() + 1
  const startYear = month >= 10 ? year : year - 1
  const endYY = String((startYear + 1) % 100).padStart(2, '0')
  return `${startYear}-${endYY}`
}

export const api = {
  async getCurrentSeason() {
    const r = await request('/season/current')
    return r.season
  },
  getSeasons(count = 5) {
    return request(`/season/list?count=${count}`)
  },
  searchPlayers(q) {
    return request(`/players?q=${encodeURIComponent(q || '')}`)
  },
  getPlayer(id) {
    return request(`/players/${id}`)
  },
  getGames(playerId, season = '2024-25') {
    return request(`/players/${playerId}/games?season=${encodeURIComponent(season)}`)
  },
  getStats(playerId, season = '2024-25') {
    return request(`/players/${playerId}/stats?season=${encodeURIComponent(season)}`)
  },
  getOverUnder(playerId, { season = '2024-25', stat = 'pts', threshold = 25, lastN } = {}) {
    let path = `/players/${playerId}/over-under?season=${encodeURIComponent(season)}&stat=${encodeURIComponent(stat)}&threshold=${threshold}`
    if (lastN != null && lastN > 0) path += `&lastN=${lastN}`
    return request(path)
  },
}
