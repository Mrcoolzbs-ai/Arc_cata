package com.kostya.cataclysm.client;

import com.kostya.cataclysm.*;
import com.kostya.cataclysm.network.SpellNetwork;
import net.minecraft.client.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

@EventBusSubscriber(modid=Cataclysm.MODID,value=Dist.CLIENT)
public final class ClientEffects {
    public static final List<Fx> ACTIVE=new ArrayList<>();
    public static SpellNetwork.Progress latestProgress;
    private static long progressUntil;
    private static CloudStatus savedClouds;
    public static void progress(SpellNetwork.Progress p){latestProgress=p;if(mc().level!=null)progressUntil=mc().level.getGameTime()+140;}

    public static final class Fx {
        public final Spell spell;public final SpellNetwork.Fx packet;public final List<Vec3> strikes;
        public SoundInstance chargeSound,scoreSound;public boolean scoreStarted;public int age;public boolean skipped;int soundDelay=-1;
        // 0.7.4: reality sync - filled by SpellNetwork.Strike when terrain actually flushes to this client.
        public int realStrikes;public double lastStrikeAge=-1;public float strikePower=1;
        // 0.7.8: strikes wait for the terrain change this client has actually applied.
        BlockPos[] probePos;BlockState[] probes;final List<PendingStrike> pendingStrikes=new ArrayList<>();
        Fx(SpellNetwork.Fx packet){this.packet=packet;spell=Spell.byIndex(packet.spell());strikes=SpellPatterns.strikes(packet.target(),packet.seed(),spell.radius*packet.scale()*.86,spell==Spell.TEMPEST_WRATH?40:72);}
        public float age(float partial){return age+partial;}
        public double radius(){return spell.radius*packet.scale();}
    }
    public record View(Vec3 pos,float yaw,float pitch,float roll){}
    private record PendingStrike(SpellNetwork.Strike packet,int age){}
    private static Minecraft mc(){return Minecraft.getInstance();}
    public static void receive(SpellNetwork.Fx packet){
        if(mc().level==null||packet.spell()<0||packet.spell()>=Spell.values().length||!Float.isFinite(packet.scale())||packet.scale()<=0||packet.scale()>1.5)return;
        if(ACTIVE.size()>=12){var old=ACTIVE.removeFirst();SpellFilm.stop(old);if(old.chargeSound!=null)mc().getSoundManager().stop(old.chargeSound);}
        var fx=new Fx(packet);ACTIVE.add(fx);sampleProbes(fx);
        if(mc().player!=null&&packet.caster()==mc().player.getId())mc().getMusicManager().stopPlaying();
        if(SpellFilm.eligible(fx)){SpellFilm.audio(fx);return;}
        fx.chargeSound=new SimpleSoundInstance(Cataclysm.id(fx.spell.id+"_charge"),SoundSource.PLAYERS,.55f,1f,RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true);
        mc().getSoundManager().play(fx.chargeSound);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        Minecraft m=mc();
        while(ClientSetup.CAMERA.consumeClick()){
            ClientOptions.CAMERA.set(!ClientOptions.CAMERA.get());ClientOptions.SPEC.save();
            if(m.player!=null)m.player.displayClientMessage(Component.translatable(ClientOptions.CAMERA.get()?"message.arcane_cataclysm.camera_on":"message.arcane_cataclysm.camera_off"),true);
        }
        while(ClientSetup.SKIP.consumeClick())for(Fx f:ACTIVE)f.skipped=true;
        if(m.level==null||m.player==null){if(savedClouds!=null){m.options.cloudStatus().set(savedClouds);savedClouds=null;}for(Fx f:ACTIVE)SpellFilm.stop(f);ACTIVE.clear();latestProgress=null;return;}
        if(cinematic()!=null){if(savedClouds==null)savedClouds=m.options.cloudStatus().get();m.options.cloudStatus().set(CloudStatus.OFF);}else if(savedClouds!=null){m.options.cloudStatus().set(savedClouds);savedClouds=null;}
        if(m.isPaused())return;
        for(var it=ACTIVE.iterator();it.hasNext();){
            Fx f=it.next();f.age++;SpellFilm.audio(f);confirmStrikes(f);
            if(f.age==f.spell.impact){if(f.chargeSound!=null)m.getSoundManager().stop(f.chargeSound);f.soundDelay=(int)Math.min(40,m.player.position().distanceTo(f.packet.target())/17);}
            if(f.soundDelay>=0 && f.soundDelay--==0&&f.scoreSound==null){sound(f.spell.id+"_impact",.9f,1f);}
            if(f.age>f.spell.duration){SpellFilm.stop(f);if(f.chargeSound!=null)m.getSoundManager().stop(f.chargeSound);it.remove();}
        }
    }
    private static void sound(String id,float volume,float pitch){
        mc().getSoundManager().play(new SimpleSoundInstance(Cataclysm.id(id),SoundSource.PLAYERS,volume,pitch,RandomSource.create(),false,0,SoundInstance.Attenuation.NONE,0,0,0,true));
    }
    public static Fx cinematic(){
        if(!ClientOptions.CAMERA.get()||mc().player==null||mc().player.isSpectator())return null;
        for(int i=ACTIVE.size()-1;i>=0;i--){Fx f=ACTIVE.get(i);if(!f.skipped&&(!SpellFilm.epic(f.spell)||(ClientOptions.ASTRAL.get()&&ClientOptions.POST.get()&&f.age<f.spell.impact+100))&&f.packet.caster()==mc().player.getId())return f;}return null;
    }
    public static float casting(int entity){for(Fx f:ACTIVE)if(f.packet.caster()==entity&&f.age<f.spell.impact)return Math.min(1,f.age/16f);return 0;}
    @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent e){
        if(mc().player==null||mc().screen!=null||!mc().player.isShiftKeyDown())return;
        if(mc().player.getMainHandItem().getItem() instanceof GrimoireItem||mc().player.getOffhandItem().getItem() instanceof GrimoireItem){
            PacketDistributor.sendToServer(new SpellNetwork.Select(e.getScrollDeltaY()>0?-1:1));e.setCanceled(true);
        }
    }
    @SubscribeEvent public static void movement(MovementInputUpdateEvent e){
        if(cinematic()!=null||EndCinematic.running()){var input=e.getInput();input.forwardImpulse=0;input.leftImpulse=0;input.jumping=false;input.up=false;input.down=false;input.left=false;input.right=false;}
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){if(savedClouds!=null){mc().options.cloudStatus().set(savedClouds);savedClouds=null;}for(Fx f:ACTIVE){SpellFilm.stop(f);if(f.chargeSound!=null)mc().getSoundManager().stop(f.chargeSound);}ACTIVE.clear();latestProgress=null;}
    @SubscribeEvent public static void hand(RenderHandEvent e){if(cinematic()!=null||EndCinematic.running())e.setCanceled(true);}
    @SubscribeEvent public static void vanillaHud(RenderGuiLayerEvent.Pre e){
        if(EndCinematic.running()||SpellFilm.running()){if(e.getName().getNamespace().equals("minecraft"))e.setCanceled(true);return;}
        if(cinematic()==null)return;
        if(InvocationDirector.supports(cinematic().spell)){
            if(e.getName().getNamespace().equals("minecraft"))e.setCanceled(true);
            return;
        }
        var n=e.getName();
        if(n.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.CROSSHAIR)||n.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR)||n.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.SELECTED_ITEM_NAME)||n.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_BAR)||n.equals(net.neoforged.neoforge.client.gui.VanillaGuiLayers.EXPERIENCE_LEVEL))e.setCanceled(true);
    }
    /** 0.7.8: a Strike packet is queued, not applied - the punch lands when this client has applied the terrain change. */
    public static void strike(SpellNetwork.Strike p){
        if(mc().level==null||p.spell()<0||p.spell()>=Spell.values().length)return;
        for(Fx f:ACTIVE)if(f.spell.ordinal()==p.spell()&&f.packet.target().distanceToSqr(p.target())<4.){
            f.pendingStrikes.add(new PendingStrike(p,f.age));return;}
    }
    /** 0.7.8: snapshot the impact ring so strikes can wait for the change this player actually sees. */
    private static void sampleProbes(Fx f){
        var level=mc().level;var t=f.packet.target();double r=Math.max(2,f.radius()*.65);
        f.probePos=new BlockPos[9];f.probes=new BlockState[9];
        for(int i=0;i<9;i++){double a=i*Math.PI/4.,x=t.x+Math.cos(a)*r,z=t.z+Math.sin(a)*r;
            int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,(int)Math.floor(x),(int)Math.floor(z))-1;
            f.probePos[i]=new BlockPos((int)Math.floor(x),y,(int)Math.floor(z));f.probes[i]=level.getBlockState(f.probePos[i]);}
    }
    /** 0.7.8: fire when the probe ring actually changed here; the 2 s timeout keeps barrage pacing if this client never sees the terrain. */
    private static void confirmStrikes(Fx f){
        if(f.pendingStrikes.isEmpty()||mc().level==null)return;
        PendingStrike newest=f.pendingStrikes.get(f.pendingStrikes.size()-1);
        if(f.probes==null||f.age-f.pendingStrikes.get(0).age()>40||changedProbes(f)>=3){
            f.pendingStrikes.clear();
            f.lastStrikeAge=f.age;f.realStrikes++;f.strikePower=Math.max(.4f,Math.min(1.25f,newest.packet().intensity()));
            if(f.age>=f.spell.impact)sound(f.spell.id+"_impact",.30f*f.strikePower,.9f+RandomSource.create().nextFloat()*.2f);
        }
    }
    private static int changedProbes(Fx f){
        int n=0;for(int i=0;i<9;i++)if(mc().level.getBlockState(f.probePos[i])!=f.probes[i])n++;return n;
    }
    /** 0.7.4: real casts follow server strike events; preview and visual-only casts keep the synthetic barrage. */
    public static double sinceStrike(Fx f,double age){
        return f.realStrikes>0?age-f.lastStrikeAge:ImpactDynamics.sinceHit(age-f.spell.impact,f.spell.tier());
    }
    public static double shake(float partial){
        if(!ClientOptions.SHAKE.get()||mc().player==null)return 0;
        double amount=0;for(Fx f:ACTIVE){double age=sinceStrike(f,f.age(partial));if(age<0||age>90)continue;
            double distance=mc().player.position().distanceTo(f.packet.target());amount+=ImpactDynamics.shake(age,ImpactDynamics.power(f.spell.tier(),f.spell.radius,f.packet.scale())*f.strikePower,distance);
        }return Math.min(amount,1.3);
    }
    /** 0.7.0: brief hard directional kick on impact (bounded like shake, never server-visible). */
    public static double shove(float partial){
        if(!ClientOptions.SHAKE.get()||mc().player==null)return 0;
        double amount=0;for(Fx f:ACTIVE){double age=sinceStrike(f,f.age(partial));if(age<0||age>=6)continue;
            double distance=mc().player.position().distanceTo(f.packet.target());
            amount+=ImpactDynamics.kick(age,ImpactDynamics.power(f.spell.tier(),f.spell.radius,f.packet.scale())*f.strikePower)*Math.min(1,160/Math.max(1,distance));
        }return Math.min(amount,3.2);
    }
    private static Vec3 nearestImpact(){
        if(mc().player==null)return new Vec3(0,0,0);
        Vec3 best=mc().player.position();double bd=Double.MAX_VALUE;
        for(Fx f:ACTIVE){double d=mc().player.position().distanceToSqr(f.packet.target());if(d<bd){bd=d;best=f.packet.target();}}
        return best;
    }
    public static View view(Camera camera,float partial){
        if(SpellFilm.running())return null;
        Fx f=cinematic();double shake=shake(partial),shove=shove(partial);double t=mc().level==null?0:mc().level.getGameTime()+partial;
        Vec3 base=camera.getPosition();float yaw=camera.getYRot(),pitch=camera.getXRot(),roll=camera.getRoll();
        if(f!=null){
            if(InvocationDirector.supports(f.spell)){
                View v=InvocationDirector.camera(camera,f,partial);base=v.pos();yaw=v.yaw();pitch=v.pitch();roll=v.roll();
            }else{
            // Real camera is anchored to the PLAYER, never to the distant impact point.
            // Astral fly-throughs are rendered by ScreenFX and do not touch chunk-cache centres.
            double age=f.age(partial),progress=age/f.spell.duration,blend=smooth(Math.min(age/18,(f.spell.duration-age)/28));
            Vec3 eye=mc().player.getEyePosition(partial),target=f.packet.target();
            double pre=1-smooth((age-f.spell.impact)/40);
            double focus=switch(f.spell){case STARFALL->28;case HEAVENS_LANCE->48;case TEMPEST_WRATH->50;case SOLAR_NOVA->55;case LAST_CATACLYSM,RUNE_APOCALYPSE->65;case GLACIAL_RAGNAROK->33;case INFERNO_TIDE->48;case VERDANT_RUIN,OBSIDIAN_CATHEDRAL->52;default->38;};
            Vec3 lookAt=target.add(0,focus*f.packet.scale()*pre,0);
            Vec3 forward=target.subtract(eye).multiply(1,0,1).normalize(),side=new Vec3(-forward.z,0,forward.x);
            double shot=age<f.spell.impact*.42?-1:(age<f.spell.impact?1:.25);
            Vec3 desired=eye.add(side.scale(shot*3.5*Math.sin(progress*Math.PI))).add(forward.scale(-4)).add(0,2.5+2*Math.sin(progress*Math.PI),0);
            var hit=mc().level.clip(new ClipContext(eye,desired,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc().player));
            if(hit.getType()!=HitResult.Type.MISS)desired=hit.getLocation().lerp(eye,.1);
            if(!mc().level.hasChunkAt(net.minecraft.core.BlockPos.containing(desired)))desired=eye;
            Vec3 pos=base.lerp(desired,blend),look=lookAt.subtract(pos);
            float desiredYaw=(float)(Math.atan2(look.z,look.x)*180/Math.PI-90),desiredPitch=(float)(-Math.atan2(look.y,look.horizontalDistance())*180/Math.PI);
            yaw+=Mth.wrapDegrees(desiredYaw-yaw)*(float)blend;pitch+=(desiredPitch-pitch)*(float)blend;base=pos;
            roll=(float)(Math.sin(progress*Math.PI*2)*1.2*blend);
            }
        }else if(shake<.001&&shove<.001)return null;
        // 0.7.0 punch pass: a short directional shove from the impact, then a high-frequency tremor.
        if(shove>.001){Vec3 away=base.subtract(nearestImpact());
            if(away.lengthSqr()>.01)base=base.add(away.normalize().scale(shove*.14));}
        base=base.add(Math.sin(t*5.1)*shake*.05,Math.cos(t*6.3)*shake*.04,Math.sin(t*4.4)*shake*.045);
        return new View(base,yaw+(float)(Math.sin(t*4.7)*shake*.28),pitch+(float)(Math.cos(t*5.9)*shake*.24),roll+(float)(Math.sin(t*3.8)*shake*.4));
    }
    public static double smooth(double p){p=Math.max(0,Math.min(1,p));return p*p*(3-2*p);}
    @SubscribeEvent public static void fov(ViewportEvent.ComputeFov e){
        var f=cinematic();if(f!=null&&!SpellFilm.running()){e.setFOV(e.getFOV()*(InvocationDirector.supports(f.spell)?InvocationDirector.fov(f,(float)e.getPartialTick()):((f.spell.cosmic()?.89:1.08)+.035*Math.sin(f.age*.012))));return;}
        // 0.7.3: every strike of the barrage punches the FOV open for a few ticks.
        if(mc().player==null)return;float partial=(float)e.getPartialTick();double punch=0;
        for(Fx fx:ACTIVE){double after=sinceStrike(fx,fx.age(partial));if(after<0||after>=10)continue;
            double d=mc().player.position().distanceTo(fx.packet.target());
            punch+=ImpactDynamics.kick(after,ImpactDynamics.power(fx.spell.tier(),fx.spell.radius,fx.packet.scale())*fx.strikePower)*Math.min(1,160/Math.max(1,d));}
        if(punch>.01)e.setFOV(e.getFOV()*(1+Math.min(punch,.9)*.20));
    }
    @SubscribeEvent public static void fog(ViewportEvent.ComputeFogColor e){
        if(ACTIVE.isEmpty())return;Fx f=ACTIVE.getLast();float mix=.10f*(float)Math.sin(Math.min(1,f.age/(double)f.spell.duration)*Math.PI);
        e.setRed(Mth.lerp(mix,e.getRed(),((f.spell.color>>16)&255)/255f));e.setGreen(Mth.lerp(mix,e.getGreen(),((f.spell.color>>8)&255)/255f));e.setBlue(Mth.lerp(mix,e.getBlue(),(f.spell.color&255)/255f));
    }
    public static boolean hudVisible(){var m=mc();return m.player!=null&&!m.player.isSpectator()&&!m.options.hideGui;}
    public static void hud(GuiGraphics g,DeltaTracker tracker){
        Minecraft m=mc();if(!hudVisible()||SpellFilm.running())return;
        int w=g.guiWidth(),h=g.guiHeight();float partial=tracker.getGameTimeDeltaPartialTick(false);
        if(cinematic()==null&&(m.player.getMainHandItem().getItem() instanceof GrimoireItem||m.player.getOffhandItem().getItem() instanceof GrimoireItem)){
            var stack=m.player.getMainHandItem().getItem() instanceof GrimoireItem?m.player.getMainHandItem():m.player.getOffhandItem();Spell s=GrimoireItem.selected(stack);
            int x=w/2,y=h-73;g.fill(x-117,y-5,x+117,y+29,0xb312101e);g.fill(x-117,y-5,x+117,y-4,0xff000000|s.color);
            g.drawCenteredString(m.font,Component.literal(String.format("%02d / %02d  ",s.tier(),Spell.values().length)).append(s.title()),x,y,0xff000000|s.color);
            g.drawCenteredString(m.font,Component.translatable("hud.arcane_cataclysm.controls"),x,y+13,0xffb1aabe);
            float cd=m.player.getCooldowns().getCooldownPercent(Cataclysm.GRIMOIRE.get(),partial);g.fill(x-117,y+29,x-117+(int)(234*(1-cd)),y+31,0xff000000|s.secondary);
        }
        Fx f=cinematic();
        if(f!=null){
            double age=f.age(partial),fade=smooth(Math.min(age/18,(f.spell.duration-age)/24));int bar=(int)(Math.max(h*.075,InvocationDirector.supports(f.spell)?20:0)*fade);
            g.fill(0,0,w,bar,0xff000000);g.fill(0,h-bar,w,h,0xff000000);
            if(InvocationDirector.supports(f.spell))InvocationDirector.title(g,f,(float)age,bar);
            else if(age<65){int a=(int)(255*smooth(Math.min(age/15,(65-age)/15)));if(a>3){
                g.drawCenteredString(m.font,Component.translatable("hud.arcane_cataclysm.invocation"),w/2,bar+15,(a<<24)|0xc4b5d5);
                g.pose().pushPose();g.pose().translate(w/2f,bar+33,0);g.pose().scale(1.7f,1.7f,1);g.drawCenteredString(m.font,f.spell.title(),0,0,(a<<24)|f.spell.color);g.pose().popPose();
            }}
            g.drawCenteredString(m.font,Component.translatable("hud.arcane_cataclysm.skip",ClientSetup.SKIP.getTranslatedKeyMessage(),ClientSetup.CAMERA.getTranslatedKeyMessage()),w/2,h-Math.max(12,bar/2+4),0xffd7cedf);
        }
        if(latestProgress!=null&&m.level.getGameTime()<progressUntil&&(f==null||!InvocationDirector.supports(f.spell)||f.age>=f.spell.impact+60)){
            var p=latestProgress;String key=p.phase()==0?"hud.arcane_cataclysm.preparing":(p.phase()==1?"hud.arcane_cataclysm.destroying":(p.phase()==3?"hud.arcane_cataclysm.incomplete":"hud.arcane_cataclysm.complete"));
            int y=f==null?18:(InvocationDirector.supports(f.spell)?h-(int)(h*.075)-14:(int)(h*.075)+9);
            g.drawCenteredString(m.font,Component.translatable(key,p.phase()==0?p.prepared():p.completed(),p.total(),String.format(java.util.Locale.ROOT,"%,d",p.blocks())),w/2,y,0xffb9a8d8);
        }

    }
    private ClientEffects(){}
}
