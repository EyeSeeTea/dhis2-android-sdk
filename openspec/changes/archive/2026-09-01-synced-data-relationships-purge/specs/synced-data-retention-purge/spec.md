## MODIFIED Requirements

### Requirement: Purge eligibility requires a fully synced record tree
A record, or for hierarchical data its entire ancestor-descendant tree, SHALL be
eligible for purge only when its aggregated sync state is fully synced. A single
non-synced record anywhere in the tree SHALL protect every record in that tree
from purge, including the otherwise-eligible ancestors and descendants.

A tracked entity instance, enrollment, or event that participates in a
relationship SHALL additionally be eligible for purge only if the other side of
that relationship — its own record tree — is also fully synced. A relationship
with one side eligible and one side not SHALL protect both sides from purge,
including the otherwise-eligible side's own tree.

#### Scenario: Fully synced tree is eligible
- **WHEN** a tracked entity instance and all of its enrollments, events, and
  values are fully synced
- **THEN** the tracked entity instance is eligible for purge

#### Scenario: One pending descendant protects the whole tree
- **WHEN** a tracked entity instance is fully synced but one of its events has a
  pending change
- **THEN** neither the tracked entity instance, its enrollments, nor any of its
  events or values are purged

#### Scenario: Leaf data without children uses its own sync state
- **WHEN** an aggregate data value has no child records
- **THEN** its eligibility is determined directly by its own sync state, not an
  aggregated one

#### Scenario: A relationship to a non-eligible counterpart protects the eligible side
- **WHEN** a fully synced tracked entity instance has a relationship to another
  tracked entity instance whose own record tree is not fully synced
- **THEN** the fully synced tracked entity instance is not purged, even though
  its own tree is otherwise eligible

#### Scenario: A relationship between two fully synced trees does not block purge
- **WHEN** a tracked entity instance, enrollment, or event has a relationship to
  a counterpart whose own record tree is also fully synced
- **THEN** the relationship does not prevent either side from being purged
