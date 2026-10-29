"""0.6.2/0.6.3 routing/resource regressions; actual GPU/client captures are a separate test."""
from pathlib import Path
import json,hashlib,subprocess
P=Path(__file__).resolve().parents[2];J=P/'src/main/java/com/kostya/cataclysm';R=P/'src/main/resources/assets/arcane_cataclysm'
film=(J/'client/SpellFilm.java').read_text();client=(J/'client/ClientEffects.java').read_text();world=(J/'client/SpellRenderer.java').read_text()
assert 's.ordinal()>=9&&s.ordinal()<15' in film
assert 'f.age<f.spell.impact+100' in film and 'SoundSource.RECORDS' in film
assert 'active=!EndCinematic.running()' in film
assert 'Math.min(main.width,960)' in film and 'rh>540' in film
assert 'ScreenEvent.Render.Post' in film and 'target.destroyBuffers()' in film
assert 'if(SpellFilm.running())return null;' in client and '||SpellFilm.running())return;' in client
assert 'SpellFilm.stop(f)' in client and 'SpellFilm.audio(f)' in client
# 0.6.3: toggling the cinematic camera (C) must not silence a running film score.
keep=film[film.index('boolean keep='):film.index('if(!active&&!keep)stop(f);')]
assert 'CAMERA' not in keep and 'eligible' not in keep and 'f.skipped' in keep and 'EndCinematic.running()' in keep
assert 'if(!active)stop(f);' not in film and 'f.age<=20' in film
assert '&&f.scoreSound==null){sound(' in client
assert 'SpellFilm.scorePlaying()' in (J/'mixin/MusicMixin.java').read_text()
print('SCORE_CAMERA_TOGGLE_CONTRACT_PASS')
assert 'HighTierWorldRenderer.draw(f,partial);return;' in world
assert 'modelView.set(e.getModelViewMatrix())' in world and 'RenderSystem.restoreProjectionMatrix()' in world
assert world.index('HighTierWorldRenderer.draw')<world.index('RitualRenderer.draw')
world6=(J/'client/HighTierWorldRenderer.java').read_text();assert 'blade(' not in world6 and world6.count('FxMesh.disk(')==2
assert 'if(after>=80)return' in world6 and 'new Vec3(p.x,0,p.z)' in world6
second=(J/'client/SecondFourRenderer.java').read_text();nova=second[second.index('case SOLAR_NOVA'):second.index('default->')]
assert 'ribbon(' not in nova
assert 'FxMesh.surface' in nova and 'hold,0,.8f,6,0,true' in nova
invocation=(R/'shaders/core/invocation.fsh').read_text();nova=invocation[invocation.index('void nova()'):invocation.index('vec3 theatre')];assert 'beam(' not in nova
saga=(R/'shaders/include/ac_film.glsl').read_text();assert 'texture(Sampler0' not in saga and 'int shot=' in saga
assert json.loads((R/'shaders/core/saga.json').read_text())['samplers']==[{'name':'Landscape'}]
for id in range(9,15):
 name='saga' if id==9 else 'film_'+str(id)
 assert f'#define FILM_ID {id}' in (R/'shaders/core'/f'{name}.fsh').read_text()
 assert json.loads((R/'shaders/core'/f'{name}.json').read_text())['samplers']==[{'name':'Landscape'}]
plan=(J/'world/DestructionPlan.java').read_text();assert 'spell==Spell.ABYSSAL_DELUGE&&y<=0' in plan
grave=plan[plan.index('case GLACIAL_RAGNAROK->{'):plan.index('case TEMPEST_WRATH->{')];assert 'spike=' in grave  # 0.8.1: the frost grave alone keeps its ice monoliths
life=plan[plan.index('case VERDANT_RUIN->{'):plan.index('case OBSIDIAN_CATHEDRAL->{')];assert 'spike=' not in life and 'hi=terrain;' in life  # 0.8.1: no columns - the withered crater stays clear
assert 'private BlockState built' not in plan  # 0.8.1: nothing but the frost grave builds anything after a cast
engine=(J/'world/DestructionEngine.java').read_text();assert 'h.plan.spell==Spell.VERDANT_RUIN))return;' in engine
assert 'cap.cursor<256&&System.nanoTime()<deadline' in engine
assert 'unpin(h,h.capture.chunk.getPos());h.capture=null' in engine
records=json.loads((R/'sounds/audio_v062_manifest.json').read_text());assert len(records)==6
for item in records:
 path=R/'sounds'/item['file'];assert hashlib.sha256(path.read_bytes()).hexdigest()==item['sha256']
 meta=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(path)]))
 assert meta['streams'][0]['channels']==2 and abs(float(meta['format']['duration'])-item['seconds'])<.02
 print('FILM_SCORE_PASS',item['file'],item['seconds'])
print('FILM_CONTRACT_PASS opaque_layer=6 no_HUD_or_real_camera bounded_960x540 continuous_scores=6 transient_world=80ticks y0=explicit incremental_life_snapshot=true')
