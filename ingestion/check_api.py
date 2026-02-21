"""
Quick check if the NBA stats API (stats.nba.com) is responding.
Run from the ingestion folder:  python check_api.py
"""
from nba_api.stats.endpoints import commonplayerinfo

# Use a known player ID (LeBron James = 2544)
PLAYER_ID = 2544

if __name__ == "__main__":
    try:
        info = commonplayerinfo.CommonPlayerInfo(player_id=PLAYER_ID)
        df = info.get_data_frames()[0]
        name = df["DISPLAY_FIRST_LAST"].iloc[0]
        print("OK: API responded. Player:", name)
    except Exception as e:
        print("FAIL:", type(e).__name__, "-", e)
