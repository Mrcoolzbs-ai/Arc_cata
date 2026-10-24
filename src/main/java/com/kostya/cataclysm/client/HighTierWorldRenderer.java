package com.kostya.cataclysm.client;

import com.kostya.cataclysm.*;
import net.minecraft.world.phys.Vec3;

/** World counterparts of the six films. Bounded geometry; no legacy ritual stacks or blades.
 * All transient visuals expire 80 ticks after impact, independently of server-job progress. */
public final class HighTierWorldRenderer {
    static double ease(double x){return ClientEffects.smooth(x);}
    static void matter(ClientEffects.Fx f,float t,float a,int color){FxMesh.begin(FxMesh.surface,t,color,f.spell.secondary,a,7,1,3,0,true);}
    public static void draw(ClientEffects.Fx f,float partial){
        float age=f.age(partial),t=age/20;double phase=age/f.spell.impact,after=age-f.spell.impact,s=f.packet.scale();
        if(after>=80)return;float a=(float)(ease(age/24.)*(1-ease(after/80.)));if(a<.001)return;
        double grow=ease((phase-.06)/.42),release=ease((phase-.7)/.3),decay=1-ease(after/65.);Vec3 p=f.packet.target();
        switch(f.spell){
            case LAST_CATACLYSM->{
                Vec3 sun=p.add(0,(85-release*55)*s,0);FxMesh.begin(FxMesh.voidEnergy,t,0x281a42,0xdd719f,a,1,1,3,0,true);FxMesh.sphere(sun,(5+19*grow)*s*decay,28,16);FxMesh.end();
                matter(f,t,a,0x52465e);for(int k=0;k<12;k++){double angle=k*Math.PI/6,r=(31+release*12)*s;FxMesh.box(sun.add(Math.cos(angle)*r,-release*(15+k%3*8)*s,Math.sin(angle)*r),2*s,8*s,1.5*s);}FxMesh.end();
            }
            case VERDANT_RUIN->{
                // One modest dead tree, opaque/depth-writing, 31 branches instead of translucent roots.
                matter(f,t,a,0x665649);double h=(12+32*grow)*(1-release*.15)*s*decay;
                FxMesh.beam(p,p.add(0,h,0),1.8*s,6);
                for(int k=0;k<10;k++){double angle=k*2.399,y=h*(.42+(k%4)*.12),r=(7+k%3*3)*s*grow*decay;Vec3 tip=p.add(Math.cos(angle)*r,y+8*s*decay,Math.sin(angle)*r);
                    FxMesh.beam(p.add(0,y,0),tip,.55*s,5);FxMesh.beam(tip,tip.add(Math.sin(angle)*3*s,4*s,Math.cos(angle)*3*s),.22*s,4);
                }FxMesh.end();
                double drain=ease((phase-.37)/.45);
                if(drain<.97){matter(f,t,a*(float)(1-drain),drain<.35?0x527b43:drain<.7?0x76653e:0x666052);for(int k=0;k<10;k++){double angle=k*2.399;FxMesh.box(p.add(Math.cos(angle)*(9+k%3*2)*s,h*.7+4*s,Math.sin(angle)*(9+k%3*2)*s),3*s,2*s,3*s);}FxMesh.end();}
            }
            case OBSIDIAN_CATHEDRAL->{
                matter(f,t,a,0x363044);double y=-55*s*(1-grow)-ease(after/70.)*35*s;
                for(int side=-1;side<=1;side+=2){FxMesh.box(p.add(side*18*s,y,-8*s),5*s,68*s,6*s);for(int k=0;k<4;k++)FxMesh.box(p.add(side*18*s,y+(68+k*4)*s,-8*s),(5-k)*s,4*s,(6-k)*s);
                    FxMesh.box(p.add(side*12*s,y,20*s),2*s,42*s,24*s);}
                FxMesh.box(p.add(0,y+43*s,20*s),14*s,3*s,25*s);FxMesh.end();
                FxMesh.begin(FxMesh.energy,t,0x602594,0xb779ee,a*(float)release,4,1,3,0,false);FxMesh.box(p.add(0,3*s,41*s),8*s,31*s,.15*s);FxMesh.end();
            }
            case ABYSSAL_DELUGE->{
                // The ocean is explicitly anchored to requested Y=0, not the aimed block's height.
                Vec3 sea=new Vec3(p.x,0,p.z);double tide=ease((phase-.25)/.73);double front=(1-tide)*100*s-tide*80*s,width=Math.min(f.radius()*.6,140*s);
                FxMesh.begin(FxMesh.surface,t,0x207a9c,0x8edce5,a*.85f,8,1,3,0,true);
                for(int k=0;k<32;k++){double x=(k/31.-.5)*width*2,height=(8+grow*32+3*Math.sin(k*.6+t))*(1-ease(after/65.))*s;
                    FxMesh.box(sea.add(x,0,front+Math.sin(k*.35)*3*s),width/31.+.1,height,5*s);}
                FxMesh.end();
            }
            case WORLD_SUNDER->{
                double split=ease((phase-.35)/.6),sink=ease(after/70.);
                matter(f,t,a,0x62594d);
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
                    double fault=x+.30*z+Math.sin(z*.7)*.25;if(Math.abs(fault)<.38)continue;
                    double side=Math.signum(fault),xx=(x*12+side*split*18)*s,zz=z*12*s;
                    double top=(.8+grow*(.5+.4*Math.sin(x*5+z*3))-sink*(8+Math.abs((x+z)%3)*2))*s;
                    Vec3 q=p.add(xx,-10*s+top,zz);FxMesh.tint(0x62594d,a);FxMesh.box(q,5.7*s,10*s,5.7*s);
                    FxMesh.tint(0x708464,a);Vec3 cap=p.add(xx,top+.03,zz);double w=5.7*s;
                    FxMesh.quad(cap.add(-w,0,-w),cap.add(w,0,-w),cap.add(w,0,w),cap.add(-w,0,w),new Vec3(0,1,0));
                }
                FxMesh.end();FxMesh.begin(FxMesh.surface,t,0xff8d24,0xb2290c,a,0,1,4,0,true);FxMesh.sphere(p.add(0,-15*s,0),(2+split*10)*s,20,12);FxMesh.end();
                FxMesh.begin(FxMesh.glow,t,0xffb149,0xff681c,a*.6f,3,1,.4f,0,false);
                for(int k=-8;k<8;k++){
                    double z=k*5*s,x=-.30*z+Math.sin(k*.7)*2*s;
                    FxMesh.ribbon(p.add(x,.2,z),p.add(-.30*(z+5*s)+Math.sin((k+1)*.7)*2*s,.2,z+5*s),(.25+split*.6)*s);
                }FxMesh.end();
            }
            case RUNE_APOCALYPSE->{
                matter(f,t,a,0x554054);for(int k=0;k<8;k++){double angle=k*Math.PI/4,r=65*s;FxMesh.box(p.add(Math.cos(angle)*r,0,Math.sin(angle)*r),2*s,(12+grow*35)*s*decay,2*s);}FxMesh.end();
                // Exactly one judgement seal; not a pile of floor, ceiling and satellite circles.
                FxMesh.begin(FxMesh.rune,t,f.spell.color,f.spell.secondary,a*.55f,23,t*.025f,(float)grow,8,true);FxMesh.disk(p.add(0,65*s,0),62*s*grow,0);FxMesh.end();
            }
            default->{}
        }
        if(after>=0){double u=ease(after/65.);FxMesh.begin(FxMesh.glow,t,f.spell.color,f.spell.secondary,a*.3f,1,1,.012f,0,false);FxMesh.disk(p.add(0,.35,0),Math.max(.1,f.radius()*u),0);FxMesh.end();}
    }
    private HighTierWorldRenderer(){}
}
