package com.kostya.cataclysm.client;

import com.kostya.cataclysm.*;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;

/** The same five motifs are built in world space for observers and for the return-to-impact shot. */
public final class FirstFiveRenderer {
    private static final double TAU=Math.PI*2;
    private static double ease(double x){return ClientEffects.smooth(x);}
    private static void begin(ShaderInstance shader,float time,int c,int c2,float alpha,float mode,float speed,float scale,float extra){
        FxMesh.begin(shader,time,c,c2,alpha,mode,speed,scale,extra,shader==FxMesh.surface||shader==FxMesh.rune);
    }
    private static void rays(float t,int c,int c2,float a){begin(FxMesh.glow,t,c,c2,a,3,1.5f,.3f,0);}
    private static void seal(float t,int c,int c2,float a,float spin,float reveal,int points){begin(FxMesh.rune,t,c,c2,a,19,spin,reveal,points);}
    private static Vec3 radial(double angle,double r,double y){return new Vec3(Math.cos(angle)*r,y,Math.sin(angle)*r);}
    private static boolean visibleCrystal(Vec3 base,double height,double width){
        Vec3 camera=net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx=camera.x-base.x,dz=camera.z-base.z,margin=width*2+3;
        return dx*dx+dz*dz>margin*margin||camera.y<base.y-3||camera.y>base.y+height+3;
    }
    public static void draw(ClientEffects.Fx f,float partial){
        float age=f.age(partial),t=age/20f;double charge=Math.min(1,age/f.spell.impact),after=age-f.spell.impact;
        double s=f.packet.scale(),r=f.radius(),grow=ease((charge-.04)/.55),release=ease((charge-.70)/.30);
        float fade=(float)ease(Math.min(age/18.,(f.spell.duration-age)/35.));
        float hold=fade*(float)(after<0?1:Math.max(0,1-after/65.));int c=f.spell.color,c2=f.spell.secondary;
        Vec3 p=f.packet.target(),origin=f.packet.origin();
        // Performer prologue: three rotating seals climb around the caster, then threads are drawn to the target.
        if(age<f.spell.impact){
            seal(t,c,c2,fade*.8f,t*.3f,(float)ease(age/32.),6);
            FxMesh.disk(origin.add(0,.06,0),3.3,0);
            FxMesh.disk(origin.add(0,2.1*ease(age/45.),0),2.0,0);FxMesh.end();
            rays(t,c,c2,fade*.38f);
            for(int k=0;k<6;k++){
                double a=k*TAU/6+t*.28;Vec3 last=origin.add(radial(a,2.6,.1));
                for(int j=1;j<13;j++){double u=j/12.,bend=Math.sin(u*Math.PI);
                    Vec3 q=origin.lerp(p,u).add(radial(a+u*3,2.6*bend,(3+15*s)*bend));
                    FxMesh.ribbon(last,q,.08+.10*s);last=q;
                }
            }FxMesh.end();
        }
        if(after<70){
            seal(t,c,c2,hold*.8f,-t*.11f,(float)ease(age/55.),f.spell==Spell.GLACIAL_RAGNAROK?6:8);
            FxMesh.disk(p.add(0,.18,0),r*.83*ease(age/70.),0);FxMesh.end();
        }
        switch(f.spell){
            case HEAVENS_LANCE->{
                for(int k=0;k<3;k++){
                    double q=ease((charge-.08-k*.10)/.25),y=(24+k*22)*s;
                    seal(t,c,c2,hold*.9f,t*(k%2==0?.12f:-.15f),(float)q,6);
                    FxMesh.disk(p.add(0,y,0),(29-k*4)*s*q,0);FxMesh.end();
                }
                rays(t,c,c2,hold*.70f);
                for(int k=0;k<10;k++){double a=k*TAU/10+t*.04;
                    Vec3 q=p.add(radial(a,31*s,0));FxMesh.ribbon(q,q.add(0,(65+8*Math.sin(a))*s*grow,0),.18*s);
                }
                double tip=(after<0?72*(1-release):-Math.min(24,after*2))*s;
                Vec3 spear=p.add(0,tip,0);FxMesh.beam(spear.add(0,14*s,0),spear.add(0,93*s,0),.65*s,12);
                for(int k=0;k<6;k++){double y=(65-k*3)*s;
                    FxMesh.ribbon(spear.add(0,y,0),spear.add(-(15+k*2)*s,y+(9-k)*s,0),.25*s);
                    FxMesh.ribbon(spear.add(0,y,0),spear.add((15+k*2)*s,y+(9-k)*s,0),.25*s);
                }FxMesh.end();
                begin(FxMesh.surface,t,c,c2,hold,4,1,4,0);FxMesh.blade(spear,26*s,t*.035);FxMesh.end();
                if(after>=0&&after<55){rays(t,c,c2,fade*(float)(1-after/55.));FxMesh.beam(p.add(0,-25*s,0),p.add(0,145*s,0),(3+after*.22)*s,20);FxMesh.end();}
            }
            case THOUSAND_BLADES->{
                for(int tier=0;tier<3;tier++){
                    double y=(32+tier*26)*s,q=ease((charge-.10-tier*.1)/.3);
                    seal(t,c,c2,hold*.7f,t*(tier%2==0?.10f:-.13f),(float)q,8);
                    FxMesh.disk(p.add(0,y+20*s,0),(23+tier*17)*s,0);FxMesh.end();
                }
                begin(FxMesh.surface,t,c,c2,hold,4,1,4,0);
                for(int k=0;k<72;k++){
                    int tier=k/24;double a=k*TAU/24+t*(tier%2==0?.17:-.13),radius=(23+tier*16)*s;
                    double q=ease((charge-.1-tier*.1)/.3),drop=ease((charge-.74-(k%9)*.008)/.20);
                    double yBase=(28+tier*22)*s*q;
                    Vec3 tip=p.add(radial(a,radius,after<0?yBase*(1-drop):-after*1.2*s));
                    FxMesh.blade(tip,(11+tier*3)*s,a+Math.PI*.5);
                }FxMesh.end();
                if(charge>.75){rays(t,c,c2,hold*.6f);
                    for(int k=0;k<48;k++){Vec3 q=f.strikes.get(k%f.strikes.size());double drop=ease((charge-.75-(k%8)*.01)/.18);double y=(1-drop)*90*s;
                        FxMesh.ribbon(q.add(0,y,0),q.add(0,y+25*s,0),.4*s);
                    }FxMesh.end();}
            }
            case GLACIAL_RAGNAROK->{
                begin(FxMesh.surface,t,c,c2,hold*.9f,2,1,4,0);
                for(int k=0;k<12;k++){double a=k*TAU/12,q=ease((charge-.06-(k%6)*.035)/.4);
                    Vec3 base=p.add(radial(a,(35+(k%2)*18)*s,0));double height=(30+k%3*14)*s*q,width=(3+k%2*2)*s;
                    if(visibleCrystal(base,height,width))FxMesh.crystal(base,height,width,a);
                }
                FxMesh.crystal(p.add(0,(16-release*12)*s,0),40*s*grow,9*s,t*.12);FxMesh.end();
                rays(t,c,c2,hold*.8f);FxMesh.beam(p,p.add(0,79*s*grow,0),.7*s,8);
                for(int k=0;k<6;k++){
                    double a=k*TAU/6;Vec3 root=p.add(0,42*s,0),tip=p.add(radial(a,51*s*grow,82*s*grow));
                    FxMesh.ribbon(root,tip,.65*s);
                    for(int j=1;j<5;j++){Vec3 mid=root.lerp(tip,j/5.);Vec3 side=radial(a+Math.PI*.5,12*s*grow,12*s*grow);
                        FxMesh.ribbon(mid,mid.add(side),.25*s);FxMesh.ribbon(mid,mid.add(-side.x,side.y,-side.z),.25*s);}
                }FxMesh.end();
                seal(t,c,c2,hold*.7f,-t*.08f,1,6);FxMesh.disk(p.add(0,87*s*grow,0),53*s*grow,0);FxMesh.end();
                if(after>=0&&after<100){begin(FxMesh.surface,t,c,c2,fade*.85f,2,1,4,0);
                    for(int k=0;k<48;k++){Vec3 q=f.strikes.get(k%f.strikes.size());double wave=ease((after-k*.45)/24.),height=(12+k%5*7)*s*wave;
                        if(visibleCrystal(q,height,2.4*s))FxMesh.crystal(q,height,2.4*s,k);
                    }FxMesh.end();}
            }
            case TEMPEST_WRATH->{
                // Low-opacity moving cloud puffs leave the branching circuit readable.
                begin(FxMesh.glow,t,0x27213f,0x746396,hold*.65f,4,1,3,0);
                for(int k=0;k<22;k++){double a=k*2.399+t*.07;FxMesh.billboard(p.add(radial(a,(23+k%4*12)*s,(95+Math.sin(a)*6)*s*grow)),(17+k%3*5)*s);}FxMesh.end();
                for(int tier=0;tier<3;tier++){
                    seal(t,c,c2,hold*.8f,t*(tier%2==0?.17f:-.23f),1,7);
                    FxMesh.disk(p.add(0,(68+tier*16)*s*grow,0),(25+tier*17)*s*grow,0);FxMesh.end();
                }
                rays(t,c,c2,hold*.85f);
                for(int k=0;k<14;k++){
                    double a=k*TAU/14;Vec3 q=p.add(radial(a,(25+k%2*28)*s,.6));
                    Vec3 last=p.add(radial(a,14*s,96*s*grow));double pulse=.35+.65*Math.pow(.5+.5*Math.sin(t*2.5+k*1.7),6);
                    for(int j=1;j<=11;j++){
                        double u=j/11.;Vec3 next=p.add(radial(a,(14+(11+k%2*28)*u)*s,(96*grow*(1-u))*s)).add(Math.sin(j*17+k*3)*5*s,Math.cos(j*9+k)*2*s,Math.cos(j*11+k*5)*5*s);
                        FxMesh.tint(k%2==0?c:c2,hold*(float)pulse);FxMesh.ribbon(last,next,(after<0?.25:1.2)*s);
                        if(j==5)FxMesh.ribbon(next,next.add(radial(a+.7,14*s,-9*s)),.18*s);last=next;
                    }
                }FxMesh.end();
            }
            case STARFALL->{
                if(after<20){
                    double fall=ease((charge-.50)/.50);Vec3 core=p.add((-115*(1-fall))*s,(165*(1-fall)+7)*s,55*(1-fall)*s);double size=(9+grow*10)*s;
                    begin(FxMesh.surface,t,c,c2,hold,0,1.6f,5,0);FxMesh.sphere(core,size,32,20);FxMesh.end();
                    rays(t,c,c2,hold*.65f);for(int k=0;k<15;k++){
                        double a=k*2.399;Vec3 side=radial(a,size*.65,Math.sin(a)*size*.4),tail=new Vec3(-90*s,155*s,50*s);
                        FxMesh.ribbon(core.add(side),core.add(side).add(tail.scale(.6+k%3*.25)),(1.2+k%3*.6)*s);
                    }FxMesh.end();
                    begin(FxMesh.glow,t,c,c2,hold*.60f,2,0,0,0);FxMesh.billboard(core,size*3);FxMesh.end();
                }
            }
            default->{}
        }
        // Motif-specific motes: icy snow descends, swords climb, embers follow the meteor wake.
        int count=100*ClientOptions.DENSITY.get()/100;
        begin(FxMesh.glow,t,c,c2,fade*.75f,0,0,0,0);
        for(int k=0;k<count;k++){
            double a=k*2.399+t*.05,h=(k*17.31+age*(f.spell==Spell.GLACIAL_RAGNAROK?-.45:.30))%75;if(h<0)h+=75;
            double radius=r*(.2+.65*((k*7%31)/31.));
            FxMesh.billboard(p.add(radial(a,radius,h*s)),(.18+k%4*.12)*s);
        }FxMesh.end();
        if(after>=0){
            for(int k=0;k<3;k++){
                double wave=Math.max(0,after-k*8),progress=Math.min(1,wave/(50+k*10.));float alpha=fade*(float)(1-progress);
                begin(FxMesh.glow,t,c,c2,alpha,1,.87f,.018f,0);FxMesh.disk(p.add(0,(1+k*2)*s,0),Math.max(.1,r*progress*(1+k*.15)),0);FxMesh.end();
            }
            if(after<70){rays(t,c,c2,fade*(float)(1-after/70.));
                for(int k=0;k<24;k++){double a=k*TAU/24,v=(15+k%5*5)*s,u=after/40.;
                    Vec3 q=p.add(radial(a,v*u,(15*u-18*u*u)*s));FxMesh.ribbon(q,q.add(radial(a,5*s,4*s)),.3*s);
                }FxMesh.end();}
        }
    }
    private FirstFiveRenderer(){}
}
