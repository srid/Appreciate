# Appreciate — Agent Guidelines

## Commit Convention

This project uses **Conventional Commits** for automated changelog generation via release-please.

Every commit message **must** follow this format:

```
<type>: <description>

[optional body]
```

### Types

| Type | When to use |
|---|---|
| `feat` | New feature or user-visible behavior change |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `chore` | Build scripts, CI, maintenance (no user-facing change) |
| `refactor` | Code restructuring without behavior change |
| `style` | Formatting, whitespace, cosmetic |

### Examples

```
feat: add custom font selection in settings
fix: timer not firing due to wrong run loop mode
docs: add install instructions for unsigned apps
chore: update GitHub Actions to macos-15
refactor: extract color palette to separate file
```

### Rules

- Use **lowercase** type prefix
- Use **imperative mood** in description ("add X", not "added X")
- Keep the first line under 72 characters
- A `feat` or `fix` will appear in the auto-generated changelog; `chore`/`docs`/`refactor` will not
- Breaking changes: add `!` after type, e.g. `feat!: change config format`
