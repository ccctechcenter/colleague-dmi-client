# Colleague DMI Client #

## Compilation ##
    
    mvn clean test     - run unit tests and code coverage
    mvn clean compile  - compile
    mvn clean package  - package (jar) and create javadoc

## Nexus ##

Snapshots of this project are stored in nexus.ccctechcenter.org

## What is the DMI? ##

Requests to Colleague from external services follow the client-server architecture with the DMI (Datatel Messaging 
Interface) being the server. The client sends a request (log me in, retrieve data, run code, etc) and the DMI
routes the request to the appropriate place and returns the result. Under the hood, the application and data layers
in Colleague are separate, so one request might go to the database, one might go to the application.

## Colleague DMI Client Features ##

- Handles all communication with the DMI, including authentication and token management

- Uses a connection pool to maintain connectivity to the DMI as well as limit the maximum number of concurrent
  connections
  
- Handles the following types of requests: 

    * Read a single record from a table by primary key
    * Read multiple records from a table by primary key(s) and/or selection criteria 
    * Select primary keys from a table based on selection criteria and/or limiting keys 
    * Run a Colleague Transaction (execute code in the application)
  

## Adding the Colleague DMI Client to your Project ##

#### Maven Dependency ####

@TODO - from nexus

#### Create the Services Directly ####

```java
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
    EntityMetadataService entityMetadataService = new EntityMetadataService(dmiCTXService);
    DmiDataService dmiDataService = new DmiDataService(dmiService, entityMetadataService);
    
    // perform code here ... 
}
```

> Important note: `DmiService` implements `Closeable`. It's important to wrap it in a try-with-resources statement like
> above or a try-finally block (calling `close()` in `finally`), to ensure the DMI Service is closed. This will ensure
> all open sockets are closed when the service is closed. Better yet, use Spring Boot (below) and it should
> automatically handle this when the bean is destroyed.

#### Optional Configuration ####

There are a few configurable items for each service that can be configured after instantiation:

__DmiService__

1. `authorizationExpirationSeconds` - Authorization expiration. Defaults to 4 hours. When authorization expires, 
   the DMI Service will request new credentials via a login request.
2. `maxDmiTransactionRetry` - Maximum attempts to sending / receiving a DMI Transaction. Default is 2.

__EntityMetadataService__

1. `cacheExpirationSeconds` - Number of seconds before a cache entry will expire. Default is 24 hours.


#### Create Spring Beans (recommended if using Spring Boot) ####

Configuration class (includes all required and optional parameters):

```java
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
    public DmiCTXService dmiCTXService(DmiService dmiService) {
         return new DmiCTXService(dmiService);
    }

    @Bean
    public EntityMetadataService entityMetadataService(DmiCTXService dmiCTXService, 
                                                       @Value("${dmi.metadata.expiration.seconds:-1}") int metadataExpirationSeconds) {
        EntityMetadataService e = new EntityMetadataService(dmiCTXService);
        
        if (metadataExpirationSeconds >= 0)
            e.setCacheExpirationSeconds(metadataExpirationSeconds);
        
        return e;
    }

    @Bean
    public DmiDataService dmiDataService(DmiService dmiService, EntityMetadataService entityMetadataService) {
        return new DmiDataService(dmiService, entityMetadataService);
    }
}
```

Application Properties:

```
dmi.account=dev_rt
dmi.username=username
dmi.password=password 
dmi.host=ip-address-or-host-name-of-dmi
dmi.port=1234
dmi.secure=false
dmi.host.name.override= 
dmi.shared.secret=shared-secret
dmi.pool.size=10
dmi.authorization.expiration.seconds=14400
dmi.max.transaction.retry=3
dmi.metadata.cache.seconds=86400
```

## Using the Colleague DMI Client ##

### DMI Data Service ###

The DMI Data Service is used to read data from Colleague.

#### Notes / Limitations ####

* Only one table can be read at a time (no joins)

* Selection criteria is UniQuery (see Selection Criteria below)

* There is a limit to the number of columns which can differ by database. To be safe, it is recommended that no more
  than 400 columns be read at a time. _(although ... why would you ever need 400 columns from one table ?!)_
  
* There may be a maximum length to how long the selection criteria string can be, though it is likely large.

