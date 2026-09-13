package written_by_Linuxenjoyer1234.linus_meteor_litematica_printer.mixin;

import meteordevelopment.meteorclient.utils.player.TitleScreenCredits;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TitleScreenCredits.class, remap = false)
public abstract class TitleScreenCreditsMixin {
    @Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent onAddLiteral(String string) {
        if (string.equals("Linus Meteor Litematica Printer")) {
            // Use empty() as root to prevent Meteor's withStyle() from overriding our child styles
            return Component.empty()
                .append(Component.literal("Lin").withStyle(ChatFormatting.BLACK))
                .append(Component.literal("us ").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("Meteor ").withStyle(style -> style.withColor(meteordevelopment.meteorclient.MeteorClient.ADDON.color.getPacked())))
                .append(Component.literal("L").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("i").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("t").withStyle(ChatFormatting.RED))
                .append(Component.literal("e").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal("m").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("a").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("t").withStyle(ChatFormatting.RED))
                .append(Component.literal("i").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(Component.literal("c").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("a ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal("Printer").withStyle(ChatFormatting.AQUA));
        }
        
        if (string.equals("Linuxenjoyer1234")) {
            return Component.empty()
                .append(Component.literal("Linuxenj").withStyle(ChatFormatting.BLACK))
                .append(Component.literal("oyer1234").withStyle(ChatFormatting.YELLOW));
        }
        
        return Component.literal(string);
    }
}
