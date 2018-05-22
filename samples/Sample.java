import org.ccctc.colleaguedmiclient.service.DmiCTXService;
import org.ccctc.colleaguedmiclient.service.DmiDataService;
import org.ccctc.colleaguedmiclient.service.DmiService;

public class Main {
    public static void main() {
        String username = "username";
        String password = "passowrd";
        String account = "dev_rt";
        String ipAddress = "ip-address-or-host-name-of-dmi";
        int port = 1234;
        boolean secure = false;
        String hostnameOverride = null;
        String sharedSecret = "shared-secret";
        int poolSize = 10;


        try (DmiService dmiService = new DmiService(account, username, password, ipAddress, port, secure, hostnameOverride, sharedSecret, poolSize)) {
            DmiCTXService dmiCTXService = new DmiCTXService(dmiService);
            DmiDataService dmiDataService = new DmiDataService(dmiService, dmiCTXService);

            // perform code here ...
        }
    }
}