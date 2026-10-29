package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.NotificationTokenResponse;
import bf.anptic.geoportail.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/backoffice/api/v1/notifications")
public class AdminNotificationController {

    private static final ZoneId FUSEAU = ZoneId.of("Africa/Ouagadougou");

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Filtres : site (deja existant), ministere, ville, et periode sur la
    // date d'enregistrement (meme principe que la page "Historique des
    // incidents" : soit "jours" glissant, soit "date" precise - mais ici
    // SANS fenetre par defaut, "jours"/"date" absents == tout l'historique,
    // contrairement aux incidents ou 30 jours est le defaut. Un abonnement
    // push reste pertinent indefiniment tant qu'il n'est pas expire, pas
    // de raison de masquer les plus anciens par defaut).
    @GetMapping
    public List<NotificationTokenResponse> listTokens(
            @RequestParam(required = false) String siteId,
            @RequestParam(required = false) String ministere,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) Integer jours,
            @RequestParam(required = false) String date) {

        Instant dateDebut = null;
        Instant dateFin = null;

        if (date != null && !date.isBlank()) {
            LocalDate jour = LocalDate.parse(date);
            dateDebut = jour.atStartOfDay(FUSEAU).toInstant();
            dateFin = jour.plusDays(1).atStartOfDay(FUSEAU).toInstant();
        } else if (jours != null && jours > 0) {
            dateFin = Instant.now();
            dateDebut = dateFin.minus(jours, ChronoUnit.DAYS);
        }

        return notificationService.listTokens(siteId, ministere, ville, dateDebut, dateFin);
    }

    @DeleteMapping("/{id}")
    public void deleteToken(@PathVariable Long id, Authentication authentication) {
        notificationService.deleteToken(id, authentication.getName());
    }
}
