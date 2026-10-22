#!/usr/bin/env python3
"""Render a README hero from unmodified app screenshots and the brand SVG."""
import argparse
from pathlib import Path
import subprocess
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('--chrome', default='google-chrome')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
images = root / 'docs/images'
for name in ('speak-light.png', 'history-dark.png'):
    if not (images/name).is_file():
        raise SystemExit('Run capture_readme.py first: missing '+name)
build = root / 'build/readme'
build.mkdir(parents=True, exist_ok=True)
html = '''<!doctype html><html lang="en"><meta charset="utf-8"><title>Varnuvi</title>
<style>
*{box-sizing:border-box}html,body{margin:0;width:1920px;height:1120px;overflow:hidden}
body{background:#fff8e7;color:#19213a;font-family:"DejaVu Sans",sans-serif}
.brand{position:absolute;top:86px;left:112px;display:flex;align-items:center;gap:18px;font-size:54px;font-weight:700}
.brand img{width:64px;height:64px}
.copy{position:absolute;left:112px;top:330px;width:820px}
h1{font-size:96px;font-weight:600;line-height:1.1;white-space:nowrap;margin:0 0 44px}
p{font-size:33px;line-height:1.48;color:#616b83;margin:0;max-width:690px}
.tags{display:flex;gap:12px;margin-top:44px;font-size:23px;color:#3156d3}
.tags span{padding:12px 20px;background:#ede7d9;border-radius:99px}
.footer{position:absolute;left:112px;bottom:90px;width:700px;border-top:1px solid #d7ccb7;padding-top:25px;font-size:24px;color:#616b83}
.phone{position:absolute;background:#19213a;padding:14px;border-radius:40px;overflow:hidden;border:1px solid #19213a}
.phone img{width:100%;height:auto;display:block;border-radius:8px}
.light{left:1000px;top:119px;width:390px}
.dark{left:1434px;top:245px;width:340px}
.caption{position:absolute;font-size:20px;color:#616b83;text-align:center}
.c1{left:1000px;top:1020px;width:390px}.c2{left:1434px;top:1020px;width:340px}
</style>
<div class="brand"><img src="BRAND" alt="">varnuvi</div>
<div class="copy"><h1>A little space<br>for your voice.</h1>
<p>A floating mic for the way you think.<br>Dictate, review, copy, and carry on.</p>
<div class="tags"><span>Android 13+</span><span>Self-hosted</span><span>Open source</span></div></div>
<div class="phone light"><img src="SPEAK" alt="Varnuvi Speak screen"></div>
<div class="phone dark"><img src="HISTORY" alt="Varnuvi History screen in dark mode"></div>
<div class="caption c1">Speak freely.</div><div class="caption c2">Keep your thoughts close.</div>
<div class="footer">Your server. Your words.</div>
</html>'''
html = html.replace('BRAND', (root/'assets/brand/varnuvi-mark.svg').as_uri())
html = html.replace('SPEAK', (images/'speak-light.png').as_uri()).replace('HISTORY', (images/'history-dark.png').as_uri())
page = build/'hero.html'
page.write_text(html)
with tempfile.TemporaryDirectory(prefix='varnuvi-readme-chrome-') as profile:
    result = subprocess.run([args.chrome, '--headless=new', '--no-sandbox', '--disable-gpu',
        '--disable-background-networking', '--disable-extensions', '--no-first-run',
        '--hide-scrollbars', '--allow-file-access-from-files', '--force-device-scale-factor=1',
        '--window-size=1920,1120', '--virtual-time-budget=1500', '--user-data-dir='+profile,
        '--screenshot='+str(images/'hero.png'), page.as_uri()], capture_output=True, text=True, timeout=45)
    if result.returncode:
        raise SystemExit('Chrome could not render the local README artwork.')
print('Rendered docs/images/hero.png from real app captures.')
