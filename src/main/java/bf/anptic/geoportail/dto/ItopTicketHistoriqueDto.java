package bf.anptic.geoportail.dto;

import java.time.Instant;

// Une ligne de la page Backoffice "Historique des incidents" - onglet
// "Incidents iTop" (section distincte de l'onglet "Alertes reseau",
// cf. IncidentHistoriqueDto).
public record ItopTicketHistoriqueDto(
        Long id,
        String ref,
        String titre,
        String ministere,
        String statut,
        Instant survenuLe,
        Instant clotureLe,
        boolean cloture,
        // Section V du compte rendu des amendements : nombre de tickets
        // iTop pour LE MINISTERE de cette ligne, sur la meme fenetre/les
        // memes filtres que le reste de la page. Pas d'equivalent "par
        // site" ici (a la difference d'IncidentHistoriqueDto) : les
        // tickets iTop n'ont pas de reference structuree a un site, voir
        // ItopClient - seul le ministere est fiable. Pas de "disponibilite"
        // non plus, cette notion n'a pas de sens sans population de sites.
        int nombreTicketsMinistere
) {}
