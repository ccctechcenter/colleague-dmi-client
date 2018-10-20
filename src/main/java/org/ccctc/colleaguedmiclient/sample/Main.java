package org.ccctc.colleaguedmiclient.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.sample.model.StudentAcadCredRecord;
import org.ccctc.colleaguedmiclient.service.DmiCTXService;
import org.ccctc.colleaguedmiclient.service.DmiDataService;
import org.ccctc.colleaguedmiclient.service.DmiEntityService;
import org.ccctc.colleaguedmiclient.service.DmiService;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * See the README for instructions on using this sample!!
 */
public class Main {

    static {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showShortLogname", "true");

        // change this property to enable trace logging if desired
        System.setProperty("org.apache.commons.logging.simplelog.defaultlog", "trace");
    }

    private static final Log log = LogFactory.getLog(Main.class);

    // get 100 records from 2010FA
    private static final String selectionCriteria = "STC.TERM = '2010FA' SAMPLE 100";

    public static void main(String[] args) throws IOException {


        // Butte College
        Properties prop = new Properties();

        prop.load(new FileInputStream("dmi.credentials"));

        String username = prop.getProperty("username");
        String password = prop.getProperty("password");
        String account = prop.getProperty("account");
        String host = prop.getProperty("host");
        int port = Integer.parseInt(prop.getProperty("port"));
        boolean secure = Boolean.valueOf(prop.getProperty("secure", "false"));
        String hostnameOverride = prop.getProperty("hostname.override");
        String sharedSecret = prop.getProperty("shared.secret");

        if ("".equals(hostnameOverride)) hostnameOverride = null;

        // pool size
        int poolSize = 5;

        //
        // put DmiService in a try block to ensure it's properly closed after it's done
        //
        try (DmiService dmiService = new DmiService(account, username, password, host, port, secure, hostnameOverride, sharedSecret, poolSize)) {
            DmiCTXService dmiCTXService = new DmiCTXService(dmiService);
            DmiDataService dmiDataService = new DmiDataService(dmiService, dmiCTXService);
            DmiEntityService dmiEntityService = new DmiEntityService(dmiDataService);

            //
            // verify connectivity to Colleague
            //
            dmiService.keepAlive();

            List<StudentAcadCredRecord> data1, data2, data3;
            long start, end, duration1, duration2, duration3;

            //
            // read from STUDENT.ACAD.CRED with selectionCriteria into StudentAcadCredRecord.class
            //
            start = System.currentTimeMillis();
            data1 = dmiEntityService.readForEntity("STUDENT.ACAD.CRED",
                    selectionCriteria, null, StudentAcadCredRecord.class);
            end = System.currentTimeMillis();
            duration1 = end - start;

            //
            // perform the same operation again - this should show improved performance due to caching, especially
            // on the metadata from Colleague entities
            //
            start = System.currentTimeMillis();
            data2 = dmiEntityService.readForEntity("STUDENT.ACAD.CRED",
                    selectionCriteria, null, StudentAcadCredRecord.class);
            end = System.currentTimeMillis();
            duration2 = end - start;




            //
            // for the third iteration enable concurrent query execution
            //
            dmiEntityService.enableConcurrentQueries();
            start = System.currentTimeMillis();
            data3 = dmiEntityService.readForEntity("STUDENT.ACAD.CRED",
                    selectionCriteria, null, StudentAcadCredRecord.class);
            end = System.currentTimeMillis();
            duration3 = end - start;

            //
            assert data1.size() == data2.size();
            assert data1.size() == data3.size();

            log.info("+---------------------------------------------------------------+");
            log.info("|Execution summary:                                             |");
            log.info("+---------------------------------------------------------------+");
            log.info("|First run: " + duration1 + "ms                                              |");
            log.info("|Second run (metadata cached): " + duration2 + "ms                           |");
            log.info("|Third run (metadata cached, concurrent joins: " + duration3 + "ms           |");
            log.info("+---------------------------------------------------------------+");

        }
    }
}
