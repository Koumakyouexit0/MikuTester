package kome.hatsunemiku.addon.modules;

import kome.hatsunemiku.addon.MikuTester;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SmoothAim extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> rotateSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("rotate-speed")
        .description("Fixed degrees moved per tick toward target. Constant = bypass Vulcan.")
        .defaultValue(1.2)
        .min(0.1)
        .max(5.0)
        .sliderMin(0.1)
        .sliderMax(5.0)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Range to find targets.")
        .defaultValue(6.0)
        .min(1.0)
        .max(20.0)
        .sliderMax(20.0)
        .build()
    );

    private final Setting<Double> fovCheck = sgGeneral.add(new DoubleSetting.Builder()
        .name("fov")
        .description("Only aim at players within this FOV. 180 = no limit.")
        .defaultValue(60.0)
        .min(1.0)
        .max(180.0)
        .sliderMax(180.0)
        .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-friends")
        .description("Does not aim at friends.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> aimHead = sgGeneral.add(new BoolSetting.Builder()
        .name("aim-head")
        .description("Aims at head instead of body center.")
        .defaultValue(true)
        .build()
    );

    public SmoothAim() {
        super(MikuTester.CATEGORY, "smooth-aim",
            "Smoothly moves your crosshair towards the nearest player.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        PlayerEntity target = findTarget();
        if (target == null) return;

        Vec3d targetPos = aimHead.get()
            ? new Vec3d(target.getX(), target.getEyeY(), target.getZ())
            : new Vec3d(target.getX(), target.getY() + target.getHeight() / 2.0, target.getZ());

        Vec3d diff = targetPos.subtract(mc.player.getEyePos());

        float targetYaw   = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        double hDist      = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float targetPitch = (float) Math.toDegrees(-Math.atan2(diff.y, hDist));

        float currentYaw   = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float deltaYaw   = wrapDegrees(targetYaw   - currentYaw);
        float deltaPitch = wrapDegrees(targetPitch - currentPitch);

        float speed = rotateSpeed.get().floatValue();

        // Move FIXED degrees per tick — delta is constant, Vulcan won't flag
        float moveYaw   = MathHelper.clamp(deltaYaw,   -speed, speed);
        float movePitch = MathHelper.clamp(deltaPitch, -speed, speed);

        // Stop jittering when already close enough
        if (Math.abs(deltaYaw)   < 0.1f) moveYaw   = 0f;
        if (Math.abs(deltaPitch) < 0.1f) movePitch = 0f;

        mc.player.setYaw(currentYaw + moveYaw);
        mc.player.setPitch(MathHelper.clamp(currentPitch + movePitch, -90f, 90f));
    }

    private PlayerEntity findTarget() {
        PlayerEntity best = null;
        double bestAngle = fovCheck.get();

        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof PlayerEntity player)) continue;
            if (player == mc.player) continue;
            if (ignoreFriends.get() && Friends.get().isFriend(player)) continue;
            if (mc.player.distanceTo(player) > range.get()) continue;

            double angle = getAngleTo(player);
            if (angle < bestAngle) {
                best = player;
                bestAngle = angle;
            }
        }

        return best;
    }

    private double getAngleTo(PlayerEntity target) {
        Vec3d diff = new Vec3d(target.getX(), target.getEyeY(), target.getZ())
            .subtract(mc.player.getEyePos()).normalize();

        double tYaw   = Math.toDegrees(Math.atan2(-diff.x, diff.z));
        double hDist  = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        double tPitch = Math.toDegrees(-Math.atan2(diff.y, hDist));

        double dy = Math.abs(wrapDegrees(tYaw   - mc.player.getYaw()));
        double dp = Math.abs(wrapDegrees(tPitch - mc.player.getPitch()));
        return Math.sqrt(dy * dy + dp * dp);
    }

    private float wrapDegrees(double deg) {
        float f = (float)(deg % 360.0);
        if (f >= 180f)  f -= 360f;
        if (f < -180f)  f += 360f;
        return f;
    }
}