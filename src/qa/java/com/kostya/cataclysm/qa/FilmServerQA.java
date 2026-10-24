package com.kostya.cataclysm.qa;

import com.kostya.cataclysm.*;
import com.kostya.cataclysm.world.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Small, disposable server pipeline regression, not a full-radius performance benchmark. */
@EventBusSubscriber(modid=Cataclysm.MODID)
public final class FilmServerQA {
    private static int ticks,stage,start,quiet;private static long settled;
    private static DestructionEngine.Handle handle;
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        if(!Boolean.getBoolean("ac.filmserverqa"))return;
        try{
            ticks++;var server=e.getServer();var level=server.overworld();
            if(ticks==5){
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamerule spawnChunkRadius 0");
                CataclysmConfig.GENERATE_CHUNKS.set(true);CataclysmConfig.INSTANT_IMPACT.set(true);
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.getChunk(x,z);
                handle=DestructionEngine.prepare(level,new DestructionPlan(Spell.VERDANT_RUIN,new Vec3(0,60,0),new Vec3(0,0,1),6101,.1),null);
                require(handle!=null,"life prepare");start=ticks;stage=1;
            }
            if(stage==1&&ticks-start==60){
                require(handle.blocks==0&&handle.prepared>0,"life pre-impact preparation");
                DestructionEngine.release(handle);
                require(handle.blocks==0&&handle.burstChunks==0,"life bypassed tick budget at impact");stage=2;
                System.out.println("AC061_LIFE_RELEASE_PASS instantImpact=true blocksAtRelease=0 burstChunks=0 prepared="+handle.prepared);
            }
            if(stage==2&&handle.done){
                require(handle.failed==0&&handle.skipped==0&&handle.blocks>0,"life job incomplete");
                require(DestructionEngine.isIdle(),"life left queued work/lighting");
                var c=handle.plan.column(0,0,60,level.getMinBuildHeight(),level.getMaxBuildHeight());
                require(!level.getBlockState(new BlockPos(0,c.floor(),0)).is(Blocks.DARK_OAK_WOOD),"persistent wood");
                require(level.getBlockState(new BlockPos(0,80,0)).isAir(),"life filled above terrain");
                System.out.println("AC061_LIFE_DONE_PASS blocks="+handle.blocks+" chunks="+handle.committed+" "+DestructionEngine.status());
                stage=3;quiet=ticks;settled=DestructionEngine.changedBlocks();
            }
            if(stage==3&&ticks-quiet==40){
                require(DestructionEngine.isIdle()&&DestructionEngine.changedBlocks()==settled,"life continued work after completion");
                CataclysmConfig.INSTANT_IMPACT.set(false);
                for(int x=7;x<=13;x++)for(int z=-3;z<=3;z++)level.getChunk(x,z);
                handle=DestructionEngine.prepare(level,new DestructionPlan(Spell.ABYSSAL_DELUGE,new Vec3(160,-40,0),new Vec3(0,0,1),6102,.1),null);
                require(handle!=null,"water prepare");start=ticks;stage=4;
                System.out.println("AC061_LIFE_IDLE_PASS ticks=40");
            }
            if(stage==4&&ticks-start==60){DestructionEngine.release(handle);stage=5;}
            if(stage==5&&handle.done){
                require(handle.failed==0&&handle.skipped==0&&handle.blocks>0,"water job incomplete");
                for(int y=-58;y<=0;y++)require(level.getBlockState(new BlockPos(160,y,0)).is(Blocks.WATER),"missing real water at "+y);
                for(int y=1;y<=60;y++)require(level.getBlockState(new BlockPos(160,y,0)).isAir(),"water/solid above sea surface "+y);
                require(DestructionEngine.isIdle(),"water left queued work");
                System.out.println("AC061_WATER_LIVE_PASS surface=0 inclusiveWater=-58..0 air=1..60 blocks="+handle.blocks+" chunks="+handle.committed);
                // Cancel during preparation and verify no retained job; shutdown covers capture ticket cleanup.
                handle=DestructionEngine.prepare(level,new DestructionPlan(Spell.VERDANT_RUIN,new Vec3(0,60,0),new Vec3(0,0,1),6103,.1),null);start=ticks;stage=6;
            }
            if(stage==6&&ticks-start==1){DestructionEngine.abort(handle);stage=7;}
            if(stage==7&&handle.done){require(DestructionEngine.isIdle(),"cancel left pending work");System.out.println("AC061_SERVER_ALL_PASS life=.1 water=.1 idle=true cancel=true; not full-radius stress or multiplayer");server.halt(false);}
            if(ticks>4000)throw new AssertionError("pipeline timeout "+DestructionEngine.status());
        }catch(Throwable ex){ex.printStackTrace();Runtime.getRuntime().halt(1);}
    }
    private FilmServerQA(){}
}
