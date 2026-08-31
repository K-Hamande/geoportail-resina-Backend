package bf.anptic.geoportail.controller;

import bf.anptic.geoportail.dto.AlertPreferenceDto;
import bf.anptic.geoportail.security.AccessScopeHolder;
import bf.anptic.geoportail.service.DecideurAuthService;
import org.springframework.web.bind.annotation.*;

// Self-service : le decideur connecte consulte/modifie SA propre
// preference de reception des alertes email (bandeau "Activer les
// alertes" cote frontend decideur). Aucune authentification Backoffice
// ici, ni audit - ce n'est pas une action d'administration, cf.
// NotificationService.registerToken pour le meme principe.
@RestController
@RequestMapping("/api/v1/decideur/alertes")
public class DecideurPreferencesController {

    private final DecideurAuthService decideurAuthService;

    public DecideurPreferencesController(DecideurAuthService decideurAuthService) {
        this.decideurAuthService = decideurAuthService;
    }

    @GetMapping
    public AlertPreferenceDto getPreference() {
        return new AlertPreferenceDto(decideurAuthService.getAlertesActivees(AccessScopeHolder.getUserId()));
    }

    @PostMapping("/activer")
    public AlertPreferenceDto activer() {
        decideurAuthService.setAlertesActivees(AccessScopeHolder.getUserId(), true);
        return new AlertPreferenceDto(true);
    }

    @PostMapping("/desactiver")
    public AlertPreferenceDto desactiver() {
        decideurAuthService.setAlertesActivees(AccessScopeHolder.getUserId(), false);
        return new AlertPreferenceDto(false);
    }
}