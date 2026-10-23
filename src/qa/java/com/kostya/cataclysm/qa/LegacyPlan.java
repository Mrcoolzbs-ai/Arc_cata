package com.kostya.cataclysm.qa;

import com.kostya.cataclysm.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Pure column geometry; shared deterministic patterns do not read/load other chunks. */
public final class LegacyPlan {
    public final Spell spell;
    public final Vec3 center, direction;
    public final long seed;
    public final double scale, radius;
    private final List<Vec3> strikes;
    public record Column(int bottom,int top,int floor,int spike,boolean freeze,boolean scorch) {}
    public LegacyPlan(Spell spell,Vec3 center,Vec3 direction,long seed,double scale){
        this.spell=spell;this.center=center;this.direction=direction;this.seed=seed;this.scale=scale;
        radius=spell.radius*scale;
        strikes=SpellPatterns.strikes(center,seed,radius*.86,spell==Spell.TEMPEST_WRATH?28:36);
    }
    public Column column(int x,int z,int terrain,int min,int max){
        double dx=x+.5-center.x,dz=z+.5-center.z,d=Math.hypot(dx,dz),r=radius;
        if(d>r+2)return null;
        double n=(SpellPatterns.hash(x,0,z,seed)-.5)*2;
        int floor=terrain,hi=terrain,spike=0;boolean frost=false,scorch=false;
        switch(spell){
            case STARFALL -> {if(d>r)return null;floor=(int)(center.y-36*scale*(1-d*d/(r*r))+n);hi=Math.max(terrain,(int)(center.y+r*.7));}
            case HEAVENS_LANCE -> {if(d>r)return null;floor=min+5+(int)(6*Math.pow(d/r,6));hi=max-1;}
            case SINGULARITY -> {if(d>r)return null;double h=Math.sqrt(r*r-d*d);floor=(int)(center.y+18*scale-h);hi=(int)(center.y+18*scale+h);}
            case GLACIAL_RAGNAROK -> {
                if(d>r||terrain<min+4)return null;frost=true;
                int gx=Math.floorDiv(x,16),gz=Math.floorDiv(z,16);
                double px=gx*16+3+SpellPatterns.hash(gx,3,gz,seed)*10,pz=gz*16+3+SpellPatterns.hash(gx,5,gz,seed)*10;
                double q=Math.hypot(x-px,z-pz),size=3.0+SpellPatterns.hash(gx,8,gz,seed)*3;
                if(q<size)spike=(int)((1-q/size)*(10+30*SpellPatterns.hash(gx,9,gz,seed))*scale*(1-d/r*.4));
            }
            case TEMPEST_WRATH,THOUSAND_BLADES -> {
                double closest=1e8;for(Vec3 p:strikes)closest=Math.min(closest,Math.hypot(x+.5-p.x,z+.5-p.z));
                double cut=spell==Spell.TEMPEST_WRATH?10*scale:7*scale;
                if(d<r*.25){closest=Math.min(closest,d);cut=Math.max(cut,r*.25);}
                if(spell==Spell.THOUSAND_BLADES){closest=Math.min(closest,Math.min(Math.abs(dx-dz),Math.abs(dx+dz))*.7071);cut=7*scale;}
                if(closest>cut)return null;floor=(int)(center.y-(spell==Spell.TEMPEST_WRATH?16:28)*scale*(1-closest/cut));hi=terrain;
            }
            case TECTONIC_RIFT -> {
                double len=Math.hypot(direction.x,direction.z),ux=len<.01?1:direction.x/len,uz=len<.01?0:direction.z/len;
                double along=dx*ux+dz*uz,across=Math.abs(-dx*uz+dz*ux+Math.sin(along*.045)*5*scale);
                double width=22*scale*Math.sqrt(Math.max(0,1-along*along/(r*r)));
                if(across>width||width<.1)return null;floor=(int)(center.y-65*scale*(1-across/width));hi=terrain;
            }
            case SOLAR_NOVA -> {if(d>r)return null;floor=(int)(center.y-50*scale*(1-d*d/(r*r))+n);hi=Math.max(terrain,(int)(center.y+r*.6));}
            case INFERNO_TIDE -> {if(d>r)return null;scorch=true;floor=terrain-(int)(3+7*(1-d/r));}
            case LAST_CATACLYSM -> {
                if(d>r)return null;double core=176*scale;
                if(d<core){floor=(int)(center.y-85*scale*(1-d*d/(core*core))+n*2);hi=Math.max(terrain,(int)(center.y+core*.6));}
                else {scorch=true;floor=terrain-2;}
            }
        }
        floor=Math.max(min+4,Math.min(max-2,floor));hi=Math.max(floor,Math.min(max-1,hi));
        if(frost){floor=terrain;hi=Math.min(max-1,terrain+spike);}
        return new Column(Math.max(min+4,floor-3),hi,floor,spike,frost,scorch);
    }
    public BlockState state(Column c,int x,int y,int z,BlockState old){
        if(c.freeze){
            if(y>c.floor)return Blocks.BLUE_ICE.defaultBlockState();
            if(old.isAir())return old;
            if(!old.getFluidState().isEmpty())return Blocks.ICE.defaultBlockState();
            return (y==c.floor?Blocks.SNOW_BLOCK:Blocks.PACKED_ICE).defaultBlockState();
        }
        if(y>c.floor)return Blocks.AIR.defaultBlockState();
        if(old.isAir())return old;
        double h=SpellPatterns.hash(x,y,z,seed);
        Block b=switch(spell){
            case HEAVENS_LANCE -> h<.2?Blocks.SEA_LANTERN:Blocks.OBSIDIAN;
            case SINGULARITY -> h<.15?Blocks.CRYING_OBSIDIAN:Blocks.POLISHED_BLACKSTONE;
            case GLACIAL_RAGNAROK -> Blocks.PACKED_ICE;
            case TEMPEST_WRATH -> h<.14?Blocks.OBSIDIAN:Blocks.BLACKSTONE;
            case THOUSAND_BLADES -> h<.15?Blocks.AMETHYST_BLOCK:Blocks.CALCITE;
            case SOLAR_NOVA -> h<.4?Blocks.ORANGE_STAINED_GLASS:Blocks.OBSIDIAN;
            case TECTONIC_RIFT -> h<.35?Blocks.MAGMA_BLOCK:Blocks.DEEPSLATE;
            default -> h<.28?Blocks.MAGMA_BLOCK:(h<.65?Blocks.BLACKSTONE:Blocks.BASALT);
        };
        // Stable magma instead of ticking lava/fire: mass edits must not create a fluid-update storm.
        return b.defaultBlockState();
    }
}
