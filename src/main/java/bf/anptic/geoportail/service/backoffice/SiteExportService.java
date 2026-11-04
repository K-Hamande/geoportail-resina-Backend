package bf.anptic.geoportail.service.backoffice;

import bf.anptic.geoportail.dto.SiteAdminResponse;
import bf.anptic.geoportail.model.enums.SiteGravite;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

// Amendement "Tableau de bord et gestion des sites" : export de la liste
// des sites en Excel (.xlsx) et PDF, en remplacement du bouton
// "Importer" (CSV). Opere sur la liste DEJA FILTREE cote frontend (voir
// AdminSiteController.exporterSites : le frontend envoie les siteId
// correspondant aux filtres/recherche actifs sur la page, pas de
// re-filtrage ici) - "ce que vous voyez est ce que vous exportez".
//
// Colonnes : nom, localisation (ville/province/region), ministere, noeud
// NetXMS, nombre d'equipements, statut (actif/inactif) et gravite
// (Critique/Majeur/Moyen/Normal/Inconnu, reutilise la meme classification
// que le tableau de bord - voir SiteStatusSnapshotService/SiteGravite).
// PAS de colonne "disponibilite" : ce calcul (fenetre glissante sur
// l'historique d'incidents) n'existe actuellement que pour la page
// "Historique des incidents", pas pour "Gestion des sites" - l'ajouter
// ici demanderait une nouvelle agregation dediee, hors scope de ce
// premier export.
@Service
public class SiteExportService {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] ENTETES = {
            "Site", "Ville", "Province", "Région", "Ministère",
            "Nœud NetXMS", "Équipements", "Statut", "Gravité"
    };

    public byte[] exporterExcel(List<SiteAdminResponse> sites, Map<String, SiteGravite> graviteParSite) {
        try (XSSFWorkbook classeur = new XSSFWorkbook(); ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            Sheet feuille = classeur.createSheet("Sites");

            CellStyle styleEntete = classeur.createCellStyle();
            org.apache.poi.ss.usermodel.Font policeEntete = classeur.createFont();
            policeEntete.setBold(true);
            styleEntete.setFont(policeEntete);

            Row ligneEntete = feuille.createRow(0);
            for (int i = 0; i < ENTETES.length; i++) {
                Cell cellule = ligneEntete.createCell(i);
                cellule.setCellValue(ENTETES[i]);
                cellule.setCellStyle(styleEntete);
            }

            int numeroLigne = 1;
            for (SiteAdminResponse site : sites) {
                Row ligne = feuille.createRow(numeroLigne++);
                ecrireLigne(ligne, site, graviteParSite);
            }

            for (int i = 0; i < ENTETES.length; i++) {
                feuille.autoSizeColumn(i);
            }

            classeur.write(sortie);
            return sortie.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la génération du fichier Excel", e);
        }
    }

    private void ecrireLigne(Row ligne, SiteAdminResponse site, Map<String, SiteGravite> graviteParSite) {
        int col = 0;
        ligne.createCell(col++).setCellValue(valeurOuVide(site.nom()));
        ligne.createCell(col++).setCellValue(valeurOuVide(site.ville()));
        ligne.createCell(col++).setCellValue(valeurOuVide(site.province()));
        ligne.createCell(col++).setCellValue(valeurOuVide(site.regionAdministrative()));
        ligne.createCell(col++).setCellValue(valeurOuVide(site.ministere()));
        ligne.createCell(col++).setCellValue(site.netxmsNodeId() != null ? site.netxmsNodeId().toString() : "—");
        ligne.createCell(col++).setCellValue(site.nombreEquipements() != null ? site.nombreEquipements() : 0);
        ligne.createCell(col++).setCellValue(Boolean.TRUE.equals(site.actif()) ? "Actif" : "Inactif");
        ligne.createCell(col).setCellValue(libelleGravite(graviteParSite.get(site.siteId())));
    }

    public byte[] exporterPdf(List<SiteAdminResponse> sites, Map<String, SiteGravite> graviteParSite) {
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        try (ByteArrayOutputStream sortie = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, sortie);
            document.open();

            Font policeTitre = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font policeSousTitre = new Font(Font.HELVETICA, 9, Font.NORMAL, java.awt.Color.GRAY);
            Font policeEntete = new Font(Font.HELVETICA, 9, Font.BOLD, java.awt.Color.WHITE);
            Font policeCellule = new Font(Font.HELVETICA, 8, Font.NORMAL);

            Paragraph titre = new Paragraph("GéoPortail RESINA — Liste des sites", policeTitre);
            titre.setAlignment(Element.ALIGN_CENTER);
            document.add(titre);

            Paragraph sousTitre = new Paragraph(
                    sites.size() + " site(s) — exporté le " + LocalDate.now().format(FORMAT_DATE), policeSousTitre);
            sousTitre.setAlignment(Element.ALIGN_CENTER);
            sousTitre.setSpacingAfter(14);
            document.add(sousTitre);

            PdfPTable table = new PdfPTable(ENTETES.length);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{18, 12, 12, 12, 16, 10, 8, 8, 10});

            for (String entete : ENTETES) {
                PdfPCell cellule = new PdfPCell(new Paragraph(entete, policeEntete));
                cellule.setBackgroundColor(new java.awt.Color(10, 61, 122));
                cellule.setPadding(5);
                table.addCell(cellule);
            }

            for (SiteAdminResponse site : sites) {
                ajouterLignePdf(table, site, graviteParSite, policeCellule);
            }

            document.add(table);
            document.close();
            return sortie.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Erreur lors de la génération du fichier PDF", e);
        }
    }

    private void ajouterLignePdf(PdfPTable table, SiteAdminResponse site, Map<String, SiteGravite> graviteParSite,
                                  Font police) {
        String[] valeurs = {
                valeurOuVide(site.nom()),
                valeurOuVide(site.ville()),
                valeurOuVide(site.province()),
                valeurOuVide(site.regionAdministrative()),
                valeurOuVide(site.ministere()),
                site.netxmsNodeId() != null ? site.netxmsNodeId().toString() : "—",
                site.nombreEquipements() != null ? site.nombreEquipements().toString() : "0",
                Boolean.TRUE.equals(site.actif()) ? "Actif" : "Inactif",
                libelleGravite(graviteParSite.get(site.siteId()))
        };
        for (String valeur : valeurs) {
            PdfPCell cellule = new PdfPCell(new Paragraph(valeur, police));
            cellule.setPadding(4);
            table.addCell(cellule);
        }
    }

    private String valeurOuVide(String valeur) {
        return (valeur != null && !valeur.isBlank()) ? valeur : "—";
    }

    private String libelleGravite(SiteGravite gravite) {
        if (gravite == null) return "—";
        return switch (gravite) {
            case CRITIQUE -> "Critique";
            case MAJEUR -> "Majeur";
            case MOYEN -> "Moyen";
            case NORMAL -> "Normal";
            case INCONNU -> "Inconnu";
        };
    }
}
