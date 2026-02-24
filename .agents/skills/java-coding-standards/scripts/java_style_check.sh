#!/usr/bin/env bash
set -euo pipefail

# Simple grep-based checks (extend as needed)
rg -n "\t" src || true
rg -n "TODO" src || true
