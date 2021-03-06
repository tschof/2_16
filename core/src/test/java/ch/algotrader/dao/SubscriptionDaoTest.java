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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.algotrader.entity.Position;
import ch.algotrader.entity.PositionImpl;
import ch.algotrader.entity.Subscription;
import ch.algotrader.entity.SubscriptionImpl;
import ch.algotrader.entity.security.Forex;
import ch.algotrader.entity.security.ForexImpl;
import ch.algotrader.entity.security.Security;
import ch.algotrader.entity.security.SecurityFamily;
import ch.algotrader.entity.security.SecurityFamilyImpl;
import ch.algotrader.entity.security.SecurityImpl;
import ch.algotrader.entity.strategy.Strategy;
import ch.algotrader.entity.strategy.StrategyImpl;
import ch.algotrader.enumeration.Currency;
import ch.algotrader.enumeration.FeedType;
import ch.algotrader.hibernate.InMemoryDBTest;
import ch.algotrader.util.collection.Pair;

/**
* Unit tests for {@link ch.algotrader.entity.Subscription}.
*
* @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
*
* @version $Revision$ $Date$
*/
public class SubscriptionDaoTest extends InMemoryDBTest {

    private SubscriptionDao dao;

    private SecurityFamily family1;

    private Forex forex1;

    private Strategy strategy1;

    private SecurityFamily family2;

    private Forex forex2;

    private Strategy strategy2;

    public SubscriptionDaoTest() throws IOException {

        super();
    }

    @Override
    @Before
    public void setup() throws Exception {

        super.setup();

        this.dao = new SubscriptionDaoImpl(this.sessionFactory);

        this.family1 = new SecurityFamilyImpl();
        this.family1.setName("Forex1");
        this.family1.setTickSizePattern("0<0.1");
        this.family1.setCurrency(Currency.USD);

        this.forex1 = new ForexImpl();
        this.forex1.setSymbol("EUR.USD");
        this.forex1.setBaseCurrency(Currency.EUR);
        this.forex1.setSecurityFamily(this.family1);

        this.strategy1 = new StrategyImpl();
        this.strategy1.setName("Strategy1");

        this.family2 = new SecurityFamilyImpl();
        this.family2.setName("Forex2");
        this.family2.setTickSizePattern("0>0.1");
        this.family2.setCurrency(Currency.AUD);

        this.forex2 = new ForexImpl();
        this.forex2.setSymbol("EUR.AUD");
        this.forex2.setBaseCurrency(Currency.AUD);
        this.forex2.setSecurityFamily(this.family2);

        this.strategy2 = new StrategyImpl();
        this.strategy2.setName("Strategy2");
    }

    @Test
    public void testFindBySecurity() {

        this.session.save(this.family1);
        this.session.save(this.forex1);
        this.session.save(this.strategy1);
        this.session.save(this.strategy2);

        List<Subscription> subscriptions1 = this.dao.findBySecurity(this.forex1.getId());

        Assert.assertEquals(0, subscriptions1.size());

        Subscription subscription1 = new SubscriptionImpl();
        subscription1.setFeedType(FeedType.SIM.name());
        subscription1.setSecurity(this.forex1);
        subscription1.setStrategy(this.strategy1);

        this.session.save(subscription1);

        Subscription subscription2 = new SubscriptionImpl();
        subscription2.setFeedType(FeedType.SIM.name());
        subscription2.setSecurity(this.forex1);
        subscription2.setStrategy(this.strategy2);

        this.session.save(subscription2);
        this.session.flush();

        Subscription subscription3 = new SubscriptionImpl();
        subscription3.setFeedType(FeedType.IB.name());
        subscription3.setSecurity(this.forex1);
        subscription3.setStrategy(this.strategy2);

        this.session.save(subscription3);
        this.session.flush();

        List<Subscription> subscriptions2 = this.dao.findBySecurity(this.forex1.getId());

        Assert.assertEquals(3, subscriptions2.size());
    }

    @Test
    public void testFindByStrategy() {

        this.session.save(this.family1);
        this.session.save(this.forex1);
        this.session.save(this.strategy1);

        Subscription subscription1 = new SubscriptionImpl();
        subscription1.setFeedType(FeedType.SIM.name());
        subscription1.setSecurity(this.forex1);
        subscription1.setStrategy(this.strategy1);

        this.session.save(subscription1);
        this.session.save(this.family2);
        this.session.save(this.forex2);
        this.session.save(this.strategy2);

        Subscription subscription2 = new SubscriptionImpl();
        subscription2.setFeedType(FeedType.IB.name());
        subscription2.setSecurity(this.forex2);
        subscription2.setStrategy(this.strategy1);

        this.session.save(subscription2);
        this.session.flush();

        List<Subscription> subscriptions1 = this.dao.findByStrategy("Dummy");

        Assert.assertEquals(0, subscriptions1.size());

        List<Subscription> subscriptions2 = this.dao.findByStrategy("Strategy1");

        Assert.assertEquals(2, subscriptions2.size());

        Map<Security, Subscription> map = new HashMap<>();

        for (Subscription subscription : subscriptions2) {

            map.put(subscription.getSecurity(), subscription);
        }

        Assert.assertSame(subscription1, map.get(this.forex1));
        Assert.assertSame(subscription2, map.get(this.forex2));
    }

