package com.kostya.cataclysm.world;

import com.kostya.cataclysm.*;
import com.kostya.cataclysm.network.SpellNetwork;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class SpellManager {
    private static final List<Active> ACTIVE=new ArrayList<>();
    public static final ResourceKey<DamageType> ARCANE=ResourceKey.create(Registries.DAMAGE_TYPE,Cataclysm.id("cataclysm"));
    private static final class Active {
        final ServerLevel level;final UUID caster;final Spell spell;final Vec3 target,dir;final long seed;final float scale;final DestructionEngine.Handle terrain;int age;
        Active(ServerPlayer p,Spell s,Vec3 target,Vec3 dir,long seed,float scale){level=p.serverLevel();caster=p.getUUID();spell=s;this.target=target;this.dir=dir;this.seed=seed;this.scale=scale;terrain=DestructionEngine.prepare(level,new DestructionPlan(s,target,dir,seed,scale),p.getUUID());}
    }
    public static boolean cast(ServerPlayer p,Spell spell){
        if(EndEvent.active()){p.sendSystemMessage(Component.translatable("message.arcane_cataclysm.end_busy"));return false;}
        if(CataclysmConfig.REQUIRE_OPERATOR.get()&&!p.server.isSingleplayer()&&!p.hasPermissions(2)&&!p.isCreative()){
            p.displayClientMessage(Component.translatable("message.arcane_cataclysm.permission"),true);return false;
        }
        if(p.isSpectator()||p.getCooldowns().isOnCooldown(Cataclysm.GRIMOIRE.get()))return false;
        if(ACTIVE.size()>=CataclysmConfig.MAX_ACTIVE.get()){
            p.displayClientMessage(Component.translatable("message.arcane_cataclysm.busy"),true);return false;
        }
        Vec3 origin=p.getEyePosition(),direction=p.getLookAngle();
        // Clamp the ray to loaded FULL chunks, so aiming cannot synchronously generate terrain.
        double reach=0;
        for(double d=1;d<=192;d++){
            Vec3 q=origin.add(direction.scale(d));
            if(p.serverLevel().getChunkSource().getChunkNow(net.minecraft.util.Mth.floor(q.x)>>4,net.minecraft.util.Mth.floor(q.z)>>4)==null)break;
            reach=d;
        }
        if(reach<4)return false;
        var hit=p.serverLevel().clip(new ClipContext(origin,origin.add(direction.scale(reach)),ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,p));
        Vec3 target=hit.getType()==HitResult.Type.MISS?origin.add(direction.scale(Math.min(96,reach))):hit.getLocation();
        if(hit.getType()==HitResult.Type.MISS){
            var chunk=p.serverLevel().getChunkSource().getChunkNow(net.minecraft.util.Mth.floor(target.x)>>4,net.minecraft.util.Mth.floor(target.z)>>4);
            if(chunk==null)return false;
            int y=chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,net.minecraft.util.Mth.floor(target.x)&15,net.minecraft.util.Mth.floor(target.z)&15)+1;
            target=new Vec3(target.x,y,target.z);
        }
        com.mojang.logging.LogUtils.getLogger().info("[Cataclysm] GRIMOIRE {} player={} target={} minY={} destruction={}",spell.id,p.getScoreboardName(),target,p.serverLevel().getMinBuildHeight(),CataclysmConfig.DESTRUCTION.get());
        return castAt(p,spell,target,direction);
    }
    public static boolean castAt(ServerPlayer p,Spell spell,Vec3 target,Vec3 dir){
        if(p.isSpectator())return false;
        if(EndEvent.active())return false;
        if(!Double.isFinite(target.x)||!Double.isFinite(target.y)||!Double.isFinite(target.z)||p.position().distanceToSqr(target)>512*512||!p.serverLevel().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(target)))return false;
        if(ACTIVE.size()>=CataclysmConfig.MAX_ACTIVE.get())return false;
        if(spell==Spell.END_OF_ALL)return EndEvent.start(p,target,dir);
        float scale=CataclysmConfig.RADIUS_SCALE.get().floatValue();long seed=p.getRandom().nextLong();
        Active active=new Active(p,spell,target,dir,seed,scale);
        if(CataclysmConfig.DESTRUCTION.get()&&active.terrain==null){p.displayClientMessage(Component.translatable("message.arcane_cataclysm.busy"),true);return false;}
        ACTIVE.add(active);
        if(!CataclysmConfig.DESTRUCTION.get())p.sendSystemMessage(Component.translatable("message.arcane_cataclysm.visual_only"));
        p.getCooldowns().addCooldown(Cataclysm.GRIMOIRE.get(),spell.cooldown);
        var packet=new SpellNetwork.Fx(spell.ordinal(),p.getId(),p.position(),target,dir,seed,scale);
        for(ServerPlayer viewer:p.serverLevel().players())if(viewer.position().distanceToSqr(target)<1024*1024||viewer==p)PacketDistributor.sendToPlayer(viewer,packet);
        return true;
    }
    public static void tick(ServerTickEvent.Post e){
        EndEvent.tick(e.getServer());
        for(var it=ACTIVE.iterator();it.hasNext();){
            Active a=it.next();a.age++;
            Entity caster=a.level.getPlayerByUUID(a.caster);
            // Caster protection persists for the cinematic; other players are not protected by default against terrain loss.
            if(caster instanceof ServerPlayer p){p.fallDistance=0;p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,30,4,false,false));}
            if(a.age==a.spell.impact){
                DestructionEngine.release(a.terrain);
                impact(a,caster);
            }
            if(a.age>=a.spell.duration)it.remove();
        }
        DestructionEngine.tick();
    }
    /** Whole horizontal impact footprint, full build-height cylinder, no distance falloff. */
    public static float impactDamage(Spell spell){return (float)(Math.max(1000,CataclysmConfig.DAMAGE.get())*(1+(spell.tier()-1)*.01));}
    private static void impact(Active a,Entity caster){
        var hits=a.terrain==null?new HashSet<UUID>():a.terrain.damagedEntities;
        int affected=AreaDamage.apply(a.level,a.spell,a.target,a.scale,a.caster,hits);
        com.mojang.logging.LogUtils.getLogger().info("[Cataclysm] IMPACT {} damage={} entities={}",a.spell.id,impactDamage(a.spell),affected);
    }
    /** Private visual rehearsal. No Active/EndEvent, inventory, cooldown, damage or terrain job is created. */
    private static int preview(CommandSourceStack source,Spell spell)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        if(spell.ordinal()>=5){source.sendFailure(Component.translatable("command.arcane_cataclysm.preview_scope"));return 0;}
        var p=source.getPlayerOrException();Vec3 eye=p.getEyePosition(),dir=p.getLookAngle();double reach=0;
        for(int d=1;d<=80;d++){
            Vec3 q=eye.add(dir.scale(d));
            if(p.serverLevel().getChunkSource().getChunkNow(net.minecraft.util.Mth.floor(q.x)>>4,net.minecraft.util.Mth.floor(q.z)>>4)==null)break;
            reach=d;
        }
        if(reach<4)return 0;
        var hit=p.serverLevel().clip(new ClipContext(eye,eye.add(dir.scale(reach)),ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,p));
        Vec3 target=hit.getType()==HitResult.Type.MISS?eye.add(dir.scale(Math.min(52,reach))):hit.getLocation();
        if(hit.getType()==HitResult.Type.MISS){
            var chunk=p.serverLevel().getChunkSource().getChunkNow(net.minecraft.util.Mth.floor(target.x)>>4,net.minecraft.util.Mth.floor(target.z)>>4);
            if(chunk==null)return 0;
            int y=chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,net.minecraft.util.Mth.floor(target.x)&15,net.minecraft.util.Mth.floor(target.z)&15)+1;
            target=new Vec3(target.x,y,target.z);
        }
        PacketDistributor.sendToPlayer(p,new SpellNetwork.Fx(spell.ordinal(),p.getId(),p.position(),target,dir,p.getRandom().nextLong(),CataclysmConfig.RADIUS_SCALE.get().floatValue()));
        source.sendSuccess(()->Component.translatable("command.arcane_cataclysm.preview",spell.title()),false);
        return 1;
    }
    private static int endOfAllSetting(CommandSourceStack source,boolean enabled){
        CataclysmConfig.END_ENABLED.set(enabled);
        CataclysmConfig.SPEC.save();
        source.sendSuccess(()->Component.translatable("command.arcane_cataclysm.end_setting",Component.translatable(enabled?"command.arcane_cataclysm.enabled":"command.arcane_cataclysm.disabled")),true);
        return 1;
    }
    public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("cataclysm").requires(s->s.hasPermission(2))
            .then(Commands.literal("preview").then(Commands.argument("spell",StringArgumentType.word()).suggests((c,b)->{for(int i=0;i<5;i++)b.suggest(Spell.values()[i].id);return b.buildFuture();}).executes(c->{
                try{return preview(c.getSource(),Spell.byId(StringArgumentType.getString(c,"spell")));}catch(IllegalArgumentException ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
            })))
            .then(Commands.literal("endofall")
                .executes(c->{c.getSource().sendSuccess(()->Component.translatable("command.arcane_cataclysm.end_status",Component.translatable(CataclysmConfig.END_ENABLED.get()?"command.arcane_cataclysm.enabled":"command.arcane_cataclysm.disabled")),false);return 1;})
                .then(Commands.literal("toggle").executes(c->endOfAllSetting(c.getSource(),!CataclysmConfig.END_ENABLED.get())))
                .then(Commands.argument("enabled",BoolArgumentType.bool()).executes(c->endOfAllSetting(c.getSource(),BoolArgumentType.getBool(c,"enabled")))))
            .then(Commands.literal("give").executes(c->{c.getSource().getPlayerOrException().getInventory().add(Cataclysm.GRIMOIRE.get().getDefaultInstance());return 1;}))
            .then(Commands.literal("diagnostics").executes(c->{var p=c.getSource().getPlayerOrException();c.getSource().sendSuccess(()->Component.literal("destruction="+CataclysmConfig.DESTRUCTION.get()+", generation="+CataclysmConfig.GENERATE_CHUNKS.get()+", scale="+CataclysmConfig.RADIUS_SCALE.get()+", minY="+p.level().getMinBuildHeight()+", "+DestructionEngine.status()),false);return 1;}))
            .then(Commands.literal("status").executes(c->{c.getSource().sendSuccess(()->Component.literal("active spells="+ACTIVE.size()+", "+DestructionEngine.status()),false);return 1;}))
            .then(Commands.literal("cancel").executes(c->{ACTIVE.clear();DestructionEngine.cancel();c.getSource().sendSuccess(()->Component.literal("Ordinary casts and terrain work canceled. End of All is not canceled. Existing terrain edits cannot be undone."),true);return 1;}))
            .then(Commands.literal("cast").then(Commands.argument("spell",StringArgumentType.word()).suggests((c,b)->{for(Spell s:Spell.values())b.suggest(s.id);return b.buildFuture();}).executes(c->{
                try{return cast(c.getSource().getPlayerOrException(),Spell.byId(StringArgumentType.getString(c,"spell")))?1:0;}catch(IllegalArgumentException ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
            }))));
    }
    public static void stop(ServerStoppingEvent e){EndEvent.reset();ACTIVE.clear();DestructionEngine.shutdown();}
    private SpellManager(){}
}
