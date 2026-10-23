package com.kostya.cataclysm;
import net.minecraft.network.chat.Component;
/** Catalogue order is the nominal terrain devastation tier, not a world-independent block-count guarantee. */
public enum Spell {
 HEAVENS_LANCE("heavens_lance",0xffe9a3,0x55cdff,580,360,620,48),
 THOUSAND_BLADES("thousand_blades",0x92ffe8,0x5178ff,620,400,660,80),
 GLACIAL_RAGNAROK("snow_grave",0xb7f7ff,0x2977ff,660,420,700,112),
 TEMPEST_WRATH("tempest_wrath",0xb9b2ff,0x4c67ff,680,440,720,144),
 STARFALL("starfall",0xffab53,0xff3c31,660,420,700,168),
 TECTONIC_RIFT("tectonic_rift",0xffa23a,0xee321f,700,460,740,200),
 INFERNO_TIDE("inferno_tide",0xffb736,0xff2424,720,480,760,224),
 SINGULARITY("singularity",0xb177ff,0x30d9ff,760,500,800,248),
 SOLAR_NOVA("solar_nova",0xffe88a,0xff512c,780,520,820,280),
 LAST_CATACLYSM("last_cataclysm",0xe9c2ff,0xff456f,860,580,900,320),
 VERDANT_RUIN("verdant_ruin",0x8eea75,0x276741,900,620,940,352),
 OBSIDIAN_CATHEDRAL("obsidian_cathedral",0xcc9bff,0x6c36b7,940,660,980,384),
 ABYSSAL_DELUGE("abyssal_deluge",0x6be7ec,0x1858aa,980,700,1020,416),
 WORLD_SUNDER("world_sunder",0xffcc89,0xd33d21,1020,740,1060,448),
 RUNE_APOCALYPSE("rune_apocalypse",0xffe5ec,0xd83b73,1100,800,1140,512),
 END_OF_ALL("end_of_all",0xec2448,0xe8fff5,900,900,72000,1024);
 public final String id;public final int color,secondary,duration,impact,cooldown;public final double radius;
 Spell(String id,int color,int secondary,int duration,int impact,int cooldown,double radius){this.id=id;this.color=color;this.secondary=secondary;this.duration=duration;this.impact=impact;this.cooldown=cooldown;this.radius=radius;}
 public int tier(){return ordinal()+1;}
 public int visualId(){return switch(this){case STARFALL->0;case HEAVENS_LANCE->1;case SINGULARITY->2;case GLACIAL_RAGNAROK->3;case TEMPEST_WRATH->4;case TECTONIC_RIFT->5;case SOLAR_NOVA->6;case THOUSAND_BLADES->7;case INFERNO_TIDE->8;case LAST_CATACLYSM->9;default->ordinal();};}
 public boolean cosmic(){return this==STARFALL||this==SINGULARITY||this==SOLAR_NOVA;}
 public boolean fiery(){return switch(this){case STARFALL,TECTONIC_RIFT,INFERNO_TIDE,SOLAR_NOVA,LAST_CATACLYSM,OBSIDIAN_CATHEDRAL,WORLD_SUNDER,RUNE_APOCALYPSE->true;default->false;};}
 public Component title(){return Component.translatable("spell.arcane_cataclysm."+id);}
 public static Spell byIndex(int i){return values()[Math.floorMod(i,values().length)];}
 public static Spell byId(String id){if(id.equals("glacial_ragnarok"))return GLACIAL_RAGNAROK;for(var s:values())if(s.id.equals(id))return s;throw new IllegalArgumentException("Unknown spell: "+id);}
}
