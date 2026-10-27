#!/usr/bin/env bash
# Loads opicer-api/.env into the environment, then runs the API (macOS, Linux, WSL).
# Spring Boot does not read .env files itself; secrets stay in .env only.
set -euo pipefail

api_dir="$(cd "$(dirname "$0")" && pwd)"
env_file="$api_dir/.env"

if [[ -f "$env_file" ]]; then
	while IFS= read -r line || [[ -n "$line" ]]; do
		line="${line%$'\r'}"
		[[ "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)[[:space:]]*=[[:space:]]*(.*)$ ]] || continue
		value="${BASH_REMATCH[2]}"
		value="${value%"${value##*[![:space:]]}"}"
		value="${value#\"}"; value="${value%\"}"
		value="${value#\'}"; value="${value%\'}"
		# An empty value would override the default in application.yaml, so leave it unset.
		[[ -n "$value" ]] || continue
		export "${BASH_REMATCH[1]}=$value"
	done < "$env_file"
	echo "Loaded env from $env_file"
else
	echo "No .env found at $env_file; using defaults"
fi

exec "$api_dir/gradlew" -p "$api_dir" bootRun "$@"
