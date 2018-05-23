package org.ccctc.colleaguedmiclient.service;

import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.model.CTXAssociation;
import org.ccctc.colleaguedmiclient.model.CTXData;
import org.ccctc.colleaguedmiclient.model.CTXMetadata;
import org.ccctc.colleaguedmiclient.model.CTXVariable;
import org.ccctc.colleaguedmiclient.model.CddEntry;
import org.ccctc.colleaguedmiclient.model.KeyValuePair;
import org.ccctc.colleaguedmiclient.model.SessionCredentials;
import org.ccctc.colleaguedmiclient.transaction.DmiTransaction;
import org.ccctc.colleaguedmiclient.transaction.ctx.CTXRequest;
import org.ccctc.colleaguedmiclient.transaction.ctx.CTXResponse;
import org.ccctc.colleaguedmiclient.util.CddUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ccctc.colleaguedmiclient.util.StringUtils.parseIntOrNull;

/**
 * Service for running a Colleague Transaction via the DMI Service.
 * <p>
 * To instantiate this object, you will need to first create a {@code DmiService}.
 * <p>
 * In a Spring environment, these services would typically be beans.
 *
 * @see DmiService
 */
public class DmiCTXService {

    private final Log log = LogFactory.getLog(DmiCTXService.class);
    private final DmiService dmiService;

    /**
     * CTX Metadata service used to translate a response with field names, data types and associations
     */
    @Getter private final CTXMetadataService ctxMetadataService;

    /**
     * Create a DMI CTX Service (for running Colleague Transactions). This requires a DMI Service (to send/receive
     * DMI transactions). A default CTX Metadata Service created to map results to field names, types and associations.
     *
     * @param dmiService DMI Service
     */
    public DmiCTXService(@NonNull DmiService dmiService) {
        this.dmiService = dmiService;
        this.ctxMetadataService = new CTXMetadataService(this);
    }


    /**
     * Create a DMI CTX Service (for running Colleague Transactions). This requires a DMI Service (to send/receive
     * DMI transactions) and a CTX Metadata Service (to map variable names and associations).
     *
     * @param dmiService         DMI Service
     * @param ctxMetadataService CTX Metadata Service
     */
    public DmiCTXService(@NonNull DmiService dmiService, @NonNull CTXMetadataService ctxMetadataService) {
        this.dmiService = dmiService;
        this.ctxMetadataService = ctxMetadataService;
    }


    /**
     * Execute a Colleague Transaction and return data from the response (the output parameters of the Colleague
     * Transaction), with field names and types mapped based on the specifications of the Colleague Transaction.
     *
     * @param appl            Application
     * @param transactionName Transaction Name
     * @param params          Transaction Input parameters
     * @return Data from the output parameters of the Colleague Transaction
     */
    public CTXData execute(@NonNull String appl, @NonNull String transactionName, List<KeyValuePair<String, String>> params) {
        SessionCredentials creds = dmiService.getSessionCredentials();

        // convert parameter names
        List<KeyValuePair<String, String>> newParams = null;
        if (params != null) {
            newParams = new ArrayList<>();
            CTXMetadata metadata = ctxMetadataService.get(appl, transactionName);
            for (KeyValuePair<String, String> p : params) {
                String key = p.getKey();
                String val = p.getValue();

                if (key != null) {
                    for (CTXVariable v : metadata.getVariables()) {
                        if (key.equals(v.getVarName()) || key.equals(v.getVarAliasName())) {
                            newParams.add(new KeyValuePair<>(v.getVarName(), val));
                            break;
                        }
                    }
                }
            }
        }

        CTXRequest request = new CTXRequest(dmiService.getAccount(), creds.getToken(), creds.getControlId(),
                dmiService.getSharedSecret(), appl, transactionName, newParams);

        DmiTransaction dmiResponse = dmiService.send(request);
        return processResponse(dmiResponse, appl, transactionName);
    }


