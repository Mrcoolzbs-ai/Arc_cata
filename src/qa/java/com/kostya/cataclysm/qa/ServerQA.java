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
public final class ServerQA {
    static int ticks,scene,nextCast=26;static DestructionEngine.Handle handle;static boolean began;
    static void require(boolean p,String m){if(!p)throw new AssertionError(m);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        if(!Boolean.getBoolean("ac.serverqa"))return;ticks++;var server=e.getServer();var level=server.overworld();
        if(ticks==10){
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tick rate 100");
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamerule spawnChunkRadius 0");
            CataclysmConfig.RADIUS_SCALE.set(.12);CataclysmConfig.GENERATE_CHUNKS.set(false);
            // Old min+4 guard excludes ALL dirt/grass in the standard four-layer superflat.
            for(Spell s:Spell.values()){
                var old=new LegacyPlan(s,new Vec3(0,-60,0),new Vec3(0,0,1),123,.12);
                var c=old.column(0,0,-61,-64,320);require(c==null||c.bottom()>-61,"Legacy reproduction no longer matches");
            }
            System.out.println("AC2_LEGACY_THIN_FLAT_BUG_REPRODUCED all=10");
        }
        if(ticks>=nextCast&&!began){
            began=true;nextCast=ticks+Spell.values()[scene].duration+5;int x=scene*160;
            // This synchronous generation is fixture setup only, not production spell code.
            for(int cx=(x-64)>>4;cx<=(x+64)>>4;cx++)for(int cz=-4;cz<=4;cz++)level.getChunk(cx,cz);
            var player=FakePlayerFactory.getMinecraft(level);player.setGameMode(GameType.CREATIVE);
            player.setPos(x,-54,-16);player.setYRot(0);player.setXRot(42);player.getCooldowns().removeCooldown(Cataclysm.GRIMOIRE.get());
            var item=Cataclysm.GRIMOIRE.get().getDefaultInstance();item.set(Cataclysm.SELECTED.get(),scene);player.setItemInHand(InteractionHand.MAIN_HAND,item);
            ((GrimoireItem)item.getItem()).use(level,player,InteractionHand.MAIN_HAND);
            handle=DestructionEngine.latestFor(player.getUUID());require(handle!=null,"Actual item use did not create terrain job: "+scene);
            System.out.println("AC2_ITEM_BEGIN "+Spell.values()[scene].id+" target="+handle.plan.center);
        }
        if(began&&handle!=null&&handle.done){
            require(handle.blocks>0,"No terrain changes for "+handle.plan.spell.id);
            require(handle.failed==0,"Chunk/geometry failure");
            int x=scene*160;require(level.getBlockState(new BlockPos(x,-64,0)).is(Blocks.BEDROCK),"Bedrock lost");
            System.out.println("AC2_ITEM_PASS "+handle.plan.spell.id+" blocks="+handle.blocks+" chunks="+handle.committed);
            scene++;began=false;handle=null;
            if(scene==10){System.out.println("AC2_ALL_THIN_FLAT_ITEM_CASTS_PASS "+DestructionEngine.status());server.halt(false);}
        }
        if(ticks>9000)throw new AssertionError("Timeout "+DestructionEngine.status());
    }
}
