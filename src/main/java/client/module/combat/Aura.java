package client.module.combat;

import client.module.Category;
import client.module.Module;
import client.network.PacketEvent;
import client.render.HudRenderContext;
import client.setting.BooleanSetting;
import client.setting.ListSetting;
import client.setting.MultilistSetting;
import client.setting.SliderSetting;
import client.util.TargetSelector;
import client.util.rot.FunTimeRotation;
import client.util.rot.LegitRotation;
import client.util.rot.Rotation;
import client.util.rot.RotationMath;
import client.util.rot.SpookyTimeRotation;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Aura with EXACT rotation ports from old Releon (Legit / SpookyTime / FunTimeSnap2).
 * No formula simplification.
 */
public class Aura extends Module {

   private final ListSetting aimMode;
   private final MultilistSetting targetType;
   private final SliderSetting attackRange;
   private final SliderSetting lookRange;
   private final SliderSetting snapHold;
   private final SliderSetting snapSpeed;
   private final MultilistSetting attackSetting;
   private final SliderSetting hitChance;
   private final BooleanSetting onlySword;
   private final ListSetting correctionType;
   private final BooleanSetting smartCrits;
   private final BooleanSetting tpsSync;
   private final BooleanSetting noAttack;

   private LivingEntity target;
   private LivingEntity lastTarget;
   private long lastAttackMs;
   private Rotation currentRot = new Rotation(0, 0);
   private final AttackTimer attackTimer = new AttackTimer();
   private final LegitRotation legitRotation = new LegitRotation();
   private final FovCircleRenderer fovCircleRenderer = new FovCircleRenderer();

   private static Aura instance;

   public static Aura getInstance() {
      return instance;
   }

   public Aura() {
      super("Aura", Category.COMBAT);
      instance = this;

      this.aimMode = new ListSetting(
         "", "", Arrays.asList("Legit", "SpookyTime", "FunTime", "Snap", "Linear"),
         List.of("Legit"), false
      );
      this.aimMode.setName("Наводка");

      this.targetType = new MultilistSetting(
         "", "",
         Arrays.asList("Players", "Naked Players", "Mobs", "Animals", "Friends", "Armor Stand"),
         List.of("Players", "Mobs", "Animals")
      );
      this.targetType.setName("Тип таргета");

      this.attackRange = new SliderSetting("", "", 3.0, 1.0, 6.0, 0.1);
      this.attackRange.setName("Дистанция удара");

      this.lookRange = new SliderSetting("", "", 1.5, 0.0, 3.0, 0.1);
      this.lookRange.setName("Доп. дистанция поиска");

      this.snapHold = new SliderSetting("", "", 50.0, 1.0, 300.0, 1.0);
      this.snapHold.setName("Snap Hold");
      this.snapHold.setVisibleWhen(() -> "Snap".equals(this.aimMode.getString2()));

      this.snapSpeed = new SliderSetting("", "", 50.0, 15.0, 180.0, 1.0);
      this.snapSpeed.setName("Snap Speed");
      this.snapSpeed.setVisibleWhen(() -> "Snap".equals(this.aimMode.getString2()));

      this.attackSetting = new MultilistSetting(
         "", "",
         Arrays.asList("Only Critical", "Break Shield", "UnPress Shield", "No Attack When Eat", "Ignore The Walls", "Hit Chance"),
         List.of("Only Critical")
      );
      this.attackSetting.setName("Настройки");

      this.hitChance = new SliderSetting("", "", 100.0, 1.0, 100.0, 1.0);
      this.hitChance.setName("Шанс удара %");
      this.hitChance.setVisibleWhen(() -> this.attackSetting.getList().contains("Hit Chance"));

      this.onlySword = new BooleanSetting("", "", true);
      this.onlySword.setName("Только с мечом");

      this.correctionType = new ListSetting(
         "", "", Arrays.asList("Free", "Focused", "Target", "Not visible"),
         List.of("Free"), false
      );
      this.correctionType.setName("Коррекция движения");

      this.smartCrits = new BooleanSetting("", "", false);
      this.smartCrits.setName("Удары на земле");
      this.smartCrits.setVisibleWhen(() -> this.attackSetting.getList().contains("Only Critical"));

      this.tpsSync = new BooleanSetting("", "", true);
      this.tpsSync.setName("Синхронизация с TPS");

      this.noAttack = new BooleanSetting("", "", false);
      this.noAttack.setName("Не атаковать");

      this.addSettings(
         this.aimMode, this.correctionType, this.targetType,
         this.attackRange, this.lookRange, this.snapHold, this.snapSpeed,
         this.attackSetting, this.hitChance, this.onlySword,
         this.smartCrits, this.tpsSync, this.noAttack
      );
   }

   public AttackTimer getAttackTimer() {
      return this.attackTimer;
   }

