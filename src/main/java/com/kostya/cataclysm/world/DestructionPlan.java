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
   // 0.8.0 #1: a narrow sky bore with a cross of lance trenches and a central light spire.
   case HEAVENS_LANCE->{floor=min+(int)(5+12*Math.pow(u,5));hi=max-1;if(Math.abs(d-r*.73)<Math.max(1.2,2.5*scale)&&Math.abs(Math.sin(a*4))<.22)spike=Math.max(0,terrain-floor)+(int)(34*scale);if(u<.05)spike=Math.max(spike,(int)(96*scale));}
   // 0.8.0 #2: a lattice of blade cuts; the sharpest cuts now leave crystal blades standing.
   case THOUSAND_BLADES->{double cut=7*scale,lines=Math.min(Math.abs(dx-dz),Math.abs(dx+dz))*.7071;closest=Math.min(closest,lines);if(closest>cut)return null;floor=(int)(center.y-48*scale*(1-closest/cut));if(closest<Math.max(.7,1.2*scale)&&Math.sin(a*11+d*.07)>.72)spike=Math.max(0,terrain-floor)+(int)(35*scale);}
   // 0.8.0 #3: the frost grave - per-cell ice clusters and a central blue-ice monolith.
   case GLACIAL_RAGNAROK->{if(terrain<min)return null;frost=true;int gx=Math.floorDiv(x,16),gz=Math.floorDiv(z,16);double px=gx*16+3+SpellPatterns.hash(gx,3,gz,seed)*10,pz=gz*16+3+SpellPatterns.hash(gx,5,gz,seed)*10;double q=Math.hypot(x-px,z-pz),size=3+SpellPatterns.hash(gx,8,gz,seed)*3;if(q<size)spike=(int)((1-q/size)*(22+60*SpellPatterns.hash(gx,9,gz,seed))*scale*(1-u*.4));if(u<.21)spike=Math.max(spike,(int)(64*scale*Math.sqrt(1-u*u/(.21*.21))));}
   // 0.8.0 #4: storm - strike channels, and copper rod spires where the bolts touched down.
   case TEMPEST_WRATH->{double branch=Math.abs(Math.sin(a*7+d*.025))*d;if(closest>12*scale&&branch>3*scale)return null;floor=(int)(center.y-42*scale*Math.max(.15,1-closest/(15*scale)));if(closest<2*scale)spike=Math.max(0,terrain-floor)+(int)((15+noise(z,x)*35)*scale);}
   // 0.8.0 #5: meteor shower - local pocks under the fallen; some keep basalt-shelled, magma-cored meteorites.
   case STARFALL->{floor=(int)(center.y-80*scale*(1-u*u)+n*3);hi=Math.max(terrain,(int)(center.y+r*.55));double near=1e8;for(Vec3 p:strikes)near=Math.min(near,Math.hypot(x+.5-p.x,z+.5-p.z));if(near<6*scale)floor=Math.min(floor,(int)(center.y-20*scale));if(near<2.2*scale&&SpellPatterns.hash(x,11,z,seed)>.82)spike=Math.max(0,terrain-floor)+(int)(14*scale);if(u>.83)spike=Math.max(spike,(int)(Math.sin((u-.83)/.17*Math.PI)*12*scale));}
   // 0.8.0 #6: a fresh volcanic rift - andesite terraces, magma seam down the middle.
   case TECTONIC_RIFT->{double len=Math.hypot(direction.x,direction.z),ux=len<.01?1:direction.x/len,uz=len<.01?0:direction.z/len;double along=dx*ux+dz*uz,across=Math.abs(-dx*uz+dz*ux+Math.sin(along*.045)*7*scale),width=42*scale*Math.sqrt(Math.max(0,1-along*along/(r*r)));if(across>width||width<.1)return null;floor=(int)(center.y-125*scale*(1-across/width));if(across/width>.78)spike=(int)(25*scale*(1-noise(x,z)));}
   // 0.8.0 #7: a fire tide - scorch rings with magma wave crests rolling outward.
   case INFERNO_TIDE->{double rings=Math.sin(d/Math.max(1,9*scale));floor=terrain-(int)((12+26*(1-u))*scale+rings*4*scale);if(rings>.93&&u>.25)spike=(int)(20*scale);}
   // 0.8.0 #8: a spherical gravity void ringed by weeping obsidian.
   case SINGULARITY->{double h=Math.sqrt(r*r-d*d);floor=(int)(center.y+18*scale-h);hi=(int)(center.y+18*scale+h);if(u>.79&&u<.86)spike=(int)(18*scale);}
   // 0.8.0 #9: solar corona - an obsidian basin with glass rays.
   case SOLAR_NOVA->{floor=(int)(center.y-112*scale*(1-u*u));hi=Math.max(terrain,(int)(center.y+r*.7));if(u>.88)spike=(int)(Math.max(0,Math.sin(a*23))*20*scale);}
   // 0.8.0 #10: the cataclysm - stepped terraces, amethyst veins, a crystal ring wall.
   case LAST_CATACLYSM->{floor=(int)(center.y-150*scale*(1-u*u));floor=(int)(Math.floor(floor/8.)*8);hi=Math.max(terrain,(int)(center.y+110*scale));if(Math.abs(d-r*.75)<4*scale&&Math.abs(Math.sin(a*6))<.15)spike=Math.max(0,terrain-floor)+(int)(70*scale);}
   // 0.8.0 #11: withering - a barren crater where dead trunks still stand.
   case VERDANT_RUIN->{floor=(int)(center.y-165*scale*(1-u*u));hi=terrain+(int)(19*scale);if(noise(x,z)>.86)spike=(int)(18*scale);}
   // 0.8.0 #12: the cathedral - obsidian buttresses with purpur bands and a central spire.
   case OBSIDIAN_CATHEDRAL->{floor=(int)(center.y-178*scale*(1-u*u));hi=Math.max(terrain,(int)(center.y+110*scale));double ring=Math.abs(d-r*.68);if(ring<5*scale&&Math.abs(Math.sin(a*8))<.22)spike=Math.max(0,terrain-floor)+(int)(110*scale);if(u<.075)spike=Math.max(0,terrain-floor)+(int)(145*scale);}
   // 0.8.0 #13: the deluge - a prismarine trench with gravel shoals, flooded below sea level.
   case ABYSSAL_DELUGE->{floor=(int)(center.y-195*scale*Math.pow(1-u*u,.6));hi=Math.max(terrain,0);if(u>.86&&Math.abs(Math.sin(a*17))>.97)spike=Math.max(0,terrain-floor)+(int)(70*scale);}
   // 0.8.0 #14: world sunder - continental plates of deepslate torn along glowing faults.
   case WORLD_SUNDER->{double fault=Math.abs(Math.sin(a*7+d*.006));floor=(int)(center.y-(85+155*(1-fault))*scale*(1-u*.65));hi=Math.max(terrain,(int)(center.y+90*scale));if(fault>.9)spike=Math.max(0,terrain-floor)+(int)(55*scale*(1-u));}
   case END_OF_ALL->{return new Column(min,max-1,min-1,0,false,false,terrain);}
   // 0.8.0 #15: rune apocalypse - glowing rune bands and obsidian monoliths.
   case RUNE_APOCALYPSE->{floor=(int)(center.y-280*scale*(1-u*.65));hi=max-1;double bands=Math.abs(Math.sin(d/(Math.max(1,28*scale))));if(bands<.09&&Math.abs(Math.sin(a*12))<.22)spike=Math.max(0,terrain-floor)+(int)(100*scale);if(u<.05)spike=0;}
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
   if(y<=c.floor+c.spike)return built(c,x,y,z);
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
 private BlockState built(Column c,int x,int y,int z){
  int peak=c.floor+c.spike;double h=noise(x,z+y);Block b=switch(spell){
   case HEAVENS_LANCE->y==peak?Blocks.GLOWSTONE:(y%7==0?Blocks.CHISELED_QUARTZ_BLOCK:Blocks.QUARTZ_PILLAR);
   case THOUSAND_BLADES->Blocks.AMETHYST_BLOCK;
   case TEMPEST_WRATH->y==peak?Blocks.WAXED_COPPER_BLOCK:Blocks.POLISHED_BASALT;
   case STARFALL->y==peak?Blocks.MAGMA_BLOCK:Blocks.BASALT;
   case TECTONIC_RIFT->y==peak?Blocks.MAGMA_BLOCK:Blocks.ANDESITE;
   case INFERNO_TIDE->Blocks.MAGMA_BLOCK;
   case SOLAR_NOVA->y==peak?Blocks.YELLOW_STAINED_GLASS:Blocks.ORANGE_STAINED_GLASS;
   case SINGULARITY->y==peak?Blocks.CRYING_OBSIDIAN:Blocks.OBSIDIAN;
   case LAST_CATACLYSM->y%8==0?Blocks.AMETHYST_BLOCK:Blocks.POLISHED_BLACKSTONE;
   case VERDANT_RUIN->y==peak?Blocks.MOSS_BLOCK:(y>peak-3?Blocks.DARK_OAK_WOOD:Blocks.STRIPPED_DARK_OAK_WOOD);
   case OBSIDIAN_CATHEDRAL->y%11==0?Blocks.CRYING_OBSIDIAN:(y%13==0?Blocks.PURPUR_BLOCK:Blocks.OBSIDIAN);
   case ABYSSAL_DELUGE->y==peak?Blocks.SEA_LANTERN:Blocks.PRISMARINE_BRICKS;
   case WORLD_SUNDER->y==peak?Blocks.TUFF:Blocks.COBBLED_DEEPSLATE;
   case RUNE_APOCALYPSE->y%9==0?Blocks.GLOWSTONE:(y==peak?Blocks.CHISELED_STONE_BRICKS:Blocks.OBSIDIAN);
   default->Blocks.BASALT;
  };return b.defaultBlockState();
 }
}
