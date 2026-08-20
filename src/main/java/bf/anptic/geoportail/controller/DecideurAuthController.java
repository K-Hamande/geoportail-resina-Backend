package bf.anptic.geoportail.controller;

import bf.anptic.geoportail.dto.DecideurAuthDto;
import bf.anptic.geoportail.service.DecideurAuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class DecideurAuthController {

    private final DecideurAuthService decideurAuthService;

    public DecideurAuthController(DecideurAuthService decideurAuthService) {
        this.decideurAuthService = decideurAuthService;
    }

    @PostMapping("/login")
    public DecideurAuthDto.LoginResponse login(@RequestBody DecideurAuthDto.LoginRequest request) {
        return decideurAuthService.login(request);
    }
}