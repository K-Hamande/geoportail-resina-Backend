package bf.anptic.geoportail.service.backoffice;

import bf.anptic.geoportail.dto.MinistryAccessTokenCreateRequest;
import bf.anptic.geoportail.dto.MinistryAccessTokenDto;
import bf.anptic.geoportail.model.MinistryAccessToken;
import bf.anptic.geoportail.repository.MinistryAccessTokenRepository;
import bf.anptic.geoportail.repository.SiteRepository;
import bf.anptic.geoportail.service.AuditService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class AdminMinistryTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MinistryAccessTokenRepository tokenRepository;
    private final SiteRepository siteRepository;
    private final AuditService auditService;

    public AdminMinistryTokenService(MinistryAccessTokenRepository tokenRepository,
                                      SiteRepository siteRepository,
                                      AuditService auditService) {
        this.tokenRepository = tokenRepository;
        this.siteRepository = siteRepository;
        this.auditService = auditService;
    }

public List<String> listMinisteres() {
        return siteRepository.findDistinctMinisteres();
    }

    public List<MinistryAccessTokenDto> listTokens() {
        return tokenRepository.findAllByOrderByCreeLeDesc().stream().map(this::toDto).toList();
    }

    public MinistryAccessTokenDto createToken(MinistryAccessTokenCreateRequest request, String auteur) {
        MinistryAccessToken token = new MinistryAccessToken();
        token.setToken(genererJetonAleatoire());
        token.setMinistere(request.ministere());
        token.setLibelle(request.libelle());
        token.setActif(true);
        token.setCreeLe(Instant.now());
        token.setCreePar(auteur);

        MinistryAccessToken saved = tokenRepository.save(token);
        auditService.record(auteur, "Création lien décideur", "ministère=" + request.ministere());
        return toDto(saved);
    }

    public void setActive(Long id, boolean actif, String auteur) {
        MinistryAccessToken token = tokenRepository.findById(id).orElseThrow();
        token.setActif(actif);
        tokenRepository.save(token);
        auditService.record(auteur, actif ? "Réactivation lien décideur" : "Révocation lien décideur",
                "ministère=" + token.getMinistere());
    }

    private static String genererJetonAleatoire() {
        byte[] octets = new byte[24];
        RANDOM.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    private MinistryAccessTokenDto toDto(MinistryAccessToken t) {
        return new MinistryAccessTokenDto(t.getId(), t.getToken(), t.getMinistere(), t.getLibelle(),
                t.getActif(), t.getCreeLe(), t.getCreePar());
    }
}