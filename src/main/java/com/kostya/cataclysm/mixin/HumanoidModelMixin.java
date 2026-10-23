package com.kostya.cataclysm.mixin;
import com.kostya.cataclysm.client.ClientEffects;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {
    @Shadow public ModelPart rightArm;
    @Shadow public ModelPart leftArm;
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void cataclysm$pose(LivingEntity e,float a,float b,float t,float y,float x,CallbackInfo ci){
        float f=ClientEffects.casting(e.getId());if(f<=0)return;
        rightArm.xRot=rightArm.xRot*(1-f)+(-2.3f+(float)Math.sin(t*.12)*.06f)*f;rightArm.zRot=.23f*f;
        leftArm.xRot=leftArm.xRot*(1-f)-1.4f*f;leftArm.zRot=-.35f*f;
    }
}
