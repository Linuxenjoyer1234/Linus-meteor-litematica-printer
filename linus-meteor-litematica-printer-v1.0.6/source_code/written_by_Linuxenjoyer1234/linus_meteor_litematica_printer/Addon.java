package written_by_Linuxenjoyer1234.linus_meteor_litematica_printer;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Addon extends MeteorAddon {
   public static final Logger LOG = LogManager.getLogger();
   public static final Category CATEGORY = new Category("Litematica", () -> new ItemStack(Items.PINK_CARPET));

   public void onInitialize() {
      LOG.info("Initializing linus meteor litematica printer");
      Modules.get().add(new Printer());
      // Modules.get().add(new Shredder());
      // Shredder will be added in v1.2.0
   }

   public String getPackage() {
      return "written_by_Linuxenjoyer1234.linus_meteor_litematica_printer";
   }

   public void onRegisterCategories() {
      Modules.registerCategory(CATEGORY);
   }
}
