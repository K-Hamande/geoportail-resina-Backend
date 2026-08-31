package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.IncidentDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class AlertEmailService {

    private static final Logger log = LoggerFactory.getLogger(AlertEmailService.class);
    private static final DateTimeFormatter FORMAT_DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("Africa/Ouagadougou"));

    private final JavaMailSender mailSender;

    @Value("${resina.alertes.email-destinataire:}")
    private String emailDestinataire;

    public AlertEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Envoie au destinataire unique configure (resina.alertes.email-destinataire) -
    // celui-ci est notifie pour n'importe quel incident, quel que soit le site.
    public void envoyerAlerteNouvelIncident(IncidentDto incident) {
        if (emailDestinataire == null || emailDestinataire.isBlank()) {
            log.warn("Alerte non envoyee au destinataire unique : resina.alertes.email-destinataire n'est pas configure.");
            return;
        }
        envoyerA(emailDestinataire, incident);
    }

    // Envoie a l'adresse email d'un decideur cible (celui dont le ministere
    // est proprietaire du site concerne par l'incident).
    public void envoyerAlerteADecideur(String emailDecideur, IncidentDto incident) {
        if (emailDecideur == null || emailDecideur.isBlank()) {
            return;
        }
        envoyerA(emailDecideur, incident);
    }

    private void envoyerA(String destinataire, IncidentDto incident) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(destinataire);
            message.setSubject("[GéoPortail RESINA] Incident " + incident.nouveauStatut() + " - " + incident.siteNom());
            message.setText(
                    "Un incident vient d'être détecté sur le réseau RESINA.\n\n"
                            + "Site : " + incident.siteNom() + " (" + incident.ville() + ")\n"
                            + "Type : " + incident.type() + "\n"
                            + "Statut : " + incident.nouveauStatut() + "\n"
                            + "Détail : " + incident.message() + "\n"
                            + "Survenu le : " + FORMAT_DATE.format(incident.survenuLe()) + "\n"
            );
            mailSender.send(message);
            log.info("Email d'alerte envoye pour l'incident {} a {}", incident.id(), destinataire);
        } catch (Exception e) {
            log.error("Echec de l'envoi de l'email d'alerte pour l'incident {} a {}", incident.id(), destinataire, e);
        }
    }
}