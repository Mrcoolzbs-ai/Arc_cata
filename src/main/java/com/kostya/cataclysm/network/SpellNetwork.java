package com.kostya.cataclysm.network;
import com.kostya.cataclysm.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.server.level.ServerPlayer;

public final class SpellNetwork {
    /** Assigned by the client mod event subscriber, never loads client classes on a server. */
    public static java.util.function.Consumer<Fx> clientReceiver=p->{};
    public static java.util.function.Consumer<Progress> progressReceiver=p->{};
    public static java.util.function.Consumer<EndState> endReceiver=p->{};
    public record EndState(long seed,int age,boolean active) implements CustomPacketPayload {
        public static final Type<EndState> TYPE=new Type<>(Cataclysm.id("end_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,EndState> CODEC=new StreamCodec<>(){
            public EndState decode(RegistryFriendlyByteBuf b){return new EndState(b.readLong(),b.readVarInt(),b.readBoolean());}
            public void encode(RegistryFriendlyByteBuf b,EndState p){b.writeLong(p.seed);b.writeVarInt(p.age);b.writeBoolean(p.active);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Fx(int spell,int caster,Vec3 origin,Vec3 target,Vec3 direction,long seed,float scale) implements CustomPacketPayload {
        public static final Type<Fx> TYPE=new Type<>(Cataclysm.id("spell_fx"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Fx> CODEC=new StreamCodec<>(){
            public Fx decode(RegistryFriendlyByteBuf b){return new Fx(b.readVarInt(),b.readVarInt(),vec(b),vec(b),vec(b),b.readLong(),b.readFloat());}
            public void encode(RegistryFriendlyByteBuf b,Fx p){b.writeVarInt(p.spell);b.writeVarInt(p.caster);vec(b,p.origin);vec(b,p.target);vec(b,p.direction);b.writeLong(p.seed);b.writeFloat(p.scale);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Progress(long id,int phase,int prepared,int completed,int total,long blocks,int skipped) implements CustomPacketPayload {
        public static final Type<Progress> TYPE=new Type<>(Cataclysm.id("progress"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Progress> CODEC=new StreamCodec<>(){
            public Progress decode(RegistryFriendlyByteBuf b){return new Progress(b.readLong(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarLong(),b.readVarInt());}
            public void encode(RegistryFriendlyByteBuf b,Progress p){b.writeLong(p.id);b.writeVarInt(p.phase);b.writeVarInt(p.prepared);b.writeVarInt(p.completed);b.writeVarInt(p.total);b.writeVarLong(p.blocks);b.writeVarInt(p.skipped);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Select(int step) implements CustomPacketPayload {
        public static final Type<Select> TYPE=new Type<>(Cataclysm.id("select"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Select> CODEC=new StreamCodec<>(){
            public Select decode(RegistryFriendlyByteBuf b){return new Select(b.readByte());}
            public void encode(RegistryFriendlyByteBuf b,Select p){b.writeByte(p.step);}
        };
        public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private static Vec3 vec(RegistryFriendlyByteBuf b){return new Vec3(b.readDouble(),b.readDouble(),b.readDouble());}
    private static void vec(RegistryFriendlyByteBuf b,Vec3 v){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}
    public static void register(RegisterPayloadHandlersEvent event){
        var r=event.registrar("4");
        r.playToClient(EndState.TYPE,EndState.CODEC,(p,c)->c.enqueueWork(()->endReceiver.accept(p)));
        r.playToClient(Progress.TYPE,Progress.CODEC,(p,c)->c.enqueueWork(()->progressReceiver.accept(p)));
        r.playToClient(Fx.TYPE,Fx.CODEC,(p,c)->c.enqueueWork(()->clientReceiver.accept(p)));
        r.playToServer(Select.TYPE,Select.CODEC,(p,c)->c.enqueueWork(()->{
            if(c.player() instanceof ServerPlayer sp && (p.step==1||p.step==-1)){
                // At most one selection per two server ticks, including malicious packet spam.
                long now=sp.serverLevel().getGameTime(),last=sp.getPersistentData().getLong("ac_select_tick");
                if(now-last>=2){GrimoireItem.select(sp,p.step);sp.getPersistentData().putLong("ac_select_tick",now);}
            }
        }));
    }
    private SpellNetwork(){}
}
