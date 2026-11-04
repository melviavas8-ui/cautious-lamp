package client.util.rot;

import client.module.combat.Aura;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Exact port of old l.FunTimeSnap2.
 */
public class FunTimeRotation {
   public static final FunTimeRotation INSTANCE = new FunTimeRotation();

   private final MinecraftClient mc = MinecraftClient.getInstance();

   public Rotation update(Rotation current, Rotation target, Vec3d aimPoint, Entity entity) {
      Aura aura = Aura.getInstance();
      if (aura == null || mc.player == null) {
         return current;
      }

      Aura.AttackTimer timer = aura.getAttackTimer();
      Rotation delta = RotationMath.delta(current, target);
      float dYaw = delta.getYaw();
      float dPitch = delta.getPitch();
      float hyp = Math.max((float) Math.hypot(dYaw, dPitch), 1.0E-4F);

      float yawLimit = Math.abs(dYaw / hyp) * 40.0F;
      float pitchLimit = Math.abs(dPitch / hyp) * 0.0F; // original had * 0.0F

      if (timer != null && timer.canAttackWindow(1)) {
         yawLimit = Math.abs(dYaw / hyp) * 65.0F;
      }

      float oscY = osc(0.003F, 1.3F, true);
      float oscP = osc(0.003F, 0.8F, true);

      if (timer != null && !timer.passedMs(300.0)) {
         int hits = timer.getHitCount();
         if (hits % 2 == 0 && hits > 0) {
            if (!timer.passedMs(250.0)) {
               oscY = osc(0.003F, -12.3F, true);
               oscP = osc(0.003F, -5.8F, true);
            }
         } else {
            oscY = osc(0.003F, 12.3F, true);
            oscP = osc(0.003F, 5.8F, true);
         }

         if (hits % 30 == 0 && hits > 0) {
            mc.player.swingHand(Hand.MAIN_HAND);
         }
      }

      return new Rotation(
         current.getYaw() + MathHelper.clamp(dYaw, -yawLimit, yawLimit) + oscY,
         MathHelper.clamp(mc.player.getPitch(), -90.0F, 90.0F) + oscP
      );
   }

   /** Old method3550 */
   private float osc(float speed, float amp, boolean cos) {
      double t = (float) System.currentTimeMillis() * speed;
      return (float) ((cos ? Math.cos(t) : Math.sin(t)) * amp);
   }
}
