package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.IncidentHistoriqueDto;
import bf.anptic.geoportail.dto.IncidentHistoriquePageDto;
import bf.anptic.geoportail.service.IncidentHistoryService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

// Page Backoffice "Historique des incidents" : accessible a tout
// utilisateur Backoffice authentifie (pas de @PreAuthorize par role), sur
// le meme principe que les autres ecrans de consultation (Supervision,
// Sites...) - reserve seulement la gestion des COMPTES a SUPER_ADMIN
// (voir AdminUserController).
@RestController
@RequestMapping("/backoffice/api/v1/incidents")
public class AdminIncidentController {

    // Le reseau RESINA est au Burkina Faso (UTC+0, pas de changement
    // d'heure) - meme fuseau que AlertEmailService pour les dates
    // affichees dans les emails.
    private static final ZoneId FUSEAU = ZoneId.of("Africa/Ouagadougou");

    private final IncidentHistoryService incidentHistoryService;

    public AdminIncidentController(IncidentHistoryService incidentHistoryService) {
        this.incidentHistoryService = incidentHistoryService;
    }

    // Deux facons de choisir la fenetre de dates :
    //  - "date" (format YYYY-MM-DD) : un jour precis, ex. 2025-02-10 ->
    //    tous les incidents qui etaient en cours ce jour-la.
    //  - "jours" (defaut 30) : une fenetre glissante des N derniers jours
    //    jusqu'a maintenant. Ignore si "date" est fourni.
    // "etat" : "EN_COURS" ou "RESOLU" pour filtrer, absent = tous.
    @GetMapping("/historique")
    public IncidentHistoriquePageDto historique(
            @RequestParam(required = false) Integer jours,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String etat,
            @RequestParam(required = false) String ministere,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer taille) {

        Instant fenetreDebut;
        Instant fenetreFin;

        if (date != null && !date.isBlank()) {
            LocalDate jour = LocalDate.parse(date);
            fenetreDebut = jour.atStartOfDay(FUSEAU).toInstant();
            fenetreFin = jour.plusDays(1).atStartOfDay(FUSEAU).toInstant();
        } else {
            int nombreJours = (jours != null && jours > 0) ? jours : 30;
            fenetreFin = Instant.now();
            fenetreDebut = fenetreFin.minus(nombreJours, ChronoUnit.DAYS);
        }

        int numeroPage = (page != null && page >= 0) ? page : 0;
        int taillePage = (taille != null && taille > 0) ? taille : 20;

        return incidentHistoryService.rechercher(
                fenetreDebut, fenetreFin, type, etat, ministere, recherche, numeroPage, taillePage);
    }

    // Export CSV de TOUT l'historique correspondant aux filtres actifs sur
    // la page (memes parametres que /historique, sans page/taille - pas de
    // pagination ici, on veut tout). Delimiteur ";" et BOM UTF-8 en tete
    // pour qu'Excel (locale FR, deja utilisee a l'ANPTIC) ouvre le fichier
    // directement avec les accents corrects, sans passer par l'assistant
    // d'import.
    @GetMapping("/historique/export")
    public ResponseEntity<byte[]> exporterHistorique(
            @RequestParam(required = false) Integer jours,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String etat,
            @RequestParam(required = false) String ministere,
            @RequestParam(required = false) String recherche) {

        Instant fenetreDebut;
        Instant fenetreFin;

        if (date != null && !date.isBlank()) {
            LocalDate jour = LocalDate.parse(date);
            fenetreDebut = jour.atStartOfDay(FUSEAU).toInstant();
            fenetreFin = jour.plusDays(1).atStartOfDay(FUSEAU).toInstant();
        } else {
            int nombreJours = (jours != null && jours > 0) ? jours : 30;
            fenetreFin = Instant.now();
            fenetreDebut = fenetreFin.minus(nombreJours, ChronoUnit.DAYS);
        }

        List<IncidentHistoriqueDto> incidents = incidentHistoryService.rechercherPourExport(
                fenetreDebut, fenetreFin, type, etat, ministere, recherche);

        String csv = construireCsv(incidents);
        byte[] corpsCsv = csv.getBytes(StandardCharsets.UTF_8);
        // BOM UTF-8 (EF BB BF) en tete de fichier - indispensable pour
        // qu'Excel detecte l'encodage tout seul et affiche les accents
        // correctement, sinon il suppose du Windows-1252 par defaut.
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] contenu = new byte[bom.length + corpsCsv.length];
        System.arraycopy(bom, 0, contenu, 0, bom.length);
        System.arraycopy(corpsCsv, 0, contenu, bom.length, corpsCsv.length);

        String nomFichier = "historique-incidents-"
                + LocalDate.now(FUSEAU).format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenu);
    }

    private static final DateTimeFormatter FORMAT_DATE_HEURE_CSV =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(FUSEAU);

    private String construireCsv(List<IncidentHistoriqueDto> incidents) {
        StringBuilder csv = new StringBuilder();
        csv.append("Site;Ville;Ministere;Type;Statut;Etat;Debut;Fin;Duree (minutes)\r\n");
        for (IncidentHistoriqueDto i : incidents) {
            csv.append(echapper(i.siteNom())).append(';')
                    .append(echapper(i.ville())).append(';')
                    .append(echapper(i.ministere())).append(';')
                    .append(echapper(i.type())).append(';')
                    .append(echapper(i.statut())).append(';')
                    .append(i.enCours() ? "En cours" : "Resolu").append(';')
                    .append(FORMAT_DATE_HEURE_CSV.format(i.debutLe())).append(';')
                    .append(i.finLe() != null ? FORMAT_DATE_HEURE_CSV.format(i.finLe()) : "").append(';')
                    .append(i.dureeMinutes())
                    .append("\r\n");
        }
        return csv.toString();
    }

    // Protege contre les valeurs contenant le delimiteur ";", des
    // guillemets ou un retour a la ligne (peu probable ici - noms de site/
    // ville fixes en base - mais reste correct si ca change un jour).
    private String echapper(String valeur) {
        if (valeur == null) return "";
        if (valeur.contains(";") || valeur.contains("\"") || valeur.contains("\n")) {
            return "\"" + valeur.replace("\"", "\"\"") + "\"";
        }
        return valeur;
    }
}