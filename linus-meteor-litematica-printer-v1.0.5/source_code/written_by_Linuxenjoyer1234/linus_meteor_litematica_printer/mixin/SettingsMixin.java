package written_by_Linuxenjoyer1234.linus_meteor_litematica_printer.mixin;

// import isreal
import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.widgets.WMeteorMultiLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = DefaultSettingsWidgetFactory.class, remap = false)
public abstract class SettingsMixin {
    @Inject(method = "intW", at = @At("TAIL"))
    private void onIntW(WTable table, IntSetting setting, CallbackInfo ci) {
        if ("blocks/tick".equals(setting.name) && "How many blocks place per tick.".equals(setting.description)) {
            if (setting.module != null) {
                Setting<Boolean> ss = (Setting<Boolean>) setting.module.settings.get("Superspeed");
                if (ss != null) {
                    table.row();
                    table.add(table.theme.label(""));
                    
                    // Add the optimized warning label
                    WWarningLabel label = new WWarningLabel(setting, ss);
                    table.add(label).expandX().center();
                }
            }
        }
    }

    private static class WWarningLabel extends WMeteorMultiLabel {
        private final IntSetting setting;
        private final Setting<Boolean> superSpeed;

        public WWarningLabel(IntSetting setting, Setting<Boolean> superSpeed) {
            super("", false, 150.0);
            this.setting = setting;
            this.superSpeed = superSpeed;
            this.color = new Color(255, 0, 0);
        }

        private boolean shouldShow() {
            // Tie visibility to the slider's visibility to handle the "hidden setting" case
            return setting.isVisible() && superSpeed.get() && setting.get() > 3;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            if (this.width > 0 && this.height > 0) {
                super.onRender(renderer, mouseX, mouseY, delta);
            }
        }

        @Override
        protected void onCalculateSize() {
            // Update text before size calculation to ensure wrapping logic triggers correctly
            this.text = shouldShow() ? "Warning, this may cause a lot of rubber banding in servers!" : "";
            super.onCalculateSize();
            
            // Explicitly force zero height if not showing to ensure the row collapses
            if (this.text.isEmpty()) {
                this.width = 0;
                this.height = 0;
            }
        }
    }
}
