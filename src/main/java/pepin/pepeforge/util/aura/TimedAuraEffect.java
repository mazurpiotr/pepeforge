package pepin.pepeforge.util.aura;

public interface TimedAuraEffect extends AuraEffect {

    /**
     * @return true, jeśli aura wygasła i powiązywane z nią zadanie ma zostać usunięte.
     */
    boolean isExpired();

    /**
     * Zwiększa czas trwania lub resetuje cykl aury.
     * Służy zapobieganiu tworzeniu zduplikowanych tasków.
     * @param ticks Ilość ticków do przedłużenia/odnowienia.
     */
    void extendDuration(int ticks);
}
