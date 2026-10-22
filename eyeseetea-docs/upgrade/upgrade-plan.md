# Upgrade Plan

Use this file when you are actually executing an Oslo upgrade.

Use `eyeseetea-docs/README.md` to understand the model first. Use this file
as the upgrade runbook.

## Preconditions

- the upgrade source is always a **tagged release** of upstream Oslo
  (`dhis2/dhis2-android-sdk`), never `upstream/develop` — Oslo's `develop` is a
  moving target; merging a released tag makes the upgrade reproducible
- there is no intermediate baseline to go through: the upgrade lands in
  `develop-eyeseetea` directly (see `README.md`, "Model: one fork, no client
  layer")
- every active customization has a spec in `openspec/specs/` and an entry in
  `customization-files.md` — if section 3 of `customization-files.md` still
  holds unclassified drift, do the onboarding inventory first (`README.md`,
  "Onboarding the existing customizations"): an upgrade cannot verify what was
  never inventoried
- do not use `customization-files.md` or `conflict-rules.md` as a temporary
  merge notebook
- do not treat a technical migration as a confirmed customization without
  checking the matching spec

## Documents to keep open during the upgrade

- `openspec/specs/` (all specs — what must survive)
- `eyeseetea-docs/customization-files.md` (where it lives)
- `eyeseetea-docs/upgrade/validation-checklist.md`
- `eyeseetea-docs/upgrade/conflict-rules.md`
- `eyeseetea-docs/upgrade/upgrade-<version>-notes.md`
- `openspec/changes/upgrade-to-<version>/`

## Developer checklist by phase

### Phase 1. Fetch, pin and plan the target Oslo version

1. `git fetch upstream --tags`
2. Decide the target Oslo **tag** (e.g. `1.15.0`) — check
   https://github.com/dhis2/dhis2-android-sdk/releases, not just the latest
   `develop` commit.
3. Create a local branch from that tag and push it to `origin` under the
   `upstream/<version>` naming this fork already uses:
   ```bash
   git checkout -b upstream/<version> <version>   # e.g. upstream/1.15.0
   git push origin upstream/<version>
   ```
4. Create the upgrade branch from `develop-eyeseetea`:
   ```bash
   git checkout develop-eyeseetea
   git checkout -b feature/upgrade_<version>   # e.g. feature/upgrade_1.15.0
   ```
5. Create the upgrade OpenSpec change with `/opsx:propose upgrade-to-<version>`
   (e.g. `upgrade-to-1-15-0`). The proposal states the target Oslo version and
   the expected conflict surface (which customizations touch Oslo files that
   changed between the two tags); `tasks.md` mirrors the phases of this file.
   The change preserves every active spec at the requirements level — only the
   baseline moves.
6. Copy `upgrade/template/upgrade-notes-template.md` to
   `upgrade/upgrade-<version>-notes.md` and fill in the header.

Done when:
- `origin/upstream/<version>` exists and points at the target Oslo tag
- `feature/upgrade_<version>` exists, branched from `develop-eyeseetea`
- the upgrade change and the notes file exist

### Phase 2. Merge

1. From `feature/upgrade_<version>`, merge the upstream branch:
   ```bash
   git merge origin/upstream/<version>
   ```
2. Resolve build-breaking conflicts only enough to get a compiling tree
   (`gradle/libs.versions.toml` conflicts on the version number itself are
   common here); do not resolve customization conflicts yet.

Done when:
- the merge is committed (or in progress with conflicts marked) and the tree
  at least parses

### Phase 3. Classification

1. Classify every affected file:
   - easy conflict (`accept_theirs`)
   - manual conflict (`manual_reapply_on_theirs`)
   - needs validation (`defer_after_build_verification`)
   - non-conflict diff that may still be a customization (git auto-merged it
     silently — see the automerge verification rule in `conflict-rules.md`)
2. Cross-check every file listed in `customization-files.md` explicitly, even
   if git did not report it as conflicted.
3. Record each file's classification, expected delta, and linked
   customization title (or `n/a`) in `upgrade-<version>-notes.md`.

Done when:
- every file in `customization-files.md` has been re-checked against the new
  upstream, not just the files git flagged as conflicted
- easy and hard work are separated before editing starts

### Phase 4. Resolve the easy batch

1. Resolve `accept_theirs` files.
2. Review what was resolved automatically before moving to manual conflicts.

Why:
- this is the checkpoint to catch anything misclassified before deeper,
  harder-to-undo edits start

### Phase 5. Resolve manual conflicts

For each file classified `manual_reapply_on_theirs`:

1. Confirm the minimum EyeSeeTea-specific behavior that must survive (the
   spec says what it is).
2. Confirm whether the customization still exists or has been absorbed by
   the new upstream version.
3. Keep or add the `// EyeSeeTea customization - <Title>` comment, with the
   exact `# <Title>` of the spec.
4. If the surviving file used to be Java and Oslo replaced it with Kotlin,
   follow the "File migration rule: Java to Kotlin" in `conflict-rules.md`.
5. Decide whether the final diff is acceptable.

Done when:
- the branch keeps only intentional custom behavior, each traceable to a spec

### Phase 6. Validate

```bash
./gradlew :core:ktlintCheck :core:detekt
./gradlew :core:testDebugUnitTest
./gradlew :core:apiCheck            # Oslo 1.15.0+: :core:releaseApiCheck
```

1. Run the targeted `androidTest` classes listed per customization in
   `upgrade/validation-checklist.md` (`./gradlew :core:connectedDebugAndroidTest ...`,
   needs a device or emulator).
2. Work through the manual checks of `upgrade/validation-checklist.md`.
3. Confirm each active customization has either a targeted automated test or
   a manual validation entry — not neither.

### Phase 7. Finalize

1. Two-dot diff the result against the new upstream for every file in
   `customization-files.md` (`git diff origin/upstream/<version>..HEAD -- <file>`).
2. Identify any remaining differences not linked to a known customization.
   Either document them (spec + section 2, or section 3 with a reason) or
   remove them.
3. Confirm each spec is either still present and validated, or explicitly
   marked `absorbed`/`removed` (remove the spec and its section 2 entry).
4. Update `dhis2AndroidSdkVersion` in `gradle/libs.versions.toml` to
   `<version>-eyeseetea-fork-1` and update the header of
   `customization-files.md`.
5. Run `python3 eyeseetea-docs/scripts/check_upgrade_docs.py` and
   `openspec validate --specs`.
6. Archive the upgrade change (`/opsx:archive`) and delete
   `upgrade-<version>-notes.md`.

Done when:
- no unexplained difference against the new upstream remains in any
  customized file
- specs, `customization-files.md`, the checklist and code comments describe
  the same final state
- the full validation suite (Phase 6) is green

## AI agent support

The AI agent may help with:
- diffing the upgrade branch against upstream
- drafting the upgrade proposal (`/opsx:propose`)
- classifying conflicts by the rules in `conflict-rules.md`
- resolving obvious `accept_theirs` cases
- updating `upgrade-<version>-notes.md`
- keeping the user informed at the end of each batch

When resolving a manually-conflicted customized file, the agent should:
- start from the new upstream version
- reapply only the minimum customization logic
- avoid reintroducing obsolete code
- if the customization can be isolated cleanly, move the custom helper,
  function, or constants block toward the end of the file
- add or preserve a nearby `// EyeSeeTea customization - <Title>` comment
- update `customization-files.md` only if the customization still exists
  after the merge
- if the file still differs but the business meaning is unclear, record it in
  `upgrade-<version>-notes.md` as `needs_validation`

## AI agent limits

The AI agent must not automatically:
- merge upstream into any branch other than the dedicated upgrade branch
- rewrite all conflicts by blindly taking one side
- treat `upgrade-<version>-notes.md` as stable documentation
- remove customization logic when the impact of removing it is unclear
- create, rename or retire a spec without developer confirmation

## Documentation placement rule

- temporary, upgrade-specific information → `upgrade-<version>-notes.md`
- reusable merge rule → `conflict-rules.md`
- what a surviving customization does → its spec in `openspec/specs/`
- where a surviving customization lives → `customization-files.md`

## Minimal agent checklist

1. Confirm current branch (`feature/upgrade_<version>`) and that
   `origin/upstream/<version>` points at the intended Oslo tag.
2. Read `conflict-rules.md`.
3. Classify conflicts and surviving diff before editing files (Phase 3).
4. Resolve easy conflicts first (Phase 4).
5. Pause for user review after the easy batch unless instructed otherwise.
6. Resolve manual conflicts by reapplying minimal customization logic
   (Phase 5).
7. Ensure each surviving customization has a nearby
   `// EyeSeeTea customization - <Title>` comment.
8. Update `customization-files.md` only with confirmed surviving
   customizations.
9. Run the validation suite (Phase 6) and `check_upgrade_docs.py` before
   closing.
10. Do not close the upgrade while unexplained diff remains.
