package com.kostya.cataclysm.client;
import com.kostya.cataclysm.*;
import net.minecraft.world.phys.Vec3;
/** World-space, staged magical set pieces. No planets, star fields, or camera teleports. */
public final class RitualRenderer {
 static void surface(ClientEffects.Fx f,float t,float fade,int color,int mode){FxMesh.begin(mode==2?FxMesh.surface:FxMesh.energy,t,color,f.spell.secondary,fade,(mode==2&&f.spell!=Spell.GLACIAL_RAGNAROK)?6:mode,1,4,0,mode==2);}
 static void light(ClientEffects.Fx f,float t,float fade){FxMesh.begin(FxMesh.glow,t,f.spell.color,f.spell.secondary,fade,3,2,.65f,0,false);}
 static void rune(ClientEffects.Fx f,float t,float fade,int pattern){FxMesh.begin(FxMesh.rune,t,f.spell.color,f.spell.secondary,fade,pattern,t*.03f,1,8,true);}
 public static void draw(ClientEffects.Fx f,float t,float age,float fade){
  Vec3 p=f.packet.target();double s=f.packet.scale(),charge=Math.min(1,age/f.spell.impact),after=age-f.spell.impact;
  double grow=ClientEffects.smooth((charge-.08)/.55),release=after<0?1:Math.max(0,1-after/100.);float a=fade*(float)release;
  double radius=Math.min(95*s,f.radius()*.7),rise=(after<0?grow:1)*s;
  switch(f.spell){
   case HEAVENS_LANCE->{
    // A cathedral of invocatory gates closes onto a descending holy spear.
    surface(f,t,a*.65f,0xd9ceb2,2);
    for(int i=0;i<8;i++){double th=i*Math.PI/4;Vec3 at=p.add(Math.cos(th)*radius*.6,0,Math.sin(th)*radius*.6);FxMesh.box(at,2*s,78*rise,2*s);FxMesh.crystal(at.add(0,78*rise,0),18*rise,5*s,th);}FxMesh.end();
    rune(f,t,a*.9f,11);for(int i=0;i<3;i++)FxMesh.verticalSeal(p.add(0,(28+i*26)*rise,0),24*s,t*.04+i*Math.PI/3);FxMesh.end();
    surface(f,t,a,f.spell.color,4);double descend=after<0?100*(1-ClientEffects.smooth((charge-.6)/.4))*s:-25*s;FxMesh.blade(p.add(0,descend,0),125*s,0);FxMesh.end();
    if(charge>.85){light(f,t,fade);FxMesh.beam(p.add(0,130*s,0),p.add(0,after<0?15*s:-50*s,0),after<0?1*s:7*s,24);FxMesh.end();}
   }
   case THOUSAND_BLADES->{
    rune(f,t,a,29);for(int i=0;i<8;i++){double th=i*Math.PI/4;FxMesh.verticalSeal(p.add(Math.cos(th)*radius,32*rise,Math.sin(th)*radius),18*s,th+Math.PI/2);}FxMesh.end();
    surface(f,t,fade*.85f,f.spell.color,4);for(int i=0;i<64;i++){Vec3 q=f.strikes.get(i%f.strikes.size());double drop=ClientEffects.smooth((charge-.65-i*.002)/.35);FxMesh.blade(q.add(0,(1-drop)*(60+i%5*9)*s,0),(15+i%4*5)*s,t*.03+i);}FxMesh.end();
   }
   case GLACIAL_RAGNAROK->{
    // Six frozen tombstones and a suspended snowflake coffin, not an orbiting ice planet.
    surface(f,t,a*.8f,f.spell.color,2);for(int i=0;i<6;i++){double th=i*Math.PI/3;Vec3 at=p.add(Math.cos(th)*radius*.7,0,Math.sin(th)*radius*.7);FxMesh.box(at,5*s,50*rise,4*s);FxMesh.crystal(at.add(0,50*rise,0),24*rise,8*s,th);}FxMesh.box(p.add(0,6*rise,0),12*s,38*rise,7*s);FxMesh.end();
    light(f,t,a);for(int i=0;i<6;i++){double th=i*Math.PI/3;Vec3 tip=p.add(Math.cos(th)*radius,65*rise,Math.sin(th)*radius);FxMesh.ribbon(p.add(0,65*rise,0),tip,.7*s);for(int j=1;j<4;j++){Vec3 mid=p.add(Math.cos(th)*radius*j/4,65*rise,Math.sin(th)*radius*j/4);FxMesh.ribbon(mid,mid.add(Math.cos(th+.8)*8*s,0,Math.sin(th+.8)*8*s),.4*s);}}FxMesh.end();
    if(after>=0){surface(f,t,fade,f.spell.color,2);for(int i=0;i<36;i++){double th=i*2.399;FxMesh.crystal(p.add(Math.cos(th)*radius*(i%5+1)/5,0,Math.sin(th)*radius*(i%5+1)/5),Math.min(1,after/40)*(20+i%6*8)*s,3*s,th);}FxMesh.end();}
   }
   case TEMPEST_WRATH->{
    // Ritual conductors, a storm canopy, branching bolts and a seven-point circuit.
    FxMesh.begin(FxMesh.glow,t,0x17182d,0x655b89,a*.65f,4,1,3,0,true);for(int i=0;i<18;i++){double th=i*2.399;FxMesh.billboard(p.add(Math.cos(th)*radius*.7,100*rise+(i%3)*4*s,Math.sin(th)*radius*.7),25*s);}FxMesh.end();
    surface(f,t,a*.8f,0x6c6988,2);for(int i=0;i<7;i++){double th=i*Math.PI*2/7;FxMesh.box(p.add(Math.cos(th)*radius*.7,0,Math.sin(th)*radius*.7),2*s,(24+i%3*7)*rise,2*s);}FxMesh.end();
    light(f,t,fade);int bolts=after<0?(int)(charge*9):28;for(int i=0;i<bolts;i++){Vec3 q=f.strikes.get(i%f.strikes.size());Vec3 last=p.add(q.x-p.x,105*rise,q.z-p.z);for(int j=1;j<=9;j++){Vec3 next=q.add(Math.sin(i*7+j*17+t*19)*5*s,105*rise*(1-j/9.),Math.cos(i*11+j*13+t*19)*5*s);FxMesh.ribbon(last,next,(after<0?.35:1.7)*s);last=next;}}FxMesh.end();
   }
   case TECTONIC_RIFT,WORLD_SUNDER->{
    surface(f,t,a*.9f,0x93877c,2);int count=f.spell==Spell.WORLD_SUNDER?14:8;for(int i=0;i<count;i++){double th=i*Math.PI*2/count;Vec3 at=p.add(Math.cos(th)*radius*.7,30*rise+Math.sin(t+i)*3*s,Math.sin(th)*radius*.7);FxMesh.box(at,6*s,18*rise,5*s);FxMesh.crystal(at.add(0,18*rise,0),10*s,4*s,th);}FxMesh.box(p.add(0,95*(1-charge)*s+35*s,0),18*s,22*s,12*s);FxMesh.end();
    light(f,t,fade);for(int i=0;i<(f.spell==Spell.WORLD_SUNDER?7:2);i++){double th=i*Math.PI/(f.spell==Spell.WORLD_SUNDER?7:1);Vec3 dir=new Vec3(Math.cos(th),0,Math.sin(th));for(int j=-12;j<12;j++){Vec3 q=p.add(dir.scale(radius*j/12));FxMesh.ribbon(q.add(0,.5,0),q.add(dir.scale(radius/12)).add(0,.5,0),(after<0?.5:4)*s);}}FxMesh.end();
   }
   case INFERNO_TIDE->{
    surface(f,t,a*.85f,0x753a37,2);FxMesh.box(p.add(-27*s,0,0),5*s,95*rise,6*s);FxMesh.box(p.add(27*s,0,0),5*s,95*rise,6*s);FxMesh.box(p.add(0,85*rise,0),32*s,8*s,6*s);FxMesh.crystal(p.add(-27*s,92*rise,0),25*rise,5*s,-.6);FxMesh.crystal(p.add(27*s,92*rise,0),25*rise,5*s,.6);FxMesh.end();
    surface(f,t,a*.8f,f.spell.color,5);FxMesh.wall(p,after<0?18*s:Math.min(f.radius(),after/80*f.radius()),after<0?65*rise:40*s,96);FxMesh.end();
    rune(f,t,a,44);FxMesh.verticalSeal(p.add(0,45*rise,-2*s),25*s,0);FxMesh.end();
   }
   case LAST_CATACLYSM,RUNE_APOCALYPSE->{
    // An arcane court: stacked seals, twelve tablets and the execution obelisk.
    int count=f.spell==Spell.RUNE_APOCALYPSE?12:8;surface(f,t,a*.8f,0x655777,2);for(int i=0;i<count;i++){double th=i*Math.PI*2/count+t*.025;Vec3 q=p.add(Math.cos(th)*radius*.72,(14+i%3*12)*rise,Math.sin(th)*radius*.72);FxMesh.box(q,5*s,34*rise,2*s);}FxMesh.crystal(p.add(0,12*s,0),120*rise,14*s,t*.05);FxMesh.end();
    rune(f,t,a*.8f,61);for(int i=0;i<4;i++){FxMesh.disk(p.add(0,(10+i*28)*rise,0),(26+i*12)*s,t*.02);if(i<3)FxMesh.verticalSeal(p.add(0,58*rise,0),45*s,i*Math.PI/3);}FxMesh.end();
    if(after>=0){light(f,t,fade);FxMesh.beam(p.add(0,-30*s,0),p.add(0,190*s,0),12*s,32);FxMesh.end();}
   }
   case VERDANT_RUIN->{
    surface(f,t,a*.9f,0x79c976,2);FxMesh.box(p,8*s,100*rise,8*s);for(int i=0;i<12;i++){double th=i*2.399;Vec3 q=p.add(Math.cos(th)*(22+i%3*8)*rise,(40+i%5*10)*rise,Math.sin(th)*(22+i%3*8)*rise);FxMesh.crystal(q,24*rise,12*s,th);}FxMesh.end();
    light(f,t,fade*.8f);for(int i=0;i<18;i++){double th=i*Math.PI*2/18;Vec3 last=p;for(int j=1;j<9;j++){Vec3 next=p.add(Math.cos(th+j*.07)*radius*j/8,Math.sin(j*Math.PI/8)*15*rise,Math.sin(th+j*.07)*radius*j/8);FxMesh.ribbon(last,next,2*s);last=next;}}FxMesh.end();
   }
   case OBSIDIAN_CATHEDRAL->{
    surface(f,t,a*.85f,0x796085,2);for(int i=0;i<12;i++){double th=i*Math.PI/6;Vec3 q=p.add(Math.cos(th)*radius*.65,0,Math.sin(th)*radius*.65);FxMesh.box(q,4*s,85*rise,4*s);FxMesh.crystal(q.add(0,85*rise,0),28*rise,7*s,th);}FxMesh.end();
    rune(f,t,a*.8f,72);for(int i=0;i<6;i++){double th=i*Math.PI/3;FxMesh.verticalSeal(p.add(Math.cos(th)*radius*.57,50*rise,Math.sin(th)*radius*.57),24*s,th+Math.PI/2);}FxMesh.end();
    light(f,t,a);for(int i=0;i<12;i++){double th=i*Math.PI/6,th2=(i+1)*Math.PI/6;FxMesh.ribbon(p.add(Math.cos(th)*radius*.65,82*rise,Math.sin(th)*radius*.65),p.add(Math.cos(th2)*radius*.65,82*rise,Math.sin(th2)*radius*.65),1*s);}FxMesh.end();
   }
   case ABYSSAL_DELUGE->{
    surface(f,t,a*.65f,0x52c7e4,5);for(int i=0;i<4;i++)FxMesh.wall(p,(24+i*16)*s,(20+i*15)*rise,80);FxMesh.end();
    rune(f,t,a,85);for(int i=0;i<5;i++){double th=i*Math.PI*2/5;FxMesh.verticalSeal(p.add(Math.cos(th)*radius*.75,40*rise,Math.sin(th)*radius*.75),25*s,th);}FxMesh.end();
    surface(f,t,a*.85f,f.spell.color,4);for(int i=-1;i<=1;i++)FxMesh.blade(p.add(i*12*s,30*rise+Math.abs(i)*8*s,0),65*rise,0);FxMesh.end();
   }
   default->{}
  }
 }
 private RitualRenderer(){}
}