    @Test
    public void testFindByStrategyAndSecurity() {

        this.session.save(this.family1);
        this.session.save(this.forex1);
        this.session.save(this.strategy1);

        Subscription subscription1 = new SubscriptionImpl();
        subscription1.setFeedType(FeedType.SIM.name());
        subscription1.setSecurity(this.forex1);
        subscription1.setStrategy(this.strategy1);

        this.session.save(subscription1);
        this.session.flush();

        Subscription subscription2 = this.dao.findByStrategyAndSecurity("Strategy1", 0);

        Assert.assertNull(subscription2);

        Subscription subscription3 = this.dao.findByStrategyAndSecurity("Strategy1", this.forex1.getId());

        Assert.assertNotNull(subscription3);

        Assert.assertEquals(FeedType.SIM.name(), subscription3.getFeedType());
        Assert.assertSame(this.forex1, subscription3.getSecurity());
        Assert.assertSame(this.family1, subscription3.getSecurity().getSecurityFamily());
        Assert.assertSame(this.strategy1, subscription3.getStrategy());
    }

    @Test
    public void testFindByStrategySecurityAndFeedType() {

        this.session.save(this.family1);
        this.session.save(this.forex1);
        this.session.save(this.strategy1);

        Subscription subscription1 = new SubscriptionImpl();
        subscription1.setFeedType(FeedType.SIM.name());
        subscription1.setSecurity(this.forex1);
        subscription1.setStrategy(this.strategy1);

        this.session.save(subscription1);
        this.session.flush();

        Subscription subscription2 = this.dao.findByStrategySecurityAndFeedType("Strategy1", this.forex1.getId(), FeedType.BB.name());

        Assert.assertNull(subscription2);

        Subscription subscription3 = this.dao.findByStrategySecurityAndFeedType("Strategy1", this.forex1.getId(), FeedType.SIM.name());

        Assert.assertNotNull(subscription3);

        Assert.assertEquals(FeedType.SIM.name(), subscription3.getFeedType());
        Assert.assertSame(this.forex1, subscription3.getSecurity());
        Assert.assertSame(this.family1, subscription3.getSecurity().getSecurityFamily());
        Assert.assertSame(this.strategy1, subscription3.getStrategy());
    }

    @Test
    public void testFindBySecurityAndFeedTypeForAutoActivateStrategies() {

        this.session.save(this.family1);
        this.session.save(this.forex1);
        this.session.save(this.strategy1);

        Subscription subscription1 = new SubscriptionImpl();
        subscription1.setFeedType(FeedType.SIM.name());
        subscription1.setSecurity(this.forex1);
        subscription1.setStrategy(this.strategy1);

        this.session.save(subscription1);
        this.session.save(this.family2);
        this.session.save(this.forex2);
        this.session.save(this.strategy2);

        Subscription subscription2 = new SubscriptionImpl();
        subscription2.setSecurity(this.forex1);
        subscription2.setFeedType(FeedType.SIM.name());
        subscription2.setStrategy(this.strategy2);

        this.session.save(subscription2);
        this.session.flush();

        List<Subscription> subscriptions1 = this.dao.findBySecurityAndFeedTypeForAutoActivateStrategies(this.forex1.getId(), FeedType.SIM.name());

        Assert.assertEquals(0, subscriptions1.size());

        this.strategy1.setAutoActivate(true);
        this.strategy2.setAutoActivate(true);

        this.session.flush();

        List<Subscription> subscriptions2 = this.dao.findBySecurityAndFeedTypeForAutoActivateStrategies(this.forex1.getId(), FeedType.SIM.name());

        Assert.assertEquals(2, subscriptions2.size());

        Map<Strategy, Subscription> map = new HashMap<>();

        for (Subscription subscription: subscriptions2) {

            map.put(subscription.getStrategy(), subscription);
        }

        Assert.assertSame(subscription1, map.get(this.strategy1));
        Assert.assertSame(subscription2, map.get(this.strategy2));
    }

    @Test
    public void testFindNonPersistent() {

        this.session.save(this.family1);
        this.session.save(this.forex1);
        this.session.save(this.strategy1);

        Subscription subscription1 = new SubscriptionImpl();
        subscription1.setFeedType(FeedType.SIM.name());
        subscription1.setSecurity(this.forex1);
        subscription1.setStrategy(this.strategy1);
        subscription1.setPersistent(true);

        this.session.save(subscription1);
        this.session.save(this.family2);
        this.session.save(this.forex2);
        this.session.save(this.strategy2);

        Subscription subscription2 = new SubscriptionImpl();
        subscription2.setFeedType(FeedType.IB.name());
        subscription2.setSecurity(this.forex2);
        subscription2.setStrategy(this.strategy2);
        subscription2.setPersistent(true);

        this.session.save(subscription2);
        this.session.flush();

        List<Subscription> subscriptions1 = this.dao.findNonPersistent();

        Assert.assertEquals(0, subscriptions1.size());

        subscription1.setPersistent(false);
        subscription2.setPersistent(false);

        this.session.flush();

        List<Subscription> subscriptions2 = this.dao.findNonPersistent();

        Assert.assertEquals(2, subscriptions2.size());
    }

