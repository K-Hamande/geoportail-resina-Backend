package bf.anptic.geoportail.security;

// Porte le "ministere" ET le "role" resolu pour la requete decideur EN
// COURS. ThreadLocal = une valeur par thread, donc par requete HTTP.
// Rempli par SiteAccessTokenFilter (token URL) ou DecideurTokenFilter
// (JWT), TOUJOURS nettoye en finally.
public class AccessScopeHolder {

    private static final ThreadLocal<String> MINISTERE = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();

    public static void setMinistere(String ministere) { MINISTERE.set(ministere); }
    public static String getMinistere() { return MINISTERE.get(); }

    public static void setRole(String role) { ROLE.set(role); }
    public static String getRole() { return ROLE.get(); }

    // "LAMBDA" = acces consultation uniquement (statut global 🟢/🔴)
    public static boolean estLambda() { return "LAMBDA".equals(ROLE.get()); }

    public static void clear() {
        MINISTERE.remove();
        ROLE.remove();
    }
}