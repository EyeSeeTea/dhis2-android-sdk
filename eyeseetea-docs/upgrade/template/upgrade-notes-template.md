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
- final customization inventory (that goes in `customizations.md`)
- long-term functional documentation

## Header

- Upstream source: `dhis2/dhis2-android-sdk`
- From Oslo version/commit: `<from>`
- To Oslo version (tag): `<version>`
- Upstream tag branch: `upstream/<version>`
- Upgrade branch: `feature/upgrade_<version>`
- Started on: `<date>`
- Status: `in_progress`

## Progress

- upstream fetched: `yes/no`
- merge started: `yes/no`
- easy conflicts resolved: `yes/no`
- manual conflicts pending: `yes/no`
- validation started: `yes/no`

## Decisions

| File | Classification | Expected delta | Customization | Status | Notes |
|------|----------------|----------------|---------------|--------|-------|
| path/to/file | accept_theirs / manual_reapply_on_theirs / defer_after_build_verification | one helper call / one assertion / absorbed upstream | customization title or `n/a` | pending / resolved_keep_theirs / resolved_manual_merge / needs_validation | short reason |

## Open Questions

- question 1
- question 2

## Validation Notes

- `./gradlew ktlintCheck`:
- `./gradlew testDebugUnitTest testDhis2DebugUnitTest testAndroidHostTest`:
- targeted `androidTest` for touched modules:
- manual checks (see `upgrade/validation-checklist.md`):

## Finalization

- surviving customizations moved to `customizations.md`: `yes/no`
- stable rules moved to `conflict-rules.md`: `yes/no`
- temporary notes ready to archive/remove: `yes/no`
- unexplained diff remaining against upstream: `yes/no`
