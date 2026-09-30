#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "[modern-build] Running vd_server tests on host..."
go test ./...

echo "[modern-build] Building Android/ARM64 vd_server..."
CGO_ENABLED=0 GOOS=android GOARCH=arm64     go build -ldflags="-s -w" -o vd_server .

ls -lh vd_server
file vd_server
