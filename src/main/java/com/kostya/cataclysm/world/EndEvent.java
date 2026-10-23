package com.kostya.cataclysm.world;

import com.kostya.cataclysm.*;
import com.kostya.cataclysm.network.SpellNetwork;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import com.mojang.logging.LogUtils;
import java.util.*;

/** Explicitly enabled, operator-started gameplay event. No persistent account/network changes. */
public final class EndEvent {
    public static final int DURATION=900;
    public static final java.util.List<Integer> WAVES=EndSchedule.WAVES;
    public static final ResourceKey<DamageType> TYPE=ResourceKey.create(Registries.DAMAGE_TYPE,Cataclysm.id("end_of_all"));
    private record Victim(ResourceKey<Level> dimension,UUID id){}
    private static Event current;
    private static final class Event {
        final long seed;final UUID caster;final DestructionEngine.Handle terrain;
        final Map<UUID,Integer> players=new LinkedHashMap<>();
        final List<List<Victim>> mobs=new ArrayList<>();
        final Set<UUID> killed=new HashSet<>();int age,wave;
        Event(ServerPlayer p,Vec3 target,Vec3 direction){
            seed=p.getRandom().nextLong();caster=p.getUUID();
            var ids=new ArrayList<UUID>();for(var q:p.server.getPlayerList().getPlayers())if(!q.getUUID().equals(caster))ids.add(q.getUUID());
            players.putAll(EndSchedule.assign(ids,caster,seed));
            for(int i=0;i<6;i++)mobs.add(new ArrayList<>());
            int index=0;for(var level:p.server.getAllLevels())for(Entity e:level.getAllEntities())if(e instanceof LivingEntity&&!(e instanceof net.minecraft.world.entity.player.Player)&&e.isAlive())mobs.get(index++%6).add(new Victim(level.dimension(),e.getUUID()));
            terrain=DestructionEngine.prepare(p.serverLevel(),new DestructionPlan(Spell.END_OF_ALL,target,direction,seed,CataclysmConfig.RADIUS_SCALE.get()),caster);
        }
    }
    public static boolean active(){return current!=null;}
    public static boolean respawnLocked(ServerPlayer p){return current!=null;}
    public static boolean protects(UUID id){return current!=null&&current.wave<6&&current.caster.equals(id);}
    public static void protectDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e){if(protects(e.getEntity().getUUID()))e.setCanceled(true);}
    public static void protectDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e){if(protects(e.getEntity().getUUID())){e.setCanceled(true);e.getEntity().setHealth(Math.max(1,e.getEntity().getHealth()));}}
    public static int age(){return current==null?-1:current.age;}
    public static Map<UUID,Integer> groups(){return current==null?Map.of():Map.copyOf(current.players);}
    public static boolean start(ServerPlayer p,Vec3 target,Vec3 direction){
        if(!CataclysmConfig.END_ENABLED.get()){p.sendSystemMessage(Component.translatable("message.arcane_cataclysm.end_disabled"));return false;}
        if(!p.hasPermissions(2)){p.sendSystemMessage(Component.translatable("message.arcane_cataclysm.end_permission"));return false;}
        if(active())return false;
        int cost=CataclysmConfig.END_STARS.get(),available=0;
        for(var stack:p.getInventory().items)if(stack.is(Items.NETHER_STAR))available+=stack.getCount();
        if(!p.isCreative()&&available<cost){p.sendSystemMessage(Component.translatable("message.arcane_cataclysm.end_cost",cost));return false;}
        var event=new Event(p,target,direction);
        if(CataclysmConfig.DESTRUCTION.get()&&event.terrain==null)return false;
        if(!p.isCreative())for(var stack:p.getInventory().items)if(stack.is(Items.NETHER_STAR)&&cost>0){int n=Math.min(cost,stack.getCount());stack.shrink(n);cost-=n;}
        current=event;
        // Resolve the start-of-cast loaded-mob snapshot immediately, before chunks can unload.
        for(var group:event.mobs)for(var victim:group){var level=p.server.getLevel(victim.dimension);if(level!=null&&level.getEntity(victim.id) instanceof LivingEntity entity)kill(entity);}
        p.getCooldowns().addCooldown(Cataclysm.GRIMOIRE.get(),Spell.END_OF_ALL.cooldown);
        broadcast(p.server,true);
        LogUtils.getLogger().warn("[Cataclysm] END_BEGIN seed={} players={} mobs={} terrainChunks={}",event.seed,event.players.size(),event.mobs.stream().mapToInt(List::size).sum(),event.terrain==null?0:event.terrain.total);
        return true;
    }
    public static void tick(MinecraftServer server){
        var a=current;if(a==null)return;
        try {
            a.age++;
            // Joiners see the current scene. They enter the last group, never alter initial six-way allocation.
            for(var p:server.getPlayerList().getPlayers())a.players.putIfAbsent(p.getUUID(),5);
            while(a.wave<6&&a.age>=WAVES.get(a.wave)){
                int wave=a.wave++;
                for(var entry:a.players.entrySet())if(entry.getValue()<=wave&&!a.killed.contains(entry.getKey())){
                    var p=server.getPlayerList().getPlayer(entry.getKey());if(p!=null){kill(p);a.killed.add(entry.getKey());}
                }
                for(Victim v:a.mobs.get(wave)){var l=server.getLevel(v.dimension);if(l!=null&&l.getEntity(v.id) instanceof LivingEntity e&&e.isAlive())kill(e);}
                LogUtils.getLogger().info("[Cataclysm] END_WAVE {} age={} playersDead={}",wave+1,a.age,a.killed.size());
            }
            // A scheduled player disconnecting is not kicked. On rejoin during this event the due death is applied.
            for(var p:server.getPlayerList().getPlayers())if(a.players.get(p.getUUID())<a.wave&&!a.killed.contains(p.getUUID())){kill(p);a.killed.add(p.getUUID());}
            if(a.age>=DURATION){
                DestructionEngine.release(a.terrain);broadcast(server,false);
                LogUtils.getLogger().warn("[Cataclysm] END_COMPLETE; respawn unlocked; terrain continues under budget");unlock(server);
            }else if(a.age%10==0)broadcast(server,true);
        }catch(Throwable ex){
            // User-facing uninterruptibility is not an excuse for an eternal lock after a code error.
            LogUtils.getLogger().error("[Cataclysm] END failure; releasing transient respawn lock",ex);
            DestructionEngine.abort(a.terrain);broadcast(server,false);unlock(server);
        }
    }
    private static void unlock(MinecraftServer server){
        current=null;
        // Vanilla immediate respawn sends only once. Its earlier packet may have been blocked.
        for(var p:List.copyOf(server.getPlayerList().getPlayers()))if(!p.isAlive()&&p.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DO_IMMEDIATE_RESPAWN)){
            p.connection.handleClientCommand(new net.minecraft.network.protocol.game.ServerboundClientCommandPacket(net.minecraft.network.protocol.game.ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
        }
    }
    private static void kill(LivingEntity entity){
        if(!entity.isAlive())return;
        var source=new DamageSource(entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TYPE));
        entity.getCombatTracker().recordDamage(source,Float.MAX_VALUE);
        entity.setHealth(0);entity.die(source);
    }
    private static void broadcast(MinecraftServer server,boolean active){
        if(current==null)return;var packet=new SpellNetwork.EndState(current.seed,current.age,active);
        for(var p:server.getPlayerList().getPlayers())PacketDistributor.sendToPlayer(p,packet);
    }
    public static void reset(){current=null;}
    private EndEvent(){}
}
