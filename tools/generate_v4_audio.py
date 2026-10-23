"""Non-looping, full charge-length cues. User-supplied choir is the musical texture, not a short repeated loop.
Run: python3 tools/generate_v4_audio.py /path/to/choir_whispers_45s.wav
"""
from pathlib import Path
import numpy as np,subprocess,json,sys
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/arcane_cataclysm'
source=Path(sys.argv[1]);sr=24000
raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(source),'-f','f32le','-ac','1','-ar',str(sr),'-'])
choir=np.frombuffer(raw,dtype='<f4').astype(np.float64)
ids=['heavens_lance','thousand_blades','snow_grave','tempest_wrath','starfall','tectonic_rift','inferno_tide','singularity','solar_nova','last_cataclysm','verdant_ruin','obsidian_cathedral','abyssal_deluge','world_sunder','rune_apocalypse']
lengths=[18,20,21,22,21,23,24,25,26,29,31,33,35,37,40]
def avg(x,n):
 c=np.r_[0.,np.cumsum(x)];v=(c[n:]-c[:-n])/n;return np.pad(v,(n//2,n-1-n//2))[:len(x)]
def save(name,x):
 x=np.nan_to_num(x);x=np.tanh(x*.9);x/=max(1.,abs(x).max()/ .92)
 proc=subprocess.run(['ffmpeg','-y','-v','error','-f','f32le','-ar',str(sr),'-ac','1','-i','-','-c:a','libvorbis','-q:a','5',str(root/'sounds'/f'{name}.ogg')],input=x.astype('<f4').tobytes(),check=True)
sounds=json.loads((root/'sounds.json').read_text())
for i,(id,secs) in enumerate(zip(ids,lengths)):
 rng=np.random.default_rng(4100+i)
 for phase in ['charge','impact']:
  dur=secs if phase=='charge' else 7.;t=np.arange(int(sr*dur))/sr;u=t/dur;noise=rng.normal(0,1,len(t));low=avg(noise,320);mid=avg(noise,17);impact=phase=='impact'
  musical=np.interp(np.linspace(i*.09,min(44.9,i*.09+dur),len(t))*sr,np.arange(len(choir)),choir)
  x=musical*(.14 if i in [1,3,5,6,13] else .68)
  # Each invocation has an arc: emergence, changing material gestures, then an approach to impact.
  if not impact:x*=.25+.75*u**1.6
  count=9+i//3 if not impact else 5
  times=np.sort(rng.uniform(.2,max(.3,dur-.8),count)) if not impact else np.r_[0.,rng.uniform(.1,2.4,count-1)]
  for k,start in enumerate(times):
   a=t-start;active=a>=0;attack=(1-np.exp(-np.maximum(0,a)*100));decay=np.exp(-np.maximum(0,a)/(rng.uniform(.13,.7) if i in [1,2] else rng.uniform(.7,2.8)))
   env=active*attack*decay
   if i==1:gesture=mid*(.5+k/count)+np.sin(2*np.pi*(630+137*k)*a)*.06 # distinct blade flybys
   elif i==2:gesture=mid*.2+sum(np.sin(2*np.pi*f*a)*.035 for f in [947+k*39,1731+k*53,2513+k*71])
   elif i in [3,5,13]:gesture=low*(5+k*.18)+mid*.15 # irregular thunder/stone bursts, no periodic gate
   elif i==6:gesture=mid*.6+low*2.+noise*.07
   elif i==10:gesture=mid*.3+low*2.+np.sin(2*np.pi*(86*a+12*a*a))*.035
   elif i==12:gesture=avg(noise,55)*1.4+low*2
   else:gesture=low*1.8+sum(np.sin(2*np.pi*f*a)*.025 for f in [65.4*(1+k%3),196.,329.6])
   x+=gesture*env*(.28+.32*(k/count))
  rise=np.maximum(0,u-.65)/.35
  if not impact:x+=low*rise**2*2.2+mid*rise**4*.12
  fadein=np.minimum(1,t/(.035 if impact else .8));fadeout=np.minimum(1,(dur-t)/(.7 if impact else .09));x*=fadein*fadeout
  save(id+'_'+phase,x);sounds[id+'_'+phase]={'subtitle':'subtitles.arcane_cataclysm.'+id+'_'+phase,'sounds':[{'name':'arcane_cataclysm:'+id+'_'+phase,'stream':True}]}
  print(id,phase,dur,flush=True)
subprocess.run(['ffmpeg','-y','-v','error','-i',str(source),'-c:a','libvorbis','-q:a','6',str(root/'sounds/end_of_all_charge.ogg')],check=True)
save('end_of_all_impact',np.zeros(sr//10))
for phase in ['charge','impact']:sounds['end_of_all_'+phase]={'sounds':[{'name':'arcane_cataclysm:end_of_all_'+phase,'stream':True}]}
(root/'sounds.json').write_text(json.dumps(sounds,indent=2))
