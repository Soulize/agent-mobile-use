#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "[build] Testing vd_server..."
go test ./...

echo "[build] Building Android/ARM64 vd_server..."
CGO_ENABLED=0 GOOS=android GOARCH=arm64     go build -ldflags="-s -w" -o vd_server .

echo "[build] Done: vd_server binary ready."
ls -lh vd_server
stat -c '[build] vd_server bytes=%s' vd_server
file vd_server
