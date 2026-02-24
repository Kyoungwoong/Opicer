#!/usr/bin/env bash
set -euo pipefail

# Usage: safe_read.sh <file>
# Prints file with line numbers for safe review.

nl -ba "$1"
