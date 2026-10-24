package com.kostya.cataclysm;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.*;
import com.kostya.cataclysm.network.SpellNetwork;
import com.kostya.cataclysm.world.SpellManager;

@Mod(Cataclysm.MODID)
public final class Cataclysm {
    public static final String MODID="arcane_cataclysm";
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(MODID);
    public static final DeferredRegister.DataComponents COMPONENTS=DeferredRegister.createDataComponents(MODID);
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> SELECTED=COMPONENTS.registerComponentType("selected_spell",b->b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT));
    public static final DeferredItem<GrimoireItem> GRIMOIRE=ITEMS.register("grimoire_of_cataclysm",()->new GrimoireItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,MODID);
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> TAB=TABS.register("cataclysm",()->CreativeModeTab.builder().title(Component.translatable("itemGroup.arcane_cataclysm")).icon(()->GRIMOIRE.get().getDefaultInstance()).displayItems((p,out)->out.accept(GRIMOIRE.get())).build());
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(Registries.SOUND_EVENT,MODID);
    public static final DeferredHolder<SoundEvent,SoundEvent> CHARGE=SOUNDS.register("charge",()->SoundEvent.createVariableRangeEvent(id("charge")));
    public static final DeferredHolder<SoundEvent,SoundEvent> IMPACT=SOUNDS.register("impact",()->SoundEvent.createVariableRangeEvent(id("impact")));
    public static final DeferredHolder<SoundEvent,SoundEvent> RUMBLE=SOUNDS.register("rumble",()->SoundEvent.createVariableRangeEvent(id("rumble")));
    static {for(Spell spell:Spell.values())for(String phase:(spell.ordinal()>=9&&spell.ordinal()<15?new String[]{"charge","impact","score"}:new String[]{"charge","impact"})){String name=spell.id+"_"+phase;SOUNDS.register(name,()->SoundEvent.createVariableRangeEvent(id(name)));}}
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(MODID,path);}
    public Cataclysm(IEventBus bus,ModContainer container){
        ITEMS.register(bus);COMPONENTS.register(bus);TABS.register(bus);SOUNDS.register(bus);
        bus.addListener(SpellNetwork::register);
        container.registerConfig(ModConfig.Type.SERVER,CataclysmConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT,ClientOptions.SPEC);
        NeoForge.EVENT_BUS.addListener(SpellManager::tick);
        NeoForge.EVENT_BUS.addListener(com.kostya.cataclysm.world.EndEvent::protectDamage);
        NeoForge.EVENT_BUS.addListener(com.kostya.cataclysm.world.EndEvent::protectDeath);
        NeoForge.EVENT_BUS.addListener(SpellManager::commands);
        NeoForge.EVENT_BUS.addListener(SpellManager::stop);
    }
}
