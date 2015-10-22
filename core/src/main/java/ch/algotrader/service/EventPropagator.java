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
package ch.algotrader.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.config.CommonConfig;
import ch.algotrader.entity.marketData.MarketDataEventVO;
import ch.algotrader.enumeration.LifecyclePhase;
import ch.algotrader.event.dispatch.EventDispatcher;
import ch.algotrader.event.listener.LifecycleEventListener;
import ch.algotrader.util.metric.MetricsUtil;
import ch.algotrader.vo.GenericEventVO;
import ch.algotrader.vo.LifecycleEventVO;
import ch.algotrader.vo.TradingStatusEventVO;

/**
 * A utility class that propagates generic and market data events through {@link EventDispatcher}.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 */
public class EventPropagator implements LifecycleEventListener {

    private static final Logger LOGGER = LogManager.getLogger(EventPropagator.class);

    private final EventDispatcher eventDispatcher;
    private volatile boolean active;

    public EventPropagator(final EventDispatcher eventDispatcher, final CommonConfig commonConfig) {
        this.eventDispatcher = eventDispatcher;
        this.active = !commonConfig.isEmbedded() && !commonConfig.isSimulation();
    }

    @Override
    public void onChange(final LifecycleEventVO event) {
        if (event.getPhase() == LifecyclePhase.START) {
            this.active = true;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Life-cycle phase {}", event.getPhase());
        }
    }

    public void propagateMarketData(final MarketDataEventVO marketDataEvent) {

        if (this.active) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(marketDataEvent);
            }

            long startTime = System.nanoTime();
            this.eventDispatcher.sendMarketDataEvent(marketDataEvent);
            MetricsUtil.accountEnd("PropagateMarketDataEventSubscriber.update", startTime);
        }
    }

    public void propagateGenericEvent(final GenericEventVO genericEvent) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(genericEvent);
        }
        this.eventDispatcher.broadcastAllStrategies(genericEvent);
    }

    public void propagateTradingStatusEvent(final TradingStatusEventVO tradingStatusEvent) {

        if (this.active) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(tradingStatusEvent);
            }
            this.eventDispatcher.broadcastAllStrategies(tradingStatusEvent);
        }
    }

}
