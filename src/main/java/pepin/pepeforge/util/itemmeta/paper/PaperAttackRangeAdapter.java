package pepin.pepeforge.util.itemmeta.paper;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.AttackRange;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.util.itemmeta.AttackRangeAdapter;

public final class PaperAttackRangeAdapter implements AttackRangeAdapter {

    @Override
    public boolean apply(ItemStack item, double minReach, double maxReach,
                         double minCreativeReach, double maxCreativeReach, double hitboxMargin) {
        AttackRange existing = item.getData(DataComponentTypes.ATTACK_RANGE);
        AttackRange.Builder range = AttackRange.attackRange()
                .minReach((float) minReach)
                .maxReach((float) maxReach)
                .minCreativeReach((float) minCreativeReach)
                .maxCreativeReach((float) maxCreativeReach)
                .hitboxMargin((float) hitboxMargin);
        if (existing != null) {
            range.mobFactor(existing.mobFactor());
        }
        item.setData(DataComponentTypes.ATTACK_RANGE, range.build());
        return true;
    }
}
