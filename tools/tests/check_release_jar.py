"""Run after clean build WITHOUT QA flags. Checks shipped resources, not runtime behavior."""
from pathlib import Path
import hashlib,json,zipfile
P=Path(__file__).resolve().parents[2]
version=next(s.split('=',1)[1] for s in (P/'gradle.properties').read_text().splitlines() if s.startswith('mod_version='))
jar=P/'build/libs'/f'arcane_cataclysm-{version}.jar';prefix='assets/arcane_cataclysm/'
with zipfile.ZipFile(jar) as z:
 names=z.namelist();assert not any('/qa/' in n for n in names),'QA classes in release'
 assert version in z.read('META-INF/neoforge.mods.toml').decode()
 assert 'com/kostya/cataclysm/client/SpellFilm.class' in names
 assert 'com/kostya/cataclysm/client/HighTierWorldRenderer.class' in names
 for id in range(9,15):
  name='saga' if id==9 else 'film_'+str(id)
  assert f'#define FILM_ID {id}' in z.read(prefix+f'shaders/core/{name}.fsh').decode()
  assert json.loads(z.read(prefix+f'shaders/core/{name}.json'))['samplers']==[]
 assert prefix+'shaders/include/ac_film.glsl' in names
 records=json.loads(z.read(prefix+'sounds/audio_v061_manifest.json'))
 for item in records:assert hashlib.sha256(z.read(prefix+'sounds/'+item['file'])).hexdigest()==item['sha256']
 for item in json.loads((P/'docs/UNCHANGED_0.6.1.json').read_text())['unchanged']:
  if '/resources/' in item['file']:
   path=item['file'].split('/resources/',1)[1];assert hashlib.sha256(z.read(path)).hexdigest()==item['sha256']
print('RELEASE_JAR_PASS',jar.name,'bytes='+str(jar.stat().st_size),'no_QA=true films=6 scores=6 End_resources=unchanged')
