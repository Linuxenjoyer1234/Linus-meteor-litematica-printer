package written_by_Linuxenjoyer1234.linus_meteor_litematica_printer;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.meteor.MouseScrollEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractItemEvent;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.GameType;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Printer extends Module {
   private final SettingGroup sgGeneral;
   private final SettingGroup sgWorkMode;
   private final SettingGroup sgNiche;
   private final SettingGroup sgRendering;
   private final Setting<Double> printing_range;
   private final Setting<Double> printing_delay;
   private final Setting<Boolean> placeThroughWall;
   private final Setting<Boolean> returnHand;
   private final Setting<Boolean> clientSide;
   private final Setting<Boolean> dirtgrass;
   private final Setting<SortAlgorithm> firstAlgorithm;
   private final Setting<SortingSecond> secondAlgorithm;
   private final Setting<FilterMode> listMode;
   private final Setting<List<Block>> filterBlocks;
   private final Setting<Boolean> superSpeed;
   private final Setting<Integer> bpt;
   private final Setting<Boolean> renderBlocks;
   private final Setting<Double> fadeTime;
   private final Setting<Double> fadeAmount;
   private final Setting<SettingColor> colour;
   private final Setting<Boolean> useOffhand;
   private final Setting<Boolean> respectFalling;
   private final Setting<Boolean> specialFix;
   private final Setting<Boolean> noInteract;
   private final Setting<Integer> blockTimeAmount;
   private final Setting<Boolean> stopOnInterrupt;
   private final Setting<Integer> interruptTicks;
   private final Setting<Boolean> antiWrongDoor;
   private final Setting<Boolean> dontRemindDoor;
   private final Setting<Boolean> acknowledgeDoor;
   private final Setting<Boolean> tellChestError;
   private final Setting<Boolean> dontRemindChest;
   private final Setting<Boolean> acknowledgeChest;
   private final Setting<Boolean> reserveSlot;
   private final Setting<Integer> reservedSlotValue;
   private final Setting<Boolean> antiBoxIn;
   private final Setting<Integer> antiBoxInX;
   private final Setting<Integer> antiBoxInY;
   private final Setting<Integer> antiBoxInZ;
   private final Setting<AntiBoxInOrigin> antiBoxInOrigin;
   private final Setting<Boolean> viewPosition;
   private final Setting<SettingColor> antiBoxInColor;
   private double timer;
   private int usedSlot;
   private int lastSlot = -1;
   private int printerSelectedSlot = -1;
   private int interruptTimer;
   private long lastPlacementTime;
   private long lastDoorErrorMessageTime;
   private long lastChestErrorMessageTime;
   private final List<BlockPos> toSort;
   private final List<Tuple<Integer, BlockPos>> placed_fade;
   private final Map<BlockPos, BlockState> syncTasks;

   public Printer() {
      super(Addon.CATEGORY, "Printer", "Automatically prints open schematics.");
      this.sgGeneral = this.settings.getDefaultGroup();
      this.sgWorkMode = this.settings.createGroup("Work Mode");
      this.sgNiche = this.settings.createGroup("Niche");
      this.sgRendering = this.settings.createGroup("Rendering");
      this.printing_range = this.sgGeneral.add(new DoubleSetting.Builder().name("printing-range").description("The block place range.").defaultValue(5.0).min(1.0).sliderMin(1.0).max(5.0).sliderMax(5.0).decimalPlaces(2).build());
      this.printing_delay = this.sgGeneral.add(new DoubleSetting.Builder().name("printing-delay").description("Delay between printing blocks in ticks.").defaultValue(0.5).min(0.0).sliderMin(0.0).max(100.0).sliderMax(40.0).decimalPlaces(3).build());
      this.placeThroughWall = this.sgGeneral.add(new BoolSetting.Builder().name("Place Through Blocks").description("Allow the printer to place through blocks.").defaultValue(true).build());
      this.returnHand = new BoolSetting.Builder().name("return-slot").description("Return to old slot.").defaultValue(false).build();
      this.clientSide = this.sgGeneral.add(new BoolSetting.Builder().name("Rotation visibility").description("Makes your character look like it's on meth when printing. (Go into 3rd person mode using f5)").defaultValue(false).build());
      this.dirtgrass = this.sgGeneral.add(((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)(new BoolSetting.Builder()).name("dirt-as-grass")).description("Use dirt instead of grass.")).defaultValue(false)).build());
      this.firstAlgorithm = this.sgGeneral.add(((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)(new EnumSetting.Builder()).name("first-sorting-mode")).description("The blocks you want to place first.")).defaultValue(Printer.SortAlgorithm.None)).build());
      this.secondAlgorithm = this.sgGeneral.add(((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)(new EnumSetting.Builder()).name("second-sorting-mode")).description("Second pass of sorting eg. place first blocks higher and closest to you.")).defaultValue(Printer.SortingSecond.None)).visible(() -> ((SortAlgorithm)this.firstAlgorithm.get()).applySecondSorting)).build());
      this.listMode = this.sgWorkMode.add(((EnumSetting.Builder)((EnumSetting.Builder)((EnumSetting.Builder)(new EnumSetting.Builder()).name("list-mode")).description("Block list mode.")).defaultValue(Printer.FilterMode.NONE)).build());
      this.filterBlocks = this.sgWorkMode.add(((BlockListSetting.Builder)((BlockListSetting.Builder)((BlockListSetting.Builder)(new BlockListSetting.Builder()).name("filter-blocks")).description("Blocks to whitelist or blacklist.")).visible(() -> this.listMode.get() != Printer.FilterMode.NONE)).build());
      this.renderBlocks = this.sgRendering.add(new BoolSetting.Builder().name("render-placed-blocks").description("Renders block placements.").defaultValue(true).build());
      this.fadeTime = this.sgRendering.add(new DoubleSetting.Builder().name("fade-time").description("Time for the rendering to fade. Duration multiplied by tick amount below.").defaultValue(1.15).min(0.1).sliderMin(0.1).max(1000.0).sliderMax(20.0).decimalPlaces(2).visible(this.renderBlocks::get).build());
      this.fadeAmount = this.sgRendering.add(new DoubleSetting.Builder().name("Duration amount").description("The time in ticks for it to fade away expire.").defaultValue(10.0).min(0.0).sliderMin(0.0).max(20.0).sliderMax(20.0).decimalPlaces(1).visible(this.renderBlocks::get).build());
      this.colour = this.sgRendering.add(new ColorSetting.Builder().name("colour").description("The cubes colour.").defaultValue(new SettingColor(95, 190, 255)).visible(this.renderBlocks::get).build());
      this.useOffhand = ((BoolSetting.Builder)((BoolSetting.Builder)((BoolSetting.Builder)(new BoolSetting.Builder()).name("use-offhand")).description("Automatically put block items in the offhand while printing.")).defaultValue(false)).build();
      this.respectFalling = this.sgGeneral.add(new BoolSetting.Builder().name("Respect falling blocks").description("Refuse to place falling blocks until there's ground beneath them.").defaultValue(true).build());
      this.specialFix = this.sgGeneral.add(new BoolSetting.Builder().name("Special object fix").description("Rapidly interact with blocks like Doors and Repeaters until they match the schematic.").defaultValue(true).build());
      this.noInteract = this.sgWorkMode.add(new BoolSetting.Builder().name("Block accidental interactions").description("This fixes accidental interactions caused by the printer.").defaultValue(true).build());
      this.blockTimeAmount = this.sgWorkMode.add(new IntSetting.Builder().name("Block time amount").description("Blocks any other interactions other than the printer's for the set amount of ticks.").defaultValue(2).min(1).max(20).sliderMin(1).sliderMax(20).visible(this.noInteract::get).build());
      
      this.stopOnInterrupt = this.sgWorkMode.add(new BoolSetting.Builder().name("Stop on interrupt").description("Pauses the printer if you manually interact or switch hotbar slots.").defaultValue(false).build());
      this.interruptTicks = this.sgWorkMode.add(new IntSetting.Builder().name("Interrupt ticks").description("How many ticks to wait before resuming after an interruption.").defaultValue(10).min(1).max(1000).sliderMin(1).sliderMax(20).visible(this.stopOnInterrupt::get).build());

      this.antiWrongDoor = this.sgNiche.add(new BoolSetting.Builder().name("Anti-wrong door placement").description("Only place doors if the predicted hinge matches the schematic.").defaultValue(true).build());
      this.dontRemindDoor = this.sgNiche.add(new BoolSetting.Builder().name("Don't remind to disable (Doors)").description("Removes the 'You can disable this...' part from door warnings.").defaultValue(false).visible(this.antiWrongDoor::get).build());
      this.acknowledgeDoor = this.sgNiche.add(new BoolSetting.Builder().name("Acknowledge (Doors)").description("Silences all door placement warnings.").defaultValue(false).visible(() -> (Boolean)this.antiWrongDoor.get() && (Boolean)this.dontRemindDoor.get()).build());

      this.tellChestError = this.sgNiche.add(new BoolSetting.Builder().name("Tell when chest placement won't be correct").description("Informs you using chat if the placement of chests will not be correct.").defaultValue(true).build());
      this.dontRemindChest = this.sgNiche.add(new BoolSetting.Builder().name("Don't remind to disable (Chests)").description("Removes the 'You can disable this...' part from chest warnings.").defaultValue(false).visible(this.tellChestError::get).build());
      this.acknowledgeChest = this.sgNiche.add(new BoolSetting.Builder().name("Acknowledge (Chests)").description("Silences all chest placement warnings.").defaultValue(false).visible(() -> (Boolean)this.tellChestError.get() && (Boolean)this.dontRemindChest.get()).build());

      this.reserveSlot = this.sgWorkMode.add(new BoolSetting.Builder().name("Reserve slot").description("Makes the printer only use that slot and not the one you recently used.").defaultValue(false).build());
      this.reservedSlotValue = this.sgWorkMode.add(new IntSetting.Builder().name("Reserved slot").description("The hotbar slot to reserve for the printer.").defaultValue(1).min(1).max(9).sliderMin(1).sliderMax(9).visible(this.reserveSlot::get).build());
      this.antiBoxIn = this.sgWorkMode.add(new BoolSetting.Builder().name("Anti-box in").description("Configure this setting to prevent awkward situations where the printer boxes you in.").defaultValue(false).build());
      this.antiBoxInX = this.sgWorkMode.add(new IntSetting.Builder().name("anti-box-in-x").description("X rotation for anti-box in.").defaultValue(0).min(0).max(360).sliderMin(0).sliderMax(360).visible(this.antiBoxIn::get).build());
      this.antiBoxInY = this.sgWorkMode.add(new IntSetting.Builder().name("anti-box-in-y").description("Y rotation for anti-box in.").defaultValue(0).min(0).max(360).sliderMin(0).sliderMax(360).visible(this.antiBoxIn::get).build());
      this.antiBoxInZ = this.sgWorkMode.add(new IntSetting.Builder().name("anti-box-in-z").description("Z rotation for anti-box in.").defaultValue(0).min(0).max(360).sliderMin(0).sliderMax(360).visible(this.antiBoxIn::get).build());
      this.antiBoxInOrigin = this.sgWorkMode.add(new EnumSetting.Builder<AntiBoxInOrigin>().name("anti-box-in-origin").description("The origin for anti-box in.").defaultValue(AntiBoxInOrigin.Legs).visible(this.antiBoxIn::get).build());
      this.viewPosition = this.sgWorkMode.add(new BoolSetting.Builder().name("View position").description("Shows an arrow at your character showing you exactly where the printer will print.").defaultValue(false).visible(this.antiBoxIn::get).build());
      this.antiBoxInColor = this.sgWorkMode.add(new ColorSetting.Builder().name("anti-box-in-color").description("The color of the anti-box in arrow.").defaultValue(new SettingColor(255, 0, 0)).visible(() -> (Boolean) this.antiBoxIn.get() && (Boolean) this.viewPosition.get()).build());

      this.superSpeed = this.sgWorkMode.add(new BoolSetting.Builder().name("Superspeed").description("Place blocks at supersonic speeds! NOTE: You will lose the ability to place complicated blocks such as water, lava, hoppers, chests, ect.").defaultValue(false).onChanged(v -> {
         if (this.mc.screen instanceof meteordevelopment.meteorclient.gui.WidgetScreen) ((meteordevelopment.meteorclient.gui.WidgetScreen) this.mc.screen).invalidate();
      }).build());
      this.bpt = this.sgWorkMode.add(new IntSetting.Builder().name("blocks/tick").description("How many blocks place per tick.").defaultValue(2).min(2).sliderMin(1).max(15).sliderMax(15).visible(this.superSpeed::get).onChanged(v -> {
         if (this.mc.screen instanceof meteordevelopment.meteorclient.gui.WidgetScreen) ((meteordevelopment.meteorclient.gui.WidgetScreen) this.mc.screen).invalidate();
      }).build());

      this.usedSlot = -1;
      this.toSort = new ArrayList();
      this.placed_fade = new ArrayList();
      this.syncTasks = new HashMap<>();
      this.lastSlot = -1;
      this.printerSelectedSlot = -1;
      this.interruptTimer = 0;
   }

   public void onActivate() {
      this.onDeactivate();
      if (this.mc.player != null) {
         this.lastSlot = this.mc.player.getInventory().getSelectedSlot();
         this.printerSelectedSlot = this.lastSlot;
      }
   }

   public void onDeactivate() {
      this.placed_fade.clear();
      this.interruptTimer = 0;
   }

   private void resetInterrupt() {
      if ((Boolean)this.stopOnInterrupt.get()) {
         this.interruptTimer = (Integer)this.interruptTicks.get();
      }
      // Force clearing last placement time so manual clicks work immediately
      this.lastPlacementTime = 0;
   }

   @EventHandler
   private void onInteractBlock(InteractBlockEvent event) {
      if (MyUtils.isPlacing) return;
      resetInterrupt();

      if ((Boolean)this.noInteract.get()) {
         if (System.currentTimeMillis() - this.lastPlacementTime < (long) (Integer) this.blockTimeAmount.get() * 50L) {
            event.cancel();
         }
      }
   }

   @EventHandler
   private void onInteractEntity(InteractEntityEvent event) {
      if (MyUtils.isPlacing) return;
      resetInterrupt();

      if ((Boolean)this.noInteract.get()) {
         if (System.currentTimeMillis() - this.lastPlacementTime < (long) (Integer) this.blockTimeAmount.get() * 50L) {
            event.cancel();
         }
      }
   }

   @EventHandler
   private void onInteractItem(InteractItemEvent event) {
      if (MyUtils.isPlacing) return;
      resetInterrupt();
   }

   @EventHandler
   private void onAttackEntity(AttackEntityEvent event) {
      resetInterrupt();
   }

   @EventHandler
   private void onStartBreakingBlock(StartBreakingBlockEvent event) {
      resetInterrupt();
   }

   @EventHandler
   private void onMouseScroll(MouseScrollEvent event) {
      if (this.mc.screen == null) {
         resetInterrupt();
      }
   }

   @EventHandler
   private void onTick(TickEvent.Post event) {
      if (this.mc.player != null && this.mc.level != null && this.mc.gameMode != null) {
         // Update render fading regardless of interruption
         this.placed_fade.forEach((s) -> s.setA((Integer)s.getA() - 1));
         this.placed_fade.removeIf((s) -> (Integer)s.getA() <= 0);

         // Interruption Detection: Hotbar change
         int currentSlot = this.mc.player.getInventory().getSelectedSlot();
         if (this.lastSlot != -1 && currentSlot != this.lastSlot) {
            // If the new slot doesn't match what the printer just picked, it's a manual change
            if (currentSlot != this.printerSelectedSlot) {
               resetInterrupt();
            }
         }
         this.lastSlot = currentSlot;

         // Handle Timer
         if (this.interruptTimer > 0) {
            this.interruptTimer--;
            return;
         }

         GameType gameMode = this.mc.gameMode.getPlayerMode();
         if (gameMode == GameType.SPECTATOR || gameMode == GameType.ADVENTURE) {
            error("This gamemode cannot place blocks, therefore is unsupported.");
            this.toggle();
            return;
         }

         long stackingRetryTimeout = (long) (this.printing_delay.get() * 50.0 + 50.0);
         MyUtils.cleanStackingMemory(stackingRetryTimeout);

         if ((Boolean)this.specialFix.get()) {
            this.syncTasks.entrySet().removeIf(entry -> {
               BlockPos p = entry.getKey();
               BlockState req = entry.getValue();
               if (this.mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(p)) > this.printing_range.get()) return false;
               BlockState curr = this.mc.level.getBlockState(p);
               if (curr.getBlock() != req.getBlock()) return true;

               Vec3 visiblePoint = MyUtils.getVisiblePoint(p, curr);
               if (visiblePoint != null) {
                  boolean done = this.syncStateImmediate(req, curr, p, visiblePoint);
                  if (!done && (Boolean)this.clientSide.get()) {
                      Rotation r = RotationStuff.calcRotationFromVec3d(this.mc.player.getEyePosition(), visiblePoint, new Rotation(this.mc.player.getYRot(), this.mc.player.getXRot()));
                      Rotations.rotate(r.getYaw(), r.getPitch(), 100, true, () -> {});
                  }
                  return done;
               }
               return false;
            });
         }

         WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
         if (worldSchematic == null) {
            this.placed_fade.clear();
            this.toggle();
         } else {
            this.toSort.clear();
            this.timer += 1.0;
            if (this.timer >= this.printing_delay.get()) {
               BlockIterator.register(this.printing_range.get().intValue() + 1, this.printing_range.get().intValue() + 1, (pos, blockState) -> {
                  BlockState required = worldSchematic.getBlockState(pos);
                  // Real world state vs Virtual (optimistic) state
                  BlockState worldState = blockState;
                  BlockState virtualState = MyUtils.getStackingState(pos, worldState);

                  if (this.mc.player.blockPosition().closerThan(pos, this.printing_range.get()) && required.canSurvive(this.mc.level, pos) && required.getFluidState().isEmpty() && !required.isAir() && DataManager.getRenderLayerRange().isPositionWithinRange(pos)) {
                     if ((Boolean)this.antiBoxIn.get()) {
                         Vec3 playerPos = this.antiBoxInOrigin.get() == AntiBoxInOrigin.Legs ? this.mc.player.position() : this.mc.player.getEyePosition();
                         Vec3 blockPos = Vec3.atCenterOf(pos);
                         Vec3 relative = blockPos.subtract(playerPos);
                         Vec3 facing = getAntiBoxInVector();
                         if (relative.dot(facing) < 0) return;
                     }
                     if (required.getBlock() instanceof BedBlock && required.getValue(BedBlock.PART) == BedPart.HEAD) return;
                     if (required.getBlock() instanceof DoorBlock && required.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) return;
                     if (required.getBlock().getDescriptionId().contains("sign")) return; // Unsupported blocks (Reversible: just remove this line)

                     boolean isWrongBlock = virtualState.getBlock() != required.getBlock();
                     if (!isWrongBlock && required.hasProperty(BlockStateProperties.SLAB_TYPE) && virtualState.hasProperty(BlockStateProperties.SLAB_TYPE)) {
                         if (required.getValue(BlockStateProperties.SLAB_TYPE) != virtualState.getValue(BlockStateProperties.SLAB_TYPE)) {
                             // Slabs of same block but wrong type are considered wrong
                             isWrongBlock = true;
                         }
                     }
                     if (!isWrongBlock && required.hasProperty(BlockStateProperties.CHEST_TYPE) && virtualState.hasProperty(BlockStateProperties.CHEST_TYPE)) {
                         if (required.getValue(BlockStateProperties.CHEST_TYPE) != virtualState.getValue(BlockStateProperties.CHEST_TYPE)) {
                             isWrongBlock = true;
                         }
                     }

                     // We need to place if the WORLD is under-stacked, but only if we haven't already VIRTULLY placed it.
                     boolean worldNeedsMore = worldState.getBlock() == required.getBlock() && this.isUnderStacked(required, worldState);
                     boolean virtualNeedsMore = virtualState.getBlock() == required.getBlock() && this.isUnderStacked(required, virtualState);
                     
                     boolean isStale = false;
                     if (worldNeedsMore) {
                         long retryDelay = (long) (this.printing_delay.get() * 50.0 + 50.0);
                         isStale = MyUtils.isStaleStacking(pos, retryDelay);
                     }

                     if ((virtualState.canBeReplaced() && isWrongBlock) || virtualNeedsMore || (worldNeedsMore && isStale)) {
                        if (!worldNeedsMore || required.getBlock() instanceof net.minecraft.world.level.block.SlabBlock) {
                           if (this.mc.player.getBoundingBox().intersects(Vec3.atLowerCornerOf(pos), Vec3.atLowerCornerOf(pos).add((double)1.0F, (double)1.0F, (double)1.0F))) return;
                        }
                        
                        if ((Boolean)this.respectFalling.get()) {
                           boolean isFalling = MyUtils.isFallingBlock(required.getBlock());
                           boolean isStackable = MyUtils.isStackable(required.getBlock()) && !(required.getBlock() instanceof SlabBlock);
                           if ((isFalling || isStackable) && this.mc.level.getBlockState(pos.relative(Direction.DOWN)).isAir()) {
                              return;
                           }
                        }

                        DoorHingeSide wantedHinge = null;
                        if (required.getBlock() instanceof DoorBlock) {
                           wantedHinge = MyUtils.getHinge(worldSchematic, pos, required);
                        }

                        if (wantedHinge != null && (Boolean)this.antiWrongDoor.get()) {
                            if (MyUtils.predictHinge(pos, required) != wantedHinge) {
                                if (System.currentTimeMillis() - lastDoorErrorMessageTime > 10000) {
                                    boolean hasDoor = mc.player.isCreative() || InvUtils.find(required.getBlock().asItem()).found();
                                    if (hasDoor) {
                                        if (!(Boolean)this.acknowledgeDoor.get()) {
                                            String msg = "The door you are attempting to place will not have the right properties.";
                                            if (!(Boolean)this.dontRemindDoor.get()) msg += " You can disable this in settings in the Niche tab.";
                                            info(msg);
                                        }
                                        lastDoorErrorMessageTime = System.currentTimeMillis();
                                    }
                                }
                                return;
                            }
                        }

                        boolean isBlockInLineOfSight = MyUtils.isBlockInLineOfSight(pos, required);
                        if ((Boolean)this.placeThroughWall.get() || isBlockInLineOfSight) {
                           FilterMode fm = (FilterMode)this.listMode.get();
                           if (fm == Printer.FilterMode.NONE || fm == Printer.FilterMode.WHITELIST && ((List)this.filterBlocks.get()).contains(required.getBlock()) || fm == Printer.FilterMode.BLACKLIST && !((List)this.filterBlocks.get()).contains(required.getBlock())) {
                              // MATERIAL CHECK: Skip blocks if we don't have them in inventory (treat as "out of range")
                              Item item = getItem(required);
                              if (this.mc.player.isCreative() || InvUtils.find(item).found()) {
                                 this.toSort.add(new BlockPos(pos));
                              }
                           }
                        }
                     } else if ((Boolean)this.specialFix.get() && worldState.getBlock() == required.getBlock() && !this.syncTasks.containsKey(pos)) {
                        if (required.getBlock() instanceof BedBlock && required.getValue(BedBlock.PART) == BedPart.HEAD) return;
                        if (required.getBlock() instanceof DoorBlock && required.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) return;
                        if (this.needsSync(required, worldState, pos)) {
                           this.syncTasks.put(new BlockPos(pos), required);
                        }
                     }
                  }

               });
               BlockIterator.after(() -> {
                  if (this.firstAlgorithm.get() != Printer.SortAlgorithm.None) {
                     if (((SortAlgorithm)this.firstAlgorithm.get()).applySecondSorting && this.secondAlgorithm.get() != Printer.SortingSecond.None) {
                        this.toSort.sort(((SortingSecond)this.secondAlgorithm.get()).algorithm);
                     }

                     this.toSort.sort(((SortAlgorithm)this.firstAlgorithm.get()).algorithm);
                  }
                  
                  // Prioritize Double Chests
                  this.toSort.sort((a, b) -> {
                      BlockState stateA = worldSchematic.getBlockState(a);
                      BlockState stateB = worldSchematic.getBlockState(b);
                      boolean isDoubleA = stateA.getBlock() instanceof ChestBlock && stateA.hasProperty(BlockStateProperties.CHEST_TYPE) && stateA.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE;
                      boolean isDoubleB = stateB.getBlock() instanceof ChestBlock && stateB.hasProperty(BlockStateProperties.CHEST_TYPE) && stateB.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE;
                      if (isDoubleA && !isDoubleB) return -1;
                      if (!isDoubleA && isDoubleB) return 1;
                      return 0;
                  });

                  int placed = 0;
                  int maxBlocks = (Boolean)this.superSpeed.get() ? Math.max(2, (Integer)this.bpt.get()) : 1;
                  Item typeToPlace = null;

                  for(BlockPos pos : this.toSort) {
                     BlockState state = worldSchematic.getBlockState(pos);

                     // SUPERSPEED FILTER: Only allow simple blocks and lock to one type per tick
                     if ((Boolean)this.superSpeed.get()) {
                         if (!isSimpleBlock(state)) continue;
                         Item currentItem = getItem(state);
                         if (typeToPlace != null && currentItem != typeToPlace) continue;
                     }

                     Item item = getItem(state);
                     
                     // Item Mapping for Blocks without direct Item counterparts (e.g. Vine Plants)
                     String blockId = state.getBlock().getDescriptionId();
                     if (item == Items.AIR) {
                         if (blockId.contains("weeping_vines")) item = Items.WEEPING_VINES;
                         else if (blockId.contains("twisting_vines")) item = Items.TWISTING_VINES;
                     }
                     
                     if ((Boolean)this.dirtgrass.get() && item == Items.GRASS_BLOCK) {
                        item = Items.DIRT;
                     }

                     boolean placedBlock = false;
                     if ((Boolean)this.useOffhand.get()) {
                        placedBlock = this.switchItemOffhand(item, () -> this.place(state, pos));
                     } else {
                        placedBlock = this.switchItem(item, state, () -> this.place(state, pos));
                     }

                     if (placedBlock) {
                        if (typeToPlace == null && (Boolean)this.superSpeed.get()) typeToPlace = item;
                        this.timer = 0.0;
                        this.lastPlacementTime = System.currentTimeMillis();
                        ++placed;
                        if ((Boolean)this.renderBlocks.get()) {
                           this.placed_fade.add(new Tuple((int) (this.fadeTime.get() * this.fadeAmount.get()), new BlockPos(pos)));
                        }

                        if ((Boolean)this.specialFix.get()) {
                           this.syncTasks.put(new BlockPos(pos), state);
                        }

                        if (placed >= maxBlocks) {
                           return;
                        }
                     }
                  }

               });
            }
         }
      } else {
         this.placed_fade.clear();
      }
   }

   public boolean place(BlockState required, BlockPos pos) {
      if (this.mc.player != null && this.mc.level != null) {
         if (required.getBlock().getDescriptionId().contains("sign")) return false; // Unsupported blocks (Reversible)
         BlockState worldState = this.mc.level.getBlockState(pos);
         BlockState virtualState = MyUtils.getStackingState(pos, worldState);

         boolean worldStacking = worldState.getBlock() == required.getBlock() && isUnderStacked(required, worldState);
         boolean virtualStacking = virtualState.getBlock() == required.getBlock() && isUnderStacked(required, virtualState);

         if (!worldState.canBeReplaced() && !worldStacking) {
            return false;
         } else if (!virtualStacking && worldStacking) {
            long retryDelay = (long) (this.printing_delay.get() * 50.0 + 50.0);
            if (!MyUtils.isStaleStacking(pos, retryDelay)) return false;
            return true; // Stale, allow retry
         } else {
            try {
               WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
               SlabType wantedSlabType = required.hasProperty(BlockStateProperties.SLAB_TYPE) ? (SlabType)required.getValue(BlockStateProperties.SLAB_TYPE) : null;
               Half wantedBlockHalf = required.hasProperty(BlockStateProperties.HALF) ? (Half)required.getValue(BlockStateProperties.HALF) : null;
               Direction wantedFacing = MyUtils.getFacing(required);
               Direction.Axis wantedAxies = required.hasProperty(BlockStateProperties.AXIS) ? (Direction.Axis)required.getValue(BlockStateProperties.AXIS) : null;
               DoorHingeSide wantedHinge = (required.getBlock() instanceof DoorBlock) ? MyUtils.getHinge(worldSchematic, pos, required) : null;

               if (wantedHinge != null && (Boolean)this.antiWrongDoor.get()) {
                   DoorHingeSide predicted = MyUtils.predictHinge(pos, required);
                   if (predicted != wantedHinge) {
                       if (System.currentTimeMillis() - lastDoorErrorMessageTime > 10000) {
                           if (!(Boolean)this.acknowledgeDoor.get()) {
                               String msg = "The door you are attempting to place will not have the right properties.";
                               if (!(Boolean)this.dontRemindDoor.get()) msg += " You can disable this in settings in the Niche tab.";
                               info(msg);
                           }
                           lastDoorErrorMessageTime = System.currentTimeMillis();
                       }
                       return false;
                   }
               }

               Direction placeSide = null;
               boolean effectiveAirPlace = true;

               boolean forceSneak = false;
               if (required.getBlock() instanceof ChestBlock) {
                   MyUtils.PredictionResult chestPrediction = MyUtils.predictChestType(pos, required, worldSchematic);
                   if (chestPrediction == MyUtils.PredictionResult.FAIL_INCORRECT_CONNECTION) {
                       if ((Boolean)this.tellChestError.get() && System.currentTimeMillis() - lastChestErrorMessageTime > 10000) {
                           if (!(Boolean)this.acknowledgeChest.get()) {
                               String msg = "The double chest you're trying to place will not have the right properties.";
                               if (!(Boolean)this.dontRemindChest.get()) msg += " You can disable this in the settings in the niche tab.";
                               info(msg);
                           }
                           lastChestErrorMessageTime = System.currentTimeMillis();
                       }
                       return false;
                   }
                   forceSneak = (chestPrediction == MyUtils.PredictionResult.SHOULD_SNEAK);
               }

               boolean result = MyUtils.place(pos, placeSide, wantedSlabType, wantedBlockHalf, wantedFacing, wantedAxies, effectiveAirPlace, false, true, (Boolean)this.clientSide.get(), this.printing_range.get().intValue(), (Boolean)this.useOffhand.get() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, required, wantedHinge, forceSneak);

               return result;
            } finally {
            }
         }
      } else {
         return false;
      }
   }

   private boolean switchItem(Item item, BlockState state, Supplier<Boolean> action) {
      if (this.mc.player == null) {
         return false;
      } else {
         int selectedSlot = this.mc.player.getInventory().getSelectedSlot();
         boolean isCreative = this.mc.player.isCreative();
         int targetSlot = (Boolean)this.reserveSlot.get() ? (Integer)this.reservedSlotValue.get() - 1 : -1;

         // ONLY swap to the reserved slot if we actually have the item (or are in creative)
         if ((Boolean)this.reserveSlot.get() && selectedSlot != targetSlot) {
            if (isCreative || InvUtils.find(item).found()) {
               InvUtils.swap(targetSlot, (Boolean)this.returnHand.get());
               this.printerSelectedSlot = targetSlot;
            } else {
               return false;
            }
         }

         if (this.mc.player.getMainHandItem().getItem() == item) {
            if ((Boolean)action.get()) {
               this.usedSlot = this.mc.player.getInventory().getSelectedSlot();
               this.printerSelectedSlot = this.usedSlot;
               return true;
            } else {
               return false;
            }
         } else if (!(Boolean)this.reserveSlot.get() && this.usedSlot != -1 && this.mc.player.getInventory().getItem(this.usedSlot).getItem() == item) {
            InvUtils.swap(this.usedSlot, (Boolean)this.returnHand.get());
            this.printerSelectedSlot = this.usedSlot;
            if ((Boolean)action.get()) {
               return true;
            } else {
               InvUtils.swap(selectedSlot, (Boolean)this.returnHand.get());
               this.printerSelectedSlot = selectedSlot;
               return false;
            }
            // 500 lines YAY!
         } else if (isCreative) {
            int slot = (Boolean)this.reserveSlot.get() ? targetSlot : this.mc.player.getInventory().getSelectedSlot();
            FindItemResult hotbarResult = InvUtils.find((stack) -> stack.getItem() == item, 0, 8);
            if (hotbarResult.found()) {
               if ((Boolean)this.reserveSlot.get() && hotbarResult.slot() != targetSlot) {
                  ItemStack stack = new ItemStack(item);
                  this.mc.player.getInventory().setItem(targetSlot, stack);
                  this.mc.player.containerMenu.getSlot(36 + targetSlot).set(stack);
                  this.mc.getConnection().send(new ServerboundSetCreativeModeSlotPacket((short)(36 + targetSlot), stack));
                  InvUtils.swap(targetSlot, (Boolean)this.returnHand.get());
                  this.printerSelectedSlot = targetSlot;
               } else {
                  InvUtils.swap(hotbarResult.slot(), (Boolean)this.returnHand.get());
                  this.printerSelectedSlot = hotbarResult.slot();
               }

               this.usedSlot = this.mc.player.getInventory().getSelectedSlot();
               this.printerSelectedSlot = this.usedSlot;
               return (Boolean)action.get();
            } else {
               ItemStack stack = new ItemStack(item);
               this.mc.player.getInventory().setItem(slot, stack);
               this.mc.player.containerMenu.getSlot(36 + slot).set(stack);
               this.mc.getConnection().send(new ServerboundSetCreativeModeSlotPacket((short)(36 + slot), stack));
               if (this.mc.player.getInventory().getSelectedSlot() != slot) {
                  InvUtils.swap(slot, (Boolean)this.returnHand.get());
               }
               this.usedSlot = slot;
               this.printerSelectedSlot = slot;
               return (Boolean)action.get();
            }
         } else {
            FindItemResult result = InvUtils.find(new Item[]{item});
            if (result.found()) {
               if ((Boolean)this.reserveSlot.get()) {
                  if (result.slot() != targetSlot) {
                     InvUtils.move().from(result.slot()).toHotbar(targetSlot);
                  }

                  InvUtils.swap(targetSlot, (Boolean)this.returnHand.get());
                  this.printerSelectedSlot = targetSlot;
                  if ((Boolean)action.get()) {
                     this.usedSlot = targetSlot;
                     this.printerSelectedSlot = targetSlot;
                     return true;
                  } else {
                     return false;
                  }
               } else if (result.isHotbar()) {
                  InvUtils.swap(result.slot(), (Boolean)this.returnHand.get());
                  this.printerSelectedSlot = result.slot();
                  if ((Boolean)action.get()) {
                     this.usedSlot = this.mc.player.getInventory().getSelectedSlot();
                     this.printerSelectedSlot = this.usedSlot;
                     return true;
                  } else {
                     InvUtils.swap(selectedSlot, (Boolean)this.returnHand.get());
                     this.printerSelectedSlot = selectedSlot;
                     return false;
                  }
               } else if (result.isMain()) {
                  FindItemResult empty = InvUtils.findEmpty();
                  if (empty.found() && empty.isHotbar()) {
                     InvUtils.move().from(result.slot()).toHotbar(empty.slot());
                     InvUtils.swap(empty.slot(), (Boolean)this.returnHand.get());
                     this.printerSelectedSlot = empty.slot();
                     if ((Boolean)action.get()) {
                        this.usedSlot = this.mc.player.getInventory().getSelectedSlot();
                        this.printerSelectedSlot = this.usedSlot;
                        return true;
                     } else {
                        InvUtils.swap(selectedSlot, (Boolean)this.returnHand.get());
                        this.printerSelectedSlot = selectedSlot;
                        return false;
                     }
                  } else if (this.usedSlot != -1) {
                     InvUtils.move().from(result.slot()).toHotbar(this.usedSlot);
                     InvUtils.swap(this.usedSlot, (Boolean)this.returnHand.get());
                     this.printerSelectedSlot = this.usedSlot;
                     if ((Boolean)action.get()) {
                        return true;
                     } else {
                        InvUtils.swap(selectedSlot, (Boolean)this.returnHand.get());
                        this.printerSelectedSlot = selectedSlot;
                        return false;
                     }
                  } else {
                     return false;
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         }
      }
   }

   private boolean switchItemOffhand(Item item, Supplier<Boolean> action) {
      if (this.mc.player != null && (Boolean)this.useOffhand.get()) {
         if (this.mc.player.getOffhandItem().getItem() == item) {
            return (Boolean)action.get();
         } else {
            FindItemResult result = InvUtils.find((stack) -> stack.getItem() == item, 0, 8);
            if (result.found()) {
               InvUtils.move().from(result.slot()).toOffhand();
               return (Boolean)action.get();
            } else if (this.mc.player.isCreative()) {
               this.mc.getConnection().send(new ServerboundSetCreativeModeSlotPacket((short)(this.mc.player.getInventory().getSelectedSlot() + 36), new ItemStack(item)));
               return false;
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private boolean syncStateImmediate(BlockState required, BlockState current, BlockPos pos, Vec3 hitPos) {
      if (this.mc.player == null || this.mc.level == null || this.mc.gameMode == null) return true;

      // Sync Trapdoors and Doors (OPEN property)
      if (required.hasProperty(BlockStateProperties.OPEN) && current.hasProperty(BlockStateProperties.OPEN)) {
          if (required.getValue(BlockStateProperties.OPEN) != current.getValue(BlockStateProperties.OPEN)) {
              if (required.getBlock() instanceof DoorBlock || required.getBlock() instanceof TrapDoorBlock) {
                  // Exclude Iron
                  String name = required.getBlock().getDescriptionId();
                  if (name.contains("iron")) return true;
              }
              this.interact(pos, hitPos, required);
              return false;
          }
      }

      // Sync Repeaters (DELAY property)
      if (required.hasProperty(BlockStateProperties.DELAY) && current.hasProperty(BlockStateProperties.DELAY)) {
          int targetDelay = required.getValue(BlockStateProperties.DELAY);
          int currentDelay = current.getValue(BlockStateProperties.DELAY);
          if (targetDelay != currentDelay) {
              this.interact(pos, hitPos, required);
              return false;
          }
      }

      // Sync Cake (BITES property)
      if (required.hasProperty(BlockStateProperties.BITES) && current.hasProperty(BlockStateProperties.BITES)) {
          int targetBites = required.getValue(BlockStateProperties.BITES);
          int currentBites = current.getValue(BlockStateProperties.BITES);
          if (currentBites < targetBites) {
              if (this.mc.player.isCreative() || this.mc.player.getFoodData().needsFood()) {
                  this.interact(pos, hitPos, required);
                  return false;
              }
          }
      }

      return true;
   }

   private boolean isUnderStacked(BlockState required, BlockState current) {
      return MyUtils.isUnderStacked(required, current);
   }

   private boolean needsSync(BlockState required, BlockState current, BlockPos pos) {
      if (required.hasProperty(BlockStateProperties.OPEN) && current.hasProperty(BlockStateProperties.OPEN)) {
          if (required.getValue(BlockStateProperties.OPEN) != current.getValue(BlockStateProperties.OPEN)) return true;
      }
      if (required.hasProperty(BlockStateProperties.DELAY) && current.hasProperty(BlockStateProperties.DELAY)) {
          if (required.getValue(BlockStateProperties.DELAY) != current.getValue(BlockStateProperties.DELAY)) return true;
      }
      if (required.hasProperty(BlockStateProperties.BITES) && current.hasProperty(BlockStateProperties.BITES)) {
          if (required.getValue(BlockStateProperties.BITES) > current.getValue(BlockStateProperties.BITES)) {
              return this.mc.player != null && (this.mc.player.isCreative() || this.mc.player.getFoodData().needsFood());
          }
      }
      if (required.getBlock() instanceof DoorBlock && required.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER) {
          WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
          if (worldSchematic != null) {
              BlockState reqUpper = worldSchematic.getBlockState(pos.above());
              BlockState currUpper = this.mc.level.getBlockState(pos.above());
              if (reqUpper.getBlock() instanceof DoorBlock && currUpper.getBlock() instanceof DoorBlock) {
                  if (reqUpper.hasProperty(BlockStateProperties.DOOR_HINGE) && currUpper.hasProperty(BlockStateProperties.DOOR_HINGE)) {
                      if (reqUpper.getValue(BlockStateProperties.DOOR_HINGE) != currUpper.getValue(BlockStateProperties.DOOR_HINGE)) return true;
                  }
              }
          }
      }
      return false;
   }

   private void interact(BlockPos pos, Vec3 hitPos, BlockState required) {
       try {
           Rotation rot = RotationStuff.calcRotationFromVec3d(this.mc.player.getEyePosition(), hitPos, new Rotation(this.mc.player.getYRot(), this.mc.player.getXRot()));

           float targetYaw = rot.getYaw();
           float targetPitch = rot.getPitch();

           if (required.getBlock() instanceof FenceGateBlock) {
               Direction facing = MyUtils.getFacing(required);
               if (facing != null && facing.getAxis().isHorizontal()) {
                   targetYaw = MyUtils.getYawFromDirection(facing);
               }
           }

           float oldYaw = this.mc.player.getYRot();
           float oldPitch = this.mc.player.getXRot();

           this.mc.player.setYRot(targetYaw);
           this.mc.player.setXRot(targetPitch);
           this.mc.player.yHeadRot = targetYaw;
           this.mc.player.yBodyRot = targetYaw;
           this.mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(targetYaw, targetPitch, this.mc.player.onGround(), this.mc.player.horizontalCollision));

           // Calculate direction based on hit position relative to block center
           double dx = hitPos.x - (pos.getX() + 0.5);
           double dy = hitPos.y - (pos.getY() + 0.5);
           double dz = hitPos.z - (pos.getZ() + 0.5);
           Direction side = Direction.UP;
           double absX = Math.abs(dx);
           double absY = Math.abs(dy);
           double absZ = Math.abs(dz);
           if (absX > absY && absX > absZ) side = dx > 0 ? Direction.EAST : Direction.WEST;
           else if (absZ > absY && absZ > absX) side = dz > 0 ? Direction.SOUTH : Direction.NORTH;
           else side = dy > 0 ? Direction.UP : Direction.DOWN;

           BlockHitResult bhr = new BlockHitResult(hitPos, side, pos, false);
           MyUtils.isPlacing = true;
           try {
               this.mc.gameMode.useItemOn(this.mc.player, InteractionHand.MAIN_HAND, bhr);
           } finally {
               MyUtils.isPlacing = false;
           }
           this.lastPlacementTime = System.currentTimeMillis();

           this.mc.player.setYRot(oldYaw);
           this.mc.player.setXRot(oldPitch);

           if ((Boolean)this.clientSide.get()) {
               Rotations.rotate(rot.getYaw(), rot.getPitch(), 100, true, () -> {});
           }
       } finally {
       }
   }

   private Vec3 getAntiBoxInVector() {
      double rx = Math.toRadians((Integer)this.antiBoxInX.get());
      double ry = Math.toRadians((Integer)this.antiBoxInY.get());
      double rz = Math.toRadians((Integer)this.antiBoxInZ.get());

      Vec3 v = new Vec3(0, 0, 1);
      v = rotateX(v, rx);
      v = rotateY(v, ry);
      v = rotateZ(v, rz);
      return v;
   }

   private Vec3 rotateX(Vec3 v, double angle) {
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      return new Vec3(v.x, v.y * cos - v.z * sin, v.y * sin + v.z * cos);
   }

   private Vec3 rotateY(Vec3 v, double angle) {
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      return new Vec3(v.x * cos + v.z * sin, v.y, -v.x * sin + v.z * cos);
   }

   private Vec3 rotateZ(Vec3 v, double angle) {
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      return new Vec3(v.x * cos - v.y * sin, v.x * sin + v.y * cos, v.z);
   }

   public boolean isSuperspeedEnabled() {
      return (Boolean) this.superSpeed.get();
   }

   private Direction dir(BlockState state) {
      Direction dir = MyUtils.getFacing(state);
      return dir != null ? dir : Direction.UP;
   }

   private boolean isSimpleBlock(BlockState state) {
      for (Property<?> prop : state.getProperties()) {
         String name = prop.getName().toLowerCase();
         if (name.equals("waterlogged") || name.contains("age")) continue;
         return false;
      }
      return true;
   }

   @Override
   public meteordevelopment.meteorclient.gui.widgets.WWidget getWidget(meteordevelopment.meteorclient.gui.GuiTheme theme) {
      return null;
   }

   @EventHandler
   private void onRender(Render3DEvent event) {
      this.placed_fade.forEach((s) -> {
         Color a = new Color(((SettingColor)this.colour.get()).r, ((SettingColor)this.colour.get()).g, ((SettingColor)this.colour.get()).b, (int)((float)(Integer)s.getA() / (float)(this.fadeTime.get() * this.fadeAmount.get()) * (float)((SettingColor)this.colour.get()).a));
         event.renderer.box((BlockPos)s.getB(), a, (Color)null, ShapeMode.Sides, 0);
      });
      if ((Boolean)this.antiBoxIn.get() && (Boolean)this.viewPosition.get() && this.mc.player != null) {
          Vec3 playerPos = this.antiBoxInOrigin.get() == AntiBoxInOrigin.Legs ? this.mc.player.position() : this.mc.player.getEyePosition(event.tickDelta);
          Vec3 facing = getAntiBoxInVector();
          Vec3 endPos = playerPos.add(facing.scale(1.5));
          Color color = (Color)this.antiBoxInColor.get();
          event.renderer.line(playerPos.x, playerPos.y, playerPos.z, endPos.x, endPos.y, endPos.z, color);

          // Arrowhead logic
          Vec3 v = facing.normalize();
          Vec3 axis = (Math.abs(v.y) < 0.99) ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
          Vec3 u = v.cross(axis).normalize().scale(0.15);
          Vec3 w = v.cross(u).normalize().scale(0.15);
          Vec3 base = endPos.subtract(v.scale(0.3));

          // Draw 20480 lines for the absolutely mathematically perfect cone arrowhead
          for (int i = 0; i < 20480; i++) {
              double angle = i * (Math.PI / 10240.0);
              double c = Math.cos(angle);
              double s = Math.sin(angle);
              Vec3 offset = u.scale(c).add(w.scale(s));
              Vec3 p = base.add(offset);
              event.renderer.line(endPos.x, endPos.y, endPos.z, p.x, p.y, p.z, color);
          }
      }
   }

   public static enum SortAlgorithm {
      None(false, (a, b) -> 0),
      TopDown(true, Comparator.comparingInt((value) -> value.getY() * -1)),
      DownTop(true, Comparator.comparingInt(Vec3i::getY)),
      Nearest(false, Comparator.comparingDouble((value) -> MeteorClient.mc.player != null ? Utils.squaredDistance(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ(), (double)value.getX() + (double)0.5F, (double)value.getY() + (double)0.5F, (double)value.getZ() + (double)0.5F) : (double)0.0F)),
      Furthest(false, Comparator.comparingDouble((value) -> MeteorClient.mc.player != null ? Utils.squaredDistance(MeteorClient.mc.player.getX(), MeteorClient.mc.player.getY(), MeteorClient.mc.player.getZ(), (double)value.getX() + (double)0.5F, (double)value.getY() + (double)0.5F, (double)value.getZ() + (double)0.5F) * (double)-1.0F : (double)0.0F));

      final boolean applySecondSorting;
      final Comparator<BlockPos> algorithm;

      private SortAlgorithm(boolean applySecondSorting, Comparator<BlockPos> algorithm) {
         this.applySecondSorting = applySecondSorting;
         this.algorithm = algorithm;
      }

      // $FF: synthetic method
      private static SortAlgorithm[] $values() {
         return new SortAlgorithm[]{None, TopDown, DownTop, Nearest, Furthest};
      }
   }

   public static enum SortingSecond {
      None(Printer.SortAlgorithm.None.algorithm),
      Nearest(Printer.SortAlgorithm.Nearest.algorithm),
      Furthest(Printer.SortAlgorithm.Furthest.algorithm);

      final Comparator<BlockPos> algorithm;

      private SortingSecond(Comparator<BlockPos> algorithm) {
         this.algorithm = algorithm;
      }

      // $FF: synthetic method
      private static SortingSecond[] $values() {
         return new SortingSecond[]{None, Nearest, Furthest};
      }
   }

   @EventHandler
   private void onPacketSend(PacketEvent.Send event) {
       if (event.packet instanceof ServerboundPlayerInputPacket packet) {
           if (!MyUtils.isAutoSneaking) {
               Input input = packet.input();
               if (input.shift()) {
                   event.packet = new ServerboundPlayerInputPacket(new Input(input.forward(), input.backward(), input.left(), input.right(), input.jump(), false, input.sprint()));
               }
           }
       }
       
       if (event.packet instanceof net.minecraft.network.protocol.game.ServerboundSwingPacket) {
           if (!MyUtils.isPlacing) {
               resetInterrupt();
           }
       }

       if (event.packet instanceof net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket packet) {
           // Intercept the intent to change slots before the next tick
           if (packet.getSlot() != this.printerSelectedSlot) {
               resetInterrupt();
           }
       }
   }

   private Item getItem(BlockState state) {
      Item item = state.getBlock().asItem();
      String blockId = state.getBlock().getDescriptionId();
      if (item == Items.AIR) {
          if (blockId.contains("weeping_vines")) item = Items.WEEPING_VINES;
          else if (blockId.contains("twisting_vines")) item = Items.TWISTING_VINES;
      }
      if ((Boolean)this.dirtgrass.get() && item == Items.GRASS_BLOCK) {
         item = Items.DIRT;
      }
      return item;
   }

   public static enum FilterMode {
      NONE,
      WHITELIST,
      BLACKLIST;

      // $FF: synthetic method
      private static FilterMode[] $values() {
         return new FilterMode[]{NONE, WHITELIST, BLACKLIST};
      }
   }

   public static enum AntiBoxInOrigin {
      Legs,
      Eyes
   }
}
