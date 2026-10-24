package com.kostya.cataclysm.qa;
import com.kostya.cataclysm.*;
import com.kostya.cataclysm.world.*;
import com.kostya.cataclysm.network.SpellNetwork;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import io.netty.buffer.Unpooled;
import java.util.*;
public final class V4PureQA {
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){
  SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
  require(Spell.values().length==16,"catalogue");long checks=0,changes=0;Random rng=new Random(43);
  for(Spell spell:Spell.values())for(double scale:new double[]{.1,1.,1.5})for(int terrain:new int[]{-65,-60,0,60,319}){
   var plan=new DestructionPlan(spell,new Vec3(-19.5,60,-7.5),new Vec3(.8,0,.6),717,scale);
   for(int k=0;k<2000;k++){
    int x=(int)Math.floor(plan.center.x+(rng.nextDouble()*2-1)*plan.radius),z=(int)Math.floor(plan.center.z+(rng.nextDouble()*2-1)*plan.radius);
    var c=plan.column(x,z,terrain,-64,320);if(c==null)continue;
    require(c.bottom()>=-64&&c.top()<320&&c.bottom()<=c.top(),"column bounds "+spell);
    if(spell==Spell.THOUSAND_BLADES||spell==Spell.TEMPEST_WRATH||spell==Spell.INFERNO_TIDE||spell==Spell.VERDANT_RUIN){require(c.spike()==0,"unwanted structure");for(int y=c.bottom();y<=c.top();y++)require(plan.state(c,x,y,z,Blocks.AIR.defaultBlockState()).isAir(),"unwanted fill "+spell);}
    checks++;
   }
   var samples=new PreparedChunk.Sample[256];BlockState[][] originals=new BlockState[256][];
   for(int i=0;i<256;i++){
    int x=-16+(i&15),z=-16+(i>>4);var c=plan.column(x,z,terrain,-64,320);if(c==null)continue;
    BlockState[] states=new BlockState[c.top()-c.bottom()+1];BitSet protectedBits=new BitSet();
    for(int q=0;q<states.length;q++){
     int y=c.bottom()+q;states[q]=y<=terrain?(y==-64?Blocks.BEDROCK:((q%83==0)?Blocks.CHEST:Blocks.STONE)).defaultBlockState():Blocks.AIR.defaultBlockState();
     if(q%53==0)protectedBits.set(q);
    }
    originals[i]=states.clone();samples[i]=new PreparedChunk.Sample(c,states,protectedBits);
   }
   PreparedChunk patch=new PreparedChunk(plan,-16,-16,samples);long actual=0;
   for(int i=0;i<256;i++)if(samples[i]!=null){
    var sample=samples[i];var c=sample.geometry();BlockState[] finalStates=sample.states().clone();
    for(var run:patch.edits[i]){require(run.from()<=run.to(),"run inverted");for(int y=run.from();y<=run.to();y++){finalStates[y-c.bottom()]=run.state();actual++;}}
    for(int q=0;q<finalStates.length;q++){
     int y=c.bottom()+q;BlockState expected=sample.immune().get(q)?originals[i][q]:plan.state(c,-16+(i&15),y,-16+(i>>4),originals[i][q]);
     require(finalStates[q]==expected,"patch mismatch "+spell);require(sample.states()[q]==originals[i][q],"snapshot mutated");
    }
   }
   require(actual==patch.changes,"changed count");changes+=actual;
  }
  long waterChecks=0;
  for(double sy:new double[]{-60,0,60,200})for(double scale:new double[]{.1,1.,1.5})for(int min:new int[]{-64,-128,16}){
   var plan=new DestructionPlan(Spell.ABYSSAL_DELUGE,new Vec3(-.5,sy,-.5),new Vec3(0,0,1),42,scale);
   var c=plan.column(-1,-1,(int)sy,min,320);
   for(int y=c.floor()+1;y<=c.top();y++){
    var state=plan.state(c,-1,y,-1,Blocks.AIR.defaultBlockState());
    if(y<=0)require(state.is(Blocks.WATER),"deluge missing water at y="+y+" target="+sy);
    else require(state.isAir(),"deluge filled above y=0 at "+y);
    waterChecks++;
   }
   if(c.floor()<0&&min<=0)require(c.top()>=0,"deluge snapshot misses sea surface");
  }
  System.out.println("AC061_WATER_PASS y=0-inclusive states="+waterChecks+" targets=-60,0,60,200 minY=-128,-64,16 scales=.1,1,1.5");
  System.out.println("AC4_PATCH_PASS geometryColumns="+checks+" replacementStates="+changes);
  for(int n=1;n<=240;n++){
   var ids=new ArrayList<UUID>();for(int i=0;i<n;i++)ids.add(new UUID(n,i));UUID caster=ids.get(n-1);
   var groups=EndSchedule.assign(ids,caster,42);require(groups.size()==n,"group missing");require(groups.get(caster)==5,"caster not last");
   int[] sizes=new int[6];for(int g:groups.values())sizes[g]++;require(Arrays.stream(sizes).max().getAsInt()-Arrays.stream(sizes).min().getAsInt()<=1,"unbalanced groups");
   require(groups.equals(EndSchedule.assign(ids,caster,42)),"nondeterministic schedule");
  }
  require(EndSchedule.WAVES.equals(List.of(9*20,17*20,21*20,25*20,29*20,42*20)),"timestamps");require(EndEvent.DURATION==45*20,"duration");
  var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);var state=new SpellNetwork.EndState(917,840,true);SpellNetwork.EndState.CODEC.encode(b,state);require(state.equals(SpellNetwork.EndState.CODEC.decode(b)),"packet codec");b.release();
  System.out.println("AC4_SCHEDULE_CODEC_PASS players=1..240 waves=9,17,21,25,29,42 duration=45");
  System.out.println("AC4_PURE_ALL_PASS (no live-server or multiplayer claims)");
 }
}
