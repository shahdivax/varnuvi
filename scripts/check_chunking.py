#!/usr/bin/env python3
"""Run pure Kotlin chunking and insertion checks with the project's already-cached compiler."""
import os
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
cache = Path(os.environ.get('GRADLE_USER_HOME', Path.home() / '.gradle')) / 'caches/modules-2/files-2.1'

def jar(group, artifact, version):
    hits = list((cache / group / artifact / version).glob('*/*.jar'))
    if not hits:
        raise SystemExit(f'Missing cached {artifact}:{version}. Build the Android app first.')
    return str(hits[0])

stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.2.20')
compiler = [jar('org.jetbrains.kotlin', artifact, version) for artifact, version in [
    ('kotlin-compiler-embeddable', '2.2.20'), ('kotlin-stdlib', '2.2.20'),
    ('kotlin-script-runtime', '2.2.20'), ('kotlin-reflect', '1.6.10'),
    ('kotlin-daemon-embeddable', '2.2.20')]]
compiler.append(jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.8.0'))
annotations = list((cache / 'org.jetbrains' / 'annotations').glob('*/*/*.jar'))
compiler.extend(map(str, annotations))
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if 'JAVA_HOME' in os.environ else 'java'
flags = ['-XX:ActiveProcessorCount=2', '-XX:TieredStopAtLevel=1']
source = root / 'android/app/src/main/kotlin/dev/voiceagent/app/speech'
with tempfile.TemporaryDirectory(prefix='veya-chunk-check-') as temp:
    output = str(Path(temp) / 'checks.jar')
    subprocess.run([java, *flags, '-cp', os.pathsep.join(compiler),
        'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler', '-no-stdlib', '-no-reflect',
        '-classpath', stdlib, '-d', output, str(source / 'ChunkTranscript.kt'),
        str(source / 'PcmChunks.kt'), str(source.parent / 'accessibility/BubbleRefreshPolicy.kt'), str(source.parent / 'insert/InsertionText.kt'), str(source.parent / 'writing/WritingPolicy.kt'),
        str(root / 'test/native/BubbleRefreshCheck.kt'), str(root / 'test/native/ChunkingCheck.kt'), str(root / 'test/native/InsertionCheck.kt'), str(root / 'test/native/WritingPolicyCheck.kt')], check=True)
    subprocess.run([java, *flags, '-cp', os.pathsep.join([output, stdlib]), 'ChunkingCheckKt', str(Path(temp) / 'audio')], check=True)

    subprocess.run([java, *flags, '-cp', os.pathsep.join([output, stdlib]), 'InsertionCheckKt'], check=True)

    subprocess.run([java, *flags, '-cp', os.pathsep.join([output, stdlib]), 'WritingPolicyCheckKt'], check=True)

    subprocess.run([java, *flags, '-cp', os.pathsep.join([output, stdlib]), 'BubbleRefreshCheckKt'], check=True)
