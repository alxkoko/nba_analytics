"""Quick check: connect with DATABASE_URL from .env and list DB + tables."""
import os
from dotenv import load_dotenv

load_dotenv()
url = os.environ.get("DATABASE_URL", "")
if not url:
    print("DATABASE_URL is not set in .env")
    exit(1)
# Mask password: show only host and after
masked = url.split("@")[-1] if "@" in url else url
print("Connecting to:", masked)

import psycopg2

conn = psycopg2.connect(url)
cur = conn.cursor()
cur.execute("SELECT current_database(), current_schema()")
print("Connected to DB:", cur.fetchone())
cur.execute(
    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name"
)
tables = [r[0] for r in cur.fetchall()]
print("Tables in public:", tables)
conn.close()
print("Done.")
