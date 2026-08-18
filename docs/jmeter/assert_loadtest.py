# -*- coding: utf-8 -*-
"""Assert loadtest invariants after JMeter run."""
from __future__ import annotations

import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
META = ROOT / "docs" / "jmeter" / "loadtest-meta.properties"
MYSQL = r"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
REDIS_CLI = r"D:\Users\peiyaowan\redis\redis-windows-8.8.0\redis-windows-8.8.0\redis-cli.exe"


def load_meta() -> dict[str, str]:
    meta = {}
    for line in META.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            meta[k] = v
    return meta


def mysql(sql: str) -> str:
    p = subprocess.run(
        [MYSQL, "-uroot", "-p123456", "museum_book", "-N", "-e", sql],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if p.returncode != 0:
        raise RuntimeError(p.stderr)
    return p.stdout.strip()


def redis(cmd: list[str]) -> str:
    p = subprocess.run(
        [REDIS_CLI, *cmd],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    return p.stdout.strip()


def main() -> None:
    meta = load_meta()
    tm = meta["timeMark"]
    limit = int(meta["limitCnt"])
    meet_day = meta["meetDay"]

    row = mysql(f"SELECT LIMIT_CNT, SUCC_CNT FROM time WHERE TIME_MARK='{tm}'")
    limit_db, succ = [int(x) for x in row.split()]
    joins = int(mysql(f"SELECT COUNT(*) FROM `join` WHERE TIME_MARK='{tm}' AND JOIN_STATUS=1"))
    stock = redis(["GET", f"booking:stock:{tm}"])
    remain = int(stock) if stock not in ("", "(nil)") else None
    booked_n = len([k for k in redis(["KEYS", f"booking:booked:{meet_day}:*"]).splitlines() if k])

    print(f"timeMark={tm}")
    print(f"LIMIT_CNT={limit_db} SUCC_CNT={succ} join_success={joins}")
    print(f"redis_stock={remain} booked_keys={booked_n}")

    ok = True
    if succ != joins:
        print("FAIL: SUCC_CNT != join success count")
        ok = False
    if remain is None:
        print("FAIL: redis stock missing")
        ok = False
    elif succ + remain != limit_db:
        print(f"FAIL: succ+remain != limit ({succ}+{remain}!={limit_db})")
        ok = False
    if joins > limit:
        print("FAIL: oversell detected")
        ok = False
    if joins != limit:
        print(f"WARN: expected exactly {limit} successes, got {joins}")
    if ok and joins == limit and remain == 0:
        print("PASS: no oversell; SUCC_CNT + Redis remain = LIMIT_CNT")
    elif ok:
        print("PASS_PARTIAL: invariant holds but success count != limit")
    else:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
