package com.kostya.cataclysm.client;

import com.kostya.cataclysm.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL30;

/** Astral inserts are a separate procedural scene, NOT a camera teleported into unloaded world chunks. */
public final class ScreenFX {
    public static ShaderInstance shader,invocation;
    private static TextureTarget copy;
    public static void render(GuiGraphics gui,DeltaTracker delta){
        if(SpellFilm.running()||shader==null||ClientEffects.ACTIVE.isEmpty()||!ClientOptions.POST.get())return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.player.isSpectator()||mc.options.hideGui)return;
        var f=ClientEffects.cinematic();if(f==null){
            for(var candidate:ClientEffects.ACTIVE)if(mc.player.position().distanceToSqr(candidate.packet.target())<400*400){f=candidate;break;}
        }
        if(f==null)return;
        float age=f.age(delta.getGameTimeDeltaPartialTick(false)),phase=age/f.spell.impact;
        float realm=ClientEffects.cinematic()==f&&f.spell.cosmic()&&ClientOptions.ASTRAL.get()?(float)(ClientEffects.smooth((age-22)/25)* (1-ClientEffects.smooth((age-(f.spell.impact-48))/28))):0;
        float shock=ClientOptions.FLASH.get()?(float)Math.exp(-Math.max(0,age-f.spell.impact)/13)*(age>=f.spell.impact?1:0):0;
        var main=mc.getMainRenderTarget();
        if(copy==null)copy=new TextureTarget(main.width,main.height,false,Minecraft.ON_OSX);
        else if(copy.width!=main.width||copy.height!=main.height)copy.resize(main.width,main.height,Minecraft.ON_OSX);
        gui.flush();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,copy.frameBufferId);
        GL30.glBlitFramebuffer(0,0,main.width,main.height,0,0,copy.width,copy.height,GL30.GL_COLOR_BUFFER_BIT,GL30.GL_NEAREST);
        main.bindWrite(true);
        RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableBlend();RenderSystem.disableCull();
        boolean directed=!SpellFilm.epic(f.spell)&&InvocationDirector.supports(f.spell)&&ClientEffects.cinematic()==f&&invocation!=null;
        ShaderInstance active=directed?invocation:shader;
        RenderSystem.setShaderColor(1,1,1,1);RenderSystem.setShader(()->active);RenderSystem.setShaderTexture(0,copy.getColorTextureId());
        active.safeGetUniform("FxParams").set((float)(directed?f.spell.ordinal():f.spell.visualId()),phase,directed?InvocationDirector.theatre(f,age):realm,directed?(ClientOptions.FLASH.get()?1f:0f):shock);
        active.safeGetUniform("FxColor2").set(((f.spell.color>>16)&255)/255f,((f.spell.color>>8)&255)/255f,(f.spell.color&255)/255f,ClientOptions.BLOOM.get().floatValue());
        // 0.7.2: secondary spell color feeds the opposite fringe band.
        active.safeGetUniform("FxColor3").set(((f.spell.secondary>>16)&255)/255f,((f.spell.secondary>>8)&255)/255f,(f.spell.secondary&255)/255f,1f);
        double power=ImpactDynamics.power(f.spell.tier(),f.spell.radius,f.packet.scale());
        // 0.7.3: a tier-scaled barrage of strikes hammers the envelopes; the shader clock follows the latest hit.
        float sinceHit=(float)ImpactDynamics.sinceHit(age-f.spell.impact,f.spell.tier());
        active.safeGetUniform("FxImpact").set(sinceHit,(float)power,
            (float)(ClientOptions.GLITCH.get()&&ClientOptions.FLASH.get()?ImpactDynamics.glitch(sinceHit,f.spell.tier()):0),
            ClientOptions.SHAKE.get()?(float)ImpactDynamics.shake(sinceHit,power,0):0f);
        active.safeGetUniform("FxTime").set(age/20f);
        active.safeGetUniform("FxScreen").set((float)main.width,(float)main.height);
        var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
        int w=gui.guiWidth(),h=gui.guiHeight();
        b.addVertex(0,0,0).setUv(0,0).setColor(-1);b.addVertex(0,h,0).setUv(0,1).setColor(-1);
        b.addVertex(w,h,0).setUv(1,1).setColor(-1);b.addVertex(w,0,0).setUv(1,0).setColor(-1);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShaderColor(1,1,1,1);
    }
    private ScreenFX(){}
}
