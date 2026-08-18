# -*- coding: utf-8 -*-
"""Login all loadtest mobiles and write docs/jmeter/users.csv (token,identityId)."""
from __future__ import annotations

import csv
import json
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MOBILES = ROOT / "docs" / "jmeter" / "mobiles.csv"
USERS = ROOT / "docs" / "jmeter" / "users.csv"
META = ROOT / "docs" / "jmeter" / "loadtest-meta.properties"
BASE = "http://localhost:8081"


def login(mobile: str) -> str:
    req = urllib.request.Request(
        f"{BASE}/app/user/login",
        data=json.dumps({"mobile": mobile, "code": "1234"}).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=30) as resp:
        body = json.loads(resp.read().decode("utf-8"))
    if body.get("code") != 200:
        raise RuntimeError(f"login failed for {mobile}: {body}")
    return body["data"]["token"]


def main() -> None:
    rows_out = [("token", "identityId")]
    with MOBILES.open(encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            token = login(row["mobile"])
            rows_out.append((token, row["identityId"]))
            print(f"ok {row['mobile']} -> {row['identityId']}")

    with USERS.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f)
        writer.writerows(rows_out)

    meta = {}
    for line in META.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            meta[k] = v
    print(f"Wrote {USERS} rows={len(rows_out)-1} timeMark={meta.get('timeMark')}")


if __name__ == "__main__":
    main()
