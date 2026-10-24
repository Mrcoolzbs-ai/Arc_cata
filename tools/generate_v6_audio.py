"""Original deterministic stereo scores + elemental Foley for spells 1–15.
No external samples; never opens or writes End of All. Requires numpy and ffmpeg.
Each charge is a through-composed four-chapter cue, exactly impact/20 seconds.
python3 tools/generate_v6_audio.py [--out /some/review/directory] [--only 4]
"""
from pathlib import Path
import argparse,json,subprocess,hashlib
import numpy as np
SR=32000
IDS=['heavens_lance','thousand_blades','snow_grave','tempest_wrath','starfall','tectonic_rift','inferno_tide','singularity','solar_nova','last_cataclysm','verdant_ruin','obsidian_cathedral','abyssal_deluge','world_sunder','rune_apocalypse']
SECONDS=[18,20,21,22,21,23,24,25,26,29,31,33,35,37,40]
# Root MIDI, mode intervals, motif, tempo, instruments. End of All is deliberately absent.
PLANS=[
 (50,[0,2,4,7,9],[0,4,2,6,4,7],72,'brass','bell'),
 (54,[0,2,3,7,10],[0,3,1,4,2,6,3,7],104,'pluck','metal'),
 (62,[0,1,5,7,8],[0,4,2,1,6,4],63,'glass','choir'),
 (45,[0,2,3,5,7],[0,1,4,3,5,2],84,'brass','strings'),
 (48,[0,2,3,7,9],[0,2,4,6,5,7],78,'choir','bell'),
 (38,[0,1,3,5,7],[0,0,3,2,5,4],66,'brass','metal'),
 (41,[0,1,4,5,7],[0,2,1,4,3,6],92,'strings','brass'),
 (42,[0,1,3,6,7],[0,3,1,5,2,6],58,'choir','glass'),
 (55,[0,2,4,6,7],[0,2,4,6,7,9],76,'brass','bell'),
 (37,[0,1,3,6,7],[0,3,2,5,1,7],64,'choir','brass'),
 (52,[0,2,3,7,9],[0,4,3,2,1,0],60,'flute','strings'),
 (36,[0,1,3,5,8],[0,2,4,3,1,5],56,'organ','bell'),
 (43,[0,2,3,5,10],[0,3,2,5,4,7],62,'choir','glass'),
 (34,[0,1,3,5,7],[0,0,2,3,1,5],70,'brass','strings'),
 (49,[0,1,4,6,8],[0,4,1,5,2,6],68,'organ','glass')]
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources/assets/arcane_cataclysm'

