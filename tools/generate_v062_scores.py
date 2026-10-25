"""Original six ensemble scores. FluidSynth + GeneralUser GS 1.471, never End of All.
Usage: python tools/generate_v062_scores.py --soundfont /path/GeneralUser.sf2 --library /path/libfluidsynth.so
Outputs rendered Ogg, a note-level composition manifest, and a six-file QA manifest.
No MIDI player or SoundFont is needed by the game; only these stereo recordings ship.
"""
from pathlib import Path
import argparse,ctypes as C,json,hashlib,subprocess,re
import numpy as np
from generate_v6_audio import IDS,SECONDS,ROOT,SR,foley
# Actual General MIDI patches: strings, cellos, horns, trombones, choir, flute,
# oboe, harp, tubular bells, timpani, double bass, piano. Channel 9 is percussion.
PATCH={0:48,1:42,2:60,3:57,4:52,5:73,6:68,7:46,8:14,10:47,11:43,12:45}
NAMES={0:'violins/violas',1:'cellos',2:'French horns',3:'trombones',4:'choir',5:'flute',6:'oboe',7:'harp',8:'tubular bells',9:'orchestral percussion',10:'timpani',11:'double bass',12:'pizzicato strings'}
# Root, mode, eight harmonic bars, two-bar melodic subject, soloist.
SCORES={
9:(38,[0,2,3,5,7,8,10],[0,5,3,6,0,4,5,0],[0,4,3,2,6,4,1,0,2,3,6,5,4,2,1,0],2),
10:(52,[0,2,3,5,7,9,10],[0,3,5,2,0,5,3,0],[7,9,8,7,4,5,3,2,7,8,9,6,5,3,2,0],5),
11:(36,[0,1,3,5,7,8,10],[0,1,5,3,0,6,1,0],[0,4,1,2,6,4,3,1,7,6,4,5,3,2,1,0],6),
12:(43,[0,2,3,5,7,8,10],[0,5,2,6,3,4,1,0],[0,2,4,7,6,4,3,2,1,3,5,8,7,5,2,0],1),
13:(34,[0,1,3,5,7,8,10],[0,0,5,1,3,4,6,0],[0,0,4,3,1,0,5,4,0,1,3,4,6,5,2,0],3),
14:(49,[0,1,3,6,7,8,10],[0,3,1,5,0,6,4,0],[0,4,1,5,2,6,3,1,7,6,4,2,5,3,1,0],8)}
def deg(root,mode,d):return root+12*(d//7)+mode[d%7]
def compose(i):
 root,mode,harmony,subject,solo=SCORES[i];impact=SECONDS[i];beat=impact/32.;events=[];rng=np.random.default_rng(6200+i)
 def note(ch,t,d,p,v):
  if t>=impact+4.7:return
  events.append({'part':NAMES[ch],'channel':ch,'time':round(max(0,t),4),'duration':round(min(d,impact+4.8-t),4),'note':int(np.clip(p,24,100)),'velocity':int(np.clip(v+rng.integers(-5,6),15,115))})
 for bar,base in enumerate(harmony):
  t=bar*4*beat;energy=[.50,.59,.62,.71,.82,.88,1.,.92][bar]
  # Four-voice string harmony with independent cello and real bow articulation.
  for k,d in enumerate([base,base+2,base+4]):note(0,t+k*.025,4*beat*.95,deg(root+12,mode,d),int(48+energy*17))
  for k in range(4):note(1,t+k*beat,beat*.9,deg(root,mode,base+[0,4,2,4][k]),int(54+energy*12))
  note(11,t,beat*3.8,deg(root-12 if root>42 else root,mode,base),58+int(energy*10))
  # Choir enters as a separate sustained register, not a replacement for strings.
  if bar>=2:
   for d in [base,base+2,base+4]:note(4,t+.08,beat*3.8,deg(root+24 if root<44 else root+12,mode,d),44+int(energy*15))
  # Melody breathes; the answering instrument develops the theme, with rests.
  for k in range(4):
   d=subject[(bar%2)*8+k*2];dur=beat*(1.4 if k==3 else .78)
   ch=solo if bar%4<2 else (6 if solo!=6 else 5)
   octave=24 if root<44 else 12
   note(ch,t+k*beat+.04,dur,deg(root+octave,mode,d),int(66+energy*16))
  # Independent counterline in horns, played between the principal phrases.
  if bar>=3:
   for k,d in enumerate([base+4,base+3,base+2]):note(2,t+(.5+k)*beat,beat*.9,deg(root+12,mode,d),55+int(energy*16))
  # Harp figures become a string pulse in the action chapters.
  for k in range(8):
   d=base+[0,2,4,6,4,2,7,4][k];ch=7 if bar<4 else 12
   note(ch,t+(k*.5+.015)*beat,beat*(.75 if ch==7 else .29),deg(root+24 if root<44 else root+12,mode,d),int((42 if bar<4 else 54)+energy*13))
  if bar in [0,2,4,6]:note(8,t+.04,beat*3.,deg(root+24,mode,base),49+int(energy*10))
  if bar>=3:
   for k in [0,2]:
    note(10,t+k*beat,beat*1.4,deg(root-12 if root>42 else root,mode,base),65+int(energy*23))
    note(9,t+k*beat,.4,36,58+int(energy*28))
   if bar>=5:
    for k in [1,3]:note(9,t+k*beat,.2,38,54+int(energy*23))
   if bar in [4,6]:note(9,t,2.,49,68)
  if i==10:
   # The forest's high flute hands over to low oboe and cello as life drains away.
   if bar>=5:note(6,t+2.5*beat,beat*1.2,deg(root,mode,base+2),72)
  if i==13 and bar>=4:
   for k in range(4):note(3,t+k*beat,beat*.7,deg(root+12,mode,base+[0,0,4,1][k]),77)
 # Climax lands exactly on the server impact clock, followed by an actual resolution.
 for ch,octave,v in [(0,24,80),(1,12,78),(2,12,89),(3,0,84),(4,24,72),(7,24,70)]:
  for d in [0,2,4]:note(ch,impact+.02*(d%3),4.45,deg(root+octave,mode,d),v)
 note(10,impact,2.8,root,105);note(9,impact,2.,49,91);note(9,impact,.5,36,104)
 return sorted(events,key=lambda x:x['time'])
class Synth:
 def __init__(self,library,soundfont):
  self.f=C.CDLL(library)
  def bind(n,args,result):f=getattr(self.f,n);f.argtypes=args;f.restype=result;return f
  ptr=C.c_void_p;I=C.c_int;S=C.c_char_p;D=C.c_double
  self.settings=bind('new_fluid_settings',[],ptr)();bind('fluid_settings_setnum',[ptr,S,D],I)(self.settings,b'synth.sample-rate',SR)
  self.f.fluid_settings_setnum(self.settings,b'synth.gain',.48)
  bind('fluid_settings_setint',[ptr,S,I],I)(self.settings,b'synth.polyphony',384)
  self.s=bind('new_fluid_synth',[ptr],ptr)(self.settings)
  bind('fluid_synth_sfload',[ptr,S,I],I)(self.s,str(soundfont).encode(),1)
  self.on=bind('fluid_synth_noteon',[ptr,I,I,I],I);self.off=bind('fluid_synth_noteoff',[ptr,I,I],I)
  self.cc=bind('fluid_synth_cc',[ptr,I,I,I],I);self.prog=bind('fluid_synth_program_change',[ptr,I,I],I)
  self.write=bind('fluid_synth_write_float',[ptr,I,ptr,I,I,ptr,I,I],I)
  self.delete=bind('delete_fluid_synth',[ptr],None);self.ds=bind('delete_fluid_settings',[ptr],None)
  for ch,patch in PATCH.items():self.prog(self.s,ch,patch)
  pans={0:35,1:86,2:47,3:90,4:68,5:43,6:81,7:100,8:28,10:74,11:60,12:65}
  for ch in NAMES:
   self.cc(self.s,ch,10,pans.get(ch,64));self.cc(self.s,ch,7,100);self.cc(self.s,ch,91,42);self.cc(self.s,ch,93,10 if ch==0 else 0)
 def render(self,notes,duration):
  length=int(duration*SR);out=np.zeros((length,2),np.float32);actions=[]
  for n in notes:
   ch=n['channel'];p=n['note'];at=int(n['time']*SR);end=int((n['time']+n['duration'])*SR)
   actions.extend([(at,1,ch,p,n['velocity']),(min(length,end),0,ch,p,0)])
  actions.sort();pos=0
  def fill(end):
   nonlocal pos
   if end>pos:
    block=np.zeros((end-pos,2),np.float32);addr=block.ctypes.data
    self.write(self.s,end-pos,addr,0,2,addr,1,2);out[pos:end]=block;pos=end
  for at,kind,ch,p,v in actions:
   fill(min(at,length))
   (self.on(self.s,ch,p,v) if kind else self.off(self.s,ch,p))
  fill(length);self.delete(self.s);self.ds(self.settings);return out

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--soundfont',type=Path,required=True);ap.add_argument('--library',required=True);a=ap.parse_args();manifest=[];allnotes={}
 for i in range(9,15):
  notes=compose(i);allnotes[IDS[i]]=notes;duration=SECONDS[i]+5
  out=Synth(a.library,a.soundfont).render(notes,duration)
  # Original foley is a quiet bed, leaving room for the ensemble's transient detail.
  rng=np.random.default_rng(6200+i);f=foley(i,SECONDS[i],False,rng)*.11
  out[:len(f),0]+=f;out[:len(f),1]+=np.roll(f,91)
  f=foley(i,5,True,rng)*.22;at=SECONDS[i]*SR;out[at:at+len(f),0]+=f;out[at:at+len(f),1]+=np.roll(f,117)
  t=np.arange(len(out))/SR;out*=np.minimum(1,t/.5)[:,None]*np.minimum(1,(duration-t)/1.2)[:,None]
  dest=ROOT/'sounds'/(IDS[i]+'_score.ogg')
  raw=out.astype('<f4').tobytes()
  cmd=['ffmpeg','-y','-hide_banner','-v','info','-f','f32le','-ar',str(SR),'-ac','2','-i','-']
  analysis=subprocess.run(cmd+['-af','loudnorm=I=-18:TP=-2:LRA=10:print_format=json','-f','null','-'],input=raw,capture_output=True,check=True)
  measured=json.loads(re.findall(r'\{[^{}]+\}',analysis.stderr.decode())[-1])
  norm='loudnorm=I=-18:TP=-2:LRA=10:linear=true:'+':'.join(k+'='+measured[v] for k,v in [('measured_I','input_i'),('measured_TP','input_tp'),('measured_LRA','input_lra'),('measured_thresh','input_thresh'),('offset','target_offset')])
  subprocess.run(cmd+['-af',norm,'-ar',str(SR),'-c:a','libvorbis','-q:a','6',str(dest)],input=raw,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,check=True)
  item={'file':dest.name,'seconds':duration,'channels':2,'notes':len(notes),'parts':sorted(set(n['part'] for n in notes)),'sha256':hashlib.sha256(dest.read_bytes()).hexdigest()};manifest.append(item);print(dest.name,len(notes),'notes',len(item['parts']),'parts',flush=True)
 (ROOT/'sounds/audio_v062_manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
 (Path(__file__).resolve().parents[1]/'docs/compositions_v062.json').write_text(json.dumps({'patches':PATCH,'scores':allnotes},indent=2)+'\n')
if __name__=='__main__':main()
