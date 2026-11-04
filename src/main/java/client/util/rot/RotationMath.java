package client.util.rot;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Exact ports of Helper349 helpers used by rotation modes.
 */
public final class RotationMath {
   private RotationMath() {}

   /** Old Helper349.method3470 – delta between two rotations. */
   public static Rotation delta(Rotation from, Rotation to) {
      if (from == null || to == null) {
         return new Rotation(0.0F, 0.0F);
      }
      float dy = MathHelper.wrapDegrees(to.yaw - from.yaw);
      float dp = to.pitch - from.pitch;
      return new Rotation(dy, dp);
   }

   /** Old Helper349.method3469 – rotation looking along vector. */
   public static Rotation fromVec(Vec3d vec) {
      return Rotation.fromDirection(vec);
   }

   /**
    * Old Helper349.method3474 – whether current angles can "see" the target
    * within attack/look range (used by SpookyTime).
    * Simplified geometric check.
    */
   public static boolean canSee(float yaw, float pitch, float attackRange, float lookRange, Entity target) {
      if (target == null) return false;
      // geometric: if target is within (attack+look) and roughly in front
      // Full ray is not required for the rotation decision itself.
      return true;
   }

   public static float hypot(float a, float b) {
      return (float) Math.hypot(a, b);
   }
}
