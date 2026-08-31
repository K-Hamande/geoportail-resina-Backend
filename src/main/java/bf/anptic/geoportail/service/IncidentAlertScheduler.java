package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.IncidentDto;
import bf.anptic.geoportail.model.DecideurUser;
import bf.anptic.geoportail.repository.DecideurUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Surveille periodiquement les incidents actifs (calcules a la volee par
 * IncidentService) et declenche un email d'alerte des qu'un NOUVEL incident
 * apparait, vers deux destinations :
 *   1) le destinataire unique configure (resina.alertes.email-destinataire),
 *      toujours notifie quel que soit le site ;
 *   2) les comptes decideurs (role DECIDEUR, actifs, avec un email renseigne
 *      ET ayant explicitement active la preference "alertes") dont le
 *      ministere correspond au ministere proprietaire du site touche.
 * Un meme incident n'est notifie qu'une seule fois tant qu'il reste actif ;
 * s'il se resout puis reapparait, il redeclenche un nouvel envoi (retainAll
 * oublie les incidents qui ne sont plus actifs).
 */
@Component
public class IncidentAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(IncidentAlertScheduler.class);

    private final IncidentService incidentService;
    private final AlertEmailService alertEmailService;
    private final DecideurUserRepository decideurUserRepository;
    private final Set<String> incidentsDejaNotifies = ConcurrentHashMap.newKeySet();

    @Value("${resina.alertes.enabled:true}")
    private boolean alertesActivees;

    public IncidentAlertScheduler(IncidentService incidentService,
                                   AlertEmailService alertEmailService,
                                   DecideurUserRepository decideurUserRepository) {
        this.incidentService = incidentService;
        this.alertEmailService = alertEmailService;
        this.decideurUserRepository = decideurUserRepository;
    }

    @Scheduled(cron = "${resina.alertes.check-cron:0 */5 * * * *}")
    public void verifierIncidents() {
        if (!alertesActivees) {
            return;
        }
        try {
            List<IncidentDto> incidentsActifs = incidentService.listIncidents();
            Set<String> idsActifs = ConcurrentHashMap.newKeySet();

            for (IncidentDto incident : incidentsActifs) {
                idsActifs.add(incident.id());
                if (incidentsDejaNotifies.add(incident.id())) {
                    log.info("Nouvel incident detecte, envoi d'une alerte email : {}", incident.id());

                    // 1) Destinataire unique historique (tous sites confondus).
                    alertEmailService.envoyerAlerteNouvelIncident(incident);

                    // 2) Decideurs du ministere proprietaire du site concerne.
                    if (incident.ministere() != null && !incident.ministere().isBlank()) {
                        List<DecideurUser> decideurs = decideurUserRepository
                                .findByMinistereAndRoleAndActifTrue(incident.ministere(), DecideurUser.Role.DECIDEUR);

                        Set<String> emailsDejaEnvoyes = new HashSet<>();
                        for (DecideurUser decideur : decideurs) {
                            // Le decideur doit avoir explicitement active les alertes
                            // email de son cote (bandeau "Activer les alertes") -
                            // sinon il ne recoit rien, meme avec un email renseigne.
                            if (!Boolean.TRUE.equals(decideur.getAlertesActivees())) {
                                continue;
                            }
                            String email = decideur.getEmail();
                            if (email != null && !email.isBlank() && emailsDejaEnvoyes.add(email)) {
                                alertEmailService.envoyerAlerteADecideur(email, incident);
                            }
                        }
                    }
                }
            }

            incidentsDejaNotifies.retainAll(idsActifs);
        } catch (Exception e) {
            log.error("Echec de la verification des incidents pour alerte email", e);
        }
    }
}