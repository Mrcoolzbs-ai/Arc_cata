package com.kostya.cataclysm.client;

import com.kostya.cataclysm.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import java.util.Random;

@EventBusSubscriber(modid=Cataclysm.MODID,value=Dist.CLIENT)
public final class SpellRenderer {
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||ClientEffects.ACTIVE.isEmpty()||FxMesh.glow==null||FxMesh.rune==null||FxMesh.energy==null||FxMesh.surface==null||FxMesh.voidEnergy==null)return;
        float partial=e.getPartialTick().getGameTimeDeltaPartialTick(false);FxMesh.frame(e.getCamera());
        RenderSystem.enableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.setShaderColor(1,1,1,1);
        try{for(ClientEffects.Fx f:ClientEffects.ACTIVE)draw(f,partial);}finally{
            RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.defaultBlendFunc();RenderSystem.disableBlend();RenderSystem.setShaderColor(1,1,1,1);
        }
    }
    private static void draw(ClientEffects.Fx f,float partial){
        float age=f.age(partial),time=age/20f;double before=Math.min(1,age/f.spell.impact),after=age-f.spell.impact;
        float fade=(float)ClientEffects.smooth(Math.min(age/16,(f.spell.duration-age)/30d));
        Vec3 p=f.packet.target();double s=f.packet.scale(),r=f.radius();int c=f.spell.color,c2=f.spell.secondary;
        if(InvocationDirector.supports(f.spell)){FirstFiveRenderer.draw(f,partial);return;}
        // Each invocation starts with rotating, procedurally inscribed seals, not static particle spam.
        if(after<55){
            float a=fade*(float)(after<0?1:Math.max(0,1-after/55));
            begin(FxMesh.rune,time,c,c2,a,(float)(f.packet.seed()&4095),time*.13f,(float)Math.min(1,age/42),5+f.spell.ordinal()%4);
            FxMesh.disk(p.add(0,.12,0),Math.min(r*.8,75)*ClientEffects.smooth(before*2),0);
            FxMesh.disk(f.packet.origin().add(0,.07,0),3.2,0);FxMesh.end();
        }
        if(!f.spell.cosmic()){
            RitualRenderer.draw(f,time,age,fade);
            if(after>=0){shock(f,time,after,r,fade);debris(f,time,after,fade*.6f);}
            sparks(f,time,age,after,fade);return;
        }
        switch(f.spell){
            case STARFALL -> {
                if(after<5){
                    double progress=Math.pow(before,2.4);Vec3 meteor=p.add(-155*s*(1-progress),240*s*(1-progress)+4,95*s*(1-progress));
                    energy(time,c,c2,fade,0,1.6f,4,0);FxMesh.sphere(meteor,(9+before*25)*s,32,18);FxMesh.end();
                    begin(FxMesh.glow,time,c,c2,fade,3,4,.8f,0);
                    for(int i=0;i<7;i++){double a=i*2.399;Vec3 side=new Vec3(Math.cos(a)*i*s,Math.sin(a)*i*s,Math.sin(a+.5)*i*s);
                        FxMesh.ribbon(meteor.add(side),meteor.add(side).add(-110*s,170*s,70*s),(4-i*.35)*s);}
                    FxMesh.end();orb(time,c,c2,meteor,52*s,fade*.35f,2);
                }
                if(after>=0)blast(f,time,after,fade,1);
            }
            case SINGULARITY -> {
                Vec3 core=p.add(0,24*s,0);double size=(6+before*43)*s;
                if(after>=0)size*=Math.max(.02,1-after/60d);
                if(size>.5){
                    begin(FxMesh.voidEnergy,time,c,c2,fade,1,.9f,4,0);FxMesh.sphere(core,size,40,24);FxMesh.end();
                    begin(FxMesh.glow,time,c,c2,fade,5,.28f,1.5f,0);FxMesh.disk(core,size*2.5,time*.07);FxMesh.end();
                    orbitMotes(f,time,core,size*2.8,fade,true);
                }
                if(after>=0)blast(f,time,after,fade,.7);
            }
            case SOLAR_NOVA -> {
                Vec3 sun=p.add(0,65*s,0);
                if(after<10){double size=(6+before*48)*s;energy(time,c,c2,fade,0,.8f,6,0);FxMesh.sphere(sun,size,40,24);FxMesh.end();orb(time,c,c2,sun,size*2,fade*.6f,2);orbitMotes(f,time,sun,size*1.7,fade,false);}
                if(after>=0){double expand=Math.min(1,after/48d);energy(time,c,c2,fade*(float)(1-expand)*.5f,0,3,8,1);FxMesh.sphere(sun,12*s+r*1.6*expand,48,24);FxMesh.end();blast(f,time,after,fade,1.4);}
            }
            default->{}
        }
        ceremonialArcs(f,time,age,after,fade);
        sparks(f,time,age,after,fade);
    }
    private static void ceremonialArcs(ClientEffects.Fx f,float time,float age,double after,float fade){
        Vec3 p=f.packet.target();double s=f.packet.scale();
        if(after<0&&age>22){
            double rise=ClientEffects.smooth((age-22)/(f.spell.impact-22d));
            Vec3 focus=p.add(0,(f.spell==Spell.LAST_CATACLYSM?100:35)*s,0);
            begin(FxMesh.glow,time,f.spell.color,f.spell.secondary,fade*.5f,3,2,.6f,0);
            int count=f.spell==Spell.LAST_CATACLYSM?5:2;
            for(int i=0;i<count;i++)FxMesh.orbitRing(focus,(18+i*12)*s*rise,.35+i*.65,time*(i%2==0?.12:-.12),64,.25*s);
            FxMesh.end();
            // Vertical satellites inscribe a three-dimensional volume, not just one floor decal.
            begin(FxMesh.rune,time,f.spell.color,f.spell.secondary,fade*.55f,11,time*.12f,(float)rise,6);
            if(f.spell==Spell.LAST_CATACLYSM||f.spell==Spell.HEAVENS_LANCE){
                for(int i=0;i<4;i++){double a=i*Math.PI/2+time*.08;FxMesh.disk(p.add(Math.cos(a)*60*s,42*s,Math.sin(a)*60*s),18*s,0);}
            }FxMesh.end();
        }
        if(after>=0&&after<100){
            float a=fade*(float)(1-after/100);double r=f.radius()*Math.min(1,after/65);
            begin(FxMesh.glow,time,f.spell.color,f.spell.secondary,a*.65f,1,.87f,.024f,0);
            FxMesh.disk(p.add(0,2*s,0),Math.max(1,r),0);FxMesh.disk(p.add(0,7*s,0),Math.max(1,r*.76),0);FxMesh.end();
            if(f.spell==Spell.INFERNO_TIDE){energy(time,f.spell.color,f.spell.secondary,a*.65f,5,2,9,1);FxMesh.wall(p,r*.75,20*s,72);FxMesh.wall(p,r*.5,38*s,64);FxMesh.end();}
        }
    }
    private static void begin(ShaderInstance shader,float time,int c,int c2,float alpha,float x,float y,float z,float w){FxMesh.begin(shader,time,c,c2,alpha,x,y,z,w,shader==FxMesh.voidEnergy||shader==FxMesh.surface||shader==FxMesh.rune);}
    private static void energy(float time,int c,int c2,float alpha,float mode,float speed,float scale,float extra){begin((mode==0||mode==2)?FxMesh.surface:FxMesh.energy,time,c,c2,alpha,mode,speed,scale,extra);}
    private static void orb(float time,int c,int c2,Vec3 p,double size,float a,int mode){begin(FxMesh.glow,time,c,c2,a,mode,0,0,0);FxMesh.billboard(p,size);FxMesh.end();}
    private static void shock(ClientEffects.Fx f,float time,double after,double radius,float fade){
        double progress=Math.min(1,after/70);float a=fade*(float)(1-progress);
        begin(FxMesh.glow,time,f.spell.color,f.spell.secondary,a,1,.84f,.035f,1);
        FxMesh.disk(f.packet.target().add(0,.5+after*.025,0),Math.max(1,progress*radius),0);FxMesh.end();
    }
    private static void blast(ClientEffects.Fx f,float time,double after,float fade,double intensity){
        shock(f,time,after,f.radius()*1.5,fade);
        if(after<38)orb(time,f.spell.color,f.spell.secondary,f.packet.target().add(0,6,0),(12+after*2.5)*f.packet.scale()*intensity,fade*(float)(1-after/38),2);
        debris(f,time,after,fade);
    }
    private static void bolt(ClientEffects.Fx f,Vec3 p,double height,double width,int index,int frame){
        Vec3 last=p;
        for(int i=1;i<=14;i++){
            double offset=i==14?0:9*f.packet.scale();
            Vec3 q=p.add((SpellPatterns.hash(index,i,frame,f.packet.seed())-.5)*offset,height*i/14,(SpellPatterns.hash(index+7,i,frame,f.packet.seed())-.5)*offset);
            FxMesh.ribbon(last,q,width);last=q;
        }
    }
    private static void orbitMotes(ClientEffects.Fx f,float time,Vec3 p,double radius,float fade,boolean collapse){
        begin(FxMesh.glow,time,f.spell.color,f.spell.secondary,fade,3,2,.6f,0);
        for(int i=0;i<24;i++){
            double a=i*2.399+time*(collapse?1.3:.5),r=radius*(.7+.3*Math.sin(i+time));
            Vec3 at=p.add(Math.cos(a)*r,Math.sin(a*.7)*radius*.3,Math.sin(a)*r);
            Vec3 tail=p.add(Math.cos(a-.3)*r,Math.sin((a-.3)*.7)*radius*.3,Math.sin(a-.3)*r);
            FxMesh.ribbon(at,tail,.45*f.packet.scale());
        }FxMesh.end();
    }
    private static void sparks(ClientEffects.Fx f,float time,float age,double after,float fade){
        int count=140*ClientOptions.DENSITY.get()/100;Random random=new Random(f.packet.seed());
        begin(FxMesh.glow,time,f.spell.color,f.spell.secondary,fade*.8f,0,0,0,0);
        for(int i=0;i<count;i++){
            double a=random.nextDouble()*Math.PI*2,r=f.radius()*(.15+random.nextDouble()*.8),speed=.2+random.nextDouble()*.4;
            double phase=(age*speed+i*13)%65;Vec3 at;
            if(after<0){double pull=1-phase/65;at=f.packet.target().add(Math.cos(a)*r*pull,phase*.65,Math.sin(a)*r*pull);}
            else {double k=after/20,vel=(.4+random.nextDouble()*.9);at=f.packet.target().add(Math.cos(a)*r*Math.min(1,k*.65),vel*after*.7-after*after*.004,Math.sin(a)*r*Math.min(1,k*.65));}
            FxMesh.billboard(at,(.25+random.nextDouble()*.7)*f.packet.scale());
        }FxMesh.end();
    }
    private static void debris(ClientEffects.Fx f,float time,double after,float fade){
        if(after>85)return;int count=48*ClientOptions.DENSITY.get()/100;Random random=new Random(f.packet.seed()+31);
        energy(time,0x64566e,f.spell.secondary,fade*.75f,2,1,3,0);
        for(int i=0;i<count;i++){
            double a=random.nextDouble()*Math.PI*2,v=.4+random.nextDouble()*1.6,k=after;
            Vec3 p=f.packet.target().add(Math.cos(a)*k*v,k*(.8+random.nextDouble())-k*k*.018,Math.sin(a)*k*v);
            if(p.y<f.packet.target().y-20)continue;FxMesh.sphere(p,(.4+random.nextDouble()*1.6)*f.packet.scale(),5,3);
        }FxMesh.end();
    }
    private SpellRenderer(){}
}
