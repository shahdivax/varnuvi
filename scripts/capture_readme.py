#!/usr/bin/env python3
"""Capture the real app with synthetic data on an isolated emulator only."""
import argparse
import json
from pathlib import Path
import re
import shlex
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument('--serial', default='emulator-5556')
parser.add_argument('--adb', default='adb')
args = parser.parse_args()
if not re.fullmatch(r'emulator-\d+', args.serial):
    raise SystemExit('Refusing a physical device: this fixture clears emulator app data.')
root = Path(__file__).resolve().parents[1]
out = root / 'docs/images'
out.mkdir(parents=True, exist_ok=True)
pkg = 'dev.voiceagent.app'

def adb(*command, data=None):
    return subprocess.check_output([args.adb, '-s', args.serial, *command], input=data, stderr=subprocess.STDOUT)

def shell(command):
    return adb('shell', command)

def prefs(name, values):
    tree = ET.Element('map')
    for key, value in values.items():
        if isinstance(value, int):
            ET.SubElement(tree, 'int', name=key, value=str(value))
        else:
            ET.SubElement(tree, 'string', name=key).text = value
    command = f'mkdir -p shared_prefs && cat > shared_prefs/{name}.xml'
    adb('shell', f'run-as {pkg} sh -c {shlex.quote(command)}', data=ET.tostring(tree, encoding='utf-8'))

def nodes():
    shell('uiautomator dump /data/local/tmp/varnuvi-capture.xml >/dev/null')
    return ET.fromstring(shell('cat /data/local/tmp/varnuvi-capture.xml')).iter('node')

def tap(label, kind=None):
    for attempt in range(4):
        for node in nodes():
            text = node.get('content-desc', '') + '\n' + node.get('text', '') + '\n' + node.get('hint', '')
            if label in text and (kind is None or node.get('class') == kind):
                x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
                shell(f'input tap {(x1+x2)//2} {(y1+y2)//2}')
                time.sleep(.6)
                return
        time.sleep(.4)
    raise RuntimeError('Could not locate fixture control: ' + label)

def capture(name):
    time.sleep(.5)
    (out / name).write_bytes(adb('exec-out', 'screencap', '-p'))
    print('Captured ' + name, flush=True)

now = int(time.time() * 1000)
items = [
    {'text': 'Make space for a slower morning. A short walk, a warm cup of tea, and a little time to think.', 'createdAt': now-3600000},
    {'text': 'Product notes\nKeep the first screen calm. Make the next step clear. Let the words do the work.', 'createdAt': now-7200000},
    {'text': 'Tomorrow\nReview the draft, share the notes, and leave the afternoon open.', 'createdAt': now-86400000},
]

# This clears only the explicitly selected throwaway emulator's app state.
shell(f'pm clear {pkg}')
shell('settings put global sysui_demo_allowed 1')
for command in ('enter', 'clock --es hhmm 0941', 'battery --es level 100 --es plugged false',
                'network --es wifi show --es level 4 --es mobile hide --es fully true', 'notifications --es visible false'):
    name, _, extra = command.partition(' ')
    shell(f'am broadcast -a com.android.systemui.demo --es command {name} {extra}')


def launch(theme):
    shell(f'am force-stop {pkg}')
    prefs('veya_settings', {'theme': theme})
    prefs('veya_dictations', {'history': json.dumps(items)})
    shell(f'am start -n {pkg}/.MainActivity')
    time.sleep(1.4)

prefs('bubble', {'compact_x': 880, 'compact_y': 1880})
launch('light')
tap('Your words appear here')
sample = 'Write the first draft while the idea is still fresh. Keep the parts that sound like you.'
shell('input text ' + shlex.quote(sample.replace(' ', '%s')))
shell('input keyevent 4')
capture('speak-light.png')
tap('History\nTab')
capture('history-light.png')

launch('dark')
tap('History\nTab')
capture('history-dark.png')
tap('Settings\nTab')
tap('Speech server')
capture('server-dark.png')
shell('input keyevent 4')
tap('Bubble\nTab')
# Use real Android permission/service state rather than drawing a fake ready UI.
shell(f'pm grant {pkg} android.permission.RECORD_AUDIO')
shell(f'pm grant {pkg} android.permission.POST_NOTIFICATIONS')
shell(f'appops set {pkg} SYSTEM_ALERT_WINDOW allow')
tap('Refresh bubble status')
tap('Show bubble')
time.sleep(1)
# Move the real bubble below the setup card so it doesn't cover the text.
for node in nodes():
    if node.get('content-desc') == 'Start dictation':
        x1,y1,x2,y2 = map(int, re.findall(r'\d+',node.get('bounds')))
        shell(f'input swipe {(x1+x2)//2} {(y1+y2)//2} 950 1650 600')
        break
capture('bubble-dark.png')
shell(f'am force-stop {pkg}')
print('Done. Synthetic UI captures only; no microphone or API calls were made.', flush=True)
