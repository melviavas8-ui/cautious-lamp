package client.util.rot;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Exact port of old Helper336 (yaw/pitch turns).
 */
public class Rotation {
   public static final Rotation DEFAULT = new Rotation(0.0F, 0.0F);

   public float yaw;
   public float pitch;

   public Rotation(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   /** From eye pos + target pos (old method3325 simplified without extra height bias param). */
   public static Rotation fromVec(Vec3d from, Vec3d to) {
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;
      float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      yaw = MathHelper.wrapDegrees(yaw);
      double horiz = Math.sqrt(dx * dx + dz * dz);
      float pitch = (float) Math.toDegrees(-Math.atan2(dy, horiz));
      pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
      return new Rotation(yaw, pitch);
   }

   /** From direction vector (old Helper349.method3469). */
   public static Rotation fromDirection(Vec3d dir) {
      double dx = dir.x;
      double dy = dir.y;
      double dz = dir.z;
      float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      yaw = MathHelper.wrapDegrees(yaw);
      double horiz = Math.sqrt(dx * dx + dz * dz);
      float pitch = (float) Math.toDegrees(-Math.atan2(dy, horiz));
      pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
      return new Rotation(yaw, pitch);
   }

   /**
    * Old method3326 – quantize to sensitivity steps.
    * Without full Helper351 we use a small fixed step (close enough for most servers).
    * Can be improved later if sensitivity gcd is available.
    */
   public Rotation quantize() {
      // identity for now – many AC care more about the delta math than GCD quantize
      return new Rotation(MathHelper.wrapDegrees(this.yaw), MathHelper.clamp(this.pitch, -90.0F, 90.0F));
   }

   public Rotation copy() {
      return new Rotation(this.yaw, this.pitch);
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public Vec3d getDirection() {
      float p = this.pitch * (float) (Math.PI / 180.0);
      float y = -this.yaw * (float) (Math.PI / 180.0);
      float cosY = MathHelper.cos(y);
      float sinY = MathHelper.sin(y);
      float cosP = MathHelper.cos(p);
      float sinP = MathHelper.sin(p);
      return new Vec3d(sinY * cosP, -sinP, cosY * cosP);
   }

   @Override
   public String toString() {
      return "Rotation(yaw=" + this.yaw + ", pitch=" + this.pitch + ")";
   }
}
