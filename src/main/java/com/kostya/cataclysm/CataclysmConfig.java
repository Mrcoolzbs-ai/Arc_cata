package com.kostya.cataclysm;
import net.neoforged.neoforge.common.ModConfigSpec;
public final class CataclysmConfig {
 public static final ModConfigSpec SPEC;
 public static final ModConfigSpec.BooleanValue END_ENABLED,BREAK_BEDROCK;
 public static final ModConfigSpec.IntValue MAX_PREPARED,END_STARS;
 public static final ModConfigSpec.BooleanValue DESTRUCTION,REQUIRE_OPERATOR,DAMAGE_PLAYERS,GENERATE_CHUNKS,INSTANT_IMPACT,FIRE,BIOMES,ENTITY_DAMAGE;
 public static final ModConfigSpec.DoubleValue RADIUS_SCALE,BUDGET_MS,DAMAGE,PREP_BUDGET;
 public static final ModConfigSpec.IntValue COLUMNS_PER_TICK,MAX_ACTIVE,PINNED_CHUNKS,FIRE_PER_CHUNK;
 static {var b=new ModConfigSpec.Builder();b.comment("BACK UP WORLD. 0.3 adds structures, biome edits, real fire and lethal area damage.").push("server");
 DESTRUCTION=b.define("destruction",true);REQUIRE_OPERATOR=b.define("requireOperator",true);
 DAMAGE_PLAYERS=b.comment("New in 0.3: damage all non-caster survival/adventure players in the impact cylinder. Creative/spectators remain immune.").define("friendlyFirePlayers",true);
 GENERATE_CHUNKS=b.define("generateMissingChunks",true);
 INSTANT_IMPACT=b.comment("Apply ALL ready AND currently loaded chunks synchronously on impact. Can cause a long freeze. false uses terrainBudgetMs instead.").define("instantPreparedImpact",true);
 ENTITY_DAMAGE=b.define("damageEntities",true);FIRE=b.define("igniteSurroundings",true);BIOMES=b.define("snowGraveBiome",true);FIRE_PER_CHUNK=b.defineInRange("maxIgnitionsPerChunk",4,0,16);
 RADIUS_SCALE=b.defineInRange("radiusScale",1.,.1,1.5);BUDGET_MS=b.defineInRange("terrainBudgetMs",6.,.5,20.);PREP_BUDGET=b.defineInRange("preparationBudgetMs",4.,.5,15.);
 PINNED_CHUNKS=b.comment("Only nearest prepared chunks remain pinned; geometry for the rest is kept without retaining world chunks.").defineInRange("pinnedPreparedChunks",128,16,512);
 COLUMNS_PER_TICK=b.defineInRange("maxColumnsPerTick",512,8,4096);MAX_ACTIVE=b.defineInRange("maxActiveSpells",2,1,8);
 DAMAGE=b.comment("No distance falloff. Existing values below 1000 are corrected by the config spec.").defineInRange("impactDamage",1200.,1000.,10000.);
 BREAK_BEDROCK=b.comment("Tier 10+ may remove BEDROCK only; other immune/admin blocks remain protected.").define("highTierBreaksBedrock",true);
 MAX_PREPARED=b.comment("Backpressure limit on prepared chunk patches, not world chunk tickets.").defineInRange("maxPreparedChunkPatches",2048,32,8192);
 END_ENABLED=b.comment("DANGER: enabled by default. Use /cataclysm endofall false to disable new casts. Server-wide event. Kills game characters in all dimensions/modes, locks respawn for 900 ticks and erases the target footprint including bedrock. BACKUP FIRST. Operators only, including API calls.").define("enableEndOfAll",true);
 END_STARS=b.comment("Nether stars consumed for a non-creative End of All cast.").defineInRange("endOfAllNetherStarCost",64,1,4096);
 b.pop();SPEC=b.build();}
 private CataclysmConfig(){}
}
