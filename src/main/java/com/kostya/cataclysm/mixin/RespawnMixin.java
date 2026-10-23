package com.kostya.cataclysm.mixin;
import com.kostya.cataclysm.world.EndEvent;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class RespawnMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleClientCommand",at=@At("HEAD"),cancellable=true)
    private void cataclysm$hold(ServerboundClientCommandPacket packet,CallbackInfo ci){
        // Netty thread must not read the game event; normal handler will schedule onto the server first.
        if(!player.server.isSameThread())return;
        if(packet.getAction()==ServerboundClientCommandPacket.Action.PERFORM_RESPAWN&&EndEvent.respawnLocked(player)){
            player.displayClientMessage(Component.translatable("message.arcane_cataclysm.end_respawn"),true);ci.cancel();
        }
    }
}