* Large data requests are processed in batches to avoid overwhelming the DMI server with too large of transactions. This
  also means that for all data requests with selection criteria, the keys are read first, then split into batches and
  read by key. This is a safety feature to reduce load and/or the possibility of a crash on the DMI. 
  

#### Methods ####

* ``singleKey`` - retrieve data from a table based on a single primary key

* ``batchKeys`` - retrieve data from a table based on a list of primary keys 

* ``batchSelect`` - retrieve data from a table based on selection criteria

* ``selectKeys`` - retrieve a list of primary keys to a table based on selection criteria and/or limiting keys

#### Selection Criteria ####


@TODO


### DMI CTX (Colleague Transaction) Service ###

@TODO


### Entity Metadata Service ###

@TODO


## APPENDIX A: The Anatomy of a DMI Transaction ##

### Disclaimer ###

**Note: this section is purely for understanding the nuts of bolts of how this client works, which could come in
handy if you are working on bug fixes and improvements to this code. If you are just using the client itself,
you do not need to read this section!** 

### Now Then ... ###

Requests and responses from the DMI are called "DMI Transactions". 

A DMI Transaction is written in delimited text, encoded in Windows-1252 format. The format of the request and the
response are identical as follows:

* The first 16 lines are the same for all DMI Transactions and contain information about the type of request that is
  being performed or was performed.
  
* After the first 16 lines, blocks of sub-transactions contain additional information about the request / response.

* Requests that require authorization contain a final sub-transaction block with a hashed value that includes the
  body of the transaction plus the "shared secret" which adds an additional level of security to ensure the request
  is authentic.
  
* Finally, the entire DMI Transaction contains a header that indicates how many bytes the transaction is, and a 
  footer to indicate the request is complete.   
  
### Example of a DMI Transaction ###

    #257#DMIþ1.4þDAFQþdev_rtþUTþ123456789012345þþ123456789ý1503944062þ18385þ47426þCoreWSý2.0þþþþþþSDAFQþ16þ0þFþSTANDARDþSINGLEKEYþþVIEWþPERSONþ0þFIRST.NAME,LAST.NAMEþ1234567þPHYSþþPERSON.ENDþSDAFQ.ENDþSDHSQþ5þ0þ195CE39912D366249B8C4B1C58477CD6B3B989DAþSDHSQ.END#END#

The above DMI Transaction is a request for the columns FIRST.NAME and LAST.NAME from the table PERSON for a single
record with a primary key of 1234567. Notice that there is a header and footer wrapped in hash marks (`#256#` and 
`#END`). The body of the request is delimited by the ``þ`` character (ascii 254), which corresponds to a "file marker"
or `@FM` in Envision code. Notice also that some values have a ``ý`` (ascii 253) delimiter which is used to delimit 
array values. 

#### Header, Body and Footer ####

The header of the transactions specified the number of bytes that follow the header. This includes the bytes of the
footer. For this request we see that there are 256 bytes.

    #257#

The body of the transaction is the delimited data (more on that below)

    DMIþ1.4þDAFQþdev_rtþUTþ123456789012345þþ123456789ý1503944062þ18385þ47426 ...

Finally we have the footer of the transaction which just indicates that this is the end.

    #END#

#### Body in Detail ####

Breaking out the request into lines we see the following the first 16 lines are as follows (these are the lines that are
common for all DMI transactions):

```
1.  DMI                   (always DMI)
2.  1.4                   (version - always 1.4)
3.  DAFQ                  (type of transaction - DAFQ is a data request)
4.  dev_rt                (account / environment - this indicates the "dev" environment)
5.  UT                    (Colleague application - always UT for data requests)
6.  123456789012345       (token - see below for description)
7.                        (listener id)
8.  123456789ý1503944062  (control id - see below for description)
9.  18385                 (date of DMI request in UniData format)
10. 47426                 (time of DMI request in UniData format)
11. CoreWSý2.0            (who transaction was created by)
12.                       (type of transaction this is in response to)
13.                       (debug level)
14.                       (last processed by)
15.                       (last processed date in UniData format)
16.                       (last processed time in UniData format)
```

Note: the token and first values of the control ID were obtained from a login request and serve to validate this
transaction. The second part of the Control ID is a unique (random) value for the transaction.

The rest of the transaction are sub-transactions. The first one details what data is requested:

