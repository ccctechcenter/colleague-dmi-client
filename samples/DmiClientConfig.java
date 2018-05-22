import org.ccctc.colleaguedmiclient.service.DmiCTXService;
import org.ccctc.colleaguedmiclient.service.DmiDataService;
import org.ccctc.colleaguedmiclient.service.DmiService;

/**
 * This is a sample of usage with Spring Boot, including full configuration. See the README for more info.
 */
@Configuration
public class DmiClientConfig {

    @Bean
    public DmiService dmiService(@Value("${dmi.account}") String account,
                                 @Value("${dmi.username}") String username,
                                 @Value("${dmi.password}") String password,
                                 @Value("${dmi.host}") String host,
                                 @Value("${dmi.port}") int port,
                                 @Value("${dmi.secure:false}") boolean secure,
                                 @Value("${dmi.host.name.override:null}") String hostnameOverride,
                                 @Value("${dmi.shared.secret}") String sharedSecret,
                                 @Value("${dmi.pool.size:10}") int poolSize,
                                 @Value("${dmi.authorization.expiration.seconds:-1}") long authorizationExpirationSeconds,
                                 @Value("${dmi.max.transaction.retry:-1}") int maxDmiTransactionRetry) {
        DmiService d = new DmiService(account, username, password, host, port, secure, hostnameOverride, sharedSecret, poolSize);

        if (authorizationExpirationSeconds >= 0)
            d.setAuthorizationExpirationSeconds(authorizationExpirationSeconds);

        if (maxDmiTransactionRetry >= 0)
            d.setMaxDmiTransactionRetry(maxDmiTransactionRetry);

        return d;
    }

    @Bean
    public DmiCTXService dmiCTXService(DmiService dmiService,
                                       @Value("${dmi.metadata.expiration.seconds:-1}") int metadataExpirationSeconds) {
        DmiCTXService d = new DmiCTXService(dmiService);

        if (metadataExpirationSeconds >= 0)
            d.getCtxMetadataService().setCacheExpirationSeconds(metadataExpirationSeconds);

        return d;
    }

    @Bean
    public DmiDataService dmiDataService(DmiService dmiService, DmiCTXService dmiCTXService,
                                         @Value("${dmi.metadata.expiration.seconds:-1}") int metadataExpirationSeconds) {
        DmiDataService d = new DmiDataService(dmiService, dmiCTXService);

        if (metadataExpirationSeconds >= 0)
            d.getEntityMetadataService().setCacheExpirationSeconds(metadataExpirationSeconds);

        return d;
    }
}