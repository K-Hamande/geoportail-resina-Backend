package bf.anptic.geoportail.controller.backoffice;

import bf.anptic.geoportail.dto.IncidentHistoriqueDto;
import bf.anptic.geoportail.dto.IncidentHistoriquePageDto;
import bf.anptic.geoportail.dto.ItopTicketHistoriqueDto;
import bf.anptic.geoportail.dto.ItopTicketHistoriquePageDto;
import bf.anptic.geoportail.service.IncidentHistoryService;
import bf.anptic.geoportail.service.ItopTicketHistoryService;
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
    private final ItopTicketHistoryService itopTicketHistoryService;

    public AdminIncidentController(IncidentHistoryService incidentHistoryService,
                                    ItopTicketHistoryService itopTicketHistoryService) {
        this.incidentHistoryService = incidentHistoryService;
        this.itopTicketHistoryService = itopTicketHistoryService;
    }

    // Fenetre de dates commune aux 2 onglets (alertes reseau / incidents
    // iTop) - meme logique "date precise" vs "jours glissants" que
    // historique()/exporterHistorique() ci-dessous.
    private Instant[] calculerFenetre(Integer jours, String date) {
        if (date != null && !date.isBlank()) {
            LocalDate jour = LocalDate.parse(date);
            return new Instant[]{
                    jour.atStartOfDay(FUSEAU).toInstant(),
                    jour.plusDays(1).atStartOfDay(FUSEAU).toInstant()
            };
        }
        int nombreJours = (jours != null && jours > 0) ? jours : 30;
        Instant fin = Instant.now();
        return new Instant[]{fin.minus(nombreJours, ChronoUnit.DAYS), fin};
    }

    // Onglet "Incidents iTop" - meme principe de filtres que /historique
    // (periode/date, ministere, recherche), "statut" remplace "type"+"etat"
    // (valeurs iTop brutes : ongoing/resolved/closed, cf. TicketIncidentDto
    // cote decideur).
    @GetMapping("/itop-historique")
    public ItopTicketHistoriquePageDto itopHistorique(
            @RequestParam(required = false) Integer jours,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String ministere,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer taille) {

        Instant[] fenetre = calculerFenetre(jours, date);
        int numeroPage = (page != null && page >= 0) ? page : 0;
        int taillePage = (taille != null && taille > 0) ? taille : 20;

        return itopTicketHistoryService.rechercher(
                fenetre[0], fenetre[1], statut, ministere, recherche, numeroPage, taillePage);
    }

    @GetMapping("/itop-historique/export")
    public ResponseEntity<byte[]> exporterItopHistorique(
            @RequestParam(required = false) Integer jours,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String ministere,
            @RequestParam(required = false) String recherche) {

        Instant[] fenetre = calculerFenetre(jours, date);
        List<ItopTicketHistoriqueDto> tickets = itopTicketHistoryService.rechercherPourExport(
                fenetre[0], fenetre[1], statut, ministere, recherche);

        String csv = construireCsvItop(tickets);
        byte[] contenu = ajouterBomUtf8(csv);
        String nomFichier = "historique-incidents-itop-"
                + LocalDate.now(FUSEAU).format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenu);
    }

    private String construireCsvItop(List<ItopTicketHistoriqueDto> tickets) {
        StringBuilder csv = new StringBuilder();
        csv.append("Reference;Titre;Ministere;Statut;Debut;Cloture\r\n");
        for (ItopTicketHistoriqueDto t : tickets) {
            csv.append(echapper(t.ref())).append(';')
                    .append(echapper(t.titre())).append(';')
                    .append(echapper(t.ministere())).append(';')
                    .append(echapper(t.statut())).append(';')
                    .append(FORMAT_DATE_HEURE_CSV.format(t.survenuLe())).append(';')
                    .append(t.clotureLe() != null ? FORMAT_DATE_HEURE_CSV.format(t.clotureLe()) : "")
                    .append("\r\n");
        }
        return csv.toString();
    }

    private byte[] ajouterBomUtf8(String csv) {
        byte[] corpsCsv = csv.getBytes(StandardCharsets.UTF_8);
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] contenu = new byte[bom.length + corpsCsv.length];
        System.arraycopy(bom, 0, contenu, 0, bom.length);
        System.arraycopy(corpsCsv, 0, contenu, bom.length, corpsCsv.length);
        return contenu;
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