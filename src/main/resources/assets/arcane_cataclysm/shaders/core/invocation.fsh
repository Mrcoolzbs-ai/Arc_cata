#version 150
#moj_import <arcane_cataclysm:ac_common.glsl>
// Nine authored invocation tableaux. Analytic 3-D intersections, not a pre-rendered movie.
// Camera and choreography are driven only by cast age; no world loads, entities or terrain edits.
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec4 FxParams; // catalogue index, age / impact, theatre mix, flash allowed
uniform vec4 FxColor2;
uniform vec4 FxImpact; // after ticks, bounded power, glitch envelope, shake envelope
uniform float FxTime;
uniform vec2 FxScreen;
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
const float PI=3.14159265359;
const float TAU=6.28318530718;
vec3 ro=vec3(0),rd=vec3(0,0,1),col=vec3(0),primary=vec3(1),accent=vec3(1);
float phase=0.,tm=0.,opaqueDepth=100000.;
mat2 rotation(float a){return mat2(cos(a),-sin(a),sin(a),cos(a));}
float sat(float x){return clamp(x,0.,1.);}
float ease(float a,float b,float x){return smoothstep(a,b,x);}
float band(float x,float w){return exp(-min(70.,x*x/(w*w)));}
float hash1(float x){return fract(sin(x*127.13+17.7)*43758.5453);}
vec3 film(vec3 x){return clamp((x*(2.51*x+.03))/(x*(2.43*x+.59)+.14),0.,1.);}
float line2(vec2 p,vec2 a,vec2 b){vec2 ab=b-a;return length(p-a-ab*sat(dot(p-a,ab)/max(.00001,dot(ab,ab))));}
// Ray/segment distance. The second result is distance along the view ray (positive only).
vec2 raySegment(vec3 a,vec3 b){
 vec3 v=b-a,w=ro-a;float vv=dot(v,v),rv=dot(rd,v),rw=dot(rd,w),vw=dot(v,w);
 float u=clamp((vw-rv*rw)/max(.00001,vv-rv*rv),0.,1.);
 float d=max(.0,dot(a+u*v-ro,rd));return vec2(length(ro+rd*d-(a+u*v)),d);
}
void beam(vec3 a,vec3 b,float width,vec3 color,float power){
 vec2 d=raySegment(a,b);if(d.y<=.001||d.y>opaqueDepth)return;
 float core=band(d.x,width),halo=band(d.x,width*5.);
 col+=color*(core*1.1+halo*.14)*power;
}
void spark(vec3 p,float size,vec3 color,float power){
 float d=dot(p-ro,rd);if(d<0.||d>opaqueDepth)return;vec3 at=ro+rd*d-p;
 float r=length(at);col+=color*(band(r,size)+band(r,size*5.)*.13)*power;
}
// Geometrically correct circles in arbitrary planes, with concentric engraving and glyph band.
void seal(vec3 center,vec3 normal,float radius,float spin,float power,float style){
 float denom=dot(rd,normal);if(abs(denom)<.015)return;float t=dot(center-ro,normal)/denom;
 if(t<0.||t>opaqueDepth)return;
 vec3 tangent=normalize(cross(normal,abs(normal.y)>.95?vec3(1,0,0):vec3(0,1,0))),bitangent=cross(normal,tangent);
 vec3 hit=ro+rd*t-center;vec2 p=vec2(dot(hit,tangent),dot(hit,bitangent))/radius;
 float r=length(p);if(r>1.09)return;float a=atan(p.y,p.x)+spin;
 float aa=clamp(t/FxScreen.y/radius*.8,.0015,.012);
 float stroke=band(r-1.,aa)+band(r-.965,aa*.7)+band(r-.78,aa*.7)+band(r-.54,aa*.6);
 float ticks=pow(max(0.,cos(a*64.)),26.)*ease(.82,.84,r)*(1.-ease(.93,.95,r));
 float glyph=step(.55,fract(sin(floor(a/TAU*36.)*31.7+style)*437.3));
 float gr=band(r-.87,.024)*pow(max(0.,cos(a*36.)),5.)*(.4+glyph*.6);
 float polygon=100.;int pts=style<2.?6:8;
 for(int i=0;i<8;i++){if(i>=pts)break;float a0=TAU*float(i)/float(pts)-spin*.65,a1=TAU*float(i+2)/float(pts)-spin*.65;
  polygon=min(polygon,line2(p,vec2(cos(a0),sin(a0))*.74,vec2(cos(a1),sin(a1))*.74));}
 float reveal=ease(.03,.22,phase);
 col+=mix(primary,accent,.28+.28*sin(a*3.))*(stroke*.9+ticks*.6+gr+band(polygon,aa*.8)*.72)*power*reveal;
 col+=primary*band(r-1.,.025)*power*.10;
}
float sphereHit(vec3 center,float radius){
 vec3 oc=ro-center;float b=dot(oc,rd),h=b*b-dot(oc,oc)+radius*radius;
 return h>0.?-b-sqrt(h):-1.;
}
// Opaque faceted hexagonal ice spire, with a prism-to-pyramid shoulder.
void crystal(vec3 base,float height,float width,float spin,float reveal){
 if(reveal<.01)return;float h=height*reveal;
 vec3 o=ro-base,d=rd;float nearT=-10000.,farT=10000.;vec3 nHit=vec3(0,1,0);
 for(int i=0;i<14;i++){
  vec3 n;float limit;
  if(i<6){float a=TAU*float(i)/6.+spin;n=vec3(cos(a),0,sin(a));limit=width;}
  else if(i<12){float a=TAU*float(i-6)/6.+spin;n=normalize(vec3(cos(a),width/max(.01,h*.45),sin(a)));limit=width/length(vec3(1.,width/max(.01,h*.45),0.))+n.y*h*.55;}
  else {n=vec3(0,i==12?1.:-1.,0);limit=i==12?h:0.;}
  float den=dot(d,n),dis=limit-dot(o,n);
  if(abs(den)<.0001){if(dis<0.)return;}else{float t=dis/den;if(den<0.){if(t>nearT){nearT=t;nHit=n;}}else farT=min(farT,t);}
 }
 if(nearT<0.||nearT>farT||nearT>opaqueDepth)return;
 opaqueDepth=nearT;vec3 hit=ro+rd*nearT-base;
 float light=.28+.72*max(0.,dot(nHit,normalize(vec3(-.6,.8,-.5))));
 float inner=pow(1.-abs(dot(rd,nHit)),3.);
 float vein=pow(.5+.5*sin(hit.y*5.+hit.x*3.+hit.z*4.),45.);
 float inclusions=acNoise(hit*2.4)*acNoise(hit*.8);
 light*=.82+.3*inclusions;
 col=mix(vec3(.012,.035,.10),vec3(.18,.52,.72),light)*(.7+inner*.7)+accent*vein*.18;
 col+=vec3(.16,.65,1.2)*band(length(hit.xz),width*.3)*.35;
 col+=vec3(.3,.85,1.4)*pow(light,12.)*.8;
}
// Flat forged sword intersects a rotating 3-D plane. Central ridge/metal edge is shaded, not a line icon.
void sword(vec3 tip,float len,float yaw,float roll,float power,bool holy){
 vec3 up=vec3(sin(roll),cos(roll),0),side=vec3(cos(roll)*cos(yaw),-sin(roll)*cos(yaw),sin(yaw));
 vec3 normal=normalize(cross(side,up));float den=dot(rd,normal);if(abs(den)<.02)return;
 float t=dot(tip-ro,normal)/den;if(t<0.||t>opaqueDepth)return;
 vec3 h=ro+rd*t-tip;vec2 p=vec2(dot(h,side),dot(h,up))/len;
 if(p.y<-.04||p.y>1.24||abs(p.x)>.27)return;
 float width=holy?(p.y<.28?min(p.y*.29,(.30-p.y)*.34):.013):min(p.y*.20,.043)*(1.-ease(.74,.84,p.y)*.3);
 float blade=max(abs(p.x)-width,max(-p.y,p.y-.85));
 float guard=max(abs(p.x)-(.23-.10*abs(p.x)),abs(p.y-.88)-.017);
 float handle=max(abs(p.x)-.017,max(.88-p.y,p.y-1.12));
 float diamond=abs(p.x)+abs(p.y-1.16)*.65-.042;
 float dist=min(min(blade,guard),min(handle,diamond));
 float edge=band(dist,.004),a=1.-smoothstep(-.001,.0015,dist);
 if(a>.01){
  vec3 material=mix(accent*.18,vec3(.68,.91,1.05),.50+.37*sign(p.x));
  if((holy&&p.y>.28)||p.y>.85)material=mix(vec3(.38,.17,.035),vec3(1.45,1.08,.43),.6+.4*cos(p.x*70.));
  float ridge=band(p.x,.005)*ease(.05,.15,p.y)*(1.-ease(.80,.84,p.y));
  material+=primary*(ridge*1.3+edge*1.3);
  col=mix(col,material*power,a);opaqueDepth=t;
 }
 col+=primary*band(max(dist,0.),.018)*power*.12;
}
vec3 background(vec3 dir,int id){
 float haze=acNoise(dir*3.+vec3(0,tm*.025,0));
 vec3 c=vec3(.006,.011,.028)+accent*.018;
 float galaxy=exp(-pow(dot(dir,normalize(vec3(.15,1.,.38)))+.08,2.)*32.);
 float nebula=acFbm(dir*6.+vec3(17.,3.,tm*.008));
 c+=mix(vec3(.045,.08,.18),accent*.22,nebula)*galaxy*pow(nebula,1.7)*2.4;
 c+=primary*.035*pow(haze,3.);
 float bandN=acNoise(dir*7.+vec3(10,tm*.012,3));
 c+=mix(primary*.055,accent*.08,bandN)*pow(max(0.,1.-abs(dir.y+.06)*1.8),6.)*haze;
 // Individual stars and drifting motes use direction-stable cells.
 vec3 grid=dir*210.;vec3 cell=floor(grid),f=fract(grid)-.5;
 float star=pow(max(0.,1.-length(f)*2.),12.)*step(.977,acHash(cell));
 c+=mix(vec3(.35,.62,1.),vec3(1.,.78,.4),acHash(cell+7.))*star*3.;
 float lowCloud=pow(acNoise(vec3(dir.x*5.,dir.y*11.+tm*.03,dir.z*5.)),3.);
 c+=accent*lowCloud*.10*(1.-ease(-.35,.15,dir.y));return c;
}
void floorScene(){
 if(rd.y>=-.0001)return;float t=-ro.y/rd.y;if(t<0.)return;
 vec3 p=ro+rd*t;float r=length(p.xz),fog=exp(-t*.012);
 float n=acNoise(p*.23+vec3(0,tm*.07,0));
 col=mix(col,vec3(.006,.014,.03)+accent*n*.025,fog*.92);
 if(r<16.){
  float stone=acNoise(p*1.8)*.035+acNoise(p*.23)*.025;
  float cut=pow(max(0.,cos(r*5.)),40.);
  vec3 material=vec3(.018,.035,.065)+accent*(stone+.022)+primary*cut*.045;
  float rim=band(r-15.6,.09)+band(r-14.8,.04);
  col=material+primary*rim*.32;opaqueDepth=t;
 }
 float reflectGlow=exp(-length(p.xz)*.10);
 col+=primary*reflectGlow*.10;
 seal(vec3(0,.03,0),vec3(0,1,0),11.+ease(.02,.25,phase)*5.,tm*.045,.75,0.);
 seal(vec3(0,.05,0),vec3(0,1,0),7.2,-tm*.075,.35,2.);
 // A thin distant fog shelf gives the object a grounded scale cue.
 // Horizon haze is applied continuously after the stage, on both sides of the horizon.
}
void particles(int id){
 for(int i=0;i<30;i++){
  float fi=float(i),h=hash1(fi+7.);float a=fi*2.399+tm*.04;
  float y=mod(fi*3.17+tm*(id==2?-1.2:.7),27.)+.2;
  float r=7.+h*13.;vec3 p=vec3(cos(a)*r,y,sin(a)*r);
  spark(p,.018+h*.028,mix(primary,vec3(1),.5),.35+hash1(fi*7.)*.8);
 }
}
void aureole(vec3 center,float radius,float spin,float power){
 float den=rd.y;if(abs(den)<.001)return;float t=(center.y-ro.y)/den;
 if(t<0.||t>opaqueDepth)return;vec2 p=(ro+rd*t-center).xz/radius;
 float r=length(p),a=atan(p.y,p.x)+spin;
 float ringWidth=.058+.018*pow(max(0.,cos(a*12.)),8.);
 if(abs(r-1.)<ringWidth){
  float bevel=ease(0.,.012,ringWidth-abs(r-1.));
  float etch=band(r-.986,.002)*(.4+.6*step(.8,fract(a/TAU*72.)));
  vec3 material=mix(accent*.035,primary*.16,.35+.3*cos(a-1.));
  material+=primary*(pow(1.-bevel,3.)*.7+etch*.6);
  col=mix(col,material,power);opaqueDepth=t;
 }
}
void lance(){
 float open=ease(.10,.52,phase),release=ease(.73,.98,phase);
 vec3 spear=vec3(0,mix(7.5,-4.,release),0);float len=18.;
 aureole(vec3(0,16.,0.),10.5,tm*.025,open);
 // A three-tier heavenly gate, opened in succession, with petals / wings of light.
 for(int k=0;k<3;k++){
  float i=float(k),grow=ease(.11+i*.085,.28+i*.085,phase);
  vec3 p=vec3(0,5.+i*5.,0);float r=(8.-i*1.1)*grow;
  if(r>.05){seal(p,vec3(0,1,0),r,tm*(k%2==0?.08:-.11),1.25,1.);
   for(int j=0;j<8;j++){float a=float(j)*TAU/8.+tm*.035;vec3 v=vec3(cos(a),0,sin(a));
    beam(p+v*r,p+v*(r+1.7)+vec3(0,1.5,0),.035,primary,.75);}}
 }
 // Raised gothic window silhouettes around the processional aisle.
 for(int k=0;k<8;k++){
  float a=TAU*float(k)/8.;vec3 p=vec3(cos(a)*12.,0,sin(a)*12.);
  float h=(11.+float(k%2)*4.)*open;
  beam(p,p+vec3(0,h,0),.035,accent,.5);
  beam(p+vec3(0,h,0),p*.80+vec3(0,h+2.,0),.05,primary,.7);
 }
 // Rays lead the eye toward the central shaft; silhouette is deliberately not a generic pillar.
 sword(spear,len,tm*.045,.0,1.,true);
 for(int j=0;j<5;j++){
  float i=float(j),y=20.5-i*.8;
  beam(vec3(-.4,y,0),vec3(-4.5-i*.65,y+2.0-i*.6,0),.09,primary,open*.8);
  beam(vec3(.4,y,0),vec3(4.5+i*.65,y+2.0-i*.6,0),.09,primary,open*.8);
 }
 beam(spear+vec3(0,3,0),spear+vec3(0,20,0),.07,vec3(1.8,1.3,.55),.8);
 spark(spear+vec3(0,16.1,0),.22,vec3(1.6,1.05,.4),2.0);
}
void blades(){
 float formation=ease(.12,.60,phase),launch=ease(.78,1.,phase);
 // Four independently counter-rotating tiers; each actual sword has tip, bevel, guard and pommel.
 for(int tier=0;tier<3;tier++){
  float k=float(tier),radius=6.5+k*3.;float rise=ease(.12+k*.1,.35+k*.1,phase);
  float y=6.+k*5.;seal(vec3(0,y+6.,0),vec3(0,1,0),radius+1.,tm*(tier%2==0?.09:-.09),rise*.65,2.);
  for(int j=0;j<16;j++){
   float i=float(j),a=i*TAU/16.+tm*(tier%2==0?.12:-.09),drop=ease(.76+hash1(i+k*37.)*.08,1.0,phase);
   vec3 p=vec3(cos(a)*radius,mix(2.,y,rise)*(1.-drop),sin(a)*radius);
   float len=3.5+k*.45;
   sword(p,len,a+PI*.5,sin(a)*.10,1.,false);
   if(launch>0.)beam(p+vec3(0,len*1.1,0),p+vec3(0,len+launch*12.,0),.04,primary,launch*.8);
  }
 }
 seal(vec3(0,12,0),vec3(0,1,0),4.5,-tm*.1,.55,2.);
 spark(vec3(0,12,0),.26,vec3(.6,1.6,1.9),2.);
}
void frost(){
 float grow=ease(.10,.60,phase),close=ease(.73,.98,phase);
 // Six grave-spires and a levitating faceted casket. The crown grows branch by branch.
 for(int k=0;k<6;k++){
  float a=float(k)*TAU/6.+.16;float r=8.3;
  crystal(vec3(cos(a)*r,0,sin(a)*r),8.+float(k%2)*3.,1.0,a,ease(.12+float(k)*.035,.40+float(k)*.035,phase));
 }
 crystal(vec3(0,5.-close*3.,0),8.0,2.1,tm*.075,grow);
 seal(vec3(0,14.,0),vec3(0,1,0),9.,-tm*.055,.65,1.);
 // Frozen Yggdrasil canopy: bright structural branches, secondary frost forks, crystal tips.
 beam(vec3(0,1,0),vec3(0,16.*grow,0),.15,accent,.6);
 for(int k=0;k<6;k++){
  float a=float(k)*TAU/6.;vec3 v=vec3(cos(a),0,sin(a));
  vec3 root=vec3(0,10.,0),tip=root+v*(8.5*grow)+vec3(0,7.*grow,0);
  beam(root,tip,.09,primary,.8*grow);
  for(int j=1;j<4;j++){
   float f=float(j)/4.;vec3 mid=mix(root,tip,f);
   vec3 side=vec3(-v.z,0,v.x);
   beam(mid,mid+side*2.0*grow+v*1.1+vec3(0,2.*grow,0),.035,primary,.7);
   beam(mid,mid-side*2.0*grow+v*1.1+vec3(0,2.*grow,0),.035,primary,.7);
  }
  crystal(tip-vec3(0,1,0),2.5,.35,a,grow);
 }
 float snow=pow(acNoise(rd*110.+vec3(tm*.7,-tm*2.,0)),28.);
 col+=vec3(.6,.9,1.3)*snow*3.;
}
void storm(){
 float build=ease(.08,.55,phase),release=ease(.81,1.,phase);
 // A hollow rotating supercell: concentric helical volumes, never a flat cloud plane.
 vec3 inv=1./(rd+vec3(.00001));
 vec3 b0=(vec3(-18.,0.,-18.)-ro)*inv,b1=(vec3(18.,30.,18.)-ro)*inv;
 vec3 lo=min(b0,b1),hi=max(b0,b1);
 float enter=max(0.,max(lo.x,max(lo.y,lo.z))),leave=min(hi.x,min(hi.y,hi.z));
 if(leave>enter){
  float stride=(leave-enter)/28.,trans=1.;vec3 mist=vec3(0);
  for(int i=0;i<28;i++){
   vec3 q=ro+rd*(enter+(float(i)+.5)*stride);float y=q.y/28.,r=length(q.xz),a=atan(q.z,q.x);
   float radius=1.6+11.*y*y;
   float spiral=sin(a*3.-y*13.+tm*1.35+r*.3);
   float n=acNoise(q*.64+vec3(tm*.12,0,tm*.08));
   float shell=band(r-radius,1.1+y*2.2)*ease(0.,.15,y)*(1.-ease(.88,1.1,y));
   float density=shell*(.25+.75*ease(-.6,.5,spiral+n*.8))*build;
   float alpha=1.-exp(-density*stride*.70);
   vec3 lit=mix(vec3(.012,.022,.045),vec3(.11,.22,.31),n);
   lit+=vec3(.18,.52,.80)*pow(max(0.,spiral),12.)*.2;
   mist+=lit*alpha*trans;trans*=1.-alpha;
  }col=col*trans+mist;
 }
 for(int k=0;k<7;k++){
  float a=float(k)*TAU/7.;vec3 p=vec3(cos(a)*12.5,0,sin(a)*12.5);
  crystal(p,3.+float(k%3),.65,a,build);
 }
 // Lightning crawls around the funnel before one branching stroke reaches the ground.
 for(int k=0;k<5;k++){
  float y=6.+float(k)*4.,a=float(k)*2.399-tm*.6;
  vec3 last=vec3(cos(a)*(2.+y*.23),y,sin(a)*(2.+y*.23));
  float pulse=.18+.82*pow(.5+.5*sin(tm*1.7+float(k)*2.3),8.);
  for(int j=1;j<=8;j++){
   float u=float(j)/8.,b=a+u*1.7,yy=y-u*3.;
   vec3 next=vec3(cos(b)*(2.+yy*.23),yy,sin(b)*(2.+yy*.23));
   next+=vec3(sin(float(j)*31.),cos(float(j)*13.),sin(float(j)*17.))*.32;
   beam(last,next,.035,vec3(.7,1.4,2.),pulse*build);last=next;
  }
 }
 if(release>.001){vec3 last=vec3(0,23.,0);
  for(int j=1;j<=14;j++){
   float u=float(j)/14.;vec3 next=vec3(sin(float(j)*17.)*1.3*(1.-u),23.*(1.-u),cos(float(j)*11.)*1.3*(1.-u));
   beam(last,next,.045+release*.07,vec3(1.1,1.6,2.),release*1.15);
   if(j==6||j==9)beam(next,next+vec3(3.,-3.,-1.),.025,accent,release*.7);last=next;
  }
 }
}
void starfall(){
 float summon=ease(.08,.40,phase),fall=ease(.64,1.,phase);
 vec3 center=mix(vec3(-3.,15.,0.),vec3(3.,3.,1.),fall);
 float radius=3.1*summon+.08;
 float t=sphereHit(center,radius);
 // Deep blue atmosphere / distant crescent provides scale independent of the meteor.
 float planet=sphereHit(vec3(16.,13.,35.),12.);
 if(planet>0.){
  vec3 normal=normalize(ro+rd*planet-vec3(16.,13.,35.));
  float lighting=max(0.,dot(normal,normalize(vec3(-1,.3,-.4))));
  float clouds=acNoise(normal*18.);
  col=mix(col,vec3(.025,.09,.24)*(.12+lighting)+vec3(.10,.28,.48)*pow(clouds,3.)*lighting,.9);
  col+=vec3(.12,.35,.8)*pow(1.-abs(dot(normal,rd)),5.)*.45;
 }
 if(t>0.){
  vec3 normal=normalize(ro+rd*t-center),q=normal*4.;q.xz=rotation(tm*.16)*q.xz;
  float n=acFbm(q*2.),cracks=pow(max(0.,1.-abs(n-.51)*12.),4.);
  col=mix(vec3(.055,.012,.018),vec3(.22,.055,.02),n)+vec3(2.5,.58,.045)*cracks;
  col+=vec3(1.0,.18,.025)*pow(1.-abs(dot(normal,rd)),3.);
  opaqueDepth=t;
 }
 float d=dot(center-ro,rd);float r=length(ro+rd*max(0.,d)-center);
 if(d>0.)col+=vec3(1.5,.25,.025)*band(r-radius,radius*.13)*summon;
 vec3 tail=normalize(vec3(-.7,1.,-.15));
 for(int i=0;i<9;i++){
  float k=float(i),a=k*2.399+tm*.2;vec3 side=vec3(cos(a),sin(a)*.35,sin(a))*radius*.7;
  beam(center+side,center+side+tail*(9.+float(i%3)*4.+fall*14.),.12+.025*k,mix(vec3(1.8,.28,.035),vec3(1.,.7,.15),k/10.),summon*.4);
 }
 seal(center+vec3(0,-4.5,0),vec3(0,1,0),6.8,-tm*.10,.55,0.);
 for(int i=0;i<6;i++){
  float k=float(i),a=k*TAU/6.+tm*.20;vec3 p=center+vec3(cos(a)*7.,sin(a)*3.,sin(a)*7.);
  spark(p,.15,mix(primary,accent,.5),.8*summon);
 }
}
// Authored counterparts of world-space spells 6–9.
void rock(vec3 center,vec3 size,float hot){
 vec3 inv=1./(rd+vec3(.000001)),a=(center-size-ro)*inv,b=(center+size-ro)*inv;
 vec3 lo=min(a,b),hi=max(a,b);float t=max(lo.x,max(lo.y,lo.z)),end=min(hi.x,min(hi.y,hi.z));
 if(t<0.||t>end||t>opaqueDepth)return;vec3 q=ro+rd*t-center;
 vec3 normal=abs(q/size);float light=normal.y>max(normal.x,normal.z)?.8:normal.x>normal.z?.35:.18;
 float n=acNoise((q+center)*1.8);col=vec3(.08,.07,.07)*light*(.7+n*.5)+primary*hot*(pow(1.-abs(n-.5)*2.,28.)*.8+.025);
 opaqueDepth=t;
}
void rift(){
 float open=ease(.14,.92,phase),rise=ease(.12,.55,phase);
 for(int k=-7;k<=7;k++)for(int side=-1;side<=1;side+=2){
  float z=float(k)*2.5,gap=1.+open*3.8+sin(z*.6)*.5;
  rock(vec3(float(side)*(gap+2.0),-.8+sin(float(k)*2.3)*rise,z),vec3(2.,1.5+rise*float((k+9)%3)*.65,1.1),.13);
 }
 for(int k=-12;k<12;k++){
  float z=float(k)*1.5;vec3 a=vec3(sin(z*1.2)*.55,.15,z),b=vec3(sin((z+1.5)*1.2)*.55,.15,z+1.5);
  beam(a,b,.09+open*.20,vec3(2.,.35,.03),.8);
  if(k%3==0)beam(a,vec3(float(k%2==0?1:-1)*(4.+open*5.),.2,z+2.),.045,primary,open*.5);
 }
 seal(vec3(0,11.,0),vec3(0,1,0),9.5,-tm*.05,.42*rise,2.);
 for(int k=0;k<10;k++){float a=float(k)*2.399;rock(vec3(cos(a)*10.,5.+sin(tm*.5+float(k))*2.,sin(a)*10.),vec3(.65,1.1,.7)*rise,.15);}
}
float lavaField(vec3 q,float grow,float surge){
 float r=length(q.xz),an=atan(q.z,q.x),crest=3.+grow*(5.+sin(an*3.-tm*.8)*2.)+surge*3.;
 float shell=abs(r-(5.+surge*7.+pow(q.y/crest,2.)*3.))-.7;
 return max(shell,max(q.y-crest,-q.y));
}
void inferno(){
 float grow=ease(.08,.55,phase),surge=ease(.65,1.,phase);
 // Three fluid crests with thickness and a dark molten face, sampled only inside a bounded volume.
 vec3 inv=1./(rd+vec3(.00001));vec3 a=(vec3(-18.,0.,-18.)-ro)*inv,b=(vec3(18.,17.,18.)-ro)*inv;
 vec3 lo=min(a,b),hi=max(a,b);float enter=max(0.,max(lo.x,max(lo.y,lo.z))),leave=min(hi.x,min(hi.y,hi.z));
 if(leave>enter){float t=enter;
  for(int i=0;i<112;i++){
   if(t>leave||t>opaqueDepth)break;
   vec3 q=ro+rd*t;float r=length(q.xz),an=atan(q.z,q.x);
   float field=lavaField(q,grow,surge);
   if(field<.018){
    // Conservative distance tracing keeps the thin crest continuous at grazing angles.
    r=length(q.xz);an=atan(q.z,q.x);
    float crest=3.+grow*(5.+sin(an*3.-tm*.8)*2.)+surge*3.;
    float n=acFbm(q*.7+vec3(0,-tm,0)),lip=ease(crest-1.0,crest,q.y);
    col=mix(vec3(.11,.018,.012),vec3(1.6,.21,.012),pow(n,3.))+vec3(1.4,.45,.035)*lip;
    opaqueDepth=t;break;
   }
   t+=max(.018,field*.40);
  }
 }
 for(int i=0;i<16;i++){float k=float(i),an=k*2.399;vec3 p=vec3(cos(an)*(6.+surge*7.),mod(k*1.7+tm*2.,16.),sin(an)*(6.+surge*7.));spark(p,.07,vec3(1.5,.5,.04),grow);}
 seal(vec3(0,.1,0),vec3(0,1,0),15.,tm*.03,.4,2.);
}
void singularity(){
 float build=ease(.08,.55,phase),collapse=ease(.76,1.,phase);vec3 center=vec3(0,10.,0);float radius=(2.+build*2.)*(1.-collapse*.25);
 vec3 oc=ro-center;float t=sphereHit(center,radius);float along=dot(center-ro,rd),dist=length(ro+rd*max(0.,along)-center);
 if(along>0.){
  col+=vec3(.36,.18,1.2)*band(dist-radius,radius*.18)*.7;
  col+=vec3(.25,.68,1.5)*band(dist-radius*1.23,radius*.035)*1.2;
 }
 if(t>0.&&t<opaqueDepth){col=vec3(.0008,.0003,.003);opaqueDepth=t;}
 // Tilted accretion ribbons, backed by orbiting stone pulled into the event horizon.
 vec3 normal=normalize(vec3(.18,1.,.38));float den=dot(rd,normal);
 if(abs(den)>.001){float d=dot(center-ro,normal)/den;vec3 q=ro+rd*d-center;float r=length(q);
  if(d>0.&&d<opaqueDepth&&r>radius*1.12&&r<radius*3.3){float a=atan(q.z,q.x),n=acNoise(q*2.+vec3(tm,0,0));
   float bands=pow(.5+.5*sin(r*8.-a*2.-tm*2.),4.);col+=mix(vec3(.12,.3,1.),vec3(1.1,.5,1.5),n)*bands*exp(-(r-radius)*.3)*1.2;
  }
 }
 for(int i=0;i<18;i++){float k=float(i),a=k*2.399+tm*(.20+hash1(k)*.12),r=(8.+hash1(k+2.)*6.)*(1.-collapse*.65);
  rock(center+vec3(cos(a)*r,sin(a*1.7)*2.,sin(a)*r),vec3(.25,.45,.3)*(1.-collapse*.4),.08);
 }
}
void nova(){
 float grow=ease(.08,.60,phase),compress=ease(.69,.90,phase),release=ease(.9,1.,phase);vec3 center=vec3(0,12.,0);float radius=(2.+grow*4.)*(1.-compress*.18)+release*1.2;
 float t=sphereHit(center,radius);float d=dot(center-ro,rd),r=length(ro+rd*max(0.,d)-center);
 if(d>0.)col+=vec3(1.1,.27,.02)*band(r-radius,radius*.14)*(.7+release*.4);
 if(t>0.&&t<opaqueDepth){vec3 n=normalize(ro+rd*t-center);float f=acFbm(n*9.+vec3(0,tm*.2,0));
  col=mix(vec3(.65,.075,.004),vec3(2.3,1.3,.18),f)+vec3(1.5,.35,.015)*pow(1.-abs(dot(n,rd)),4.);opaqueDepth=t;
 }
 // The stellar atmosphere is volumetric/radial, never long projected ribbons.
 seal(vec3(0,1.,0),vec3(0,1,0),12.,-tm*.05,.35,0.);
}
vec3 theatre(vec2 uv,int id){
 phase=FxParams.y;tm=FxTime;
 primary=id==0?vec3(1.6,1.05,.34):id==1?vec3(.20,1.5,1.27):id==2?vec3(.36,1.22,1.8):id==3?vec3(1.05,.43,1.9):vec3(1.9,.65,.10);
 accent=id==0?vec3(.10,.52,1.2):id==1?vec3(.12,.25,1.4):id==2?vec3(.08,.24,.72):id==3?vec3(.19,.27,1.35):vec3(.70,.045,.10);
 if(id==3){primary=vec3(.55,1.2,1.8);accent=vec3(.12,.38,.75);}
 if(id==5){primary=vec3(1.9,.48,.09);accent=vec3(.42,.21,.15);}
 if(id==6){primary=vec3(1.9,.48,.035);accent=vec3(.65,.045,.02);}
 if(id==7){primary=vec3(.7,.30,1.7);accent=vec3(.13,.55,1.2);}
 if(id==8){primary=vec3(2.,1.2,.25);accent=vec3(1.1,.13,.015);}
 // 0.7.0: snappier blocking - the reveal lands sooner, the final dive is shorter and much harder.
 float reveal=ease(.08,.26,phase),hero=ease(.26,.60,phase),dive=ease(.70,.94,phase);
 float angle=mix(-.38,.42,hero)+float(id)*.12;
 float radius=mix(32.,37.,reveal)-dive*10.;
 float eyeY=mix(4.,12.,hero)-dive*4.;
 ro=vec3(sin(angle)*radius,eyeY,-cos(angle)*radius);
 vec3 focus=vec3(0,mix(11.,14.,hero)-dive*3.,0);
 if(id==1){ro.y=8.+hero*11.;focus.y=12.-dive*4.;radius=36.;ro.xz=vec2(sin(angle),-cos(angle))*radius;}
 if(id==2){ro.y=5.+hero*9.;focus.y=9.;}
 if(id==3){ro=vec3(-12.+hero*16.,10.+hero*8.,-39.);focus=vec3(0,14.,0);}
 if(id==4){ro=vec3(-14.+hero*20.,7.+hero*7.,-26.+dive*6.);focus=vec3(-2.+dive*4.,12.-dive*7.,0.);}
 if(id==5){ro=vec3(-19.+hero*10.,13.+hero*4.,-26.+dive*9.);focus=vec3(0,1.,0);}
 if(id==6){ro=vec3(-22.+hero*12.,6.+hero*7.,-28.);focus=vec3(0,6.,0);}
 if(id==7||id==8){focus=vec3(0,11.,0);ro=vec3(sin(angle)*34.,10.+hero*6.,-cos(angle)*34.);}
 vec3 forward=normalize(focus-ro),right=normalize(cross(forward,vec3(0,1,0))),up=cross(right,forward);
 float lens=mix(1.19,.95,reveal)+hero*.045+dive*.06;
 rd=normalize(forward*lens+right*uv.x+up*uv.y);
 opaqueDepth=100000.;col=background(rd,id);
 floorScene();
 if(id==0)lance();else if(id==1)blades();else if(id==2)frost();else if(id==3)storm();else if(id==4)starfall();else if(id==5)rift();else if(id==6)inferno();else if(id==7)singularity();else nova();
 particles(id);
 col+=accent*exp(-rd.y*rd.y*90.)*.055;
 // Very restrained optical bloom and veil: bright core never blanks the whole composition.
 float halo=pow(max(0.,dot(rd,normalize(vec3(0,12,0)-ro))),120.);
 col+=primary*halo*.09;
 return pow(film(col*1.05),vec3(.78));
}
void main(){
 vec2 uv=(texCoord-.5)*vec2(FxScreen.x/FxScreen.y,1.);uv.y=-uv.y;
 int id=int(FxParams.x+.5);float blend=FxParams.z;
 vec2 sampleUV=vec2(texCoord.x,1.-texCoord.y);
 // Short, signature-colored impact fractures; no effect before impact or in reduced-flash mode.
 float fracture=FxImpact.z,force=clamp(FxImpact.y/2.1,.35,1.8);
 float clock=floor(max(0.,FxImpact.x)*1.6),row=floor(sampleUV.y*43.);
 float torn=step(.77,hash1(row*17.+clock*31.));
 float jitter=(hash1(row+clock*71.)-.5)*torn*.12*fracture*force;
 vec2 warped=clamp(sampleUV+vec2(jitter,0),vec2(.002),vec2(.998));
 float split=(.006+.026*torn)*fracture*force;
 // 0.7.3: full anime madness - the split rides the whole barrage at ~10% frame width.
 float radialCA=(.030*FxImpact.w+.16*exp(-max(0.,FxImpact.x)*.30)*step(0.,FxImpact.x))*force;
 radialCA+=.004*ease(.88,1.,min(1.,FxParams.y))*step(FxParams.y,1.);
 vec2 radialDir=(sampleUV-.5)*radialCA;
 vec3 world;
 world.r=texture(Sampler0,clamp(warped+vec2(split,0)+radialDir,vec2(.002),vec2(.998))).r;
 world.g=texture(Sampler0,warped).g;
 world.b=texture(Sampler0,clamp(warped-vec2(split,0)-radialDir,vec2(.002),vec2(.998))).b;
 // 0.7.2: painted signature fringes - saturated per-spell bands like the reference frames.
 world+=primary*smoothstep(.1,1.,abs(sampleUV.x-.5)*2.)*radialCA*6.+accent*smoothstep(.1,1.,abs(sampleUV.y-.5)*2.)*radialCA*6.;
 float fineRow=floor(sampleUV.y*FxScreen.y/3.);
 float shards=step(.989,hash1(floor(sampleUV.x*90.)+fineRow*113.+clock*271.));
 vec3 prism=mix(primary,accent,step(.5,hash1(row+clock)));
 prism=mix(prism,FxColor2.rgb,.40);
 world+=prism*(torn*.40+shards*1.1)*fracture*force;
 float scan=cos(sampleUV.y*FxScreen.y*1.57+clock)*.035*fracture;
 world*=1.+scan;
 // A single exposure dip replaces persistent rapid full-screen strobing.
 world*=1.-.17*fracture*step(2.,FxImpact.x);
 // World insert grading: keep the real terrain visible, only cool the shadows.
 float p=FxParams.y,pre=1.-ease(.98,1.06,p);
 world*=mix(vec3(1),vec3(.79,.85,1.),pre*.55);
 world+=FxColor2.rgb*.022*pre;
 // 0.7.3: the flash re-fires on every strike of the barrage.
 float impact=exp(-max(0.,FxImpact.x)*26.)*step(0.,FxImpact.x)*FxParams.w;
 world=mix(world,vec3(1.,.94,.82),clamp(impact,0.,1.)*.45);
 world*=1.+impact*.30*(1.-ease(0.,.9,length(uv)));
 vec3 scene=world;
 if(blend>.001)scene=mix(world,theatre(uv,id),blend);
 // 0.7.0: radial speed streaks on the first half-second after the strike.
 float speed=exp(-max(0.,FxImpact.x)*.14)*step(0.,FxImpact.x)*force;
 if(speed>.01){
  float ang=atan(uv.y,uv.x),rr=length(uv);
  float streak=pow(max(0.,sin(ang*44.+hash1(floor(ang*7.63))*9.)),24.);
  vec3 streakCol=mix(FxColor2.rgb,accent,step(.5,hash1(floor(ang*7.63)+clock)));
  scene+=streakCol*streak*smoothstep(.28,.6,rr)*(1.-smoothstep(1.1,1.6,rr))*speed*.55;
 }
 // 0.7.1: reference look - the frame desaturates while the RGB split is at its peak.
 scene=mix(scene,vec3(dot(scene,vec3(.299,.587,.114))),clamp(radialCA*5.,0.,.4));
 float vignette=1.-.18*ease(.26,1.1,length(uv));scene*=vignette;
 scene*=1.-.10*clamp(FxImpact.w,0.,1.)*step(0.,FxImpact.x);
 fragColor=vec4(scene,1.)*ColorModulator;
}
