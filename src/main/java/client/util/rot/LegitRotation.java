package client.util.rot;

import client.module.combat.Aura;
import java.security.SecureRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Exact port of old l.Legit (method3146 + helpers).
 * No formula simplification.
 */
public class LegitRotation {
   private static final float ROTATION_SPEED = 11.0F;
   private static final float LIMIT_ROTATION_SPEED = 70.0F;
   private static final long RELEASE_HOLD_MS = 300L;
   private static final long RELEASE_SLOWDOWN_MS = 200L;
   private static final float SHAKE_INTENSITY = 0.0F;
   private static final float SHAKE_SPEED = 3.0F;
   private static final float EPSILON = 1.0F;
   private static final float MANUAL_YAW_LIMIT = 12.0F;
   private static final float MANUAL_PITCH_LIMIT = 20.0F;
   private static final float MANUAL_INFLUENCE = 0.55F;
   private static final float DESYNC_SNAP_THRESHOLD = 55.0F;
   private static final float DESYNC_RECOVER_STEP = 18.0F;
   private static final float FINAL_MAX_YAW_STEP = 22.0F;
   private static final float FINAL_MAX_PITCH_STEP = 14.0F;
   private static final float HITBOX_WIDTH_SCALE = 1.0F;
   private static final float HITBOX_HEIGHT_SCALE = 0.93F;
   private static final SecureRandom RANDOM = new SecureRandom();

   private static boolean releaseHoldActive = false;
   private static long releaseHoldUntil = 0L;
   private static boolean releaseSlowdownActive = false;
   private static long releaseSlowdownStart = 0L;
   private static Rotation releaseFromAngle = null;

   private final MinecraftClient mc = MinecraftClient.getInstance();

   public Rotation update(Rotation current, Rotation target, Vec3d aimPoint, Entity entity) {
      Aura aura = Aura.getInstance();
      boolean hasTarget = aura != null && aura.isEnabled() && aura.getTarget() != null && entity != null;
      long now = System.currentTimeMillis();

      if (!hasTarget) {
         if (!releaseHoldActive) {
            releaseHoldActive = true;
            releaseHoldUntil = now + RELEASE_HOLD_MS;
            releaseSlowdownActive = false;
            releaseSlowdownStart = 0L;
            releaseFromAngle = null;
         }

         if (now < releaseHoldUntil) {
            return new Rotation(current.getYaw(), current.getPitch()).quantize();
         } else {
            if (!releaseSlowdownActive) {
               releaseSlowdownActive = true;
               releaseSlowdownStart = now;
               releaseFromAngle = new Rotation(current.getYaw(), current.getPitch());
            }

            float t = MathHelper.clamp((float) (now - releaseSlowdownStart) / (float) RELEASE_SLOWDOWN_MS, 0.0F, 1.0F);
            float eased = ease(t);
            Rotation from = releaseFromAngle != null ? releaseFromAngle : current;
            Rotation to = target != null ? target : current;
            Rotation out = new Rotation(
               MathHelper.lerpAngleDegrees(eased, from.getYaw(), to.getYaw()),
               MathHelper.clamp(MathHelper.lerp(eased, from.getPitch(), to.getPitch()), -89.0F, 90.0F)
            );
            if (t >= 1.0F) {
               releaseSlowdownActive = false;
               releaseSlowdownStart = 0L;
               releaseFromAngle = null;
            }
            return out.quantize();
         }
      } else {
         // active target
         releaseHoldActive = false;
         releaseHoldUntil = 0L;
         releaseSlowdownActive = false;
         releaseSlowdownStart = 0L;
         releaseFromAngle = null;

         Rotation delta = RotationMath.delta(current, target);
         float dYaw = delta.getYaw();
         float dPitch = delta.getPitch();
         float hyp = (float) Math.hypot(dYaw, dPitch);

         float yawCap = Math.min(Math.abs(dYaw), 74.0F + rand(0.0F, 1.0329834F));
         float pitchCap = (float) Math.min((double) Math.abs(dPitch), 32.334);

         Rotation out = new Rotation(current.getYaw(), current.getPitch());
         float eps = 1.0F + rand(0.0F, 0.35F);

         if (hyp > eps) {
            boolean pitchDominant = Math.abs(dPitch) >= pitchCap;
            float limit = pitchDominant ? rand(45.5F, 70.0F) : rand(7.7F, 12.1F);
            float step = Math.min(hyp, limit);
            float factor = step / hyp;
            if (!pitchDominant) {
               factor = ease(factor);
            }
            float newPitch = MathHelper.clamp(current.getPitch() + dPitch * factor, -89.0F, 90.0F);
            out.setPitch(newPitch);
         }

         if (hyp > eps) {
            boolean yawDominant = Math.abs(dYaw) >= yawCap;
            float limit = yawDominant ? rand(45.5F, 70.0F) : rand(7.7F, 12.1F);
            float step = Math.min(hyp, limit);
            float factor = step / hyp;
            if (!yawDominant) {
               factor = ease(factor);
            }
            float newYaw = current.getYaw() + dYaw * factor;
            out.setYaw(newYaw);
         }

         boolean desynced = applyManualInfluence(out, current);
         if (!desynced) {
            clampToHitbox(out, entity);
            // method3154 was empty in original
            clampToHitbox(out, entity);
         }

         applyFinalStepLimit(out, current);
         return out.quantize();
      }
   }

