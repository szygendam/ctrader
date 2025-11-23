@Service
public class CTraderAuthService {
    private static final Logger logger = LoggerFactory.getLogger(CTraderAuthService.class);

    private final CTraderConfig config;
    private String accessToken;
    private String refreshToken;
    private String authorizationCode;
    private long traderAccountId;

    public CTraderAuthService(CTraderConfig config) {
        this.config = config;
    }

    public void updateTokens(String code, String incomingAccessToken, String incomingRefreshToken) {
        logger.info("Updating tokens: code={}, incomingAccessToken={}, incomingRefreshToken={}",
                code, incomingAccessToken, incomingRefreshToken);
        if (incomingAccessToken != null && !incomingAccessToken.isEmpty()) {
            this.accessToken = incomingAccessToken;
        }
        if (incomingRefreshToken != null && !incomingRefreshToken.isEmpty()) {
            this.refreshToken = incomingRefreshToken;
        }
        if (code != null && !code.isEmpty()) {
            this.authorizationCode = code;
        }
        // Opcjonalnie możesz wywołać od razu authenticateWithTokens lub zostawić do oddzielnego endpointu
    }

    public AuthResult authenticateWithTokens(String code, String incomingAccessToken, String incomingRefreshToken) throws Exception {
        updateTokens(code, incomingAccessToken, incomingRefreshToken);
        // reszta logiki autoryzacji …
        logger.info("Starting authentication flow with tokens or code");
        performApplicationAuth();
        performGetAccountList();
        performAccountAuth();
        return new AuthResult(this.accessToken, this.refreshToken, this.traderAccountId);
    }

    // ... pozostałe metody (performApplicationAuth, performGetAccountList, etc.) ...
}
