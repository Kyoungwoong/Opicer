#!/usr/bin/env python3
import json
import os
import sys
import urllib.request

BASE_URL = os.getenv("OPICER_API_BASE_URL", "http://localhost:8080")
ADMIN_TOKEN = os.getenv("OPICER_ADMIN_TOKEN")
AUTH_COOKIE = os.getenv("OPICER_AUTH_COOKIE")

DATA_PATH = os.path.join(os.path.dirname(__file__), "universal-sentences.json")


def build_headers():
    headers = {"Content-Type": "application/json"}
    if ADMIN_TOKEN:
        headers["Authorization"] = f"Bearer {ADMIN_TOKEN}"
    elif AUTH_COOKIE:
        if "OPICER_AUTH=" in AUTH_COOKIE:
            headers["Cookie"] = AUTH_COOKIE
        else:
            headers["Cookie"] = f"OPICER_AUTH={AUTH_COOKIE}"
    else:
        raise RuntimeError(
            "Missing admin auth. Set OPICER_ADMIN_TOKEN or OPICER_AUTH_COOKIE."
        )
    return headers


def post_entry(entry, headers):
    url = f"{BASE_URL}/api/admin/universal-sentences"
    body = json.dumps(entry).encode("utf-8")
    request = urllib.request.Request(url, data=body, headers=headers, method="POST")
    with urllib.request.urlopen(request) as response:
        return response.status, response.read().decode("utf-8")


def main():
    with open(DATA_PATH, "r", encoding="utf-8") as handle:
        payload = json.load(handle)

    headers = build_headers()
    failures = 0
    for idx, entry in enumerate(payload, start=1):
        try:
            status, body = post_entry(entry, headers)
            if status >= 400:
                print(f"[{idx}] FAIL {status}: {body}")
                failures += 1
            else:
                print(f"[{idx}] OK {status}")
        except Exception as exc:
            print(f"[{idx}] ERROR: {exc}")
            failures += 1

    if failures:
        print(f"Completed with {failures} failures.")
        sys.exit(1)
    print("Completed successfully.")


if __name__ == "__main__":
    main()
