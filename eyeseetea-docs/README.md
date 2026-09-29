# EyeSeeTea Docs

This folder documents the EyeSeeTea fork of the DHIS2 Android SDK
(`dhis2-android-sdk`), consumed by `dhis2-android-capture-app-extra` and its
flavors via JitPack (`com.github.EyeSeeTea:dhis2-android-sdk`).

## Model: one fork, no client layer

`dhis2-android-capture-app-extra` has a two-layer fork model: Oslo upstream →
`develop-eyeseetea` shared baseline → per-client branches (`oca`,
`simprints`, ...), each with its own `AGENTS-<client>.md` and
`customizations/<client>/customization-files.md`.

This repository has a single layer: **Oslo upstream
(`dhis2/dhis2-android-sdk`) → this EyeSeeTea fork**. There is no
per-client branch here and no per-client inventory — every customization in
this repo is shared by every consumer of the published artifact, regardless
of which app flavor pulls it in. Do not port the app's client/flavor
machinery here (`AGENTS-<client>.md`, `customizations/<client>/`,
`new-fork.md`, `onboarding-fork-guide.md`) — it solves a "several clients on
one shared baseline" problem this repository does not have.

## Golden rules

- never assume a diff against upstream Oslo is a real customization without
  checking what the code actually does
- keep temporary upgrade progress in `upgrade/upgrade-<version>-notes.md`,
  not in `customizations.md`
- mark every surviving customization in code with
  `// EyeSeeTea customization - [Title]` (or `// EyeSeeTea fix - ...` for a
  pure Oslo bug fix), and keep `customizations.md` in sync

## Documents

- [`customizations.md`](customizations.md)
  Canonical inventory of every EyeSeeTea customization on top of Oslo:
  where it lives, what it does, and why it exists. Source of truth for the
  `// EyeSeeTea customization - [Title]` comments in code.
- [`upgrade/upgrade-plan.md`](upgrade/upgrade-plan.md)
  The upgrade runbook — phase-by-phase checklist for actually executing an
  Oslo upgrade. Start here when doing one.
- [`upgrade/conflict-rules.md`](upgrade/conflict-rules.md)
  Reusable merge rules referenced by the runbook: conflict classification,
  comment conventions, Java→Kotlin migration, automerge verification.
- [`upgrade/validation-checklist.md`](upgrade/validation-checklist.md)
  Manual validation flows for each customization, for the parts the
  automated suite alone doesn't give enough confidence on (real migrations,
  encrypted databases, account state).
- [`upgrade/template/upgrade-notes-template.md`](upgrade/template/upgrade-notes-template.md)
  Template for the temporary per-upgrade progress log
  (`upgrade/upgrade-<version>-notes.md`).

## Related

- [`EyeSeeTea.md`](/EyeSeeTea.md) (repo root) — how this fork is published to
  JitPack and consumed locally (composite build), not a customization
  inventory.
- App-side docs: `eyeseetea-docs/` in `dhis2-android-capture-app-extra`
  (a different repository) — covers the app's own client-fork model and how
  the app consumes this SDK artifact.

## Read this if...

### I want to know what's customized in this SDK fork

Read `customizations.md`.

### I'm upgrading this fork from a newer Oslo version

Read `upgrade/upgrade-plan.md` (the runbook) and follow its phases; it
points to `upgrade/conflict-rules.md` for the conflict-resolution rules and
to `customizations.md` for what must survive.

### I just built a customization and need to document it

Add a `// EyeSeeTea customization - [Title]` comment at the point of change
(see `upgrade/conflict-rules.md` for the exact convention), then add an
entry to `customizations.md` under the matching category.
