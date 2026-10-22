# Customization Techniques

Reusable techniques for customizing Oslo code in this SDK fork.

Use this file when you need to change SDK behavior and want to know **how**
this fork has already solved the same shape of problem. Each entry says what
the technique is for, how it works, which customization uses it, and what it
costs at upgrade time.

This is not an inventory — that lives in `customization-files.md`. This is the
catalogue of *mechanisms*. The app keeps its own catalogue in its
`eyeseetea-docs/customization-techniques.md`.

Before inventing a new mechanism, check whether one of these already fits.
After inventing one, add it here.

The techniques serve the placement hierarchy in `upgrade/conflict-rules.md`:
EyeSeeTea-owned package → new file in an Oslo package → block at the end of an
Oslo file → inline edit.

---

## T1. A new public `<Domain>Module` entry point

**Use when:** a customization adds a capability consumers call directly,
rather than changing an existing Oslo flow.

**Used by:** Synced Data Retention Scope — `D2.retentionModule().purge()`.

**How it works.** It follows the pattern Oslo uses for its own modules
(`WipeModule`, `SettingModule`), so everything except the wiring lives in an
EyeSeeTea-owned package:

| File | Placement | Role |
|---|---|---|
| `retention/RetentionModule.kt` | owned package | Public interface — the only public type |
| `retention/internal/RetentionModuleImpl.kt` | owned package | `@Singleton internal` implementation |
| `retention/internal/RetentionDIModule.kt` | owned package | `@Module @ComponentScan` Koin module |
| `arch/d2/internal/D2DIComponentFactory.kt` | Oslo, inline | Registers `RetentionDIModule().module` |
| `arch/d2/internal/D2DIComponent.kt` | Oslo, inline | Constructor property `retentionModule` |
| `D2.kt` | Oslo, inline | `fun retentionModule()` accessor |

**Gotcha.** Forgetting the `D2DIComponentFactory` registration compiles and
passes unit tests, but Koin fails at runtime when `D2` is built (fixed in
`621c988be3`). Only an instrumented test or the consumer app catches it.

**Upgrade cost.** Low. Three one-line edits in Oslo files that Oslo also edits
whenever it adds a module, so conflicts there are frequent but mechanical:
keep both sides. `core.api` changes and must be regenerated, not merged.

---

## T2. Map a server value to an existing public `D2ErrorCode`

**Use when:** a customization must report a new server condition to the
consumer, and an existing `D2ErrorCode` already means the same thing.

**Used by:** Disabled Account Login Handling — the `/api/auth/login`
`loginStatus = ACCOUNT_DISABLED` wire value is kept as a private constant in
`LogInCall.kt` and reported as the existing `D2ErrorCode.USER_ACCOUNT_DISABLED`.

**How it works.** The wire value stays internal (`private companion object`
constant); the public contract is the existing enum value. `core.api` does not
change.

**Upgrade cost.** Low. Adding a value to `D2ErrorCode.java` instead would be a
public API change that every consumer must handle and that conflicts every
time Oslo adds its own codes next to it. Prefer reusing a code; add one only
when no existing code has the same meaning (the 2FA codes are an example where
there was none).

---

## T3. An extra query on an existing Oslo store

**Use when:** a customization in an owned package needs data that only an Oslo
store can read efficiently.

**Used by:** Synced Data Retention Scope —
`DataSetElementStore.getDataSetsForDataElement()`, implemented in
`persistence/dataset/DataSetDataElementLinkStoreImpl.kt`.

**How it works.** Add the method at the **end** of the Oslo store interface and
of its implementation, each with its `// EyeSeeTea customization - <Title>`
comment, and cover it with an integration test against a real database.

**Prefer first:** a query in the owned package through `DatabaseAdapter`, if
it does not duplicate Oslo's table knowledge.

**Upgrade cost.** Medium. Oslo has been migrating stores to Room; when the
store or its implementation is rewritten, the method has to be re-ported to
the new structure. The integration test is what tells you it still works.

---

## T4. Appended overload over an Oslo public API

**Use when:** a customization needs an extra argument on an Oslo public method
that apps call, and Oslo keeps changing that method's shape.

**Used by:** Two-Factor Authentication Login — the four-argument
`logIn` / `rxLogIn` / `blockingLogIn` / `suspendLogIn` with `twoFactorCode`,
appended at the end of `user/UserModule.kt`, and `suspendLogIn` at the end of
`user/internal/UserModuleImpl.kt`.

**How it works.** Leave Oslo's declarations untouched. Append a titled block
with real overloads that take the extra argument (Java callers cannot use
Kotlin default arguments), each delegating to one abstract `suspend` overload
implemented in an appended block of the Oslo implementation. The internal call
takes the extra argument as a trailing parameter with a default, so Oslo's own
path reaches the same flow without it:

```kotlin
// EyeSeeTea customization - Two-Factor Authentication Login
fun blockingLogIn(username: String, password: String, serverUrl: String, twoFactorCode: String?): User =
    runBlocking { suspendLogIn(username, password, serverUrl, twoFactorCode) }
```

**Prefer first:** a new owned module (T1) when the behaviour is not a variant
of an Oslo call.

**Upgrade cost.** Low. Oslo's tests and callers keep compiling with Oslo's
signatures, so they merge without conflicts; `core.api` only gains the
overloads. When Oslo renames or deprecates the base method, add the matching
overload and deprecate ours the same way.

---

## How to add a technique here

Add an entry when you solve an Oslo-code customization in a way another
customization could reuse. Include:

- **Use when** — the problem it solves, not the implementation
- **Used by** — which customization (spec title), so the reader can look at a
  real example
- **How it works** — the mechanism, with the minimum code to recognise it
- **Upgrade cost** — what breaks when Oslo changes, and how loudly
