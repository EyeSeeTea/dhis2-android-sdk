#!/usr/bin/env python3
"""Consistency checks for the EyeSeeTea SDK fork documentation.

Adapted from the app's `eyeseetea-docs/scripts/check_upgrade_docs.py` to this
repository's single fork layer (no per-client folders). It checks that:

- relative markdown links inside `eyeseetea-docs/` resolve
- every spec in `openspec/specs/` starts with a human `# <Title>`
- spec titles, `customization-files.md` section 2 headings and
  `upgrade/validation-checklist.md` section headings are the same set
- every section 2 entry of `customization-files.md` has a `Status:` line
- every `EyeSeeTea customization` marker in code has a title, and that title
  is a spec title
- every code file touched by a feature commit listed in section 4 of
  `customization-files.md` is listed under the matching section 2 entry

Run it from anywhere: `python3 eyeseetea-docs/scripts/check_upgrade_docs.py`.
"""

from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
DOCS_ROOT = ROOT / "eyeseetea-docs"
SPECS_ROOT = ROOT / "openspec" / "specs"
INVENTORY_PATH = DOCS_ROOT / "customization-files.md"
CHECKLIST_PATH = DOCS_ROOT / "upgrade" / "validation-checklist.md"

CODE_EXTENSIONS = {".kt", ".java", ".kts", ".xml", ".sql"}
SKIPPED_DIRS = {".git", ".gradle", ".idea", ".kotlin", "build", "eyeseetea-docs", "openspec"}

MARKER_RE = re.compile(r"EyeSeeTea customization(.*)")
TITLED_MARKER_RE = re.compile(r"^ - (.+?)\s*(?:-->|\*/)?\s*$")
LINK_RE = re.compile(r"\[[^\]]+\]\(([^)]+)\)")
H1_RE = re.compile(r"^#\s+(.+)$")
TOP_LEVEL_SECTION_RE = re.compile(r"^##\s+(\d+)\.")
INVENTORY_SUBSECTION_RE = re.compile(r"^###\s+\d+\.\d+\s+(.+)$")
CHECKLIST_SECTION_RE = re.compile(r"^##\s+(\d+)\.\s+(.+)$")
FILE_BULLET_RE = re.compile(r"^-\s+`([^`]+)`")
FEAT_COMMIT_RE = re.compile(r"^-\s+`([0-9a-f]{7,40})`\s*[—-]")


def run_git(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", *args],
        cwd=ROOT,
        text=True,
        capture_output=True,
        check=False,
    )


def collect_markdown_links() -> list[str]:
    errors: list[str] = []
    for path in sorted(DOCS_ROOT.rglob("*.md")):
        text = path.read_text(encoding="utf-8")
        for lineno, line in enumerate(text.splitlines(), start=1):
            for raw_link in LINK_RE.findall(line):
                if raw_link.startswith(("http://", "https://", "mailto:", "#")):
                    continue
                target = raw_link.split("#", 1)[0]
                if not target:
                    continue
                if not (path.parent / target).resolve().exists():
                    errors.append(f"{path.relative_to(ROOT)}:{lineno} broken link -> {raw_link}")
    return errors


def collect_spec_titles(issues: list[str]) -> set[str]:
    titles: set[str] = set()
    if not SPECS_ROOT.is_dir():
        return titles
    for spec_file in sorted(SPECS_ROOT.glob("*/spec.md")):
        first_line = next(
            (line.strip() for line in spec_file.read_text(encoding="utf-8").splitlines() if line.strip()),
            "",
        )
        match = H1_RE.match(first_line)
        if not match:
            issues.append(f"{spec_file.relative_to(ROOT)} does not start with a '# <Title>' line")
            continue
        title = match.group(1).strip()
        if title.endswith(" Specification") or title == spec_file.parent.name:
            issues.append(
                f"{spec_file.relative_to(ROOT)} title '{title}' looks generated — use the human customization title"
            )
        titles.add(title)
    return titles


def parse_inventory() -> tuple[dict[str, set[str]], dict[str, list[str]], list[str]]:
    """Parse `customization-files.md`.

    Returns:
      - files: title -> file paths listed under its section 2 entry
      - commits: title -> commit SHAs listed under its section 4 entry
      - titles in section 2 without a `Status:` line
    """
    files: dict[str, set[str]] = {}
    commits: dict[str, list[str]] = {}
    missing_status: list[str] = []
    if not INVENTORY_PATH.exists():
        return files, commits, missing_status

    section: str | None = None
    title: str | None = None
    has_status = False

    def close_entry() -> None:
        if section == "2" and title and not has_status:
            missing_status.append(title)

    for raw_line in INVENTORY_PATH.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        top = TOP_LEVEL_SECTION_RE.match(line)
        if top:
            close_entry()
            section, title, has_status = top.group(1), None, False
            continue
        sub = INVENTORY_SUBSECTION_RE.match(line)
        if sub:
            close_entry()
            title, has_status = sub.group(1).strip(), False
            if section == "2":
                files.setdefault(title, set())
            elif section == "4":
                commits.setdefault(title, [])
            continue
        if not title:
            continue
        if section == "2":
            if line.startswith("Status:"):
                has_status = True
            match = FILE_BULLET_RE.match(line)
            if match:
                files[title].add(match.group(1).strip())
        elif section == "4":
            match = FEAT_COMMIT_RE.match(line)
            if match:
                commits[title].append(match.group(1))
    close_entry()
    return files, commits, missing_status


