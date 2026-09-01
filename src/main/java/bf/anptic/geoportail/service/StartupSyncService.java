package bf.anptic.geoportail.service;

import bf.anptic.geoportail.service.backoffice.EquipmentSyncService;
import bf.anptic.geoportail.service.backoffice.NetxmsSiteImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

// Synchronise automatiquement les sites et les equipements depuis
// NetXMS au demarrage de l'application — utile sur un nouvel
// environnement (nouveau poste dev, nouveau serveur) pour ne pas
// avoir a lancer la synchro manuellement depuis le Backoffice.
// La synchro nocturne planifiee (2h00/2h15) prend le relais ensuite.
@Service
public class StartupSyncService {

    private static final Logger log = LoggerFactory.getLogger(StartupSyncService.class);

    private final NetxmsSiteImportService netxmsSiteImportService;
    private final EquipmentSyncService equipmentSyncService;

    public StartupSyncService(NetxmsSiteImportService netxmsSiteImportService,
                               EquipmentSyncService equipmentSyncService) {
        this.netxmsSiteImportService = netxmsSiteImportService;
        this.equipmentSyncService = equipmentSyncService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncAuDemarrage() {
        log.info("Synchronisation automatique au demarrage...");
        try {
            NetxmsSiteImportService.ImportResult sites = netxmsSiteImportService.importSitesResina();
            log.info("Sites : {} crees, {} mis a jour, {} au total", sites.crees, sites.misAJour, sites.total);

            EquipmentSyncService.SyncResult equip = equipmentSyncService.syncEquipements();
            log.info("Equipements : {} crees, {} mis a jour, {} ignores", equip.crees, equip.misAJour, equip.ignores);
        } catch (Exception e) {
            // On ne bloque pas le demarrage si NetXMS est inaccessible.
            //
            // IMPORTANT (diagnostic temporaire) : on passe l'exception
            // ENTIERE a SLF4J (pas juste e.getMessage()) pour que la
            // trace complete - y compris "Caused by: ..." avec le vrai
            // message PostgreSQL - s'affiche dans la console. Avec
            // e.getMessage() seul, on ne voyait QUE le resume de haut
            // niveau ("StatementCallback; bad SQL grammar [...]"),
            // jamais la cause reelle en dessous - d'ou l'impossibilite
            // de diagnostiquer le probleme jusqu'ici. A remettre en
            // "e.getMessage()" une fois le vrai probleme identifie et
            // corrige, pour ne pas polluer la console au quotidien.
            log.warn("Synchronisation au demarrage echouee (NetXMS inaccessible ?)", e);
        }
    }
}