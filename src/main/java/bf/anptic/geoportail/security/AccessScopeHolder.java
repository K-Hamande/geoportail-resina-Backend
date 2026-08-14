package bf.anptic.geoportail.security;

public class AccessScopeHolder {

    private static final ThreadLocal<String> MINISTERE = new ThreadLocal<>();

    public static void setMinistere(String ministere) {
        MINISTERE.set(ministere);
    }

    public static String getMinistere() {
        return MINISTERE.get();
    }

    public static void clear() {
        MINISTERE.remove();
    }
}