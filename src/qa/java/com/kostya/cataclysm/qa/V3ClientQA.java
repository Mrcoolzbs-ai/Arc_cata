package com.kostya.cataclysm.qa;
import com.kostya.cataclysm.*;
import com.kostya.cataclysm.client.*;
import com.kostya.cataclysm.world.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.io.File;
@EventBusSubscriber(modid=Cataclysm.MODID,value=Dist.CLIENT)
public final class V3ClientQA {
 static int ticks,age;static boolean setup,cast,passed,spectator;static volatile boolean started;static volatile Result result;
 static DestructionEngine.Handle job;static BlockPos sample;
 record Result(BlockPos pos,BlockState state,long blocks){}
 static void require(boolean b,String m){if(!b)throw new AssertionError(m);}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){
  if(!Boolean.getBoolean("ac.v3sync"))return;var m=Minecraft.getInstance();if(m.player==null||m.level==null||m.getSingleplayerServer()==null)return;
  if(m.screen!=null)m.setScreen(null);m.options.pauseOnLostFocus=false;ticks++;
  if(!setup&&ticks>60){setup=true;new File("qa/screenshots").mkdirs();m.getSingleplayerServer().execute(()->{var p=m.getSingleplayerServer().getPlayerList().getPlayer(m.player.getUUID());p.setGameMode(GameType.CREATIVE);p.teleportTo(96,85,-115);p.setYRot(0);p.setXRot(25);p.getAbilities().flying=true;p.onUpdateAbilities();var item=Cataclysm.GRIMOIRE.get().getDefaultInstance();item.set(Cataclysm.SELECTED.get(),Spell.GLACIAL_RAGNAROK.ordinal());p.setItemInHand(InteractionHand.MAIN_HAND,item);CataclysmConfig.RADIUS_SCALE.set(.25);CataclysmConfig.GENERATE_CHUNKS.set(true);});}
  if(!cast&&ticks>180){cast=true;m.getSingleplayerServer().execute(()->{var p=m.getSingleplayerServer().getPlayerList().getPlayer(m.player.getUUID());p.setYRot(0);p.setXRot(25);p.getCooldowns().removeCooldown(Cataclysm.GRIMOIRE.get());((GrimoireItem)p.getMainHandItem().getItem()).use(p.level(),p,InteractionHand.MAIN_HAND);job=DestructionEngine.latestFor(p.getUUID());require(job!=null,"Real snow-grave cast rejected");sample=BlockPos.containing(job.plan.center).above(2);started=true;System.out.println("AC3_CLIENT_ITEM_BEGIN target="+job.plan.center);});}
  if(started){age++;
   if(age%20==0)m.getSingleplayerServer().execute(()->{if(job.done){require(job.level.getBiome(sample).is(Biomes.ICE_SPIKES),"Server biome");var state=job.level.getBlockState(sample);require(!state.isAir(),"No new ice above former ground");result=new Result(sample,state,job.blocks);}});
   if(result!=null&&!passed){var r=result;if(m.level.getBlockState(r.pos).equals(r.state)&&m.level.getBiome(r.pos).is(Biomes.ICE_SPIKES)&&ClientEffects.latestProgress!=null&&ClientEffects.latestProgress.blocks()>0){passed=true;System.out.println("AC3_CLIENT_BIOME_STRUCTURE_PROGRESS_PASS pos="+r.pos+" state="+r.state+" blocks="+r.blocks);Screenshot.grab(new File("qa"),"v3-client-ice-spikes.png",m.getMainRenderTarget(),x->{});}}
   if(passed&&!spectator&&age>500){spectator=true;m.getSingleplayerServer().execute(()->m.getSingleplayerServer().getPlayerList().getPlayer(m.player.getUUID()).setGameMode(GameType.SPECTATOR));}
   if(spectator&&age>530){require(m.player.isSpectator(),"Gamemode change not received");require(!ClientEffects.hudVisible(),"Spectator HUD visible");require(ClientEffects.cinematic()==null,"Spectator camera captured");System.out.println("AC3_CLIENT_SPECTATOR_HUD_PASS");Screenshot.grab(new File("qa"),"v3-spectator-no-hud.png",m.getMainRenderTarget(),x->{});}
   if(spectator&&age>535){System.out.println("AC3_CLIENT_ALL_PASS");m.stop();}
   if(age>1800)throw new AssertionError("Client sync timeout");
  }
 }
}
