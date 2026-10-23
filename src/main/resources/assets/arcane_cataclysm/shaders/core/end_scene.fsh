#version 150
uniform float FxTime;
uniform float FxFlash;
uniform float FxSeed;
uniform vec2 FxScreen;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
float hash(vec2 p){return fract(sin(dot(p,vec2(127.1,311.7))+FxSeed*.01)*43758.5453);}
float noise(vec2 p){vec2 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(hash(i),hash(i+vec2(1,0)),f.x),mix(hash(i+vec2(0,1)),hash(i+1.),f.x),f.y);}
float fbm(vec2 p){return noise(p)*.55+noise(p*2.07)*.27+noise(p*4.19)*.13+noise(p*8.4)*.05;}
mat2 rot(float a){return mat2(cos(a),-sin(a),sin(a),cos(a));}
vec3 landscape(vec2 uv,float t){
 float shot=floor(t/2.5),local=fract(t/2.5);vec2 p=uv*vec2(FxScreen.x/FxScreen.y,1.);
 vec3 ro=vec3(shot*193.+sin(t*.15)*10.,39.+sin(shot*2.)*7.,shot*317.+t*6.);
 vec3 rd=normalize(vec3(p.x,p.y-.25,1.35));rd.xz=rot(shot*.73)*rd.xz;
 vec3 col=mix(vec3(.035,.02,.065),vec3(.19,.075,.13),clamp(rd.y*.5+.5,0.,1.));
 float dist=0.;bool hit=false;vec3 q=ro;
 for(int i=0;i<80;i++){
  q=ro+rd*dist;vec2 cell=floor(q.xz/16.);float original=8.+floor(fbm(floor(q.xz/2.)/32.)*28.);
  float onset=hash(cell+shot*41.)*.68+.12;
  float vanish=smoothstep(onset,onset+.14,local);float ground=original-vanish*140.;
  float d=q.y-ground;
  if(d<.65){hit=true;break;}dist+=clamp(d*.42,.65,7.);if(dist>280.)break;
 }
 if(hit){
  vec2 cell=floor(q.xz/16.);float onset=hash(cell+shot*41.)*.68+.12;
  float vanish=smoothstep(onset,onset+.14,local);
  float top=8.+floor(fbm(floor(q.xz/2.)/32.)*28.);
  vec3 grass=mix(vec3(.08,.18,.11),vec3(.19,.29,.13),hash(floor(q.xz)));
  vec3 rock=mix(vec3(.105,.1,.13),vec3(.28,.22,.24),hash(floor(q.xz+q.y)));
  col=mix(grass,rock,step(1.,top-q.y));
  vec2 edge=abs(fract(q.xz/16.)-.5);float grid=smoothstep(.465,.495,max(edge.x,edge.y));
  float flash=FxFlash*exp(-pow((local-onset)*18.,2.));col+=grid*flash*vec3(2.5,.24,.5);
  col+=vec3(.4,.01,.045)*vanish;col=mix(col,vec3(.09,.025,.08),1.-exp(-dist*.007));
 }
 // Suspended voxel fragments and ritual fissures above the vanishing scene.
 vec2 cells=uv*vec2(27.,16.);vec2 id=floor(cells);vec2 f=fract(cells)-.5;
 float rnd=hash(id+shot*47.);float pulse=sin(clamp((local-rnd*.7)*3.,0.,1.)*3.14159);
 float square=step(max(abs(f.x),abs(f.y)),.025+.07*pulse);
 col+=square*pulse*vec3(.45,.15,.22);
 float cut=1.-smoothstep(.0,.055,min(local,1.-local));col*=1.-cut*.95*FxFlash;
 return col;
}
vec3 planet(vec2 uv,float t){
 float pull=smoothstep(35.,42.,t);float radius=1.1-.77*pull;
 vec2 p=vec2(uv.x*FxScreen.x/FxScreen.y,uv.y-.08)/radius*.5;
 float d=length(p);vec3 col=vec3(.004,.003,.012);
 float stars=step(.9985,hash(floor(texCoord*FxScreen*.5)));col+=stars*.35;
 if(d<1.){
  vec3 n=vec3(p.x,-p.y,sqrt(1.-d*d));vec3 nr=n;nr.xz=rot(t*.035)*nr.xz;
  vec2 sphere=vec2(atan(nr.z,nr.x),asin(nr.y));float terrain=fbm(sphere*7.);
  float land=smoothstep(.47,.52,terrain);
  vec3 natural=mix(vec3(.025,.085,.15),vec3(.08,.19,.105),land);
  float density=t<42.?.03+.13*clamp((t-35.)/7.,0.,1.):.16+.84*smoothstep(42.,45.,t);
  float bleed=smoothstep(.0,.85,density+noise(sphere*18.)*.22);
  col=mix(natural,vec3(.38,.006,.038)*(0.7+terrain),bleed);
  float lighting=.18+.82*max(0.,dot(n,normalize(vec3(-.5,.7,1.))));col*=lighting;
  float grid=pow(abs(sin(sphere.x*24.)*sin(sphere.y*24.)),24.);col+=grid*bleed*vec3(.25,.04,.06);
  col+=pow(1.-n.z,4.)*vec3(.15,.035,.09);
 }else{float glow=exp(-(d-1.)*24.);col+=glow*vec3(.28,.01,.055);}
 float end=smoothstep(44.2,45.,t);col=mix(col,vec3(.65,.016,.055),end*.35);
 return col;
}
void main(){vec2 uv=texCoord*2.-1.;float t=FxTime;vec3 c=t<35.?landscape(vec2(uv.x,-uv.y),t):planet(uv,t);float vignette=1.-.25*dot(uv,uv);c*=vignette;c+=vec3(hash(floor(texCoord*FxScreen)+floor(t*24.))-.5)*.025*FxFlash;fragColor=vec4(c,1.);}
