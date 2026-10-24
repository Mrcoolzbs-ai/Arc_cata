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
                // Low grounded plates follow the actual packet fault direction; no floating crystals.
                Vec3 forward=f.packet.direction().multiply(1,0,1).normalize();if(forward.lengthSqr()<.1)forward=new Vec3(1,0,0);
                Vec3 across=new Vec3(-forward.z,0,forward.x);
                solid(f,t,hold,0x6e645f,7);
                for(int k=-8;k<=8;k++)for(int side=-1;side<=1;side+=2){
                    Vec3 q=p.add(forward.scale(k*10*s)).add(across.scale((side*(9+release*14)+Math.sin(k*.9)*2)*s));
                    faultPlate(q,forward,across,(4.3+.4*Math.sin(k*2.1))*s,(5.5+Math.abs(Math.sin(k*3.1))*1.5)*s,
                        (2+grow*(5+Math.sin(k*3.7)*2))*s,(.3+grow*.8)*s,-3*s,side);
                }FxMesh.end();
                light(f,t,hold*.65f);
                for(int k=-13;k<13;k++){
                    Vec3 a=p.add(forward.scale(k*7*s)).add(across.scale(Math.sin(k*1.7)*2*s)).add(0,.25,0);
                    Vec3 b=p.add(forward.scale((k+1)*7*s)).add(across.scale(Math.sin((k+1)*1.7)*2*s)).add(0,.25,0);
                    FxMesh.ribbon(a,b,(.2+release*.6)*s);
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
                FxMesh.begin(FxMesh.surface,t,0xffcf72,0xff391a,hold,0,.8f,6,0,true);FxMesh.sphere(center,radius,40,24);FxMesh.end();

            }
            default->{}
        }
        if(after>=0)for(int k=0;k<3;k++){
            double u=Math.max(0,Math.min(1,(after-k*9)/(55+k*12.)));
            FxMesh.begin(FxMesh.glow,t,f.spell.color,f.spell.secondary,fade*(float)(1-u),1,.8f,.018f,0,false);
            FxMesh.disk(p.add(0,1+k*2,0),Math.max(.1,f.radius()*u),0);FxMesh.end();
        }
    }
    /** A grounded tilted rock wedge, not a floating box or a continuous flat plank. */
    private static void faultPlate(Vec3 p,Vec3 forward,Vec3 across,double length,double width,double inner,double outer,double bottom,int side){
        Vec3 along=forward.scale(length),cross=across.scale(width*side);
        Vec3 a=p.subtract(along).subtract(cross).add(0,inner,0),b=p.add(along).subtract(cross).add(0,inner,0);
        Vec3 c=p.add(along).add(cross).add(0,outer,0),d=p.subtract(along).add(cross).add(0,outer,0);
        Vec3 aa=new Vec3(a.x,p.y+bottom,a.z),bb=new Vec3(b.x,p.y+bottom,b.z),cc=new Vec3(c.x,p.y+bottom,c.z),dd=new Vec3(d.x,p.y+bottom,d.z);
        FxMesh.quad(a,b,c,d,b.subtract(a).cross(d.subtract(a)).normalize());
        FxMesh.quad(aa,bb,b,a,across.scale(-side));FxMesh.quad(bb,cc,c,b,forward);
        FxMesh.quad(cc,dd,d,c,across.scale(side));FxMesh.quad(dd,aa,a,d,forward.scale(-1));
    }
    private SecondFourRenderer(){}
}
