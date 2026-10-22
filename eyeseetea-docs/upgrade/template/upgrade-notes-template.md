# Upgrade Notes Template

Use this file as the temporary working notes for one concrete Oslo upgrade.

Recommended filename after copying:
- `eyeseetea-docs/upgrade/upgrade-<target-version>-notes.md`

## Purpose

This file is for:
- temporary upgrade progress
- conflict decisions taken during the current upgrade
- unresolved questions
- follow-up checks before closing the upgrade

This file is not for:
- stable merge rules (those go in `conflict-rules.md`)
- final customization inventory (that goes in `customization-files.md`)
- functional documentation (that goes in `openspec/specs/`)

## Header

- Upstream source: `dhis2/dhis2-android-sdk`
- From Oslo version: `<from>`
- To Oslo version (tag): `<version>`
- Upstream tag branch: `origin/upstream/<version>`
- Upgrade branch: `feature/upgrade_<version>`
- OpenSpec change: `openspec/changes/upgrade-to-<version>/`
- Started on: `<date>`
- Status: `in_progress`

## Progress

- upstream tag branch pushed: `yes/no`
- upgrade change proposed: `yes/no`
- merge started: `yes/no`
- easy conflicts resolved: `yes/no`
- manual conflicts pending: `yes/no`
- validation started: `yes/no`

## Decisions

| File | Classification | Expected delta | Customization | Status | Notes |
|------|----------------|----------------|---------------|--------|-------|
| path/to/file | accept_theirs / manual_reapply_on_theirs / defer_after_build_verification | one helper call / one assertion / absorbed upstream | spec title or `n/a` | pending / resolved_keep_theirs / resolved_manual_merge / needs_validation | short reason |

## Open Questions

- question 1

## Validation Notes

- `./gradlew :core:ktlintCheck :core:detekt`:
- `./gradlew :core:testDebugUnitTest`:
- `./gradlew :core:apiCheck` (or `:core:releaseApiCheck`):
- targeted `connectedDebugAndroidTest` (see `validation-checklist.md`):
- manual checks (see `validation-checklist.md`):
- `python3 eyeseetea-docs/scripts/check_upgrade_docs.py`:

## Finalization

- surviving customizations reflected in specs and `customization-files.md`: `yes/no`
- absorbed customizations removed (code, spec, inventory, checklist): `yes/no`
- stable rules moved to `conflict-rules.md`: `yes/no`
- unexplained diff remaining against upstream: `yes/no`
- upgrade change archived and this file deleted: `yes/no`
