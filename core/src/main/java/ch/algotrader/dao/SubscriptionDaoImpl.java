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
import java.util.stream.Collectors;

import org.apache.commons.lang.Validate;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

import ch.algotrader.entity.Subscription;
import ch.algotrader.entity.SubscriptionImpl;
import ch.algotrader.entity.security.Combination;
import ch.algotrader.entity.security.Security;
import ch.algotrader.enumeration.QueryType;
import ch.algotrader.util.collection.Pair;
import ch.algotrader.visitor.InitializationVisitor;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
@Repository // Required for exception translation
public class SubscriptionDaoImpl extends AbstractDao<Subscription> implements SubscriptionDao {

    public SubscriptionDaoImpl(final SessionFactory sessionFactory) {

        super(SubscriptionImpl.class, sessionFactory);
    }

    @Override
    public List<Subscription> findBySecurity(long securityId) {

        return findCaching("Subscription.findBySecurity", QueryType.BY_NAME, new NamedParam("securityId", securityId));
    }

    @Override
    public List<Subscription> findByStrategy(String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return findCaching("Subscription.findByStrategy", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));
    }

    @Override
    public List<Subscription> findByStrategyInclProps(String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return find("Subscription.findByStrategyInclProps", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));
    }

    @Override
    public Subscription findByStrategyAndSecurity(String strategyName, long securityId) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return findUniqueCaching("Subscription.findByStrategyAndSecurity", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam("securityId", securityId));
    }

    @Override
    public Subscription findByStrategySecurityAndFeedType(String strategyName, long securityId, String feedType) {

        Validate.notEmpty(strategyName, "Strategy name is empty");
        Validate.notNull(feedType, "String is null");

        return findUniqueCaching("Subscription.findByStrategySecurityAndFeedType", QueryType.BY_NAME, new NamedParam("strategyName", strategyName), new NamedParam("securityId", securityId), new NamedParam(
                "feedType", feedType));
    }

    @Override
    public List<Subscription> findBySecurityAndFeedTypeForAutoActivateStrategies(long securityId, String feedType) {

        Validate.notNull(feedType, "String is null");

        return findCaching("Subscription.findBySecurityAndFeedTypeForAutoActivateStrategies", QueryType.BY_NAME, new NamedParam("securityId", securityId), new NamedParam("feedType", feedType));
    }

    @Override
    public List<Subscription> findNonPersistent() {

        return findCaching("Subscription.findNonPersistent", QueryType.BY_NAME);
    }

    @Override
    public List<Subscription> findNonPositionSubscriptions(String strategyName) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return findCaching("Subscription.findNonPositionSubscriptions", QueryType.BY_NAME, new NamedParam("strategyName", strategyName));
    }

    @Override
    public List<Subscription> findNonPositionSubscriptionsByType(String strategyName, Class<? extends Security> type) {

        Validate.notEmpty(strategyName, "Strategy name is empty");

        return findCaching("Subscription.findNonPositionSubscriptionsByType", QueryType.BY_NAME, new NamedParam("strategyName", strategyName),
                new NamedParam("type", type.getSimpleName()));
    }

    @Override
    public List<Pair<Security, String>> findSubscribedAndFeedTypeForAutoActivateStrategies() {

        List<Subscription> subscriptions = findCaching("Subscription.findSubscribedAndFeedTypeForAutoActivateStrategies", QueryType.BY_NAME);
        List<Pair<Security, String>> list = subscriptions.stream().map(s -> new Pair<>(s.getSecurity(), s.getFeedType())).collect(Collectors.toList());

        for (Pair<Security, String> pair : list) {
            if (pair.getFirst() instanceof Combination) {
                pair.getFirst().accept(InitializationVisitor.INSTANCE, HibernateInitializer.INSTANCE);
            }
        }

        return list;
    }

}
