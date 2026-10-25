"""Technical QA, not a claim of subjective listening quality. Requires FFmpeg/numpy."""
from pathlib import Path
import json,subprocess,re,hashlib,numpy as np
P=Path(__file__).resolve().parents[2];R=P/'src/main/resources/assets/arcane_cataclysm'
a=json.loads((R/'sounds/audio_v062_manifest.json').read_text());notes=json.loads((P/'docs/compositions_v062.json').read_text())['scores'];report=[]
for x in a:
 f=R/'sounds'/x['file'];assert hashlib.sha256(f.read_bytes()).hexdigest()==x['sha256']
 p=subprocess.run(['ffmpeg','-hide_banner','-i',str(f),'-af','loudnorm=I=-18:TP=-2:LRA=10:print_format=json','-f','null','-'],capture_output=True,text=True,check=True)
 stats=json.loads(re.findall(r'\{[^{}]+\}',p.stderr)[-1]);assert -19<float(stats['input_i'])<-17,(f,stats);assert float(stats['input_tp'])<-.8,(f,stats)
 raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(f),'-f','f32le','-ac','2','-ar','8000','-']);v=np.frombuffer(raw,'<f4').reshape(-1,2);assert np.isfinite(v).all();corr=float(np.corrcoef(v.T)[0,1]);assert corr<.999
 n=notes[x['file'].replace('_score.ogg','')];peaks=[]
 for t in np.arange(0,x['seconds'],.1):peaks.append(len(set(k['part'] for k in n if k['time']<=t<k['time']+k['duration'])))
 assert len(x['parts'])>=10 and max(peaks)>=6
 report.append(dict(file=x['file'],loudness_lufs=float(stats['input_i']),true_peak_dbtp=float(stats['input_tp']),loudness_range_lu=float(stats['input_lra']),stereo_correlation=round(corr,5),notes=len(n),parts=x['parts'],max_simultaneous_parts=max(peaks),seconds_with_four_or_more_parts=round(sum(k>=4 for k in peaks)*.1,1),seconds=x['seconds'],sha256=x['sha256']))
 print('ENSEMBLE_AUDIO_PASS',f.name,'LUFS='+stats['input_i'],'dBTP='+stats['input_tp'],'parts='+str(len(x['parts'])))
(P/'docs/AUDIO_QA_0.6.2.json').write_text(json.dumps(report,indent=2)+'\n')
print('ENSEMBLE_QA_PASS recordings=6')
