"""Offline GLSL acceptance preview: pip install moderngl numpy pillow.
Needs EGL/OpenGL (Mesa works). Renders all six shot states at the game target size.
This complements, and does not replace, a Minecraft client run.
"""
import moderngl as gl,numpy as np,time,sys
from pathlib import Path
from PIL import Image
P=Path(__file__).resolve().parents[2]/'src/main/resources/assets/arcane_cataclysm';out=Path('previews/cinematic-0.6.2/frames');out.mkdir(parents=True,exist_ok=True)
ctx=gl.create_standalone_context(backend='egl');print(ctx.info['GL_RENDERER'],flush=True)
common=(P/'shaders/include/ac_common.glsl').read_text();film=(P/'shaders/include/ac_film.glsl').read_text().replace('#moj_import <arcane_cataclysm:ac_common.glsl>',common)
vs='''#version 330
in vec2 pos;out vec2 texCoord;out vec4 vertexColor;void main(){gl_Position=vec4(pos,0,1);texCoord=vec2(pos.x*.5+.5,.5-pos.y*.5);vertexColor=vec4(1);}
'''
w,h=(960,540);buf=ctx.buffer(np.array([-1,-1,1,-1,-1,1,1,1],dtype='f4'));fb=ctx.simple_framebuffer((w,h));fb.use()
ids=list(map(int,sys.argv[1:])) or list(range(9,15))
for i in ids:
 t0=time.time();prog=ctx.program(vertex_shader=vs,fragment_shader='#version 330\n#define FILM_ID '+str(i)+'\n'+film)
 tex=ctx.texture((1024,1024),3,Image.open(P/f'textures/cinematic/landscape_{i}.png').tobytes());tex.filter=(gl.LINEAR,gl.LINEAR);tex.use(0)
 for k,v in {'ColorModulator':(1,1,1,1),'FxScreen':(w,h),'Landscape':0}.items():
  if k in prog:prog[k].value=v
 vao=ctx.simple_vertex_array(prog,buf,'pos')
 for p in [.10,.28,.50,.74,.91,1.04]:
  dur=[29,31,33,35,37,40][i-9];prog['FxParams'].value=(i,p,1,1);prog['FxTime'].value=p*dur;prog['FxImpact'].value=((p-1)*dur*20,0,0,0)
  vao.render(gl.TRIANGLE_STRIP);ctx.finish();img=Image.frombytes('RGB',(w,h),fb.read(components=3)).transpose(Image.Transpose.FLIP_TOP_BOTTOM);img.save(out/f'{i}-{p:.2f}.png'); assert np.asarray(img).std()>8., (i,p,'blank or nearly flat frame')
 print(i,round(time.time()-t0,2),'s',flush=True)
