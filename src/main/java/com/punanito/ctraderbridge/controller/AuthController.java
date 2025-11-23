@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final CTraderAuthService authService;

    public AuthController(CTraderAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/update‑tokens")
    public ResponseEntity<Void> updateTokens(@RequestBody AuthRequest request) {
        logger.info("Received token update request: code={}, access_token={}, refresh_token={}",
                request.getCode(), request.getAccess_token(), request.getRefresh_token());
        authService.updateTokens(
                request.getCode(),
                request.getAccess_token(),
                request.getRefresh_token()
        );
        return ResponseEntity.ok().build();
    }

    // Stary endpoint '/complete' jeśli nadal potrzebny można zostawić
}
