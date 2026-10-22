#!/usr/bin/env bash
# Lightweight staged-content check. Prints file names only, never matching values.
set -euo pipefail
python3 - <<'PY'
import re
import subprocess
from pathlib import PurePosixPath

paths = subprocess.check_output([
    'git', 'diff', '--cached', '--name-only', '--diff-filter=ACMR', '-z'
]).decode().split('\0')
patterns = [
    r'sk-or-v1-[0-9a-fA-F]{32,}',
    r'sk-[A-Za-z0-9]{16,}',
    r'AIza[0-9A-Za-z_-]{20,}',
    r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',
    r'''(?i)(?:api[_-]?key|secret|token|password)["' ]*[:=]\s*["'][A-Za-z0-9_/-]{24,}["']''',
]
failed = False
for path in filter(None, paths):
    name = PurePosixPath(path).name
    if (name in {'local.config.json', 'local.env', 'local.properties', 'key.properties', '.env'}
            or name.endswith(('.jks', '.keystore', '.hprof'))
            or (name.startswith('.env.') and not name.endswith('.example'))):
        print(f'Refusing staged private/local file: {path}')
        failed = True
        continue
    data = subprocess.check_output(['git', 'show', f':{path}']).decode('utf-8', errors='replace')
    if any(re.search(pattern, data) for pattern in patterns):
        print(f'Refusing possible credential in staged file: {path}')
        failed = True
if failed:
    raise SystemExit(1)
print('Staged credential check: clean (not a full history scan).')
PY
