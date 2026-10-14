#!/usr/bin/env bash
# Fails if staged changes contain secret-looking strings or local config files.
set -euo pipefail
staged=$(git diff --cached --name-only)
if echo "$staged" | grep -E '(^|/)(local\.config\.json|local\.env|local\.properties|.*\.jks|.*\.keystore|key\.properties)$'; then
  echo "Refusing: local config or key file staged"; exit 1
fi
if git diff --cached -U0 | grep -E '^\+' | grep -iE '(api[_-]?key|secret|token|password)["'\'' ]*[:=][ ]*["'\'']?[A-Za-z0-9_\-]{12,}|sk-[A-Za-z0-9]{16,}|AIza[0-9A-Za-z_\-]{20,}|-----BEGIN [A-Z ]*PRIVATE KEY'; then
  echo "Refusing: secret-looking string staged"; exit 1
fi
echo "secret check: clean"
