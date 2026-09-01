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

/**
 * Surveille periodiquement les incidents actifs et declenche un email
 * d'alerte des qu'un NOUVEL incident apparait, vers deux destinations :
 *   1) le destinataire unique configure (resina.alertes.email-destinataire),
 *      toujours notifie quel que soit le site ;
 *   2) les comptes decideurs (role DECIDEUR, actifs, avec un email renseigne)
 *      dont le ministere correspond au ministere proprietaire du site touche,
 *      et qui ont eux-memes active la reception des alertes email.
 *
 * La detection "nouvel incident vs deja connu" est deleguee a
 * IncidentHistoryService, qui persiste chaque incident dans la table
 * incident_historique (debut/fin reels). C'est aussi ce qui alimente la
 * page Backoffice "Historique des incidents" - un seul et meme passage
 * periodique sert donc les deux besoins, evitant d'interroger NetXMS deux
 * fois en parallele.
 *
 * Important : la mise a jour de l'historique tourne TOUJOURS (meme si
 * resina.alertes.enabled=false), seul l'ENVOI d'email est conditionne par
 * ce reglage. Avant cette version, la detection "deja notifie" reposait
 * sur un simple Set en memoire, remis a zero a chaque redemarrage du
 * serveur - ce qui provoquait un nouvel envoi d'email pour des pannes deja
 * actives avant le redemarrage. S'appuyer sur la table d'historique
 * (persistante) corrige ce defaut : un incident deja ouvert en base avant
 * le redemarrage n'est jamais traite comme "nouveau".
 */
@Component
public class IncidentAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(IncidentAlertScheduler.class);

    private final IncidentHistoryService incidentHistoryService;
    private final AlertEmailService alertEmailService;
    private final DecideurUserRepository decideurUserRepository;

    @Value("${resina.alertes.enabled:true}")
    private boolean alertesActivees;

    public IncidentAlertScheduler(IncidentHistoryService incidentHistoryService,
                                   AlertEmailService alertEmailService,
                                   DecideurUserRepository decideurUserRepository) {
        this.incidentHistoryService = incidentHistoryService;
        this.alertEmailService = alertEmailService;
        this.decideurUserRepository = decideurUserRepository;
    }

    @Scheduled(cron = "${resina.alertes.check-cron:0 */5 * * * *}")
    public void verifierIncidents() {
        try {
            // Toujours execute : alimente l'historique persistant, meme si
            // l'envoi d'email est desactive juste en dessous.
            List<IncidentDto> nouveaux = incidentHistoryService.detecterEtEnregistrer();

            if (!alertesActivees || nouveaux.isEmpty()) {
                return;
            }

            for (IncidentDto incident : nouveaux) {
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
                        // email de son cote (page "Alertes") - sinon il ne
                        // recoit rien, meme avec un email renseigne.
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
        } catch (Exception e) {
            log.error("Echec de la verification des incidents pour alerte email", e);
        }
    }
}