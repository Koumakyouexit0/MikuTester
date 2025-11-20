package kome.hatsunemiku.addon.modules;

import kome.hatsunemiku.addon.MikuTester;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

public class MaceSwap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWeapons = settings.createGroup("Weapons");

    public enum WeaponType {
        Mace,
        Sword,
        Axe
    }

    private final Setting<WeaponType> weaponType = sgGeneral.add(new EnumSetting.Builder<WeaponType>()
        .name("weapon-type")
        .description("Type of weapon to swap to.")
        .defaultValue(WeaponType.Mace)
        .build()
    );

    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
        .name("auto-switch")
        .description("Automatically switches to weapon when attacking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder()
        .name("switch-back")
        .description("Switches back to your previous item after attacking.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> switchDelay = sgGeneral.add(new IntSetting.Builder()
        .name("switch-delay")
        .description("Delay in ticks before switching back to previous item. (1 tick recommended to avoid anticheat)")
        .defaultValue(1)
        .min(0)
        .max(20)
        .sliderMax(20)
        .visible(switchBack::get)
        .build()
    );

    private final Setting<Boolean> onlyWhenFalling = sgGeneral.add(new BoolSetting.Builder()
        .name("only-when-falling")
        .description("Only swaps to weapon when falling (useful for mace maximum damage).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> fallDistance = sgGeneral.add(new DoubleSetting.Builder()
        .name("fall-distance")
        .description("Minimum fall distance required to swap to weapon.")
        .defaultValue(3.0)
        .min(0.0)
        .max(20.0)
        .sliderMax(20.0)
        .visible(onlyWhenFalling::get)
        .build()
    );

    // Sword preferences
    private final Setting<Boolean> preferNetherite = sgWeapons.add(new BoolSetting.Builder()
        .name("prefer-netherite")
        .description("Prefer netherite weapons over diamond.")
        .defaultValue(true)
        .visible(() -> weaponType.get() == WeaponType.Sword || weaponType.get() == WeaponType.Axe)
        .build()
    );

    private final Setting<Boolean> preferDiamond = sgWeapons.add(new BoolSetting.Builder()
        .name("prefer-diamond")
        .description("Prefer diamond weapons over other materials.")
        .defaultValue(true)
        .visible(() -> weaponType.get() == WeaponType.Sword || weaponType.get() == WeaponType.Axe)
        .build()
    );

    private int previousSlot = -1;
    private int switchBackTimer = 0;
    private ItemStack previousItem = null;

    public MaceSwap() {
        super(MikuTester.CATEGORY, "weapon-swap", "Automatically swaps to selected weapon when attacking entities.");
    }

    @Override
    public void onActivate() {
        previousSlot = -1;
        switchBackTimer = 0;
        previousItem = null;
    }

    private FindItemResult findWeapon() {
        switch (weaponType.get()) {
            case Mace:
                return InvUtils.find(Items.MACE);

            case Sword:
                // Try to find best sword based on preferences
                if (preferNetherite.get()) {
                    FindItemResult netherite = InvUtils.find(Items.NETHERITE_SWORD);
                    if (netherite.found()) return netherite;
                }
                if (preferDiamond.get()) {
                    FindItemResult diamond = InvUtils.find(Items.DIAMOND_SWORD);
                    if (diamond.found()) return diamond;
                }
                // Fallback to other swords
                FindItemResult iron = InvUtils.find(Items.IRON_SWORD);
                if (iron.found()) return iron;

                FindItemResult golden = InvUtils.find(Items.GOLDEN_SWORD);
                if (golden.found()) return golden;

                FindItemResult stone = InvUtils.find(Items.STONE_SWORD);
                if (stone.found()) return stone;

                FindItemResult wooden = InvUtils.find(Items.WOODEN_SWORD);
                if (wooden.found()) return wooden;

                // If no sword found, return empty result
                return InvUtils.find(itemStack -> false);

            case Axe:
                // Try to find best axe based on preferences
                if (preferNetherite.get()) {
                    FindItemResult netherite = InvUtils.find(Items.NETHERITE_AXE);
                    if (netherite.found()) return netherite;
                }
                if (preferDiamond.get()) {
                    FindItemResult diamond = InvUtils.find(Items.DIAMOND_AXE);
                    if (diamond.found()) return diamond;
                }
                // Fallback to other axes
                FindItemResult ironAxe = InvUtils.find(Items.IRON_AXE);
                if (ironAxe.found()) return ironAxe;

                FindItemResult goldenAxe = InvUtils.find(Items.GOLDEN_AXE);
                if (goldenAxe.found()) return goldenAxe;

                FindItemResult stoneAxe = InvUtils.find(Items.STONE_AXE);
                if (stoneAxe.found()) return stoneAxe;

                FindItemResult woodenAxe = InvUtils.find(Items.WOODEN_AXE);
                if (woodenAxe.found()) return woodenAxe;

                // If no axe found, return empty result
                return InvUtils.find(itemStack -> false);

            default:
                return InvUtils.find(Items.MACE);
        }
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (!autoSwitch.get()) return;
        if (mc.player == null) return;

        // Check if we should only swap when falling
        if (onlyWhenFalling.get()) {
            if (mc.player.fallDistance < fallDistance.get()) return;
        }

        // Find weapon in inventory
        FindItemResult weapon = findWeapon();
        if (!weapon.found()) return;

        // Check if we're already holding the weapon
        Item currentItem = mc.player.getMainHandStack().getItem();
        Item targetItem = mc.player.getInventory().getStack(weapon.slot()).getItem();
        if (currentItem == targetItem) return;

        // Store current slot and item if not already stored
        if (previousSlot == -1) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i) == mc.player.getMainHandStack()) {
                    previousSlot = i;
                    previousItem = mc.player.getMainHandStack().copy();
                    break;
                }
            }
        }

        // Switch to weapon
        InvUtils.swap(weapon.slot(), true);

        // Set timer for switching back
        if (switchBack.get()) {
            switchBackTimer = switchDelay.get();
        }
    }

    @Override
    public void onDeactivate() {
        // Switch back to previous item if needed
        if (previousSlot != -1 && switchBack.get() && mc.player != null) {
            InvUtils.swap(previousSlot, true);
            previousSlot = -1;
        }
        switchBackTimer = 0;
        previousItem = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;

        // Handle switch back timer
        if (switchBackTimer > 0) {
            switchBackTimer--;

            if (switchBackTimer == 0 && previousSlot != -1) {
                InvUtils.swap(previousSlot, true);
                previousSlot = -1;
                previousItem = null;
            }
        }
    }
}
