package bf.anptic.geoportail.dto;

import bf.anptic.geoportail.model.enums.NodeStatus;

// Renvoye cote DECIDEUR : vue "Carte" (Amendement 1, Annexe A.2).
// Chaque marqueur est colore selon statutGlobal (vert=OK, orange=WARN,
// rouge=KO). statutAnptic/statutLan sont exposes en plus pour que le
// backoffice (tableau de bord) puisse construire son tableau "sites en
// anomalie" SANS refaire 2 appels par site - tout est deja calcule en
// une seule passe par SiteStatusSnapshotService.
public record MapSiteDto(
        String siteId,
        String nom,
        String ville,
        Double latitude,
        Double longitude,
        NodeStatus statutGlobal,
        NodeStatus statutAnptic,
        NodeStatus statutLan
) {}