package client.util.rot;

import client.module.combat.Aura;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Exact port of old l.SpookyTime.
 * Attack-timer dependent limits (canCrit / canAttack style) are driven by Aura.AttackTimer.
 */
public class SpookyTimeRotation {
   public static final SpookyTimeRotation INSTANCE = new SpookyTimeRotation();

   private static final long SMOOTHBACK_DURATION_MS = 100L;
   private long smoothbackShakeStartMs = -1L;
   private Rotation releaseFromAngle;
   private Rotation releaseToAngle;

   private final MinecraftClient mc = MinecraftClient.getInstance();

   public Rotation update(Rotation current, Rotation target, Vec3d aimPoint, Entity entity) {
      Aura aura = Aura.getInstance();
      if (aura == null || mc.player == null) {
         return current;
      }

      Aura.AttackTimer timer = aura.getAttackTimer();

      if (aura.isEnabled() && aura.getTarget() != null && entity != null && timer != null) {
         this.smoothbackShakeStartMs = -1L;
         this.releaseFromAngle = null;
         this.releaseToAngle = null;

         Rotation delta = RotationMath.delta(current, target);
         float hyp = Math.max((float) Math.hypot(Math.abs(delta.getYaw()), Math.abs(delta.getPitch())), 1.0E-4F);

         // old: var25 = method3283(cfg, 2), var26 = method3283(cfg, 0), var27 = method3283(cfg, 5)
         boolean flag2 = timer.canAttackWindow(2);
         boolean flag0 = timer.canAttackWindow(0);
         boolean flag5 = timer.canAttackWindow(5);
         boolean inAir = !mc.player.isOnGround();

         boolean canSee = RotationMath.canSee(
            current.getYaw(), current.getPitch(),
            aura.getAttackRangeValue(), aura.getLookRangeValue(),
            aura.getTarget()
         );

         float yawLimit = flag0 ? 30.0F : (inAir ? rand(13, 35) : rand(15, 33));
         float pitchLimit = flag0 ? 25.0F : (inAir ? rand(12, 25) : rand(13, 21));

         float yawStep = Math.abs(delta.getYaw() / hyp) * yawLimit;
         float pitchStep = Math.abs(delta.getPitch() / hyp) * pitchLimit;

         // old noise terms were 0 when conditions matched
         float noiseY = flag5 ? (!canSee && flag2 ? 0 : rand(0, 0)) : 0.0F;
         float noiseP = flag5 ? (!canSee && flag2 ? 0 : rand(0, 0)) : 0.0F;

         return new Rotation(
            current.getYaw() + MathHelper.clamp(delta.getYaw(), -yawStep, yawStep) + noiseY,
            current.getPitch() + MathHelper.clamp(delta.getPitch(), -pitchStep, pitchStep) + noiseP
         );
      } else {
         // smooth back to real camera
         if (aura.isEnabled() && aura.getTarget() != null) {
            this.smoothbackShakeStartMs = -1L;
            this.releaseFromAngle = null;
            this.releaseToAngle = null;
         } else if (this.smoothbackShakeStartMs < 0L) {
            this.smoothbackShakeStartMs = System.currentTimeMillis();
            this.releaseFromAngle = new Rotation(current.getYaw(), current.getPitch());
            this.releaseToAngle = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         } else if (this.releaseToAngle != null) {
            this.releaseToAngle.setYaw(mc.player.getYaw());
            this.releaseToAngle.setPitch(mc.player.getPitch());
         }

         Rotation to = this.releaseToAngle != null
            ? this.releaseToAngle
            : new Rotation(mc.player.getYaw(), mc.player.getPitch());

         Rotation delta = RotationMath.delta(current, to);
         float hyp = Math.max((float) Math.hypot(Math.abs(delta.getYaw()), Math.abs(delta.getPitch())), 1.0E-4F);

         float t = this.smoothbackShakeStartMs >= 0L
            ? MathHelper.clamp((float) (System.currentTimeMillis() - this.smoothbackShakeStartMs) / (float) SMOOTHBACK_DURATION_MS, 0.0F, 1.0F)
            : 1.0F;
         float eased = 1.0F - (float) Math.pow(1.0F - t, 3.0);

         float yawLim = MathHelper.lerp(eased, 18.0F, 35.0F);
         float pitchLim = MathHelper.lerp(eased, 16.0F, 28.0F);
         float yawStep = Math.abs(delta.getYaw() / hyp) * yawLim;
         float pitchStep = Math.abs(delta.getPitch() / hyp) * pitchLim;
         float lerpFactor = MathHelper.lerp(eased, 0.28F, 0.35F);

         Rotation out = new Rotation(
            MathHelper.lerp(lerpFactor, current.getYaw(), current.getYaw() + MathHelper.clamp(delta.getYaw(), -yawStep, yawStep)),
            MathHelper.clamp(
               MathHelper.lerp(lerpFactor, current.getPitch(), current.getPitch() + MathHelper.clamp(delta.getPitch(), -pitchStep, pitchStep)),
               -90.0F, 90.0F
            )
         );

         if (this.smoothbackShakeStartMs >= 0L && this.releaseFromAngle != null) {
            out.setYaw(MathHelper.lerpAngleDegrees(eased, this.releaseFromAngle.getYaw(), out.getYaw()));
            out.setPitch(MathHelper.lerp(eased, this.releaseFromAngle.getPitch(), out.getPitch()));
            if (t >= 1.0F) {
               this.releaseFromAngle = null;
               this.releaseToAngle = null;
            }
         }
         return out;
      }
   }

   private float rand(int min, int max) {
      return min + (float) (Math.random() * (max - min));
   }
}
