package bf.anptic.geoportail.service.backoffice;

import bf.anptic.geoportail.dto.CartographyItemDto;
import bf.anptic.geoportail.dto.CartographyUpdateRequest;
import bf.anptic.geoportail.dto.EquipmentAdminRequest;
import bf.anptic.geoportail.dto.EquipmentResponse;
import bf.anptic.geoportail.dto.SiteAdminRequest;
import bf.anptic.geoportail.dto.SiteAdminResponse;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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
                        site.getInfoAuSurvol(), site.getLatitude() != null && site.getLongitude() != null))
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
        site.setNetxmsNodeId(request.netxmsNodeId());
        site.setNiveaux(request.niveaux());

        if (site.getActif() == null) {
            site.setActif(true);
        }

        Site saved = siteRepository.save(site);
        auditService.record(auteur, isNew ? "Création site" : "Modification site",
                "site=" + saved.getSiteId() + " (" + saved.getNom() + ")");

        return toSiteResponse(saved);
    }

    public void setActive(String siteId, boolean active, String auteur, Authentication authentication) {
        verifierAccesSite(siteId, authentication);

        Site site = findSiteOrThrow(siteId);
        site.setActif(active);
        siteRepository.save(site);

        auditService.record(auteur, active ? "Réactivation site" : "Désactivation site", "site=" + siteId);
    }

    public EquipmentResponse addEquipment(String siteId, EquipmentAdminRequest request, String auteur, Authentication authentication) {
        verifierAccesSite(siteId, authentication);

        Site site = findSiteOrThrow(siteId);
        Equipment equipment = new Equipment();
        equipment.setSite(site);
        equipment.setEtageLabel(request.etageLabel());
        equipment.setType(Equipment.EquipmentType.valueOf(request.type()));
        equipment.setLibelleAffiche(request.libelleAffiche());
        equipment.setNetxmsObjectId(request.netxmsObjectId());

        Equipment saved = equipmentRepository.save(equipment);
        auditService.record(auteur, "Ajout équipement LAN",
                "site=" + siteId + " équipement=" + saved.getLibelleAffiche() + " (" + saved.getEtageLabel() + ")");

        return toEquipmentResponse(saved);
    }

    public void deleteEquipment(Long equipmentId, String auteur, Authentication authentication) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Équipement introuvable : " + equipmentId));

        verifierAccesSite(equipment.getSite().getSiteId(), authentication);

        equipmentRepository.deleteById(equipmentId);
        auditService.record(auteur, "Suppression équipement LAN", "equipmentId=" + equipmentId);
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
                site.getSiteId(), site.getNom(), site.getVille(), site.getRegionAdministrative(), site.getBatiment(),
                site.getLatitude(), site.getLongitude(), site.getContactDsiNom(), site.getContactDsiTelephone(),
                site.getNetxmsNodeId(), site.getNiveaux(), nombreEquipements, site.getActif()
        );
    }

    private EquipmentResponse toEquipmentResponse(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(), equipment.getSite().getSiteId(), equipment.getEtageLabel(),
                equipment.getType().name(), equipment.getLibelleAffiche(), equipment.getNetxmsObjectId()
        );
    }
}