   public float getAttackRangeValue() {
      return this.attackRange.getValueAsFloat();
   }

   public float getLookRangeValue() {
      return this.lookRange.getValueAsFloat();
   }

   public LivingEntity getTarget() {
      return this.target;
   }

   public LivingEntity getLastTarget() {
      return this.lastTarget;
   }

   @Override
   public void onEnable() {
      this.target = null;
      this.lastTarget = null;
      this.attackTimer.reset();
      if (mc.player != null) {
         this.currentRot = new Rotation(mc.player.getYaw(), mc.player.getPitch());
      }
   }

   @Override
   public void onDisable() {
      this.target = null;
      this.lastTarget = null;
   }

   @Override
   public void onTick() {
      if (mc.player == null || mc.world == null || !this.isEnabled()) return;

      LivingEntity prev = this.target;
      this.target = this.findTarget();

      if (this.target != null) {
         this.lastTarget = this.target;
         this.rotateExact();
         if (!this.noAttack.getValue()) {
            this.tryAttack();
         }
      } else if (prev != null) {
         this.rotateExact();
      }
   }

   private LivingEntity findTarget() {
      float range = this.attackRange.getValueAsFloat() + this.lookRange.getValueAsFloat();
      boolean ignoreWalls = this.attackSetting.getList().contains("Ignore The Walls");

      try {
         @SuppressWarnings("unchecked")
         java.util.stream.Stream<Object> stream =
            (java.util.stream.Stream<Object>) (java.util.stream.Stream<?>) TargetSelector.getStreamByDoubleBooleanDouble(range, ignoreWalls, range);

         LivingEntity best = null;
         double bestDist = Double.MAX_VALUE;
         for (Object o : stream.toList()) {
            if (!(o instanceof LivingEntity e)) continue;
            if (!this.isValidTarget(e)) continue;
            double d = mc.player.distanceTo(e);
            if (d < bestDist) {
               bestDist = d;
               best = e;
            }
         }
         return best;
      } catch (Throwable t) {
         return null;
      }
   }

   private boolean isValidTarget(LivingEntity e) {
      if (e == null || !e.isAlive() || e == mc.player) return false;
      if (e instanceof ArmorStandEntity && !this.targetType.getList().contains("Armor Stand")) return false;

      if (e instanceof PlayerEntity p) {
         if (this.isFriend(p) && !this.targetType.getList().contains("Friends")) return false;
         boolean wantPlayers = this.targetType.getList().contains("Players");
         boolean wantNaked = this.targetType.getList().contains("Naked Players");
         if (!wantPlayers && !wantNaked) return false;
         if (wantNaked && !wantPlayers) {
            int armor = 0;
            for (var stack : p.getArmorItems()) if (!stack.isEmpty()) armor++;
            if (armor > 0) return false;
         }
         return true;
      }
      if (e instanceof AnimalEntity) return this.targetType.getList().contains("Animals");
      if (e instanceof MobEntity) return this.targetType.getList().contains("Mobs");
      return false;
   }

   private void rotateExact() {
      if (mc.player == null) return;

      Rotation current = this.currentRot != null
         ? this.currentRot
         : new Rotation(mc.player.getYaw(), mc.player.getPitch());

      Rotation targetRot;
      Vec3d aimPoint;
      Entity entity = this.target;

      if (this.target != null) {
         aimPoint = getAimPoint(this.target);
         targetRot = Rotation.fromVec(mc.player.getEyePos(), aimPoint);
      } else {
         aimPoint = null;
         targetRot = new Rotation(mc.player.getYaw(), mc.player.getPitch());
      }

      String mode = this.aimMode.getString2();
      Rotation result;

      switch (mode) {
         case "Legit" -> result = this.legitRotation.update(current, targetRot, aimPoint, entity);
         case "SpookyTime" -> result = SpookyTimeRotation.INSTANCE.update(current, targetRot, aimPoint, entity);
         case "FunTime" -> result = FunTimeRotation.INSTANCE.update(current, targetRot, aimPoint, entity);
         case "Snap" -> result = snapRotate(current, targetRot);
         default -> result = linearRotate(current, targetRot);
      }

      this.currentRot = result;

      if (!"Not visible".equals(this.correctionType.getString2())) {
         mc.player.setYaw(result.getYaw());
         mc.player.setPitch(result.getPitch());
      }
   }

   private Rotation snapRotate(Rotation current, Rotation target) {
      float speed = this.snapSpeed.getValueAsFloat() / 100.0F;
      float dy = MathHelper.wrapDegrees(target.getYaw() - current.getYaw()) * speed;
      float dp = (target.getPitch() - current.getPitch()) * speed;
      return new Rotation(
         MathHelper.wrapDegrees(current.getYaw() + dy),
         MathHelper.clamp(current.getPitch() + dp, -89.0F, 89.0F)
      );
   }

