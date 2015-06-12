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
package ch.algotrader.adapter.fix.fix44;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import ch.algotrader.entity.trade.Fill;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderStatus;
import ch.algotrader.enumeration.Status;
import ch.algotrader.esper.EngineLocator;
import ch.algotrader.service.LookupService;
import ch.algotrader.util.MyLogger;
import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.field.MsgSeqNum;
import quickfix.field.OrdStatus;
import quickfix.field.Text;
import quickfix.field.TransactTime;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.OrderCancelReject;

/**
 * Abstract FIX44 order message handler implementing generic functionality common to all broker specific
 * interfaces..
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 *
 * @version $Revision$ $Date$
 */
public abstract class AbstractFix44OrderMessageHandler extends AbstractFix44MessageHandler {

    private static Logger LOGGER = MyLogger.getLogger(AbstractFix44OrderMessageHandler.class.getName());

    private LookupService lookupService;

    public void setLookupService(final LookupService lookupService) {
        this.lookupService = lookupService;
    }

    public LookupService getLookupService() {
        return this.lookupService;
    }

    protected abstract boolean discardReport(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract boolean isOrderRejected(ExecutionReport executionReport) throws FieldNotFound;

    protected abstract OrderStatus createStatus(ExecutionReport executionReport, Order order) throws FieldNotFound;

    protected abstract Fill createFill(ExecutionReport executionReport, Order order) throws FieldNotFound;

    public void onMessage(final ExecutionReport executionReport, final SessionID sessionID) throws FieldNotFound {

        if (discardReport(executionReport)) {

            return;
        }

        String intId = resolveIntOrderId(executionReport);

        // get the order from the OpenOrderWindow
        Order order = getLookupService().getOpenOrderByRootIntId(intId);
        if (order == null) {

            if (LOGGER.isEnabledFor(Level.ERROR)) {

                LOGGER.error("Order with int ID " + intId + " matching the execution report could not be found");
            }
            return;
        }

        if (isOrderRejected(executionReport)) {

            String statusText = getStatusText(executionReport);

            if (LOGGER.isEnabledFor(Level.ERROR)) {

                StringBuilder buf = new StringBuilder();
                buf.append("Order with int ID ").append(intId).append(" has been rejected");
                if (statusText != null) {

                    buf.append("; reason given: ").append(statusText);
                }
                LOGGER.error(buf.toString());
            }

            OrderStatus orderStatus = OrderStatus.Factory.newInstance();
            orderStatus.setStatus(Status.REJECTED);
            orderStatus.setIntId(intId);
            orderStatus.setSequenceNumber(executionReport.getHeader().getInt(MsgSeqNum.FIELD));
            orderStatus.setOrder(order);
            if (executionReport.isSetField(TransactTime.FIELD)) {

                orderStatus.setExtDateTime(executionReport.getTransactTime().getValue());
            }
            if (statusText != null) {

                orderStatus.setReason(statusText);
            }

            EngineLocator.instance().getServerEngine().sendEvent(orderStatus);

            return;
        }

        OrderStatus orderStatus = createStatus(executionReport, order);

        EngineLocator.instance().getServerEngine().sendEvent(orderStatus);

        Fill fill = createFill(executionReport, order);
        if (fill != null) {

            // associate the fill with the order
            fill.setOrder(order);

            EngineLocator.instance().getServerEngine().sendEvent(fill);
        }
    }

    /**
     * Resolves intId of the order this execution report is intended for.
     */
    protected String resolveIntOrderId(final ExecutionReport executionReport) throws FieldNotFound {

        return executionReport.getClOrdID().getValue();
    }

    /**
     * This method can be overridden to provide a custom translation of status (error) codes
     * to human readable status (error) messages.
     */
    protected String getStatusText(final ExecutionReport executionReport) throws FieldNotFound {
        if (executionReport.isSetText()) {

            return executionReport.getText().getValue();
        } else {

            return null;
        }
    }

    public void onMessage(final OrderCancelReject reject, final SessionID sessionID) throws FieldNotFound {

        if (LOGGER.isEnabledFor(Level.WARN)) {

            StringBuilder buf = new StringBuilder();
            buf.append("Order cancel/replace has been rejected");
            String clOrdID = reject.getClOrdID().getValue();
            buf.append(" [order ID: ").append(clOrdID).append("]");
            String origClOrdID = reject.getOrigClOrdID().getValue();
            buf.append(" [original order ID: ").append(origClOrdID).append("]");
            if (reject.isSetField(Text.FIELD)) {
                String text = reject.getText().getValue();
                buf.append(": ").append(text);
            }

            // warning only if order was already filled
            OrdStatus ordStatus = reject.getOrdStatus();
            if (ordStatus.getValue() == OrdStatus.FILLED) {
                LOGGER.warn(buf.toString());
            } else {
                LOGGER.error(buf.toString());
            }
        }

        String intId = reject.getClOrdID().getValue();

        // get the order from the OpenOrderWindow
        Order order = getLookupService().getOpenOrderByRootIntId(intId);
        if (order == null) {

            if (LOGGER.isEnabledFor(Level.ERROR)) {

                LOGGER.error("Order with int ID " + intId + " matching the execution report could not be found");
            }
            return;
        }

        OrderStatus orderStatus = OrderStatus.Factory.newInstance();
        orderStatus.setStatus(Status.REJECTED);
        orderStatus.setIntId(intId);
        orderStatus.setSequenceNumber(reject.getHeader().getInt(MsgSeqNum.FIELD));
        orderStatus.setOrder(order);
        if (reject.isSetField(TransactTime.FIELD)) {

            orderStatus.setExtDateTime(reject.getTransactTime().getValue());
        }
        if (reject.isSetField(Text.FIELD)) {

            orderStatus.setReason(reject.getText().getValue());
        }

        EngineLocator.instance().getServerEngine().sendEvent(orderStatus);
    }

}
