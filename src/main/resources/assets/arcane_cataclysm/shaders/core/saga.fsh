#version 150
#moj_import <arcane_cataclysm:ac_common.glsl>
// Six self-contained films. All geometry is procedural and isolated from the real Minecraft world.
// Catalogue ids 9..14; FxParams.y is server-catalogue impact-normalized client age.
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec4 FxParams;
uniform vec4 FxColor2;
uniform vec4 FxImpact;
uniform float FxTime;
uniform vec2 FxScreen;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
const float PI=3.14159265359,TAU=6.28318530718;
vec3 ro,rd,col,key,fill;
float depth=100000.,p,tm;
float sat(float x){return clamp(x,0.,1.);}
float ease(float a,float b,float x){return smoothstep(a,b,x);}
float hash(float x){return fract(sin(x*127.13+9.7)*43758.5453);}
float band(float x,float w){return exp(-min(70.,x*x/(w*w)));}
vec3 film(vec3 x){return clamp((x*(2.51*x+.03))/(x*(2.43*x+.59)+.14),0.,1.);}
mat2 rot(float a){return mat2(cos(a),-sin(a),sin(a),cos(a));}
void camera(vec3 origin,vec3 target,vec2 uv,float lens){
 ro=origin;vec3 f=normalize(target-origin),right=normalize(cross(f,vec3(0,1,0))),up=cross(right,f);
 rd=normalize(f*lens+right*uv.x+up*uv.y);
}
void box(vec3 center,vec3 size,vec3 color,float emission){
 size=max(size,vec3(.015));vec3 inv=1./(rd+vec3(.000001));
 vec3 a=(center-size-ro)*inv,b=(center+size-ro)*inv,lo=min(a,b),hi=max(a,b);
 float t=max(lo.x,max(lo.y,lo.z)),end=min(hi.x,min(hi.y,hi.z));
 if(t<=0.||t>end||t>depth)return;
 vec3 q=ro+rd*t-center,v=abs(q/size),normal;
 if(v.x>v.y&&v.x>v.z)normal=vec3(sign(q.x),0,0);else if(v.y>v.z)normal=vec3(0,sign(q.y),0);else normal=vec3(0,0,sign(q.z));
 float lit=.32+.68*max(0.,dot(normal,normalize(vec3(-.5,.8,-.35))));
 float n=acNoise((q+center)*2.3),edges=pow(max(v.x,max(v.y,v.z)),25.);
 float bricks=step(.95,fract((q.y+center.y)*1.4))*.14;
 if(color.g>color.r*1.5)bricks=0.;
 bool urban=color.b>.19&&color.g>.15&&color.r<.14;
 vec3 pos=q+center;
 float window=step(.30,fract(pos.y*.95))*step(fract(pos.y*.95),.70)*step(.28,fract((normal.x==0.?pos.x:pos.z)*.9))*step(fract((normal.x==0.?pos.x:pos.z)*.9),.66);
 float litWindow=step(.48,acHash(floor(pos*vec3(.9,.95,.9))));
 vec3 material=color*(lit*(.75+n*.35)-bricks)+fill*.035;
 material+=key*emission*(.15+pow(n,4.));
 if(urban&&abs(normal.y)<.5)material+=vec3(.21,.20,.13)*window*litWindow*(1.-ease(.82,1.05,p));
 material+=color*pow(1.-abs(dot(rd,normal)),3.)*.12;
 float fog=1.-exp(-t*.0025);col=mix(material,fill*.08,fog);depth=t;
}
void sphere(vec3 center,float radius,vec3 color,float glow){
 vec3 o=ro-center;float b=dot(o,rd),h=b*b-dot(o,o)+radius*radius;if(h<0.)return;
 float t=-b-sqrt(h);if(t<=0.||t>depth)return;vec3 n=normalize(ro+rd*t-center);
 float light=.15+.85*max(0.,dot(n,normalize(vec3(-.6,.4,-.7))));
 float noise=acFbm(n*6.+vec3(0,tm*.04,0));col=color*light*(.65+noise*.6)+key*glow*(.3+noise*.7);
 col+=fill*pow(1.-abs(dot(n,rd)),4.)*.35;depth=t;
}
vec2 segment(vec3 a,vec3 b){
 vec3 v=b-a,w=ro-a;float vv=dot(v,v),rv=dot(rd,v),rw=dot(rd,w),vw=dot(v,w);
 float u=clamp((vw-rv*rw)/max(.00001,vv-rv*rv),0.,1.),d=max(0.,dot(a+v*u-ro,rd));
 return vec2(length(ro+rd*d-a-v*u),d);
}
void beam(vec3 a,vec3 b,float width,vec3 color,float power){
 vec2 d=segment(a,b);if(d.y<=.001||d.y>depth+.08)return;
 col+=color*(band(d.x,width)+band(d.x,width*5.)*.12)*power;
}
// Solid branches and buttresses use true ray/capsule intersections, not screen-space line art.
void branch(vec3 a,vec3 b,float radius,vec3 color){
 vec3 ba=b-a,oa=ro-a;float baba=dot(ba,ba),bard=dot(ba,rd),baoa=dot(ba,oa),rdoa=dot(rd,oa),oaoa=dot(oa,oa);
 float A=baba-bard*bard,B=baba*rdoa-baoa*bard,C=baba*oaoa-baoa*baoa-radius*radius*baba;
 float h=B*B-A*C;if(h<0.||abs(A)<.00001)return;float t=(-B-sqrt(h))/A,y=baoa+t*bard;
 if(y<0.||y>baba)return;if(t<=0.||t>depth)return;
 vec3 n=normalize(oa+rd*t-ba*y/baba);float light=.24+.76*max(0.,dot(n,normalize(vec3(-.5,.8,-.5))));
 float grain=acNoise((ro+rd*t)*4.);col=color*light*(.65+grain*.5)+fill*.02;depth=t;
}
void glyph(vec3 center,vec3 normal,float radius,float angle,float power){
 float den=dot(rd,normal);if(abs(den)<.002)return;float t=dot(center-ro,normal)/den;if(t<=0.||t>depth)return;
 vec3 u=normalize(cross(normal,abs(normal.y)>.9?vec3(1,0,0):vec3(0,1,0))),v=cross(normal,u);
 vec3 q=ro+rd*t-center;vec2 xy=vec2(dot(q,u),dot(q,v))/radius;float r=length(xy),a=atan(xy.y,xy.x)+angle;
 if(r>1.08)return;float aa=clamp(t/FxScreen.y/radius*.6,.0015,.008);
 float ring=band(r-1.,aa)+band(r-.93,aa)+band(r-.65,aa*.7);
 float ticks=pow(max(0.,cos(a*48.)),22.)*ease(.68,.73,r)*(1.-ease(.87,.91,r));
 float runes=band(r-.81,.025)*step(.4,hash(floor(a/TAU*32.)))*pow(max(0.,cos(a*32.)),5.);
 float spokes=pow(max(0.,cos(a*8.)),90.)*ease(.1,.2,r)*(1.-ease(.60,.65,r));
 col+=key*(ring*.7+ticks*.5+runes+spokes*.6)*power;
}
vec3 sky(int id){
 vec3 c=mix(vec3(.006,.012,.026),fill*.13,ease(-.3,.3,-rd.y));
 float neb=acFbm(rd*5.+vec3(0,tm*.008,7));c+=fill*pow(neb,3.)*.13;
 float horizon=exp(-abs(rd.y+.04)*12.);c+=fill*horizon*.10;
 vec3 cell=floor(rd*180.),f=fract(rd*180.)-.5;float star=step(.985,acHash(cell))*pow(max(0.,1.-length(f)*2.),14.);
 c+=vec3(.65,.80,1.)*star*.8;
 if(id==12)c=mix(c,vec3(.002,.028,.048),.65);
 return c;
}
void ground(int id){
 if(rd.y>=-.0001)return;float t=(-2.-ro.y)/rd.y;if(t<=0.)return;vec3 q=ro+rd*t;float n=acNoise(q*.5),fog=exp(-t*.008);
 vec3 base=id==10?vec3(.025,.047,.027):id==12?vec3(.012,.04,.055):vec3(.030,.035,.048);
 vec2 tile=abs(fract(q.xz/3.)-.5);float grid=step(.47,max(tile.x,tile.y));
 col=mix(col,base*(.7+n*.8)*(1.-grid*.2)+fill*.025,fog);depth=t;
}
void debris(float amount,float spread,vec3 color){
 for(int i=0;i<38;i++){
  float k=float(i),a=k*2.399,r=(5.+hash(k)*20.)*(1.+amount*spread),y=hash(k+21.)*9.+amount*(7.+hash(k+8.)*24.);
  vec3 q=vec3(cos(a)*r,y,sin(a)*r);float size=.17+hash(k+7.)*.6;
  box(q,vec3(size,size*.8,size)*max(.05,1.-ease(.98,1.11,p)),color,.08);
 }
}
void city(float destruction,float water){
 for(int i=0;i<44;i++){
  float k=float(i),x=(mod(k,8.)-3.5)*4.,z=(floor(k/8.)-2.5)*5.,h=2.+hash(k+3.)*8.;
  float fall=destruction*ease(.05,.95,hash(k+91.)+destruction*.4);
  vec3 q=vec3(x*(1.+fall*.55),h*.5-fall*(8.+hash(k)*12.)-water,z*(1.+fall*.55));
  box(q,vec3(1.2,h*.5,1.35),vec3(.13,.16,.21),.0);
  if(fall<.6)box(q+vec3(0,h*.5+.3,0),vec3(.6,.3,.7),vec3(.18,.20,.26),.03);
 }
}
void cataclysm(){
 float omen=ease(.04,.25,p),fracture=ease(.25,.64,p),fall=ease(.64,1.,p),after=ease(1.,1.12,p);
 city(fall,0.);debris(fall,.5,vec3(.19,.16,.22));
 // The black sun crosses the horizon; its crown breaks into falling celestial plates.
 sphere(vec3(0,23.,23.),7.,vec3(.028,.022,.05),.03);
 for(int i=0;i<12;i++){
  float k=float(i),a=k*TAU/12.,r=10.+fracture*5.,yy=23.+sin(a)*r-fall*(13.+hash(k)*12.);
  box(vec3(cos(a)*r,yy,20.+sin(a*.7)*3.),vec3(1.3,2.4,.5)*(1.-after*.8),vec3(.15,.12,.22),fracture*.22);
 }
 for(int i=0;i<7;i++){
  float k=float(i),a=-1.1+k*.37;vec3 last=vec3(0,24.,18.);
  for(int j=1;j<=6;j++){float u=float(j)/6.;vec3 q=vec3(sin(a)*u*36.,24.+cos(a)*u*21.,18.-u*8.+sin(float(j)*7.+k)*2.);
   beam(last,q,.035+fracture*.055,vec3(1.4,.48,.74),omen*fracture*(1.-after));last=q;}
 }
 float pulse=ease(.91,1.,p)*(1.-after);beam(vec3(0,-5.,0),vec3(0,42.,0),.35+pulse*1.5,vec3(1.7,.9,1.3),pulse*.8);
}
void life(){
 float awake=ease(.03,.24,p),drain=ease(.37,.82,p),collapse=ease(.82,1.08,p);
 vec3 bark=mix(vec3(.22,.14,.07),vec3(.10,.11,.13),drain);
 // Roots are solid and remain rooted in the scene; none originate at the player.
 branch(vec3(0,-2.,0),vec3(-.3,11.-collapse*3.,0),1.2,bark);
 branch(vec3(-.3,10.-collapse*3.,0),vec3(1.,21.-collapse*10.,.5),.70,bark);
 for(int k=0;k<9;k++){
  float a=float(k)*2.399;vec3 v=vec3(cos(a),0,sin(a));
  vec3 root=v*13.+vec3(0,-1.8,0),mid=v*5.+vec3(0,1.,0);
  branch(root,mid,.35,bark);branch(mid,vec3(0,3.,0),.55,bark);
  vec3 base=vec3(0,10.+float(k%3)*2.-collapse*4.,0),tip=v*(8.+float(k%3)*2.)+vec3(0,18.+float(k%4)*2.-collapse*(8.+hash(float(k))*5.),0);
  branch(base,tip,.33,bark);
  float crown=1.-ease(.35,.91,drain);
  if(crown>.02)box(tip+vec3(0,.5-collapse*5.,0),vec3(3.1,1.7,2.8)*crown,mix(vec3(.13,.32,.13),vec3(.26,.18,.07),drain),.04*awake);
  for(int n=0;n<3;n++){
   float f=float(n)/3.;vec3 q=mix(base,tip,.5+f*.5),side=vec3(-v.z,0,v.x)*(2.+f*2.);
   branch(q,q+side+vec3(0,2.,0),.12,bark);
   float leaf=1.-ease(.25+f*.13,.76+f*.13,drain);
   if(leaf>.015)box(q+side+vec3(0,2.2-collapse*8.,0),vec3(2.1,1.,1.8)*leaf,mix(vec3(.13,.34,.15),vec3(.29,.19,.07),drain),.05*awake);
  }
 }
 for(int k=0;k<20;k++){
  float a=float(k)*2.399,down=mod(tm*(.4+drain)+float(k)*1.37,21.);
  vec3 q=vec3(cos(a)*(5.+hash(float(k))*8.),21.-down,sin(a)*(5.+hash(float(k))*8.));
  box(q,vec3(.11,.025,.23),mix(vec3(.23,.52,.19),vec3(.18,.16,.13),drain),.07*(1.-drain));
 }
 float pulse=awake*(1.-drain);beam(vec3(0,1.,0),vec3(0,20.,0),.10,vec3(.35,1.5,.45),pulse*.35);
}
void cathedral(){
 float rise=ease(.06,.38,p),gates=ease(.4,.72,p),sentence=ease(.73,1.,p),collapse=ease(1.,1.12,p);
 vec3 stone=vec3(.075,.067,.11);float y=-12.*(1.-rise)-collapse*9.;
 // Nave, twin towers, pointed roof steps, flying buttresses and a genuinely opening portal.
 // A hollow nave, not a solid box behind the supposedly opening doors.
 box(vec3(-5.4,3.+y,5.),vec3(.7,5.,9.),stone,0.);
 box(vec3(5.4,3.+y,5.),vec3(.7,5.,9.),stone,0.);
 box(vec3(0,7.5+y,5.),vec3(5.4,.6,9.),stone,0.);
 box(vec3(0,-1.6+y,5.),vec3(5.4,.3,9.),stone,0.);
 box(vec3(0,3.+y,13.6),vec3(5.4,5.,.4),stone,0.);
 for(int k=0;k<4;k++)for(int side=-1;side<=1;side+=2){
  float z=-1.+float(k)*3.8;box(vec3(float(side)*3.8,2.5+y,z),vec3(.35,4.5,.35),stone*1.5,.02);
 }
 box(vec3(0,9.+y,-4.),vec3(5.4,1.5,.35),stone,0.);
 for(int k=0;k<5;k++)box(vec3(0,8.+float(k)*.8+y,5.),vec3(6.-float(k)*1.15,.45,9.5),stone,.025);
 for(int side=-1;side<=1;side+=2){
  float x=float(side)*7.;box(vec3(x,7.+y,-4.),vec3(2.,9.,2.3),stone,.02);
  for(int k=0;k<3;k++)box(vec3(x,5.+float(k)*4.+y,-6.34),vec3(.28,1.1,.025),vec3(.13,.065,.21),rise*.28);
  for(int k=0;k<5;k++)box(vec3(x,16.+float(k)*1.25+y,-4.),vec3(max(.15,2.1-float(k)*.42),.7,max(.15,2.3-float(k)*.46)),stone,.04);
  for(int k=0;k<4;k++){
   float z=-1.+float(k)*4.;box(vec3(float(side)*9.,3.+y,z),vec3(.7,5.,.7),stone,.0);
   branch(vec3(float(side)*9.,7.+y,z),vec3(float(side)*5.,10.+y,z),.32,stone*1.4);
  }
  // Door wings slide apart before the camera passes through the threshold.
  box(vec3(float(side)*(1.6+gates*2.4),3.+y,-4.15),vec3(1.45,4.,.24),vec3(.14,.10,.20),gates*.12);
 }
 // Bright interior is behind the door geometry, never painted over the solid facade.
 box(vec3(0,3.+y,12.9),vec3(3.,4.,.12),vec3(.025,.008,.045),gates*.9);
 glyph(vec3(0,9.3+y,-5.0),vec3(0,0,-1),1.2,tm*.025,rise*.85);
 beam(vec3(-3.,7.+y,-4.7),vec3(0,10.+y,-4.7),.025,key,rise*.55);
 beam(vec3(0,10.+y,-4.7),vec3(3.,7.+y,-4.7),.025,key,rise*.55);
 beam(vec3(0,1.+y,5),vec3(0,26.+y,5),.12+sentence*.5,vec3(.72,.27,1.5),sentence*.7);
}
float waterField(vec3 q,float front,float height){
 float crest=height+sin(q.x*.16+tm*.35)*2.1,overhang=sin(sat(q.y/crest)*PI)*3.;
 float face=front-overhang+sin(q.x*.24+q.y*.12)*.6;
 return max(face-q.z,max(q.y-crest,-3.-q.y));
}
void abyss(){
 float tide=ease(.25,.98,p),drown=ease(.72,1.05,p);city(drown*.55,drown*4.);
 // Advancing ocean wall is a continuous thick water volume with a rolling overhanging lip.
 float front=mix(58.,-40.,tide),height=4.+tide*19.;
 vec3 inv=1./(rd+vec3(.00001));vec3 a=(vec3(-50.,-4.,front-3.)-ro)*inv,b=(vec3(50.,height+5.,front+12.)-ro)*inv;
 vec3 lo=min(a,b),hi=max(a,b);float enter=max(.01,max(lo.x,max(lo.y,lo.z))),leave=min(hi.x,min(hi.y,hi.z));
 if(leave>enter&&waterField(ro,front,height)>.0){float t=enter;
  for(int i=0;i<80;i++){
   if(t>leave||t>depth)break;vec3 q=ro+rd*t;
   float field=waterField(q,front,height);
   if(field<.018){
    float crest=height+sin(q.x*.16+tm*.35)*2.1;
    float foam=ease(crest-1.2,crest,q.y),n=acNoise(q*.9+vec3(0,tm*.3,0));
    col=mix(vec3(.008,.055,.11),vec3(.025,.23,.30),.3+n*.5)+vec3(.32,.65,.69)*foam*.8;
    col+=vec3(.07,.30,.36)*pow(max(0.,sin(q.x*.6+q.y*1.4-tm)),22.)*.2;depth=t;break;
   }
   t+=max(.018,field*.40);
  }
 }
 if(drown>.05){
  float caustic=pow(max(0.,sin(rd.x*30.+tm)*sin(rd.y*24.-tm*.7)),10.);
  col=mix(col,vec3(.003,.032,.065)+vec3(.03,.16,.19)*caustic,drown*.58);
  col+=vec3(.025,.075,.09)*pow(max(0.,cos(rd.x*11.+rd.y*4.+tm*.09)),16.)*drown;
  for(int k=0;k<13;k++){float f=float(k),a=f*2.399;vec3 q=vec3(cos(a)*6.,mod(f*1.7+tm*1.1,22.)-3.,sin(a)*7.-8.);sphere(q,.05+hash(f)*.07,vec3(.1,.28,.34),.0);}
 }
}
void sunder(){
 float crack=ease(.14,.57,p),split=ease(.48,.94,p),after=ease(.96,1.12,p);
 // A suspended, finite cross-section of a continent: topsoil, crust and luminous exposed mantle.
 for(int i=0;i<56;i++){
  float k=float(i),x=(mod(k,8.)-3.5)*4.,z=(floor(k/8.)-3.)*4.;
  float fault=x+.30*z+sin(z*.3)*1.5,side=sign(fault),separate=side*split*(3.+abs(z)*.15);
  float yy=1.+hash(k+13.)*1.6-abs(fault)*.025-after*(hash(k)*7.);
  vec3 q=vec3(x+separate,yy,z);float width=abs(fault)<2.8?1.6*(1.-crack*.65):1.85;
  box(q-vec3(0,3.,0),vec3(width,3.,1.85),vec3(.16,.12,.10),abs(fault)<5.?crack*.35:.02);
  box(q+vec3(0,.35,0),vec3(width,.40,1.85),vec3(.16,.23,.17),.0);
  if(hash(k+9.)>.66&&abs(fault)>5.)box(q+vec3(0,1.3,0),vec3(.55,.7,.6),vec3(.20,.23,.26),0.);
 }
 sphere(vec3(0,-8.,0),3.5+split*1.5,vec3(.7,.12,.015),.5*crack);
 for(int k=0;k<16;k++){
  float z=-16.+float(k)*2.,x=-.30*z-sin(z*.3)*1.5;
  beam(vec3(x-split*3.,.4,z),vec3(x-split*3.-.6,.4,z+2.),.06,vec3(1.7,.5,.08),crack);
  beam(vec3(x+split*3.,.4,z),vec3(x+split*3.-.6,.4,z+2.),.06,vec3(1.7,.5,.08),crack);
 }
}
void apocalypse(){
 float law=ease(.10,.48,p),cage=ease(.4,.73,p),erase=ease(.69,1.07,p);
 // Reality is removed row by row, leaving only the glyph court suspended in darkness.
 for(int i=0;i<54;i++){
  float k=float(i),x=(mod(k,9.)-4.)*3.2,z=(floor(k/9.)-2.5)*4.;
  float threshold=(z+12.)/24.,gone=ease(threshold*.8,threshold*.8+.20,erase);
  float h=1.+hash(k+33.)*6.;
  if(gone<.995){
   vec3 q=vec3(x,h*.5+gone*12.,z);vec3 sz=vec3(1.35,h*.5,1.65)*(1.-gone);
   box(q,sz,vec3(.15,.13,.19),gone*.5);
  }
 }
 for(int i=0;i<8;i++){
  float a=float(i)*TAU/8.+tm*.015;vec3 at=vec3(cos(a)*19.,5.+law*8.,sin(a)*19.);
  box(at,vec3(1.1,4.5,.45),vec3(.12,.09,.16),.05*law);
  glyph(at+vec3(0,0,-.5),normalize(vec3(cos(a),0,sin(a))),3.4,tm*.06,law*.65);
 }
 glyph(vec3(0,25.,0),vec3(0,1,0),23.,tm*.035,law*.75);
 glyph(vec3(0,-1.5,0),vec3(0,1,0),23.,-tm*.035,cage*.75);
 for(int k=0;k<8;k++){
  float a=float(k)*TAU/8.;vec3 v=vec3(cos(a)*23.,0,sin(a)*23.);
  beam(v-vec3(0,1.,0),v+vec3(0,25.,0),.025,vec3(1.2,.35,.52),cage*.6*(1.-ease(1.02,1.12,p)));
 }
 // The erasure front is a thin travelling plane, not a full-screen flash.
 glyph(vec3(0,2.+erase*22.,0),vec3(0,1,0),16.,-tm*.08,erase*.75);
}
vec3 story(vec2 uv,int id){
 p=FxParams.y;tm=FxTime;
 key=id==9?vec3(1.6,.5,.8):id==10?vec3(.40,1.1,.45):id==11?vec3(.85,.40,1.5):id==12?vec3(.12,.65,.85):id==13?vec3(1.8,.65,.12):vec3(1.5,.48,.68);
 fill=id==9?vec3(.30,.20,.5):id==10?vec3(.16,.35,.20):id==11?vec3(.29,.22,.50):id==12?vec3(.10,.32,.45):id==13?vec3(.40,.25,.20):vec3(.35,.18,.30);
 float reveal=ease(.03,.23,p),orbit=ease(.23,.63,p),finale=ease(.66,1.,p),after=ease(1.,1.12,p);
 // Distinct dolly/orbit/threshold/orbital camera scripts, continuous at every chapter boundary.
 if(id==9)camera(vec3(-16.+orbit*19.,5.+reveal*9.+finale*8.,-36.-finale*12.),vec3(0,7.+reveal*4.,3.),uv,1.05);
 if(id==10){float a=-.28+orbit*.80;camera(vec3(sin(a)*46.,8.+reveal*6.+finale*3.,-cos(a)*46.),vec3(0,11.-after*4.,0),uv,1.02);}
 if(id==11)camera(vec3(-12.*(1.-orbit),5.+reveal*3.-finale*4.,-38.+finale*41.-after*10.),vec3(0,8.+reveal*2.-finale*6.,2.+finale*9.),uv,1.12);
 if(id==12)camera(vec3(-17.+orbit*20.,5.+reveal*6.-finale*10.,-33.+finale*8.),vec3(0,4.+reveal*4.-finale*4.,9.),uv,1.0);
 if(id==13){float a=-.42+orbit*.65;camera(vec3(sin(a)*43.,13.+reveal*15.+finale*5.,-cos(a)*43.),vec3(0,-1.,0),uv,1.14);}
 if(id==14)camera(vec3(-17.+orbit*22.,8.+reveal*10.+finale*7.,-39.-finale*7.),vec3(0,12.+finale*2.,0),uv,1.02);
 depth=100000.;col=sky(id);
 // Orbital continental section has no infinite floor; all other films have their own environment.
 if(id!=13)ground(id);
 if(id==9)cataclysm();else if(id==10)life();else if(id==11)cathedral();else if(id==12)abyss();else if(id==13)sunder();else apocalypse();
 float haze=exp(-abs(rd.y)*18.);col+=fill*haze*.035;
 // Fade to an intact view of the real world, not a fake disconnect or a respawn overlay.
 return pow(film(col*1.35),vec3(.82));
}
void main(){
 vec2 uv=(texCoord-.5)*vec2(FxScreen.x/FxScreen.y,1.);uv.y=-uv.y;
 vec3 world=texture(Sampler0,vec2(texCoord.x,1.-texCoord.y)).rgb;
 int id=int(FxParams.x+.5);vec3 scene=world;
 if(FxParams.z>.001)scene=mix(world,story(uv,id),FxParams.z);
 // Respect the existing reduced-flash switch; no rapid full-screen strobe.
 float impact=FxParams.w*exp(-max(0.,FxImpact.x)/9.)*step(0.,FxImpact.x);
 scene+=FxColor2.rgb*impact*.055;
 scene*=1.-.13*ease(.35,1.15,length(uv));
 fragColor=vec4(scene,1.)*ColorModulator;
}
