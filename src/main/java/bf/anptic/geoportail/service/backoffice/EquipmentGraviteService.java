package bf.anptic.geoportail.service.backoffice;

import bf.anptic.geoportail.dto.EquipmentGraviteStatsDto;
import bf.anptic.geoportail.model.Equipment;
import bf.anptic.geoportail.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Repartition de TOUS les equipements par niveau de gravite (section II
// du compte rendu des amendements). Codes de statut NetXMS (meme
// convention que NodeStatus.fromNetXmsSeverityCode) : 0=Normal
// 1=Warning 2=Minor 3=Major 4=Critical 5=Unknown 6=Unmanaged 7=Disabled
// 8=Testing. NodeStatus fusionne habituellement 1+2 dans WARN et 3+4
// dans KO ; ici on les distingue pour retrouver les 4 niveaux demandes
// (Critique/Majeur/Intermediaire/Normal) - ce n'est pas une nouvelle
// regle metier, seulement la granularite deja fournie par NetXMS,
// jusque-la masquee par la fusion. Le libelle du niveau intermediaire
// ("Moyen" vs "Mineur") reste a valider (section VIII).
@Service
public class EquipmentGraviteService {

    private static final String SELECT_STATUTS = """
            SELECT object_id, status
            FROM public.object_properties
            WHERE object_id IN (:ids)
            """;

    private final EquipmentRepository equipmentRepository;
    private final NamedParameterJdbcTemplate netxmsNamedJdbcTemplate;

    public EquipmentGraviteService(EquipmentRepository equipmentRepository,
                                    @Qualifier("netxmsNamedJdbcTemplate") NamedParameterJdbcTemplate netxmsNamedJdbcTemplate) {
        this.equipmentRepository = equipmentRepository;
        this.netxmsNamedJdbcTemplate = netxmsNamedJdbcTemplate;
    }

    public EquipmentGraviteStatsDto calculer() {
        List<Equipment> equipements = equipmentRepository.findAll();
        List<Integer> ids = equipements.stream()
                .map(Equipment::getNetxmsObjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Integer, Integer> statutParObjectId = new HashMap<>();
        if (!ids.isEmpty()) {
            netxmsNamedJdbcTemplate.query(SELECT_STATUTS, new MapSqlParameterSource("ids", ids), rs -> {
                statutParObjectId.put(rs.getInt("object_id"), (Integer) rs.getObject("status"));
            });
        }

        long normal = 0, intermediaire = 0, majeur = 0, critique = 0, inconnu = 0;
        for (Equipment eq : equipements) {
            Integer code = eq.getNetxmsObjectId() != null ? statutParObjectId.get(eq.getNetxmsObjectId()) : null;
            if (code == null) {
                inconnu++;
                continue;
            }
            switch (code) {
                case 0 -> normal++;
                case 1, 2 -> intermediaire++;
                case 3 -> majeur++;
                case 4 -> critique++;
                default -> inconnu++;
            }
        }
        return new EquipmentGraviteStatsDto(normal, intermediaire, majeur, critique, inconnu);
    }
}
