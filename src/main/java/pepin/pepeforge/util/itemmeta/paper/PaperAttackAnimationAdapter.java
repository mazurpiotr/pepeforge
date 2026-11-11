package pepin.pepeforge.util.itemmeta.paper;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.SwingAnimation;
import org.bukkit.inventory.ItemStack;
import pepin.pepeforge.util.itemmeta.AttackAnimationAdapter;

public final class PaperAttackAnimationAdapter implements AttackAnimationAdapter {

    private static final DataComponentType.Valued<SwingAnimation> ATTACK_ANIMATION = resolveComponent();

    @Override
    public boolean apply(ItemStack item, boolean stab) {
        item.setData(ATTACK_ANIMATION, SwingAnimation.swingAnimation()
                .type(stab ? SwingAnimation.Animation.STAB : SwingAnimation.Animation.WHACK)
                .build());
        return true;
    }

    @SuppressWarnings("unchecked")
    private static DataComponentType.Valued<SwingAnimation> resolveComponent() {
        try {
            String fieldName;
            try {
                DataComponentTypes.class.getField("ATTACK_ANIMATION");
                fieldName = "ATTACK_ANIMATION";
            } catch (NoSuchFieldException exception) {
                fieldName = "SWING_ANIMATION";
            }
            return (DataComponentType.Valued<SwingAnimation>) DataComponentTypes.class.getField(fieldName).get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("This Paper API does not expose an attack animation component", exception);
        }
    }
}