def avg(x,n):
 c=np.r_[0.,np.cumsum(x,dtype=np.float64)];v=(c[n:]-c[:-n])/n
 return np.pad(v,(n//2,n-1-n//2))[:len(x)]
def pitch(n):return 440*2**((n-69)/12)
def degree(root,mode,k):return root+12*(k//len(mode))+mode[k%len(mode)]
def voice(note,duration,kind,seed):
 t=np.arange(max(1,int(duration*SR)))/SR;f=pitch(note);rng=np.random.default_rng(seed)
 attack={'bell':.009,'metal':.003,'pluck':.006,'glass':.015,'brass':.16,'strings':.23,'choir':.32,'flute':.14,'organ':.07}[kind]
 env=(1-np.exp(-t/attack))*np.minimum(1,np.maximum(0,duration-t)/min(.45,duration*.3))
 vibrato=(.002 if kind in ['strings','choir','flute'] else .0004)*np.sin(2*np.pi*(4.7+seed%7*.09)*t)
 phase=2*np.pi*f*t+f*vibrato*.015
 y=np.zeros_like(t)
 for h in range(1,11):
  if kind=='bell':ratio=[1,2.01,2.76,4.08,5.43,6.8,8.1,9.22,10.5,12.8][h-1];gain=np.exp(-h*.45)*np.exp(-t*h/1.7)
  elif kind=='metal':ratio=[1,1.48,2.09,2.68,3.9,5.1,6.8,8.9,9.8,12.4][h-1];gain=np.exp(-h*.45)*np.exp(-t*(.8+h*.7))
  else:
   ratio=h
   if kind=='choir':gain=(np.exp(-((f*h-700)/310)**2)+.55*np.exp(-((f*h-1220)/400)**2)+.16)/h**.8
   elif kind=='strings':gain=1/h**1.15
   elif kind=='brass':gain=(1+.6*(h%2))/h**1.2
   elif kind=='organ':gain=({1:1,2:.65,3:.15,4:.38,6:.14,8:.12}.get(h,.015))
   elif kind=='flute':gain=np.exp(-h*.85)
   elif kind=='glass':gain=np.exp(-h*.65)*np.exp(-t*(.35+h*.15))
   else:gain=np.exp(-t*(1+h*.45))/h**1.5
  y+=np.sin(phase*ratio+rng.uniform(-.08,.08))*gain
 y/=max(1.,np.max(abs(y)))
 if kind in ['choir','strings']:
  y=(y+.18*np.sin(phase*1.004+1.1)+.15*np.sin(phase*.997-.7))*.78
 if kind in ['bell','glass','pluck','metal']:env*=np.exp(-t/({'bell':3.5,'glass':4.,'pluck':1.2,'metal':.65}[kind]))
 return (y*env).astype(np.float32)
def mix_note(out,start,duration,note,kind,gain,pan,seed):
 begin=int(start*SR)
 if begin>=len(out):return
 y=voice(note,min(duration,(len(out)-begin)/SR),kind,seed);n=min(len(y),len(out)-begin)
 if n<=0:return
 out[begin:begin+n,0]+=y[:n]*gain*np.sqrt((1-pan)*.5)
 out[begin:begin+n,1]+=y[:n]*gain*np.sqrt((1+pan)*.5)

def foley(index,duration,impact,rng):
 t=np.arange(int(SR*duration))/SR;u=t/duration;noise=rng.normal(0,1,len(t));low=avg(noise,320);body=avg(noise,30);air=noise-avg(noise,9)
 out=np.zeros_like(t);events=[0.,.38,1.05,2.1] if impact else [duration*x for x in [.08,.25,.43,.63,.78,.91]]
 if index==1:events=[duration*x for x in [.12,.19,.28,.36,.44,.55,.67,.73,.82,.9,.96]] if not impact else [0,.08,.17,.32,.55,1.2]
 for k,start in enumerate(events):
  a=np.maximum(0,t-start);env=(t>=start)*(1-np.exp(-a*90))*np.exp(-a/(1.5 if index in [3,5,9,13] else .65))
  if index==0:y=.12*np.sin(2*np.pi*(110*a+40*a*a))*np.exp(-a)+body*.4
  elif index==1:y=(air*.17+body*.75)*np.exp(-a*5)+np.sin(2*np.pi*(850+137*k)*a)*.035
  elif index==2:y=air*.06+sum(np.sin(2*np.pi*f*a)*.045*np.exp(-a*2) for f in [1120+k*71,1903+k*43,3021+k*53])
  elif index==3:y=low*4.8+body*.4+air*.08*np.exp(-a*10)
  elif index==4:y=low*2.5+body*.4+np.sin(2*np.pi*(95*a-15*a*a))*.06
  elif index==5:y=low*5.+body*.6+air*.12*np.exp(-a*8)
  elif index==6:y=body*.8+low*2.+air*.10
  elif index==7:y=low*2.+np.sin(2*np.pi*(45*a+8*a*a))*.09+body*.18
  elif index==8:y=low*2.+body*.2+np.sin(2*np.pi*(180*a+10*a*a))*.055
  elif index==9:y=low*5.+body*.3+np.sin(2*np.pi*37*a)*.10
  elif index==10:y=low*2.3+body*.35+air*.035
  elif index==11:y=low*3.+sum(np.sin(2*np.pi*f*a)*.035 for f in [73.42,146.83,220.25,293.66])
  elif index==12:y=avg(noise,55)*1.8+low*2.8+air*.025
  elif index==13:y=low*5.7+body*.5+air*.10*np.exp(-a*3)
  else:y=body*.25+low*2.+sum(np.sin(2*np.pi*f*a)*.03 for f in [587.3,831.,1174.7])
  out+=y*env*(.6 if impact else .16+.26*k/len(events))
 if not impact:
  rise=np.maximum(0,(u-.70)/.30);out+=(low*2.4+body*.12)*rise**2
 else:
  out+=np.sin(2*np.pi*(55*t-2*t*t))*.15*np.exp(-t*2)
 return out.astype(np.float32)

def make(index,impact):
 root,mode,motif,bpm,pad,lead=PLANS[index];duration=8.0 if impact else SECONDS[index];out=np.zeros((int(duration*SR),2),np.float32);rng=np.random.default_rng(6010+index*101+impact)
 if impact:
  # Resolve into the same tonic used by the end of the charge; score and Foley form a single strike.
  for k,d in enumerate([0,2,4,7]):mix_note(out,0,7.5,degree(root,mode,d)+(12 if k>0 else 0),pad,.10,-.65+k*.43,8800+index*7+k)
  mix_note(out,.05,5.,root+24,lead,.075,.2,9500+index)
 else:
  # Four harmonic chapters, a moving bass and a motif that changes register and answer phrase.
  bounds=[0,.18,.40,.66,1.];progressions=[0,3,1,0] if index not in [7,9,11,14] else [0,1,3,0]
  for chapter in range(4):
   start=bounds[chapter]*duration;end=bounds[chapter+1]*duration;base=progressions[chapter]
   for k,d in enumerate([base,base+2,base+4]):
    mix_note(out,start,max(1,end-start+.65),degree(root,mode,d)+12,pad,.055+chapter*.014,-.7+k*.7,1000+index*99+chapter*7+k)
   mix_note(out,start,end-start+.3,degree(root-12,mode,base),'organ',.065,0,2000+index*7+chapter)
  beat=60/bpm;step=beat*(.5 if index in [1,6] else 1 if index in [0,4,8,14] else 1.5)
  k=0
  for start in np.arange(duration*.09,duration-.25,step):
   chapter=min(3,int(start/duration*4));m=motif[k%len(motif)]
   # Answer the phrase downward in the fading tree scene, upward in the solar scene.
   register=24 if index==8 and chapter>=2 else 12 if not(index==10 and chapter>=2) else 0
   note=degree(root,mode,m)+register
   gain=(.043+chapter*.009)*(.65 if index in [3,5,13] else 1.)
   mix_note(out,float(start),min(3.5,step*2.8),note,lead,gain,float(np.sin(k*1.37)*.65),3000+index*200+k)
   # Sparse low, pitched toms; no repeated sample loops.
   if k% (2 if index in [1,6,13] else 4)==0 and start>duration*.22:
    n=int(min(SR*1.2,len(out)-start*SR));t=np.arange(n)/SR;drum=np.sin(2*np.pi*(47*t+19*(1-np.exp(-t*18))/18))*np.exp(-t*5)*(1-np.exp(-t*120))
    at=int(start*SR);out[at:at+n]+=(drum*.055)[:,None]
   k+=1
  # A final harmonic ascent, not an endless riser or repeated short loop.
  for k,d in enumerate([0,2,4,7]):mix_note(out,duration*.82+k*duration*.033,duration*.18-k*duration*.033,degree(root,mode,d)+24,'choir' if index>=9 else lead,.055,-.6+k*.4,7500+index*7+k)
 f=foley(index,duration,impact,rng);out[:,0]+=f*.82;out[:,1]+=np.roll(f,int(SR*.003))*.76
 # Stereo early reflections and a sparse diffuse tail, preserving transients.
 dry=out.copy()
 for delay,gain in [(.071,.12),(.137,.09),(.223,.10),(.359,.065),(.577,.045),(.839,.03)]:
  n=int(delay*SR);out[n:]+=dry[:-n,::-1]*gain
 t=np.arange(len(out))/SR;out*=np.minimum(1,t/(.012 if impact else .35))[:,None]*np.minimum(1,(duration-t)/(.75 if impact else .065))[:,None]
 peak=float(np.max(abs(out)));out*=.75/max(.75,peak)
 return out

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--out',type=Path,default=ROOT/'sounds');ap.add_argument('--only',type=int);args=ap.parse_args();args.out.mkdir(parents=True,exist_ok=True)
 sounds=json.loads((ROOT/'sounds.json').read_text());end_before={x:hashlib.sha256((ROOT/'sounds'/x).read_bytes()).hexdigest() for x in ['end_of_all_charge.ogg','end_of_all_impact.ogg']};report=[]
 indices=range(15) if args.only is None else [args.only-1]
 for i in indices:
  for impact in [False,True]:
   name=IDS[i]+('_impact' if impact else '_charge');out=make(i,impact);dest=args.out/(name+'.ogg')
   subprocess.run(['ffmpeg','-y','-v','error','-f','f32le','-ar',str(SR),'-ac','2','-i','-','-af',f'loudnorm=I={-17 if impact else -20}:TP=-2:LRA=9','-ar',str(SR),'-c:a','libvorbis','-q:a','5',str(dest)],input=out.astype('<f4').tobytes(),check=True)
   sounds[name]={'subtitle':'subtitles.arcane_cataclysm.'+name,'sounds':[{'name':'arcane_cataclysm:'+name,'stream':True}]}
   duration=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','csv=p=0',str(dest)]))
   expected=8 if impact else SECONDS[i];assert abs(duration-expected)<.02,(name,duration)
   report.append({'file':name+'.ogg','seconds':duration,'channels':2,'sha256':hashlib.sha256(dest.read_bytes()).hexdigest()});print(name,duration,flush=True)
 for x,h in end_before.items():assert hashlib.sha256((ROOT/'sounds'/x).read_bytes()).hexdigest()==h
 if args.out.resolve()==(ROOT/'sounds').resolve():(ROOT/'sounds.json').write_text(json.dumps(sounds,indent=2)+'\n')
 manifest=args.out/'audio_v6_manifest.json'
 if args.only is not None and manifest.exists():
  old={x['file']:x for x in json.loads(manifest.read_text())};old.update({x['file']:x for x in report});report=[old[name+'_'+phase+'.ogg'] for name in IDS for phase in ['charge','impact'] if name+'_'+phase+'.ogg' in old]
 manifest.write_text(json.dumps(report,indent=2)+'\n')
if __name__=='__main__':main()
