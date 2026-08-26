# Requirement Designs and Implementation Plans

`docs/plans/` is the canonical location for future Pixel Meter requirement designs and implementation plans.

## Naming

Use the same topic for a requirement's design and plan:

```text
YYYY-MM-DD-topic-design.md
YYYY-MM-DD-topic-plan.md
```

Example:

```text
2026-08-25-overlay-snap-design.md
2026-08-25-overlay-snap-plan.md
```

Topics use short, stable English kebab-case names. Do not use a version number as the only topic.

## Design Contents

A design should cover:

- Context, user problem, and goals.
- Explicit scope and non-goals.
- Android versions, permissions, and system constraints.
- User flows and key states.
- Architecture, component boundaries, and data flow.
- DataStore, compatibility, and migration impact.
- Risks, alternatives, and validation.

## Implementation Plan Contents

An implementation plan should cover:

- Files to create, modify, or delete.
- Steps ordered by dependency.
- Expected result of each step.
- Build, Lint, and device-validation requirements.
- Documentation, localization, permissions, and release checks.

## Lifecycle

- Update designs and plans when requirements change; do not create conflicting copies.
- Completed documents may remain when they retain architectural value.
- Do not commit one-off command logs, temporary investigations, or drafts with no long-term value.
- Documentation does not replace code review, builds, Lint, or device validation.
