#version 150
#moj_import <arcane_cataclysm:ac_common.glsl>
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform vec4 FxParams; // spell, time/impact, astral mix, impact flash
uniform vec4 FxColor2; // signature color, bloom amount
uniform float FxTime;
uniform vec2 FxScreen;
uniform vec4 FxImpact; // ticks since the latest barrage strike, bounded power, glitch envelope, shake envelope
uniform vec4 FxColor3; // secondary spell color for the opposite fringe band
in vec2 texCoord;
in vec4 vertexColor;
out vec4 fragColor;
const float PI=3.14159265;
mat2 rot(float a){float c=cos(a),s=sin(a);return mat2(c,-s,s,c);}
float noise2(vec2 p){return acNoise(vec3(p,2.7));}
float neb(vec2 p){return noise2(p)*.6+noise2(p*2.13+3.1)*.28+noise2(p*4.31-1.3)*.12;}
float ring(float r,float at,float width){return exp(-pow((r-at)/width,2.0));}
float segment(vec2 p,vec2 a,vec2 b){vec2 ba=b-a;return length(p-a-ba*clamp(dot(p-a,ba)/max(dot(ba,ba),.0001),0.,1.));}
vec3 stars(vec2 p){
    vec3 outCol=vec3(0.);
    for(int i=0;i<3;i++){
        float k=38.+float(i)*27.;vec2 q=p*k+vec2(FxTime*.12*(float(i)+1.),float(i)*14.8);
        vec2 id=floor(q),f=fract(q)-.5;float h=acHash(vec3(id,float(i)+9.));
        float d=length(f);float dot_=exp(-d*d*750.)*step(.962,h);
        float flare=exp(-abs(f.x)*200.)*exp(-abs(f.y)*14.)+exp(-abs(f.y)*200.)*exp(-abs(f.x)*14.);
        outCol+=mix(vec3(.35,.55,1.),vec3(1.,.78,.49),fract(h*51.))*(dot_*1.5+flare*step(.997,h)*.6);
    }
    return outCol;
}
vec3 astral(vec2 p,int spell,float phase){
    float time=FxTime,r=length(p),a=atan(p.y,p.x);vec3 sig=FxColor2.rgb;
    vec2 q=rot(time*.025)*p;
    float n=neb(q*3.+vec2(time*.025,0));
    vec3 col=vec3(.003,.005,.015)+stars(q);
    col+=mix(vec3(.025,.04,.16),sig*.19,n)*pow(n,3.)*2.0;
    float crescendo=smoothstep(.18,.9,phase);
    if(spell==0||spell==5||spell==6||spell==9){
        // Procedural celestial body, lit from within; distinct material per invocation.
        vec2 pc=p-vec2(spell==0?-.12:0.,-.02);float rr=length(pc),rad=spell==9?.34:.28;
        if(rr<rad){
            vec3 normal=normalize(vec3(pc/rad,sqrt(max(0.,1.-rr*rr/(rad*rad)))));
            float surface=neb(normal.xy*4.+vec2(time*.12,normal.z*3.));
            float light=max(.08,dot(normal,normalize(vec3(-.5,.8,1.))));
            vec3 body=mix(sig*.1,sig*.85,surface)*light;
            if(spell==5){float crack=pow(1.-abs(sin(pc.x*19.+neb(pc*7.)*9.)),18.);body+=vec3(2.,.24,.015)*crack;}
            if(spell==6)body=mix(vec3(1.,.13,.005),vec3(2.,1.3,.25),surface)*(.5+.5*normal.z);
            col=mix(col,body,(1.-smoothstep(rad-.008,rad,rr)));
        }
        float corona=exp(-abs(rr-rad)*35.)*(.4+neb(pc*13.-time*.15));
        col+=sig*corona*(spell==6?2.5:1.2);
        if(spell==0){
            vec2 head=mix(vec2(.9,.6),vec2(-.12,0.),crescendo);float trail=exp(-segment(p,head,head+vec2(.8,.45))*180.);
            col+=vec3(1.8,.65,.18)*(trail*.65+exp(-length(p-head)*65.)*4.);
        }
        if(spell==9){for(int i=0;i<5;i++){
            float th=float(i)*6.28318/5.+time*.25;vec2 moon=vec2(cos(th)*.62,sin(th)*.22);
            col+=sig*exp(-length(p-moon)*65.)*3.;
            col+=sig*ring(length(vec2(p.x,p.y*2.6)),.64,.003)*.5;
        }}
    }else if(spell==2){
        float warp=.15/max(r,.04);vec2 lens=rot(warp)*p;float swirl=neb(lens*7.+vec2(a*2.,-time*.1));
        float disk=exp(-pow((length(vec2(p.x,p.y*3.6))-.3)/.055,2.));
        col+=mix(sig,vec3(.12,.7,1.),swirl)*(disk*3.+ring(r,.2,.008)*3.);
        col*=smoothstep(.165,.2,r);col+=sig*ring(r,.19,.004)*2.;
    }else if(spell==3){
        // A six-sided frost sepulchre instead of a generic sword-shaped ice particle.
        vec2 ice=rot(.12*sin(time*.2))*p;float poly=max(abs(ice.x)*.866+abs(ice.y)*.5,abs(ice.y));
        col+=sig*(ring(poly,.38,.004)*2.+ring(poly,.33,.002));
        for(int i=0;i<6;i++){
            float angle=float(i)*PI/3.;vec2 tip=vec2(cos(angle),sin(angle))*.58;
            float line=segment(ice,tip*.4,tip);col+=vec3(.2,.7,1.5)*exp(-line*240.);
        }
        float flakes=pow(noise2(vec2(p.x*90.,p.y*90.+time*9.)),22.);col+=vec3(.5,.85,1.)*flakes*5.;
        col+=sig*exp(-r*r*14.)*.3;
    }else if(spell==1){
        // Celestial colonnade / focusing lens, receding into depth.
        for(int i=0;i<9;i++){
            float depth=float(i)/8.;float sz=mix(.08,.75,depth*depth);vec2 pp=p-vec2(0.,.12);
            float box=max(abs(pp.x),abs(pp.y)*2.3);
            col+=sig*ring(box,sz,.0015+depth*.001)*(.3+depth);
        }
        col+=vec3(1.,.9,.5)*exp(-abs(p.x)*180.)*crescendo*2.;
    }else if(spell==4){
        float eye=neb(rot(time*.1)*p*5.+vec2(a,r*7.-time*.15));col+=sig*pow(eye,3.)*.8;
        for(int i=0;i<8;i++){
            float ar=float(i)*PI/4.+.08*sin(time);vec2 pp=rot(ar)*p;
            float curve=pp.x-(noise2(vec2(pp.y*15.,floor(time*6.)+float(i)))-.5)*.11;
            col+=vec3(.4,.55,1.)*exp(-abs(curve)*330.)*smoothstep(.08,.3,r)*(1.-smoothstep(.4,.8,r));
        }
        col+=sig*ring(r,.3,.008)*1.5;
    }else if(spell==7){
        for(int i=0;i<32;i++){
            float ang=float(i)*6.28318/32.+time*.08;vec2 pp=rot(ang)*p-vec2(.34,0.);
            float blade=segment(pp,vec2(0.),vec2(.22,0.));float guard=segment(pp,vec2(.17,-.035),vec2(.17,.035));
            col+=sig*(exp(-blade*400.)+exp(-guard*350.))*(.45+float(i%3)*.2);
        }
        col+=sig*ring(r,.29,.003);col+=stars(rot(-.1)*p*.7)*.8;
    }else{
        vec2 flame=p;flame.y+=time*.12;float cloud=neb(flame*5.);
        float gate=length(vec2(p.x,p.y*.8));col+=vec3(2.,.25,.01)*ring(gate,.38,.045)*(.3+cloud);
        col+=sig*pow(cloud,5.)*(1.-smoothstep(.2,.8,r));
        col+=sig*ring(max(abs(p.x)*.8,abs(p.y)),.43,.003);
    }
    // Orbiting calibration arcs and an imploding corona give the inserts camera/scale cues.
    float tick=step(.94,fract((a+time*.08)*32.));
    col+=sig*(ring(r,.72,.0015)*(.2+tick*.8)+ring(r,.68,.001)*.3);
    float speedlines=pow(max(0.,sin(a*90.+noise2(vec2(a*4.,0.))*6.)),30.);
    col+=sig*speedlines*smoothstep(.3,1.2,r)*crescendo*.18;
    col*=1.-smoothstep(.3,1.4,r)*.5;
    return col;
}
vec3 bright(vec2 uv){vec3 c=texture(Sampler0,uv).rgb;return c*smoothstep(.65,1.2,max(c.r,max(c.g,c.b)));}
void main(){
    vec2 uv=vec2(texCoord.x,1.-texCoord.y),p=(texCoord-.5)*vec2(FxScreen.x/FxScreen.y,1.);
    float after=FxParams.y-1.;vec2 q=uv-.5;float hit=step(0.,after);float afterHit=FxImpact.x;
    // 0.7.3: every strike of the barrage re-fires the shockfront - two hard fronts per hit.
    float shock=sin(length(q)*44.-afterHit*38.)*exp(-max(0.,afterHit)*3.2)*.016*hit;
    shock+=sin(length(q)*30.-(afterHit-6.)*26.)*exp(-abs(afterHit-6.)*.8)*.010*step(6.,afterHit);
    // 0.7.7: the split is a brief hit accent - the glitch envelope now dies inside half a second.
    float caEnv=max(FxImpact.w*.024,FxImpact.z*.09)+.0035*smoothstep(.82,1.,min(1.,FxParams.y))*(1.-hit);
    vec2 caDir=q*vec2(FxScreen.x/FxScreen.y,1.);
    vec2 sampleUV=clamp(uv+normalize(q+vec2(.0001))*shock,vec2(.001),vec2(.999));
    vec3 world;
    world.r=texture(Sampler0,clamp(sampleUV+caDir*caEnv,vec2(.001),vec2(.999))).r;
    world.g=texture(Sampler0,sampleUV).g;
    world.b=texture(Sampler0,clamp(sampleUV-caDir*caEnv,vec2(.001),vec2(.999))).b;
    // 0.7.2: painted signature fringes - saturated per-spell bands like the reference frames, not content-derived tints.
    world+=FxColor2.rgb*smoothstep(.34,.95,abs(q.x))*caEnv*2.2+FxColor3.rgb*smoothstep(.34,.95,abs(q.y))*caEnv*2.2;
    vec2 pixel=1./FxScreen;vec3 bloom=vec3(0.);
    for(int i=0;i<8;i++){
        float a=float(i)*PI/4.;vec2 off=vec2(cos(a),sin(a))*pixel;
        bloom+=bright(sampleUV+off*3.)*.07+bright(sampleUV+off*9.)*.04;
    }
    float grade=1.-.24*smoothstep(.1,.8,min(1.,FxParams.y));
    if(FxParams.y>1.)grade=1.;world*=grade;
    world+=bloom*FxColor2.a;
    // 0.7.3: the flash re-fires on every strike of the barrage, not just the first.
    float flash=exp(-max(0.,afterHit)*.42)*hit*FxParams.w;
    world=mix(world,vec3(1.,.96,.9)+FxColor2.rgb*.4,clamp(flash,0.,.85));
    if(flash>.02){vec3 pull=vec3(0.);
        for(int k=1;k<=4;k++){float fstep=float(k)*.25;
            pull+=texture(Sampler0,clamp(sampleUV-q*fstep*vec2(FxScreen.x/FxScreen.y,1.)*.14,vec2(.001),vec2(.999))).rgb;}
        world=mix(world,pull*.33,clamp(flash*.9,0.,.75));}
    // 0.7.1: reference look - the frame desaturates toward the edges while the RGB split is at its peak.
    world=mix(world,vec3(dot(world,vec3(.299,.587,.114))),clamp(caEnv*1.3*smoothstep(.3,.95,length(q)),0.,.20));
    world=world/(vec3(1.)+world*.22);
    world=mix(world,world+FxColor2.rgb*.18,FxParams.w);
    if(FxParams.z>.001){vec3 vision=astral(p,int(FxParams.x+.5),FxParams.y);vision=vec3(1.)-exp(-vision*1.2);world=mix(world,vision,FxParams.z);}
    // 0.7.5: TSS-style impact frames - one stylized frame per tick for six ticks after each strike (ink/inverted/accent).
    float ifStep=FxImpact.x>=0.?floor(min(FxImpact.x,7.)):-1.; // 0.7.6: negative clock = no strike yet, no impact frame
    int ifMode=ifStep==0.?3:ifStep==1.?1:ifStep==2.?3:ifStep==3.?1:ifStep==4.?0:(ifStep==5.?3:-1);
    if(ifMode>=0&&FxColor3.a>0.){
        float ifLum=dot(world,vec3(.299,.587,.114));vec3 ifFrame;
        if(ifMode==1)ifFrame=vec3(1.)-world;
        else{float ink=smoothstep(.18,.72,ifLum);
            ifFrame=mix(vec3(.015,.01,.02),FxColor2.rgb,ink);
            if(ifMode==3)ifFrame=mix(ifFrame,FxColor3.rgb,step(.5,ifLum)*.55);}
        world=mix(world,ifFrame,.92*clamp(FxColor3.a,0.,1.));}
    // 0.7.5: the hit-stop beat - the frame drains of colour for a moment after each strike.
    world=mix(world,vec3(dot(world,vec3(.299,.587,.114))),.24*FxColor3.a*step(0.,FxImpact.x)*(1.-smoothstep(6.,12.,FxImpact.x)));
    // 0.7.5: TSS zoom blur - the frame punches outward for a beat at each strike.
    float zoomK=.38*FxColor3.a*smoothstep(0.,.6,FxImpact.x)*(1.-smoothstep(6.,14.,FxImpact.x));
    if(zoomK>.02){vec3 zoom=world;
        for(int k=1;k<=5;k++){float zs=1.-float(k)*.012*zoomK;
            zoom+=texture(Sampler0,clamp((sampleUV-.5)*zs+.5,vec2(.001),vec2(.999))).rgb;}
        world=zoom/6.;}
    world*=1.-.3*smoothstep(.25,.9,length(p));
    fragColor=vec4(world,1.)*ColorModulator;
}
