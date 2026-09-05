package written_by_Linuxenjoyer1234.linus_meteor_litematica_printer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.BlastFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.CrafterBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.LoomBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SmokerBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StonecutterBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MyUtils {
   private static class StackingInfo {
       final BlockState state;
       final long time;
       StackingInfo(BlockState state) {
           this.state = state;
           this.time = System.currentTimeMillis();
       }
   }
   public static boolean isAutoSneaking = false;
   public static boolean isPlacing = false;
   private static final Map<BlockPos, StackingInfo> stackingMemory = new ConcurrentHashMap<>();

   public static void updateStackingMemory(BlockPos pos, BlockState state) {
       if (isStackable(state.getBlock())) {
           BlockState current = getStackingState(pos, MeteorClient.mc.level.getBlockState(pos));
           stackingMemory.put(pos, new StackingInfo(calculateNextState(current, state)));
       }
   }

   public static BlockState getStackingState(BlockPos pos, BlockState fallback) {
       StackingInfo info = stackingMemory.get(pos);
       if (info != null && isStackable(info.state.getBlock())) {
           return info.state;
       }
       return fallback;
   }

   public static boolean isStaleStacking(BlockPos pos, long timeout) {
       StackingInfo info = stackingMemory.get(pos);
       return info != null && System.currentTimeMillis() - info.time > timeout;
   }

   public static void cleanStackingMemory(long timeout) {
       stackingMemory.entrySet().removeIf(entry -> {
           BlockState current = MeteorClient.mc.level.getBlockState(entry.getKey());
           if (current.isAir()) return false;
           
           // If world matches or exceeds what we thought we placed, we can clear it.
           if (current.getBlock() == entry.getValue().state.getBlock() && !isUnderStacked(entry.getValue().state, current)) {
               return true;
           }

           // Deadlock Prevention: Clear if entry is older than timeout
           return System.currentTimeMillis() - entry.getValue().time > timeout;
       });
   }

   public static boolean place(BlockPos blockPos, Direction direction, SlabType slabType, Half blockHalf, Direction wantedFacing, Direction.Axis wantedAxies, boolean airPlace, boolean swingHand, boolean rotate, boolean clientSide, int range, InteractionHand hand, BlockState state, DoorHingeSide wantedHinge, boolean forceSneak) {
      Block block = state.getBlock();
      if (MeteorClient.mc.player == null) {
         return false;
      } else if (!airPlace && !BlockUtils.canPlace(blockPos) && !isUnderStacked(state, MeteorClient.mc.level.getBlockState(blockPos))) {
         return false;
      } else {
         if (isVineType(state)) {
            Direction vineDir = null;
            if (state.getValue(BlockStateProperties.UP)) vineDir = Direction.UP;
            else if (state.getValue(BlockStateProperties.DOWN)) vineDir = Direction.DOWN;
            else if (state.getValue(BlockStateProperties.NORTH)) vineDir = Direction.NORTH;
            else if (state.getValue(BlockStateProperties.SOUTH)) vineDir = Direction.SOUTH;
            else if (state.getValue(BlockStateProperties.EAST)) vineDir = Direction.EAST;
            else if (state.getValue(BlockStateProperties.WEST)) vineDir = Direction.WEST;

            if (vineDir != null) {
               float yaw = getYawFromDirection(vineDir);
               float pitch = getPitchFromDirection(vineDir);
               Rotation vineRot = new Rotation(yaw, pitch);
               Direction finalDir = vineDir.getOpposite();
               BlockPos finalNeighbour = blockPos.relative(vineDir);
               Vec3 hitPos = new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5)
                              .add(new Vec3(vineDir.getStepX() * 0.5, vineDir.getStepY() * 0.5, vineDir.getStepZ() * 0.5));

               if (clientSide) {
                  Rotations.rotate(yaw, pitch, 100, true, () -> place(new BlockHitResult(hitPos, finalDir, finalNeighbour, false), swingHand, hand, vineRot, state, forceSneak));
               } else {
                  place(new BlockHitResult(hitPos, finalDir, finalNeighbour, false), swingHand, hand, vineRot, state, forceSneak);
               }
               return true;
            }
         }

         Vec3 hitPos = new Vec3((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + (double)0.5D, (double)blockPos.getZ() + 0.5D);

         if (block instanceof SlabBlock && slabType != null && slabType != SlabType.DOUBLE) {
             if (slabType == SlabType.BOTTOM) hitPos = new Vec3(hitPos.x, (double)blockPos.getY(), hitPos.z);
             else if (slabType == SlabType.TOP) hitPos = new Vec3(hitPos.x, (double)blockPos.getY() + 1.0D, hitPos.z);
         }

         if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
            AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
            if (face == AttachFace.FLOOR) direction = Direction.UP;
            else if (face == AttachFace.CEILING) direction = Direction.DOWN;
            else if (face == AttachFace.WALL && wantedFacing != null) direction = wantedFacing;
         } else if (block.getStateDefinition().getProperty("face") != null) {
            Property<?> prop = block.getStateDefinition().getProperty("face");
            String face = state.getValue(prop).toString().toUpperCase();
            if (face.contains("FLOOR")) direction = Direction.UP;
            else if (face.contains("CEILING")) direction = Direction.DOWN;
            else if (face.contains("WALL") && wantedFacing != null) direction = wantedFacing;
         }

         BlockState worldState = MeteorClient.mc.level.getBlockState(blockPos);
         BlockState currentState = getStackingState(blockPos, worldState);

         if (block instanceof SlabBlock && slabType != SlabType.DOUBLE && currentState.getBlock() == block) {
             if (currentState.hasProperty(SlabBlock.TYPE) && currentState.getValue(SlabBlock.TYPE) == slabType) {
                 return false;
             }
         }

         // World-First Stacking: Only stack if the block ACTUALLY exists in the world
         boolean isStackingCompletion = (block instanceof SlabBlock && slabType == SlabType.DOUBLE && worldState.getBlock() == block && worldState.hasProperty(SlabBlock.TYPE) && worldState.getValue(SlabBlock.TYPE) != SlabType.DOUBLE) || (worldState.getBlock() == block && isUnderStacked(state, worldState));

         if (block instanceof SlabBlock) {
             if (isStackingCompletion && currentState.hasProperty(SlabBlock.TYPE)) {
                 direction = (currentState.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) ? Direction.UP : Direction.DOWN;
             } else if (direction == null) {
                 // Smart Slab Air-Place: Prefer side faces to avoid accidental stacking on top/bottom neighbors
                 direction = MeteorClient.mc.player.getDirection();

                 // Fallback to vertical only if horizontal is blocked or wanted
                 if (slabType == SlabType.TOP) {
                     BlockPos above = blockPos.above();
                     if (MeteorClient.mc.level.getBlockState(above).isAir()) direction = Direction.DOWN;
                 } else if (slabType == SlabType.BOTTOM) {
                     BlockPos below = blockPos.below();
                     if (MeteorClient.mc.level.getBlockState(below).isAir()) direction = Direction.UP;
                 }
             }
         } else if (isStackingCompletion) {
             // For turtle eggs, candles, pickles: interact with the block below to stack inside the same block
             direction = Direction.UP;
             hitPos = new Vec3(hitPos.x, (double)blockPos.getY(), hitPos.z);
             blockPos = blockPos.below();
         } else if (block.getDescriptionId().contains("weeping_vines")) {
             // Weeping vines hang from the block above
             direction = Direction.DOWN;
             hitPos = new Vec3(hitPos.x, (double)blockPos.getY() + 1.0D, hitPos.z);
             blockPos = blockPos.above();
         } else if (block.getDescriptionId().contains("twisting_vines")) {
             // Twisting vines grow from the block below
             direction = Direction.UP;
             hitPos = new Vec3(hitPos.x, (double)blockPos.getY(), hitPos.z);
             blockPos = blockPos.below();
         } else if (direction == null) {
             direction = Direction.UP;
         }

         if (block instanceof DoorBlock) direction = Direction.UP;
         if (block instanceof TrapDoorBlock && blockHalf != null) direction = blockHalf == Half.TOP ? Direction.DOWN : Direction.UP;

         if (wantedFacing != null && !(block instanceof DoorBlock) && !(block instanceof TrapDoorBlock)) {
            boolean hasAttachFace = state.hasProperty(BlockStateProperties.ATTACH_FACE);
            AttachFace face = hasAttachFace ? state.getValue(BlockStateProperties.ATTACH_FACE) : null;
            if (hasAttachFace && face != AttachFace.WALL) {
                // Keep UP/DOWN from floor/ceiling logic
            } else if (block instanceof HopperBlock || isDirectBlock(block)) {
               direction = wantedFacing.getOpposite();
            } else {
               direction = wantedFacing;
            }
         }

         if (direction == null) direction = Direction.UP;

         final Direction finalDirection = direction;
         final BlockPos finalNeighbour = blockPos;

         if (block instanceof DoorBlock) {
             DoorHingeSide hinge = wantedHinge != null ? wantedHinge : DoorHingeSide.LEFT;
             double scanX = 0.5, scanZ = 0.5;
             Direction doorFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);

             // CORRECTED HARDCODED PRECISION: Matches Minecraft's DoorBlock#getHinge quadrant mapping
             // LEFT Hinge: North (X=0.25), South (X=0.75), West (Z=0.75), East (Z=0.25)
             // RIGHT Hinge: North (X=0.75), South (X=0.25), West (Z=0.25), East (Z=0.75)
             if (hinge == DoorHingeSide.LEFT) {
                 if (doorFacing == Direction.NORTH) scanX = 0.25;
                 else if (doorFacing == Direction.SOUTH) scanX = 0.75;
                 else if (doorFacing == Direction.WEST) scanZ = 0.75;
                 else if (doorFacing == Direction.EAST) scanZ = 0.25;
             } else {
                 if (doorFacing == Direction.NORTH) scanX = 0.75;
                 else if (doorFacing == Direction.SOUTH) scanX = 0.25;
                 else if (doorFacing == Direction.WEST) scanZ = 0.25;
                 else if (doorFacing == Direction.EAST) scanZ = 0.75;
             }
             hitPos = new Vec3((double)blockPos.getX() + scanX, (double)blockPos.getY(), (double)blockPos.getZ() + scanZ);
         } else if (block instanceof TrapDoorBlock && blockHalf != null) {
             hitPos = new Vec3((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + (blockHalf == Half.TOP ? 0.0D : 1.0D), (double)blockPos.getZ() + 0.5D);
         } else if (isStackingCompletion) {
            if (block instanceof SlabBlock) {
               hitPos = new Vec3((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.5D, (double)blockPos.getZ() + 0.5D);
            } else {
               // For stackables like eggs, pickles, candles:
               // Target the lower part of the block to ensure we hit the existing collision shape.
               // Turtle eggs height is 0.4375, pickles/candles are 0.375.
               hitPos = new Vec3((double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.3D, (double)blockPos.getZ() + 0.5D);
               direction = Direction.UP;
            }
         }

         final Vec3 finalHitPos = hitPos;

         if (rotate) {
            VoxelShape collisionShape = MeteorClient.mc.level.getBlockState(finalNeighbour).getCollisionShape(MeteorClient.mc.level, finalNeighbour);
            AABB aabb = collisionShape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : collisionShape.bounds();

            // Try to find a rotation that satisfies the orientation
            for(double v1 = 0.05; v1 <= 0.95; v1 += 0.2) {
               for(double v2 = 0.05; v2 <= 0.95; v2 += 0.2) {
                  // Door Hinge and Half property precision logic
                  double minXScan = 0.0, maxXScan = 1.0;
                  double minZScan = 0.0, maxZScan = 1.0;
                  double minYScan = 0.0, maxYScan = 1.0;

                  if (block instanceof DoorBlock && wantedHinge != null) {
                      Direction doorFacing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
                      // UNIVERSAL MULTI-LOGIC: Targets the specific quadrant required by Minecraft
                      if (wantedHinge == DoorHingeSide.LEFT) {
                          // Left Hinge Quadrants: North(W), South(E), West(S), East(N)
                          if (doorFacing == Direction.NORTH) { maxXScan = 0.45; }
                          else if (doorFacing == Direction.SOUTH) { minXScan = 0.55; }
                          else if (doorFacing == Direction.WEST) { minZScan = 0.55; }
                          else if (doorFacing == Direction.EAST) { maxZScan = 0.45; }
                      } else {
                          // Right Hinge Quadrants: North(E), South(W), West(N), East(S)
                          if (doorFacing == Direction.NORTH) { minXScan = 0.55; }
                          else if (doorFacing == Direction.SOUTH) { maxXScan = 0.45; }
                          else if (doorFacing == Direction.WEST) { maxZScan = 0.45; }
                          else if (doorFacing == Direction.EAST) { minZScan = 0.55; }
                      }
                      // Allow clicking anywhere on the lower half of the door
                      minYScan = 0.0;
                      maxYScan = 0.5;
                  }

                  if (wantedFacing != null && finalDirection.getAxis().isVertical()) {
                      if (block instanceof TrapDoorBlock || block instanceof StairBlock) {
                          if (wantedFacing == Direction.NORTH) maxZScan = 0.45;
                          else if (wantedFacing == Direction.SOUTH) minZScan = 0.55;
                          else if (wantedFacing == Direction.WEST) maxXScan = 0.45;
                          else if (wantedFacing == Direction.EAST) minXScan = 0.55;
                      }
                  }

                  if ((block instanceof StairBlock || block instanceof SlabBlock) && blockHalf != null) {
                      if (blockHalf == Half.TOP) minYScan = 0.55;
                      else maxYScan = 0.45;
                  }

                  if (block instanceof TrapDoorBlock && blockHalf != null && finalDirection.getAxis().isHorizontal()) {
                      if (blockHalf == Half.TOP) minYScan = 0.55;
                      else maxYScan = 0.45;
                  }

                  // Double Slab Precision Logic
                  if (block instanceof SlabBlock && slabType == SlabType.DOUBLE) {
                      if (currentState.getBlock() == block && currentState.hasProperty(SlabBlock.TYPE)) {
                          SlabType currentSlab = currentState.getValue(SlabBlock.TYPE);
                          if (currentSlab == SlabType.BOTTOM) minYScan = 0.45;
                          else if (currentSlab == SlabType.TOP) maxYScan = 0.55;
                      }
                  }

                  for(Vec3 placementMultiplier : aabbSideMultipliers(finalDirection)) {
                     double placeX, placeY, placeZ;
                     if (finalDirection.getAxis() == Direction.Axis.Y) {
                         placeX = (double)finalNeighbour.getX() + aabb.minX + v1 * (aabb.maxX - aabb.minX);
                         placeY = (double)finalNeighbour.getY() + aabb.minY + placementMultiplier.y * (aabb.maxY - aabb.minY);
                         placeZ = (double)finalNeighbour.getZ() + aabb.minZ + v2 * (aabb.maxZ - aabb.minZ);
                     } else if (finalDirection.getAxis() == Direction.Axis.Z) {
                         placeX = (double)finalNeighbour.getX() + aabb.minX + v1 * (aabb.maxX - aabb.minX);
                         placeY = (double)finalNeighbour.getY() + aabb.minY + v2 * (aabb.maxY - aabb.minY);
                         placeZ = (double)finalNeighbour.getZ() + aabb.minZ + placementMultiplier.z * (aabb.maxZ - aabb.minZ);
                     } else { // X axis
                         placeX = (double)finalNeighbour.getX() + aabb.minX + placementMultiplier.x * (aabb.maxX - aabb.minX);
                         placeY = (double)finalNeighbour.getY() + aabb.minY + v1 * (aabb.maxY - aabb.minY);
                         placeZ = (double)finalNeighbour.getZ() + aabb.minZ + v2 * (aabb.maxZ - aabb.minZ);
                     }

                     // Filter based on scan bounds, relative to the target block position
                     double dx = placeX - (double)blockPos.getX();
                     double dy = placeY - (double)blockPos.getY();
                     double dz = placeZ - (double)blockPos.getZ();

                     if (dx < minXScan || dx > maxXScan) continue;
                     if (dy < minYScan || dy > maxYScan) continue;
                     if (dz < minZScan || dz > maxZScan) continue;

                     if (block instanceof SlabBlock && slabType != null && slabType != SlabType.DOUBLE) {
                        // Single Slab: Strict Stacking Protection
                        // If clicking a neighbor slab, we MUST hit the half that doesn't trigger a stack.
                        if (slabType == SlabType.BOTTOM) {
                           // Placing Bottom Slab:
                           // 1. If neighbor is below, we are clicking its UP face.
                           // 2. If neighbor is on side, we MUST click the bottom half of the face.
                           if (finalDirection.getAxis().isHorizontal() && dy > 0.45) continue;
                           if (finalDirection == Direction.DOWN) continue; // Clicking bottom of neighbor above results in TOP slab
                        } else if (slabType == SlabType.TOP) {
                           // Placing Top Slab:
                           // 1. If neighbor is above, we are clicking its DOWN face.
                           // 2. If neighbor is on side, we MUST click the top half of the face.
                           if (finalDirection.getAxis().isHorizontal() && dy < 0.55) continue;
                           if (finalDirection == Direction.UP) continue; // Clicking top of neighbor below results in BOTTOM slab
                        }
                     }

                     if (block instanceof TrapDoorBlock && state.hasProperty(BlockStateProperties.HALF) && finalDirection.getAxis() != Direction.Axis.Y) {
                         Half half = state.getValue(BlockStateProperties.HALF);
                         if (half == Half.TOP && dy <= 0.5D) continue;
                         if (half == Half.BOTTOM && dy > 0.5D) continue;
                     }

                     Vec3 testHitPos = new Vec3(placeX, placeY, placeZ);
                     Vec3 playerHead = MeteorClient.mc.player.getEyePosition();
                     Rotation rot = RotationStuff.calcRotationFromVec3d(playerHead, testHitPos, new Rotation(MeteorClient.mc.player.getYRot(), MeteorClient.mc.player.getXRot()));

                     if (isPlayerOrientationDesired(state, wantedFacing, rot.normalize(), 0.1f) || (block instanceof TrapDoorBlock || block instanceof DoorBlock || block instanceof BedBlock || block instanceof StairBlock || block instanceof EndRodBlock || block instanceof LightningRodBlock || block instanceof AmethystClusterBlock || block instanceof HopperBlock || block instanceof BellBlock || block instanceof CrafterBlock || block instanceof GrindstoneBlock || block instanceof AnvilBlock || block instanceof FenceGateBlock || block.getDescriptionId().contains("candle") || block.getDescriptionId().contains("sea_pickle") || block.getDescriptionId().contains("turtle_egg") || block instanceof ButtonBlock)) {
                        Rotation finalRot;
                        if (wantedFacing != null) {
                            boolean isDirect = isDirectBlock(block);
                            if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
                                isDirect = state.getValue(BlockStateProperties.ATTACH_FACE) != AttachFace.WALL;
                            }
                            
                            float targetYaw = getYawFromDirection(isDirect ? wantedFacing : wantedFacing.getOpposite());
                            float targetPitch = getPitchFromDirection(isDirect ? wantedFacing : wantedFacing.getOpposite());

                            if (block instanceof TrapDoorBlock && blockHalf != null && finalDirection.getAxis().isVertical()) {
                                targetPitch = blockHalf == Half.TOP ? -90.0f : 90.0f;
                            }

                            if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
                                AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
                                if (face == AttachFace.FLOOR) targetPitch = 90.0f;
                                else if (face == AttachFace.CEILING) targetPitch = -90.0f;
                                else if (face == AttachFace.WALL) targetPitch = 0.0f;
                            } else if (state.getBlock().getStateDefinition().getProperty("face") != null) {
                                Property<?> prop = state.getBlock().getStateDefinition().getProperty("face");
                                String face = state.getValue(prop).toString().toUpperCase();
                                if (face.contains("FLOOR")) targetPitch = 90.0f;
                                else if (face.contains("CEILING")) targetPitch = -90.0f;
                                else if (face.contains("WALL")) targetPitch = 0.0f;
                            }

                            if (block instanceof AnvilBlock) {
                                targetYaw = getYawFromDirection(wantedFacing.getCounterClockWise());
                                targetPitch = 0;
                            }

                            if (block instanceof DoorBlock && wantedHinge != null) {
                                // Multi-Logic Rotation: Universal CCW/CW bias
                                Direction biasDir = (wantedHinge == DoorHingeSide.LEFT) ? wantedFacing.getCounterClockWise() : wantedFacing.getClockWise();
                                float biasYaw = getYawFromDirection(biasDir);
                                float diff = biasYaw - targetYaw;
                                while (diff > 180) diff -= 360;
                                while (diff < -180) diff += 360;
                                // 28 degree bias is strong enough to trigger hinge but safe for cardinal facing
                                targetYaw = targetYaw + Math.max(-28.0f, Math.min(28.0f, diff * 0.5f));
                            }
                            finalRot = new Rotation(targetYaw, targetPitch);
                        } else {
                            finalRot = rot.normalize();
                        }

                        if (clientSide) {
                            Rotations.rotate(finalRot.getYaw(), finalRot.getPitch(), 100, true, () -> place(new BlockHitResult(testHitPos, finalDirection, finalNeighbour, false), swingHand, hand, finalRot, state, forceSneak));
                        } else {
                            place(new BlockHitResult(testHitPos, finalDirection, finalNeighbour, false), swingHand, hand, finalRot, state, forceSneak);
                        }
                        return true;
                     }
                  }
               }
            }

            // Fallback: If we couldn't find a rotation by scanning pixels, FORCE the ideal yaw/pitch and try again
            if (wantedFacing != null) {
                boolean isDirect = isDirectBlock(block);
                if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
                    isDirect = state.getValue(BlockStateProperties.ATTACH_FACE) != AttachFace.WALL;
                }
                
                float targetYaw = getYawFromDirection(isDirect ? wantedFacing : wantedFacing.getOpposite());
                float targetPitch = getPitchFromDirection(isDirect ? wantedFacing : wantedFacing.getOpposite());

                if (block instanceof TrapDoorBlock && blockHalf != null && finalDirection.getAxis().isVertical()) {
                    targetPitch = blockHalf == Half.TOP ? -90.0f : 90.0f;
                }

                if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
                    AttachFace face = state.getValue(BlockStateProperties.ATTACH_FACE);
                    if (face == AttachFace.FLOOR) targetPitch = 90.0f;
                    else if (face == AttachFace.CEILING) targetPitch = -90.0f;
                    else if (face == AttachFace.WALL) targetPitch = 0.0f;
                } else if (state.getBlock().getStateDefinition().getProperty("face") != null) {
                    Property<?> prop = state.getBlock().getStateDefinition().getProperty("face");
                    String face = state.getValue(prop).toString().toUpperCase();
                    if (face.contains("FLOOR")) targetPitch = 90.0f;
                    else if (face.contains("CEILING")) targetPitch = -90.0f;
                    else if (face.contains("WALL")) targetPitch = 0.0f;
                }

                if (block instanceof DoorBlock && wantedHinge != null) {
                    float offset = (wantedHinge == DoorHingeSide.LEFT) ? 25.0f : -25.0f;
                    if (wantedFacing == Direction.SOUTH || wantedFacing == Direction.WEST) offset = -offset;
                    targetYaw = targetYaw + offset;
                }

                if (block instanceof AnvilBlock) {
                    targetYaw = getYawFromDirection(wantedFacing.getCounterClockWise());
                    targetPitch = 0;
                }

                // 500 yay!
                Rotation finalRot = new Rotation(targetYaw, targetPitch);

                if (clientSide) {
                    Rotations.rotate(finalRot.getYaw(), finalRot.getPitch(), 100, true, () -> place(new BlockHitResult(finalHitPos, finalDirection, finalNeighbour, false), swingHand, hand, finalRot, state, forceSneak));
                } else {
                    place(new BlockHitResult(finalHitPos, finalDirection, finalNeighbour, false), swingHand, hand, finalRot, state, forceSneak);
                }
                return true;
            }

            return false;
         }

         place(new BlockHitResult(finalHitPos, finalDirection, finalNeighbour, false), swingHand, hand, null, state, forceSneak);
         return true;
      }
   }

   public enum PredictionResult {
       SUCCESS,
       SHOULD_SNEAK,
       FAIL_INCORRECT_CONNECTION
   }

   public static PredictionResult predictChestType(BlockPos pos, BlockState required, WorldSchematic schematic) {
       if (!(required.getBlock() instanceof ChestBlock)) return PredictionResult.SUCCESS;
       if (!required.hasProperty(BlockStateProperties.CHEST_TYPE) || !required.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return PredictionResult.SUCCESS;

       ChestType reqType = required.getValue(BlockStateProperties.CHEST_TYPE);
       Direction reqFacing = required.getValue(BlockStateProperties.HORIZONTAL_FACING);

       // 1. Proximity Check: Prioritize nearby unplaced double chests of the same facing
       if (reqType == ChestType.SINGLE && schematic != null) {
           for (int x = -1; x <= 1; x++) {
               for (int z = -1; z <= 1; z++) {
                   if (x == 0 && z == 0) continue;
                   BlockPos neighborPos = pos.offset(x, 0, z);
                   BlockState schematicState = schematic.getBlockState(neighborPos);
                   
                   if (schematicState.getBlock() instanceof ChestBlock && schematicState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && schematicState.getValue(BlockStateProperties.HORIZONTAL_FACING) == reqFacing) {
                       ChestType schematicType = schematicState.hasProperty(BlockStateProperties.CHEST_TYPE) ? schematicState.getValue(BlockStateProperties.CHEST_TYPE) : ChestType.SINGLE;
                       if (schematicType != ChestType.SINGLE) {
                           // There is a double chest in the schematic here. Check if it's placed correctly in the world.
                           BlockState worldState = MeteorClient.mc.level.getBlockState(neighborPos);
                           boolean isPlacedCorrectly = worldState.getBlock() == schematicState.getBlock() && worldState.hasProperty(BlockStateProperties.CHEST_TYPE) && worldState.getValue(BlockStateProperties.CHEST_TYPE) == schematicType;
                           
                           if (!isPlacedCorrectly) {
                               // A double chest part of same facing is nearby but not ready. 
                               // We refuse to place the single chest to avoid blocking it or connecting incorrectly.
                               return PredictionResult.FAIL_INCORRECT_CONNECTION;
                           }
                       }
                   }
               }
           }
       }

       // 2. World Connection Guard: Check if placing this would form an illegal connection
       for (Direction dir : Direction.Plane.HORIZONTAL) {
           BlockPos neighborPos = pos.relative(dir);
           BlockState neighborState = MeteorClient.mc.level.getBlockState(neighborPos);

           if (neighborState.getBlock() == required.getBlock()) {
               if (neighborState.hasProperty(BlockStateProperties.HORIZONTAL_FACING) && neighborState.getValue(BlockStateProperties.HORIZONTAL_FACING) == reqFacing) {
                   ChestType neighborType = neighborState.hasProperty(BlockStateProperties.CHEST_TYPE) ? neighborState.getValue(BlockStateProperties.CHEST_TYPE) : ChestType.SINGLE;

                   if (neighborType == ChestType.SINGLE) {
                       // Neighbor is single and same facing, it WILL try to connect.
                       if (reqType == ChestType.SINGLE) return PredictionResult.SHOULD_SNEAK;

                       Direction expectedPartnerDir = (reqType == ChestType.LEFT) ? reqFacing.getClockWise() : reqFacing.getCounterClockWise();
                       if (dir != expectedPartnerDir) {
                           // Neighbor is NOT our intended partner but would connect anyway
                           return PredictionResult.FAIL_INCORRECT_CONNECTION;
                       }
                   }
               }
           }
       }

       return PredictionResult.SUCCESS;
   }

   private static void place(BlockHitResult blockHitResult, boolean swing, InteractionHand hand, Rotation fakeRotation, BlockState state, boolean forceSneak) {
      if (MeteorClient.mc.player != null && MeteorClient.mc.gameMode != null && MeteorClient.mc.getConnection() != null) {
         float oldYaw = MeteorClient.mc.player.getYRot();
         float oldPitch = MeteorClient.mc.player.getXRot();

         if (fakeRotation != null) {
            MeteorClient.mc.player.setYRot(fakeRotation.getYaw());
            MeteorClient.mc.player.setXRot(fakeRotation.getPitch());
            MeteorClient.mc.player.yHeadRot = fakeRotation.getYaw();
            MeteorClient.mc.player.yBodyRot = fakeRotation.getYaw();
            MeteorClient.mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(fakeRotation.getYaw(), fakeRotation.getPitch(), MeteorClient.mc.player.onGround(), MeteorClient.mc.player.horizontalCollision));
         }

         boolean shouldSneak = forceSneak;
         if (!shouldSneak && state.getBlock() instanceof ChestBlock && state.hasProperty(BlockStateProperties.CHEST_TYPE)) {
             shouldSneak = state.getValue(BlockStateProperties.CHEST_TYPE) == ChestType.SINGLE;
         }
         
         Input currentInput = MeteorClient.mc.player.input.keyPresses;
         Input sneakInput = new Input(currentInput.forward(), currentInput.backward(), currentInput.left(), currentInput.right(), currentInput.jump(), shouldSneak, currentInput.sprint());
         
         if (shouldSneak) isAutoSneaking = true;
         try {
             MeteorClient.mc.getConnection().send(new ServerboundPlayerInputPacket(sneakInput));
             
             isPlacing = true;
             // STANDARD PLACEMENT
             try {
                InteractionResult result = MeteorClient.mc.gameMode.useItemOn(MeteorClient.mc.player, hand, blockHitResult);
                if (result == InteractionResult.SUCCESS || result == InteractionResult.CONSUME) {
                   updateStackingMemory(blockHitResult.getBlockPos(), state);
                   if (swing) {
                      MeteorClient.mc.player.swing(hand);
                   } else {
                      MeteorClient.mc.getConnection().send(new ServerboundSwingPacket(hand));
                   }
                }
             } finally {
                isPlacing = false;
             }
         } finally {
             isAutoSneaking = false;
         }

         MeteorClient.mc.getConnection().send(new ServerboundPlayerInputPacket(currentInput));

         if (fakeRotation != null) {
            MeteorClient.mc.player.setYRot(oldYaw);
            MeteorClient.mc.player.setXRot(oldPitch);
         }
      }
   }

   public static boolean isBlockNormalCube(BlockState state, BlockPos pos) {
      if (state == null || state.isAir() || MeteorClient.mc.level == null) return false;
      Block block = state.getBlock();
      if (block instanceof ScaffoldingBlock || block instanceof ShulkerBoxBlock || block instanceof PointedDripstoneBlock || block instanceof AmethystClusterBlock) return false;
      try {
         return state.isCollisionShapeFullBlock(MeteorClient.mc.level, pos);
      } catch (Exception var3) {
         return false;
      }
   }

   public static boolean isBlockNormalCube(BlockState state) {
       return isBlockNormalCube(state, BlockPos.ZERO);
   }

   public static boolean isStackable(BlockState state) {
       if (state == null) return false;
       return state.hasProperty(BlockStateProperties.SLAB_TYPE) ||
              state.getBlock().getDescriptionId().contains("candle") ||
              state.getBlock().getDescriptionId().contains("sea_pickle") ||
              state.getBlock().getDescriptionId().contains("turtle_egg");
   }

   public static boolean isStackable(Block block) {
       return block instanceof SlabBlock || block.getDescriptionId().contains("candle") || block.getDescriptionId().contains("sea_pickle") || block.getDescriptionId().contains("turtle_egg");
   }

   public static boolean isUnderStacked(BlockState required, BlockState current) {
       if (required.getBlock() != current.getBlock()) return false;
       if (required.hasProperty(BlockStateProperties.SLAB_TYPE) && current.hasProperty(BlockStateProperties.SLAB_TYPE)) {
           return required.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE && current.getValue(BlockStateProperties.SLAB_TYPE) != SlabType.DOUBLE;
       }
       for (Property<?> prop : required.getProperties()) {
           if (prop instanceof IntegerProperty) {
               IntegerProperty intProp = (IntegerProperty) prop;
               String name = prop.getName().toLowerCase();
               
               // Strict Property Matching: ONLY "egg", "candle", or "pickle"
               if (name.contains("egg") || name.contains("candle") || name.contains("pickle")) {
                   for (Property<?> currentProp : current.getProperties()) {
                       if (currentProp.getName().equals(prop.getName()) && currentProp instanceof IntegerProperty) {
                           int reqVal = required.getValue(intProp);
                           int curVal = current.getValue((IntegerProperty) currentProp);
                           
                           // Individual amount tracking (1-4)
                           if (reqVal == 2 && curVal == 1) return true;
                           if (reqVal == 3 && curVal < 3) return true;
                           if (reqVal == 4 && curVal < 4) return true;
                           
                           return reqVal > curVal;
                       }
                   }
               }
           }
       }
       return false;
   }


   public static boolean isBlockInLineOfSight(BlockPos placeAt, BlockState placeAtState) {
      return getVisiblePoint(placeAt, placeAtState) != null;
   }

   public static Vec3 getVisiblePoint(BlockPos placeAt, BlockState placeAtState) {
      return getVisiblePoint(placeAt, placeAtState, true);
   }

   private static Vec3 getVisiblePoint(BlockPos placeAt, BlockState placeAtState, boolean checkOtherHalf) {
      Vec3 playerHead = MeteorClient.mc.player.getEyePosition();
      VoxelShape shape = placeAtState.getShape(MeteorClient.mc.level, placeAt);
      if (shape.isEmpty()) shape = Shapes.block();

      AABB bounds = shape.bounds();

      // Increase point density for Doors/Trapdoors to ensure we hit the physical model
      double[] offsets = (placeAtState.getBlock() instanceof DoorBlock || placeAtState.getBlock() instanceof TrapDoorBlock)
          ? new double[]{0.05, 0.25, 0.5, 0.75, 0.95}
          : new double[]{0.1, 0.5, 0.9};

      for (double xOffset : offsets) {
          for (double yOffset : offsets) {
              for (double zOffset : offsets) {
                  double x = bounds.minX + xOffset * (bounds.maxX - bounds.minX);
                  double y = bounds.minY + yOffset * (bounds.maxY - bounds.minY);
                  double z = bounds.minZ + zOffset * (bounds.maxZ - bounds.minZ);

                  Vec3 targetVec = Vec3.atLowerCornerOf(placeAt).add(x, y, z);
                  ClipContext context = new ClipContext(playerHead, targetVec, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, MeteorClient.mc.player);
                  BlockHitResult bhr = MeteorClient.mc.level.clip(context);

                  if (bhr.getType() == Type.MISS || bhr.getBlockPos().equals(placeAt)) return targetVec;

                  // Structural awareness for Doors: if we hit ANY part of a Door block, it's valid
                  if (placeAtState.getBlock() instanceof DoorBlock) {
                      BlockState hitState = MeteorClient.mc.level.getBlockState(bhr.getBlockPos());
                      if (hitState.getBlock() instanceof DoorBlock) {
                          return targetVec;
                      }
                  }
              }
          }
      }

      // If primary block is not visible, check the other half of the door
      if (checkOtherHalf && placeAtState.getBlock() instanceof DoorBlock) {
          BlockPos otherHalfPos = placeAtState.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? placeAt.relative(Direction.UP) : placeAt.relative(Direction.DOWN);
          BlockState otherHalfState = MeteorClient.mc.level.getBlockState(otherHalfPos);
          if (otherHalfState.getBlock() instanceof DoorBlock) {
              return getVisiblePoint(otherHalfPos, otherHalfState, false);
          }
      }
      return null;
   }

   public static boolean isBlockSameAsPlaceDir(Block block) {
      return block instanceof HopperBlock;
   }

   public static boolean isBlockPlacementOppositeToPlacePos(Block block) {
      if (block.getStateDefinition().getProperty("orientation") != null) return true;
      return !isDirectBlock(block) && (block instanceof ChestBlock || block instanceof FurnaceBlock || block instanceof BarrelBlock || block instanceof EnderChestBlock || block instanceof PistonBaseBlock || block instanceof DispenserBlock || block instanceof DropperBlock || block instanceof DiodeBlock || block instanceof BeehiveBlock || block instanceof SmokerBlock || block instanceof BlastFurnaceBlock || block instanceof StonecutterBlock || block instanceof LecternBlock || block instanceof LoomBlock || block instanceof CarvedPumpkinBlock || block instanceof CampfireBlock || block instanceof ChiseledBookShelfBlock || block instanceof CrafterBlock || block instanceof LightningRodBlock || block instanceof TrapDoorBlock || block instanceof EndRodBlock || block instanceof ButtonBlock);
   }

   public static boolean isBlockSpecialCase(Block block) {
      return block instanceof ObserverBlock || block instanceof AnvilBlock || block instanceof GrindstoneBlock || block instanceof ButtonBlock || block.getDescriptionId().contains("sculk_sensor");
   }

   public static boolean isBlockLikeButton(Block block) {
      return block instanceof ButtonBlock || block instanceof BellBlock || block instanceof GrindstoneBlock || block instanceof TrapDoorBlock;
   }

   public static boolean isBlockCheckingPitchForVerticalDir(Block block) {
      return block instanceof ObserverBlock || block instanceof PistonBaseBlock;
   }


   public static boolean isDirectBlock(Block block) {
       return block instanceof ObserverBlock || block instanceof AmethystClusterBlock || block instanceof HopperBlock || block instanceof ShulkerBoxBlock || block instanceof DoorBlock || block instanceof StairBlock || block instanceof BedBlock || block instanceof FenceGateBlock || block instanceof BellBlock || block instanceof GrindstoneBlock || block.getDescriptionId().contains("sculk_sensor");
   }

   public static boolean isPlayerOrientationDesired(BlockState state, Direction facing, Rotation playerRotation) {
      return isPlayerOrientationDesired(state, facing, playerRotation, 0.1f);
   }

   public static boolean isPlayerOrientationDesired(BlockState state, Direction facing, Rotation playerRotation, float margin) {
      if (facing == null) return true;
      Block block = state.getBlock();

      if (block instanceof EndRodBlock || block instanceof LightningRodBlock || block instanceof AmethystClusterBlock || block instanceof HopperBlock) return true;

      if (block.getStateDefinition().getProperty("axis") != null) return true;

      boolean isDirect = isDirectBlock(block);
      if (state.hasProperty(BlockStateProperties.ATTACH_FACE)) {
          isDirect = state.getValue(BlockStateProperties.ATTACH_FACE) != AttachFace.WALL;
      }

      float targetYaw = getYawFromDirection(isDirect ? facing : facing.getOpposite());
      if (block instanceof AnvilBlock) {
          targetYaw = getYawFromDirection(facing.getCounterClockWise());
      }

      float yawDiff = Math.abs(playerRotation.getYaw() - targetYaw);
      if (yawDiff > 180) yawDiff = 360 - yawDiff;

      if (facing.getAxis().isVertical()) {
          float targetPitch = getPitchFromDirection(isDirect ? facing : facing.getOpposite());
          float pitchDiff = Math.abs(playerRotation.getPitch() - targetPitch);
          return yawDiff < margin && pitchDiff < margin;
      }

      return yawDiff < margin;
   }

   public static float getYawFromDirection(Direction direction) {
       switch (direction) {
           case NORTH: return 180;
           case SOUTH: return 0;
           case WEST: return 90;
           case EAST: return 270;
           default: return 0;
       }
   }

   public static float getPitchFromDirection(Direction direction) {
       switch (direction) {
           case UP: return -90;
           case DOWN: return 90;
           default: return 0;
       }
   }

   public static boolean isFallingBlock(Block block) {
       if (block == Blocks.WATER || block == Blocks.LAVA) return false;
       return block instanceof FallingBlock || block == Blocks.ANVIL || block == Blocks.CHIPPED_ANVIL || block == Blocks.DAMAGED_ANVIL || block == Blocks.SUSPICIOUS_GRAVEL || block == Blocks.SUSPICIOUS_SAND || block.getDescriptionId().contains("concrete_powder");
   }

   public static boolean isVineType(BlockState state) {
       return state.hasProperty(BlockStateProperties.NORTH) && 
              state.hasProperty(BlockStateProperties.SOUTH) && 
              state.hasProperty(BlockStateProperties.EAST) && 
              state.hasProperty(BlockStateProperties.WEST) && 
              state.hasProperty(BlockStateProperties.UP) && 
              state.hasProperty(BlockStateProperties.DOWN);
   }

   private static BlockState calculateNextState(BlockState current, BlockState required) {
       if (required.getBlock() != current.getBlock()) {
           BlockState next = required.getBlock().defaultBlockState();
           if (required.hasProperty(BlockStateProperties.SLAB_TYPE)) {
               next = next.setValue(BlockStateProperties.SLAB_TYPE, required.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE ? SlabType.BOTTOM : required.getValue(BlockStateProperties.SLAB_TYPE));
           }
           return next;
       }
       BlockState next = current;
       if (required.hasProperty(BlockStateProperties.SLAB_TYPE) && current.hasProperty(BlockStateProperties.SLAB_TYPE)) {
           if (required.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) next = current.setValue(BlockStateProperties.SLAB_TYPE, SlabType.DOUBLE);
       }
       for (Property<?> prop : required.getProperties()) {
           if (prop instanceof IntegerProperty) {
               String name = prop.getName().toLowerCase();
               boolean isStrictKeyword = name.contains("egg") || name.contains("candle") || name.contains("pickle");

               if (isStrictKeyword) {
                   for (Property<?> currentProp : current.getProperties()) {
                       if (currentProp.getName().equals(prop.getName()) && currentProp instanceof IntegerProperty) {
                           int currVal = current.getValue((IntegerProperty) currentProp);
                           int reqVal = required.getValue((IntegerProperty) prop);
                           if (currVal < reqVal) next = setIntProperty(next, prop, currVal + 1);
                       }
                   }
               }
           }
       }
       return next;
   }

   private static BlockState setIntProperty(BlockState state, Property<?> prop, int val) {
       for (Object obj : prop.getPossibleValues()) {
           if (obj.toString().equals(String.valueOf(val))) return state.setValue((Property)prop, (Comparable)obj);
       }
       return state;
   }



   private static Vec3[] aabbSideMultipliers(Direction side) {
      switch (side) {
         case UP:
            return new Vec3[]{new Vec3((double)0.5F, (double)1.0F, (double)0.5F), new Vec3(0.1, (double)1.0F, (double)0.5F), new Vec3(0.9, (double)1.0F, (double)0.5F), new Vec3((double)0.5F, (double)1.0F, 0.1), new Vec3((double)0.5F, (double)1.0F, 0.9)};
         case DOWN:
            return new Vec3[]{new Vec3((double)0.5F, (double)0.0F, (double)0.5F), new Vec3(0.1, (double)0.0F, (double)0.5F), new Vec3(0.9, (double)0.0F, (double)0.5F), new Vec3((double)0.5F, (double)0.0F, 0.1), new Vec3((double)0.5F, (double)0.0F, 0.9)};
         case NORTH:
         case SOUTH:
         case EAST:
         case WEST:
            double xMultiplier = side.getStepX() == 0 ? (double)0.5F : (double)(1 + side.getStepX()) / (double)2.0F;
            double zMultiplier = side.getStepZ() == 0 ? (double)0.5F : (double)(1 + side.getStepZ()) / (double)2.0F;

            // Add center-top, center-bottom, and horizontal extremes for hinge/precision support
            return new Vec3[]{
                new Vec3(xMultiplier, 0.20, zMultiplier),
                new Vec3(xMultiplier, 0.80, zMultiplier),
                new Vec3(side.getAxis() == Direction.Axis.X ? xMultiplier : 0.05, 0.5, side.getAxis() == Direction.Axis.Z ? zMultiplier : 0.05),
                new Vec3(side.getAxis() == Direction.Axis.X ? xMultiplier : 0.95, 0.5, side.getAxis() == Direction.Axis.Z ? zMultiplier : 0.95)
            };
         default:
            throw new IllegalStateException();
      }
   }

   public static Direction getHorizontalDirectionFromYaw(float yaw) {
      yaw %= 360.0F;
      if (yaw < 0.0F) {
         yaw += 360.0F;
      }

      if ((!(yaw >= 45.0F) || !(yaw < 135.0F)) && (!(yaw >= -315.0F) || !(yaw < -225.0F))) {
         if ((!(yaw >= 135.0F) || !(yaw < 225.0F)) && (!(yaw >= -225.0F) || !(yaw < -135.0F))) {
            return (!(yaw >= 225.0F) || !(yaw < 315.0F)) && (!(yaw >= -135.0F) || !(yaw < -45.0F)) ? Direction.SOUTH : Direction.EAST;
         } else {
            return Direction.NORTH;
         }
      } else {
         return Direction.WEST;
      }
   }

   public static Direction getVerticalDirectionFromPitch(float pitch) {
      if (pitch > 45.0F) {
         return Direction.DOWN;
      } else {
         return pitch < -45.0F ? Direction.UP : null;
      }
   }

   public static Direction getFacing(BlockState state) {
      for (Property<?> prop : state.getProperties()) {
         if (prop instanceof EnumProperty && (prop.getName().equals("facing") || prop.getName().equals("horizontal_facing") || prop.getName().equals("orientation"))) {
            Object value = state.getValue(prop);
            if (value instanceof Direction) return (Direction) value;
            if (value.toString().toUpperCase().contains("NORTH")) return Direction.NORTH;
            if (value.toString().toUpperCase().contains("SOUTH")) return Direction.SOUTH;
            if (value.toString().toUpperCase().contains("EAST")) return Direction.EAST;
            if (value.toString().toUpperCase().contains("WEST")) return Direction.WEST;
            if (value.toString().toUpperCase().contains("UP")) return Direction.UP;
            if (value.toString().toUpperCase().contains("DOWN")) return Direction.DOWN;
         }
      }
      if (state.hasProperty(BlockStateProperties.AXIS)) {
          return Direction.fromAxisAndDirection(state.getValue(BlockStateProperties.AXIS), AxisDirection.POSITIVE);
      }
      return null;
   }

   public static DoorHingeSide getHinge(WorldSchematic schematic, BlockPos pos, BlockState state) {
       if (state.hasProperty(BlockStateProperties.DOOR_HINGE)) {
           return state.getValue(BlockStateProperties.DOOR_HINGE);
       }
       if (schematic != null) {
           BlockPos otherHalf = state.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? pos.above() : pos.below();
           BlockState otherState = schematic.getBlockState(otherHalf);
           if (otherState.getBlock() instanceof DoorBlock && otherState.hasProperty(BlockStateProperties.DOOR_HINGE)) {
               return otherState.getValue(BlockStateProperties.DOOR_HINGE);
           }
       }
       return DoorHingeSide.LEFT;
   }

   public static DoorHingeSide predictHinge(BlockPos pos, BlockState state) {
       Direction facing = getFacing(state);
       if (facing == null) return DoorHingeSide.LEFT;

       Direction ccw = facing.getCounterClockWise();
       Direction cw = facing.getClockWise();

       BlockPos leftLower = pos.relative(ccw);
       BlockPos leftUpper = leftLower.above();
       BlockPos rightLower = pos.relative(cw);
       BlockPos rightUpper = rightLower.above();

       BlockState leftLowerState = MeteorClient.mc.level.getBlockState(leftLower);
       BlockState leftUpperState = MeteorClient.mc.level.getBlockState(leftUpper);
       BlockState rightLowerState = MeteorClient.mc.level.getBlockState(rightLower);
       BlockState rightUpperState = MeteorClient.mc.level.getBlockState(rightUpper);

       // Step 1: Door pairing logic (Double-door formation)
       // Minecraft pairs hinges to existing doors with high priority
       boolean leftIsDoor = (leftLowerState.getBlock() instanceof DoorBlock) || (leftUpperState.getBlock() instanceof DoorBlock);
       boolean rightIsDoor = (rightLowerState.getBlock() instanceof DoorBlock) || (rightUpperState.getBlock() instanceof DoorBlock);

       if (leftIsDoor && !rightIsDoor) return DoorHingeSide.RIGHT; // Mirror left door
       if (rightIsDoor && !leftIsDoor) return DoorHingeSide.LEFT;  // Mirror right door

       // Step 2: Wall Attachment (Attach hinge to solid blocks)
       int leftBlocks = (isBlockNormalCube(leftLowerState, leftLower) ? 1 : 0) + (isBlockNormalCube(leftUpperState, leftUpper) ? 1 : 0);
       int rightBlocks = (isBlockNormalCube(rightLowerState, rightLower) ? 1 : 0) + (isBlockNormalCube(rightUpperState, rightUpper) ? 1 : 0);

       if (leftBlocks > rightBlocks) return DoorHingeSide.LEFT;
       if (rightBlocks > leftBlocks) return DoorHingeSide.RIGHT;

       // Step 3: Balanced Environment
       // Server will fall back to our precision quadrant click, which always matches schematic
       // 🎉 1000 lines of code milestone! 🎉
       return getHinge(null, pos, state);
   }
}
