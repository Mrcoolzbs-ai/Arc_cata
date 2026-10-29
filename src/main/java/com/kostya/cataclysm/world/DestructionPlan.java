package com.kostya.cataclysm.world;
import com.kostya.cataclysm.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
/** Pure immutable geometry. No Level/Chunk or registry access occurs on a worker thread. */
public final class DestructionPlan {
 public final Spell spell;public final Vec3 center,direction;public final long seed;public final double scale,radius;
 private final List<Vec3> strikes;
 public record Column(int bottom,int top,int floor,int spike,boolean freeze,boolean scorch,int base){}
 public DestructionPlan(Spell s,Vec3 c,Vec3 dir,long seed,double scale){spell=s;center=c;direction=dir;this.seed=seed;this.scale=scale;radius=s.radius*scale;strikes=SpellPatterns.strikes(c,seed,radius*.86,s==Spell.TEMPEST_WRATH?40:72);}
 public boolean inImpact(double x,double z){double dx=x-center.x,dz=z-center.z;return dx*dx+dz*dz<=radius*radius;}
 public Column column(int x,int z,int terrain,int min,int max){
  double dx=x+.5-center.x,dz=z+.5-center.z,d=Math.hypot(dx,dz),r=radius;if(d>r)return null;
  double n=noise(x,z)*2-1,a=Math.atan2(dz,dx),u=d/r;int floor=terrain,hi=terrain,spike=0;boolean frost=false,scorch=spell.fiery();
  double closest=1e8;if(spell==Spell.TEMPEST_WRATH||spell==Spell.THOUSAND_BLADES)for(Vec3 p:strikes)closest=Math.min(closest,Math.hypot(x+.5-p.x,z+.5-p.z));
  switch(spell){
   // 0.8.1 #1: a narrow sky bore with a cross of lance trenches - the light spire is gone.
   case HEAVENS_LANCE->{floor=min+(int)(5+12*Math.pow(u,5));hi=max-1;}
   // 0.8.1 #2: a lattice of blade cuts - no crystal blades stand any more.
   case THOUSAND_BLADES->{double cut=7*scale,lines=Math.min(Math.abs(dx-dz),Math.abs(dx+dz))*.7071;closest=Math.min(closest,lines);if(closest>cut)return null;floor=(int)(center.y-48*scale*(1-closest/cut));}
   // 0.8.0 #3: the frost grave - per-cell ice clusters and a central blue-ice monolith (0.8.1: kept unchanged).
   case GLACIAL_RAGNAROK->{if(terrain<min)return null;frost=true;int gx=Math.floorDiv(x,16),gz=Math.floorDiv(z,16);double px=gx*16+3+SpellPatterns.hash(gx,3,gz,seed)*10,pz=gz*16+3+SpellPatterns.hash(gx,5,gz,seed)*10;double q=Math.hypot(x-px,z-pz),size=3+SpellPatterns.hash(gx,8,gz,seed)*3;if(q<size)spike=(int)((1-q/size)*(22+60*SpellPatterns.hash(gx,9,gz,seed))*scale*(1-u*.4));if(u<.21)spike=Math.max(spike,(int)(64*scale*Math.sqrt(1-u*u/(.21*.21))));}
   // 0.8.1 #4: storm strike channels - the copper rods are gone.
   case TEMPEST_WRATH->{double branch=Math.abs(Math.sin(a*7+d*.025))*d;if(closest>12*scale&&branch>3*scale)return null;floor=(int)(center.y-42*scale*Math.max(.15,1-closest/(15*scale)));}
   // 0.8.1 #5: meteor shower - local pocks under the fallen, nothing standing.
   case STARFALL->{floor=(int)(center.y-80*scale*(1-u*u)+n*3);double near=1e8;for(Vec3 p:strikes)near=Math.min(near,Math.hypot(x+.5-p.x,z+.5-p.z));if(near<6*scale)floor=Math.min(floor,(int)(center.y-20*scale));}
   // 0.8.1 #6: a fresh volcanic rift - andesite terraces, magma seam down the middle, no rim columns.
   case TECTONIC_RIFT->{double len=Math.hypot(direction.x,direction.z),ux=len<.01?1:direction.x/len,uz=len<.01?0:direction.z/len;double along=dx*ux+dz*uz,across=Math.abs(-dx*uz+dz*ux+Math.sin(along*.045)*7*scale),width=42*scale*Math.sqrt(Math.max(0,1-along*along/(r*r)));if(across>width||width<.1)return null;floor=(int)(center.y-125*scale*(1-across/width));}
   // 0.8.1 #7: a fire tide - scorch rings rolling outward, no magma crests standing.
   case INFERNO_TIDE->{double rings=Math.sin(d/Math.max(1,9*scale));floor=terrain-(int)((12+26*(1-u))*scale+rings*4*scale);}
   // 0.8.1 #8: a spherical gravity void - the weeping-obsidian ring wall is gone.
   case SINGULARITY->{double h=Math.sqrt(r*r-d*d);floor=(int)(center.y+18*scale-h);hi=(int)(center.y+18*scale+h);}
   // 0.8.1 #9: solar corona - an obsidian basin with a glowstone photosphere, no glass rays.
   case SOLAR_NOVA->{floor=(int)(center.y-112*scale*(1-u*u));}
   // 0.8.1 #10: the cataclysm - stepped terraces with amethyst veins, no crystal ring wall.
   case LAST_CATACLYSM->{floor=(int)(center.y-150*scale*(1-u*u));floor=(int)(Math.floor(floor/8.)*8);}
   // 0.8.1 #11: withering - a barren crater, nothing left standing.
   case VERDANT_RUIN->{floor=(int)(center.y-165*scale*(1-u*u));hi=terrain;}
   // 0.8.1 #12: the cathedral - a carved obsidian basin, no buttresses, no spire.
   case OBSIDIAN_CATHEDRAL->{floor=(int)(center.y-178*scale*(1-u*u));}
   // 0.8.1 #13: the deluge - a prismarine trench with gravel shoals, flooded below sea level, no shoal columns.
   case ABYSSAL_DELUGE->{floor=(int)(center.y-195*scale*Math.pow(1-u*u,.6));hi=Math.max(terrain,0);}
   // 0.8.1 #14: world sunder - continental plates torn along glowing faults, no fault columns.
   case WORLD_SUNDER->{double fault=Math.abs(Math.sin(a*7+d*.006));floor=(int)(center.y-(85+155*(1-fault))*scale*(1-u*.65));}
   case END_OF_ALL->{return new Column(min,max-1,min-1,0,false,false,terrain);}
   // 0.8.1 #15: rune apocalypse - glowing rune veins in the obsidian field, no monoliths.
   case RUNE_APOCALYPSE->{floor=(int)(center.y-280*scale*(1-u*.65));hi=max-1;}
  }
  floor=Math.max(min,Math.min(max-2,floor));spike=Math.max(0,Math.min(spike,max-1-floor));if(frost)floor=Math.max(min,Math.min(max-2,terrain));
  hi=Math.max(floor+spike,Math.max(floor,Math.min(max-1,hi)));hi=Math.min(max-1,hi);
  return new Column(Math.max(min,floor-4),hi,floor,Math.min(spike,max-1-floor),frost,scorch,terrain);
 }
 private double noise(int x,int z){int gx=Math.floorDiv(x,24),gz=Math.floorDiv(z,24);double u=Math.floorMod(x,24)/24.,v=Math.floorMod(z,24)/24.;u=u*u*(3-2*u);v=v*v*(3-2*v);double a=SpellPatterns.hash(gx,0,gz,seed),b=SpellPatterns.hash(gx+1,0,gz,seed),c=SpellPatterns.hash(gx,0,gz+1,seed),d=SpellPatterns.hash(gx+1,0,gz+1,seed);return(a+(b-a)*u)*(1-v)+(c+(d-c)*u)*v;}
 public boolean creates(Column c){return c.freeze||c.spike>0||spell==Spell.ABYSSAL_DELUGE;}
 public BlockState state(Column c,int x,int y,int z,BlockState old){
  if(c.freeze){if(y>c.floor)return Blocks.BLUE_ICE.defaultBlockState();if(old.isAir())return old;if(!old.getFluidState().isEmpty())return Blocks.ICE.defaultBlockState();return(y==c.floor?Blocks.SNOW_BLOCK:Blocks.PACKED_ICE).defaultBlockState();}
  if(y>c.floor){
   if(spell==Spell.ABYSSAL_DELUGE&&y<=0)return Blocks.WATER.defaultBlockState();
   return Blocks.AIR.defaultBlockState();
  }
  if(old.isAir())return old;double h=noise(x,z+y);
  Block block=switch(spell){
   case HEAVENS_LANCE->h<.18?Blocks.GLOWSTONE:Blocks.QUARTZ_BLOCK;
   case THOUSAND_BLADES->h<.2?Blocks.AMETHYST_BLOCK:Blocks.CALCITE;
   case TEMPEST_WRATH->h<.06?Blocks.GLOWSTONE:(h<.3?Blocks.TUFF:Blocks.BLACKSTONE);
   case STARFALL->h<.2?Blocks.MAGMA_BLOCK:(h<.6?Blocks.TERRACOTTA:Blocks.BASALT);
   case TECTONIC_RIFT->h<.15?Blocks.MAGMA_BLOCK:(h<.5?Blocks.TUFF:Blocks.ANDESITE);
   case INFERNO_TIDE->h<.25?Blocks.CRIMSON_NYLIUM:(h<.6?Blocks.NETHERRACK:Blocks.BASALT);
   case SINGULARITY->h<.12?Blocks.CRYING_OBSIDIAN:Blocks.POLISHED_BLACKSTONE;
   case SOLAR_NOVA->h<.3?Blocks.GLOWSTONE:(h<.55?Blocks.ORANGE_STAINED_GLASS:Blocks.OBSIDIAN);
   case LAST_CATACLYSM->h<.15?Blocks.AMETHYST_BLOCK:(h<.5?Blocks.DEEPSLATE:Blocks.POLISHED_BLACKSTONE);
   case VERDANT_RUIN->h<.12?Blocks.MOSSY_COBBLESTONE:(h<.35?Blocks.ROOTED_DIRT:Blocks.COARSE_DIRT);
   case OBSIDIAN_CATHEDRAL->h<.15?Blocks.CRYING_OBSIDIAN:Blocks.OBSIDIAN;
   case ABYSSAL_DELUGE->h<.2?Blocks.SEA_LANTERN:(h<.5?Blocks.GRAVEL:Blocks.DARK_PRISMARINE);
   case WORLD_SUNDER->h<.14?Blocks.MAGMA_BLOCK:(h<.45?Blocks.TUFF:Blocks.COBBLED_DEEPSLATE);
   case RUNE_APOCALYPSE->h<.12?Blocks.GLOWSTONE:Blocks.OBSIDIAN;
   default->h<.2?Blocks.MAGMA_BLOCK:Blocks.POLISHED_BLACKSTONE;
  };return block.defaultBlockState();
 }
}
