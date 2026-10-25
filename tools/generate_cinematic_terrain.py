"""Authored continuous 4 km terrain/biome fields for films 10–15, not finite dioramas.
RGB: integer altitude, fractional altitude, rock strata noise. Original procedural art, no external images.
"""
from pathlib import Path
import numpy as np
from PIL import Image
R=Path(__file__).resolve().parents[1]/'src/main/resources/assets/arcane_cataclysm/textures/cinematic'
R.mkdir(parents=True,exist_ok=True)
N=1024
z,x=np.mgrid[:N,:N];x=(x-N/2)*4.;z=(z-N/2)*4.;d=np.hypot(x,z)
def field(seed,base):
 rng=np.random.default_rng(seed);v=np.zeros((N,N))
 for k,w in enumerate([.52,.25,.13,.065,.035]):
  n=base*2**k;a=rng.uniform(0,1,(n,n));im=Image.fromarray(a.astype('float32')).resize((N,N),Image.Resampling.BICUBIC)
  v+=np.asarray(im)*w
 return np.clip(v,0,1)
for id in range(9,15):
 a=field(6200+id,6);b=field(6700+id,10);m=field(6900+id,13)
 mountains=18+155*np.maximum(0,(a-.23))**1.25+28*b
 valley=np.clip((d-250)/650,0,1);valley=valley*valley*(3-2*valley)
 h=24+mountains*valley+8*b
 if id==10:h=18+mountains*.75+12*b
 if id==11:h=20+mountains*valley+6*b
 if id==12:
  # Real coast: inhabited terraces at the foreground, deep seafloor behind the skyline.
  coast=np.clip((z+110)/340,0,1);coast=coast*coast*(3-2*coast)
  h=(12+mountains*.35)*(1-coast)+4*coast # Shader subtracts 26 -> ocean floor -22.
 if id==13:h=25+mountains*1.15
 rgb=np.dstack([np.floor(np.clip(h,0,255)),np.mod(h,1.)*255,b*255]).astype('uint8')
 path=R/f'landscape_{id}.png';Image.fromarray(rgb).save(path)
 path.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":true,"clamp":false}}\n')
 print(id,int(h.min()),int(h.max()),path.stat().st_size)
