package com.kostya.cataclysm.client;

import com.kostya.cataclysm.*;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.opengl.GL30;

/** Independent opaque film layer, like EndCinematic. Never teleports the world camera.
 * A bounded render target keeps procedural scene cost independent of monitor resolution.
 * V/C skip only presentation; they never cancel a server spell. EndCinematic is separate. */
@EventBusSubscriber(modid=Cataclysm.MODID,value=Dist.CLIENT)
public final class SpellFilm {
    public static final net.minecraft.client.renderer.ShaderInstance[] shaders=new net.minecraft.client.renderer.ShaderInstance[6];
    private static TextureTarget target;
    public static boolean epic(Spell s){return s.ordinal()>=9&&s.ordinal()<15;}
    public static boolean eligible(ClientEffects.Fx f){
        var m=Minecraft.getInstance();return epic(f.spell)&&!f.skipped&&f.age<f.spell.impact+100
            &&m.player!=null&&!m.player.isSpectator()&&f.packet.caster()==m.player.getId()
            &&ClientOptions.CAMERA.get()&&ClientOptions.ASTRAL.get()&&ClientOptions.POST.get();
    }
    public static ClientEffects.Fx current(){var f=ClientEffects.cinematic();return f!=null&&eligible(f)&&shaders[f.spell.ordinal()-9]!=null?f:null;}
    public static boolean running(){return current()!=null&&!EndCinematic.running();}
    public static void audio(ClientEffects.Fx f){
        var m=Minecraft.getInstance();boolean active=!EndCinematic.running()&&eligible(f)&&ClientEffects.cinematic()==f;
        if(active&&!f.scoreStarted){
            f.scoreStarted=true;m.getMusicManager().stopPlaying();
            if(f.chargeSound!=null)m.getSoundManager().stop(f.chargeSound);
            f.scoreSound=new SimpleSoundInstance(Cataclysm.id(f.spell.id+"_score"),SoundSource.RECORDS,.85f,1f,RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true);
            m.getSoundManager().play(f.scoreSound);
        }
        if(!active)stop(f);
    }
    public static void stop(ClientEffects.Fx f){if(f.scoreSound!=null)Minecraft.getInstance().getSoundManager().stop(f.scoreSound);f.scoreSound=null;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){if(target!=null){target.destroyBuffers();target=null;}}
    public static void layer(GuiGraphics g,DeltaTracker tracker){if(Minecraft.getInstance().screen==null)render(g,tracker.getGameTimeDeltaPartialTick(false));}
    @SubscribeEvent public static void screen(ScreenEvent.Render.Post e){render(e.getGuiGraphics(),e.getPartialTick());}
    private static void render(GuiGraphics g,float partial){
        var f=current();if(f==null||EndCinematic.running())return;
        var m=Minecraft.getInstance();var main=m.getMainRenderTarget();var shader=shaders[f.spell.ordinal()-9];
        int rw=Math.min(main.width,960),rh=Math.max(1,(int)Math.round(rw*(double)main.height/main.width));
        if(rh>540){rw=Math.max(1,rw*540/rh);rh=540;}
        if(target==null)target=new TextureTarget(rw,rh,false,Minecraft.ON_OSX);
        else if(target.width!=rw||target.height!=rh)target.resize(rw,rh,Minecraft.ON_OSX);
        g.flush();target.bindWrite(true);
        RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();RenderSystem.disableCull();
        RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(()->shader);
        float age=f.age(partial);
        shader.safeGetUniform("FxParams").set((float)f.spell.ordinal(),age/f.spell.impact,1f,ClientOptions.FLASH.get()?1f:0f);
        shader.safeGetUniform("FxTime").set(age/20f);shader.safeGetUniform("FxScreen").set((float)rw,(float)rh);
        shader.safeGetUniform("FxImpact").set(age-f.spell.impact,0f,0f,0f);
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        int w=g.guiWidth(),h=g.guiHeight();
        b.addVertex(0,0,0).setUv(0,0).setColor(-1);b.addVertex(0,h,0).setUv(0,1).setColor(-1);
        b.addVertex(w,h,0).setUv(1,1).setColor(-1);b.addVertex(w,0,0).setUv(1,0).setColor(-1);
        BufferUploader.drawWithShader(b.buildOrThrow());
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,target.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,main.frameBufferId);
        GL30.glBlitFramebuffer(0,0,rw,rh,0,0,main.width,main.height,GL30.GL_COLOR_BUFFER_BIT,GL30.GL_LINEAR);
        main.bindWrite(true);RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
    }
    private SpellFilm(){}
}
