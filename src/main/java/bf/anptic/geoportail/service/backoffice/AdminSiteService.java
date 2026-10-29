package bf.anptic.geoportail.service.backoffice;

import bf.anptic.geoportail.dto.CartographyItemDto;
import bf.anptic.geoportail.dto.CartographyUpdateRequest;
import bf.anptic.geoportail.dto.EquipmentFloorAssignmentRequest;
import bf.anptic.geoportail.dto.EquipmentResponse;
import bf.anptic.geoportail.dto.SiteAdminRequest;
import bf.anptic.geoportail.dto.SiteAdminResponse;
import bf.anptic.geoportail.dto.SiteImportResultDto;
import bf.anptic.geoportail.dto.SiteImportResultDto.SiteImportErreurDto;
import bf.anptic.geoportail.model.AdminUser;
import bf.anptic.geoportail.model.Equipment;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.repository.AdminUserRepository;
import bf.anptic.geoportail.repository.EquipmentRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import bf.anptic.geoportail.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminSiteService {

    private final SiteRepository siteRepository;
    private final EquipmentRepository equipmentRepository;
    private final AuditService auditService;
    private final AdminUserRepository adminUserRepository;

    public AdminSiteService(SiteRepository siteRepository,
                             EquipmentRepository equipmentRepository,
                             AuditService auditService,
                             AdminUserRepository adminUserRepository) {
        this.siteRepository = siteRepository;
        this.equipmentRepository = equipmentRepository;
        this.auditService = auditService;
        this.adminUserRepository = adminUserRepository;
    }

    // ---- LECTURE : filtree selon les droits de l'utilisateur connecte ----

    public List<SiteAdminResponse> listAllSites(Authentication authentication) {
        List<Site> sites = filtrerSitesAccessibles(siteRepository.findAll(), authentication);
        return sites.stream().map(this::toSiteResponse).toList();
    }

    public List<EquipmentResponse> listEquipments(String siteId, Authentication authentication) {
        verifierAccesSite(siteId, authentication);
        return equipmentRepository.findBySite_SiteId(siteId).stream()
                .map(this::toEquipmentResponse)
                .toList();
    }

    public List<CartographyItemDto> listCartography(Authentication authentication) {
        List<Site> sites = filtrerSitesAccessibles(siteRepository.findAll(), authentication);
        return sites.stream()
                .map(site -> new CartographyItemDto(
                        site.getSiteId(), site.getNom(), site.getLatitude(), site.getLongitude(),
                        site.getInfoAuSurvol(), site.getLatitude() != null && site.getLongitude() != null,
                        site.getVille(), site.getProvince(), site.getRegionAdministrative(), site.getMinistere()))
                .toList();
    }

    // ---- ECRITURE : verifiee au cas par cas ----

    public SiteAdminResponse createOrUpdateSite(SiteAdminRequest request, String auteur, Authentication authentication) {
        verifierAccesSite(request.siteId(), authentication);

        boolean isNew = siteRepository.findById(request.siteId()).isEmpty();
        Site site = siteRepository.findById(request.siteId()).orElseGet(Site::new);

        site.setSiteId(request.siteId());
        site.setNom(request.nom());
        site.setVille(request.ville());
        site.setRegionAdministrative(request.regionAdministrative());
        site.setBatiment(request.batiment());
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        site.setContactDsiNom(request.contactDsiNom());
        site.setContactDsiTelephone(request.contactDsiTelephone());
        site.setContactDsiEmail(request.contactDsiEmail());
        site.setNetxmsNodeId(request.netxmsNodeId());
        site.setNiveaux(request.niveaux());
        if (request.ministere() != null) site.setMinistere(request.ministere());
        if (request.province() != null) site.setProvince(request.province());

        if (site.getActif() == null) {
            site.setActif(true);
        }

        Site saved = siteRepository.save(site);
        auditService.record(auteur, isNew ? "Création site" : "Modification site",
                "site=" + saved.getSiteId() + " (" + saved.getNom() + ")");

        return toSiteResponse(saved);
    }

    // Import CSV de sites (section VI du compte rendu des amendements),
    // en complement de la synchro automatique NetXMS - utile pour des
    // sites pas encore remontes par NetXMS, ou pour corriger/completer en
    // masse (ministere, contact DSI...). Reutilise createOrUpdateSite(...)
    // ligne par ligne (meme validation, meme controle d'acces, meme
    // audit) : UNE ligne en erreur n'annule PAS les autres, elle est
    // simplement listee a part pour correction/reimport cible.
    //
    // Colonnes attendues (entete obligatoire, ordre libre - identifiees
    // par nom, insensible a la casse) : siteId;nom (obligatoires),
    // ville;province;regionAdministrative;ministere;batiment;latitude;
    // longitude;contactDsiNom;contactDsiTelephone;contactDsiEmail;
    // netxmsNodeId;niveaux (tous optionnels). Delimiteur ";", meme
    // convention que les exports CSV deja existants dans ce projet.
    public SiteImportResultDto importerSitesCsv(MultipartFile fichier, String auteur, Authentication authentication) {
        List<SiteImportErreurDto> erreurs = new ArrayList<>();
        int crees = 0;
        int modifies = 0;
        int lignesTraitees = 0;

        try (BufferedReader lecteur = new BufferedReader(
                new InputStreamReader(fichier.getInputStream(), StandardCharsets.UTF_8))) {

            String ligneEntete = lecteur.readLine();
            if (ligneEntete == null || ligneEntete.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier CSV vide.");
            }
            // BOM UTF-8 eventuel (fichier produit par Excel) : retire du
            // tout premier caractere de l'entete, sinon la 1ere colonne
            // ("siteId" en general) ne matcherait jamais.
            if (ligneEntete.startsWith("﻿")) {
                ligneEntete = ligneEntete.substring(1);
            }

            String[] entetes = parserLigneCsv(ligneEntete);
            Map<String, Integer> indexParColonne = new HashMap<>();
            for (int i = 0; i < entetes.length; i++) {
                indexParColonne.put(entetes[i].trim().toLowerCase(), i);
            }
            if (!indexParColonne.containsKey("siteid") || !indexParColonne.containsKey("nom")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le fichier CSV doit contenir au minimum les colonnes 'siteId' et 'nom'.");
            }

            String ligne;
            int numeroLigne = 1; // ligne 1 = l'entete ; les donnees commencent a 2
            while ((ligne = lecteur.readLine()) != null) {
                numeroLigne++;
                if (ligne.isBlank()) continue;
                lignesTraitees++;

                try {
                    String[] champs = parserLigneCsv(ligne);
                    SiteAdminRequest requete = construireRequeteDepuisCsv(champs, indexParColonne);
                    boolean isNew = siteRepository.findById(requete.siteId()).isEmpty();
                    createOrUpdateSite(requete, auteur, authentication);
                    if (isNew) crees++; else modifies++;
                } catch (ResponseStatusException e) {
                    erreurs.add(new SiteImportErreurDto(numeroLigne, e.getReason()));
                } catch (Exception e) {
                    erreurs.add(new SiteImportErreurDto(numeroLigne, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible de lire le fichier : " + e.getMessage());
        }

        auditService.record(auteur, "Import CSV de sites",
                lignesTraitees + " ligne(s) traitée(s), " + crees + " créé(s), "
                        + modifies + " modifié(s), " + erreurs.size() + " erreur(s)");

        return new SiteImportResultDto(lignesTraitees, crees, modifies, erreurs);
    }

    private SiteAdminRequest construireRequeteDepuisCsv(String[] champs, Map<String, Integer> index) {
        String siteId = valeurCsv(champs, index, "siteid");
        String nom = valeurCsv(champs, index, "nom");
        if (siteId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Colonne 'siteId' manquante ou vide.");
        }
        if (nom == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Colonne 'nom' manquante ou vide.");
        }

        return new SiteAdminRequest(
                siteId,
                nom,
                valeurCsv(champs, index, "ville"),
                valeurCsv(champs, index, "regionadministrative"),
                valeurCsv(champs, index, "batiment"),
                valeurDoubleCsv(champs, index, "latitude"),
                valeurDoubleCsv(champs, index, "longitude"),
                valeurCsv(champs, index, "contactdsinom"),
                valeurCsv(champs, index, "contactdsitelephone"),
                valeurCsv(champs, index, "contactdsiemail"),
                valeurEntierCsv(champs, index, "netxmsnodeid"),
                valeurEntierCsv(champs, index, "niveaux"),
                valeurCsv(champs, index, "ministere"),
                valeurCsv(champs, index, "province")
        );
    }

    private static String valeurCsv(String[] champs, Map<String, Integer> index, String colonne) {
        Integer i = index.get(colonne);
        if (i == null || i >= champs.length) return null;
        String valeur = champs[i].trim();
        return valeur.isBlank() ? null : valeur;
    }

    private static Double valeurDoubleCsv(String[] champs, Map<String, Integer> index, String colonne) {
        String valeur = valeurCsv(champs, index, colonne);
        if (valeur == null) return null;
        try {
            return Double.parseDouble(valeur.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valeur numérique invalide pour '" + colonne + "' : " + valeur);
        }
    }

    private static Integer valeurEntierCsv(String[] champs, Map<String, Integer> index, String colonne) {
        String valeur = valeurCsv(champs, index, colonne);
        if (valeur == null) return null;
        try {
            return Integer.parseInt(valeur);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Valeur entière invalide pour '" + colonne + "' : " + valeur);
        }
    }

    // Parseur CSV minimal (delimiteur ";", champs entre guillemets
    // doubles supportes avec echappement '""') - pas de dependance
    // externe pour un besoin aussi simple, meme convention que les
    // exports CSV deja ecrits a la main dans ce projet.
    private static String[] parserLigneCsv(String ligne) {
        List<String> champs = new ArrayList<>();
        StringBuilder courant = new StringBuilder();
        boolean dansGuillemets = false;

        for (int i = 0; i < ligne.length(); i++) {
            char c = ligne.charAt(i);
            if (dansGuillemets) {
                if (c == '"') {
                    if (i + 1 < ligne.length() && ligne.charAt(i + 1) == '"') {
                        courant.append('"');
                        i++;
                    } else {
                        dansGuillemets = false;
                    }
                } else {
                    courant.append(c);
                }
            } else if (c == '"') {
                dansGuillemets = true;
            } else if (c == ';') {
                champs.add(courant.toString());
                courant.setLength(0);
            } else {
                courant.append(c);
            }
        }
        champs.add(courant.toString());
        return champs.toArray(new String[0]);
    }

    public void setActive(String siteId, boolean active, String auteur, Authentication authentication) {
        verifierAccesSite(siteId, authentication);

        Site site = findSiteOrThrow(siteId);
        site.setActif(active);
        siteRepository.save(site);

        auditService.record(auteur, active ? "Réactivation site" : "Désactivation site", "site=" + siteId);
    }

    // Les equipements sont decouverts automatiquement depuis NetXMS
    // (EquipmentSyncService) - le backoffice ne peut plus qu'assigner
    // un etage a un equipement deja synchronise.
    public EquipmentResponse assignFloor(Long equipmentId, EquipmentFloorAssignmentRequest request, String auteur, Authentication authentication) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipement introuvable : " + equipmentId));

        verifierAccesSite(equipment.getSite().getSiteId(), authentication);

        equipment.setEtageLabel(request.etageLabel());
        equipment.setLibelleAffiche(request.libelleAffiche());
        Equipment saved = equipmentRepository.save(equipment);

        auditService.record(auteur, "Modification équipement",
                "equipmentId=" + equipmentId + " étage=" + request.etageLabel() + " libellé=" + request.libelleAffiche());

        return toEquipmentResponse(saved);
    }

    public void updateCartography(String siteId, CartographyUpdateRequest request, String auteur, Authentication authentication) {
        verifierAccesSite(siteId, authentication);

        Site site = findSiteOrThrow(siteId);
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        site.setInfoAuSurvol(request.infoAuSurvol());
        siteRepository.save(site);

        auditService.record(auteur, "Modification coordonnées GPS",
                "site=" + siteId + " lat=" + request.latitude() + " lon=" + request.longitude());
    }

    // ---- Utilitaires de controle d'acces (§3.2.6b : droits par site ou globaux) ----

    /**
     * Ne garde, dans la liste fournie, que les sites accessibles a
     * l'utilisateur connecte (tous, si SUPER_ADMIN ou aucune restriction
     * definie sur son compte).
     */
    private List<Site> filtrerSitesAccessibles(List<Site> sites, Authentication authentication) {
        boolean superAdmin = estSuperAdmin(authentication);
        if (superAdmin) {
            return sites;
        }

        AdminUser user = adminUserRepository.findByLoginAndActifTrue(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte introuvable"));

        if (user.aAccesGlobal()) {
            return sites;
        }

        return sites.stream()
                .filter(site -> user.peutAccederAuSite(site.getSiteId()))
                .toList();
    }

    /** Leve une 403 si l'utilisateur connecte n'a pas le droit d'agir sur ce site. */
    private void verifierAccesSite(String siteId, Authentication authentication) {
        if (estSuperAdmin(authentication)) {
            return;
        }

        AdminUser user = adminUserRepository.findByLoginAndActifTrue(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte introuvable"));

        if (!user.peutAccederAuSite(siteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n'avez pas les droits sur le site : " + siteId);
        }
    }

    private boolean estSuperAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private Site findSiteOrThrow(String siteId) {
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Site introuvable : " + siteId));
    }

    private SiteAdminResponse toSiteResponse(Site site) {
        int nombreEquipements = (int) equipmentRepository.countBySite_SiteId(site.getSiteId());
        return new SiteAdminResponse(
                site.getSiteId(), site.getNom(), site.getVille(), site.getProvince(), site.getRegionAdministrative(),
                site.getMinistere(), site.getBatiment(),
                site.getLatitude(), site.getLongitude(), site.getContactDsiNom(), site.getContactDsiTelephone(),
                site.getContactDsiEmail(), site.getNetxmsNodeId(), site.getNiveaux(), nombreEquipements, site.getActif()
        );
    }

    private EquipmentResponse toEquipmentResponse(Equipment eq) {
        return new EquipmentResponse(
                eq.getId(), eq.getSite().getSiteId(), eq.getEtageLabel(),
                eq.getType() != null ? eq.getType().name() : null,
                eq.getLibelleAffiche(), eq.getNomTechniqueNetxms(), eq.getNetxmsObjectId()
        );
    }
}