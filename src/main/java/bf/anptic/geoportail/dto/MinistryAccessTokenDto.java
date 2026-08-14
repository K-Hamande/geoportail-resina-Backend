package bf.anptic.geoportail.dto;

import java.time.Instant;

public record MinistryAccessTokenDto(
        Long id,
        String token,
        String ministere,
        String libelle,
        Boolean actif,
        Instant creeLe,
        String creePar
) {}