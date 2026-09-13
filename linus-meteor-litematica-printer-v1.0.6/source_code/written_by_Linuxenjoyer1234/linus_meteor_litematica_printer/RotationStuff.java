package written_by_Linuxenjoyer1234.linus_meteor_litematica_printer;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class RotationStuff {
   public static final double DEG_TO_RAD = (Math.PI / 180D);
   public static final float DEG_TO_RAD_F = ((float)Math.PI / 180F);
   public static final double RAD_TO_DEG = (180D / Math.PI);
   public static final float RAD_TO_DEG_F = 57.29578F;

   public static Rotation calcRotationFromVec3d(Vec3 orig, Vec3 dest, Rotation current) {
      return wrapAnglesToRelative(current, calcRotationFromVec3d(orig, dest));
   }

   private static Rotation calcRotationFromVec3d(Vec3 orig, Vec3 dest) {
      double dx = dest.x - orig.x;
      double dy = dest.y - orig.y;
      double dz = dest.z - orig.z;
      
      // Minecraft standard: yaw = atan2(-dx, dz), pitch = atan2(-dy, sqrt(dx*dx + dz*dz))
      double yaw = Math.atan2(-dx, dz) * RAD_TO_DEG;
      double dist = Math.sqrt(dx * dx + dz * dz);
      double pitch = Math.atan2(-dy, dist) * RAD_TO_DEG;
      
      return new Rotation((float) yaw, (float) pitch);
   }

   public static Rotation wrapAnglesToRelative(Rotation current, Rotation target) {
      return current.yawIsReallyClose(target) ? new Rotation(current.getYaw(), target.getPitch()) : target.subtract(current).normalize().add(current);
   }

   public static Vec3 calcLookDirectionFromRotation(Rotation rotation) {
      float f = rotation.getPitch() * DEG_TO_RAD_F;
      float g = -rotation.getYaw() * DEG_TO_RAD_F;
      float h = Mth.cos(g);
      float i = Mth.sin(g);
      float j = Mth.cos(f);
      float k = Mth.sin(f);
      
      // Standard Minecraft coordinate mapping:
      // Yaw 0 = South (+Z), Yaw 90 = West (-X), Yaw 180 = North (-Z), Yaw 270 = East (+X)
      // Vector formula: (sin(-yaw)*cos(pitch), -sin(pitch), cos(-yaw)*cos(pitch))
      return new Vec3((double)(i * j), (double)(-k), (double)(h * j));
   }

   public static HitResult rayTraceTowards(LocalPlayer entity, Rotation rotation, double blockReachDistance) {
      Vec3 start = entity.getEyePosition(1.0F);
      Vec3 direction = calcLookDirectionFromRotation(rotation);
      Vec3 end = start.add(direction.x * blockReachDistance, direction.y * blockReachDistance, direction.z * blockReachDistance);
      return entity.level().clip(new ClipContext(start, end, Block.OUTLINE, Fluid.NONE, entity));
   }
}
