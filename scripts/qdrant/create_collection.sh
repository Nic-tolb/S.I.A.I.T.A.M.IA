#!/usr/bin/env bash
# Creates the knowledge articles collection in Qdrant. Idempotent: does nothing if it already exists.
#
# Usage (from the repo root, with Qdrant running):  ./scripts/qdrant/create_collection.sh
#
# VECTOR_SIZE must match the embedding model output:
#   baai/bge-m3 -> 1024 dimensions (verified against OpenRouter on 2026-11-11).
# Changing the embedding model means recreating the collection and re-indexing every article.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
if [[ -f "$ROOT_DIR/.env" ]]; then
    # Only read the variables this script needs (never source the whole .env)
    QDRANT_PORT="${QDRANT_PORT:-$(grep -E '^QDRANT_PORT=' "$ROOT_DIR/.env" | cut -d= -f2-)}"
    QDRANT_COLLECTION="${QDRANT_COLLECTION:-$(grep -E '^QDRANT_COLLECTION=' "$ROOT_DIR/.env" | cut -d= -f2-)}"
fi

QDRANT_URL="${QDRANT_URL:-http://localhost:${QDRANT_PORT:-6333}}"
COLLECTION="${QDRANT_COLLECTION:-knowledge_articles}"
VECTOR_SIZE="${VECTOR_SIZE:-1024}"
DISTANCE="Cosine"

status=$(curl -s -o /dev/null -w '%{http_code}' "$QDRANT_URL/collections/$COLLECTION")
if [[ "$status" == "200" ]]; then
    echo "Collection '$COLLECTION' already exists:"
    curl -s "$QDRANT_URL/collections/$COLLECTION" | grep -o '"vectors":{[^}]*}' || true
    exit 0
fi

echo "Creating collection '$COLLECTION' (size=$VECTOR_SIZE, distance=$DISTANCE) at $QDRANT_URL"
curl -sf -X PUT "$QDRANT_URL/collections/$COLLECTION" \
    -H 'Content-Type: application/json' \
    -d "{\"vectors\": {\"size\": $VECTOR_SIZE, \"distance\": \"$DISTANCE\"}}"
echo
echo "Done."
