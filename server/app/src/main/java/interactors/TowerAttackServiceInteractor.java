package interactors;

import enchantedtowers.game_models.Enchantment;
import enchantedtowers.game_models.ProtectionWall;
import enchantedtowers.game_models.TemplateDescription;
import enchantedtowers.game_models.Tower;
import components.registry.TowersRegistry;

import java.util.List;

public class TowerAttackServiceInteractor {
    private final Tower tower;

    public TowerAttackServiceInteractor(int towerId) {
        this.tower = TowersRegistry.getInstance().getTowerById(towerId).get();
    }

    public void setTowerUnderAttackState() {
        this.tower.setUnderAttack(true);
    }

    public void unsetTowerUnderAttackState() {
        this.tower.setUnderAttack(false);
    }

    public boolean isProtectionWallEnchanted(int protectionWallId) {
        ProtectionWall wall = tower.getProtectionWallById(protectionWallId).get();
        return wall.isEnchanted();
    }

    public int getEnchantedProtectionWallId() {
        return tower.getEnchantedProtectionWall().getId();
    }

    public Enchantment getWallEnchantment(int protectionWallId) {
        ProtectionWall wall = tower.getProtectionWallById(protectionWallId).get();
        return wall.getEnchantment().get();
    }

    public Enchantment enchantmentOf(List<TemplateDescription> templateDescriptions) {
        return new Enchantment(templateDescriptions);
    }

    public void destroyProtectionWallWithId(int protectionWallId) {
        ProtectionWall wall = tower.getProtectionWallById(protectionWallId).get();
        wall.destroyEnchantment();
    }
}
