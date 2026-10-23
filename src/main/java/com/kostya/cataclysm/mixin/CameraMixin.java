package com.kostya.cataclysm.mixin;
import com.kostya.cataclysm.client.ClientEffects;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(Vec3 p);
    @Shadow protected abstract void setRotation(float yaw,float pitch,float roll);
    @Shadow private boolean detached;
    @Inject(method="setup",at=@At("TAIL"))
    private void cataclysm$direct(BlockGetter level,Entity entity,boolean thirdPerson,boolean reverse,float partial,CallbackInfo ci){
        var v=ClientEffects.view((Camera)(Object)this,partial);if(v==null)return;
        setPosition(v.pos());setRotation(v.yaw(),v.pitch(),v.roll());if(ClientEffects.cinematic()!=null)detached=true;
    }
}
