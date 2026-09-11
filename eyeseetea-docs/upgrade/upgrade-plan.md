# Upgrade Plan

Use this file when you are actually executing an Oslo upgrade.

Use `eyeseetea-docs/README.md` to understand the model first. Use this file
as the upgrade runbook.

## Preconditions

- the upgrade source is always a **tagged release** of upstream Oslo
  (`dhis2/dhis2-android-sdk`), never `upstream/develop` directly — Oslo's
  `develop` branch is a moving target; merging a released tag is what makes
  the upgrade reproducible and matches this fork's own history (`git tag |
  grep -E '^1\.1[0-9]'` lists the Oslo versions already brought in this
  way)
- this fork has no intermediate baseline branch to go through first before
  merging into a client — there is only one fork (see `README.md` "Model:
  one fork, no client layer")
- do not use `customizations.md` as a temporary merge notebook
- do not use `conflict-rules.md` as a temporary merge notebook
- do not treat a technical migration as a confirmed functional customization
  without checking the matching `openspec/specs/` capability, if one exists

## Documents to keep open during the upgrade

- `eyeseetea-docs/customizations.md`
- `openspec/specs/` (capability specs — functional source of truth for
  customizations that originated from an app feature)
- `eyeseetea-docs/upgrade/validation-checklist.md`
- `eyeseetea-docs/upgrade/conflict-rules.md`
- `eyeseetea-docs/upgrade/upgrade-<version>-notes.md`

## Developer checklist by phase

### Phase 1. Fetch and pin the target Oslo version

1. `git fetch upstream --tags`
2. Decide the target Oslo **tag** to upgrade to (e.g. `1.14.3`) — check
   https://github.com/dhis2/dhis2-android-sdk/releases for the intended
   version, not just the latest `develop` commit.
3. Create a local branch from that tag and push it to `origin` under the
   `upstream/<version>` naming this fork already uses:
   ```bash
   git checkout -b upstream/<version> <version>   # e.g. upstream/1.14.3
   git push origin upstream/<version>
   ```
4. Create the upgrade branch from `develop-eyeseetea`, named
   `feature/upgrade_<version>`:
   ```bash
   git checkout develop-eyeseetea
   git checkout -b feature/upgrade_<version>   # e.g. feature/upgrade_1.14.3
   ```
5. Copy `upgrade/template/upgrade-notes-template.md` to
   `upgrade/upgrade-<version>-notes.md` and fill in the header.

Done when:
- `origin/upstream/<version>` exists and points at the target Oslo tag
- the `feature/upgrade_<version>` branch exists, branched from
  `develop-eyeseetea`, and the notes file is ready

### Phase 2. Merge

1. From `feature/upgrade_<version>`, merge the upstream branch
   created in Phase 1:
   ```bash
   git merge origin/upstream/<version>
   ```
2. Resolve build-breaking conflicts only enough to get a compiling tree
   (`gradle/libs.versions.toml` conflicts on the version number itself are
   common here — see this fork's own history, e.g. commit `74501b1bd7`);
   do not resolve customization conflicts yet.

Done when:
- the merge is committed (or in progress with conflicts marked) and the
  tree at least parses

### Phase 3. Classification

1. Diff the upgrade branch against its pre-merge state (or use conflict
   markers) and classify every affected file:
   - easy conflict (`accept_theirs`)
   - manual conflict (`manual_reapply_on_theirs`)
   - needs validation (`defer_after_build_verification`)
   - shared non-conflict diff that may still be a customization (git
     auto-merged it silently — see `conflict-rules.md`'s automerge
     verification rule)
2. Cross-check every file listed in `customizations.md` explicitly, even if
   git did not report it as conflicted — the automerge verification rule in
   `conflict-rules.md` exists precisely because clean auto-merges can drop
   customization code with no conflict markers.
3. Record each file's classification, expected delta, and linked
   customization (or `n/a`) in `upgrade-<version>-notes.md`.

Done when:
- every file in `customizations.md` has been explicitly re-checked against
  the new upstream, not just the files git flagged as conflicted
- easy and hard work are separated before editing starts

### Phase 4. Resolve the easy batch

1. Resolve `accept_theirs` files.
2. Review what was resolved automatically before moving to manual
   conflicts.

Why:
- this is the checkpoint to catch anything misclassified before deeper,
  harder-to-undo edits start

### Phase 5. Resolve manual conflicts

For each file classified `manual_reapply_on_theirs`:

1. Confirm the minimum EyeSeeTea-specific behavior that must survive.
2. Confirm whether the customization still exists or has been absorbed by
   the new upstream version.
3. If the customization originated from an app feature with an OpenSpec
   capability, use the exact title from that spec's top-level `#` heading
   for the `// EyeSeeTea customization - [Title]` comment; otherwise keep
   the existing title from `customizations.md`.
