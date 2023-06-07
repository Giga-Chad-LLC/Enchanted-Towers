package interactors;

import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.ProtectionWall;
import enchantedtowers.game_models.SpellTemplateDescription;
import enchantedtowers.game_models.Tower;
import components.registry.TowersRegistry;

import java.time.Instant;
import java.util.List;

public class ProtectionWallSetupServiceInteractor {
    private final Tower tower;

    public ProtectionWallSetupServiceInteractor(int towerId) {
        this.tower = TowersRegistry.getInstance().getTowerById(towerId).get();
    }

    public void setTowerOwner(int playerId) {
        tower.setOwnerId(playerId);
    }

    public void setCaptureLock() {
        tower.setUnderCaptureLock(true);
    }

    public void unsetCaptureLock() {
        tower.setUnderCaptureLock(false);
    }

    public void setProtectionWallInstallation() {
        tower.setUnderProtectionWallsInstallation(true);
    }

    public void unsetProtectionWallInstallation() {
        tower.setUnderProtectionWallsInstallation(false);
    }

    public void updateModificationTimestamp(Instant timestamp) {
        tower.setLastProtectionWallModificationTimestamp(timestamp);
    }

    public void createNewEnchantmentForProtectionWall(List<SpellTemplateDescription> spellTemplateDescriptions, int protectionWallId) {
        Enchantment enchantment = new Enchantment(spellTemplateDescriptions);
        ProtectionWall wall = tower.getProtectionWallById(protectionWallId).get();
        wall.setEnchantment(enchantment);
    }

    public void destroyEnchantment(int protectionWallId) {
        ProtectionWall wall = tower.getProtectionWallById(protectionWallId).get();
        wall.removeEnchantment();
    }
}
