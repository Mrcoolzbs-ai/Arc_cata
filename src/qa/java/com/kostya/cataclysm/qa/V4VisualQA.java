package com.kostya.cataclysm.qa;
import com.kostya.cataclysm.Cataclysm;
import com.kostya.cataclysm.client.EndCinematic;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import java.nio.file.*;
@EventBusSubscriber(modid=Cataclysm.MODID,value=Dist.CLIENT)
public final class V4VisualQA {
 static final int[] TIMES={40,112,178,340,560,686,704,760,840,892,906,936};static int stage,frames;
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void before(ScreenEvent.Render.Pre e){
  if(!Boolean.getBoolean("ac.v4visual")||EndCinematic.shader==null)return;
  try{var a=EndCinematic.class.getDeclaredField("age");a.setAccessible(true);a.setInt(null,TIMES[Math.min(stage,TIMES.length-1)]);var s=EndCinematic.class.getDeclaredField("seed");s.setAccessible(true);s.setLong(null,713);}
  catch(Throwable ex){ex.printStackTrace();System.exit(1);}
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void after(ScreenEvent.Render.Post e){
  if(!Boolean.getBoolean("ac.v4visual")||EndCinematic.shader==null||++frames<4)return;frames=0;
  try{e.getGuiGraphics().flush();var dir=Path.of("qa/v4screens");Files.createDirectories(dir);
   try(var image=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){image.writeToFile(dir.resolve(String.format("%02d-age-%03d.png",stage,TIMES[stage])));}
   System.out.println("AC4_VISUAL_FRAME phase="+stage+" age="+TIMES[stage]);stage++;
   if(stage==TIMES.length){System.out.println("AC4_VISUAL_ALL_PASS (staged GUI only; no world/server claims)");Minecraft.getInstance().stop();}
  }catch(Throwable ex){ex.printStackTrace();System.exit(1);}
 }
}
