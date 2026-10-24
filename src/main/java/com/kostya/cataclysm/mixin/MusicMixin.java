package com.kostya.cataclysm.mixin;
import com.kostya.cataclysm.client.EndCinematic;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MusicManager.class)
public abstract class MusicMixin {
 @Inject(method="tick",at=@At("HEAD"),cancellable=true)
 private void cataclysm$score(CallbackInfo ci){if(EndCinematic.running()||com.kostya.cataclysm.client.ClientEffects.cinematic()!=null)ci.cancel();}
}
