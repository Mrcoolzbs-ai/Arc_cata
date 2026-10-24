"""Static integration contracts. Complements Java math tests and real-client QA; not a GPU benchmark."""
from pathlib import Path
import json,re,subprocess,hashlib
P=Path(__file__).resolve().parents[2];J=P/'src/main/java/com/kostya/cataclysm';R=P/'src/main/resources/assets/arcane_cataclysm'
director=(J/'client/InvocationDirector.java').read_text()
assert 'spell.ordinal()<15' in director and 'spell.ordinal()>=9' in director
assert 'f.spell.impact+50' in director
manager=(J/'world/SpellManager.java').read_text()
assert 'spell.ordinal()>=15' in manager and 'for(int i=0;i<15;i++)' in manager
preview=manager[manager.index('private static int preview'):manager.index('private static int endOfAllSetting')]
assert 'PacketDistributor.sendToPlayer(p,' in preview
assert not any(token in preview for token in ['ACTIVE.add','EndEvent.start','DestructionEngine.prepare','impact(a','addCooldown'])
first=(J/'client/FirstFiveRenderer.java').read_text()
assert 'origin.lerp' not in first and 'threads are drawn' not in first
for path in [J/'client/FirstFiveRenderer.java',J/'client/SecondFourRenderer.java',J/'client/SpellRenderer.java',J/'client/RitualRenderer.java']:
 for line in path.read_text().splitlines():
  if 'FxMesh.ribbon(' in line or 'FxMesh.beam(' in line:
   assert not ('packet.origin' in line or 'origin.lerp' in line), (path,line)
for lang in ['ru_ru','en_us']:
 data=json.loads((R/'lang'/f'{lang}.json').read_text())
 for name in ['last_cataclysm','verdant_ruin','obsidian_cathedral','abyssal_deluge','world_sunder','rune_apocalypse']:
  for chapter in range(5):assert data.get(f'hud.arcane_cataclysm.saga.{name}.{chapter}')
for name in ['invocation','saga']:
 data=json.loads((R/'shaders/core'/f'{name}.json').read_text())
 assert data['fragment']==f'arcane_cataclysm:{name}'
 assert all(any(u['name']==x for u in data['uniforms']) for x in ['FxParams','FxImpact','FxTime','FxScreen'])
score=(P/'tools/generate_v6_audio.py').read_text();assert 'end_of_all' not in score[score.index('IDS='):score.index('SECONDS=')]
records=json.loads((R/'sounds/audio_v6_manifest.json').read_text());assert len(records)==30
for item in records:
 path=R/'sounds'/item['file'];assert hashlib.sha256(path.read_bytes()).hexdigest()==item['sha256']
 meta=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(path)]))
 assert meta['streams'][0]['channels']==2
 assert abs(float(meta['format']['duration'])-item['seconds'])<.02
sounds=json.loads((R/'sounds.json').read_text())
for item in records:
 name=item['file'][:-4];assert sounds[name]['sounds'][0]['name']=='arcane_cataclysm:'+name
print('CINEMATIC_CONTRACT_PASS scenes=15 epic=6 chapters=30 stereo_cues=30 preview=private no_caster_tethers=true')
