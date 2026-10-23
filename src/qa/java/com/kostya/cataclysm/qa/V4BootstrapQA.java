package com.kostya.cataclysm.qa;
import com.kostya.cataclysm.Cataclysm;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
@EventBusSubscriber(modid=Cataclysm.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class V4BootstrapQA {
 @SubscribeEvent public static void ready(FMLLoadCompleteEvent e){if(Boolean.getBoolean("ac.v4pure"))e.enqueueWork(()->{
  try{V4PureQA.main(new String[0]);System.out.println("AC4_BOOTSTRAP_EXIT_NO_WORLD");System.exit(0);}catch(Throwable ex){ex.printStackTrace();System.exit(1);}
 });}
}
