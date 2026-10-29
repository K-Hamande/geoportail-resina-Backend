package bf.anptic.geoportail.dto;

import java.util.List;

// Resultat de l'import CSV de sites (section VI du compte rendu des
// amendements) - un import "partiel" est volontairement accepte : une
// ligne en erreur n'annule pas les autres, elle est juste listee a part
// pour que l'administrateur puisse la corriger et la reimporter seule.
public record SiteImportResultDto(
        int lignesTraitees,
        int crees,
        int modifies,
        List<SiteImportErreurDto> erreurs
) {
    public record SiteImportErreurDto(int ligne, String message) {}
}
