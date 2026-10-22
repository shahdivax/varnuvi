#!/usr/bin/env python3
"""Ensure the shipping Android app has no accessibility service or screen access."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
manifest = ET.parse(root / 'android/app/src/main/AndroidManifest.xml').getroot()
android = '{http://schemas.android.com/apk/res/android}'
permissions = {item.get(android + 'name') for item in manifest.findall('uses-permission')}
assert 'android.permission.SYSTEM_ALERT_WINDOW' in permissions
assert not permissions.intersection({
    'android.permission.BIND_ACCESSIBILITY_SERVICE',
    'android.permission.PACKAGE_USAGE_STATS',
    'android.permission.QUERY_ALL_PACKAGES',
    'android.permission.FOREGROUND_SERVICE_MICROPHONE',
})
services = manifest.findall('application/service')
assert len(services) == 1
assert services[0].get(android + 'name') == '.overlay.BubbleService'
assert services[0].get(android + 'exported') == 'false'
assert services[0].get(android + 'foregroundServiceType') == 'specialUse'
for path in (root / 'android/app/src/main').rglob('*.kt'):
    assert 'AccessibilityService' not in path.read_text(), f'Accessibility service code remains: {path}'
print('PASS: overlay-only service; no accessibility, app tracking, or background microphone permissions')