4. If the surviving file used to be Java and Oslo's new version replaced it
   with a Kotlin equivalent, follow the "File migration rule: Java to
   Kotlin" in `conflict-rules.md` before deciding where the customization
   now lives.
5. Decide whether the final diff is acceptable (see "Conflict minimization
   rule" and "Stop and redo rule" in `conflict-rules.md`).

Done when:
- the branch keeps only intentional custom behavior, each with an
  identifiable functional reason

### Phase 6. Validate

```bash
./gradlew ktlintCheck
./gradlew testDebugUnitTest testDhis2DebugUnitTest testAndroidHostTest
```

1. Run targeted `androidTest` for every module touched by a customization
   (see `AGENTS.md` for per-module task naming).
2. Work through `upgrade/validation-checklist.md` for every customization
   that has a manual check listed there.
3. Confirm each active customization has either a targeted automated test
   or a manual validation entry — not neither.

### Phase 7. Finalize

1. Compare the surviving diff against the new upstream Oslo version for
   every file in `customizations.md`.
2. Identify any remaining differences not linked to a known customization.
   Either document them (move to `customizations.md` section 1, or add to
   section 3 as a pending candidate) or remove them if obsolete.
3. Confirm that each customization in `customizations.md` is:
   - still present and validated, or
   - explicitly marked `absorbed` or `removed`
4. Confirm the "Candidates pending confirmation" section does not silently
   grow without anyone reviewing it — either confirm or drop each entry
   touched by this upgrade.
5. Update `gradle/libs.versions.toml` (fork version) and `buildSrc/src/main/kotlin/Props.kt`
   (POM metadata) if this upgrade also bumps the fork's own version — see
   `EyeSeeTea.md` for what those fields mean.
6. Do not close the upgrade while unexplained diff remains against the new
   upstream in any file listed in `customizations.md`.
7. Delete or archive `upgrade-<version>-notes.md` once the upgrade is
   stable — it is not permanent documentation.

Done when:
- there are no unexplained surviving differences against the new upstream
  in any customized file
- `customizations.md`, code comments, and (if applicable) the OpenSpec spec
  describe the same final state
- the full validation suite (Phase 6) is green

## AI agent support

The AI agent may help with:
- diffing the upgrade branch against upstream
- classifying conflicts by the rules in `conflict-rules.md`
- resolving obvious `accept_theirs` cases
- updating `upgrade-<version>-notes.md`
- keeping the user informed at the end of each batch

When resolving a manually-conflicted customized file, the agent should:
- start from the new upstream version
- reapply only the minimum customization logic
- avoid reintroducing obsolete code
- if the customization can be isolated cleanly, move the custom helper,
  function, or constants block toward the end of the file (see "Code
  placement convention" in `conflict-rules.md`)
- add or preserve a nearby `// EyeSeeTea customization - [Title]` comment
- update `customizations.md` only if the customization still exists after
  the merge
- if the file still differs but the business meaning is unclear, record it
  in `upgrade-<version>-notes.md` as `needs_validation`, not in
  `customizations.md`

## AI agent limits

The AI agent must not automatically:
- merge upstream directly into a branch other than the dedicated upgrade
  branch
- rewrite all conflicts by blindly taking one side
- treat `upgrade-<version>-notes.md` as stable long-term documentation
- remove customization logic when the impact of removing it is unclear

## Documentation placement rule

- temporary, upgrade-specific information → `upgrade-<version>-notes.md`
- reusable merge rule → `conflict-rules.md`
- confirmed customization that survives in the final code → `customizations.md`

## Minimal agent checklist

1. Confirm current branch (`feature/upgrade_<version>`) and that
   `origin/upstream/<version>` exists and points at the intended Oslo tag.
2. Read `conflict-rules.md`.
3. Classify conflicts and surviving diff before editing files (Phase 3).
4. Resolve easy conflicts first (Phase 4).
5. Pause for user review after the easy batch unless instructed otherwise.
6. Resolve manual conflicts by reapplying minimal customization logic
   (Phase 5).
7. Ensure each surviving customization has a nearby
   `// EyeSeeTea customization - [Title]` comment.
8. Update `customizations.md` only with confirmed surviving customizations.
9. Run the full validation suite (Phase 6) before closing.
10. Do not close the upgrade while unexplained diff remains.

## Done when

- the upgrade merged a tagged Oslo release via `origin/upstream/<version>`,
  not `upstream/develop` directly or a stale/unofficial mirror
- temporary decisions live in `upgrade-<version>-notes.md`, then get deleted
- final surviving customizations live in `customizations.md`
- customization titles are aligned across code comments, `customizations.md`,
  and (when applicable) the OpenSpec spec
- unexplained diff against the new upstream does not remain open in any
  customized file
