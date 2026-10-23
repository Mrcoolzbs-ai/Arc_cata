package com.kostya.cataclysm.qa;
import com.kostya.cataclysm.*;
import com.kostya.cataclysm.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
@EventBusSubscriber(modid=Cataclysm.MODID)
public final class V3ServerQA {
 static int tick,scene=Integer.getInteger("ac.v3start",0),next=30,begin;static DestructionEngine.Handle h;static Cow center,edge,out;static boolean damageChecked;static long clock;static float centerDamage,edgeDamage,outDamage;
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Cow cow(net.minecraft.server.level.ServerLevel level,double x,double z){
  level.getChunk((int)Math.floor(x)>>4,(int)Math.floor(z)>>4);Cow c=new Cow(EntityType.COW,level);c.moveTo(x,180,z,0,0);c.setNoGravity(true);c.setNoAi(true);c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);c.setHealth(1000);level.addFreshEntity(c);return c;
 }
 @SubscribeEvent public static void damage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e){
  if(!Boolean.getBoolean("ac.v3qa")||!e.getSource().is(SpellManager.ARCANE))return;
  if(center!=null&&e.getEntity().getUUID().equals(center.getUUID()))centerDamage=e.getOriginalAmount();if(edge!=null&&e.getEntity().getUUID().equals(edge.getUUID()))edgeDamage=e.getOriginalAmount();if(out!=null&&e.getEntity().getUUID().equals(out.getUUID()))outDamage=e.getOriginalAmount();
 }
 @SubscribeEvent public static void tick(ServerTickEvent.Post e){
  if(!Boolean.getBoolean("ac.v3qa"))return;tick++;var server=e.getServer();var level=server.overworld();
  if(tick==10){server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tick rate "+Integer.getInteger("ac.v3rate",100));server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamerule spawnChunkRadius 0");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamerule doMobSpawning false");
   double radius=0;for(Spell s:Spell.values()){require(s.radius>radius,"Catalogue not ordered");radius=s.radius;require(SpellManager.impactDamage(s)>=1000,"Low damage");}System.out.println("AC3_CATALOGUE_PASS spells="+Spell.values().length);
  }
  if(h==null&&tick>=next){
   int index=Math.min(scene,14),x=scene*320;level.getChunk(x>>4,-1);var p=FakePlayerFactory.getMinecraft(level);p.setGameMode(GameType.CREATIVE);p.setPos(x,74,-16);p.setYRot(0);p.setXRot(60);p.getCooldowns().removeCooldown(Cataclysm.GRIMOIRE.get());
   CataclysmConfig.RADIUS_SCALE.set(scene==15?.35:.1);CataclysmConfig.GENERATE_CHUNKS.set(true);CataclysmConfig.INSTANT_IMPACT.set(scene!=16);
   var item=Cataclysm.GRIMOIRE.get().getDefaultInstance();item.set(Cataclysm.SELECTED.get(),index);p.setItemInHand(InteractionHand.MAIN_HAND,item);((GrimoireItem)item.getItem()).use(level,p,InteractionHand.MAIN_HAND);
   h=DestructionEngine.latestFor(p.getUUID());require(h!=null,"Item cast missing "+scene);begin=tick;next=tick+h.plan.spell.duration+5;clock=System.nanoTime();damageChecked=false;centerDamage=edgeDamage=outDamage=0;
   double cx=h.plan.center.x,cz=h.plan.center.z,r=h.plan.radius;center=cow(level,cx,cz);edge=cow(level,cx+r*.94,cz);out=cow(level,cx+r+12,cz);
   System.out.println("AC3_ITEM_BEGIN scene="+scene+" spell="+h.plan.spell.id+" radius="+r);
  }
  if(h!=null&&tick==begin+h.plan.spell.impact-5){require(h.blocks==0,"Early changes");require(h.prepared>0,"Nothing calculated before impact");System.out.println("AC3_PREPARED_PASS scene="+scene+" prepared="+h.prepared+" total="+h.total);}
  if(h!=null&&!damageChecked&&h.done){damageChecked=true;float damage=SpellManager.impactDamage(h.plan.spell);require(centerDamage>=damage-.5,"Center damage missing "+centerDamage);require(Math.abs(centerDamage-edgeDamage)<1,"Edge falloff/vertical damage error "+edgeDamage);require(outDamage==0&&out.getHealth()==1000,"Outside damaged");System.out.println("AC3_FULL_CYLINDER_DAMAGE_PASS scene="+scene+" damage="+centerDamage);center.discard();edge.discard();out.discard();}
  if(h!=null&&h.done&&damageChecked){
   require(h.blocks>0,"No world changes");require(h.failed==0&&h.skipped==0,"Chunks missing");require(scene==16?h.burstChunks==0:h.burstChunks>0,"Wrong impact mode");
   BlockPos q=BlockPos.containing(h.plan.center);require(level.getBlockState(new BlockPos(q.getX(),-64,q.getZ())).is(Blocks.BEDROCK),"Bedrock lost");
   if(h.plan.spell==Spell.GLACIAL_RAGNAROK)require(level.getBiome(q).is(Biomes.ICE_SPIKES),"Biome not changed");
   if(h.plan.spell==Spell.INFERNO_TIDE)require(h.ignitions>0,"No real fires");
   if(h.plan.spell==Spell.GLACIAL_RAGNAROK||h.plan.spell.ordinal()>=10)require(h.createdBlocks>0,"No physical construction");
   System.out.println("AC3_ITEM_PASS scene="+scene+" spell="+h.plan.spell.id+" blocks="+h.blocks+" created="+h.createdBlocks+" destroyed="+h.destroyedBlocks+" burst="+h.burstChunks+" ignitions="+h.ignitions+" biomeChunks="+h.biomeChunks+" seconds="+(System.nanoTime()-clock)/1e9);
   scene++;h=null;if(scene==Integer.getInteger("ac.v3stop",17)){System.out.println("AC3_SERVER_ALL_PASS "+DestructionEngine.status());server.halt(false);}
  }
  if(tick>70000)throw new AssertionError("Timeout "+DestructionEngine.status());
 }
}
