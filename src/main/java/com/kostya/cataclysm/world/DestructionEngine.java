package com.kostya.cataclysm.world;

import com.kostya.cataclysm.*;
import com.kostya.cataclysm.network.SpellNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.core.*;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;
import java.util.concurrent.*;

/** Bounded pipeline: copied states -> immutable final-state patches -> commit -> light barrier -> sync. */
public final class DestructionEngine {
    private static final TicketType<Long> TICKET=TicketType.create("arcane_cataclysm",Long::compare);
    public static final TagKey<Block> IMMUNE=TagKey.create(Registries.BLOCK,Cataclysm.id("destruction_immune"));
    private static final Deque<Handle> JOBS=new ArrayDeque<>();
    private static final List<Pending> LIGHTING=new ArrayList<>();
    private static ExecutorService worker;
    private static long changedBlocks,completedChunks;
    private static String lastResult="No completed casts";
    private static final int LOADS=4;
    public static final class Handle {
        public final ServerLevel level; public final DestructionPlan plan;public final long id;public final UUID owner;
        public final int total;public int prepared,committed,processed,skipped,failed;public long blocks,createdBlocks,destroyedBlocks;public int burstChunks,ignitions,biomeChunks;
        public final Set<UUID> damagedEntities=new HashSet<>();
        private final Set<Long> preparedPositions=new HashSet<>();
        public boolean released,done,cancelled;
        private final Map<Long,Integer> retries=new HashMap<>();private final Deque<ChunkPos> todo;private final Deque<Prepared> ready=new ArrayDeque<>();
        private final List<Loading> loading=new ArrayList<>();private Prepared current;private int cursor,pendingLights;
        private Capture capture;
        private long lastNotice=-1000;
        private Handle(ServerLevel level,DestructionPlan plan,UUID owner){
            this.level=level;this.plan=plan;this.id=plan.seed;this.owner=owner;
            var list=new ArrayList<ChunkPos>();
            int ax=((int)Math.floor(plan.center.x-plan.radius-2))>>4,bx=((int)Math.floor(plan.center.x+plan.radius+2))>>4;
            int az=((int)Math.floor(plan.center.z-plan.radius-2))>>4,bz=((int)Math.floor(plan.center.z+plan.radius+2))>>4;
            for(int x=ax;x<=bx;x++)for(int z=az;z<=bz;z++){
                double dx=Math.max(0,Math.abs(x*16+8-plan.center.x)-8),dz=Math.max(0,Math.abs(z*16+8-plan.center.z)-8);
                ChunkPos p=new ChunkPos(x,z);if(dx*dx+dz*dz<(plan.radius+2)*(plan.radius+2)&&level.getWorldBorder().isWithinBounds(p))list.add(p);
            }
            list.sort(Comparator.comparingDouble(p->Math.pow(p.x*16+8-plan.center.x,2)+Math.pow(p.z*16+8-plan.center.z,2)));
            todo=new ArrayDeque<>(list);total=list.size();
        }
    }
    /** Only the server reads chunks. Life snapshots yield between columns, retaining their ticket. */
    private static final class Capture {
        final LevelChunk chunk;final PreparedChunk.Sample[] samples=new PreparedChunk.Sample[256];int cursor;
        Capture(LevelChunk chunk){this.chunk=chunk;}
    }
    private record Loading(ChunkPos pos,CompletableFuture<?> future,long since){}
    private static final class Prepared {
        LevelChunk chunk;final ChunkPos pos;final CompletableFuture<PreparedChunk> geometry;CompletableFuture<?> reload;boolean counted,pinned;
        final BitSet sections=new BitSet();boolean changed;
        Prepared(LevelChunk c,CompletableFuture<PreparedChunk> g,boolean pin){chunk=pin?c:null;pos=c.getPos();geometry=g;pinned=pin;}
    }
    private record Pending(Handle job,Prepared prepared,CompletableFuture<?> future,long since){}
    private static ExecutorService worker(){if(worker==null||worker.isShutdown())worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"Cataclysm-geometry");t.setDaemon(true);return t;});return worker;}
    public static Handle prepare(ServerLevel level,DestructionPlan plan,UUID owner){
        if(!CataclysmConfig.DESTRUCTION.get()||JOBS.size()>=8)return null;
        Handle h=new Handle(level,plan,owner);JOBS.add(h);
        LogUtils.getLogger().info("[Cataclysm] PREPARE {} target={} minY={} radius={} chunks={}",plan.spell.id,plan.center,level.getMinBuildHeight(),plan.radius,h.total);
        return h;
    }
    public static void release(Handle h){
        if(h==null||h.done)return;h.released=true;
        // Explicit opt-in/default for 0.3 requested semantics: no tick budget in this synchronous burst.
        // Never join unfinished geometry or synchronously generate chunks here.
        if(!CataclysmConfig.INSTANT_IMPACT.get()||(h.plan.spell==Spell.END_OF_ALL||h.plan.spell==Spell.VERDANT_RUIN))return;
        for(var it=h.ready.iterator();it.hasNext();){Prepared p=it.next();
            if(!p.geometry.isDone()||p.geometry.isCompletedExceptionally())continue;
            var loaded=h.level.getChunkSource().getChunkNow(p.pos.x,p.pos.z);if(loaded==null)continue;
            p.chunk=loaded;if(!p.pinned){h.level.getChunkSource().addRegionTicket(TICKET,p.pos,0,h.id);p.pinned=true;}
            if(!p.counted){p.counted=true;if(h.preparedPositions.add(p.pos.toLong()))h.prepared++;}it.remove();
            for(int col=0;col<256;col++)editColumn(h,p,col);finish(h,p);h.burstChunks++;
        }
        LogUtils.getLogger().info("[Cataclysm] IMPACT_BURST {} readyApplied={} blocks={}",h.plan.spell.id,h.burstChunks,h.blocks);
    }
    public static boolean enqueue(ServerLevel level,DestructionPlan plan){Handle h=prepare(level,plan,null);release(h);return h!=null;}
    public static boolean isIdle(){return JOBS.isEmpty()&&LIGHTING.isEmpty();}
    public static Handle latestFor(UUID owner){Handle last=null;for(Handle h:JOBS)if(Objects.equals(h.owner,owner))last=h;return last;}
    public static long changedBlocks(){return changedBlocks;}
    public static String status(){return "jobs="+JOBS.size()+", lighting="+LIGHTING.size()+", completedChunks="+completedChunks+", changedBlocks="+changedBlocks+"; "+lastResult;}
    public static void tick(){
        flushLighting();
        long prepDeadline=System.nanoTime()+(long)(CataclysmConfig.PREP_BUDGET.get()*1e6);
        for(Handle h:JOBS){prepareWindow(h,prepDeadline);if(h.released&&!h.cancelled&&h.level.getGameTime()%5==0)AreaDamage.apply(h.level,h.plan.spell,h.plan.center,h.plan.scale,h.owner,h.damagedEntities);notice(h,false);}
        if(LIGHTING.size()<16){
            long deadline=System.nanoTime()+(long)(CataclysmConfig.BUDGET_MS.get()*1e6);
            int columns=CataclysmConfig.COLUMNS_PER_TICK.get(),finishes=0,rotations=0;
            while(!JOBS.isEmpty()&&columns>0&&finishes<2&&System.nanoTime()<deadline){
                Handle h=JOBS.peek();
                if(!h.released||!selectCurrent(h)){JOBS.addLast(JOBS.removeFirst());if(++rotations>=JOBS.size())break;continue;}
                editColumn(h,h.current,h.cursor++);columns--;rotations=0;
                if(h.cursor==256){finish(h,h.current);h.current=null;h.cursor=0;finishes++;JOBS.addLast(JOBS.removeFirst());}
            }
        }
        for(var it=JOBS.iterator();it.hasNext();){Handle h=it.next();
            if(h.released&&h.todo.isEmpty()&&h.loading.isEmpty()&&h.ready.isEmpty()&&h.current==null&&h.capture==null&&h.pendingLights==0){
                h.done=true;lastResult=h.plan.spell.id+": blocks="+h.blocks+", chunks="+h.committed+", skipped="+h.skipped+", failed="+h.failed+", burst="+h.burstChunks+", ignitions="+h.ignitions+", biomeChunks="+h.biomeChunks;
                LogUtils.getLogger().info("[Cataclysm] COMPLETE {}",lastResult);notice(h,true);it.remove();
            }
        }
    }
    private static void prepareWindow(Handle h,long deadline){
        var cache=h.level.getChunkSource();
        if(h.capture!=null){captureLife(h,deadline);if(h.capture!=null)return;}
        for(Prepared p:h.ready)if(!p.counted&&p.geometry.isDone()&&!p.geometry.isCompletedExceptionally()){p.counted=true;if(h.preparedPositions.add(p.pos.toLong()))h.prepared++;}
        for(var it=h.loading.iterator();it.hasNext();){Loading l=it.next();LevelChunk chunk=cache.getChunkNow(l.pos.x,l.pos.z);
            if(chunk!=null&&System.nanoTime()<deadline){snapshot(h,chunk);it.remove();if(h.capture!=null){captureLife(h,deadline);break;}}
            else if(h.level.getGameTime()-l.since>1200||l.future.isCompletedExceptionally()){
                cache.removeRegionTicket(TICKET,l.pos,0,h.id);it.remove();retry(h,l.pos,"load timeout/failure");
            }
        }
        int captured=0,skips=0;
        while(!h.todo.isEmpty()&&System.nanoTime()<deadline&&captured<2&&skips<24&&h.ready.size()<CataclysmConfig.MAX_PREPARED.get()&&h.ready.stream().filter(p->!p.geometry.isDone()).count()<4){
            ChunkPos p=h.todo.peek();LevelChunk c=cache.getChunkNow(p.x,p.z);
            if(c!=null){h.todo.remove();cache.addRegionTicket(TICKET,p,0,h.id);snapshot(h,c);captured++;if(h.capture!=null){captureLife(h,deadline);break;}}
            else if(CataclysmConfig.GENERATE_CHUNKS.get()){
                if(h.loading.size()>=LOADS)break;
                h.todo.remove();cache.addRegionTicket(TICKET,p,0,h.id);
                h.loading.add(new Loading(p,cache.getChunkFuture(p.x,p.z,ChunkStatus.FULL,true),h.level.getGameTime()));
            }else{h.todo.remove();h.skipped++;skips++;}
        }
    }
    /** Copy immutable states on the server; calculate every replacement before release. */
    private static void snapshot(Handle h,LevelChunk chunk){
        if(h.plan.spell==Spell.VERDANT_RUIN){h.capture=new Capture(chunk);return;}
        int min=h.level.getMinBuildHeight(),max=h.level.getMaxBuildHeight(),cx=chunk.getPos().getMinBlockX(),cz=chunk.getPos().getMinBlockZ();
        var samples=new PreparedChunk.Sample[256];var pos=new BlockPos.MutableBlockPos();
        for(int i=0;i<256;i++){
            int lx=i&15,lz=i>>4,x=cx+lx,z=cz+lz,terrain=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,lx,lz);
            var c=h.plan.column(x,z,terrain,min,max);if(c==null)continue;
            var states=new BlockState[c.top()-c.bottom()+1];var immune=new BitSet(states.length);
            for(int y=c.bottom();y<=c.top();y++){
                pos.set(x,y,z);int k=y-c.bottom();var old=chunk.getBlockState(pos);states[k]=old;
                if(protectedBlock(h,old,pos))immune.set(k);
            }
            samples[i]=new PreparedChunk.Sample(c,states,immune);
        }
        var future=CompletableFuture.supplyAsync(()->new PreparedChunk(h.plan,cx,cz,samples),worker());
        boolean pin=h.ready.stream().filter(p->p.pinned).count()<CataclysmConfig.PINNED_CHUNKS.get();
        h.ready.add(new Prepared(chunk,future,pin));if(!pin)unpin(h,chunk.getPos());
    }
    private static void captureLife(Handle h,long deadline){
        Capture cap=h.capture;var chunk=cap.chunk;int cx=chunk.getPos().getMinBlockX(),cz=chunk.getPos().getMinBlockZ();
        var pos=new BlockPos.MutableBlockPos();
        while(cap.cursor<256&&System.nanoTime()<deadline){
            int i=cap.cursor++,lx=i&15,lz=i>>4,x=cx+lx,z=cz+lz;
            var c=h.plan.column(x,z,chunk.getHeight(Heightmap.Types.WORLD_SURFACE,lx,lz),h.level.getMinBuildHeight(),h.level.getMaxBuildHeight());
            if(c==null)continue;var states=new BlockState[c.top()-c.bottom()+1];var immune=new BitSet(states.length);
            for(int y=c.bottom();y<=c.top();y++){pos.set(x,y,z);int k=y-c.bottom();var old=chunk.getBlockState(pos);states[k]=old;if(protectedBlock(h,old,pos))immune.set(k);}
            cap.samples[i]=new PreparedChunk.Sample(c,states,immune);
        }
        if(cap.cursor==256){
            var future=CompletableFuture.supplyAsync(()->new PreparedChunk(h.plan,cx,cz,cap.samples),worker());
            boolean pin=h.ready.stream().filter(p->p.pinned).count()<CataclysmConfig.PINNED_CHUNKS.get();
            h.ready.add(new Prepared(chunk,future,pin));if(!pin)unpin(h,chunk.getPos());h.capture=null;
        }
    }
    private static boolean protectedBlock(Handle h,BlockState state,BlockPos pos){
        if(state.is(net.minecraft.world.level.block.Blocks.BEDROCK))return h.plan.spell.tier()<10||!CataclysmConfig.BREAK_BEDROCK.get();
        return state.is(IMMUNE)||state.getDestroySpeed(h.level,pos)<0;
    }
    private static boolean selectCurrent(Handle h){
        if(h.current!=null)return true;
        Prepared p=h.ready.peek();if(p==null||!p.geometry.isDone())return false;
        if(p.geometry.isCompletedExceptionally()){
            h.ready.remove();if(p.pinned)unpin(h,p.pos);retry(h,p.pos,"preparation failed");return false;
        }
        if(!p.counted){p.counted=true;if(h.preparedPositions.add(p.pos.toLong()))h.prepared++;}
        var cache=h.level.getChunkSource();var loaded=cache.getChunkNow(p.pos.x,p.pos.z);
        if(loaded==null){
            if(!p.pinned){cache.addRegionTicket(TICKET,p.pos,0,h.id);p.pinned=true;}
            if(p.reload==null)p.reload=cache.getChunkFuture(p.pos.x,p.pos.z,ChunkStatus.FULL,true);
            if(p.reload.isCompletedExceptionally()){h.ready.remove();unpin(h,p.pos);retry(h,p.pos,"reload failed");}
            return false;
        }
        p.chunk=loaded;if(!p.pinned){cache.addRegionTicket(TICKET,p.pos,0,h.id);p.pinned=true;}
        h.ready.remove();h.current=p;return true;
    }
    private static void editColumn(Handle h,Prepared p,int col){
        var patch=p.geometry.join();var c=patch.columns[col];if(c==null)return;
        int lx=col&15,lz=col>>4,x=p.pos.getMinBlockX()+lx,z=p.pos.getMinBlockZ()+lz;
        var pos=new BlockPos.MutableBlockPos();var light=h.level.getChunkSource().getLightEngine();boolean changed=false;
        // No geometry, noise, material choice or final-state calculation occurs here.
        for(var run:patch.edits[col])for(int y=run.from();y<=run.to();y++){
            pos.set(x,y,z);int sec=p.chunk.getSectionIndex(y);var section=p.chunk.getSection(sec);
            BlockState old=section.getBlockState(lx,y&15,lz),next=run.state();
            if(old==next||protectedBlock(h,old,pos))continue;
            if(old.isAir()&&!next.isAir())h.createdBlocks++;if(!old.isAir()&&next.isAir())h.destroyedBlocks++;
            if(old.hasBlockEntity()||!BuiltInRegistries.BLOCK.getKey(old.getBlock()).getNamespace().equals("minecraft"))h.level.setBlock(pos,next,2|16);
            else{section.setBlockState(lx,y&15,lz,next,false);h.level.onBlockStateChange(pos,old,next);}
            if(old.getLightEmission(h.level,pos)>0||next.getLightEmission(h.level,pos)>0||lx==0||lx==15||lz==0||lz==15||(y&15)==0)light.checkBlock(pos.immutable());
            p.sections.set(sec);p.changed=true;changed=true;changedBlocks++;h.blocks++;
        }
        if(changed){light.checkBlock(new BlockPos(x,c.top()+1,z));light.checkBlock(new BlockPos(x,c.floor(),z));light.checkBlock(new BlockPos(x,c.bottom(),z));}
    }
    private static void finish(Handle h,Prepared p){
        h.processed++;LevelChunk chunk=p.chunk;var cache=h.level.getChunkSource();
        if(h.plan.spell==Spell.GLACIAL_RAGNAROK&&CataclysmConfig.BIOMES.get()){
            var ice=h.level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.ICE_SPIKES);
            chunk.fillBiomesFromNoise((qx,qy,qz,sampler)->h.plan.inImpact(qx*4+2,qz*4+2)?ice:chunk.getNoiseBiome(qx,qy,qz),cache.randomState().sampler());
            p.changed=true;h.biomeChunks++;
        }
        if(p.changed){
            Heightmap.primeHeightmaps(chunk,EnumSet.of(Heightmap.Types.WORLD_SURFACE,Heightmap.Types.OCEAN_FLOOR,Heightmap.Types.MOTION_BLOCKING,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES));
            if(h.plan.spell.fiery()&&CataclysmConfig.FIRE.get())ignite(h,chunk);
            chunk.initializeLightSources();
            for(int z=0;z<16;z++)for(int x=0;x<16;x++)cache.getLightEngine().checkBlock(new BlockPos(chunk.getPos().getMinBlockX()+x,chunk.getHeight(Heightmap.Types.WORLD_SURFACE,x,z)+1,chunk.getPos().getMinBlockZ()+z));
            for(int s=p.sections.nextSetBit(0);s>=0;s=p.sections.nextSetBit(s+1))cache.getLightEngine().updateSectionStatus(SectionPos.of(chunk.getPos(),chunk.getSectionYFromSectionIndex(s)),chunk.getSection(s).hasOnlyAir());
            chunk.setUnsaved(true);cache.getLightEngine().tryScheduleUpdate();h.pendingLights++;
            LIGHTING.add(new Pending(h,p,cache.getLightEngine().waitForPendingTasks(chunk.getPos().x,chunk.getPos().z),h.level.getGameTime()));
            completedChunks++;h.committed++;
        }else unpin(h,chunk.getPos());
    }
    /** Sparse real fire, not decorative particles. Vanilla may spread it according to doFireTick. */
    private static void ignite(Handle h,LevelChunk chunk){
        int cap=CataclysmConfig.FIRE_PER_CHUNK.get(),placed=0;var level=h.level;
        for(int i=0;i<256&&placed<cap;i++){
            int lx=(i*73)&15,lz=((i*73)>>4)&15,x=chunk.getPos().getMinBlockX()+lx,z=chunk.getPos().getMinBlockZ()+lz;
            if(!h.plan.inImpact(x+.5,z+.5))continue;
            int y=chunk.getHeight(Heightmap.Types.WORLD_SURFACE,lx,lz)+1;var pos=new BlockPos(x,y,z);
            if(!level.isInWorldBounds(pos)||!level.getBlockState(pos).isAir())continue;
            boolean fuel=level.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.NETHERRACK)||(SpellPatterns.hash(x,0,z,h.id)>.96&&level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP));
            for(var dir:Direction.values()){var q=pos.relative(dir);if(level.hasChunkAt(q)&&level.getBlockState(q).isFlammable(level,q,dir.getOpposite()))fuel=true;}
            var fire=net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState();
            if(fuel&&fire.canSurvive(level,pos)){level.setBlock(pos,fire,3);placed++;h.ignitions++;}
        }
    }
    private static void flushLighting(){
        int sent=0;for(var it=LIGHTING.iterator();it.hasNext()&&sent<6;){Pending p=it.next();if(!p.future.isDone()||p.job.level.getGameTime()-p.since<10)continue;
            var h=p.job;var chunk=p.prepared.chunk;
            if(p.future.isCompletedExceptionally())LogUtils.getLogger().error("[Cataclysm] Lighting failed for {}",chunk.getPos());
            var players=h.level.getChunkSource().chunkMap.getPlayers(chunk.getPos(),false);
            if(!players.isEmpty()){var packet=new ClientboundLevelChunkWithLightPacket(chunk,h.level.getChunkSource().getLightEngine(),null,null);for(var player:players)player.connection.send(packet);}
            h.pendingLights--;unpin(h,chunk.getPos());it.remove();sent++;
        }
    }
    private static void retry(Handle h,ChunkPos pos,String why){
        int attempt=h.retries.merge(pos.toLong(),1,Integer::sum);
        if(attempt<=3){h.todo.addLast(pos);LogUtils.getLogger().warn("[Cataclysm] Retry {} chunk={} attempt={}",why,pos,attempt);}
        else{h.failed++;LogUtils.getLogger().error("[Cataclysm] INCOMPLETE chunk={} after retries: {}",pos,why);}
    }
    private static void unpin(Handle h,ChunkPos p){h.level.getChunkSource().removeRegionTicket(TICKET,p,0,h.id);}
    private static void notice(Handle h,boolean force){
        if(h.owner==null)return;long now=h.level.getGameTime();if(!force&&now-h.lastNotice<10)return;h.lastNotice=now;
        var owner=h.level.getServer().getPlayerList().getPlayer(h.owner);if(owner==null)return;
        PacketDistributor.sendToPlayer(owner,new SpellNetwork.Progress(h.id,h.done?(h.cancelled||h.failed+h.skipped>0?3:2):(h.released?1:0),h.prepared,h.processed,h.total,h.blocks,h.skipped+h.failed));
    }
    public static void abort(Handle h){
        if(h==null||h.done)return;h.cancelled=true;
        if(h.capture!=null){unpin(h,h.capture.chunk.getPos());h.capture=null;}
        if(h.current!=null){finish(h,h.current);h.current=null;}
        for(Prepared p:h.ready){p.geometry.cancel(false);if(p.pinned)unpin(h,p.pos);}h.ready.clear();
        for(Loading l:h.loading)unpin(h,l.pos);h.loading.clear();h.todo.clear();h.released=true;
    }
    public static void cancel(){for(Handle h:JOBS)if(h.plan.spell!=Spell.END_OF_ALL)abort(h);}
    public static void shutdown(){for(Handle h:JOBS)abort(h);for(Pending p:LIGHTING)unpin(p.job,p.prepared.chunk.getPos());LIGHTING.clear();JOBS.clear();if(worker!=null)worker.shutdownNow();changedBlocks=0;completedChunks=0;}
    private DestructionEngine(){}
}
