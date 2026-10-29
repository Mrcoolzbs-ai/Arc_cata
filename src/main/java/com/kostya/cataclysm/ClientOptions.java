package com.kostya.cataclysm;
import net.neoforged.neoforge.common.ModConfigSpec;
/** No Minecraft client classes here: safe to register on a dedicated server. */
public final class ClientOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CAMERA,SHAKE,FLASH,POST,ASTRAL,GLITCH;
    public static final ModConfigSpec.DoubleValue BLOOM;
    public static final ModConfigSpec.IntValue DENSITY;
    static {var b=new ModConfigSpec.Builder();CAMERA=b.define("cinematicCamera",true);SHAKE=b.define("screenShake",true);FLASH=b.define("screenFlash",true);DENSITY=b.defineInRange("effectDensityPercent",100,10,100);GLITCH=b.comment("Extreme chromatic impact split and radial aberration on impact, scaling with spell tier. Also disabled by screenFlash=false.").define("impactGlitch",true);POST=b.define("postProcessing",true);ASTRAL=b.define("astralScenes",true);BLOOM=b.defineInRange("bloomStrength",.55,0.,1.5);SPEC=b.build();}
    private ClientOptions(){}
}
