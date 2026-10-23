"""Original deterministic sound design and RU/EN catalogue for Arcane Cataclysm 0.3."""
from pathlib import Path
import numpy as np,json,subprocess,wave,tempfile
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/arcane_cataclysm'
ids=['heavens_lance','thousand_blades','snow_grave','tempest_wrath','starfall','tectonic_rift','inferno_tide','singularity','solar_nova','last_cataclysm','verdant_ruin','obsidian_cathedral','abyssal_deluge','world_sunder','rune_apocalypse']
ru=['Копьё небес','Тысяча клинков','Снежная могила','Гнев бури','Падение звезды','Разлом титанов','Адский прилив','Сингулярность','Солнечная нова','Последний катаклизм','Погребение Иггдрасиля','Собор погибели','Бездна прилива','Раскол мира','Приговор мироздания']
en=['Heaven’s Lance','Thousand Blades','Snow Grave','Tempest’s Wrath','Starfall','Tectonic Rift','Inferno Tide','Singularity','Solar Nova','The Last Cataclysm','Yggdrasil’s Burial','Cathedral of Doom','Abyssal Deluge','World Sunder','Judgment of Creation']
for lang,names in [('ru_ru',ru),('en_us',en)]:
 p=root/f'lang/{lang}.json';d=json.loads(p.read_text());d.update({f'spell.arcane_cataclysm.{id}':name for id,name in zip(ids,names)});d['spell.arcane_cataclysm.glacial_ragnarok']=names[2]
 for id,name in zip(ids,names):
  for phase in ['charge','impact']:d[f'subtitles.arcane_cataclysm.{id}_{phase}']=name+(' — призыв' if lang=='ru_ru' and phase=='charge' else ' — удар' if lang=='ru_ru' else ' — '+phase)
 p.write_text(json.dumps(d,ensure_ascii=False,indent=2))
sounds=json.loads((root/'sounds.json').read_text());sr=24000
for i,id in enumerate(ids):
 rng=np.random.default_rng(713+i)
 for phase in ['charge','impact']:
  secs=6.4 if phase=='charge' else 5.8;t=np.arange(int(sr*secs))/sr;n=rng.standard_normal(len(t));low=np.convolve(n,np.ones(130)/130,'same');wind=np.convolve(n,np.ones(17)/17,'same');f=55+i*3
  impact=phase=='impact';env=np.exp(-t*(.8 if impact else .045));attack=(1-np.exp(-t*(90 if impact else 2)));gate=(.7+.3*np.sin(t*(1.3+i*.11))**2)
  if i==0: # overtone bells and solemn choir
   x=sum(np.sin(2*np.pi*freq*t)*np.exp(-t*(.35 if impact else .08))/(j+1) for j,freq in enumerate([220,330,554,880]))*.25+low*.4
  elif i==1: # metallic swishes and inharmonic clashes
   x=sum(np.sin(2*np.pi*(470+j*187)*t)*np.exp(-((t-(j*.23 if impact else j*.8))/.17)**2)*.25 for j in range(6))+wind*.22
  elif i==2: # glass harmonics, chimes, frigid wind
   x=sum(np.sin(2*np.pi*(620+j*417)*t+np.sin(t*2))*np.exp(-t*(.8+j*.07))/(j+2) for j in range(5))*.35+wind*.4
  elif i==3: # thunder clusters
   x=low*(3 if impact else 1.4)+n*.15*sum(np.exp(-((t-k)/.045)**2) for k in [0,.7,1.2,2.1])+np.sin(2*np.pi*(42*t-3*t*t))*.15
  elif i==4:x=low*2+np.sin(2*np.pi*(70*t-12*np.log1p(t)))*.32+wind*.2
  elif i==5:x=low*3+np.sin(2*np.pi*37*t)*.3+np.sin(2*np.pi*63*t)*.15+wind*.2*np.sin(t*19)**8
  elif i==6:x=wind*.7+low*1.7+n*.06+np.sin(2*np.pi*52*t)*.15
  elif i==7:x=np.sin(2*np.pi*(48*t+9*np.sin(t*.7)))*.35+np.sin(2*np.pi*51*t)*.28+low*1.5
  elif i==8:x=np.sin(2*np.pi*(90*t+15*t*t))*.18+low*2+wind*.38
  elif i==9:x=sum(np.sin(2*np.pi*q*t)*.13 for q in [44,65.4,130.8,196])+low*2
  elif i==10:x=low*2+wind*.25*np.sin(t*7)**2+np.sin(2*np.pi*(74*t+6*np.sin(t*3)))*.17
  elif i==11:x=sum(np.sin(2*np.pi*q*t)*np.exp(-t*.17)*.11 for q in [65.4,130.8,155.6,261.6,523.2])+low*1.6
  elif i==12:x=wind*.7+low*1.9+np.sin(2*np.pi*(90*t+12*np.sin(t*1.4)))*.19
  elif i==13:x=low*3+np.sin(2*np.pi*29*t)*.28+wind*.3*np.sin(t*23)**12
  else:x=sum(np.sin(2*np.pi*q*t+np.sin(t*.4))*.1 for q in [55,82.4,110,164.8,440])+low*2+n*.03
  x*=env*attack*gate*np.minimum(1,(secs-t)/.3);x=x/(max(abs(x))+.01)*.82
  name=id+'_'+phase;tmp=Path(tempfile.gettempdir())/(name+'.wav')
  with wave.open(str(tmp),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr);w.writeframes((x*32767).astype('<i2').tobytes())
  subprocess.run(['ffmpeg','-y','-loglevel','error','-i',str(tmp),'-c:a','libvorbis','-q:a','4',str(root/'sounds'/f'{name}.ogg')],check=True);tmp.unlink()
  sounds[name]={'subtitle':'subtitles.arcane_cataclysm.'+name,'sounds':[{'name':'arcane_cataclysm:'+name,'stream':False}]}
(root/'sounds.json').write_text(json.dumps(sounds,indent=2))
r=root.parents[1]/'data/minecraft/tags/damage_type'
r.mkdir(parents=True,exist_ok=True)
for name in ['bypasses_armor','bypasses_shield','bypasses_resistance','bypasses_enchantments','bypasses_cooldown']:
 (r/(name+'.json')).write_text(json.dumps({'replace':False,'values':['arcane_cataclysm:cataclysm']},indent=2))
print('Generated 15 localized spells and 30 distinct OGG charge/impact tracks')
