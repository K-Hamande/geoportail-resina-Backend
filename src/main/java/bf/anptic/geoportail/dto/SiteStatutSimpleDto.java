package bf.anptic.geoportail.dto;

// Vue simplifiee pour les utilisateurs lambda : uniquement le statut
// global (operationnel/hors service), sans aucune donnee technique
// (debit, latence, equipements LAN...).
public record SiteStatutSimpleDto(
        String siteId,
        String nom,
        String ville,
        String region,
        String ministere,
        Double latitude,
        Double longitude,
        String statut   // "OK" ou "KO" uniquement — pas de WARN ni UNKNOWN
) {}