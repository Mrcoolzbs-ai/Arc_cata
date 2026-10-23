package com.kostya.cataclysm.world;

import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Immutable final-state patch. Worker inputs are copied states; no Level/Chunk references. */
public final class PreparedChunk {
    public record Sample(DestructionPlan.Column geometry,BlockState[] states,BitSet immune){}
    public record Run(int from,int to,BlockState state){}
    public final DestructionPlan.Column[] columns=new DestructionPlan.Column[256];
    public final Run[][] edits=new Run[256][];
    public final long changes;
    public PreparedChunk(DestructionPlan plan,int minX,int minZ,Sample[] samples){
        long changed=0;
        for(int col=0;col<256;col++){
            Sample s=samples[col];if(s==null){edits[col]=new Run[0];continue;}
            var c=s.geometry;columns[col]=c;
            int x=minX+(col&15),z=minZ+(col>>4);var runs=new ArrayList<Run>();
            int first=Integer.MIN_VALUE,last=0;BlockState value=null;
            for(int i=0;i<s.states.length;i++){
                int y=c.bottom()+i;BlockState old=s.states[i];
                BlockState next=s.immune.get(i)?old:plan.state(c,x,y,z,old);
                if(next==old){if(first!=Integer.MIN_VALUE){runs.add(new Run(first,last,value));first=Integer.MIN_VALUE;}continue;}
                changed++;
                if(first!=Integer.MIN_VALUE&&last+1==y&&value==next){last=y;continue;}
                if(first!=Integer.MIN_VALUE)runs.add(new Run(first,last,value));
                first=y;last=y;value=next;
            }
            if(first!=Integer.MIN_VALUE)runs.add(new Run(first,last,value));
            edits[col]=runs.toArray(Run[]::new);
        }
        changes=changed;
    }
}
