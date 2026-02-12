#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
mr-jira.py — извлекает Jira issues из GitLab Merge Request
(версия с Bearer Token + browser-like User-Agent, как в рабочем curl)
./mr-jira-gpt.py https://gitlab.platform.corp/magnitonline/mm/backend/mm-core-bff/-/merge_requests/623
"""

from __future__ import annotations

import os
import re
import sys
import logging
import urllib.parse
from dataclasses import dataclass
from typing import List, Dict, Set, Optional

import typer
import requests
import gitlab
from rich.console import Console
from rich.table import Table
from rich.markdown import Markdown

# -----------------------------------------------------------------------------

app = typer.Typer()
console = Console()
err = Console(stderr=True, style="bold red")

DEFAULT_JIRA_BASE = os.getenv("JIRA_BASE", "https://track.magnit.ru")
DEFAULT_JIRA_KEY_RE = os.getenv("JIRA_KEY_RE", r"[A-Z][A-Z0-9]+-\d+")
DEFAULT_UA = os.getenv(
    "USER_AGENT",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/121.0.0.0 Safari/537.36",
)

IGNORE_PATTERNS = [
    r"^Merge ",
    r"^Revert ",
    r"^WIP",
    r"\[skip ci\]",
]

# -----------------------------------------------------------------------------

@dataclass(frozen=True)
class JiraIssue:
    key: str
    summary: str
    issuetype: str

    def url(self, base: str) -> str:
        return f"{base}/browse/{self.key}"

# -----------------------------------------------------------------------------

def setup_logging(verbose: bool):
    logging.basicConfig(
        level=logging.DEBUG if verbose else logging.INFO,
        format="[%(levelname)s] %(message)s",
    )

def parse_mr_url(url: str):
    """
    https://gitlab.example.com/group/proj/-/merge_requests/123
    """
    p = urllib.parse.urlparse(url)
    m = re.match(r"/(.+)/-/merge_requests/(\d+)", p.path)
    if not m:
        raise typer.BadParameter("Неверный MR URL")
    return p.netloc, m.group(1), m.group(2)

# -----------------------------------------------------------------------------
# GitLab
# -----------------------------------------------------------------------------

def gitlab_client(base: str, token: str, insecure: bool):
    gl = gitlab.Gitlab(base, private_token=token, ssl_verify=not insecure)
    gl.auth()
    return gl

def get_mr_commits(gl, project_path: str, iid: str):
    proj = gl.projects.get(project_path)
    mr = proj.mergerequests.get(iid)
    return mr.commits()

# -----------------------------------------------------------------------------
# Jira (requests, а не библиотека jira)
# -----------------------------------------------------------------------------

def jira_session(token: str, insecure: bool) -> requests.Session:
    s = requests.Session()
    s.verify = not insecure
    s.headers.update({
        "Authorization": f"Bearer {token}",
        "Accept": "application/json",
        "User-Agent": DEFAULT_UA,
    })
    return s

def jira_issue(session: requests.Session, base: str, key: str) -> dict:
    url = f"{base}/rest/api/2/issue/{key}"
    r = session.get(url, params={"fields": "summary,issuetype,parent"})
    if r.status_code != 200:
        raise RuntimeError(f"Jira {key}: HTTP {r.status_code}")
    return r.json()

def resolve_root_issue(session, base: str, key: str) -> JiraIssue:
    current = key
    while True:
        data = jira_issue(session, base, current)
        fields = data["fields"]
        parent = fields.get("parent")
        if not parent:
            return JiraIssue(
                key=current,
                summary=fields["summary"].replace("\n", " "),
                issuetype=fields["issuetype"]["name"],
            )
        current = parent["key"]

# -----------------------------------------------------------------------------
# CLI
# -----------------------------------------------------------------------------

@app.command()
def get(
    mr_url: str,
    gitlab_token: str = typer.Option(..., envvar="GITLAB_TOKEN"),
    jira_token: str = typer.Option(..., envvar="JIRA_TOKEN"),
    jira_base: str = DEFAULT_JIRA_BASE,
    insecure: bool = True,
    verbose: bool = False,
):
    setup_logging(verbose)

    host, project_path, iid = parse_mr_url(mr_url)
    gl = gitlab_client(f"https://{host}", gitlab_token, insecure)

    commits = get_mr_commits(gl, project_path, iid)

    key_rx = re.compile(DEFAULT_JIRA_KEY_RE)
    ignore_rx = [re.compile(p) for p in IGNORE_PATTERNS]

    keys: Set[str] = set()

    for c in commits:
        title = c.title or ""
        if any(rx.search(title) for rx in ignore_rx):
            continue
        keys |= set(key_rx.findall(c.message or ""))
        keys |= set(key_rx.findall(title))

    if not keys:
        console.print("❌ Jira issues не найдены")
        raise typer.Exit()

    session = jira_session(jira_token, insecure)

    roots: Dict[str, JiraIssue] = {}
    for k in sorted(keys):
        issue = resolve_root_issue(session, jira_base, k)
        roots.setdefault(issue.key, issue)

    table = Table(title=f"Jira issues in MR\n{mr_url}")
    table.add_column("Key", style="bold")
    table.add_column("Type")
    table.add_column("Summary")
    table.add_column("URL")

    for i in roots.values():
        table.add_row(i.key, i.issuetype, i.summary, i.url(jira_base))

    console.print(table)

# -----------------------------------------------------------------------------

if __name__ == "__main__":
    app()