def collect_checklist_titles() -> set[str]:
    titles: set[str] = set()
    if not CHECKLIST_PATH.exists():
        return titles
    for line in CHECKLIST_PATH.read_text(encoding="utf-8").splitlines():
        match = CHECKLIST_SECTION_RE.match(line.strip())
        if match and match.group(1) != "0":
            titles.add(match.group(2).strip())
    return titles


def collect_code_markers() -> tuple[dict[str, list[str]], list[str]]:
    """Return title -> locations for titled markers, and locations of untitled ones."""
    titled: dict[str, list[str]] = {}
    untitled: list[str] = []
    for path in sorted(ROOT.rglob("*")):
        if path.suffix not in CODE_EXTENSIONS or not path.is_file():
            continue
        relative = path.relative_to(ROOT)
        if any(part in SKIPPED_DIRS for part in relative.parts):
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        for lineno, line in enumerate(text.splitlines(), start=1):
            marker = MARKER_RE.search(line)
            if not marker:
                continue
            location = f"{relative}:{lineno}"
            titled_marker = TITLED_MARKER_RE.match(marker.group(1))
            if titled_marker:
                titled.setdefault(titled_marker.group(1).strip(), []).append(location)
            else:
                untitled.append(location)
    return titled, untitled


def commit_touched_files(sha: str) -> tuple[list[str] | None, str | None]:
    result = run_git("show", "--name-only", "--pretty=format:", sha)
    if result.returncode != 0:
        return None, result.stderr.strip() or f"unknown git failure for {sha}"
    return [line for line in result.stdout.splitlines() if line.strip()], None


def check_feat_commit_coverage(files: dict[str, set[str]], commits: dict[str, list[str]]) -> list[str]:
    issues: list[str] = []
    for title in sorted(set(files) - set(commits)):
        issues.append(f"'{title}' has no feature commits in section 4 of customization-files.md")
    for title, shas in sorted(commits.items()):
        if title not in files:
            issues.append(f"section 4 entry '{title}' has no section 2 entry in customization-files.md")
            continue
        if not shas:
            issues.append(f"'{title}' has an empty feature commit list in section 4")
        for sha in shas:
            touched, error = commit_touched_files(sha)
            if touched is None:
                issues.append(f"could not read feature commit {sha} of '{title}': {error}")
                continue
            for rel_path in touched:
                if Path(rel_path).suffix not in CODE_EXTENSIONS:
                    continue
                if not (ROOT / rel_path).exists():
                    # Renamed or deleted since; the inventory tracks its successor.
                    continue
                if rel_path not in files[title]:
                    issues.append(
                        f"'{title}': feature commit {sha} touches '{rel_path}', "
                        "which is not listed under its section 2 entry"
                    )
    return issues


def main() -> int:
    issues: list[str] = []

    broken_links = collect_markdown_links()
    if broken_links:
        issues.append("Broken markdown links:")
        issues.extend(f"  - {entry}" for entry in broken_links)

    spec_titles = collect_spec_titles(issues)
    inventory_files, inventory_commits, missing_status = parse_inventory()
    inventory_titles = set(inventory_files)
    checklist_titles = collect_checklist_titles()

    for title in missing_status:
        issues.append(f"customization-files.md entry '{title}' has no 'Status:' line")
    for title in sorted(spec_titles - inventory_titles):
        issues.append(f"spec '{title}' has no section 2 entry in customization-files.md")
    for title in sorted(inventory_titles - spec_titles):
        issues.append(f"customization-files.md entry '{title}' has no spec with that title")
    for title in sorted(spec_titles - checklist_titles):
        issues.append(f"spec '{title}' has no section in validation-checklist.md")
    for title in sorted(checklist_titles - spec_titles):
        issues.append(f"validation-checklist.md section '{title}' has no spec with that title")

    titled_markers, untitled_markers = collect_code_markers()
    for title, locations in sorted(titled_markers.items()):
        if title not in spec_titles:
            issues.append(f"code marker title '{title}' is not a spec title ({', '.join(locations)})")
    for location in untitled_markers:
        issues.append(f"code marker without '- <Title>' at {location}")

    issues.extend(check_feat_commit_coverage(inventory_files, inventory_commits))

    if issues:
        print("\n".join(issues))
        return 1
    print("All EyeSeeTea doc checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