   private Rotation linearRotate(Rotation current, Rotation target) {
      Rotation delta = RotationMath.delta(current, target);
      float hyp = Math.max((float) Math.hypot(delta.getYaw(), delta.getPitch()), 1e-4F);
      float max = 60.0F;
      float yawStep = Math.abs(delta.getYaw() / hyp) * max;
      float pitchStep = Math.abs(delta.getPitch() / hyp) * max;
      return new Rotation(
         MathHelper.wrapDegrees(current.getYaw() + MathHelper.clamp(delta.getYaw(), -yawStep, yawStep)),
         MathHelper.clamp(current.getPitch() + MathHelper.clamp(delta.getPitch(), -pitchStep, pitchStep), -89.0F, 89.0F)
      );
   }

   private Vec3d getAimPoint(LivingEntity e) {
      Box box = e.getBoundingBox();
      double y = box.minY + (box.maxY - box.minY) * 0.55;
      return new Vec3d(e.getX(), y, e.getZ());
   }

   private void tryAttack() {
      if (this.target == null || mc.player == null || mc.interactionManager == null) return;
      if (mc.player.distanceTo(this.target) > this.attackRange.getValueAsFloat()) return;

      if (this.onlySword.getValue()) {
         Item item = mc.player.getMainHandStack().getItem();
         if (!(item instanceof SwordItem) && !(item instanceof AxeItem)) return;
      }
      if (this.attackSetting.getList().contains("No Attack When Eat") && mc.player.isUsingItem()) return;
      if (this.attackSetting.getList().contains("Hit Chance")) {
         if (ThreadLocalRandom.current().nextFloat() * 100.0F > this.hitChance.getValueAsFloat()) return;
      }

      boolean onlyCrit = this.attackSetting.getList().contains("Only Critical");
      if (onlyCrit) {
         if (this.smartCrits.getValue() && mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) return;
         if (mc.player.isOnGround() && mc.player.fallDistance < 0.1F) return;
      }

      float cd = mc.player.getAttackCooldownProgress(0.5F);
      if (cd < 0.9F) return;

      long now = System.currentTimeMillis();
      if (now - this.lastAttackMs < 40) return;

      mc.interactionManager.attackEntity(mc.player, this.target);
      mc.player.swingHand(Hand.MAIN_HAND);
      this.lastAttackMs = now;
      this.attackTimer.onHit();
   }

   @Override
   public void onHudRenderContext(HudRenderContext ctx) {
      if (!this.isEnabled() || !"Legit".equals(this.aimMode.getString2())) return;
      if (mc.player == null || !mc.options.getPerspective().isFirstPerson()) return;
      this.fovCircleRenderer.tick();
   }

   @Override
   public void onPacketEvent(PacketEvent event) {
      if (!this.isEnabled()) return;
      if (event.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 30) {
         Entity e = packet.getEntity(mc.world);
         if (e != null && e == this.target && this.attackSetting.getList().contains("Break Shield")) {
            // shield break notify
         }
      }
   }

   public static class AttackTimer {
      private int hitCount = 0;
      private long lastHitMs = 0L;

      public void reset() {
         this.hitCount = 0;
         this.lastHitMs = 0L;
      }

      public void onHit() {
         this.hitCount++;
         this.lastHitMs = System.currentTimeMillis();
      }

      public int getHitCount() {
         return this.hitCount;
      }

      public boolean canAttackWindow(int windowId) {
         long sinceHit = System.currentTimeMillis() - this.lastHitMs;
         return switch (windowId) {
            case 0 -> sinceHit < 50;
            case 1 -> sinceHit < 120;
            case 2 -> sinceHit < 200;
            case 5 -> sinceHit < 400;
            default -> false;
         };
      }

      public boolean passedMs(double ms) {
         return (System.currentTimeMillis() - this.lastHitMs) >= ms;
      }
   }

   public class FovCircleRenderer {
      private float currentScale = 1.0F;
      private float targetScale = 1.0F;
      private float lastRadius = 0.0F;

      public void tick() {
         if (mc.player == null) return;
         double fov = MathHelper.clamp(mc.options.getFov().getValue().intValue(), 30, 110);
         float base = (float) MathHelper.lerp((fov - 30.0) / 80.0, 106.5, 65.0);
         float scaleFov = (float) (450.0 / fov);
         float radius = base * scaleFov;
         this.targetScale = mc.player.isSprinting() ? 0.9F : 1.0F;
         this.currentScale += (this.targetScale - this.currentScale) * 0.15F;
         this.lastRadius = radius * this.currentScale;
      }

      public float getLastRadius() {
         return this.lastRadius;
      }
   }
}
