package bf.anptic.geoportail.dto;

// Une commune et son statut de couverture RESINA (page publique
// "Geoportail national"). Reprend la logique de la vue PostGIS
// donnebase.vue_commune_connectee_resina (voir CouvertureNationaleService),
// en distinguant les deux raisons possibles d'etre "connectee" :
// CONNECTEE (contient un site administratif raccorde), PARTIELLE
// (aucun site mais une liaison fibre/LS/conduit la traverse), ou
// NON_CONNECTEE (ni l'un ni l'autre).
public record CommuneCouvertureDto(
        Integer id,
        String nom,
        String province,
        String region,
        String statut,
        long nombreSitesConnectes
) {}
