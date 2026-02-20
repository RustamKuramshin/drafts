#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Parse PostgreSQL-style slow query logs and produce CSV stats for a time window.

CSV columns:
- source (e.g. "keadmin@kazanexpress [stock-ke-backend-server]")
- query
- count
- avg_duration_ms
"""

from __future__ import annotations

import argparse
import csv
import re
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Optional, Tuple

try:
    # python-dateutil is very common; makes datetime parsing robust.
    from dateutil import parser as dtparser  # type: ignore
except Exception:  # pragma: no cover
    dtparser = None


TS_LINE_RE = re.compile(r"^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3}\s+")
DURATION_RE = re.compile(r"\bLOG:\s+duration:\s+([0-9]+(?:\.[0-9]+)?)\s+ms\b")
SQL_AFTER_EXEC_RE = re.compile(r"\bexecute\b[^:]*:\s*(.*)\s*$")


@dataclass
class Agg:
    count: int = 0
    total_ms: float = 0.0

    def add(self, ms: float) -> None:
        self.count += 1
        self.total_ms += ms

    @property
    def avg_ms(self) -> float:
        return self.total_ms / self.count if self.count else 0.0


def parse_dt(value: str) -> datetime:
    """
    Parse datetime from user input.
    Accepts: "2026-02-04 00:12:33", "2026-02-04T00:12:33Z", etc.
    Naive -> treated as UTC.
    """
    if dtparser is None:
        # Fallback: strict-ish format
        try:
            dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
        except ValueError as e:
            raise SystemExit(
                f"Cannot parse datetime '{value}'. Install python-dateutil or use ISO format."
            ) from e
    else:
        dt = dtparser.parse(value)

    if dt.tzinfo is None:
        return dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc)


def normalize_sql(sql: str) -> str:
    # Replace any internal newlines/tabs with spaces, collapse whitespace, strip trailing semicolon.
    s = re.sub(r"[\r\n\t]+", " ", sql)
    s = re.sub(r"\s+", " ", s).strip()
    if s.endswith(";"):
        s = s[:-1].rstrip()
    return s


def extract_ts_and_source(prefix: str) -> Tuple[datetime, str]:
    """
    prefix is the part BEFORE " from " in the line, e.g.
    "2026-02-04 00:12:33.598 UTC 31437 keadmin@kazanexpress [stock-ke-backend-server]"
    """
    parts = prefix.split()
    # expected: date, time.ms, tz, pid, user@db, [app]...
    if len(parts) < 5:
        raise ValueError(f"Unexpected prefix format: {prefix}")

    ts_str = f"{parts[0]} {parts[1]}"
    # log timestamps are effectively UTC (explicit 'UTC' token in examples)
    ts = datetime.strptime(ts_str, "%Y-%m-%d %H:%M:%S.%f").replace(tzinfo=timezone.utc)

    userdb = parts[4]
    extra = " ".join(parts[5:]).strip()
    source = f"{userdb} {extra}".strip() if extra else userdb
    return ts, source


def iter_entries(log_path: Path):
    """
    Yields tuples: (ts_utc, source, duration_ms, sql_text)
    Supports multi-line SQL: continuation lines are appended until a separator
    or next timestamped line appears.
    """
    cur = None  # dict with keys: ts, source, dur, sql_lines

    def flush():
        nonlocal cur
        if not cur:
            return None
        sql = normalize_sql(" ".join(cur["sql_lines"]))
        out = (cur["ts"], cur["source"], cur["dur"], sql)
        cur = None
        return out

    with log_path.open("r", encoding="utf-8", errors="replace") as f:
        for raw in f:
            line = raw.rstrip("\n")

            # separator between samples/entries (often present)
            if line.strip("- ") == "":
                entry = flush()
                if entry:
                    yield entry
                continue

            # new timestamped line?
            if TS_LINE_RE.match(line):
                # flush previous entry if any
                entry = flush()
                if entry:
                    yield entry

                # we only care about duration lines that contain SQL after execute
                dur_m = DURATION_RE.search(line)
                sql_m = SQL_AFTER_EXEC_RE.search(line)
                if not dur_m or not sql_m:
                    cur = None
                    continue

                # split prefix/source from rest
                prefix = line.split(" from ", 1)[0]
                try:
                    ts, source = extract_ts_and_source(prefix)
                except Exception:
                    cur = None
                    continue

                dur = float(dur_m.group(1))
                first_sql = sql_m.group(1)

                cur = {"ts": ts, "source": source, "dur": dur, "sql_lines": [first_sql]}
                continue

            # continuation line (only if we are inside an entry)
            if cur is not None:
                # sometimes continuation lines are indented; just append verbatim
                stripped = line.strip()
                if stripped:
                    cur["sql_lines"].append(stripped)

        # EOF flush
        entry = flush()
        if entry:
            yield entry


def main() -> int:
    ap = argparse.ArgumentParser(
        description="Analyze SQL log records in a time range and output CSV stats."
    )
    ap.add_argument(
        "--logfile",
        "-f",
        required=True,
        help="Path to the log file (local path).",
    )
    ap.add_argument(
        "--start",
        "-s",
        required=True,
        help="Start datetime (e.g. '2026-02-04 00:00:00Z' or '2026-02-04T00:00:00'). Naive -> UTC.",
    )
    ap.add_argument(
        "--end",
        "-e",
        required=True,
        help="End datetime (inclusive). Same formats as --start. Naive -> UTC.",
    )
    ap.add_argument(
        "--out",
        "-o",
        default="query_stats.csv",
        help="Output CSV path (default: query_stats.csv).",
    )
    ap.add_argument(
        "--group-by-source",
        action="store_true",
        help="If set, group stats by (source, query). If not set, group by query only.",
    )

    args = ap.parse_args()

    log_path = Path(args.logfile)
    if not log_path.exists():
        raise SystemExit(f"Log file not found: {log_path}")

    start_dt = parse_dt(args.start)
    end_dt = parse_dt(args.end)
    if end_dt < start_dt:
        raise SystemExit("--end must be >= --start")

    # key -> Agg
    stats: Dict[Tuple[str, str], Agg] = {}

    for ts, source, dur_ms, sql in iter_entries(log_path):
        if ts < start_dt or ts > end_dt:
            continue

        if args.group_by_source:
            key = (source, sql)
        else:
            key = ("", sql)

        stats.setdefault(key, Agg()).add(dur_ms)

    # write CSV
    out_path = Path(args.out)
    with out_path.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        if args.group_by_source:
            w.writerow(["source", "query", "count", "avg_duration_ms"])
        else:
            w.writerow(["query", "count", "avg_duration_ms"])

        # sort: most frequent, then slowest avg
        items = sorted(
            stats.items(),
            key=lambda kv: (-kv[1].count, -kv[1].avg_ms),
        )

        for (source, sql), agg in items:
            if args.group_by_source:
                w.writerow([source, sql, agg.count, f"{agg.avg_ms:.3f}"])
            else:
                w.writerow([sql, agg.count, f"{agg.avg_ms:.3f}"])

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
