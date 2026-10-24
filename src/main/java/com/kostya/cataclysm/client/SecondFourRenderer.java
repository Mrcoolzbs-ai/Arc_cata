package com.kostya.cataclysm.client;

import com.kostya.cataclysm.*;
import net.minecraft.world.phys.Vec3;

/** World-space counterparts of invocations 6–9. Visual geometry only, never blocks. */
public final class SecondFourRenderer {
    private static final double TAU=Math.PI*2;
    private static double ease(double x){return ClientEffects.smooth(x);}
    private static void solid(ClientEffects.Fx f,float t,float alpha,int color,int mode){FxMesh.begin(FxMesh.surface,t,color,f.spell.secondary,alpha,mode,1,4,0,true);}
    private static void light(ClientEffects.Fx f,float t,float alpha){FxMesh.begin(FxMesh.glow,t,f.spell.color,f.spell.secondary,alpha,3,1.5f,.4f,0,false);}
    public static void draw(ClientEffects.Fx f,float partial){
        float age=f.age(partial),t=age/20f;double charge=Math.min(1,age/f.spell.impact),after=age-f.spell.impact,s=f.packet.scale();
        double grow=ease((charge-.06)/.55),release=ease((charge-.72)/.28);
        float fade=(float)ease(Math.min(age/18.,(f.spell.duration-age)/35.));
        float hold=fade*(float)(after<0?1:Math.max(0,1-after/75.));Vec3 p=f.packet.target();
        FxMesh.begin(FxMesh.rune,t,f.spell.color,f.spell.secondary,hold*.65f,23,t*.08f,(float)grow,8,true);
        FxMesh.disk(f.packet.origin().add(0,.06,0),3.0,0);FxMesh.disk(p.add(0,.16,0),Math.min(f.radius()*.7,95*s)*grow,0);FxMesh.end();
        switch(f.spell){
            case TECTONIC_RIFT->{
                solid(f,t,hold,0x6e645f,6);
                for(int k=-7;k<=7;k++)for(int side=-1;side<=1;side+=2){
                    double x=side*(9+release*18)*s,y=(10+Math.sin(k*2.7)*8)*s*grow;
                    FxMesh.box(p.add(x,y,k*11*s),8*s,(12+Math.abs(k%3)*8)*s*grow,5*s);
                    FxMesh.crystal(p.add(x,y+12*s,k*11*s),14*s*grow,5*s,k*.3);
                }FxMesh.end();
                light(f,t,hold*.8f);for(int k=-12;k<12;k++){
                    Vec3 a=p.add(Math.sin(k*1.7)*4*s,.6,k*7*s),b=p.add(Math.sin((k+1)*1.7)*4*s,.6,(k+1)*7*s);
                    FxMesh.ribbon(a,b,(.5+release*2)*s);
                }FxMesh.end();
            }
            case INFERNO_TIDE->{
                // Several moving crests, with opaque dark crust and incandescent lips.
                solid(f,t,hold,0x561f20,0);
                for(int k=0;k<34;k++){double a=k*TAU/34,r=(26+release*27)*s;FxMesh.crystal(p.add(Math.cos(a)*r,2*s,Math.sin(a)*r),(20+14*Math.sin(a*3+t))*s*grow,5*s,a);}FxMesh.end();
                light(f,t,hold*.8f);for(int ring=0;ring<3;ring++){
                    Vec3 last=null;for(int k=0;k<=72;k++){double a=k*TAU/72,r=(22+ring*13+release*25)*s;Vec3 q=p.add(Math.cos(a)*r,(16+ring*5+8*Math.sin(a*3-t))*s*grow,Math.sin(a)*r);
                        if(last!=null)FxMesh.ribbon(last,q,1.1*s);last=q;}
                }FxMesh.end();
            }
            case SINGULARITY->{
                Vec3 center=p.add(0,34*s,0);double radius=(4+grow*18)*(1-release*.35)*s;
                FxMesh.begin(FxMesh.voidEnergy,t,0x6c3bba,0x77dfff,hold,1,1,4,0,true);FxMesh.sphere(center,radius,40,24);FxMesh.end();
                solid(f,t,hold,0x5e536f,6);for(int k=0;k<32;k++){
                    double a=k*2.399+t*(.25+k%3*.08),r=(35+k%4*9)*(1-release*.7)*s;
                    FxMesh.box(center.add(Math.cos(a)*r,Math.sin(a*1.7)*r*.25,Math.sin(a)*r),2*s,(3+k%4)*s,2*s);
                }FxMesh.end();
                light(f,t,hold*.65f);for(int ring=0;ring<3;ring++)FxMesh.orbitRing(center,radius*(1.5+ring*.5),.2+ring*.55,t*(ring%2==0?.3:-.2),96,.3*s);FxMesh.end();
            }
            case SOLAR_NOVA->{
                Vec3 center=p.add(0,58*s,0);double radius=(5+grow*28)*(1-release*.12)*s;
                FxMesh.begin(FxMesh.energy,t,0xffcf72,0xff391a,hold,0,.8f,6,0,false);FxMesh.sphere(center,radius,40,24);FxMesh.end();
                light(f,t,hold*.8f);for(int k=0;k<10;k++){
                    double a=k*TAU/10+t*.08;Vec3 v=new Vec3(Math.cos(a),Math.sin(a),.2*Math.sin(a*3));Vec3 last=center.add(v.scale(radius));
                    for(int q=1;q<=20;q++){double u=q/20.,arc=Math.sin(u*Math.PI);Vec3 next=center.add(v.scale(radius+arc*(16+release*35)*s)).add(0,0,(u-.5)*radius*1.3);FxMesh.ribbon(last,next,.45*s);last=next;}
                }FxMesh.end();
            }
            default->{}
        }
        if(after>=0)for(int k=0;k<3;k++){
            double u=Math.max(0,Math.min(1,(after-k*9)/(55+k*12.)));
            FxMesh.begin(FxMesh.glow,t,f.spell.color,f.spell.secondary,fade*(float)(1-u),1,.8f,.018f,0,false);
            FxMesh.disk(p.add(0,1+k*2,0),Math.max(.1,f.radius()*u),0);FxMesh.end();
        }
    }
    private SecondFourRenderer(){}
}
