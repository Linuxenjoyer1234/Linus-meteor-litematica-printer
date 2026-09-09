package written_by_Linuxenjoyer1234.linus_meteor_litematica_printer.mixin;

// Here we go fucking again, import 100000000000 shits into my ass
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.text.Font;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;

// Oh my gosh fucking meteor devs client couldn't you just add this
// printer natively you lazy fucking fat donkey so I have to do it for you

@Mixin(value = Font.class, remap = false)
public abstract class FontMixin {
    @Shadow @Final private float ascent;
    @Shadow @Final private float scale;
    @Shadow @Final private Int2ObjectOpenHashMap<Object> charMap;

    private static Method x0M, y0M, x1M, y1M, u0M, v0M, u1M, v1M, xAdvanceM;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(MeshBuilder mesh, String string, double x, double y, Color color, double scale, CallbackInfoReturnable<Double> cir) {
        if (string == null) return;

        // Specific strings to target (exact matches from fabric.mod.json)
        String printerName = "§0Lin§eus §5Meteor §eL§bi§ct§de§em§ba§ct§di§ec§ba §bPrinter";
        String authorName = "§0Linuxenj§eoyer1234";

        if (string.equals(printerName) || string.equals(authorName)) {
            try {
                ensureReflection();
                double curX = x;
                double curY = y + (double)(this.ascent * this.scale) * scale;
                Color curCol = new Color(color);
                
                int length = string.length();
                mesh.ensureCapacity(length * 4, length * 6);

                for (int i = 0; i < length; i++) {
                    char c = string.charAt(i);
                    if (c == '§' && i + 1 < length) {
                        Color newCol = getColor(string.charAt(i + 1));
                        if (newCol != null) {
                            curCol.r = newCol.r; curCol.g = newCol.g; curCol.b = newCol.b;
                            i++; continue;
                        }
                    }

                    Object cd = charMap.get(c);
                    if (cd == null) cd = charMap.get(' ');
                    if (cd == null) continue;

                    float x0 = (float) x0M.invoke(cd), y0 = (float) y0M.invoke(cd);
                    float x1 = (float) x1M.invoke(cd), y1 = (float) y1M.invoke(cd);
                    float u0 = (float) u0M.invoke(cd), v0 = (float) v0M.invoke(cd);
                    float u1 = (float) u1M.invoke(cd), v1 = (float) v1M.invoke(cd);
                    float xAdv = (float) xAdvanceM.invoke(cd);
// ...
                    mesh.quad(
                        mesh.vec2(curX + (double)x0 * scale, curY + (double)y0 * scale).vec2((double)u0, (double)v0).color(curCol).next(),
                        mesh.vec2(curX + (double)x0 * scale, curY + (double)y1 * scale).vec2((double)u0, (double)v1).color(curCol).next(),
                        mesh.vec2(curX + (double)x1 * scale, curY + (double)y1 * scale).vec2((double)u1, (double)v1).color(curCol).next(),
                        mesh.vec2(curX + (double)x1 * scale, curY + (double)y0 * scale).vec2((double)u1, (double)v0).color(curCol).next()
                    );
                    curX += (double)xAdv * scale;
                }
                cir.setReturnValue(curX);
            } catch (Exception ignored) {
                // If the meteor client is so fucking idiotic and dumb that it decides to put a middlefinger against my coloring then let it apply it's stupid ass bugged shit
            }
        }
    }

    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    private void onGetWidth(String string, int length, CallbackInfoReturnable<Double> cir) {
        if (string == null) return;
        String printerName = "§0Lin§eus §5Meteor §eL§bi§ct§de§em§ba§ct§di§ec§ba §bPrinter";
        String authorName = "§0Linuxenj§eoyer1234";

        if (string.equals(printerName) || string.equals(authorName)) {
            try {
                ensureReflection();
                double width = 0;
                for (int i = 0; i < length; i++) {
                    char c = string.charAt(i);
                    if (c == '§' && i + 1 < length) {
                        if (getColor(string.charAt(i + 1)) != null) { i++; continue; }
                    }
                    Object cd = charMap.get(c);
                    if (cd == null) cd = charMap.get(' ');
                    if (cd != null) width += (float) xAdvanceM.invoke(cd);
                }
                cir.setReturnValue(width);
            } catch (Exception ignored) {}
        }
    }

    private void ensureReflection() throws Exception {
        if (x0M == null) {
            Class<?> cd = Class.forName("meteordevelopment.meteorclient.renderer.text.Font$CharData");
            x0M = cd.getDeclaredMethod("x0"); x0M.setAccessible(true);
            y0M = cd.getDeclaredMethod("y0"); y0M.setAccessible(true);
            x1M = cd.getDeclaredMethod("x1"); x1M.setAccessible(true);
            y1M = cd.getDeclaredMethod("y1"); y1M.setAccessible(true);
            u0M = cd.getDeclaredMethod("u0"); u0M.setAccessible(true);
            v0M = cd.getDeclaredMethod("v0"); v0M.setAccessible(true);
            u1M = cd.getDeclaredMethod("u1"); u1M.setAccessible(true);
            v1M = cd.getDeclaredMethod("v1"); v1M.setAccessible(true);
            xAdvanceM = cd.getDeclaredMethod("xAdvance"); xAdvanceM.setAccessible(true);
        }
    }

    private Color getColor(char code) {
        switch (code) {
            case '0': return new Color(0, 0, 0);
            case '1': return new Color(0, 0, 170);
            case '2': return new Color(0, 170, 0);
            case '3': return new Color(0, 170, 170);
            case '4': return new Color(170, 0, 0);
            case '5': return new Color(170, 0, 170);
            case '6': return new Color(255, 170, 0);
            case '7': return new Color(170, 170, 170);
            case '8': return new Color(85, 85, 85);
            case '9': return new Color(85, 85, 255);
            case 'a': return new Color(85, 255, 85);
            case 'b': return new Color(85, 255, 255);
            case 'c': return new Color(255, 85, 85);
            case 'd': return new Color(255, 85, 255);
            case 'e': return new Color(255, 255, 85);
            case 'f': return new Color(255, 255, 255);
            default: return null;
        }
    }
}
