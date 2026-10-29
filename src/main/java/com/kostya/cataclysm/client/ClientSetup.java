package com.kostya.cataclysm.client;
import com.kostya.cataclysm.*;
import com.kostya.cataclysm.network.SpellNetwork;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import org.lwjgl.glfw.GLFW;
import java.io.IOException;
@EventBusSubscriber(modid=Cataclysm.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ClientSetup {
    public static final KeyMapping CAMERA=new KeyMapping("key.arcane_cataclysm.camera",GLFW.GLFW_KEY_C,"key.categories.arcane_cataclysm");
    public static final KeyMapping SKIP=new KeyMapping("key.arcane_cataclysm.skip",GLFW.GLFW_KEY_V,"key.categories.arcane_cataclysm");
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->{SpellNetwork.clientReceiver=ClientEffects::receive;SpellNetwork.progressReceiver=ClientEffects::progress;SpellNetwork.strikeReceiver=ClientEffects::strike;SpellNetwork.endReceiver=EndCinematic::receive;});}
    @SubscribeEvent public static void keys(RegisterKeyMappingsEvent e){e.register(CAMERA);e.register(SKIP);}
    @SubscribeEvent public static void shaders(RegisterShadersEvent e)throws IOException{
        e.registerShader(new ShaderInstance(e.getResourceProvider(),Cataclysm.id("end_scene"),DefaultVertexFormat.POSITION_TEX_COLOR),s->EndCinematic.shader=s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),Cataclysm.id("invocation"),DefaultVertexFormat.POSITION_TEX_COLOR),s->ScreenFX.invocation=s);
        for(int id=9;id<15;id++){final int index=id-9;
            e.registerShader(new ShaderInstance(e.getResourceProvider(),Cataclysm.id(id==9?"saga":"film_"+id),DefaultVertexFormat.POSITION_TEX_COLOR),s->SpellFilm.shaders[index]=s);
        }
        e.registerShader(new ShaderInstance(e.getResourceProvider(),Cataclysm.id("ac_post"),DefaultVertexFormat.POSITION_TEX_COLOR),s->ScreenFX.shader=s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),Cataclysm.id("ac_glow"),DefaultVertexFormat.POSITION_TEX_COLOR),s->FxMesh.glow=s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),Cataclysm.id("ac_rune"),DefaultVertexFormat.POSITION_TEX_COLOR),s->FxMesh.rune=s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),Cataclysm.id("ac_energy"),DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),s->FxMesh.energy=s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),Cataclysm.id("ac_void"),DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),s->FxMesh.voidEnergy=s);
        e.registerShader(new ShaderInstance(e.getResourceProvider(),Cataclysm.id("ac_surface"),DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL),s->FxMesh.surface=s);
    }
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent e){e.registerBelowAll(Cataclysm.id("screen_fx"),ScreenFX::render);e.registerAboveAll(Cataclysm.id("spell_hud"),ClientEffects::hud);e.registerAboveAll(Cataclysm.id("spell_film"),SpellFilm::layer);e.registerAboveAll(Cataclysm.id("end_scene"),EndCinematic::layer);}
}
