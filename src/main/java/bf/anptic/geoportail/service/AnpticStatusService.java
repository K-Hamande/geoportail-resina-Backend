package bf.anptic.geoportail.service;

import bf.anptic.geoportail.client.NetXmsClient;
import bf.anptic.geoportail.client.NodeMetrics;
import bf.anptic.geoportail.dto.AnpticStatusDto;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.model.enums.NodeStatus;
import bf.anptic.geoportail.repository.SiteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// @Service : comme @Component, ca dit a Spring "gere une instance
// unique de cette classe et injecte-la ou j'en ai besoin". @Service
// est juste une variante semantique de @Component, utilisee par
// convention pour les classes de logique metier.
@Service
public class AnpticStatusService {

    private final SiteRepository siteRepository;
    private final NetXmsClient netXmsClient;

    public AnpticStatusService(SiteRepository siteRepository, NetXmsClient netXmsClient) {
        this.siteRepository = siteRepository;
        this.netXmsClient = netXmsClient;
    }

    public AnpticStatusDto getAnpticStatus(String siteId) {
        // 1) Retrouver le site en base, pour connaitre son netxmsNodeId
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Site introuvable : " + siteId));

        // 2) Interroger NetXMS (notre mock, pour l'instant) avec ce nodeId
        NodeMetrics metrics = netXmsClient.getNodeMetrics(site.getNetxmsNodeId());

        // 3) Traduire le statut technique NetXMS en statut simplifie
        NodeStatus status = NodeStatus.fromNetXmsSeverity(metrics.getStatus());
        boolean disponible = status == NodeStatus.OK;

        // 4) Construire le message adapte au decideur (§5.1 : langage non technique)
        if (disponible) {
            return new AnpticStatusDto(
                    siteId,
                    status,
                    true,
                    "La connexion ANPTIC est disponible",
                    metrics.getOutboundMbps(),   // debit montant = ce que le site envoie
                    metrics.getInboundMbps(),    // debit descendant = ce que le site recoit
                    metrics.getLinkType(),
                    metrics.getSignalQuality(),
                    metrics.getLatencyMs(),
                    metrics.getAvailability30d(),
                    null,
                    null
            );
        } else {
            return new AnpticStatusDto(
                    siteId,
                    status,
                    false,
                    "La connexion ANPTIC n'est pas disponible",
                    null,
                    null,
                    metrics.getLinkType(),
                    null,
                    null,
                    metrics.getAvailability30d(),
                    metrics.getDownSince(),
                    "Veuillez contacter les services support de l'ANPTIC"
            );
        }
    }
}