package com.kostya.cataclysm.client;

import com.kostya.cataclysm.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Authored timing for the nine ritual invocations. High-tier films use SpellFilm. Rendering only: the server's impact clock is untouched. */
public final class InvocationDirector {
    public static boolean supports(Spell spell){return spell.ordinal()<9;}
    public static float theatre(ClientEffects.Fx f,float age){
        if(!ClientOptions.ASTRAL.get()||!supports(f.spell))return 0;
        double in=42,out=f.spell.impact-48;
        return (float)(ClientEffects.smooth((age-in)/32.)*(1-ClientEffects.smooth((age-out)/32.)));
    }
    public static double fov(ClientEffects.Fx f,float partial){
        double age=f.age(partial),charge=age/f.spell.impact,after=age-f.spell.impact;
        // 0.7.0: deeper pre-impact squeeze, then a fast rebound punch instead of a 35-tick drift.
        return 1.02-.16*ClientEffects.smooth(charge/.55)+(after>=0?.20*Math.exp(-after*.12):0);
    }
    public static ClientEffects.View camera(Camera camera,ClientEffects.Fx f,float partial){
        var m=Minecraft.getInstance();double age=f.age(partial),charge=age/f.spell.impact,after=age-f.spell.impact;
        double blend=ClientEffects.smooth(Math.min(age/14.,(f.spell.duration-age)/30.));
        Vec3 eye=m.player.getEyePosition(partial),origin=f.packet.origin().add(0,1.4,0),target=f.packet.target();
        Vec3 forward=target.subtract(origin).multiply(1,0,1).normalize();if(forward.lengthSqr()<.01)forward=new Vec3(0,0,1);
        Vec3 side=new Vec3(-forward.z,0,forward.x);
        // 0.7.0: reveal rises faster; the pullback starts late but accelerates hard into the impact.
        double reveal=ClientEffects.smooth((age-16)/46.),wide=ClientEffects.smooth((age-(f.spell.impact-42))/26.);
        // Close performer / rising oblique wide / impact pullback. All real shots stay near the player.
        double orbit=Math.sin(charge*Math.PI)*1.7;
        Vec3 desired=eye.add(side.scale(3.2+orbit+wide*2.0)).add(forward.scale(-2.5-wide*6.)).add(0,.4+reveal*3.0+wide*2.,0);
        Vec3 look=origin.lerp(target.add(0,after<0?(f.spell==Spell.STARFALL?28:20)*f.packet.scale():1,0),reveal);
        var hit=m.level.clip(new ClipContext(eye,desired,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,m.player));
        if(hit.getType()!=HitResult.Type.MISS)desired=hit.getLocation().lerp(eye,.12);
        if(!m.level.hasChunkAt(net.minecraft.core.BlockPos.containing(desired)))desired=eye;
        Vec3 pos=camera.getPosition().lerp(desired,blend),dir=look.subtract(pos);
        float yaw=(float)(Math.atan2(dir.z,dir.x)*180/Math.PI-90),pitch=(float)(-Math.atan2(dir.y,dir.horizontalDistance())*180/Math.PI);
        yaw=camera.getYRot()+Mth.wrapDegrees(yaw-camera.getYRot())*(float)blend;
        pitch=Mth.lerp((float)blend,camera.getXRot(),pitch);
        // 0.7.0: a single hard roll flinch on impact; the shared tremor adds the rest.
        double flinch=after>=0&&after<10?Math.sin(after*.9)*Math.exp(-after*.5):0;
        return new ClientEffects.View(pos,yaw,pitch,(float)(flinch*.9));
    }
    public static void title(GuiGraphics g,ClientEffects.Fx f,float age,int bar){
        InvocationHud.draw(g,f,age,bar);
    }

    private InvocationDirector(){}
}
