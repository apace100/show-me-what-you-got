package io.github.apace100.smwyg.compat;

import io.github.apace100.smwyg.ShowMeWhatYouGot;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class CalioPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if(!mixinClassName.equals("io.github.apace100.smwyg.mixin.CalioClientMixin")) {
            return true;
        }
        Optional<ModContainer> calioMod = FabricLoader.getInstance().getModContainer("calio");
        if(calioMod.isPresent()) {
            try {
                Version highestCalioVersionWithItemSharing = Version.parse("1.5.0");
                if(calioMod.get().getMetadata().getVersion().compareTo(highestCalioVersionWithItemSharing) < 0) {
                    ShowMeWhatYouGot.LOGGER.info("Calio < 1.5.0 detected by Show Me What You Got, turning off Calio item sharing keybind to avoid conflicts.");
                    return true;
                }
            } catch (VersionParsingException e) {
                ShowMeWhatYouGot.LOGGER.warn("Could not properly detect which version of Calio was present. Possible side-effect: items are shared in chat twice.");
            }
        }
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
