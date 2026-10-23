package com.kostya.cataclysm;
import net.minecraft.world.phys.Vec3;
import java.util.*;
/** Deterministic geometry: never relies on mutable world/client random sources. */
public final class SpellPatterns {
    public static List<Vec3> strikes(Vec3 target,long seed,double radius,int count){
        Random r=new Random(seed);List<Vec3> out=new ArrayList<>();
        for(int i=0;i<count;i++){
            double a=i*2.399963229728653+r.nextDouble()*.28,d=radius*Math.sqrt((i+.5)/count);
            out.add(target.add(Math.cos(a)*d,0,Math.sin(a)*d));
        }return out;
    }
    public static double hash(int x,int y,int z,long seed){
        long v=x*73428767L^y*912931L^z*438289L^seed;v=(v^(v>>>33))*0xff51afd7ed558ccdL;v^=v>>>33;
        return (v&0xffffff)/(double)0x1000000;
    }
    private SpellPatterns(){}
}
