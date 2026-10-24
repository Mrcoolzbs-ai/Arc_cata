"""Six original continuous film scores, with scene-synchronised Foley and an aftermath.
No external samples; never reads or writes End of All. Run from repository root.
The local film uses RECORDS; charge/impact PLAYERS cues remain for world observers.
"""
from generate_v6_audio import *

def score(i):
 root,mode,motif,bpm,pad,lead=PLANS[i];impact=SECONDS[i];duration=impact+5
 out=np.zeros((duration*SR,2),np.float32);rng=np.random.default_rng(6100+i)
 # Six edited shots: introduction / intimate theme / revelation / action / climax / aftermath.
 bounds=np.array([0,.18,.38,.62,.84,1.])*impact
 ends=list(bounds[1:])+[duration]
 harmony=[0,5,3,1,4,0] if i!=10 else [0,3,5,2,1,0]
 for chapter,(start,end,base) in enumerate(zip(bounds,ends,harmony)):
  amp=[.047,.055,.073,.085,.11,.045][chapter]
  # Moving four-voice harmony, stereo strings and choir/organ, with independent inner voice.
  for k,d in enumerate([base,base+2,base+4,base+6]):
   note=degree(root,mode,d)+12
   mix_note(out,float(start),end-start+1.,note,'strings',amp,-.8+k*.53,11000+i*71+chapter*13+k)
   mix_note(out,float(start)+.12,end-start+.8,note+12,'choir',amp*.45,.7-k*.46,12100+i*97+chapter*13+k)
  mix_note(out,float(start),end-start+.7,degree(root-12,mode,base),'organ',amp*.9,0,13000+i*13+chapter)
  if chapter in (2,4):
   for k,d in enumerate([base,base+2,base+4]):mix_note(out,float(start)+k*.06,end-start,degree(root,mode,d)+12,'brass',amp*.6,-.3+k*.3,14000+i*29+chapter+k)
 # A recognisable two-phrase melodic theme, then development and a final slower answer.
 beat=60/bpm
 for k,start in enumerate(np.arange(impact*.05,duration-1.,beat*1.5)):
  ch=min(5,int(np.searchsorted(bounds,start,side='right')-1));position=k%len(motif)
  d=motif[position]+(2 if ch==2 else 0)
  if ch==5:d=motif[-1-position]
  note=degree(root,mode,d)+(24 if i in (9,14) and ch in (2,4) else 12)
  instrument='flute' if i==10 else 'bell' if i==11 else 'brass' if i in (9,13) else 'choir' if i==12 else 'glass'
  mix_note(out,float(start),min(3.,beat*2.9),note,instrument,.065 if ch!=5 else .040,np.sin(k*.9)*.45,15000+i*131+k)
  # String ostinato is introduced with the action, not present under every shot.
  if ch in (2,3,4):
   for sub in (0,.5,1.):mix_note(out,float(start+sub*beat),beat*.65,degree(root,mode,harmony[ch]+[0,2,4][int(sub*2)])+12,'pluck',.028,(-1 if sub==.5 else 1)*.6,16000+k*7+int(sub*2))
  if ch in (3,4) and k%2==0:
   n=min(int(SR*1.5),len(out)-int(start*SR));t=np.arange(n)/SR
   drum=np.sin(2*np.pi*(43*t+2*(1-np.exp(-t*12))))*np.exp(-t*4)*(1-np.exp(-t*100))
   at=int(start*SR);out[at:at+n]+=(drum*.09)[:,None]
 # Element-specific continuous ambience plus the actual strike, inside the same full-length cue.
 f=foley(i,impact,False,rng)*.32;out[:len(f),0]+=f;out[:len(f),1]+=np.roll(f,80)
 hit=foley(i,5,True,rng)*.52;at=impact*SR;out[at:at+len(hit),0]+=hit;out[at:at+len(hit),1]+=np.roll(hit,97)
 for k,d in enumerate([0,2,4,7]):mix_note(out,impact,4.8,degree(root,mode,d)+12,'choir',.075,-.75+k*.5,17000+i*7+k)
 dry=out.copy()
 for delay,gain in [(.081,.16),(.173,.12),(.293,.10),(.439,.08),(.617,.06),(.887,.045),(1.21,.025)]:
  n=int(delay*SR);out[n:]+=dry[:-n,::-1]*gain
 t=np.arange(len(out))/SR;out*=np.minimum(1,t/.65)[:,None]*np.minimum(1,(duration-t)/1.3)[:,None]
 return out

def main():
 sounds=json.loads((ROOT/'sounds.json').read_text());report=[]
 for i in range(9,15):
  name=IDS[i]+'_score';dest=ROOT/'sounds'/(name+'.ogg');audio=score(i)
  subprocess.run(['ffmpeg','-y','-v','error','-f','f32le','-ar',str(SR),'-ac','2','-i','-','-af','loudnorm=I=-19:TP=-2:LRA=10','-ar',str(SR),'-c:a','libvorbis','-q:a','5',str(dest)],input=audio.astype('<f4').tobytes(),check=True)
  sounds[name]={'sounds':[{'name':'arcane_cataclysm:'+name,'stream':True}]}
  report.append({'file':dest.name,'seconds':SECONDS[i]+5,'channels':2,'sha256':hashlib.sha256(dest.read_bytes()).hexdigest()});print(name,SECONDS[i]+5,flush=True)
 (ROOT/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n');(ROOT/'sounds/audio_v061_manifest.json').write_text(json.dumps(report,indent=2)+'\n')
if __name__=='__main__':main()
