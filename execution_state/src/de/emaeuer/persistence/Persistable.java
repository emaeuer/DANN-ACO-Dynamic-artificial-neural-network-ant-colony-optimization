package de.emaeuer.persistence;

import java.io.Serializable;

public interface Persistable<T extends Persistable<T>> extends Serializable {

    String getName();

    String getClassName();

    void applyOther(T other);

    String getCollectionName();
}
