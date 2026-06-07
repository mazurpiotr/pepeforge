package pepin.pepeforge.util.aura;

import org.bukkit.entity.Player;

public interface AuraEffect {

    void tick(Player player);

    /**
     * Zwraca częstotliwość tickowania efektu.
     * Menedżer uruchomi efekt co N-ty własny tick, 
     * lub co N-ty tick regionu gracza w przypadku aur aktywnych.
     * @return Ilość ticków pomiędzy wywołaniami.
     */
    int getTickInterval();
}