```
1.  SDAFQ                  (type of sub-transaction - always begins with "S")
2.  16                     (number of lines of this sub-transaction)
3.  0                      (MIO level - usually 0)
4.  F                      (submit flag - usually "F")
5.  STANDARD               (mode - usually "STANDARD")
6.  SINGLEKEY              (type of data request - this request is for a single record)
7.                         (view options - not used by this client)
8.  VIEW                   (always "VIEW")
9.  PERSON                 (name of table/view)
10. 0                      (request size - may be used to limit result size)
11. FIRST.NAME,LAST.NAME   (columns requested)
12. 1234567                (primary key of record requested)
13. PHYS                   (type of view - PHYS = physical)
14.                        (requester name - not used by this client)
15. PERSON.END             (end of PERSON block)
16. SDAFQ.END              (end of sub-transaction)
```

The final part of this DMI transaction is the hash value, which provides additional security by embedding the shared
secret into the value and hashing it:

```
1. SDHSQ                                      (type of sub-transaction - always begins with "S")
2. 5                                          (number of lines of this sub-transaction)
3. 0                                          (MIO level - usually 0)
4. BCFC0CE57B015A7BAF901149A695EC65883C4896   (SHA1 hash value)
5. SDHSQ.END                                  (end of sub-transaction)
```


#### Response from DMI ####

The response to this transaction:

    #189#DMIþ1.4þDAFSþdev_rtþUTþ123456789012345þþ123456789ýj1þ18385þ48114þHOSTþDAFQþþþ18385þ48114þSDAFSþ19þ0þFþSTANDARDþSINGLEKEYþþSINGLEþPERSONþ3þþ123456789þþþAykroydþþDanþPERSON.ENDþSDAFS.END#END#

Broken down:

```
1.  DMI             (always DMI)
2.  1.4             (version - always 1.4)
3.  DAFS            (type of transaction - DAFS is a data response)
4.  dev_rt          (account / environment - this indicates the "dev" environment)
5.  UT              (Colleague application - always UT for data requests)
6.  123456789012345 (token)
7.                  (listener id)
8.  123456789ýj1    (control ID
9.  18385           (date of DMI request in UniData format)
10. 48114           (time of DMI request in UniData format)
11. HOST            (who transaction was created by)
12. DAFQ            (type of transaction this is in response to)
13.                 (debug level)
14.                 (last processed by)
15. 18385           (last processed date in UniData format)
16. 48114           (last processed time in UniData format)
17. SDAFS           (type of sub-transaction - always begins with "S")
18. 19              (number of lines of this sub-transaction)
19. 0               (MIO level - usually 0)
20. F               (submit flag - from request)
21. STANDARD        (mode - from request)
22. SINGLEKEY       (type - from rqeuest)
23. 
24. SINGLE          (indicates this is a single record)
25. PERSON          (view returned)
26. 3               (number of columns returned)
27. 
28. 123456789       (primary key of the record)
29. 
30.                 (error code)
31. Aykroyd         (data for LAST.NAME)
32.                 (data for SOURCE - blank as this field was not requested)
33. Dan             (data for FIRST.NAME)
34. PERSON.END      (end of PERSON data)
35. SDAFS.END       (end of SDAFS sub-transaction)
```

Notice that this follows the same pattern as the request, with some different fields filled in. The transaction and
sub-transaction types are `DAFS` and `SDAFS` respectively and indicate they are in response to a ``DAFQ`` request.

The key of the returned record is on line #28 (1234567)

The record data starts on line 31. The returned data is in field position order on the table. Only those columns
requested are returned, however, field positions are still maintained. Hence line #32 is blank as it would correspond to
the column `SOURCE` in `PERSON`.

In other words, line 31 - 33 are as follows:

```
31. Aykroyd   - LAST.NAME, which is the first field in PERSON
32.           - SOURCE, which is the second field in PERSON (not requested so it's blank)
33. Dan       - FIRST.NAME, which is the third field in PERSON
(no other data is returned after this as FIRST.NAME was the highest ordered field requested)
```

Notes:

* Even if the first column requested is not the first field in the table, like in this example, the transaction will
  contain all fields starting from the first and ending with the highest ordered field requested
* No fields past the highest ordered field requested are returned

#### Ugh, DMI Transactions suck, do I have to know all this?! ####

**NOPE!** You don't have to understand what's going on behind the scenes as this client handles it all for you,
including the nasty business of determining what fields returned by a data request correspond to what field names
in the table.

