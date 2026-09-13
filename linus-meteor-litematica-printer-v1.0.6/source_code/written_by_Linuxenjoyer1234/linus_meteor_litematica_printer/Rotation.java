package written_by_Linuxenjoyer1234.linus_meteor_litematica_printer;

public class Rotation {
   private final float yaw;
   private final float pitch;

   public Rotation(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
      if (Float.isInfinite(yaw) || Float.isNaN(yaw) || Float.isInfinite(pitch) || Float.isNaN(pitch)) {
         throw new IllegalStateException(yaw + " " + pitch);
      }
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public Rotation add(Rotation other) {
      return new Rotation(this.yaw + other.yaw, this.pitch + other.pitch);
   }

   public Rotation subtract(Rotation other) {
      return new Rotation(this.yaw - other.yaw, this.pitch - other.pitch);
   }

   public Rotation clamp() {
      return new Rotation(this.yaw, clampPitch(this.pitch));
   }

   public Rotation normalize() {
      return new Rotation(normalizeYaw(this.yaw), this.pitch);
   }

   public Rotation normalizeAndClamp() {
      return new Rotation(normalizeYaw(this.yaw), clampPitch(this.pitch));
   }

   public Rotation withPitch(float pitch) {
      return new Rotation(this.yaw, pitch);
   }

   public boolean isReallyCloseTo(Rotation other) {
      return this.yawIsReallyClose(other) && (double)Math.abs(this.pitch - other.pitch) < 1.5;
   }

   public boolean yawIsReallyClose(Rotation other) {
      float yawDiff = Math.abs(normalizeYaw(this.yaw) - normalizeYaw(other.yaw));
      return (double)yawDiff < 1.5 || (double)yawDiff > 358.5;
   }

   public static float clampPitch(float pitch) {
      return Math.max(-90.0F, Math.min(90.0F, pitch));
   }

   public static float normalizeYaw(float yaw) {
      float newYaw = yaw % 360.0F;
      if (newYaw < -180.0F) {
         newYaw += 360.0F;
      }

      if (newYaw > 180.0F) {
         newYaw -= 360.0F;
      }

      return newYaw;
   }

   public String toString() {
      return "Yaw: " + this.yaw + ", Pitch: " + this.pitch;
   }
}
