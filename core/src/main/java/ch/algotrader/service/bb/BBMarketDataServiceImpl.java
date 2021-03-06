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
package ch.algotrader.service.bb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Subscription;
import com.bloomberglp.blpapi.SubscriptionList;

import ch.algotrader.adapter.ExternalSessionStateHolder;
import ch.algotrader.adapter.bb.BBAdapter;
import ch.algotrader.adapter.bb.BBIdGenerator;
import ch.algotrader.adapter.bb.BBMarketDataMessageHandler;
import ch.algotrader.adapter.bb.BBSession;
import ch.algotrader.entity.security.Security;
import ch.algotrader.enumeration.FeedType;
import ch.algotrader.enumeration.InitializingServiceType;
import ch.algotrader.esper.Engine;
import ch.algotrader.service.ExternalMarketDataService;
import ch.algotrader.service.ExternalServiceException;
import ch.algotrader.service.InitializationPriority;
import ch.algotrader.service.InitializingServiceI;
import ch.algotrader.service.NativeMarketDataServiceImpl;
import ch.algotrader.service.ServiceException;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
@InitializationPriority(InitializingServiceType.BROKER_INTERFACE)
public class BBMarketDataServiceImpl extends NativeMarketDataServiceImpl implements ExternalMarketDataService, InitializingServiceI, DisposableBean {

    private static final Logger LOGGER = LogManager.getLogger(BBMarketDataServiceImpl.class);
    private static BBSession session;

    private final BBAdapter bBAdapter;
    private final ExternalSessionStateHolder sessionStateHolder;
    private final Engine serverEngine;

    public BBMarketDataServiceImpl(
            final BBAdapter bBAdapter,
            final ExternalSessionStateHolder sessionStateHolder,
            final Engine serverEngine) {

        super(serverEngine);

        Validate.notNull(bBAdapter, "BBAdapter is null");
        Validate.notNull(sessionStateHolder, "ExternalSessionStateHolder is null");
        Validate.notNull(serverEngine, "Engine is null");

        this.bBAdapter = bBAdapter;
        this.sessionStateHolder = sessionStateHolder;
        this.serverEngine = serverEngine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {

        try {
            session = this.bBAdapter.createMarketDataSession(new BBMarketDataMessageHandler(this.serverEngine, this.sessionStateHolder));
            session.startService();
        } catch (IOException ex) {
            throw new ExternalServiceException(ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException(ex);
        }
    }

    @Override
    public boolean initSubscriptionReady() {

        return this.sessionStateHolder.onSubscribe();
    }

    @Override
    public void subscribe(Security security) {

        Validate.notNull(security, "Security is null");

        if (!session.isRunning()) {
            throw new ServiceException("Bloomberg session is not running to subscribe " + security);
        }

        // create the SubscribeTickEvent (must happen before reqMktData so that Esper is ready to receive marketdata)
        String tickerId = BBIdGenerator.getInstance().getNextRequestId();
        esperSubscribe(security, tickerId);

        SubscriptionList subscriptions = getSubscriptionList(security, tickerId);

        try {
            session.subscribe(subscriptions);
        } catch (IOException ex) {
            throw new ExternalServiceException(ex);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("requested market data for: {} tickerId: {}", security, tickerId);
        }

    }

    @Override
    public void unsubscribe(Security security) {

        Validate.notNull(security, "Security is null");

        if (!session.isRunning()) {
            throw new ServiceException("Bloomberg session is not running to unsubscribe " + security);
        }

        esperUnsubscribe(security).ifPresent(tickerId -> {
            SubscriptionList subscriptions = getSubscriptionList(security, tickerId);

            try {
                session.unsubscribe(subscriptions);
            } catch (IOException ex) {
                throw new ExternalServiceException(ex);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("cancelled market data for : {}", security);
            }
        });
    }

    private SubscriptionList getSubscriptionList(Security security, String tickerId) {

        // get the topic
        String topic = "/bbgid/" + security.getBbgid();

        // defined fields
        List<String> fields = new ArrayList<>();
        fields.add("TRADE_UPDATE_STAMP_RT");
        fields.add("BID_UPDATE_STAMP_RT");
        fields.add("ASK_UPDATE_STAMP_RT");
        fields.add("VOLUME");
        fields.add("LAST_PRICE");
        fields.add("BID");
        fields.add("ASK");
        fields.add("BID_SIZE");
        fields.add("ASK_SIZE");

        // create the subscription list
        SubscriptionList subscriptions = new SubscriptionList();
        subscriptions.add(new Subscription(topic, fields, new CorrelationID(tickerId)));
        return subscriptions;
    }

    @Override
    public String getFeedType() {

        return FeedType.BB.name();
    }

    @Override
    public String getSessionQualifier() {

        return this.sessionStateHolder.getName();
    }

    @Override
    public void destroy() throws Exception {

        if (session != null && session.isRunning()) {
            session.stop();
        }
    }
}
