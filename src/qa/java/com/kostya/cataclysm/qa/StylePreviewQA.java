package com.kostya.cataclysm.qa;

import com.kostya.cataclysm.*;
import com.kostya.cataclysm.client.*;
import com.kostya.cataclysm.network.SpellNetwork;
import net.minecraft.client.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.nio.file.*;

/** Deliberately staged client preview: no server spell or terrain-destruction claims. */
@EventBusSubscriber(modid=Cataclysm.MODID,value=Dist.CLIENT)
public final class StylePreviewQA {
 static int ticks,scene=-1,frame,hold,globalFrame;static boolean requested,configured;static volatile boolean positioned;
 static final float[] SHOTS={24,76,-.62f,-.84f,10000-2,10000,10000+2,10000+5,10000+8,10000+14,10000+26,10000+55,10000+100,20000,30000,40000};
 static boolean enabled(){return Boolean.getBoolean("ac.styleqa");}
 static void require(boolean test,String msg){if(!test)throw new AssertionError(msg);}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){
  if(!enabled())return;var m=Minecraft.getInstance();ticks++;m.options.pauseOnLostFocus=false;
  if(!requested&&ticks>40&&m.getOverlay()==null){
   requested=true;m.options.renderDistance().set(4);m.options.simulationDistance().set(5);m.options.guiScale().set(Boolean.getBoolean("ac.stylecompact")?3:2);m.options.fov().set(70);m.options.bobView().set(false);m.options.framerateLimit().set(60);m.resizeDisplay();
   var rules=new GameRules();rules.getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0,null);rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
   m.createWorldOpenFlows().createFreshLevel("StyleDisposable-"+System.currentTimeMillis(),new LevelSettings("Style review / no terrain edits",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(5042,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),m.screen);
  }
  if(m.level==null||m.player==null||m.getSingleplayerServer()==null)return;
  if(!configured){configured=true;m.getSingleplayerServer().execute(()->{
   var s=m.getSingleplayerServer();var p=s.getPlayerList().getPlayer(m.player.getUUID());
   p.teleportTo(0,-56,-52);p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.setYRot(0);p.setXRot(0);p.getInventory().setItem(0,Cataclysm.GRIMOIRE.get().getDefaultInstance());p.getInventory().selected=0;
   s.overworld().setDayTime(1000);positioned=true;
  });ticks=0;}
  if(m.screen!=null)m.setScreen(null);
  if(positioned&&scene<0&&ticks>80){
   require(ScreenFX.invocation!=null,"invocation shader not loaded");
   ClientOptions.CAMERA.set(true);ClientOptions.POST.set(true);ClientOptions.ASTRAL.set(true);ClientOptions.FLASH.set(true);ClientOptions.SHAKE.set(true);ClientOptions.GLITCH.set(true);
   scene=Integer.getInteger("ac.styleonly",0);globalFrame=scene*144;start();
   if(Boolean.getBoolean("ac.stylecompact"))require(m.getWindow().getGuiScaledWidth()==320&&m.getWindow().getGuiScaledHeight()==240,"compact fixture dimensions");
   System.out.println("STYLE_CLIENT_READY gui="+m.getWindow().getGuiScaledWidth()+"x"+m.getWindow().getGuiScaledHeight()+" renderer="+com.mojang.blaze3d.systems.RenderSystem.getBackendDescription());
  }
 }
 static void start(){
  var m=Minecraft.getInstance();for(var f:ClientEffects.ACTIVE)if(f.chargeSound!=null)m.getSoundManager().stop(f.chargeSound);ClientEffects.ACTIVE.clear();
  if(Boolean.getBoolean("ac.stylecommandqa")){
   int current=scene;
   m.getSingleplayerServer().execute(()->{
    var server=m.getSingleplayerServer();var player=server.getPlayerList().getPlayer(m.player.getUUID());
    player.setYRot(0);player.setXRot(6);
    String before=com.kostya.cataclysm.world.DestructionEngine.status();
    try{
     if(current==0){
      boolean denied=false;
      try{server.getCommands().getDispatcher().execute("cataclysm preview heavens_lance",player.createCommandSourceStack().withPermission(1));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
      require(denied,"non-operator preview was accepted");
      require(server.getCommands().getDispatcher().execute("cataclysm preview tectonic_rift",player.createCommandSourceStack())==0,"preview scope exceeded first five");
      System.out.println("STYLE_PREVIEW_PERMISSION_SCOPE_PASS");
     }
     int result=server.getCommands().getDispatcher().execute("cataclysm preview "+Spell.values()[current].id,player.createCommandSourceStack());
     require(result==1,"preview command failed");
     require(before.equals(com.kostya.cataclysm.world.DestructionEngine.status()),"preview created terrain job");
     require(!com.kostya.cataclysm.world.EndEvent.active(),"preview started EndEvent");
     require(!player.getCooldowns().isOnCooldown(Cataclysm.GRIMOIRE.get()),"preview added cooldown");
     System.out.println("STYLE_COMMAND_PACKET_PASS spell="+Spell.values()[current].id);
    }catch(Throwable ex){ex.printStackTrace();Runtime.getRuntime().halt(1);}
   });
  }else ClientEffects.receive(new SpellNetwork.Fx(scene,m.player.getId(),new Vec3(0,-56,-52),new Vec3(0,-60,0),new Vec3(0,0,1),5042+scene,1f));
  frame=0;hold=0;
 }
 static int age(){
  var spell=Spell.values()[scene];
  if(Boolean.getBoolean("ac.styleimpactmovie"))return spell.impact-8+frame;
  if(Boolean.getBoolean("ac.stylemovie"))return (int)(frame/143.0*(spell.impact+100));
  float shot=SHOTS[frame];
  if(shot>=20000)return spell.impact+5;
  if(shot>9000)return spell.impact+(int)shot-10000;
  return shot>=0?(int)shot:(int)(-shot*spell.impact);
 }
 @SubscribeEvent public static void before(RenderFrameEvent.Pre e){
  if(!enabled()||scene<0||scene>=5||ClientEffects.ACTIVE.isEmpty())return;
  ClientEffects.ACTIVE.getLast().age=age();
  if(!Boolean.getBoolean("ac.stylemovie")){
   float shot=SHOTS[frame];
   ClientOptions.FLASH.set(shot!=20000);ClientOptions.GLITCH.set(shot!=30000);ClientOptions.SHAKE.set(shot!=40000);
   if(shot==40000)require(ClientEffects.shake(0)==0,"shake opt-out ignored");
  }
 }
 @SubscribeEvent public static void after(RenderFrameEvent.Post e){
  if(!enabled()||scene<0||scene>=5||ClientEffects.ACTIVE.isEmpty())return;
  var m=Minecraft.getInstance();if(m.level==null||m.screen!=null||m.getOverlay()!=null)return;
  boolean movie=Boolean.getBoolean("ac.stylemovie");if(!movie&&++hold<3)return;hold=0;
  try{
   Path dir=Path.of(Boolean.getBoolean("ac.stylecompact")?"qa/style-compact":Boolean.getBoolean("ac.styleimpactmovie")?"qa/impact-movie":movie?"qa/style-movie":"qa/style-shots");Files.createDirectories(dir);
   String file=movie?String.format("%05d.png",globalFrame++):String.format("%02d-%s-%02d-age-%03d.png",scene,Spell.values()[scene].id,frame,age());
   try(var image=Screenshot.takeScreenshot(m.getMainRenderTarget())){image.writeToFile(dir.resolve(file));}
   frame++;
   if(frame==(movie?(Boolean.getBoolean("ac.styleimpactmovie")?48:144):SHOTS.length)){
    System.out.println("STYLE_SCENE_PASS spell="+Spell.values()[scene].id+" frames="+frame);scene++;
    if(scene<5&&!Boolean.getBoolean("ac.styleonly.enabled"))start();else{
     var f=ClientEffects.ACTIVE.getLast();
     ClientOptions.FLASH.set(true);ClientOptions.GLITCH.set(true);ClientOptions.SHAKE.set(true);
     require(InvocationDirector.supports(Spell.STARFALL)&&!InvocationDirector.supports(Spell.TECTONIC_RIFT)&&!InvocationDirector.supports(Spell.END_OF_ALL),"scope");
     ClientOptions.ASTRAL.set(false);require(InvocationDirector.theatre(f,150)==0,"astral opt-out ignored");ClientOptions.ASTRAL.set(true);
     ClientOptions.CAMERA.set(false);require(ClientEffects.cinematic()==null,"camera opt-out ignored");ClientOptions.CAMERA.set(true);
     f.skipped=true;require(ClientEffects.cinematic()==null,"skip ignored");
     System.out.println("STYLE_ALL_PASS (staged client visuals, first five only; no destructive-server claims)");m.stop();
    }
   }
  }catch(Throwable ex){ex.printStackTrace();Runtime.getRuntime().halt(1);}
 }
}
