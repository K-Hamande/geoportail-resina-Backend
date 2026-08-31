package bf.anptic.geoportail.security;

// Porte le "ministere" ET le "role" resolu pour la requete decideur EN
// COURS. ThreadLocal = une valeur par thread, donc par requete HTTP.
// Rempli par SiteAccessTokenFilter (token URL) ou DecideurTokenFilter
// (JWT), TOUJOURS nettoye en finally.
public class AccessScopeHolder {

    private static final ThreadLocal<String> MINISTERE = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    public static void setMinistere(String ministere) { MINISTERE.set(ministere); }
    public static String getMinistere() { return MINISTERE.get(); }

    public static void setRole(String role) { ROLE.set(role); }
    public static String getRole() { return ROLE.get(); }

    // Identifiant du compte decideur authentifie (issu du JWT) - utilise
    // pour les actions "self-service" (ex: preference d'alertes email).
    public static void setUserId(Long userId) { USER_ID.set(userId); }
    public static Long getUserId() { return USER_ID.get(); }

    // "LAMBDA" = acces consultation uniquement (statut global 🟢/🔴)
    public static boolean estLambda() { return "LAMBDA".equals(ROLE.get()); }

    public static void clear() {
        MINISTERE.remove();
        ROLE.remove();
        USER_ID.remove();
    }
}