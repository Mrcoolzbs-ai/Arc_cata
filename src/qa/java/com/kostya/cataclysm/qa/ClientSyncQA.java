package com.kostya.cataclysm.qa;
import com.kostya.cataclysm.*;
import com.kostya.cataclysm.client.*;
import com.kostya.cataclysm.world.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.io.File;
@EventBusSubscriber(modid=Cataclysm.MODID,value=Dist.CLIENT)
public final class ClientSyncQA {
 static int ticks,age;static boolean setup,cast,passed;static volatile boolean started;static volatile Result result;
 static DestructionEngine.Handle job;static BlockPos sample;static BlockState original;
 record Result(BlockPos pos,BlockState state,long blocks){}
 static void require(boolean b,String message){if(!b)throw new AssertionError(message);}
 @SubscribeEvent public static void tick(ClientTickEvent.Post e){
  if(!Boolean.getBoolean("ac.syncqa"))return;var m=Minecraft.getInstance();if(m.player==null||m.level==null||m.getSingleplayerServer()==null)return;
  if(m.screen!=null)m.setScreen(null);m.options.pauseOnLostFocus=false;ticks++;
  if(!setup&&ticks>60){setup=true;new File("qa/screenshots").mkdirs();m.getSingleplayerServer().execute(()->{
   var p=m.getSingleplayerServer().getPlayerList().getPlayer(m.player.getUUID());p.setGameMode(GameType.CREATIVE);p.teleportTo(128,-40,-95);p.setYRot(0);p.setXRot(25);p.getAbilities().flying=true;p.onUpdateAbilities();
   p.setItemInHand(InteractionHand.MAIN_HAND,Cataclysm.GRIMOIRE.get().getDefaultInstance());CataclysmConfig.RADIUS_SCALE.set(.35);CataclysmConfig.GENERATE_CHUNKS.set(true);
  });}
  if(!cast&&ticks>160){cast=true;m.getSingleplayerServer().execute(()->{
   var p=m.getSingleplayerServer().getPlayerList().getPlayer(m.player.getUUID());p.setYRot(0);p.setXRot(25);p.getCooldowns().removeCooldown(Cataclysm.GRIMOIRE.get());
   ((GrimoireItem)p.getMainHandItem().getItem()).use(p.level(),p,InteractionHand.MAIN_HAND);job=DestructionEngine.latestFor(p.getUUID());require(job!=null,"Real client player's item cast rejected");
   sample=new BlockPos((int)Math.floor(job.plan.center.x),-61,(int)Math.floor(job.plan.center.z));original=p.level().getBlockState(sample);require(!original.isAir(),"Invalid sample fixture");started=true;System.out.println("AC2_CLIENT_ITEM_BEGIN target="+job.plan.center);
  });}
  if(started){age++;
   if(age>30&&age<150){double dist=m.gameRenderer.getMainCamera().getPosition().distanceTo(m.player.getEyePosition());require(dist<9,"Camera moved outside player chunk vicinity: "+dist);}
   if(age%20==0)m.getSingleplayerServer().execute(()->{if(job.done){var state=job.level.getBlockState(sample);require(state!=original,"Sample did not change");require(job.failed==0,"Failed chunks");result=new Result(sample,state,job.blocks);}});
   if(result!=null&&!passed&&age>200){var r=result;require(r.blocks>0,"No terrain changes");
    if(m.level.getBlockState(r.pos).equals(r.state)&&ClientEffects.latestProgress!=null&&ClientEffects.latestProgress.blocks()>0){
     passed=true;System.out.println("AC2_CLIENT_SYNC_AND_PROGRESS_PASS pos="+r.pos+" state="+r.state+" blocks="+r.blocks);Screenshot.grab(new File("qa"),"client-real-terrain.png",m.getMainRenderTarget(),x->{});
    }
   }
   if(passed&&age==300)Screenshot.grab(new File("qa"),"client-terrain-late.png",m.getMainRenderTarget(),x->{});
   if(passed&&age==305)ClientOptions.POST.set(false);
   if(passed&&age==310)Screenshot.grab(new File("qa"),"client-terrain-no-post.png",m.getMainRenderTarget(),x->{});
   if(passed&&age>385){System.out.println("AC2_CLIENT_CAMERA_SYNC_ALL_PASS");m.stop();}
   if(age>1200)throw new AssertionError("Client sync timeout");
  }
 }
}
