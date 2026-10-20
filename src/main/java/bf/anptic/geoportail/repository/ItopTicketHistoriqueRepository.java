package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.ItopTicketHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ItopTicketHistoriqueRepository extends JpaRepository<ItopTicketHistorique, Long> {

    // Retrouve l'enregistrement local correspondant a un ticket iTop pour
    // decider, lors de la synchronisation, s'il faut creer ou mettre a jour.
    Optional<ItopTicketHistorique> findByRef(String ref);

    // Point de depart de la synchronisation incrementale : ne redemande a
    // iTop que les tickets modifies APRES cette date (cf.
    // ItopTicketHistoryService.synchroniser). Vide (table jamais encore
    // synchronisee) => synchronisation complete.
    @Query("SELECT MAX(t.derniereMajItop) FROM ItopTicketHistorique t")
    Instant trouverDerniereMajConnue();

    // Pour la page Backoffice "Historique des incidents" - onglet "Incidents
    // iTop" : tickets dont la fenetre de vie [survenuLe, clotureLe[
    // chevauche [fenetreDebut, fenetreFin[ (meme logique que
    // IncidentHistoriqueRepository.rechercher), avec filtres optionnels
    // ministere/recherche. Le filtre "statut" (ongoing/resolved/closed) et
    // la pagination sont geres cote service, comme pour les alertes reseau.
    // BUG CORRIGE : "LOWER(:ministere)" (fonction appliquee SUR LE PARAMETRE
    // lui-meme, pas seulement sur la colonne) faisait echouer la requete
    // des que ministere valait null ("la fonction lower(bytea) n'existe
    // pas") - le pilote JDBC Postgres n'arrivait pas a deduire un type
    // text pour ce parametre dans ce contexte et le typait par defaut en
    // bytea. Le parametre est maintenant deja normalise en minuscules cote
    // Java (ItopTicketHistoryService) et compare tel quel, comme deja fait
    // pour "recherche" ci-dessous (seule la colonne passe par LOWER()).
    @Query("""
            SELECT t FROM ItopTicketHistorique t
            WHERE t.survenuLe < :fenetreFin
              AND (t.clotureLe IS NULL OR t.clotureLe >= :fenetreDebut)
              AND (:ministere IS NULL OR LOWER(t.ministere) = :ministere)
              AND (:recherche IS NULL OR LOWER(t.titre) LIKE :recherche OR LOWER(t.ref) LIKE :recherche)
            """)
    List<ItopTicketHistorique> rechercher(@Param("fenetreDebut") Instant fenetreDebut,
                                           @Param("fenetreFin") Instant fenetreFin,
                                           @Param("ministere") String ministere,
                                           @Param("recherche") String recherche);
}
