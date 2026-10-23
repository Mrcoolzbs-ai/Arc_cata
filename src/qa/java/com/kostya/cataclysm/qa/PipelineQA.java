package com.kostya.cataclysm.qa;
import com.kostya.cataclysm.*;
import com.kostya.cataclysm.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
@EventBusSubscriber(modid=Cataclysm.MODID)
public final class PipelineQA {
 static int ticks,scene,next=30,castTick; static DestructionEngine.Handle h;static long clock;
 static void require(boolean b,String msg){if(!b)throw new AssertionError(msg);}
 @SubscribeEvent public static void tick(ServerTickEvent.Post e){
  if(!Boolean.getBoolean("ac.pipelineqa"))return;ticks++;var server=e.getServer();var level=server.overworld();
  if(ticks==10){server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tick rate 100");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamerule spawnChunkRadius 0");CataclysmConfig.GENERATE_CHUNKS.set(true);}
  if(h==null&&ticks>=next){
   int x=scene*256;level.getChunk(x>>4,-1);
   var p=FakePlayerFactory.getMinecraft(level);p.setGameMode(GameType.CREATIVE);p.setPos(x,70,-8);p.setYRot(0);p.setXRot(60);p.getCooldowns().removeCooldown(Cataclysm.GRIMOIRE.get());
   int index=Math.min(scene,9);CataclysmConfig.RADIUS_SCALE.set(scene==10?1.:.2);
   var item=Cataclysm.GRIMOIRE.get().getDefaultInstance();item.set(Cataclysm.SELECTED.get(),index);p.setItemInHand(InteractionHand.MAIN_HAND,item);
   ((GrimoireItem)item.getItem()).use(level,p,InteractionHand.MAIN_HAND);
   h=DestructionEngine.latestFor(p.getUUID());require(h!=null,"Cast not started "+scene);castTick=ticks;clock=System.nanoTime();next=ticks+Spell.values()[index].duration+5;
   System.out.println("AC2_PIPELINE_BEGIN scene="+scene+" spell="+h.plan.spell.id+" radius="+h.plan.radius+" chunks="+h.total);
  }
  if(h!=null&&ticks==castTick+50){require(h.blocks==0,"Changed before impact");require(h.prepared>0,"No precomputation");System.out.println("AC2_PRECOMPUTE_PASS scene="+scene+" prepared="+h.prepared);}
  if(h!=null&&h.done){
   require(h.blocks>0,"No changes");require(h.failed==0&&h.skipped==0,"Missing chunks");
   var center=BlockPos.containing(h.plan.center);require(level.getBlockState(new BlockPos(center.getX(),-64,center.getZ())).is(Blocks.BEDROCK),"Bedrock lost");
   System.out.println("AC2_PIPELINE_PASS scene="+scene+" blocks="+h.blocks+" chunks="+h.committed+" seconds="+(System.nanoTime()-clock)/1e9);
   h=null;scene++;if(scene==11){System.out.println("AC2_PIPELINE_ALL_PASS "+DestructionEngine.status());server.halt(false);}
  }
  if(ticks>60000)throw new AssertionError("Timeout "+DestructionEngine.status());
 }
}
