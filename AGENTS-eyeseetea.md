# EyeSeeTea Fork — dhis2-android-sdk

EyeSeeTea fork of the DHIS2 Android SDK. This file holds the fork-specific
guidance; it is imported from `CLAUDE.md`. Oslo ships its own `CLAUDE.md` from
`1.15.0` on — keep the Oslo text untouched and only add the
`@AGENTS-eyeseetea.md` import line to it, so upstream changes merge cleanly.

- **Upstream:** `dhis2/dhis2-android-sdk` (remote `upstream`), merged from tagged releases only
- **Fork branch:** `develop-eyeseetea` (one layer: Oslo → this fork, no client branches)
- **Current version:** `dhis2AndroidSdkVersion` in `gradle/libs.versions.toml` (`<oslo>-eyeseetea-fork-<n>`)
- **Published as:** JitPack `com.github.EyeSeeTea:dhis2-android-sdk:android-core:<tag>` (see `EyeSeeTea.md`)
- **Consumers:** `EyeSeeTea/dhis2-android-capture-app` (`develop-eyeseetea` and client branches such as `feature-oca/*`, `feature-widp/*`)
- **Upstream tag branches:** `origin/upstream/<version>`; upgrade branches `feature/upgrade_<version>`

## Build and test

```bash
./gradlew :core:assembleDebug
./gradlew :core:testDebugUnitTest
./gradlew :core:testDebugUnitTest --tests "org.hisp.dhis.android.core.user.internal.LogInCallUnitShould"
./gradlew :core:connectedDebugAndroidTest      # needs a device/emulator
./gradlew :core:ktlintCheck :core:detekt
./gradlew :core:ktlintFormat
./gradlew :core:apiDump                         # after an intentional public API change (1.15.0+: releaseApiDump)
./gradlew runChecks                             # clean + ktlint, detekt, checkstyle, pmd, lint
```

Unit tests live in `core/src/test/` (`<Class>Should.kt`); tests that need a
real database go in `core/src/androidTest/` (`<Class>IntegrationShould.kt`).
Never edit `core/api/core.api` by hand.

## Customizations

One OpenSpec spec per customization in `openspec/specs/`; where each one lives
is in `eyeseetea-docs/customization-files.md`.

| # | Spec slug | Title | Status | Risk |
|---|-----------|-------|--------|------|
| 1 | `synced-data-retention-purge` | Synced Data Retention Purge | active | low |
| 2 | `synced-data-relationships-purge` | Synced Data Relationships Purge | active | low |
| 3 | `synced-data-retention-scope` | Synced Data Retention Scope | active | medium |
| 4 | `disabled-account-login` | Disabled Account Login Handling | active | medium |
| 5 | `two-factor-login` | Two-Factor Authentication Login | active | high |
| 6 | `automatic-logout-on-unauthorized` | Automatic Logout On Unauthorized Session | active | medium |
| 7 | `change-server-url` | Change Server URL | active | medium |
| 8 | `original-size-image-download` | Original Size Image Download | broken | high |
| 9 | `url-data-element` | URL Data Element Field | active | medium |

Oslo bug fixes (`// EyeSeeTea fix`) have no spec; they are listed with their
retirement condition in section 3 of `customization-files.md`.

### Customization code rules

**Principle: minimize changes to Oslo code.** Every line modified in an Oslo
file is a future merge conflict. Look for a solution that avoids touching Oslo
files first; check `eyeseetea-docs/customization-techniques.md` for known
mechanisms.

**Placement hierarchy** (prefer top options):
1. New EyeSeeTea-owned package or class (e.g. `retention/`) — zero conflict risk
2. New file in an existing Oslo package — no Oslo file touched
3. Block appended at the end of an existing Oslo file — low conflict risk
4. Inline edit in Oslo code — last resort, highest conflict risk

**Comment convention:** every customized point has
`// EyeSeeTea customization - <Title>`, where `<Title>` is exactly the
top-level `# <Title>` of the matching spec. Place it right above the
customized block (for a new file in an owned package, below the package
declaration), never on import lines. Use
`// EyeSeeTea fix - <description> (Oslo <ticket>, introduced <version>)` for a
pure Oslo bug fix. See `eyeseetea-docs/upgrade/conflict-rules.md`.

**Public API:** prefer reusing an existing public type or `D2ErrorCode` over
adding one; every public addition shows up in `core.api` and becomes a
contract with the consumer apps.

**Automerge verification:** after any merge of Oslo, run
`git diff origin/upstream/<version>..HEAD -- <file>` for **every file listed in
`customization-files.md`**, not only the files git marked as conflicted. Git
automerge can silently drop customization code. Use **two-dot diffs** —
three-dot misses deletions on the upstream side.

**Consistency check:** `python3 eyeseetea-docs/scripts/check_upgrade_docs.py`
verifies that spec titles, `customization-files.md`, the validation checklist
and code comments agree, and that every file of a feature commit is in the
inventory. Run it before committing documentation or customization changes.

## Key documentation

- `openspec/specs/` — what each customization does (source of truth for titles)
- `openspec/config.yaml` — project context and OpenSpec rules
- `eyeseetea-docs/README.md` — documentation model and onboarding
- `eyeseetea-docs/customization-files.md` — technical inventory and feature commits
- `eyeseetea-docs/customization-techniques.md` — reusable customization mechanisms
- `eyeseetea-docs/upgrade/upgrade-plan.md` — upgrade runbook
- `eyeseetea-docs/upgrade/conflict-rules.md` — merge conflict resolution rules
- `eyeseetea-docs/upgrade/validation-checklist.md` — validation per customization
- `EyeSeeTea.md` — JitPack publication and local composite build

## Automation extraction rule

Track repetitive patterns during the conversation. If you observe the same
task structure executed 3+ times with different inputs (e.g. resolving
conflicts with the same strategy, writing tests with the same shape, applying
the same transformation across files), proactively suggest extracting it into:
- an **AGENTS-eyeseetea.md rule** if it's a guideline (3-5 repetitions)
- an **agent** if it's a multi-step autonomous task (6+ repetitions)
- a **skill** if it requires a specialized protocol not derivable from context

State what pattern you detected, how many times it occurred, and a concrete
proposal for the extraction. Do not create the artifact — propose it and wait
for approval.
