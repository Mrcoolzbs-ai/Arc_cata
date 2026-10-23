"""Static orientation/configuration contract, complements actual Minecraft screenshots."""
from pathlib import Path
import json,re
p=Path(__file__).resolve().parents[2]
s=(p/'src/main/resources/assets/arcane_cataclysm/shaders/core/invocation.fsh').read_text()
calls=[line.strip() for line in s.splitlines() if 'seal(' in line and not line.startswith('void seal')]
assert len(calls)>=10, len(calls)
assert all('vec3(0,1,0)' in line for line in calls), calls
assert 'float den=rd.y;' in s[s.index('void aureole'):s.index('void lance')]
assert '(ro+rd*t-center).xz/radius' in s
world=(p/'src/main/java/com/kostya/cataclysm/client/FirstFiveRenderer.java').read_text()
assert 'verticalSeal' not in world and 'orbitRing' not in world
assert 'return new ClientEffects.View(pos,yaw,pitch,0);' in (p/'src/main/java/com/kostya/cataclysm/client/InvocationDirector.java').read_text()
j=json.loads((p/'src/main/resources/assets/arcane_cataclysm/shaders/core/invocation.json').read_text())
assert any(u['name']=='FxImpact' and u['count']==4 for u in j['uniforms'])
fx=(p/'src/main/java/com/kostya/cataclysm/client/ScreenFX.java').read_text()
assert 'ClientOptions.GLITCH.get()&&ClientOptions.FLASH.get()' in fx
assert 'ClientOptions.SHAKE.get()?' in fx
print(f'VISUAL_CONTRACT_PASS horizontal_seal_call_sites={len(calls)} halo=XZ camera_roll=0 toggles=verified')
