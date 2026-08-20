package bf.anptic.geoportail.service;

import bf.anptic.geoportail.dto.DecideurAuthDto;
import bf.anptic.geoportail.model.DecideurUser;
import bf.anptic.geoportail.repository.DecideurUserRepository;
import bf.anptic.geoportail.security.DecideurJwtService;
import bf.anptic.geoportail.service.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
public class DecideurAuthService {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private final DecideurUserRepository decideurUserRepository;
    private final DecideurJwtService jwtService;
    private final AuditService auditService;

    public DecideurAuthService(DecideurUserRepository decideurUserRepository,
                                DecideurJwtService jwtService,
                                AuditService auditService) {
        this.decideurUserRepository = decideurUserRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    // Connexion decideur : verifie login/mot de passe et retourne un JWT
    public DecideurAuthDto.LoginResponse login(DecideurAuthDto.LoginRequest request) {
        DecideurUser user = decideurUserRepository.findByLoginAndActifTrue(request.login())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Identifiants incorrects."));

        if (!ENCODER.matches(request.motDePasse(), user.getMotDePasseHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects.");
        }

        String token = jwtService.genererToken(
                user.getId(), user.getLogin(),
                user.getRole().name(), user.getMinistere()
        );

        return new DecideurAuthDto.LoginResponse(
                token, user.getNomComplet(),
                user.getRole().name(), user.getMinistere()
        );
    }

    // ---- BACKOFFICE : gestion des comptes decideurs ----

    public List<DecideurAuthDto.DecideurUserResponse> listUsers() {
        return decideurUserRepository.findAllByOrderByCreeLeDesc()
                .stream().map(this::toResponse).toList();
    }

    public DecideurAuthDto.DecideurUserResponse createUser(
            DecideurAuthDto.DecideurUserRequest request, String auteur) {

        if (decideurUserRepository.findByLoginAndActifTrue(request.login()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Un compte avec ce login existe déjà.");
        }

        DecideurUser user = new DecideurUser();
        user.setLogin(request.login());
        user.setNomComplet(request.nomComplet());
        user.setMotDePasseHash(ENCODER.encode(request.motDePasse()));
        user.setRole(DecideurUser.Role.valueOf(request.role()));
        user.setMinistere(request.ministere());
        user.setActif(true);
        user.setCreeLe(Instant.now());
        user.setCreePar(auteur);

        DecideurUser saved = decideurUserRepository.save(user);
        auditService.record(auteur, "Création compte décideur",
                "login=" + request.login() + " role=" + request.role());
        return toResponse(saved);
    }

    public DecideurAuthDto.DecideurUserResponse updateUser(
            Long id, DecideurAuthDto.DecideurUserRequest request, String auteur) {

        DecideurUser user = decideurUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        user.setNomComplet(request.nomComplet());
        user.setRole(DecideurUser.Role.valueOf(request.role()));
        user.setMinistere(request.ministere());
        if (request.motDePasse() != null && !request.motDePasse().isBlank()) {
            user.setMotDePasseHash(ENCODER.encode(request.motDePasse()));
        }

        DecideurUser saved = decideurUserRepository.save(user);
        auditService.record(auteur, "Modification compte décideur", "login=" + user.getLogin());
        return toResponse(saved);
    }

    public void setActive(Long id, boolean actif, String auteur) {
        DecideurUser user = decideurUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        user.setActif(actif);
        decideurUserRepository.save(user);
        auditService.record(auteur, actif ? "Activation compte décideur" : "Désactivation compte décideur",
                "login=" + user.getLogin());
    }

    private DecideurAuthDto.DecideurUserResponse toResponse(DecideurUser u) {
        return new DecideurAuthDto.DecideurUserResponse(
                u.getId(), u.getLogin(), u.getNomComplet(),
                u.getRole().name(), u.getMinistere(),
                u.getActif(), u.getCreeLe()
        );
    }
}