package com.kostya.cataclysm.qa;
import com.kostya.cataclysm.*;
import com.kostya.cataclysm.client.ClientEffects;
import com.kostya.cataclysm.world.*;
import net.minecraft.client.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.io.File;

/** Development-only integration/visual smoke test, enabled explicitly with -Pqa. */
@EventBusSubscriber(modid=Cataclysm.MODID,value=Dist.CLIENT)
public final class InGameQA {
    private static int ticks,scene=-1;private static boolean ready;
    private static void require(boolean test,String message){if(!test)throw new AssertionError(message);}
    private static void geometry(){
        long checks=0;
        for(Spell s:Spell.values())for(double scale:new double[]{.1,1,1.5})for(int terrain:new int[]{-65,-60,0,60,319}){
            var p=new DestructionPlan(s,new Vec3(-.5,60,-.5),new Vec3(1,0,.2).normalize(),123,scale);
            int r=(int)Math.ceil(p.radius);
            for(int x=-r-3;x<r+3;x+=3)for(int z=-r-3;z<r+3;z+=3){
                var c=p.column(x,z,terrain,-64,320);if(c==null)continue;
                require(c.bottom()>=-64,"bottom out of bounds "+s);require(c.top()<320,"top out of bounds "+s);require(c.bottom()<=c.top(),"inverted column "+s);checks++;
                require(c.equals(p.column(x,z,terrain,-64,320)),"nondeterministic column "+s);
            }
            require(p.column(r+50,r+50,60,-64,320)==null,"bounds leak "+s);
        }
        System.out.println("AC_QA_GEOMETRY_PASS columns="+checks);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        Minecraft m=Minecraft.getInstance();if(!Boolean.getBoolean("ac.qa")||m.level==null||m.player==null||m.getSingleplayerServer()==null)return;
        if(m.screen!=null)m.setScreen(null);m.options.pauseOnLostFocus=false;
        ticks++;
        if(!ready&&ticks>100){ready=true;new File("qa/screenshots").mkdirs();geometry();m.options.renderDistance().set(10);m.options.simulationDistance().set(5);m.options.guiScale().set(2);m.options.fov().set(70);m.options.bobView().set(false);
            m.getSingleplayerServer().execute(()->{
                var server=m.getSingleplayerServer();var p=server.getPlayerList().getPlayer(m.player.getUUID());p.setGameMode(GameType.CREATIVE);p.teleportTo(0,85,-115);p.getAbilities().flying=true;p.onUpdateAbilities();
                p.getInventory().setItem(0,Cataclysm.GRIMOIRE.get().getDefaultInstance());p.getInventory().selected=0;
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 9000");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamerule doDaylightCycle false");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamerule doMobSpawning false");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamerule spawnChunkRadius 0");
                var stack=p.getMainHandItem();
                for(int i=0;i<Spell.values().length;i++)GrimoireItem.select(p,1);
                require(GrimoireItem.selected(stack)==Spell.values()[0],"selection did not wrap");
                GrimoireItem.select(p,-1);require(GrimoireItem.selected(stack)==Spell.values()[Spell.values().length-1],"backward selection failed");
                GrimoireItem.select(p,1);
                var fx=new com.kostya.cataclysm.network.SpellNetwork.Fx(9,p.getId(),p.position(),new Vec3(-5,63,17),new Vec3(0,0,1),12345,.65f);
                var buf=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),server.registryAccess());
                com.kostya.cataclysm.network.SpellNetwork.Fx.CODEC.encode(buf,fx);
                require(fx.equals(com.kostya.cataclysm.network.SpellNetwork.Fx.CODEC.decode(buf)),"FX packet did not roundtrip");buf.release();
                System.out.println("AC_QA_SELECTION_AND_CODEC_PASS");
                CataclysmConfig.RADIUS_SCALE.set(.65);CataclysmConfig.GENERATE_CHUNKS.set(false);CataclysmConfig.COLUMNS_PER_TICK.set(128);CataclysmConfig.BUDGET_MS.set(5.0);
            });
            ticks=0;
        }
        if(!ready)return;
        if(scene<0&&ticks>100){scene=0;start(m,scene);ticks=0;}
        if(scene>=0&&scene<Spell.values().length){
            Spell s=Spell.values()[scene];
            if(Boolean.getBoolean("ac.visualqa")){
                if(!ClientEffects.ACTIVE.isEmpty()){
                    if(ticks==5)ClientEffects.ACTIVE.getLast().age=s.impact/2;
                    if(ticks==20)ClientEffects.ACTIVE.getLast().age=s.impact-18;
                    if(ticks==42)ClientEffects.ACTIVE.getLast().age=s.impact+24;
                }
                if(ticks==12||ticks==30||ticks==50){String file=String.format("%02d-%s-%s.png",scene,s.id,ticks==12?"ritual":(ticks==30?"charge":"impact"));m.execute(()->Screenshot.grab(new File("qa"),file,m.getMainRenderTarget(),x->System.out.println("AC_QA_VISUAL_SCREEN "+file)));}
                if(ticks>65){System.out.println("AC_QA_VISUAL_SCENE_PASS "+s.id);scene++;ticks=0;if(scene<Spell.values().length)start(m,scene);else{System.out.println("AC_QA_VISUAL_ALL_PASS");m.stop();}}
                return;
            }
            if(ticks==s.impact-8||ticks==s.impact+35){
                String file=String.format("%02d-%s-%s.png",scene,s.id,ticks<s.impact?"charge":"impact");
                m.execute(()->Screenshot.grab(new File("qa"),file,m.getMainRenderTarget(),x->System.out.println("AC_QA_SCREEN "+file)));
            }
            if(ticks>s.duration+120){System.out.println("AC_QA_SCENE_PASS "+s.id);scene++;ticks=0;if(scene<Spell.values().length)start(m,scene);else{
                m.getSingleplayerServer().execute(()->{System.out.println("AC_QA_TERRAIN "+DestructionEngine.status());});
                System.out.println("AC_QA_ALL_SCENES_PASS");m.stop();
            }}
        }
    }
    private static void start(Minecraft m,int scene){
        for(var f:ClientEffects.ACTIVE)if(f.chargeSound!=null)m.getSoundManager().stop(f.chargeSound);ClientEffects.ACTIVE.clear();
        if(Boolean.getBoolean("ac.visualqa")){
            ClientEffects.receive(new com.kostya.cataclysm.network.SpellNetwork.Fx(scene,m.player.getId(),new Vec3(0,85,-115),new Vec3(0,60,0),new Vec3(0,0,1),12345+scene,1f));return;
        }
        m.getSingleplayerServer().execute(()->{
            var p=m.getSingleplayerServer().getPlayerList().getPlayer(m.player.getUUID());
            var pos=new Vec3(0,60,0);p.teleportTo(0,85,-115);p.getAbilities().flying=true;p.onUpdateAbilities();
            p.getInventory().getItem(0).set(Cataclysm.SELECTED.get(),scene);
            boolean cast=SpellManager.castAt(p,Spell.values()[scene],pos,new Vec3(0,0,1));require(cast,"cast rejected "+scene);
            System.out.println("AC_QA_SCENE_START "+Spell.values()[scene].id);
        });
    }
}
