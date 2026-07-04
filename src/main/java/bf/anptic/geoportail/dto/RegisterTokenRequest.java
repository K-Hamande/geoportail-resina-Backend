package bf.anptic.geoportail.dto;

// Envoye par l'app decideur pour s'abonner aux alertes d'un site
// (Amendement 1, Annexe A.3 : "Activer les alertes en temps reel").
public record RegisterTokenRequest(
        String profil,       // ex: "Ministre", "Protocole"
        String plateforme,   // "ANDROID" ou "IOS"
        String token
) {}