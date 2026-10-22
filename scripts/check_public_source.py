#!/usr/bin/env python3
"""Publication guard for the current non-ignored tree; not a history/secret audit."""
import ipaddress
from pathlib import Path
import re
import subprocess

files = set(subprocess.check_output(['git', 'ls-files', '--cached', '--others', '--exclude-standard', '-z']).decode().split('\0'))
allowed = {'0.0.0.0', '127.0.0.1', '10.0.2.2'}
failed = []
for name in sorted(filter(None, files)):
    path = Path(name)
    if not path.is_file():
        continue
    try:
        text = path.read_text()
    except UnicodeError:
        continue
    reasons = []
    if re.search(r'/(?:home|media)/pc/|wisper-flow-' + 'clone-ui', text):
        reasons.append('machine-private path')
    for address in re.findall(r'(?<![\w.])(?:\d{1,3}\.){3}\d{1,3}(?![\w.])', text):
        try:
            ip = ipaddress.ip_address(address)
        except ValueError:
            continue
        if address not in allowed and not any(ip in ipaddress.ip_network(net) for net in ('192.0.2.0/24', '198.51.100.0/24', '203.0.113.0/24')):
            reasons.append('operator-specific IP address')
            break
    if re.search(r'sk-or-v1-[a-zA-Z0-9]{24,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----', text):
        reasons.append('possible credential')
    if reasons:
        failed.append(name + ': ' + ', '.join(reasons))
if failed:
    print('\n'.join(failed))
    raise SystemExit(1)
print('PASS: current source has no operator IPs, machine paths, or recognized private credentials (history/assets require separate review)')
