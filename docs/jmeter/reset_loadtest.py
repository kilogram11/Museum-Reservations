# -*- coding: utf-8 -*-
"""Reset MySQL join/succCnt and Redis booking keys before a loadtest run."""
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


def main() -> None:
    meta = load_meta()
    time_mark = meta["timeMark"]
    meet_day = meta["meetDay"]
    print(f"reset timeMark={time_mark}")
    mysql(f"DELETE FROM `join` WHERE TIME_MARK LIKE 'museum_load_%';")
    mysql("UPDATE `time` SET SUCC_CNT=0 WHERE MUSEUM_ID='museum_load';")
    # delete redis booking keys
    keys = subprocess.run(
        [REDIS_CLI, "KEYS", "booking:*"],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    ).stdout.splitlines()
    for k in keys:
        k = k.strip()
        if k:
            subprocess.run([REDIS_CLI, "DEL", k], check=False)
    print(f"deleted redis keys={len([k for k in keys if k.strip()])}")
    print(mysql(f"SELECT LIMIT_CNT, SUCC_CNT FROM time WHERE TIME_MARK='{time_mark}'"))
    print("done")


if __name__ == "__main__":
    main()
