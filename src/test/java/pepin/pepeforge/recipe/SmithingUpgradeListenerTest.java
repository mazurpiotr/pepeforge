package pepin.pepeforge.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmithingUpgradeListenerTest {

    @Test
    void customDisplayNameIsDetectedWhenItDiffersFromTheItemName() {
        assertTrue(SmithingUpgradeListener.hasCustomDisplayName("My Greatsword", "Diamond Greatsword"));
    }

    @Test
    void defaultItemNameIsNotTreatedAsACustomDisplayName() {
        assertFalse(SmithingUpgradeListener.hasCustomDisplayName("Diamond Greatsword", "Diamond Greatsword"));
        assertFalse(SmithingUpgradeListener.hasCustomDisplayName(null, "Diamond Greatsword"));
    }
}
