/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2013 Flury Trading - All rights reserved
 *
 * All information contained herein is, and remains the property of Flury Trading.
 * The intellectual and technical concepts contained herein are proprietary to
 * Flury Trading. Modification, translation, reverse engineering, decompilation,
 * disassembly or reproduction of this material is strictly forbidden unless prior
 * written permission is obtained from Flury Trading
 *
 * Fur detailed terms and conditions consult the file LICENSE.txt or contact
 *
 * Flury Trading
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package com.algoTrader.esper.callback;

import java.util.List;

import com.algoTrader.entity.trade.OrderStatus;
import com.algoTrader.esper.EsperManager;

/**
 * Esper Callback Class that will throw an exception unluss all {@code orders} passed to {@link EsperManager#addTradeCallback} have been fully exectured.
 *
 * @author <a href="mailto:andyflury@gmail.com">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class FullExecutionTradeCallback extends TradeCallback {

    public FullExecutionTradeCallback() {
        super(true);
    }

    @Override
    public void onTradeCompleted(List<OrderStatus> orderStatus) throws Exception {
        // do nothing
    }
}