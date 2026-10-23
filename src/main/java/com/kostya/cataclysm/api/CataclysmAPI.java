package com.kostya.cataclysm.api;
import com.kostya.cataclysm.Spell;
import com.kostya.cataclysm.world.SpellManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
/** Integration entry point. Call only on the logical server thread after your own permission/mana checks. */
public final class CataclysmAPI {
    public static boolean cast(ServerPlayer caster,Spell spell){return SpellManager.cast(caster,spell);}
    /** Trusted integration hook, bypasses grimoire cooldown/permission checks; still enforces range/concurrency/config terrain toggle. */
    public static boolean castAt(ServerPlayer caster,Spell spell,Vec3 target){return SpellManager.castAt(caster,spell,target,caster.getLookAngle());}
    private CataclysmAPI(){}
}
