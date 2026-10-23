package com.kostya.cataclysm.world;
import com.kostya.cataclysm.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.minecraft.core.registries.Registries;
import java.util.*;
/** One hit per entity UUID. Rechecked while terrain work runs so asynchronously loaded victims are not missed. */
public final class AreaDamage {
 public static int apply(ServerLevel level,Spell spell,Vec3 target,double scale,UUID casterId,Set<UUID> hit){
  if(!CataclysmConfig.ENTITY_DAMAGE.get())return 0;
  double r=spell.radius*scale;var caster=casterId==null?null:level.getPlayerByUUID(casterId);
  var source=new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(SpellManager.ARCANE),caster);
  var bounds=new AABB(target.x-r,level.getMinBuildHeight(),target.z-r,target.x+r,level.getMaxBuildHeight(),target.z+r);int affected=0;
  for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,bounds)){
   if(e.isSpectator()||e.getUUID().equals(casterId)||hit.contains(e.getUUID())||(!CataclysmConfig.DAMAGE_PLAYERS.get()&&e instanceof Player))continue;
   double dx=e.getX()-target.x,dz=e.getZ()-target.z;if(dx*dx+dz*dz>r*r)continue;
   if(e.hurt(source,SpellManager.impactDamage(spell))){hit.add(e.getUUID());affected++;Vec3 v=e.position().subtract(target).multiply(1,0,1).normalize().scale(spell==Spell.SINGULARITY?-2:3);e.push(v.x,.8,v.z);e.hurtMarked=true;if(spell.fiery())e.igniteForSeconds(10);}
  }
  return affected;
 }
 private AreaDamage(){}
}
