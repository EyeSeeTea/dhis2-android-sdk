## Why

The EyeSeeTea username/password login flow treats every successful HTTP response
from `/api/auth/login` as permission to continue, even when its payload reports
`loginStatus = ACCOUNT_DISABLED`. It consequently requests `/api/me`, receives a
second `401 Account disabled` response, and triggers the SDK's global account
deletion event instead of returning the disabled-account result directly to the
login caller; downstream apps can therefore navigate away from or recreate the
login screen without explaining the failure to the user.

## What Changes

- Treat `ACCOUNT_DISABLED` returned by `/api/auth/login` as a terminal login
  result and expose it through the existing `USER_ACCOUNT_DISABLED` SDK error.
- Stop the login flow before credentials are persisted or `/api/me` is requested
  when the authentication response already reports a disabled account.
- Do not delete local account data or emit a global account-deletion event for a
  disabled result returned during a login attempt. The existing policy for a
  disabled account detected during an established session remains unchanged.
- Add regression coverage for the server sequence observed in DHIS2 2.43:
  HTTP 200 with `{"loginStatus":"ACCOUNT_DISABLED"}` from `/api/auth/login`.
- Keep successful login and all supported TOTP, email, and SMS two-factor states
  unchanged.

## Capabilities

### New Capabilities

- `disabled-account-login`: defines how username/password authentication handles
  a disabled-account status returned by the login endpoint, including early
  termination, error reporting, follow-up request suppression, and preservation
  of local account data.

### Modified Capabilities

None. The current OpenSpec tree has no authentication or login capability to
extend.

## Impact

- **Affected domain module**: `user`, specifically the EyeSeeTea login flow and
  its disabled-account error handling.
- **Network behavior**: `/api/me` is no longer requested after
  `/api/auth/login` reports `ACCOUNT_DISABLED`.
- **Public behavior**: keep `ACCOUNT_DISABLED` as an internal wire-status value;
  consumers receive only the existing `D2ErrorCode.USER_ACCOUNT_DISABLED` login
  failure and do not need a second mapping for the same condition.
- **Persistence and events**: a disabled login attempt neither modifies an
  existing local account nor emits an account-deletion event. The upstream
  cleanup behavior for an established session is unchanged.
- **Unaffected modules**: event, enrollment, trackedentity, datavalue,
  fileresource, relationship, and the shared `ModuleWiper` interface are not
  touched.
- **Downstream apps**: user-facing copy and navigation after receiving
  `USER_ACCOUNT_DISABLED` remain app responsibilities and are outside this SDK
  change.
- **Compatibility**: bug fix with no public API or dependency changes.
