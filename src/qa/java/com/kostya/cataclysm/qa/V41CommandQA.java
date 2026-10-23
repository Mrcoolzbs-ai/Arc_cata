package com.kostya.cataclysm.qa;

import com.kostya.cataclysm.*;
import com.kostya.cataclysm.world.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.*;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.network.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;
import java.util.UUID;

/** Disposable dedicated-server command smoke test; excluded from release JAR. */
@EventBusSubscriber(modid=Cataclysm.MODID)
public final class V41CommandQA {
 static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static int execute(CommandSourceStack source,String text)throws Exception{return source.getServer().getCommands().getDispatcher().execute(text,source);}
 static void persisted(Path file,boolean expected)throws Exception{
  require(Files.readString(file).matches("(?s).*enableEndOfAll\\s*=\\s*"+expected+"\\b.*"),"setting not persisted: "+expected);
 }
 static final class NoopListener extends ServerGamePacketListenerImpl {
  NoopListener(net.minecraft.server.MinecraftServer server,ServerPlayer p,GameProfile profile){super(server,new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false));}
  @Override public boolean hasChannel(net.minecraft.resources.ResourceLocation id){return false;}
  @Override public boolean hasChannel(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<?> id){return false;}
  @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
  @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
 }
 @SubscribeEvent public static void ready(ServerStartedEvent event){
  if(!Boolean.getBoolean("ac.v41qa"))return;
  var server=event.getServer();var source=server.createCommandSourceStack();
  var override=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/arcane_cataclysm-server.toml");
  var file=Files.exists(override)?override:net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("arcane_cataclysm-server.toml");
  try{
   if(Boolean.getBoolean("ac.v41restart")){
    require(!CataclysmConfig.END_ENABLED.get(),"existing false overwritten by new default");persisted(file,false);
    execute(source,"cataclysm endofall true");require(CataclysmConfig.END_ENABLED.get(),"restart enable failed");persisted(file,true);
    System.out.println("AC41_RESTART_PERSISTENCE_PASS");server.halt(false);return;
   }
   require(CataclysmConfig.END_ENABLED.get(),"new-world default must be true");persisted(file,true);
   require(execute(source,"cataclysm endofall")==1,"status command failed");
   for(int permission:new int[]{0,1}){
    boolean denied=false;try{execute(source.withPermission(permission),"cataclysm endofall false");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
    require(denied&&CataclysmConfig.END_ENABLED.get(),"non-op changed setting");
   }
   execute(source.withPermission(2),"cataclysm endofall false");require(!CataclysmConfig.END_ENABLED.get(),"disable failed");persisted(file,false);
   execute(source,"cataclysm endofall");require(!CataclysmConfig.END_ENABLED.get(),"status changed setting");
   boolean rejected=false;try{execute(source,"cataclysm endofall invalid");}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){rejected=true;}
   require(rejected&&!CataclysmConfig.END_ENABLED.get(),"invalid argument changed setting");
   var profile=new GameProfile(new UUID(941,1),"AC41Operator");
   var player=new ServerPlayer(server,server.overworld(),profile,ClientInformation.createDefault());
   player.connection=new NoopListener(server,player,profile);player.setGameMode(GameType.CREATIVE);
   server.getPlayerList().getOps().add(new ServerOpListEntry(profile,2,false));
   require(!EndEvent.start(player,new Vec3(0,60,0),new Vec3(1,0,0)),"disabled cast accepted");
   execute(source,"cataclysm endofall toggle");require(CataclysmConfig.END_ENABLED.get(),"toggle-on failed");persisted(file,true);
   CataclysmConfig.DESTRUCTION.set(false);
   require(EndEvent.start(player,new Vec3(0,60,0),new Vec3(1,0,0)),"enabled operator cast rejected");
   execute(source,"cataclysm endofall false");require(EndEvent.active()&&EndEvent.respawnLocked(player),"disable interrupted active cast");
   execute(source,"cataclysm cancel");require(EndEvent.active(),"cancel interrupted active cast");
   EndEvent.reset();
   execute(source,"cataclysm endofall true");persisted(file,true);
   execute(source,"cataclysm endofall toggle");require(!CataclysmConfig.END_ENABLED.get(),"toggle-off failed");persisted(file,false);
   CataclysmConfig.DESTRUCTION.set(true);CataclysmConfig.SPEC.save();
   System.out.println("AC41_COMMANDS_ALL_PASS default=true permissions=0,1,2 console=true query=true invalid=true bool=true toggle=true disabledCast=true activeCastPreserved=true saved=false");
   server.halt(false);
  }catch(Throwable failure){failure.printStackTrace();System.out.println("AC41_COMMANDS_FAIL");Runtime.getRuntime().halt(1);}
 }
}