   /** Old method3152 – manual mouse influence / desync recover. */
   private boolean applyManualInfluence(Rotation out, Rotation previous) {
      if (mc.player == null) return false;

      float playerDeltaYaw = MathHelper.wrapDegrees(mc.player.getYaw() - previous.getYaw());
      if (Math.abs(playerDeltaYaw) > DESYNC_SNAP_THRESHOLD) {
         out.setYaw(previous.getYaw() + MathHelper.clamp(playerDeltaYaw, -DESYNC_RECOVER_STEP, DESYNC_RECOVER_STEP));
         out.setPitch(MathHelper.lerp(0.75F, out.getPitch(), mc.player.getPitch()));
         out.setPitch(MathHelper.clamp(out.getPitch(), -89.0F, 89.0F));
         return true;
      } else {
         float playerDeltaPitch = mc.player.getPitch() - previous.getPitch();
         float yawInfl = MathHelper.clamp(playerDeltaYaw, -MANUAL_YAW_LIMIT, MANUAL_YAW_LIMIT);
         float pitchInfl = MathHelper.clamp(playerDeltaPitch, -MANUAL_PITCH_LIMIT, MANUAL_PITCH_LIMIT);
         out.setYaw(out.getYaw() + yawInfl * MANUAL_INFLUENCE);
         out.setPitch(out.getPitch() + pitchInfl * MANUAL_INFLUENCE);
         out.setPitch(MathHelper.clamp(out.getPitch(), -89.0F, 89.0F));
         return false;
      }
   }

   /** Old method3153 – clamp rotation into target hitbox angular size. */
   private void clampToHitbox(Rotation rot, Entity entity) {
      if (mc.player == null || entity == null) return;

      Vec3d eye = mc.player.getEyePos();
      Vec3d center = entity.getBoundingBox().getCenter();
      Vec3d diff = center.subtract(eye);
      double horiz = Math.max(Math.hypot(diff.x, diff.z), 0.35);

      Rotation ideal = RotationMath.fromVec(diff);
      float halfW = Math.max(entity.getWidth() * 0.5F * HITBOX_WIDTH_SCALE, 0.12F);
      float halfH = Math.max(entity.getHeight() * 0.5F * HITBOX_HEIGHT_SCALE, 0.25F);
      float maxYawOff = (float) Math.toDegrees(Math.atan(halfW / horiz));
      float maxPitchOff = (float) Math.toDegrees(Math.atan(halfH / horiz));

      float dy = MathHelper.wrapDegrees(rot.getYaw() - ideal.getYaw());
      float dp = rot.getPitch() - ideal.getPitch();
      dy = MathHelper.clamp(dy, -maxYawOff, maxYawOff);
      dp = MathHelper.clamp(dp, -maxPitchOff, maxPitchOff);

      rot.setYaw(MathHelper.wrapDegrees(ideal.getYaw() + dy));
      rot.setPitch(MathHelper.clamp(ideal.getPitch() + dp, -89.0F, 89.0F));
   }

   /** Old method3155 – final max step from previous. */
   private void applyFinalStepLimit(Rotation out, Rotation previous) {
      float dy = MathHelper.clamp(MathHelper.wrapDegrees(out.getYaw() - previous.getYaw()), -FINAL_MAX_YAW_STEP, FINAL_MAX_YAW_STEP);
      float dp = MathHelper.clamp(out.getPitch() - previous.getPitch(), -FINAL_MAX_PITCH_STEP, FINAL_MAX_PITCH_STEP);
      out.setYaw(MathHelper.wrapDegrees(previous.getYaw() + dy));
      out.setPitch(MathHelper.clamp(previous.getPitch() + dp, -89.0F, 89.0F));
   }

   /** Old method3156 */
   private float ease(float t) {
      return t * (0.5F + 0.5F * t);
   }

   /** Old method3157 */
   private float rand(float min, float max) {
      return MathHelper.lerp(RANDOM.nextFloat(), min, max);
   }
}
