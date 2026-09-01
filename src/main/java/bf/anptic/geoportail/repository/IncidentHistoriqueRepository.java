package bf.anptic.geoportail.repository;

import bf.anptic.geoportail.model.IncidentHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IncidentHistoriqueRepository extends JpaRepository<IncidentHistorique, Long> {

    // L'enregistrement OUVERT (finLe == null) pour un incident donne, s'il
    // existe deja - permet de distinguer un incident deja connu d'un
    // VRAI nouvel incident (cf. IncidentHistoryService.detecterEtEnregistrer).
    Optional<IncidentHistorique> findByIncidentKeyAndFinLeIsNull(String incidentKey);

    // Tous les incidents actuellement ouverts, tous sites confondus -
    // sert a detecter ceux qui viennent de se resoudre (plus dans la
    // liste des incidents actifs) pour leur poser une date de fin.
    List<IncidentHistorique> findAllByFinLeIsNull();

    // Pour la page Backoffice "Historique des incidents" : tous les
    // incidents qui CHEVAUCHENT la fenetre [fenetreDebut, fenetreFin[
    // (commence avant la fin de la fenetre, et toujours en cours ou
    // termine apres son debut), avec filtres optionnels (type, ministere,
    // recherche texte sur le site/la ville). Chaque parametre optionnel a
    // NULL desactive son filtre. L'etat "en cours"/"resolu" et la
    // pagination sont geres cote service (IncidentHistoryService), pas
    // ici, pour pouvoir calculer les statistiques (total/resolus/en
    // cours) sur le meme ensemble filtre avant de paginer.
    //
    // IMPORTANT : contrairement a type/ministere/recherche, fenetreDebut
    // et fenetreFin ne sont JAMAIS null (AdminIncidentController calcule
    // toujours une fenetre concrete, glissante ou pour un jour precis) -
    // il ne faut donc PAS les tester avec "IS NULL" ici. PostgreSQL a
    // besoin de deduire un type concret pour chaque parametre prepare ;
    // un parametre qui n'apparaitrait QUE dans un test "? is null" (comme
    // c'etait le cas ici pour fenetreDebut) n'a aucun indice de type dans
    // ce contexte et Postgres refuse la requete avec l'erreur "n'a pas pu
    // determiner le type de donnees du parametre $1".
    @Query("""
            SELECT i FROM IncidentHistorique i
            WHERE i.debutLe < :fenetreFin
              AND (i.finLe IS NULL OR i.finLe >= :fenetreDebut)
              AND (:type IS NULL OR i.type = :type)
              AND (:ministere IS NULL OR i.ministere = :ministere)
              AND (:recherche IS NULL OR LOWER(i.siteNom) LIKE :recherche OR LOWER(i.ville) LIKE :recherche)
            """)
    List<IncidentHistorique> rechercher(@Param("fenetreDebut") Instant fenetreDebut,
                                         @Param("fenetreFin") Instant fenetreFin,
                                         @Param("type") String type,
                                         @Param("ministere") String ministere,
                                         @Param("recherche") String recherche);
}