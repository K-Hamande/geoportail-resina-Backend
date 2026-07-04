package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.NotificationTokenResponse;
import bf.anptic.geoportail.dto.RegisterTokenRequest;
import bf.anptic.geoportail.model.NotificationToken;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.repository.NotificationTokenRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationTokenRepository notificationTokenRepository;
    private final SiteRepository siteRepository;
    private final AuditService auditService;

    public NotificationService(NotificationTokenRepository notificationTokenRepository,
                                SiteRepository siteRepository,
                                AuditService auditService) {
        this.notificationTokenRepository = notificationTokenRepository;
        this.siteRepository = siteRepository;
        this.auditService = auditService;
    }

    // Cote DECIDEUR : enregistrement d'un token, action "libre" (pas
    // d'authentification Backoffice), pas d'audit non plus - ce n'est
    // pas une action d'administration, juste un abonnement utilisateur.
    public void registerToken(String siteId, RegisterTokenRequest request) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Site introuvable : " + siteId));

        NotificationToken entry = new NotificationToken();
        entry.setSite(site);
        entry.setProfil(request.profil());
        entry.setPlateforme(request.plateforme());
        entry.setToken(request.token());
        entry.setActif(true);
        entry.setEnregistreLe(Instant.now());

        notificationTokenRepository.save(entry);
    }

    // Cote BACKOFFICE : consultation (§3.2.6b)
    public List<NotificationTokenResponse> listTokens(String siteId) {
        List<NotificationToken> tokens = (siteId == null || siteId.isBlank())
                ? notificationTokenRepository.findAll()
                : notificationTokenRepository.findBySite_SiteIdAndActifTrue(siteId);

        return tokens.stream().map(this::toResponse).toList();
    }

    // Cote BACKOFFICE : suppression manuelle (§3.2.6b), avec audit cette fois
    public void deleteToken(String token, String auteur) {
        notificationTokenRepository.deleteByToken(token);
        auditService.record(auteur, "Suppression token push", "token se terminant par ..." + lastFour(token));
    }

    private NotificationTokenResponse toResponse(NotificationToken t) {
        return new NotificationTokenResponse(
                t.getId(),
                t.getSite().getSiteId(),
                t.getSite().getNom(),
                t.getProfil(),
                t.getPlateforme(),
                lastFour(t.getToken()),
                t.getActif(),
                t.getEnregistreLe()
        );
    }

    private static String lastFour(String token) {
        if (token == null || token.length() < 4) {
            return "****";
        }
        return "..." + token.substring(token.length() - 4);
    }
}