    @Test
    public void testFindNonPositionSubscriptions() {

        this.session.save(this.family1);
        this.session.save(this.forex1);
        this.session.save(this.strategy1);

        Subscription subscription1 = new SubscriptionImpl();
        subscription1.setFeedType(FeedType.SIM.name());
        subscription1.setSecurity(this.forex1);
        subscription1.setStrategy(this.strategy1);

        Subscription subscription2 = new SubscriptionImpl();
        subscription2.setFeedType(FeedType.IB.name());
        subscription2.setStrategy(this.strategy1);
        subscription2.setSecurity(this.forex1);

        Position position1 = new PositionImpl();
        position1.setQuantity(222);
        position1.setSecurity(this.forex1);
        position1.setStrategy(this.strategy1);
        position1.setCost(new BigDecimal(0.0));
        position1.setRealizedPL(new BigDecimal(0.0));

        this.session.save(subscription2);
        this.session.save(subscription1);
        this.session.save(position1);
        this.session.flush();

        List<Subscription> subscriptions1 = this.dao.findNonPositionSubscriptions("Strategy1");

        Assert.assertEquals(0, subscriptions1.size());

        position1.setQuantity(0);

        this.session.flush();

        List<Subscription> subscriptions2 = this.dao.findNonPositionSubscriptions("Strategy1");

        Assert.assertEquals(2, subscriptions2.size());
    }

    @Test
    public void testFindNonPositionSubscriptionsByType() {

        this.session.save(this.family1);
        this.session.save(this.forex1);
        this.session.save(this.strategy1);

        Subscription subscription1 = new SubscriptionImpl();
        subscription1.setFeedType(FeedType.SIM.name());
        subscription1.setSecurity(this.forex1);
        subscription1.setStrategy(this.strategy1);
        subscription1.setPersistent(true);

        Subscription subscription2 = new SubscriptionImpl();
        subscription2.setFeedType(FeedType.IB.name());
        subscription2.setStrategy(this.strategy1);
        subscription2.setSecurity(this.forex1);

        Position position1 = new PositionImpl();
        position1.setId(111);
        position1.setQuantity(222);
        position1.setSecurity(this.forex1);
        position1.setStrategy(this.strategy1);
        position1.setQuantity(0);
        position1.setCost(new BigDecimal(0.0));
        position1.setRealizedPL(new BigDecimal(0.0));

        this.session.save(subscription1);
        this.session.save(subscription2);
        this.session.save(position1);
        this.session.flush();

        List<Subscription> subscriptions1 = this.dao.findNonPositionSubscriptionsByType("Strategy1", SecurityImpl.class);

        Assert.assertEquals(0, subscriptions1.size());

        List<Subscription> subscriptions2 = this.dao.findNonPositionSubscriptionsByType("Strategy1", ForexImpl.class);

        Assert.assertEquals(2, subscriptions2.size());

        Map<String, Subscription> map = new HashMap<>();

        for (Subscription subscription: subscriptions2) {

            map.put(subscription.getFeedType(), subscription);
        }

        Assert.assertSame(subscription1, map.get(FeedType.SIM.name()));
        Assert.assertSame(subscription2, map.get(FeedType.IB.name()));
    }

    @Test
    public void testFindSubscribedAndFeedTypeForAutoActivateStrategies() {

        SecurityFamily family1 = new SecurityFamilyImpl();
        family1.setName("family1");
        family1.setTickSizePattern("0<0.1");
        family1.setCurrency(Currency.EUR);

        Forex forex1 = new ForexImpl();
        forex1.setSymbol("INR.EUR");
        forex1.setBaseCurrency(Currency.CAD);
        forex1.setSecurityFamily(family1);

        Strategy strategy1 = new StrategyImpl();
        strategy1.setName("Strategy1");
        strategy1.setAutoActivate(Boolean.FALSE);

        Subscription subscription1 = new SubscriptionImpl();
        subscription1.setFeedType(FeedType.SIM.name());
        subscription1.setSecurity(forex1);
        subscription1.setStrategy(strategy1);

        this.session.save(family1);
        this.session.save(forex1);
        this.session.save(strategy1);
        this.session.save(subscription1);

        this.session.flush();

        List<Pair<Security, String>> maps1 = this.dao.findSubscribedAndFeedTypeForAutoActivateStrategies();

        Assert.assertEquals(0, maps1.size());

        strategy1.setAutoActivate(Boolean.TRUE);
        this.session.flush();

        List<Pair<Security, String>> maps2 = this.dao.findSubscribedAndFeedTypeForAutoActivateStrategies();

        Assert.assertEquals(1, maps2.size());

        Assert.assertSame(FeedType.SIM.name(), maps2.get(0).getSecond());
        Assert.assertSame(forex1, maps2.get(0).getFirst());
    }

}
