package kome.hatsunemiku.addon;

import kome.hatsunemiku.addon.modules.AutoCrystal;
import kome.hatsunemiku.addon.modules.MaceSwap;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class AddonTemplate extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("MikuTester");
    public static final HudGroup HUD_GROUP = new HudGroup("MikuTester");

    @Override
    public void onInitialize() {
        LOG.info("Miku hiding in your wifi!");
        Modules.get().add(new MaceSwap());
        Modules.get().add(new AutoCrystal());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return "kome.hatsunemiku.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}
