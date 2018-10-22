package org.ccctc.colleaguedmiclient.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ccctc.colleaguedmiclient.annotation.Association;
import org.ccctc.colleaguedmiclient.annotation.AssociationEntity;
import org.ccctc.colleaguedmiclient.annotation.Entity;
import org.ccctc.colleaguedmiclient.annotation.Field;
import org.ccctc.colleaguedmiclient.annotation.Ignore;
import org.ccctc.colleaguedmiclient.annotation.Join;
import org.ccctc.colleaguedmiclient.exception.DmiServiceException;
import org.ccctc.colleaguedmiclient.model.ColleagueData;
import org.ccctc.colleaguedmiclient.model.ColleagueRecord;
import org.ccctc.colleaguedmiclient.model.Property;
import org.ccctc.colleaguedmiclient.transaction.data.ViewType;
import org.ccctc.colleaguedmiclient.util.StringUtils;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DmiEntityService {

    private final Log log = LogFactory.getLog(DmiEntityService.class);


    private final static Map<Class, Map<String, Property>> propertyCache = new HashMap<>();

    private final static String ID_FIELD = "@ID";
    private final static long DEFAULT_CACHE_EXPIRATION_SECONDS = 24 * 60 * 60;

    private DmiDataService dmiDataService;
    private MetadataCache<EntityMetadata> entityCache;
    private boolean concurrentQueries = false;

    public DmiEntityService(DmiDataService dmiDataService) {
        this.dmiDataService = dmiDataService;
        this.entityCache = new MetadataCache<>(DEFAULT_CACHE_EXPIRATION_SECONDS);
    }

    /**
     * Clear the cache
     */
    public void clearCache() {
        entityCache.clear();
    }


    /**
     * Get number of seconds before a new cache entry will expire.
     *
     * @return Cache entry expiration time in seconds
     */
    public long getCacheExpirationSeconds() {
        return entityCache.getCacheExpirationSeconds();
    }


    /**
     * Set number of seconds before a new cache entry will expire.
     *
     * @param cacheExpirationSeconds Cache entry expiration time in seconds
     */
    public void setCacheExpirationSeconds(long cacheExpirationSeconds) {
        entityCache.setCacheExpirationSeconds(cacheExpirationSeconds);
    }


    /**
     * Whether join queries from joins will be run concurrently
     *
     * @return true/false
     */
    public boolean isConcurrentQueries() {
        return concurrentQueries;
    }


    /**
     * Enable concurrent execution of joins
     */
    public void enableConcurrentQueries() {
        this.concurrentQueries = true;
    }


    /**
     * Disable concurrent execution of joins
     */
    public void disableConcurrentQueries() {
        this.concurrentQueries = false;
    }


    /**
     * Read data into an entity given a single key
     *
     * @param key   Key
     * @param clazz Entity type
     * @return Entity
     */
    public <T extends ColleagueRecord> T readForEntity(String key, Class<T> clazz) {
        List<T> result = readForEntity(Collections.singletonList(key), clazz);
        return result.size() > 0 ? result.get(0) : null;
    }


    /**
     * Read data into an entity given a query
     *
     * @param viewName     View name
     * @param criteria     Criteria (optional - if null will return all records in view)
     * @param limitingKeys Limiting keys (optional)
     * @param clazz        Entity type
     * @return Entities
     */
    public <T extends ColleagueRecord> List<T> readForEntity(String viewName, String criteria,
                                                             Iterable<String> limitingKeys, Class<T> clazz) {
        String[] keys = dmiDataService.selectKeys(viewName, criteria, limitingKeys);
        return readForEntity(Arrays.asList(keys), clazz);
    }


    /**
     * Read data into an entity given a list of keys
     *
     * @param keys  Keys
     * @param clazz Entity type
     * @return Entities
     */
    public <T extends ColleagueRecord> List<T> readForEntity(Collection<String> keys, Class<T> clazz) {
        if (keys == null || keys.size() == 0) return new ArrayList<>();

        Entity entityAnnotation = clazz.getAnnotation(Entity.class);

        if (entityAnnotation == null)
            throw new DmiServiceException("Class " + clazz.getName() + " does not contain @Entity annotation");

        String appl = entityAnnotation.appl();
        String viewName = entityAnnotation.name();
        ViewType viewType = entityAnnotation.type();
        String cddName = "".equals(entityAnnotation.cddName()) ? null : entityAnnotation.cddName();

        EntityMetadata entityMetadata = getEntityMetadata(clazz);
        Set<String> columns = entityMetadata.getColumns();

        // read data from the DMI
        List<ColleagueData> sourceData = null;
        if (keys.size() == 1) {
            ColleagueData data = dmiDataService.singleKey(appl, viewName, viewType, columns, keys.iterator().next(), cddName);
            if (data != null) sourceData = Collections.singletonList(data); 
        } else {
            sourceData = dmiDataService.batchKeys(appl, viewName, viewType, columns, keys, cddName);
        }

        if (sourceData != null && sourceData.size() > 0) {
            // map source data to the destination entity, including making recursive calls to this method to handle joins
            return processData(sourceData, entityMetadata, clazz);
        }

        return new ArrayList<>();
    }


    /**
     * Process the source data from Colleague, creating new instances of class T and mapping the data. Data for the parent
     * records is processed first, indexing join specifications as it goes. Finally, joins are processed on the entire
     * result set for efficiency (as opposed to record by record) using the join spec indexes.
     */
    private <T extends ColleagueRecord> List<T> processData(List<ColleagueData> sourceData, EntityMetadata metadata, Class<T> clazz) {
        Map<JoinMetadata, List<JoinSpec>> joinIndexes = new HashMap<>();
        List<T> result = new ArrayList<>();

        // read the data into our class and index the records by primary key as well as the join keys
        for (ColleagueData source : sourceData) {
            // map the data to a new object and add to our indexed result
            T dest = mapEntityData(clazz, source, metadata);
            result.add(dest);

            if (metadata.joinMap.size() > 0) {
                indexJoins(source, dest, metadata.joinMap, joinIndexes);
            }
        }

        // process joins
        processJoins(joinIndexes);

        return result;
    }

    /**
     * Calculate and index join values for a single record given its source {@code sourceData} and destination
     * {@code destData}, which includes mapped fields.
     *
     * In addition to supporting a value to value join, multi-valued joins are supported as well through "prefix"
     * and "suffix" keys. These are lists of keys from the parent record to append/prepend to the key when performing
     * the join.
     *
     * Example:
     * - The primary key of STUDENTS is Student ID (ex: 1001234)
     * - The primary key of STUDENT.ACAD.LEVELS is Student ID and Student Acad Level (ex: 1001234*UG)
     * - To join from STUDENTS to STUDENT.ACAD.LEVELS, we need to add the primary key of STUDENTS to the values in STU.ACAD.LEVELS.
     * - To accomplish this, we use a prefix of ID_FIELD on our join specification.
     * - Note that we can also join on portion of an ID field, ie @ID[2] gets the second value of the mutli-valued key
     *
     */
    private void indexJoins(ColleagueData sourceData, ColleagueRecord destData, Map<String, JoinMetadata> joinMetadataMap,
                            Map<JoinMetadata, List<JoinSpec>> joinIndexes) {
        // index the joins
        for (Map.Entry<String, JoinMetadata> join : joinMetadataMap.entrySet()) {
            Object sourceValue = getColleagueDataField(sourceData, join.getKey());

            if (sourceValue != null) {

                JoinMetadata joinMetadata = join.getValue();
                Set<String> childKeys = new HashSet<>();
                String prefix = "";
                String suffix = "";

                // get key(s) from the source record
                if (sourceValue instanceof Object[]) {
                    for (Object s : (Object[]) sourceValue) {
                        if (s != null) childKeys.add(s.toString());
                    }
                } else {
                    childKeys.add(sourceValue.toString());
                }

                // calculate prefix (for multi-valued keys)
                if (joinMetadata.prefixKeys != null) {
                    List<String> pList = new ArrayList<>();
                    for (String p : joinMetadata.prefixKeys) {
                        Object val = getColleagueDataField(sourceData, p);
                        pList.add(val == null ? "" : val.toString());
                    }
                    prefix = StringUtils.join('*', pList) + "*";
                }

                // calculate suffix (for multi-valued keys)
                if (joinMetadata.suffixKeys != null) {
                    List<String> pList = new ArrayList<>();
                    for (String p : joinMetadata.suffixKeys) {
                        Object val = getColleagueDataField(sourceData, p);
                        pList.add(val == null ? "" : val.toString());
                    }
                    suffix = "*" + StringUtils.join('*', pList);
                }

                // add the join specification to the result
                JoinSpec joinSpec = new JoinSpec(destData, prefix, suffix, childKeys);
                if (joinIndexes.get(joinMetadata) == null) {
                    List<JoinSpec> js = new ArrayList<>();
                    js.add(joinSpec);
                    joinIndexes.put(joinMetadata, js);
                } else {
                    joinIndexes.get(joinMetadata).add(joinSpec);
                }
            }

        }
    }

    private Object getColleagueDataField(ColleagueData sourceData, String field) {
        if (field == null || field.length() == 0) return null;

        if (field.charAt(0) == '@' && field.length() >= ID_FIELD.length()) {
            if (field.equals(ID_FIELD))
                return sourceData.getKey();

            int subValue = -1;
            if (field.substring(0, ID_FIELD.length()).equals(ID_FIELD) && field.length() == ID_FIELD.length() + 3) {
                if (field.charAt(ID_FIELD.length()) == '[' && field.charAt(ID_FIELD.length() + 2) == ']') {
                    char c = field.charAt(ID_FIELD.length() + 1);
                    if (c >= '1' && c <= '9') {
                        subValue = Character.getNumericValue(c) - Character.getNumericValue('1');
                    }
                }
            }

            if (subValue == -1)
                throw new DmiServiceException("Invalid join value specification for @ID. Must be @ID or @ID[x] where x between 1 and 9.");

            String[] split = StringUtils.split(sourceData.getKey(), '*');
            if (split != null && split.length > subValue && split[subValue] != null)
                return split[subValue];

            return null;
        } else {
            return sourceData.getValues().get(field);
        }
    }

    /**
     * Process joins
     */
    private void processJoins(Map<JoinMetadata, List<JoinSpec>> joinIndexes) {
        if (concurrentQueries) {

            // run the queries concurrently and wait for all of them to finish
            joinIndexes.entrySet().stream()
                    .map(j -> CompletableFuture.runAsync(() -> processOneJoin(j.getKey(), j.getValue())))
                    .collect(Collectors.toList())
                    .forEach(j -> {
                        try {
                            j.get();
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException(e);
                        }
                    });

        } else {
            for (Map.Entry<JoinMetadata, List<JoinSpec>> j : joinIndexes.entrySet()) {
                processOneJoin(j.getKey(), j.getValue());
            }
        }
    }


    /**
     * Process a single join
     */
    private void processOneJoin(JoinMetadata joinMetadata, List<JoinSpec> joinSpecs) {
        Property parentProperty = joinMetadata.property;
        Class parentPropertyType = parentProperty.getType();
        boolean parentPropertyIsCollection = (parentPropertyType == Collection.class || parentPropertyType == List.class);
        boolean parentPropertyIsArray = parentPropertyType.isArray();

        log.trace("Processing joins for field " + parentProperty.getName() + "...");

        // get the full child keys from the join index (this includes the prefix and suffix appended)
        Set<String> childKeys = new HashSet<>();
        if (joinSpecs != null) {
            for (JoinSpec a : joinSpecs) {
                childKeys.addAll(a.fullKeys);
            }
        }

        if (childKeys.size() == 0) return;

        // get the child record type - if the property is a Collection, we need its generic type
        Class childType;
        if (parentPropertyIsCollection) {
            childType = (Class) parentProperty.getGenericTypeArguments()[0];
        } else if (parentPropertyIsArray) {
            childType = parentPropertyType.getComponentType();
        } else {
            childType = parentPropertyType;
        }

        // ensure the child record type is a ColleagueRecord, otherwise the call to readForEntity will fail
        if (!ColleagueRecord.class.isAssignableFrom(childType))
            throw new DmiServiceException("Child class of Join must inherit from ColleagueRecord");

        // read child entities
        List<ColleagueRecord> children = readForEntity(childKeys, childType);

        // link child entities back to parents
        if (children != null) {
            // index values
            Map<String, ColleagueRecord> indexed = new HashMap<>();
            for (ColleagueRecord o : children) {
                indexed.put(o.getRecordId(), o);
            }

            for (JoinSpec joinSpec : joinSpecs) {
                List<ColleagueRecord> kids = new ArrayList<>();
                for (String fk : joinSpec.fullKeys) {
                    ColleagueRecord record = indexed.get(fk);
                    if (record != null) kids.add(record);
                }

                if (kids.size() > 0) {
                    if (parentPropertyIsCollection) {
                        parentProperty.setProperty(joinSpec.parent, kids);
                    } else if (parentPropertyIsArray) {
                        parentProperty.setProperty(joinSpec.parent, kids.toArray(new ColleagueRecord[0]));
                    } else {
                        parentProperty.setProperty(joinSpec.parent, kids.get(0));
                    }
                }
            }
        }
    }


    /**
     * Create a class of type T and map values from ColleagueData given the EntityMetadata specifications. This includes
     * associations, but does not include joins as those are processed separately.
     */
    private <T extends ColleagueRecord> T mapEntityData(Class<T> clazz, ColleagueData data, EntityMetadata metadata) {

        T result = newInstance(clazz);

        result.setRecordId(data.getKey());

        // map fields
        for (Map.Entry<String, Property> f : metadata.fieldMap.entrySet()) {
            Object value = getColleagueDataField(data, f.getKey());
            Property property = f.getValue();
            Class propertyType = property.getType();
            boolean propertyIsCollection = (propertyType == Collection.class || propertyType == List.class);

            if (value != null) {
                if (value.getClass().isArray() && propertyIsCollection) {
                    // convert array to List
                    property.setProperty(result, Arrays.asList((Object[]) value));
                } else if (value.getClass().isArray() && !propertyType.isArray()) {
                    // read first value of array
                    Object firstValue = ((Object[])value).length > 0 ? ((Object[])value)[0] : null;
                    property.setProperty(result, firstValue);
                } else {
                    property.setProperty(result, value);
                }

            }
        }

        // map associations
        for (Map.Entry<String, AssociationMetadata> a : metadata.assocMap.entrySet()) {
            Property parentProperty = a.getValue().property;
            Class childType = a.getValue().type;
            Map<String, Property> fieldMap = a.getValue().fieldMap;

            List<Object> assocValue = new ArrayList<>();

            // map each field into the association
            for (Map.Entry<String, Property> f : fieldMap.entrySet()) {
                Property childProperty = f.getValue();
                Object[] value = (Object[]) data.getValues().get(f.getKey());

                if (value != null) {
                    for (int x = 0; x < value.length; x++) {
                        // expand the list size as necessary to accommodate new values
                        while (assocValue.size() <= x)
                            assocValue.add(newInstance(childType));

                        // map the value
                        childProperty.setProperty(assocValue.get(x), value[x]);
                    }
                }
            }

            parentProperty.setProperty(result, assocValue);
        }

        return result;
    }


    /**
     * Get metadata about a class marked with the @Entity annotation, using caching for efficiency
     */
    private EntityMetadata getEntityMetadata(Class<?> clazz) {
        String cacheKey = Integer.toString(clazz.hashCode());

        EntityMetadata cached = entityCache.get(cacheKey);
        if (cached != null) return cached;

        Entity entityAnnotation = clazz.getAnnotation(Entity.class);

        if (entityAnnotation == null)
            throw new DmiServiceException("Class " + clazz.getName() + " does not contain @Entity annotation");

        Map<String, Property> fieldMap = new HashMap<>();
        Map<String, JoinMetadata> joinMap = new HashMap<>();
        Map<String, AssociationMetadata> assocMap = new HashMap<>();

        for (java.lang.reflect.Field f : getAllDeclaredFields(clazz)) {

            if ("recordId".equals(f.getName())) continue;

            Property property = getProperty(f.getName(), clazz);

            if (property == null)
                continue;

            Ignore ignore = f.getAnnotation(Ignore.class);
            Field field = f.getAnnotation(Field.class);
            Join join = f.getAnnotation(Join.class);
            Association association = f.getAnnotation(Association.class);

            if (ignore != null && (field != null || join != null || association != null))
                throw new DmiServiceException("Class " + clazz.getName() + " has incompatible @Ignore with @Association, @Field or @Join");

            if (ignore == null) {

                if (association != null) {
                    String name = (!"".equals(association.value())) ? association.value() : camelToColleague(f.getName());

                    assocMap.put(name, getAssociationMetadata(f, property));
                } else if (join != null) {
                    String name = (!"".equals(join.value())) ? join.value() : camelToColleague(f.getName());

                    List<String> p = join.prefixKeys().length > 0 ? Arrays.asList(join.prefixKeys()) : null;
                    List<String> s = join.suffixKeys().length > 0 ? Arrays.asList(join.suffixKeys()) : null;
                    joinMap.put(name, new JoinMetadata(property, p, s));
                } else if (field != null || entityAnnotation.autoMap()) {
                    String name = (field != null) ? field.value() : camelToColleague(f.getName());

                    fieldMap.put(name, property);
                }
            }
        }

        EntityMetadata result = new EntityMetadata(fieldMap, joinMap, assocMap);

        entityCache.put(cacheKey, result);

        return result;
    }


    /**
     * Get metadata for a class annotated that is used in an association with the @Association annotation
     */
    private AssociationMetadata getAssociationMetadata(java.lang.reflect.Field field, Property property) {
        if (property.getType() != List.class)
            throw new DmiServiceException("Association for property " + property.getName() + " must be a List");

        Class<?> clazz = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

        AssociationEntity associationEntity = clazz.getAnnotation(AssociationEntity.class);

        boolean autoMap = associationEntity == null || associationEntity.autoMap();

        Map<String, Property> fieldMap = new HashMap<>();

        for (java.lang.reflect.Field f : getAllDeclaredFields(clazz)) {

            if ("recordId".equals(f.getName())) continue;

            Ignore ignoreAnnotation = f.getAnnotation(Ignore.class);
            Field fieldAnnotation = f.getAnnotation(Field.class);

            if (ignoreAnnotation != null && (fieldAnnotation != null))
                throw new DmiServiceException("Class " + clazz.getName() + " has incompatible @Ignore with @Field");

            if (ignoreAnnotation == null && (fieldAnnotation != null || autoMap)) {
                String name = camelToColleague(f.getName());
                Property prop = getProperty(f.getName(), clazz);
                fieldMap.put(name, prop);
            }
        }

        return new AssociationMetadata(property, clazz, fieldMap);
    }


    /**
     * Convert a camel-cased field name to Colleague, ie firstName is translated to FIRST.NAME
     */
    static String camelToColleague(String name) {
        char[] output = new char[name.length() * 2];

        int count = 0;
        for (int x = 0; x < name.length(); x++) {
            char c = name.charAt(x);
            if (c >= 'A' && c <= 'Z') {
                output[count++] = '.';
            }

            output[count++] = Character.toUpperCase(c);
        }

        return new String(output, 0, count);
    }


    /**
     * Get all declared fields of a class and its super classes (excluding the Object class)
     */
    private List<java.lang.reflect.Field> getAllDeclaredFields(Class clazz) {
        List<java.lang.reflect.Field> result = new ArrayList<>();

        if (clazz.getSuperclass() != null && clazz.getSuperclass() != Object.class)
            result.addAll(getAllDeclaredFields(clazz.getSuperclass()));

        for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
            if (!f.isSynthetic()) result.add(f);
        }

        return result;
    }


    /**
     * Get all Property items from a class. The results are cached.
     */
    private static Map<String, Property> getProperties(Class clazz) {
        try {
            if (propertyCache.containsKey(clazz))
                return propertyCache.get(clazz);

            Map<String, Property> properties = new HashMap<>();

            for (PropertyDescriptor pd : Introspector.getBeanInfo(clazz).getPropertyDescriptors()) {
                if (!"class".equals(pd.getName()))
                    properties.put(pd.getName(), new Property(pd));
            }

            propertyCache.put(clazz, properties);

            return properties;
        } catch (IntrospectionException e) {
            throw new DmiServiceException("Error retrieving properties for class " + clazz.getName() + ":" + e.getMessage());
        }
    }


    /**
     * Get Property from a class by name
     */
    private static Property getProperty(String name, Class clazz) {
        Map<String, Property> properties = getProperties(clazz);
        return properties.get(name);
    }


    /**
     * Create a new instance of a class with error handling
     *
     * @param clazz Class to instantiate
     * @return New object
     */
    private <T> T newInstance(Class<T> clazz) {
        try {
            return clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new DmiServiceException("Unable to instantiate new class of type " + clazz.getName() + ": " + e.getMessage());
        }
    }


    /**
     * Metadata about a class marked with the @Entity annotation
     */
    private static class EntityMetadata {
        final Map<String, Property> fieldMap;
        final Map<String, JoinMetadata> joinMap;
        final Map<String, AssociationMetadata> assocMap;

        EntityMetadata(Map<String, Property> fieldMap, Map<String, JoinMetadata> joinMap, Map<String, AssociationMetadata> assocMap) {
            this.fieldMap = fieldMap;
            this.joinMap = joinMap;
            this.assocMap = assocMap;
        }

        /**
         * Get a distinct list of columns referenced by this entity's fields, joins and associations
         */
        Set<String> getColumns() {
            Set<String> columns = new HashSet<>();

            // get a distinct list of columns from fields, associations, joins
            columns.addAll(this.fieldMap.keySet());
            columns.addAll(this.joinMap.keySet());

            // associated fields
            for (AssociationMetadata a : this.assocMap.values()) {
                columns.addAll(a.fieldMap.keySet());
            }

            // prefix and suffix keys
            for (JoinMetadata a : this.joinMap.values()) {
                if (a.prefixKeys != null) columns.addAll(a.prefixKeys);
                if (a.suffixKeys != null) columns.addAll(a.suffixKeys);
            }

            // remove any virtual fields that start with @
            columns.removeIf(i -> (i == null || i.length() == 0 || i.charAt(0) == '@'));


            return columns;
        }
    }


    /**
     * Metadata about a join
     */
    private static class JoinMetadata {
        Property property;
        List<String> prefixKeys;
        List<String> suffixKeys;

        JoinMetadata(Property property, List<String> prefixKeys, List<String> suffixKeys) {
            this.property = property;
            this.prefixKeys = prefixKeys;
            this.suffixKeys = suffixKeys;
        }
    }


    /**
     * Metadata about a class that is an association of an Entity
     */
    private static class AssociationMetadata {
        final Property property;
        final Class type;
        final Map<String, Property> fieldMap;

        AssociationMetadata(Property property, Class type, Map<String, Property> fieldMap) {
            this.property = property;
            this.type = type;
            this.fieldMap = fieldMap;
        }
    }

    /**
     * Specifications of a join
     */
    private static class JoinSpec {
        final ColleagueRecord parent;
        final String joinPrefix;
        final String joinSuffix;
        final Set<String> keys;
        final List<String> fullKeys;

        JoinSpec(ColleagueRecord parent, String joinPrefix, String joinSuffix, Set<String> keys) {
            this.parent = parent;
            this.joinPrefix = joinPrefix;
            this.joinSuffix = joinSuffix;
            this.keys = keys;
            this.fullKeys = new ArrayList<>();

            // derive full keys by adding the prefix and suffix to each
            if (keys != null) {
                for (String k : keys) {
                    String p = joinPrefix == null ? "" : joinPrefix;
                    String s = joinSuffix == null ? "" : joinSuffix;
                    fullKeys.add(p + k + s);
                }
            }
        }
    }

}
