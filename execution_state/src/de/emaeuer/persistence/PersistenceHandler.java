package de.emaeuer.persistence;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dizitart.no2.*;
import org.dizitart.no2.filters.Filters;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class PersistenceHandler {

    private final static Logger LOG = LogManager.getLogger(PersistenceHandler.class);

    private static final String ID_ATTRIBUTE = "id";
    private static final String CLASS_NAME_ATTRIBUTE = "class_name";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VALUE_ATTRIBUTE = "value";

    private static final Nitrite db = Nitrite.builder()
            .filePath("environment.db")
            .openOrCreate("user", "password");

    private PersistenceHandler() {}

    public static void persistObject(Persistable<?> object) {
        NitriteCollection collection = db.getCollection(object.getCollectionName());
        if (!collection.hasIndex(ID_ATTRIBUTE)) {
            collection.createIndex(ID_ATTRIBUTE, IndexOptions.indexOptions(IndexType.Unique));
        }

        String stateID = object.getClassName() + "_" + object.getName();
        Document configurationDocument = Document.createDocument(ID_ATTRIBUTE, stateID);
        configurationDocument.put(CLASS_NAME_ATTRIBUTE, object.getClassName());
        configurationDocument.put(NAME_ATTRIBUTE, object.getName());
        configurationDocument.put(VALUE_ATTRIBUTE, object);

        if (collection.find(Filters.eq(ID_ATTRIBUTE, stateID)).size() == 0) {
            collection.insert(configurationDocument);
        } else {
            collection.update(Filters.eq(ID_ATTRIBUTE, stateID), configurationDocument);
        }
    }

    public static <T extends Enum<T>> List<String> getAllConfigurationNames(String keyClassName, String collectionName) {
        return db.getCollection(collectionName)
                .find(Filters.eq(CLASS_NAME_ATTRIBUTE, keyClassName))
                .project(Document.createDocument(NAME_ATTRIBUTE, null))
                .toList()
                .stream()
                .map(d -> d.get(NAME_ATTRIBUTE))
                .map(String.class::cast)
                .collect(Collectors.toList());
    }

    public static <S extends Persistable<S>> void loadObject(S persistable) {
        NitriteCollection collection = db.getCollection(persistable.getCollectionName());

        String configurationID = persistable.getClassName() + "_" + persistable.getName();
        Document configurationDocument = collection.find(Filters.eq(ID_ATTRIBUTE, configurationID)).firstOrDefault();

        if (configurationDocument == null) {
            LOG.log(Level.INFO, String.format("Failed to load object with id \"%s\" because no corresponding object was found", configurationID));
        }

        //noinspection unchecked (because genrics are lost at runtime --> error of cast should only happen for invalid persistable class)
        S loaded = (S) Objects.requireNonNull(configurationDocument).get(VALUE_ATTRIBUTE, persistable.getClass());
        persistable.applyOther(loaded);
    }

}
