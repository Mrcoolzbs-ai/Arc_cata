#moj_import <arcane_cataclysm:ac_common.glsl>
// Six authored, kilometre-scale environments isolated from the real Minecraft world.
// Catalogue ids 9..14; FxParams.y is server-catalogue impact-normalized client age.
// No sampled world framebuffer: this layer is an opaque independent film.
uniform sampler2D Landscape;
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
vec3 ro=vec3(0),rd=vec3(0),col=vec3(0),atmosphere=vec3(0),key=vec3(0),fill=vec3(0);
float depth=100000.,p=0.,tm=0.;
// 0.6.3 art direction: per-film sun/moon, sky palette, height fog and exposure.
vec3 sunCol=vec3(1.1,.78,.47),zen=vec3(.03),hz=vec3(.2),ambCol=vec3(.3);
float fogD=.0011,fogH=140.,expo=1.65,flash=0.,cloudLo=.45;
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
 float n=acHash(floor((q+center)*2.3));
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
 float grain=.5+.22*sin((ro.y+rd.y*t)*7.+(ro.x+rd.x*t)*3.);col=color*light*(.65+grain*.5)+fill*.02;depth=t;
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

// The heightfield is original terrain, not a captured Minecraft framebuffer.
// One small shared texture lookup replaces nested procedural FBM in the primary marcher.
vec3 sunDir=normalize(vec3(-.6,.38,-.55));
// Height-integrated fog with forward sun scattering keeps distance readable without milk-white wash.
vec3 aerial(vec3 c,float t){
 float k=rd.y/fogH,base=fogD*exp(-max(ro.y,0.)/fogH);
 float od=abs(k*t)<.001?base*t:base*(1.-exp(-k*t))/k;
 od+=t*.00012+pow(t/2600.,4.);
 float sun=max(0.,dot(rd,sunDir));
 vec3 fc=mix(hz,atmosphere,.55)+sunCol*(pow(sun,6.)*.28+pow(sun,40.)*.35);
 return mix(c,fc,1.-exp(-od));
}
vec3 skyLight(vec3 n){return mix(hz*.55+vec3(.02),zen*1.6+hz*.35,.5+.5*n.y)*ambCol;}
float terrain(vec2 x){
 vec3 sampleHeight=textureLod(Landscape,clamp(x/4096.+.5,vec2(.002),vec2(.998)),0.).rgb;
 float h=sampleHeight.r*255.+sampleHeight.g-26.;
 float flattening=1.-smoothstep(110.,380.,length(x));
#if FILM_ID != 12 && FILM_ID != 13
 h=mix(h,5.+sin(x.x*.018)*sin(x.y*.021)*1.4,flattening);
#endif

#if FILM_ID != 12
 h=mix(h,h*2.3,smoothstep(350.,1100.,length(x)));
#endif
 return h;
}
float fault(vec2 x){return x.x+sin(x.y*.008)*32.+sin(x.y*.029)*6.;}
float groundHeight(vec2 x){
 float h=terrain(x);
#if FILM_ID == 13
 float opening=ease(.28,.94,p)*65.;
 float f=abs(fault(x));
 h-= (1.-smoothstep(opening,opening+9.,f))*(110.+opening*2.)*ease(.10,.35,p);
 h+=exp(-abs(f-opening-17.)*.025)*ease(.15,.8,p)*13.;
#endif
 return h;
}
// Block districts are cut into a continuous landscape, never a finite tabletop.
vec2 district(vec2 x){
 vec2 cell=floor((x+14.)/28.);vec2 q=mod(x+14.,28.)-14.;
 float seed=acHash(vec3(cell,31.)),w=7.+seed*3.;
 float edge=max(abs(q.x)-w,abs(q.y)-w);
 float mask=(1.-step(580.,length(x)))*step(.15,seed);
 float h=10.+pow(seed,2.)*80.;
#if FILM_ID == 9
 float blast=ease(.73,1.02,p)*520.;float collapse=1.-ease(blast-70.,blast,length(x));h*=1.-collapse*.92;
#elif FILM_ID == 14
 float front=mix(-700.,850.,ease(.5,1.06,p));
 h*=ease(front-25.,front+35.,x.y);mask*=ease(front-40.,front+10.,x.y);
#endif
 return vec2(h*mask,edge);
}
float surface(vec2 x){
 float h=groundHeight(x);
#if FILM_ID == 13
 // Ridged basalt highlands: sharp crests, so the sundering cuts through real relief.
 float r1=1.-abs(acNoise(vec3(x*.011,3.))*2.-1.),r2=1.-abs(acNoise(vec3(x*.034,7.))*2.-1.);
 float rim=smoothstep(40.,160.,abs(fault(x)));
 h+=(r1*r1*34.+r2*r2*9.-16.)*rim+acNoise(vec3(x*.12,1.))*2.2;
#endif
#if FILM_ID == 10
 if(length(x)>370.){
  vec2 cell=floor(x/23.),q=mod(x,23.)-11.5;float seed=acHash(vec3(cell,5.));
  q-=vec2(sin(seed*81.),cos(seed*67.))*3.;float r=8.5+seed*3.;
  float dead=ease(.35+length(x)*.0003,.79+length(x)*.0003,p);
  if(length(q)<r&&seed>.12)h+=(24.+seed*22.)*sqrt(max(0.,1.-dot(q,q)/(r*r)))*(1.-dead);
 }
#endif
 return h;
}
float terrainShadow(vec3 q){
 float s=1.,t=1.5;
 for(int i=0;i<24;i++){
  vec3 x=q+sunDir*t;float d=x.y-surface(x.xz);s=min(s,9.*d/t);
  if(s<.01)return 0.;t+=clamp(d*.6,1.5,45.)+t*.07;if(t>1000.)break;
 }
 return smoothstep(0.,1.,s);
}
// Sun occlusion by the same solid district boxes: streets and lower facades fall into shade.
float cityShadow(vec3 o){
#if FILM_ID == 9 || FILM_ID == 12 || FILM_ID == 14
 vec3 d=sunDir;vec2 dir=d.xz+vec2(.000001),cell=floor((o.xz+14.)/28.);
 vec2 stepDir=sign(dir),delta=abs(28./dir),next=((cell+step(vec2(0),dir))*28.-14.-o.xz)/dir;
 vec3 inv=1./(d+vec3(.000001));
 for(int i=0;i<12;i++){
  vec2 x=cell*28.;float seed=acHash(vec3(cell,31.));
  if(length(x)<580.&&seed>.15){
   float hgt=district(x).x,w=7.+seed*3.;
   if(hgt>1.){
    vec3 size=vec3(w,hgt*.5,w),center=vec3(x.x,groundHeight(x)+hgt*.5,x.y);
    vec3 a=(center-size-o)*inv,b=(center+size-o)*inv,lo=min(a,b),hi=max(a,b);
    float t0=max(lo.x,max(lo.y,lo.z)),t1=min(hi.x,min(hi.y,hi.z));
    if(t1>max(t0,.05))return 0.;
   }
  }
  float tr;if(next.x<next.y){tr=next.x;next.x+=delta.x;cell.x+=stepDir.x;}else{tr=next.y;next.y+=delta.y;cell.y+=stepDir.y;}
  if(tr>420.||o.y+d.y*tr>95.)break;
 }
#endif
 return 1.;
}
// Exact grid traversal: each district is intersected as solid geometry. Heightfield
// marching must never skip narrow vertical facades or create perforated skyscrapers.
void metropolis(){
 vec2 dir=rd.xz+vec2(.000001),cell=floor((ro.xz+14.)/28.);
 vec2 stepDir=sign(dir),delta=abs(28./dir);
 vec2 boundary=(cell+step(vec2(0),dir))*28.-14.;
 vec2 next=(boundary-ro.xz)/dir;
 float travel=0.;
 for(int i=0;i<90;i++){
  vec2 x=cell*28.;float seed=acHash(vec3(cell,31.));
  if(length(x)<580.&&seed>.15){
   vec2 b=district(x);float w=7.+seed*3.,base=groundHeight(x),height=b.x;
   vec3 size=vec3(w,height*.5,w),center=vec3(x.x,base+height*.5,x.y);
   vec3 inv=1./(rd+vec3(.000001)),a=(center-size-ro)*inv,b2=(center+size-ro)*inv;
   vec3 lo=min(a,b2),hi=max(a,b2);float t=max(lo.x,max(lo.y,lo.z)),end=min(hi.x,min(hi.y,hi.z));
   if(height>1.&&t>0.&&t<end&&t<depth){
    vec3 q=ro+rd*t,v=abs((q-center)/size),n;
    if(v.x>v.y&&v.x>v.z)n=vec3(sign(q.x-center.x),0,0);else if(v.y>v.z)n=vec3(0,sign(q.y-center.y),0);else n=vec3(0,0,sign(q.z-center.z));
    // Three facade families: warm stone, pale concrete and dark glass towers.
    float kind=acHash(vec3(cell,57.)),up=sat((q.y-base)/max(height,1.));
    vec3 mat=kind<.38?mix(vec3(.30,.25,.20),vec3(.42,.35,.27),seed):kind<.72?mix(vec3(.30,.31,.32),vec3(.44,.44,.42),seed):vec3(.07,.09,.12);
    bool glass=kind>=.72;
    float footAO=.45+.55*sat((q.y-base)/14.);
    float diff=max(0.,dot(n,sunDir));if(diff>0.)diff*=cityShadow(q+n*.06);
    vec3 c=mat*(skyLight(n)*footAO+sunCol*diff*.95);
    if(n.y>.5){
     // Roofs: tar, parapet rim and the odd rooftop beacon on high towers.
     vec2 r=abs(q.xz-center.xz)/size.xz;float rim=step(.90,max(r.x,r.y));
     c=mix(vec3(.06,.06,.065),mat*.8,rim)*(skyLight(n)+sunCol*diff*.8);
     c+=vec3(1.6,.12,.06)*step(.45,height/80.)*exp(-dot(q.xz-center.xz,q.xz-center.xz)*2.)*(.6+.4*sin(tm*3.+seed*40.));
    }else{
     vec2 wall=vec2(abs(n.x)>.5?q.z:q.x,q.y);vec2 cellw=floor(wall*vec2(.29,.32)),win=fract(wall*vec2(.29,.32));
     float windows=step(.18,win.x)*step(win.x,.70)*step(.25,win.y)*step(win.y,.78);
     float rnd=acHash(vec3(cellw,seed*30.)),on=step(.58,rnd);
     vec3 warm=mix(vec3(1.,.62,.26),vec3(.75,.80,1.),step(.86,rnd));
     vec3 rf=reflect(rd,n);vec3 env=mix(hz,zen*1.3+hz*.3,sat(rf.y*1.6))+sunCol*pow(max(0.,dot(rf,sunDir)),24.)*.8;
     float fres=.04+.96*pow(1.-max(0.,dot(-rd,n)),5.);
     if(glass){c=mix(c,env*.85,.35+.55*fres);c+=warm*windows*on*.55*(1.-ease(.82,1.05,p));
      c*=.85+.15*step(.08,fract(wall.y*.32));}
     else{
      vec3 pane=mix(vec3(.025,.03,.04),env*.6,fres);
      c=mix(c,pane,windows);c+=warm*windows*on*(.30+.30*rnd);
      c*=.78+.22*step(.12,win.y);   // floor slab lines
      c*=.88+.12*step(.06,abs(fract(wall.x*.29+.5)-.5)); // pilasters
     }
     c*=.9+.2*up; // light gradient towards the crown
    }
#if FILM_ID == 9
    c+=vec3(.9,.16,.035)*exp(-abs(length(q.xz)-ease(.72,1.03,p)*900.)*.045)*ease(.7,.8,p);
#elif FILM_ID == 14
    c+=vec3(1.4,.12,.36)*exp(-abs(q.z-mix(-700.,850.,ease(.5,1.06,p)))*.1);
#endif
    col=aerial(c,t);depth=t;
   }
  }
  if(next.x<next.y){travel=next.x;next.x+=delta.x;cell.x+=stepDir.x;}else{travel=next.y;next.y+=delta.y;cell.y+=stepDir.y;}
  if(travel>min(depth,1800.))break;
 }
}
vec3 sky(int id){
 float y=max(rd.y,0.),horizon=pow(1.-y,5.);
 vec3 c=mix(zen,hz,horizon);
 c=mix(c,hz*.55,smoothstep(0.,-.25,rd.y));
 float sun=max(0.,dot(rd,sunDir));
 // Wide Mie glow + tight halo; the disk is per film (eclipsed in 10, moon in 12).
 c+=sunCol*(pow(sun,5.)*.10+pow(sun,48.)*.30)*(1.-horizon*.3);
#if FILM_ID != 9
 float disk=smoothstep(.99955,.99975,sun);
#if FILM_ID == 11
 c=mix(c,vec3(.80,.86,1.05)*(.85+.15*acNoise(rd*900.)),disk);
#else
 c+=sunCol*disk*3.;
#endif
#endif
#if FILM_ID == 11 || FILM_ID == 14 || FILM_ID == 9
 // Stars only above the haze layer.
 vec3 sd=rd*260.;vec3 cell=floor(sd);float star=acHash(cell);
 vec3 sp=fract(sd)-.5;float tw=.6+.4*sin(tm*3.+star*80.);
 c+=vec3(.75,.8,1.)*step(.985,star)*exp(-dot(sp,sp)*60.)*smoothstep(.04,.35,rd.y)*tw*.9;
#endif
#if FILM_ID == 14
 // Aurora curtains answer the court before the erasure.
 float ax=rd.x/(rd.y+.25)*2.+tm*.02;
 float curtain=pow(acNoise(vec3(ax*3.,tm*.05,1.)),3.)*smoothstep(.06,.3,rd.y)*(1.-smoothstep(.45,.9,rd.y));
 c+=vec3(.75,.12,.55)*curtain*.55*ease(.05,.4,p)+vec3(.15,.5,.65)*pow(acNoise(vec3(ax*5.+3.,tm*.07,4.)),4.)*curtain*.6;
#endif
 if(rd.y>0.){
  // Two-layer lit clouds: density plus one sun-offset sample for self-shadowed edges.
  vec2 uvc=rd.xz/(rd.y+.12)*1.05+vec2(tm*.004,tm*.001);
  float n=acFbm(vec3(uvc,6.)),ns=acFbm(vec3(uvc+sunDir.xz*.08,6.));
  float cover=smoothstep(cloudLo,cloudLo+.3,n)*smoothstep(0.,.12,rd.y);
  float lit=sat(.5+(n-ns)*5.);
  vec3 dark=mix(zen,hz,.35)*.55,bright=hz*.85+sunCol*(.18+.55*pow(sun,4.));
  vec3 cl=mix(dark,bright,lit);
  cl+=sunCol*pow(sun,10.)*smoothstep(.45,.62,n)*(1.-smoothstep(.62,.75,n))*.9;
  c=mix(c,cl,cover*.92);
 }
 c+=vec3(1.,.9,.8)*flash*(.2+.8*smoothstep(-.1,.4,rd.y));
atmosphere=c;
#if FILM_ID == 9
 float fall=ease(.65,1.03,p);
 vec3 toSun=vec3(0,260.-fall*210.,380.)-ro;
 float radius=(95.+fall*48.)/length(toSun);
 float d=length(rd-normalize(toSun));
 c+=vec3(1.7,.43,.19)*exp(-abs(d-radius)*150.)*(.7+ease(.15,.65,p));
 c+=vec3(.65,.13,.08)*exp(-abs(d-radius)*23.)*.5;
 c=mix(c,vec3(.004,.003,.008),1.-smoothstep(radius-.004,radius,d));
 float a=atan(rd.y-normalize(toSun).y,rd.x-normalize(toSun).x);
 c+=vec3(.9,.19,.09)*pow(max(0.,sin(a*13.+sin(a*19.)*.5-tm*.2)),12.)*exp(-abs(d-radius-.01)*32.)*.35;
#endif
 return c;
}
vec3 fogColor(){return atmosphere;}
void landscape(){
 float t=1.,old=1.;bool hit=false;
 // Bounded linear-distance growth retains near-field detail, reaches a 3 km horizon.
 for(int i=0;i<215;i++){
  vec3 q=ro+rd*t;float d=q.y-surface(q.xz);
  if(d<.08){hit=true;break;}old=t;
  t+=max(.18+t*.009,min(d*.45,25.+t*.014));
  if(t>3000.)break;
 }
 if(!hit)return;
 for(int k=0;k<6;k++){float mid=(old+t)*.5;vec3 q=ro+rd*mid;if(q.y>surface(q.xz))old=mid;else t=mid;}
 vec3 q=ro+rd*t;float eps=max(4.,t*.005),h=surface(q.xz);
 float hx0=surface(q.xz-vec2(eps,0)),hx1=surface(q.xz+vec2(eps,0)),hz0=surface(q.xz-vec2(0,eps)),hz1=surface(q.xz+vec2(0,eps));
 vec3 n=normalize(vec3(hx0-hx1,2.*eps,hz0-hz1));
 // Concavity occlusion from the same four samples: valleys and building feet darken.
 float ao=clamp(1.-((hx0+hx1+hz0+hz1)*.25-h)/eps*.55,.30,1.);
 float grain=textureLod(Landscape,q.xz/4096.+.5,0.).b;
 float near=exp(-t*.0018);
 float detail=mix(.55,acNoise(q*.68)*.65+acNoise(q*2.3)*.35,near);
 float macro=acNoise(vec3(q.xz*.006,2.)),strata=.5+.5*sin(q.y*.62+grain*7.+macro*4.);
 vec3 rock=mix(vec3(.15,.13,.115),vec3(.32,.28,.24),grain)*(.75+.4*strata);
 vec3 grass=mix(mix(vec3(.075,.15,.05),vec3(.20,.25,.08),macro),vec3(.25,.22,.10),grain*.45);
 float flatness=smoothstep(.62,.88,n.y+(detail-.5)*.25);
 vec3 mat=mix(rock,grass,flatness)*(.62+detail*.65);
#if FILM_ID == 10
 float death=ease(.35,.95,p);mat=mix(mat,mix(vec3(.26,.19,.07),vec3(.13,.11,.09),ease(.75,1.,p))*(.7+grain*.6+macro*.3),death*.85);
 if(q.y>groundHeight(q.xz)+2.)mat=mix(vec3(.08,.17,.035),vec3(.15,.10,.055),death)*(.5+detail*.7);
#elif FILM_ID == 11
 mat=mix(mat,vec3(.17,.13,.18),.5);
#elif FILM_ID == 12
 mat=mix(mat,vec3(.12,.19,.20),.5);
#elif FILM_ID == 13
 // Green highland turf on the flats, dark basalt with pale strata on every slope.
 mat=mix(mat,vec3(.085,.08,.078)*(.6+.8*strata),(1.-flatness)*.85);
 mat=mix(mat,vec3(.10,.13,.06)*(.7+detail*.6),flatness*.5);
#endif
 vec3 emission=vec3(0.);
#if FILM_ID == 9 || FILM_ID == 12 || FILM_ID == 14
 vec2 b=district(q.xz);
 if(b.y<.25&&b.x>1.&&q.y>groundHeight(q.xz)+1.){
  mat=vec3(.12,.15,.19)*(.75+grain*.6);
  if(abs(n.y)<.45){
   vec2 wall=vec2(abs(n.x)>.5?q.z:q.x,q.y);
   vec2 win=fract(wall*vec2(.34,.39));
   float windows=step(.25,win.x)*step(win.x,.70)*step(.28,win.y)*step(win.y,.75);
   float on=step(.46,acHash(vec3(floor(wall*vec2(.34,.39)),floor(q.x/28.))));
   emission=vec3(1.,.66,.26)*windows*on*.6;
   mat*=.6+.4*step(.12,win.y);
  }
 }
#endif
 float diffuse=max(0.,dot(n,sunDir)),shadow=diffuse>0.?terrainShadow(q+n*.6):0.;
 if(shadow>0.&&t<900.)shadow*=cityShadow(q+n*.3);
 vec3 light=skyLight(n)*ao+sunCol*diffuse*shadow*(.55+.45*ao);
 vec3 c=mat*light+emission;
 c+=mat*hz*.25*max(0.,-n.y*.5+.3)*ao;
#if FILM_ID == 13
 float width=ease(.28,.94,p)*65.;float f=abs(fault(q.xz));
 float seam=exp(-abs(f-width)*.13)*ease(.12,.6,p);
 float lava=(1.-smoothstep(width-5.,width+3.,f))*ease(.1,.6,p);
 float veins=smoothstep(.32,.66,acFbm(vec3(q.x*.09,q.z*.06,tm*.11)));
 float deep=1.-smoothstep(-170.,-70.,q.y);
 float cracks=pow(max(0.,sin(q.y*.45+acNoise(q*.12)*5.)),14.)*acNoise(q*.07);
 // Chasm walls: banded basalt cliffs that heat up towards the magma below.
 float below=terrain(q.xz)-q.y,wall=smoothstep(.35,.75,1.-n.y)*smoothstep(1.,6.,below);
 float bands=.5+.5*sin(q.y*1.15+acNoise(q*.045)*7.),chip=acNoise(q*.35);
 vec3 cliff=vec3(.055,.05,.05)*(.55+.9*bands*chip)*(skyLight(n)*.9+sunCol*diffuse*shadow*.6);
 cliff+=vec3(1.7,.34,.05)*(pow(sat(below/150.),2.2)*.55+cracks*sat(below/30.)*2.2);
 c=mix(c,cliff,wall*ease(.1,.45,p));
 c+=vec3(2.6,.30,.025)*(seam*.05*(1.-wall)+lava*deep*veins);
#endif
#if FILM_ID == 13
 c+=vec3(1.6,.36,.06)*exp(-max(0.,f-width)*.05)*ease(.15,.6,p)*.9*max(0.,n.y)*mat*step(width-2.,f);
#endif
#if FILM_ID == 9
 float ring=exp(-pow((length(q.xz)-ease(.72,1.03,p)*900.)/18.,2.))*ease(.7,.8,p);
 c+=vec3(3.,.6,.16)*ring;
#endif
#if FILM_ID == 14
 float front=mix(-700.,850.,ease(.5,1.06,p));
 float line=exp(-abs(q.z-front)*.13);c+=vec3(2.,.3,.65)*line;
 c*=mix(.12,1.,ease(front-20.,front+20.,q.z));
#endif
 col=aerial(c,t);depth=t;
}
// Coarse lobes plus fine leaf clusters displace the silhouette itself. This is not
// a smooth sphere with green noise painted on it. Every leaf volume remains bounded.
float leafField(vec3 q,vec3 center,float r){
 vec3 v=q-center;
 float broad=acNoise(q*.23),fine=acNoise(q*1.15);
 float lobes=acNoise(q*(2.6/max(r,1.)));
 return length(v)-r*(.70+.30*(broad-.5)+.22*(lobes-.5)+.05*(fine-.5));
}
void canopy(vec3 center,float r,float dead){
 vec3 o=ro-center;float b=dot(o,rd),h=b*b-dot(o,o)+r*r;if(h<0.)return;
 float t=max(.01,-b-sqrt(h)),end=min(depth,-b+sqrt(h));if(t>end)return;
 for(int k=0;k<40;k++){
  vec3 q=ro+rd*t;float d=leafField(q,center,r);
  if(d<.055){
   vec2 e=vec2(.12,0);vec3 n=normalize(vec3(leafField(q+e.xyy,center,r)-leafField(q-e.xyy,center,r),leafField(q+e.yxy,center,r)-leafField(q-e.yxy,center,r),leafField(q+e.yyx,center,r)-leafField(q-e.yyx,center,r)));
   float local=acNoise(q*.9),lit=max(0.,dot(n,sunDir));
   vec3 c=mix(vec3(.04,.10,.02),vec3(.13,.23,.045),local);
   float occl=.55+.45*sat(dot(normalize(q-center),n)*.5+.5+(local-.5)*.6);
   vec3 autumn=mix(mix(vec3(.42,.30,.04),vec3(.50,.13,.03),local),vec3(.16,.10,.05),sat(dead*2.-1.));
   c=mix(c,autumn,sat(dead*1.6))*(skyLight(n)*occl*1.3+sunCol*lit*.85);
   float back=pow(max(0.,dot(rd,sunDir)),4.);c+=vec3(.14,.20,.045)*back*(1.-dead);
   col=aerial(c,t);depth=t;return;
  }
  t+=max(.05,d*.5);if(t>end)return;
 }
}
#if FILM_ID == 9
void cataclysm(){
 float fracture=ease(.24,.68,p),fall=ease(.65,1.03,p);
 vec3 center=vec3(0,260.-fall*210.,380.);
 for(int i=0;i<13;i++){
  float k=float(i),a=k*TAU/13.,r=135.+fracture*75.;
  vec3 q=center+vec3(cos(a)*r,sin(a)*r,25.+sin(k)*15.);
  sphere(q,7.+hash(k)*10.,vec3(.10,.055,.065),fracture*.08);
 }
 // Fractures connect the failing celestial crown to the city, then become impacts.
 for(int i=0;i<5;i++){
  float k=float(i);vec3 end=vec3((k-2.)*150.,6.,60.+hash(k)*400.);
  vec3 last=center;
  for(int j=1;j<=6;j++){
   float f=float(j)/6.;vec3 q=mix(center,end,f)+vec3(sin(float(j)*7.+k)*12.,0,cos(float(j)*5.+k)*9.)*sin(f*PI);
   beam(last,q,.35,vec3(1.6,.23,.08),fracture*(1.-ease(.95,1.1,p))*.6);last=q;
  }
 }
}
#endif
#if FILM_ID == 10
void life(){
 float death=ease(.32,.96,p),fall=ease(.88,1.10,p);
 vec3 wood=mix(vec3(.23,.14,.065),vec3(.12,.10,.09),death);
 // Foreground trunks, middle-distance woods and a 150 metre world-tree.
 for(int i=0;i<72;i++){
  float k=float(i),a=k*2.399,r=95.+hash(k)*420.;vec2 x=vec2(cos(a),sin(a))*r;
  float y=terrain(x),ht=20.+hash(k+5.)*26.;
  branch(vec3(x.x,y,x.y),vec3(x.x,y+ht,x.y),1.4,wood);
  float local=ease(.3+r*.0005,.72+r*.0005,p);
  // Every forest tree carries a real crown of limbs, so bare winter trunks are not sticks.
  for(int j=0;j<3;j++){float a3=k*1.7+float(j)*2.094;vec3 top=vec3(x.x,y+ht*(.72+.1*float(j)),x.y);
   branch(top,top+vec3(cos(a3)*ht*.28,ht*.30,sin(a3)*ht*.28),.42,wood);}
  if(local<.98){float cr=ht*.35*(1.-local*.8);canopy(vec3(x.x,y+ht,x.y),cr,local);
   canopy(vec3(x.x+cr*.55*cos(k),y+ht-cr*.55,x.y+cr*.55*sin(k)),cr*.72,local);}
 }
 branch(vec3(0,5.,0),vec3(5.,90.-fall*40.,3.),6.,wood);
 branch(vec3(5.,70.-fall*20.,3.),vec3(-2.,143.-fall*95.,0),3.2,wood);
 for(int i=0;i<11;i++){
  float a=float(i)*2.399;vec3 v=vec3(cos(a),0,sin(a));
  branch(v*36.+vec3(0,5.,0),v*15.+vec3(0,7.,0),1.4,wood);
  branch(v*15.+vec3(0,7.,0),vec3(0,17.,0),2.5,wood);
  vec3 base=vec3(3,55.+float(i%4)*14.-fall*25.,0);
  vec3 tip=v*(28.+hash(float(i))*30.)+vec3(0,98.+float(i%4)*12.-fall*80.,0);
  branch(base,tip,1.8,wood);
  for(int j=0;j<3;j++){
   float f=float(j)/3.;vec3 q=mix(base,tip,.5+f*.5),end=q+vec3(-v.z,0,v.x)*(8.+f*10.)+vec3(0,10.,0);
   branch(q,end,.65,wood);
   float leaf=1.-ease(.67+f*.04,1.05,p);
   if(leaf>.03)canopy(end,17.*leaf,death);
   for(int twig=0;twig<3;twig++){
    float a2=a+float(twig)*2.1;vec3 tip2=end+vec3(cos(a2)*9.,4.+float(twig),sin(a2)*9.);
    branch(end,tip2,.24,wood);
   }
  }
 }
 for(int i=0;i<22;i++){
  float k=float(i),a=k*2.399;vec3 q=vec3(cos(a)*(15.+hash(k)*42.),mod(k*8.-tm*(1.+death*4.),120.)+8.,sin(a)*40.);
  sphere(q,.18+hash(k)*.4,vec3(.6,.5,.15),.1*(1.-death));
 }
 beam(vec3(0,8.,0),vec3(0,135.,0),.3,vec3(.45,1.2,.26),(1.-death)*ease(.05,.3,p)*.25);
}
#endif
float sdBox(vec3 q,vec3 b){vec3 d=abs(q)-b;return length(max(d,0.))+min(max(d.x,max(d.y,d.z)),0.);}
#if FILM_ID == 11
float cathedralField(vec3 q){
 float rise=ease(.02,.28,p);q.y+=80.*(1.-rise);
 float d=sdBox(q-vec3(0,8,30),vec3(57,3,105));
 // Twin high spires, repeated buttresses, clerestory and a hollow nave.
 vec3 a=q-vec3(0,42,24);float outer=sdBox(a,vec3(35,32,79));
 float inner=sdBox(a-vec3(0,-5,-4),vec3(29,31,82));d=min(d,max(outer,-inner));
 vec3 tower=q-vec3(0,48,-55);tower.x=abs(tower.x)-43.;
 d=min(d,sdBox(tower,vec3(12,40,15)));
 vec3 cone=tower-vec3(0,55,0);float spire=max(length(cone.xz)-(20.-cone.y)*.32,max(-cone.y-18.,cone.y-20.));d=min(d,spire);
 vec3 roof=q-vec3(0,78,24);float gable=max(abs(roof.x)*.72+roof.y-17.,max(-roof.y-3.,abs(roof.z)-81.));d=min(d,gable);
 vec3 rib=q-vec3(0,43,24);rib.z=mod(rib.z+10.,20.)-10.;rib.x=abs(rib.x)-39.;
 d=min(d,sdBox(rib,vec3(2.1,34,2.1)));
 vec3 butt=q-vec3(0,23,24);butt.z=mod(butt.z+10.,20.)-10.;butt.x=abs(butt.x)-49.;
 d=min(d,sdBox(butt,vec3(3,14,3)));
 // Stone archivolts, roof cornices and spire collars cast their own silhouette.
 d=min(d,sdBox(q-vec3(0,73,24),vec3(37,1.4,81)));
 vec3 coll=q-vec3(0,86,-55);coll.x=abs(coll.x)-43.;d=min(d,sdBox(coll,vec3(13.5,1.5,16.5)));
 vec3 front=q-vec3(0,40,-61.);front.x=abs(front.x)-26.;d=min(d,sdBox(front,vec3(1.1,31,2.3)));
 vec3 slit=q-vec3(0,43,24);slit.z=mod(slit.z+10.,20.)-10.;
 float opening=max(abs(slit.z)-3.0,max(22.-q.y,q.y-61.));
 d=max(d,-max(opening,abs(abs(q.x)-35.)-2.5));
 vec3 diagonal=q-vec3(0,44,24);diagonal.x=abs(diagonal.x)-44.;diagonal.z=mod(diagonal.z+10.,20.)-10.;
 diagonal.xy=rot(.57)*diagonal.xy;d=min(d,sdBox(diagonal,vec3(1.5,18.,1.5)));
 // Real arched opening, revealing illuminated inner apse.
 vec3 door=q-vec3(0,25,-55);float arch=max(abs(door.x)-15.,max(-door.y-16.,length(vec2(door.x,max(0.,door.y-8.)))-15.));
 d=max(d,-max(arch,abs(door.z)-12.));
 return d;
}
void cathedral(){
 float structureY=-80.*(1.-ease(.02,.28,p));
 // Clip marching to the structure, so the kilometre-scale terrain remains cheap.
 vec3 inv=1./(rd+vec3(.000001));vec3 a=(vec3(-65,0,-80)-ro)*inv,b=(vec3(65,150,140)-ro)*inv;
 vec3 lo=min(a,b),hi=max(a,b);float t=max(.1,max(lo.x,max(lo.y,lo.z))),end=min(depth,min(hi.x,min(hi.y,hi.z)));
 for(int i=0;i<110;i++){
  if(t>end)break;vec3 q=ro+rd*t;float d=cathedralField(q);
  if(d<.10){
   vec2 e=vec2(.15,0);vec3 n=normalize(vec3(cathedralField(q+e.xyy)-cathedralField(q-e.xyy),cathedralField(q+e.yxy)-cathedralField(q-e.yxy),cathedralField(q+e.yyx)-cathedralField(q-e.yyx)));
   vec2 wall=vec2(abs(n.x)>.5?q.z:q.x,q.y);
   vec2 brick=vec2(wall.x+mod(floor(wall.y/1.3),2.)*1.7,wall.y)/vec2(3.4,1.3);
   vec2 seams=abs(fract(brick)-.5);float mortar=smoothstep(.44,.49,max(seams.x,seams.y));
   float stones=acNoise(q*.6);vec3 c=mix(vec3(.11,.115,.14),vec3(.17,.17,.18),stones)*(.42+.58*max(0.,dot(n,sunDir)))*(1.-mortar*.20);
   float cornice=1.-smoothstep(.2,.7,abs(mod(q.y-12.,19.)-.0));c+=vec3(.025)*cornice;
   float localX=abs(n.x)>.5?mod(q.z+10.,20.)-10.:mod(q.x+10.,20.)-10.;
   float wy=q.y-44.;float archShape=max(abs(localX)-2.5,max(-wy-10.,length(vec2(localX,max(0.,wy-5.)))-2.5));
   float windows=(1.-smoothstep(-.2,.2,archShape))*step(27.,q.y)*step(q.y,65.);
   float mullion=step(.18,abs(localX))*step(.14,abs(mod(q.y,4.)-2.));windows*=mullion;
   windows*=.55+.45*acNoise(q*.33);
   c+=mix(vec3(.16,.12,.42),vec3(.68,.24,.10),acNoise(q*.15))*windows*(.8+ease(.3,.8,p));
   c+=vec3(.65,.31,.12)*exp(-length((q-vec3(0,22,80)).xz)*.03)*ease(.3,.8,p)*.3;
   col=aerial(c,t);depth=t;break;
  }t+=max(.08,d*.7);
 }
 for(int k=0;k<6;k++)for(int side=-1;side<=1;side+=2){
  vec3 at=vec3(float(side)*47.,9.,-95.-float(k)*25.);
  box(at,vec3(4.,4.,4.),vec3(.14,.14,.16),0.);
  box(at+vec3(0,8.,0),vec3(1.5,6.,1.5),vec3(.12,.12,.15),0.);
  sphere(at+vec3(0,15.,0),1.1,vec3(.30,.17,.06),.20*ease(.12,.4,p));
  beam(at+vec3(0,15.,0),at+vec3(0,15.01,0),1.2,vec3(1.,.55,.22),.35*ease(.12,.4,p));
 }
 float gate=ease(.32,.68,p),sentence=ease(.72,1.,p);
 for(int side=-1;side<=1;side+=2)box(vec3(float(side)*(7.5+gate*17.),26.+structureY,-61.),vec3(7.2,19.,1.1),vec3(.16,.10,.20),.03);
 glyph(vec3(0,65.+structureY,-57),vec3(0,0,-1),10.,tm*.035,ease(.05,.3,p)*.7);
 beam(vec3(0,12.+structureY,82),vec3(0,155.+structureY,82),.7+sentence*2.,vec3(.75,.32,1.5),sentence*.65);
 box(vec3(0,30.+structureY,98),vec3(18,20,.6),vec3(.025,.018,.04),0.);
 float pt=(97.-ro.z)/rd.z;
 if(pt>0.&&pt<depth){
  vec3 pq=ro+rd*pt-vec3(0,30.+structureY,97);vec2 v=pq.xy/17.;float r=length(v);
  if(r<1.){
   float a=atan(v.y,v.x),spiral=pow(max(0.,sin(a*3.+r*15.-tm*.5)),10.);
   float rim=exp(-abs(r-.94)*75.);
   col=vec3(.009,.004,.024)+vec3(.16,.045,.30)*spiral*r*r*gate+key*rim*.5;depth=pt;
  }
 }
 glyph(vec3(0,30.+structureY,96.8),vec3(0,0,-1),17.,-tm*.035,gate*.75);
}
#endif
#if FILM_ID == 12
float oceanHeight(vec2 x){
 float tide=ease(.23,1.,p),front=mix(650.,-350.,tide);
 float d=x.y-front-sin(x.x*.017)*12.;float crest=exp(-pow((d-15.)/52.,2.));
 float swell=3.+2.*sin(x.x*.035+tm*.8)+sin(x.y*.06-tm);
 return -2.+swell+(70.+tide*110.)*ease(.13,.32,p)*crest+55.*tide*ease(0.,60.,d);
}
void abyss(){
 if(ro.y<oceanHeight(ro.xz)-1.){
  // Retain actual submerged streets. An underwater shot is not a flat blue overlay.
  float fog=1.-exp(-min(depth,1500.)*.006);
  col=mix(col*vec3(.12,.48,.68),vec3(.004,.038,.065),fog*.85);
  col+=vec3(.015,.045,.055)*pow(max(0.,cos(rd.x*14.+rd.z*5.+tm*.07)),18.)*max(0.,rd.y+.5);
  for(int i=0;i<24;i++){
   float k=float(i),a=k*2.399;vec3 q=ro+vec3(cos(a)*(3.+hash(k)*16.),mod(k*2.+tm*1.6,24.)-5.,9.+hash(k+10.)*32.);
   vec2 seg=segment(q,q+vec3(.0001,0,0));float r=.05+hash(k+7.)*.12;
   if(seg.y<depth&&seg.x<r)col+=vec3(.09,.24,.27)*pow(sat(seg.x/r),5.)*.7;
  }
  return;
 }
 float t=2.,old=t;bool hit=false;
 for(int i=0;i<130;i++){
  vec3 q=ro+rd*t;float d=q.y-oceanHeight(q.xz);
  if(d<.12){hit=true;break;}old=t;t+=max(.25+t*.008,min(d*.32,24.));if(t>depth||t>2300.)break;
 }
 if(hit&&t<depth){
  for(int k=0;k<6;k++){float m=(old+t)*.5;vec3 q=ro+rd*m;if(q.y>oceanHeight(q.xz))old=m;else t=m;}
  vec3 q=ro+rd*t;float e=.6;
  vec3 n=normalize(vec3(oceanHeight(q.xz-vec2(e,0))-oceanHeight(q.xz+vec2(e,0)),2.*e,oceanHeight(q.xz-vec2(0,e))-oceanHeight(q.xz+vec2(0,e))));
  n=normalize(n+vec3(acNoise(q*.18+vec3(tm*.4,0,0))-.5,0,acNoise(q*.21+vec3(5,tm*.3,1))-.5)*.10);
  float fres=pow(1.-max(0.,dot(-rd,n)),4.);float spec=pow(max(0.,dot(reflect(-sunDir,n),-rd)),70.);
  float crestNoise=acFbm(vec3(q.x*.18,q.y*.37-tm*.45,q.z*.16));
  float foam=ease(90.,160.,q.y)*smoothstep(.44,.67,crestNoise);
  foam+=pow(max(0.,crestNoise-.45)*2.,2.)*ease(40.,150.,q.y)*.3;
  vec3 c=mix(vec3(.004,.025,.055),vec3(.12,.22,.25),fres)+vec3(.9,.73,.48)*spec*.7;
  float sss=pow(max(0.,dot(rd,sunDir)),2.)*ease(15.,130.,q.y)*(1.-foam);
  c+=vec3(.03,.30,.26)*sss*.8+vec3(.01,.08,.07)*ease(30.,150.,q.y);
  c=mix(c,vec3(.62,.74,.74)*(.6+.5*max(0.,dot(n,sunDir))),foam*.9);c+=vec3(.012,.065,.08)*max(0.,n.y);
  col=aerial(c,t);depth=t;
 }
 float under=ease(.88,1.03,p);col=mix(col,vec3(.006,.065,.095),under*.72);
}
#endif
#if FILM_ID == 14
void apocalypse(){
 float law=ease(.08,.44,p),erase=ease(.5,1.06,p);
 // A suspended court spans the city, with one travelling erasure plane.
 glyph(vec3(0,240,0),vec3(0,1,0),290.,tm*.015,law*.38);
 for(int k=0;k<7;k++){
  float a=float(k)*TAU/7.;vec3 v=vec3(cos(a)*330.,140.+sin(float(k))*25.,sin(a)*330.);
  box(v,vec3(7.,55.,5.),vec3(.12,.08,.15),law*.04);
  beam(v-vec3(0,40,6),v+vec3(0,40,6),.3,key,law*.5);
 }
 float front=mix(-700.,850.,erase);float t=(front-ro.z)/rd.z;
 if(t>0.&&t<depth){vec3 q=ro+rd*t;float f=exp(-abs(q.y-50.)*.012)*law;col+=vec3(.40,.055,.14)*f*.3;}
}
#endif
// Drifting embers/pollen/dust/spray: a few world-space cells sampled along the view ray.
vec3 motes(vec3 tint,float cellSize,vec3 drift,float amount,float size){
 vec3 acc=vec3(0.);
 for(int i=0;i<7;i++){
  float d=5.*pow(1.65,float(i));if(d>depth)break;
  float c=cellSize*pow(1.45,float(i));
  vec3 off=drift*tm;vec3 q=ro+rd*d+off;vec3 id=floor(q/c);
  vec3 h=vec3(acHash(id),acHash(id+7.13),acHash(id+3.37));
  if(h.x>amount)continue;
  vec3 pt=(id+.15+.7*h)*c-off;pt+=vec3(sin(tm*.9+h.y*30.),cos(tm*.7+h.z*20.),sin(tm*.6+h.x*40.))*c*.08;
  vec3 w=pt-ro;float along=dot(w,rd);if(along<=.5||along>depth)continue;
  float dist=length(w-rd*along),r=max(size,along*1.6/FxScreen.y);
  acc+=tint*exp(-dist*dist/(r*r))*min(1.,size/r*1.6+.25)*(.55+.45*sin(tm*4.+h.z*60.));
 }
 return acc;
}
vec3 atmosphereMotes(){
#if FILM_ID == 9
 return motes(vec3(2.2,.55,.12),9.,vec3(.6,2.2,.3),.55,.10)*ease(.3,.7,p);
#elif FILM_ID == 10
 float wither=ease(.35,.95,p);
 return motes(mix(vec3(1.2,1.1,.45),vec3(1.1,.35,.08),wither),7.,vec3(.4,mix(.25,-1.4,wither),.2),mix(.5,.8,wither),mix(.07,.12,wither));
#elif FILM_ID == 11
 return motes(vec3(.75,.55,1.4),8.,vec3(.1,.35,.1),.4,.06);
#elif FILM_ID == 12
 return motes(vec3(.45,.6,.65),6.,vec3(1.5,-.4,.8),.45*ease(.2,.6,p),.05);
#elif FILM_ID == 13
 return motes(vec3(2.4,.6,.12),9.,vec3(.8,3.,.4),.6,.12)*ease(.15,.5,p);
#else
 return motes(vec3(1.4,.5,1.0),9.,vec3(0.,1.2,0.),.5,.08)*ease(.05,.4,p);
#endif
}
vec3 story(vec2 uv,int id){
 p=FxParams.y;tm=FxTime;
 key=id==9?vec3(1.6,.5,.3):id==10?vec3(.40,1.1,.45):id==11?vec3(.85,.40,1.5):id==12?vec3(.12,.65,.85):id==13?vec3(1.8,.65,.12):vec3(1.5,.48,.68);
 fill=id==9?vec3(.30,.20,.29):id==10?vec3(.24,.35,.28):id==11?vec3(.29,.22,.40):id==12?vec3(.16,.32,.40):id==13?vec3(.40,.25,.20):vec3(.35,.18,.30);
 // Per-film art direction (sun/moon, palette, fog, exposure).
#if FILM_ID == 9
 sunDir=normalize(vec3(.12,.30,1.));sunCol=vec3(1.30,.55,.32);zen=vec3(.020,.012,.028);hz=vec3(.34,.12,.09);
 ambCol=vec3(.55,.62,1.05);fogD=.0011;fogH=120.;expo=1.5;cloudLo=.42;
#elif FILM_ID == 10
 float wither=ease(.35,.95,p);
 sunDir=normalize(vec3(-.72,mix(.26,.13,wither),.64));sunCol=mix(vec3(1.55,1.05,.58),vec3(1.25,.55,.30),wither);
 zen=mix(vec3(.08,.19,.36),vec3(.09,.06,.12),wither);hz=mix(vec3(.80,.64,.42),vec3(.58,.36,.27),wither);
 ambCol=vec3(.9);fogD=.0016;fogH=90.;expo=1.45;cloudLo=.47;
#elif FILM_ID == 11
 sunDir=normalize(vec3(.42,.34,.84));sunCol=vec3(.42,.50,.80);zen=vec3(.006,.008,.026);hz=vec3(.075,.07,.13);
 ambCol=vec3(1.1,1.05,1.3);fogD=.0035;fogH=26.;expo=2.1;cloudLo=.50;
#elif FILM_ID == 12
 sunDir=normalize(vec3(.30,.16,1.));sunCol=vec3(1.35,.86,.55);zen=vec3(.035,.065,.10);hz=vec3(.36,.40,.42);
 ambCol=vec3(.85,.95,1.05);fogD=.0013;fogH=120.;expo=1.5;cloudLo=.38;
 // Distant storm lightning, never a full-frame white flash.
 float bolt=fract(tm*.37);flash=FxParams.w*step(.5,p)*exp(-bolt*40.)*step(.55,hash(floor(tm*.37)))*.25;
#elif FILM_ID == 13
 sunDir=normalize(vec3(-.55,.30,.78));sunCol=vec3(1.30,.88,.62);zen=vec3(.045,.05,.075);hz=vec3(.27,.23,.23);
 ambCol=vec3(.65,.72,.95);fogD=.0009;fogH=150.;expo=1.5;cloudLo=.38;
#else
 sunDir=normalize(vec3(.38,.14,.92));sunCol=vec3(.95,.55,.70);zen=vec3(.008,.008,.032);hz=vec3(.13,.085,.17);
 ambCol=vec3(.70,.75,1.05);fogD=.0012;fogH=110.;expo=1.6;cloudLo=.48;
#endif
 int shot=p<.18?0:p<.38?1:p<.62?2:p<.84?3:p<1.?4:5;
 float u=shot==0?p/.18:shot==1?(p-.18)/.20:shot==2?(p-.38)/.24:shot==3?(p-.62)/.22:shot==4?(p-.84)/.16:(p-1.)/.13;u=sat(u);
 vec3 eye=vec3(0),look=vec3(0);float lens=1.3;
#if FILM_ID == 9
 if(shot==0){eye=vec3(-220.+u*60.,95.,-420.);look=vec3(0,95.,80.);}
 else if(shot==1){eye=vec3(-42.,26.,-220.+u*28.);look=vec3(45,180.,250.);lens=1.6;}
 else if(shot==2){eye=vec3(420.,230.+u*35.,-480.);look=vec3(0,90.,60.);}
 else if(shot==3){eye=vec3(-42.,18.,-360.+u*20.);look=vec3(30,160.,200.);lens=1.1;}
 else if(shot==4){eye=vec3(-480.-u*100.,310.+u*80.,-600.);look=vec3(0,8.,0);lens=1.1;}
 else{eye=vec3(350.+u*60.,170.,-470.);look=vec3(0,10.,80.);}
#elif FILM_ID == 10
 if(shot==0){eye=vec3(-200.+u*50.,48.,-260.);look=vec3(0,78.,0);}
 else if(shot==1){eye=vec3(62.-u*12.,84.,-115.);look=vec3(0,115.,0);lens=1.2;}
 else if(shot==2){eye=vec3(155.,145.+u*10.,-220.);look=vec3(0,78.,0);}
 else if(shot==3){eye=vec3(-65.+u*20.,17.,-98.);look=vec3(0,75.,0);lens=1.0;}
 else if(shot==4){eye=vec3(150.+u*70.,185.,-250.);look=vec3(0,45.,0);}
 else{eye=vec3(100.+u*25.,72.,-220.);look=vec3(0,27.,0);}
#elif FILM_ID == 11
 if(shot==0){eye=vec3(-160.+u*35.,45.,-260.);look=vec3(0,55.,20);}
 else if(shot==1){eye=vec3(100.,95.+u*25.,-120.);look=vec3(35,98.,-30);lens=1.4;}
 else if(shot==2){eye=vec3(-170.+u*40.,78.,-250.);look=vec3(0,67.,15);}
 else if(shot==3){eye=vec3(0,24.,-180.+u*90.);look=vec3(0,30.,80);lens=1.1;}
 else if(shot==4){eye=vec3(0,24.,-60.+u*105.);look=vec3(0,46.,98);lens=1.0;}
 else{eye=vec3(180.+u*50.,145.,-220.);look=vec3(0,52.,35);}
#elif FILM_ID == 12
 if(shot==0){eye=vec3(-200.+u*40.,90.,-390.);look=vec3(0,28.,220);}
 else if(shot==1){eye=vec3(-300.+u*40.,125.,-250.);look=vec3(0,105.,380);lens=1.5;}
 else if(shot==2){eye=vec3(320.,190.+u*30.,-380.);look=vec3(0,90.,180);}
 else if(shot==3){eye=vec3(-14.,34.,-350.+u*20.);look=vec3(0,90.,100);lens=1.1;}
 else if(shot==4){eye=vec3(-14.,27.-u*20.,-295.);look=vec3(0,50.,-100);lens=.95;}
 else{eye=vec3(-14.,8.,-320.-u*30.);look=vec3(0,20.,-100);}
#elif FILM_ID == 13
 if(shot==0){eye=vec3(-240.+u*50.,140.,-380.);look=vec3(0,12.,50);}
 else if(shot==1){eye=vec3(-85.,40.,-220.+u*40.);look=vec3(0,0.,90);lens=1.2;}
 else if(shot==2){eye=vec3(370.,300.+u*70.,-390.);look=vec3(0,-30.,20);}
 else if(shot==3){eye=vec3(-130.-u*60.,65.,-250.);look=vec3(0,-80.,20);lens=1.05;}
 else if(shot==4){eye=vec3(200.,440.+u*90.,-340.);look=vec3(0,-80.,40);}
 else{eye=vec3(-450.-u*70.,250.,-430.);look=vec3(0,-20.,70);}
#else
 if(shot==0){eye=vec3(-220.+u*60.,90.,-410.);look=vec3(0,85.,10);}
 else if(shot==1){eye=vec3(190.,150.+u*20.,-240.);look=vec3(0,240.,0);lens=1.0;}
 else if(shot==2){eye=vec3(-410.,270.+u*30.,-490.);look=vec3(0,130.,0);}
 else if(shot==3){eye=vec3(-14.,95.,-370.+u*45.);look=vec3(0,180.,0);lens=1.0;}
 else if(shot==4){eye=vec3(350.,380.+u*50.,-450.);look=vec3(0,65.,20);}
 else{eye=vec3(-370.-u*60.,240.,-450.);look=vec3(0,80.,0);}
#endif
 // Keep street-level camera lenses above buildings instead of cutting through blocks.
#if FILM_ID == 9 || FILM_ID == 12 || FILM_ID == 14
 eye.y=max(eye.y,groundHeight(eye.xz)+6.);
#endif
 // Terrain clearance applies to the forest too; never begin a shot inside a hillside.
 eye.y=max(eye.y,groundHeight(eye.xz)+12.);
 camera(eye,look,uv,lens);depth=100000.;col=sky(id);landscape();
#if FILM_ID == 9 || FILM_ID == 12 || FILM_ID == 14
 metropolis();
#endif
#if FILM_ID == 9
 cataclysm();
#elif FILM_ID == 10
 life();
#elif FILM_ID == 11
 cathedral();
#elif FILM_ID == 12
 abyss();
#elif FILM_ID == 14
 apocalypse();
#elif FILM_ID == 13
 // Heat haze: a short emissive march above the fault, so the wound lights the air.
 {float open=ease(.28,.94,p)*65.,g=0.;
  for(int i=0;i<14;i++){float d=(float(i)+.5)*min(depth,1400.)/14.;vec3 x=ro+rd*d;
   float f=abs(fault(x.xz));g+=exp(-max(0.,f-open*.85)*.07)*exp(-max(0.,x.y-groundHeight(x.xz))*.02)*step(4.,open)*(.6+.4*acNoise(x*.02+vec3(0,-tm*.6,0)));}
  col+=vec3(1.3,.30,.05)*g/14.*ease(.3,.7,p)*.28;}
#endif
 col+=atmosphereMotes();
 // Filmic shoulder, then a gentle grade: contrast S-curve, saturation, split toning.
 vec3 g=pow(film(col*expo),vec3(.92));
 float l=dot(g,vec3(.2126,.7152,.0722));
 g=mix(vec3(l),g,1.18);
 g=mix(g,g*g*(3.-2.*g),.30);
 g+=(1.-l)*(1.-l)*fill*.035-l*l*vec3(0.,.01,.025);
 return clamp(g,0.,1.);
}
void main(){
 vec2 uv=(texCoord-.5)*vec2(FxScreen.x/FxScreen.y,1.);uv.y=-uv.y;
 vec3 scene=story(uv,FILM_ID);
 float endFade=1.-smoothstep(75.,100.,FxImpact.x),openFade=smoothstep(0.,.7,FxTime);
 float vig=1.-.32*smoothstep(.35,1.25,length(uv*vec2(.85,1.15)));
 float grain=(acHash(vec3(texCoord*FxScreen,floor(FxTime*24.)))-.5)*.022;
 scene=scene*vig+grain*(.4+scene);
 scene*=openFade*endFade;
 fragColor=vec4(scene,1.)*ColorModulator;
}
