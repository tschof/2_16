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
package ch.algotrader.esper.callback;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.entity.marketData.MarketDataEventVO;
import ch.algotrader.entity.marketData.TickVO;
import ch.algotrader.esper.Engine;
import ch.algotrader.util.metric.MetricsUtil;

/**
 * Base Esper Callback Class that will be invoked as soon as at least one Tick has arrived for each of the {@code securities}
 * passed to {@link ch.algotrader.esper.Engine#addFirstTickCallback}
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public abstract class TickCallback extends AbstractEngineCallback {

    private static final Logger LOGGER = LogManager.getLogger(TickCallback.class);

    /**
     * Called by the "ON_FIRST_TICK" statement. Should not be invoked directly.
     */
    public void update(String strategyName, TickVO[] ticks) throws Exception {

        List<TickVO> tickList = Arrays.asList(ticks);

        // get the securityIds sorted asscending
        Set<Long> sortedSecurityIds = tickList.stream()
                .map(MarketDataEventVO::getSecurityId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // get the statement alias based on all security ids
        String alias = "ON_FIRST_TICK_" + StringUtils.join(sortedSecurityIds, "_");

        // undeploy the statement
        Engine engine = getEngine();
        if (engine != null) {
            engine.undeployStatement(alias);
        }

        long startTime = System.nanoTime();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("onFirstTick start {}", sortedSecurityIds);
        }
        // call orderCompleted
        onFirstTick(strategyName, tickList);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("onFirstTick end {}", sortedSecurityIds);
        }

        MetricsUtil.accountEnd("TickCallback." + strategyName, startTime);
    }

    /**
     * Will be exectued by the Esper Engine as soon as at least one Tick has arrived for each of the {@code securities}.
     * Needs to be overwritten by implementing classes.
     */
    public abstract void onFirstTick(String strategyName, List<TickVO> ticks) throws Exception;
}
