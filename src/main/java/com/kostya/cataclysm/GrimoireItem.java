package com.kostya.cataclysm;
import com.kostya.cataclysm.world.SpellManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import java.util.List;
public final class GrimoireItem extends Item {
    public GrimoireItem(Properties p){super(p);}
    public static Spell selected(ItemStack s){return Spell.byIndex(s.getOrDefault(Cataclysm.SELECTED.get(),0));}
    public static boolean select(Player p,int step){
        ItemStack s=p.getMainHandItem();if(!(s.getItem() instanceof GrimoireItem))s=p.getOffhandItem();
        if(!(s.getItem() instanceof GrimoireItem))return false;
        s.set(Cataclysm.SELECTED.get(),Math.floorMod(selected(s).ordinal()+Integer.signum(step),Spell.values().length));
        p.displayClientMessage(selected(s).title().copy().withStyle(ChatFormatting.LIGHT_PURPLE),true);return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack stack=player.getItemInHand(hand);
        if(!level.isClientSide && player instanceof ServerPlayer sp){
            if(player.isShiftKeyDown())select(player,1);else SpellManager.cast(sp,selected(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public boolean isFoil(ItemStack s){return true;}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag f){
        lines.add(selected(s).title().copy().withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.arcane_cataclysm.controls").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.arcane_cataclysm.warning").withStyle(ChatFormatting.RED));
    }
}
