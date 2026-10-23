package com.kostya.cataclysm.qa;
import com.kostya.cataclysm.*;
import com.kostya.cataclysm.world.*;
import com.kostya.cataclysm.client.EndCinematic;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.*;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;
import java.nio.file.*;
@EventBusSubscriber(modid=Cataclysm.MODID,value=Dist.CLIENT)
public final class V4GameQA {
 static int clientTicks,serverTicks,begin,lastWave;static boolean requested,started,configured;static volatile boolean done;static List<ServerPlayer> simulated=new ArrayList<>();static Map<UUID,Integer> groups;static DestructionEngine.Handle job;static Cow cow;static ServerPlayer caster;static int respawnCheck,scene,sceneBegin;static boolean endTerrainDone,capturedEnd;static DestructionEngine.Handle ordinary;static BlockPos barrier;static final Spell[] REGRESS={Spell.THOUSAND_BLADES,Spell.TEMPEST_WRATH,Spell.INFERNO_TIDE,Spell.LAST_CATACLYSM};
 static final class NoopListener extends ServerGamePacketListenerImpl {
  NoopListener(net.minecraft.server.MinecraftServer server,ServerPlayer p,GameProfile profile){super(server,new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false));}
  @Override public boolean hasChannel(net.minecraft.resources.ResourceLocation id){return false;}
  @Override public boolean hasChannel(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> id){return false;}
  @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
  @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
 }
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 @SubscribeEvent public static void client(ClientTickEvent.Post e){
  if(!Boolean.getBoolean("ac.v4game"))return;var m=Minecraft.getInstance();clientTicks++;m.options.pauseOnLostFocus=false;
  if(!requested&&clientTicks>40&&m.getOverlay()==null){requested=true;m.options.renderDistance().set(3);m.options.simulationDistance().set(5);var rules=new GameRules();if(Boolean.getBoolean("ac.v4immediate"))rules.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true,null);rules.getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0,null);rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);rules.getRule(GameRules.RULE_KEEPINVENTORY).set(true,null);
   m.createWorldOpenFlows().createFreshLevel("ArcaneV4Disposable-"+System.currentTimeMillis(),new LevelSettings("Arcane V4 disposable QA",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),new WorldOptions(713,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),m.screen);
  }
  if(started&&!capturedEnd&&EndEvent.age()>850){capturedEnd=true;Screenshot.grab(new java.io.File("qa"),"v4-in-world-death-scene.png",m.getMainRenderTarget(),x->{});}
  if(done){System.out.println("AC4_GAME_CLIENT_PASS");m.stop();}
  if(clientTicks>6000)throw new AssertionError("Client timeout");
 }
 @SuppressWarnings("unchecked") static List<ServerPlayer> rawPlayers(PlayerList list)throws Exception{var f=PlayerList.class.getDeclaredField("players");f.setAccessible(true);return (List<ServerPlayer>)f.get(list);}
 @SuppressWarnings("unchecked") static Map<UUID,ServerPlayer> rawMap(PlayerList list)throws Exception{var f=PlayerList.class.getDeclaredField("playersByUUID");f.setAccessible(true);return (Map<UUID,ServerPlayer>)f.get(list);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void server(ServerTickEvent.Post e){
  if(!Boolean.getBoolean("ac.v4game"))return;var server=e.getServer();serverTicks++;
  try{
   if(!started&&!configured&&serverTicks>60&&!server.getPlayerList().getPlayers().isEmpty()){
    caster=server.getPlayerList().getPlayers().getFirst();caster.setGameMode(GameType.CREATIVE);caster.teleportTo(0,-45,0);caster.getAbilities().flying=true;caster.onUpdateAbilities();
    CataclysmConfig.RADIUS_SCALE.set(.1);CataclysmConfig.GENERATE_CHUNKS.set(true);CataclysmConfig.DESTRUCTION.set(true);CataclysmConfig.BREAK_BEDROCK.set(true);CataclysmConfig.END_ENABLED.set(false);
    require(!SpellManager.castAt(caster,Spell.END_OF_ALL,new Vec3(0,-60,0),new Vec3(1,0,0)),"owner gate missing");CataclysmConfig.END_ENABLED.set(true);
    for(int i=0;i<6;i++){
     var level=i%2==0?server.overworld():server.getLevel(Level.NETHER);var profile=new GameProfile(new UUID(9004,i+1),"AC4Test"+i);
     var p=new ServerPlayer(server,level,profile,ClientInformation.createDefault());p.connection=new NoopListener(server,p,profile);p.setGameMode(GameType.values()[i%4]);p.setPos(16,70,16);rawPlayers(server.getPlayerList()).add(p);rawMap(server.getPlayerList()).put(p.getUUID(),p);simulated.add(p);
    }
    cow=new Cow(EntityType.COW,server.overworld());cow.moveTo(0,-59,3,0,0);cow.setNoAi(true);server.overworld().setChunkForced(0,0,true);server.overworld().getChunk(0,0);require(server.overworld().addFreshEntity(cow),"cow could not be added");configured=true;begin=serverTicks;return;
   }
   if(configured&&!started&&serverTicks-begin>10){
    if(server.overworld().getEntity(cow.getUUID())!=cow){require(serverTicks-begin<600,"test cow not loaded");return;}
    require(SpellManager.castAt(caster,Spell.END_OF_ALL,new Vec3(0,-60,0),new Vec3(1,0,0)),"cast rejected");require(!cow.isAlive(),"loaded cow survived");
    groups=EndEvent.groups();require(groups.get(caster.getUUID())==5,"caster group");job=DestructionEngine.latestFor(caster.getUUID());require(job!=null,"terrain missing");begin=serverTicks;started=true;
    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tick rate 100");System.out.println("AC4_GAME_BEGIN players="+groups.size()+" dimensions=overworld,nether modes=all radius="+job.plan.radius);
   }
   if(!started)return;int age=serverTicks-begin;
   if(age==120){server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"cataclysm cancel");require(EndEvent.active()&&!job.cancelled,"ordinary cancel stopped End");caster.hurt(caster.damageSources().genericKill(),Float.MAX_VALUE);require(caster.isAlive(),"caster died early");System.out.println("AC4_CASTER_PROTECTION_PASS");}
   if(age<900)require(job.blocks==0,"world edited before impact");
   int wave=0;for(int timestamp:EndSchedule.WAVES)if(age>=timestamp)wave++;
   if(wave>lastWave){for(var p:simulated)require(p.isAlive()==(groups.get(p.getUUID())>=wave),"wrong wave "+wave+" mode="+p.gameMode.getGameModeForPlayer()+" player="+p.getScoreboardName());require(caster.isAlive()==(wave<6),"caster timing");lastWave=wave;System.out.println("AC4_GAME_WAVE_PASS wave="+wave+" age="+age);}
   if(age==200){var victim=simulated.stream().filter(p->!p.isAlive()).findFirst().orElseThrow();victim.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));require(!victim.isAlive(),"respawn escaped");System.out.println("AC4_RESPAWN_BLOCK_PASS");}
   if(age==901){require(!EndEvent.active(),"event not cleared");require(!EndEvent.respawnLocked(caster),"respawn lock leaked");require(job.released,"impact not released");var id=caster.getUUID();if(Boolean.getBoolean("ac.v4immediate")){require(server.getPlayerList().getPlayer(id).isAlive(),"immediate respawn not restored");System.out.println("AC4_IMMEDIATE_RESPAWN_PASS");}caster.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));caster=server.getPlayerList().getPlayer(id);require(caster!=null&&caster.isAlive(),"respawn failed after event");caster.setGameMode(GameType.CREATIVE);caster.getAbilities().flying=true;caster.onUpdateAbilities();
    System.out.println("AC4_RELEASE_UNLOCK_RESPAWN_PASS prepared="+job.prepared+" total="+job.total);}
   if(!endTerrainDone&&age>910&&job.done){require(job.blocks>0&&job.failed==0&&job.skipped==0&&job.processed==job.total,"terrain incomplete");require(server.overworld().getBlockState(new BlockPos(0,-64,0)).isAir(),"bedrock survived");
    for(var p:simulated){var now=rawMap(server.getPlayerList()).remove(p.getUUID());rawPlayers(server.getPlayerList()).removeIf(q->q.getUUID().equals(p.getUUID()));if(now!=null)now.serverLevel().removePlayerImmediately(now,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}
    System.out.println("AC4_GAME_SERVER_ALL_PASS blocks="+job.blocks+" chunks="+job.committed+" failed="+job.failed+" skipped="+job.skipped);endTerrainDone=true;
   }
   if(endTerrainDone&&!done){
    if(ordinary==null&&scene<REGRESS.length){
     int x=300+scene*140;caster.teleportTo(x,-45,0);server.overworld().getChunk(x>>4,0);barrier=new BlockPos(x+1,-63,0);server.overworld().setBlockAndUpdate(barrier,Blocks.BARRIER.defaultBlockState());caster.getCooldowns().removeCooldown(Cataclysm.GRIMOIRE.get());
     require(SpellManager.castAt(caster,REGRESS[scene],new Vec3(x+.5,-60,.5),new Vec3(1,0,0)),"regression cast rejected");ordinary=DestructionEngine.latestFor(caster.getUUID());sceneBegin=serverTicks;require(ordinary!=null,"regression job missing");
    }
    if(ordinary!=null&&serverTicks-sceneBegin==ordinary.plan.spell.impact-2){require(ordinary.blocks==0&&ordinary.prepared>0,"precompute/early commit regression");}
    if(ordinary!=null&&ordinary.done&&serverTicks-sceneBegin>ordinary.plan.spell.duration+2){
     require(ordinary.failed==0&&ordinary.skipped==0,"regression incomplete");require(server.overworld().getBlockState(barrier).is(Blocks.BARRIER),"admin block destroyed");
     require(scene==3||ordinary.createdBlocks==0,"unwanted creation");var bedrock=server.overworld().getBlockState(BlockPos.containing(ordinary.plan.center.x,-64,ordinary.plan.center.z));require(scene==3?!bedrock.is(Blocks.BEDROCK):bedrock.is(Blocks.BEDROCK),"bedrock tier rule");
     System.out.println("AC4_REGRESSION_PASS spell="+REGRESS[scene].id+" blocks="+ordinary.blocks+" created="+ordinary.createdBlocks+" retriesFailed="+ordinary.failed);ordinary=null;scene++;
    }
    if(scene==REGRESS.length){done=true;System.out.println("AC4_GAME_ALL_PASS");}
   }
   if(age>9000)throw new AssertionError("Terrain timeout "+DestructionEngine.status());
  }catch(Throwable ex){ex.printStackTrace();System.exit(1);}
 }
}
