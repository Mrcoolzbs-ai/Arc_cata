package com.kostya.cataclysm.client;

import com.kostya.cataclysm.*;
import com.kostya.cataclysm.network.SpellNetwork;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Standalone staged scene: it never moves the real camera into unloaded chunks. */
@EventBusSubscriber(modid=Cataclysm.MODID,value=Dist.CLIENT)
public final class EndCinematic {
    public static ShaderInstance shader;
    private static long seed;private static int age=-1,confirmedAge;private static boolean serverActive;
    private static SoundInstance score;
    public static boolean running(){return age>=0&&age<940;}
    public static void receive(SpellNetwork.EndState state){
        boolean fresh=!running()||seed!=state.seed();seed=state.seed();serverActive=state.active();confirmedAge=state.age();
        age=state.active()?state.age():900;
        if(fresh&&state.active()){
            var m=Minecraft.getInstance();m.getMusicManager().stopPlaying();
            score=new SimpleSoundInstance(Cataclysm.id("end_of_all_charge"),SoundSource.RECORDS,.85f,1f,RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true);
            m.getSoundManager().play(score);
        }
        if(!state.active()&&score!=null){Minecraft.getInstance().getSoundManager().stop(score);score=null;}
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        var m=Minecraft.getInstance();if(m.level==null||m.player==null){clear();return;}
        if(!running()||m.isPaused())return;
        // Heartbeats bound extrapolation: a stalled server cannot leave the client at a fake ending.
        if(!serverActive||age<Math.min(899,confirmedAge+20))age++;
        if(age>=940)clear();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    private static void clear(){if(score!=null)Minecraft.getInstance().getSoundManager().stop(score);score=null;age=-1;serverActive=false;}
    public static void layer(GuiGraphics g,DeltaTracker tracker){if(Minecraft.getInstance().screen==null)render(g,tracker.getGameTimeDeltaPartialTick(false));}
    @SubscribeEvent public static void onScreen(ScreenEvent.Render.Post e){render(e.getGuiGraphics(),e.getPartialTick());}
    @SubscribeEvent public static void clicks(ScreenEvent.MouseButtonPressed.Pre e){if(running()&&age>=900)e.setCanceled(true);}
    public static void render(GuiGraphics g,float partial){
        if(!running())return;int w=g.guiWidth(),h=g.guiHeight();float time=(age+partial)/20f;
        g.pose().pushPose();g.pose().translate(0,0,900);
        if(age>=900){
            double out=Math.max(0,(age+partial-930)/10.);int rows=48;
            RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
            for(int row=0;row<rows;row++){
                int y=row*h/rows,hh=(row+1)*h/rows-y;
                boolean flashy=ClientOptions.FLASH.get();double hash=hash(row+(int)(age/2)*73),shift=flashy?out*(hash-.5)*w*.5:0;
                if(flashy&&out>0&&hash<out*.55)continue;
                RenderSystem.setShaderColor(1,1,1,(float)(1-out));
                g.blit(Cataclysm.id("textures/gui/fake_disconnect.png"),(int)shift,y,w,hh,0,row*768f/rows,1366,768/rows,1366,768);
                if(flashy&&out>0&&row%7==0)g.fill(0,y,w,y+Math.max(1,hh/3),0x4433ffda);
            }
            g.flush();RenderSystem.setShaderColor(1,1,1,1);g.pose().popPose();return;
        }
        g.fill(0,0,w,h,0xff020207);
        if(shader!=null){
            g.flush();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();RenderSystem.disableBlend();
            RenderSystem.setShader(()->shader);RenderSystem.setShaderColor(1,1,1,1);
            shader.safeGetUniform("FxFlash").set(ClientOptions.FLASH.get()?1f:0f);shader.safeGetUniform("FxTime").set(time);shader.safeGetUniform("FxSeed").set((float)(seed&65535));shader.safeGetUniform("FxScreen").set((float)w,(float)h);
            var b=Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_TEX_COLOR);
            var mat=g.pose().last().pose();
            b.addVertex(mat,0,0,0).setUv(0,0).setColor(-1);b.addVertex(mat,0,h,0).setUv(0,1).setColor(-1);
            b.addVertex(mat,w,h,0).setUv(1,1).setColor(-1);b.addVertex(mat,w,0,0).setUv(1,0).setColor(-1);
            BufferUploader.drawWithShader(b.buildOrThrow());RenderSystem.depthMask(true);RenderSystem.enableDepthTest();RenderSystem.enableCull();RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();
        }
        if(time>=35)crosses(g,w,h,time);
        int bar=(int)(h*.06);g.fill(0,0,w,bar,0xff000000);g.fill(0,h-bar,w,h,0xff000000);
        if(time<4){int a=(int)(255*Math.min(1,Math.min(time,4-time)));g.drawCenteredString(Minecraft.getInstance().font,net.minecraft.network.chat.Component.translatable("spell.arcane_cataclysm.end_of_all"),w/2,h/2,(Math.max(4,a)<<24)|0xf3ddd9);}
        g.pose().popPose();
    }
    private static double hash(int n){double d=Math.sin(n*127.1+(seed&4095)) *43758.5453;return d-Math.floor(d);}
    private static void crosses(GuiGraphics g,int w,int h,float t){
        double pull=ClientEffects.smooth((t-35)/7),r=h*(1.1-.77*pull),cx=w*.5,cy=h*.54;
        double density=t<42?.03+.13*Math.max(0,(t-35)/7):.16+.84*ClientEffects.smooth((t-42)/3);
        int max=360;
        for(int i=0;i<max;i++){
            double threshold=hash(i*5+19);if(threshold>density)continue;
            double y=1-2*(i+.5)/max,rad=Math.sqrt(1-y*y),theta=i*2.399963+t*.035;
            double xx=rad*Math.cos(theta),zz=rad*Math.sin(theta);if(zz<.05)continue;
            double spawn=ClientEffects.smooth((density-threshold)*35),scale=(.35+zz*.65)*spawn;
            int x=(int)(cx+xx*r),sy=(int)(cy-y*r),len=(int)(h*(.035+.11*hash(i*3))*scale);
            if(len<2)continue;int arm=Math.max(2,len/3),th=Math.max(1,(int)(h*.0015*scale));
            // Wide transparent aureole -> thin mint shaft -> white centre.
            g.fill(x-th*4,sy-len,x+th*4,sy+len/4,0x164bffd3);
            g.fill(x-arm-2,sy-len/2-2,x+arm+2,sy-len/2+3,0x229dffe0);
            g.fill(x-th,sy-len,x+th,sy+len/4,0xc69fffe5);
            g.fill(x-arm,sy-len/2-th,x+arm,sy-len/2+th,0xe6c8fff0);
            g.fill(x,sy-len,x+1,sy+len/4,0xfff1fff7);
        }
    }
    private EndCinematic(){}
}