    /**
     * Execute a Colleague Transaction and return data from the response without any field name and data type conversion.
     *
     * @param appl            Application
     * @param transactionName Transaction Name
     * @param params          Transaction Input parameters
     * @return Data from the output parameters of the Colleague Transaction
     */
    public Map<String, String> executeRaw(@NonNull String appl, @NonNull String transactionName, List<KeyValuePair<String, String>> params) {
        SessionCredentials creds = dmiService.getSessionCredentials();
        CTXRequest request = new CTXRequest(dmiService.getAccount(), creds.getToken(), creds.getControlId(),
                dmiService.getSharedSecret(), appl, transactionName, params);

        if (log.isInfoEnabled()) {
            String p = null;
            if (params != null) {
                for (KeyValuePair<String, String> kp : params) {
                    if (p == null) p = kp.getKey() + "=" + kp.getValue();
                    else p += ", " + kp.getKey() + "=" + kp.getValue();
                }
            }

            if (p == null) p = "(none)";

            if (p.length() > 97) p = p.substring(0, 97) + "...";

            log.info("Sending DMI CTX Request.    Transaction = " + transactionName + " (" + appl + ")"
                    + ", parameters = " + p);
        }

        DmiTransaction dmiResponse = dmiService.send(request);
        CTXResponse ctxResponse = CTXResponse.fromDmiTransaction(dmiResponse);

        if (log.isInfoEnabled()) {
            String p = null;
            for (Map.Entry<String, String> e : ctxResponse.getVariables().entrySet()) {
                if (p == null) p = e.getKey();
                else p += ", " + e.getKey();
            }

            if (p == null) p = "(none)";

            if (p.length() > 97) p = p.substring(0, 97) + "...";

            log.info("Received DMI CTX Response.  Transaction = " + transactionName + " (" + appl + ")"
                    + ", parameters = " + p);

        }

        return ctxResponse.getVariables();
    }

    /**
     * Process response from a CTX Transaction and map variable into their "alias" names as well as build any associations.
     *
     * @param dmiResponse     DMI Transaction
     * @param appl            Application
     * @param transactionName Transaction Name
     * @return CTX Data
     */
    private CTXData processResponse(DmiTransaction dmiResponse, String appl, String transactionName) {
        CTXMetadata metadata = ctxMetadataService.get(appl, transactionName);
        CTXResponse ctxResponse = CTXResponse.fromDmiTransaction(dmiResponse);

        Map<String, Object> variables = new HashMap<>();
        Map<String, List<Map<String, Object>>> associations = new HashMap<>();

        Map<String, String> variableAliases = new HashMap<>();

        // map output variables
        for (CTXVariable v : metadata.getVariables()) {
            if (v.getVarDirection() != null && (v.getVarDirection().equals("INOUT") || v.getVarDirection().equals("OUT"))) {
                String rawValue = ctxResponse.getVariables().get(v.getVarName());

                String variableName = (v.getVarAliasName() != null) ? v.getVarAliasName() : v.getVarName();
                Object variableValue = null;

                // save this mapping for later when we process associations
                variableAliases.put(v.getVarName(), variableName);

                if (rawValue != null) {
                    // Colleague Transactions allow for a "Boolean" type
                    if (v.getVarIsBool() != null && v.getVarIsBool().equals("Y")) {
                        Boolean boolValue = null;
                        String val = rawValue.trim();
                        if (val.length() > 0) {
                            if (val.charAt(0) == '1' || val.charAt(0) == 'Y' || val.charAt(0) == 'y')
                                boolValue = true;
                            else if (val.charAt(0) == '0' || val.charAt(0) == 'N' || val.charAt(0) == 'n')
                                boolValue = false;
                        }

                        variableValue = boolValue;
                    } else {
                        // create a partial CDD entry with what we know about this variable so we can convert it to the
                        // proper data type
                        CddEntry e = CddEntry.builder()
                                .databaseUsageType(v.getVarDataType())
                                .informConversionString(v.getVarConv())
                                .maximumStorageSize(parseIntOrNull(v.getVarSize()))
                                .build();

                        variableValue = CddUtils.convertToValue(rawValue, e);
                    }
                }

                variables.put(variableName, variableValue);
            }
        }

        // map associations
        for (CTXAssociation a : metadata.getAssociations()) {
            String assocName = (a.getAssocAliasName() != null) ? a.getAssocAliasName() : a.getAssocName();

            // determine array size
            int arraySize = 0;
            for (String var : a.getAssocMembers()) {
                String alias = variableAliases.get(var);

                Object v = variables.get(alias);

                if (v instanceof Object[]) {
                    int len = ((Object[])v).length;
                    if (len > arraySize) arraySize = len;
                }
            }

            // initialize association
            List<Map<String, Object>> assocValues = new ArrayList<>(arraySize);
            while (assocValues.size() < arraySize) assocValues.add(new HashMap<>());

            // map values to association, ensuring that each Map in the List has all variables, even if the value is null
            for (String var : a.getAssocMembers()) {
                String alias = variableAliases.get(var);

                Object v = variables.get(alias);

                if (v instanceof Object[]) {
                    Object[] o = (Object[])v;
                    for (int x = 0; x < arraySize; x++) {
                        Object ov = (o.length > x) ? o[x] : null;
                        assocValues.get(x).put(alias, ov);
                    }
                } else if (arraySize > 0) {
                    // non-array variable - put first item and null for the rest
                    assocValues.get(0).put(alias, v);
                    for (int x = 1; x < arraySize; x++)
                        assocValues.get(x).put(alias, null);
                }
            }

            associations.put(assocName, assocValues);
        }

        return new CTXData(variables, associations);
    }
}
