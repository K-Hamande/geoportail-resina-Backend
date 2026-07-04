package bf.anptic.geoportail.service.backoffice;

import bf.anptic.geoportail.dto.EquipmentAdminRequest;
import bf.anptic.geoportail.dto.EquipmentResponse;
import bf.anptic.geoportail.dto.SiteAdminRequest;
import bf.anptic.geoportail.dto.SiteAdminResponse;
import bf.anptic.geoportail.dto.CartographyItemDto;
import bf.anptic.geoportail.dto.CartographyUpdateRequest;
import bf.anptic.geoportail.model.Equipment;
import bf.anptic.geoportail.model.Site;
import bf.anptic.geoportail.repository.EquipmentRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import bf.anptic.geoportail.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminSiteService {

    private final SiteRepository siteRepository;
    private final EquipmentRepository equipmentRepository;
    private final AuditService auditService;

    public AdminSiteService(SiteRepository siteRepository,
                             EquipmentRepository equipmentRepository,
                             AuditService auditService) {
        this.siteRepository = siteRepository;
        this.equipmentRepository = equipmentRepository;
        this.auditService = auditService;
    }

    public List<SiteAdminResponse> listAllSites() {
        return siteRepository.findAll().stream()
                .map(this::toSiteResponse)
                .toList();
    }

    public SiteAdminResponse createOrUpdateSite(SiteAdminRequest request, String auteur) {
        boolean isNew = siteRepository.findById(request.siteId()).isEmpty();

        Site site = siteRepository.findById(request.siteId())
                .orElseGet(Site::new);

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

        auditService.record(auteur,
                isNew ? "Création site" : "Modification site",
                "site=" + saved.getSiteId() + " (" + saved.getNom() + ")");

        return toSiteResponse(saved);
    }

    public void setActive(String siteId, boolean active, String auteur) {
        Site site = findSiteOrThrow(siteId);
        site.setActif(active);
        siteRepository.save(site);

        auditService.record(auteur,
                active ? "Réactivation site" : "Désactivation site",
                "site=" + siteId);
    }


    public void updateCartography(String siteId, CartographyUpdateRequest request, String auteur) {
        Site site = findSiteOrThrow(siteId);
        site.setLatitude(request.latitude());
        site.setLongitude(request.longitude());
        site.setInfoAuSurvol(request.infoAuSurvol());
        siteRepository.save(site);

        auditService.record(auteur, "Modification coordonnées GPS",
                "site=" + siteId + " lat=" + request.latitude() + " lon=" + request.longitude());
    }

    public List<CartographyItemDto> listCartography() {
        return siteRepository.findAll().stream()
                .map(site -> new CartographyItemDto(
                        site.getSiteId(),
                        site.getNom(),
                        site.getLatitude(),
                        site.getLongitude(),
                        site.getInfoAuSurvol(),
                        site.getLatitude() != null && site.getLongitude() != null
                ))
                .toList();
    }



    public EquipmentResponse addEquipment(String siteId, EquipmentAdminRequest request, String auteur) {
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

    public List<EquipmentResponse> listEquipments(String siteId) {
        return equipmentRepository.findBySite_SiteId(siteId).stream()
                .map(this::toEquipmentResponse)
                .toList();
    }

    public void deleteEquipment(Long equipmentId, String auteur) {
        equipmentRepository.deleteById(equipmentId);
        auditService.record(auteur, "Suppression équipement LAN", "equipmentId=" + equipmentId);
    }

    private Site findSiteOrThrow(String siteId) {
        return siteRepository.findById(siteId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Site introuvable : " + siteId));
    }

    // ---- Mapping entite -> DTO ----

    private SiteAdminResponse toSiteResponse(Site site) {
        int nombreEquipements = equipmentRepository.findBySite_SiteId(site.getSiteId()).size();

        return new SiteAdminResponse(
                site.getSiteId(),
                site.getNom(),
                site.getVille(),
                site.getRegionAdministrative(),
                site.getBatiment(),
                site.getLatitude(),
                site.getLongitude(),
                site.getContactDsiNom(),
                site.getContactDsiTelephone(),
                site.getNetxmsNodeId(),
                site.getNiveaux(),
                nombreEquipements,
                site.getActif()
        );
    }

    private EquipmentResponse toEquipmentResponse(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getSite().getSiteId(),   // on prend juste l'id, pas l'objet Site entier
                equipment.getEtageLabel(),
                equipment.getType().name(),         // .name() convertit l'enum en texte simple
                equipment.getLibelleAffiche(),
                equipment.getNetxmsObjectId()
        );
    }
}