package kome.hatsunemiku.addon.modules;

import kome.hatsunemiku.addon.MikuTester;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.Random;

public class CrosshairAttack extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum HandMode {
        MainHand,
        OffHand,
        Both
    }

    private final Setting<Boolean> onlyWhenSword = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-sword")
        .description("Only auto-attack when holding a sword.")
        .defaultValue(true)
        .build()
    );

    private final Setting<HandMode> handMode = sgGeneral.add(new EnumSetting.Builder<HandMode>()
        .name("hand")
        .description("Which hand to attack with.")
        .defaultValue(HandMode.MainHand)
        .build()
    );

    private final Setting<Double> reach = sgGeneral.add(new DoubleSetting.Builder()
        .name("reach")
        .description("Max reach distance. Keep 3.0 for vanilla.")
        .defaultValue(3.0)
        .min(1.0)
        .max(6.0)
        .sliderMax(6.0)
        .build()
    );

    private final Setting<Double> minDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("min-delay")
        .description("Minimum delay between attacks (ticks). Supports decimals.")
        .defaultValue(4.0)
        .min(0.0)
        .max(40.0)
        .sliderMin(0.0)
        .sliderMax(40.0)
        .build()
    );

    private final Setting<Double> maxDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("max-delay")
        .description("Maximum delay between attacks (ticks). Supports decimals.")
        .defaultValue(8.0)
        .min(0.0)
        .max(40.0)
        .sliderMin(0.0)
        .sliderMax(40.0)
        .build()
    );

    // Bypass: chỉ attack khi attack cooldown gần đầy
    private final Setting<Boolean> waitCooldown = sgGeneral.add(new BoolSetting.Builder()
        .name("wait-cooldown")
        .description("Only attack when attack cooldown is above threshold. Bypass AC.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> cooldownThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("cooldown-threshold")
        .description("Attack cooldown required before hitting (0.0 - 1.0). 0.9 = 90% charged.")
        .defaultValue(0.9)
        .min(0.0)
        .max(1.0)
        .sliderMin(0.0)
        .sliderMax(1.0)
        .visible(waitCooldown::get)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Does not attack friends.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
        .name("swing-hand")
        .description("Swings hand animation when attacking.")
        .defaultValue(true)
        .build()
    );

    private double timer = 0;
    private final Random random = new Random();

    public CrosshairAttack() {
        super(MikuTester.CATEGORY, "crosshair-attack",
            "Automatically attacks players you are looking at. Does not attack mobs or blocks.");
    }

    @Override
    public void onActivate() {
        timer = nextDelay();
    }

    private double nextDelay() {
        double min = minDelay.get();
        double max = maxDelay.get();
        if (min >= max) return min;
        return min + random.nextDouble() * (max - min);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;

        if (onlyWhenSword.get()) {
            if (!mc.player.getMainHandStack().isIn(ItemTags.SWORDS)) return;
        }

        if (timer > 0) {
            timer--;
            return;
        }

        // Check attack cooldown
        if (waitCooldown.get()) {
            float cooldown = mc.player.getAttackCooldownProgress(0f);
            if (cooldown < cooldownThreshold.get()) return;
        }

        // Check crosshair target
        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return;

        EntityHitResult entityHit = (EntityHitResult) hit;
        if (!(entityHit.getEntity() instanceof PlayerEntity target)) return;
        if (target == mc.player) return;
        if (ignoreFriends.get() && Friends.get().isFriend(target)) return;

        // Reach + raycast check
        Vec3d eyePos  = mc.player.getEyePos();
        Box targetBox = target.getBoundingBox().expand(0.1);
        Vec3d lookVec = mc.player.getRotationVec(1.0f);
        Vec3d endVec  = eyePos.add(lookVec.multiply(reach.get()));

        if (targetBox.raycast(eyePos, endVec).isEmpty()) return;

        double dist = eyePos.distanceTo(new Vec3d(target.getX(), target.getEyeY(), target.getZ()));
        if (dist > reach.get()) return;

        // Attack
        mc.interactionManager.attackEntity(mc.player, target);
        if (swingHand.get()) {
            switch (handMode.get()) {
                case MainHand -> mc.player.swingHand(Hand.MAIN_HAND);
                case OffHand  -> mc.player.swingHand(Hand.OFF_HAND);
                case Both -> {
                    mc.player.swingHand(Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.OFF_HAND);
                }
            }
        }

        // Random delay cho lần tiếp theo
        timer = nextDelay();
    }
}