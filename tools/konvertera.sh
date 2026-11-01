#!/usr/bin/env bash
# Gör om klippen i mappen klipp/ till tv-anpassade filer i appen:
# inget ljud, högst 1920 px bred, H.264, mjuk tonning i början och slutet.
set -euo pipefail
IN="${1:-klipp}"
OUT="${2:-app/src/main/assets/videos}"
STILLS="${3:-app/src/main/assets/stills}"
FADE=1.2
mkdir -p "$OUT" "$STILLS"
shopt -s nullglob nocaseglob
for f in "$IN"/*.mp4 "$IN"/*.mov "$IN"/*.webm; do
  name=$(basename "$f")
  name="${name%.*}"
  name=$(echo "$name" | tr '[:upper:]' '[:lower:]' | tr ' ' '_')
  dur=$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$f")
  out_start=$(awk -v d="$dur" -v f="$FADE" 'BEGIN{s=d-f; if(s<0)s=0; printf "%.2f", s}')
  echo "Konverterar $f -> $OUT/$name.mp4 (${dur}s)"
  ffmpeg -v error -y -i "$f" -an \
    -vf "scale='min(1920,iw)':-2,fade=t=in:st=0:d=$FADE,fade=t=out:st=$out_start:d=$FADE,format=yuv420p" \
    -c:v libx264 -profile:v main -level 4.0 -preset slow -crf 19 -movflags +faststart \
    "$OUT/$name.mp4"
  # Stillbild från mitten av klippet, visas när ingen rört fjärrkontrollen på ett tag
  mid=$(awk -v d="$dur" 'BEGIN{printf "%.2f", d/2}')
  ffmpeg -v error -y -ss "$mid" -i "$f" -frames:v 1 -vf "scale='min(1920,iw)':-2" -q:v 3 "$STILLS/$name.jpg"
done
