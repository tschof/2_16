/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2015 AlgoTrader GmbH - All rights reserved
 *
 * All information contained herein is, and remains the property of AlgoTrader GmbH.
 * The intellectual and technical concepts contained herein are proprietary to
 * AlgoTrader GmbH. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from AlgoTrader GmbH
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * AlgoTrader GmbH
 * Aeschstrasse 6
 * 8834 Schindellegi
 ***********************************************************************************/
package ch.algotrader.dao;

import java.util.List;
import java.util.Set;

import ch.algotrader.entity.BaseEntityI;
import ch.algotrader.enumeration.QueryType;

/**
 * A generic Data Access Object providing Hibernate lookup methods.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public interface GenericDao {

    /**
     * gets any Entity by its {@code class} and {@code id}.
     * Securities will get initialized. For {@link ch.algotrader.entity.security.Combination Combinations} all
     * {@link ch.algotrader.entity.security.Component Components} will get initialized.
     */
    public BaseEntityI get(Class<? extends BaseEntityI> clazz, long id);

    /**
     * Performs a HQL query based on the given {@code queryString} and {@code namedParameters}
     * @return a List of Objects
     */
    public <T> List<T> find(Class<T> clazz, String queryString, QueryType type, NamedParam... namedParams);

    /**
     * Performs a HQL query based on the given {@code queryString}, {@code namedParameters} and {@code maxResults}
     * @return a List of Objects
     */
    public <T> List<T> find(Class<T> clazz, String queryString, int maxResults, QueryType type, NamedParam... namedParams);

    /**
     * Performs a HQL query based on the given {@code queryString} and {@code namedParameters}
     * @return a unique Object
     */
    public <T> T findUnique(Class<T> clazz, final String queryString, final QueryType type, final NamedParam... namedParams);

    /**
     * gets the initialized Collection specified by its {@code role} and entity {@code id}
     */
    public Object getInitializedCollection(String role, long id);

    /**
     * Gets the querySpaces (tables) associated with a query
     */
    public Set<String> getQuerySpaces(String queryString);

    /**
     * Returns the queryString of a named query
     */
    public String getNamedQuery(String queryName);

    /**
     * Gets the descriminator value based on the given class.
     */
    public int getDiscriminatorValue(Class<?> type);
}
