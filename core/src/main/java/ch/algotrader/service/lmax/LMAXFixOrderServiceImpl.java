/***********************************************************************************
 * AlgoTrader Enterprise Trading Framework
 *
 * Copyright (C) 2014 AlgoTrader GmbH - All rights reserved
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
 * Badenerstrasse 16
 * 8004 Zurich
 ***********************************************************************************/
package ch.algotrader.service.lmax;

import ch.algotrader.adapter.fix.fix44.Fix44OrderMessageFactory;
import ch.algotrader.adapter.lmax.LMAXFixOrderMessageFactory;
import ch.algotrader.entity.trade.SimpleOrder;
import ch.algotrader.enumeration.OrderServiceType;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class LMAXFixOrderServiceImpl extends LMAXFixOrderServiceBase {

    private static final long serialVersionUID = 5822672325566918821L;

    // TODO: this is a work-around required due to the existing class hierarchy
    // TODO: Implementation class should be injectable through constructor
    @Override
    protected Fix44OrderMessageFactory createMessageFactory() {
        return new LMAXFixOrderMessageFactory();
    }

    @Override
    protected void handleSendOrder(SimpleOrder order, NewOrderSingle newOrder) throws Exception {
    }

    @Override
    protected void handleModifyOrder(SimpleOrder order, OrderCancelReplaceRequest replaceRequest) throws Exception {
    }

    @Override
    protected void handleCancelOrder(SimpleOrder order, OrderCancelRequest cancelRequest) throws Exception {
    }

    @Override
    protected OrderServiceType handleGetOrderServiceType() throws Exception {

        return OrderServiceType.LMAX_FIX;
    }

}
