# Agent Guidelines

## Rules

1. **Conventional Commits** — every commit must use `feat:`, `fix:`, `docs:`, `chore:`, `refactor:`, or `style:` prefix.
2. **Update README on every feature change** — if you add, change, or remove a feature, update `README.md` to reflect it.
3. **Keep platforms in sync** — this is a multi-platform app (macOS + Android + Windows). When changing shared behavior (colors, animations, defaults, settings), update all platforms. Look for `SYNC:` comments in the code marking duplicated constants.
