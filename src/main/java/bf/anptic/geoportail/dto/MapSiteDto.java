package bf.anptic.geoportail.dto;

import bf.anptic.geoportail.model.enums.NodeStatus;

// Renvoye cote DECIDEUR : vue "Carte" (Amendement 1, Annexe A.2).
// Chaque marqueur est colore selon statutGlobal (vert=OK, orange=WARN,
// rouge=KO).
public record MapSiteDto(
        String siteId,
        String nom,
        String ville,
        Double latitude,
        Double longitude,
        NodeStatus statutGlobal
) {}