"""Package source files only; never include run data, caches, reference jars or the local JDK."""
from pathlib import Path
import zipfile
p=Path(__file__).resolve().parents[1]
out=p.parent/'releases';out.mkdir(exist_ok=True)
version=next(line.split('=',1)[1].strip() for line in (p/'gradle.properties').read_text().splitlines() if line.startswith('mod_version='))
include=['README.md','TESTING.md','TESTING_0.6.0-cinematic-preview.md','QA_RESULTS_0.6.0-cinematic-preview.txt','TESTING_0.5.0-style-preview.md','QA_RESULTS_0.5.0-style-preview.txt','TESTING_0.3.0.md','TESTING_0.4.0-preview.md','TESTING_0.4.1.md','QA_RESULTS_0.4.0-preview.txt','QA_RESULTS_0.4.1.txt','QA_RESULTS.txt','TESTING_0.5.1-style-preview.md','QA_RESULTS_0.5.1-style-preview.txt','LICENSE','TEMPLATE_LICENSE.txt','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat','.gitignore']
with zipfile.ZipFile(out/f'arcane_cataclysm-{version}-sources.zip','w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    files=[p/f for f in include]
    for directory in ['src','gradle','tools','previews','docs']:
        files.extend(f for f in (p/directory).rglob('*') if f.is_file() and '__pycache__' not in f.parts and f.suffix not in ('.pyc',))
    for f in sorted(set(files)):
        if f.exists():z.write(f,Path(f'arcane_cataclysm-{version}')/f.relative_to(p))
print(out/f'arcane_cataclysm-{version}-sources.zip')
