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
package ch.algotrader.adapter.tt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.algotrader.adapter.fix.DropCopyAllocationVO;
import ch.algotrader.adapter.fix.DropCopyAllocator;
import ch.algotrader.adapter.fix.FixApplicationException;
import ch.algotrader.adapter.fix.FixUtil;
import ch.algotrader.adapter.fix.fix42.GenericFix42OrderMessageHandler;
import ch.algotrader.entity.exchange.Exchange;
import ch.algotrader.entity.trade.ExternalFill;
import ch.algotrader.entity.trade.LimitOrder;
import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderStatus;
import ch.algotrader.entity.trade.SimpleOrder;
import ch.algotrader.entity.trade.StopLimitOrder;
import ch.algotrader.entity.trade.StopOrder;
import ch.algotrader.enumeration.Broker;
import ch.algotrader.enumeration.Status;
import ch.algotrader.enumeration.TIF;
import ch.algotrader.esper.Engine;
import ch.algotrader.service.OrderExecutionService;
import ch.algotrader.service.ServiceException;
import ch.algotrader.service.TransactionService;
import ch.algotrader.util.BeanUtil;
import ch.algotrader.util.PriceUtil;
import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.field.ExecTransType;
import quickfix.field.ExecType;
import quickfix.field.ExpireDate;
import quickfix.field.MsgSeqNum;
import quickfix.field.MsgType;
import quickfix.field.SendingTime;
import quickfix.fix42.BusinessMessageReject;
import quickfix.fix42.ExecutionReport;

/**
 * Trading Technology specific FIX/4.2 order message handler.
 *
 * @author <a href="mailto:okalnichevski@algotrader.ch">Oleg Kalnichevski</a>
 */
public class TTFixOrderMessageHandler extends GenericFix42OrderMessageHandler {

    private static final Logger LOGGER = LogManager.getLogger(TTFixOrderMessageHandler.class);

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderExecutionService orderExecutionService;
    private final TransactionService transactionService;
    private final Engine serverEngine;
    private final DropCopyAllocator dropCopyAllocator;

    public TTFixOrderMessageHandler(final OrderExecutionService orderExecutionService, final TransactionService transactionService, final Engine serverEngine, final DropCopyAllocator dropCopyAllocator) {
        super(orderExecutionService, transactionService, serverEngine);
        this.orderExecutionService = orderExecutionService;
        this.transactionService = transactionService;
        this.serverEngine = serverEngine;
        this.dropCopyAllocator = dropCopyAllocator;
    }

    @Override
    protected boolean discardReport(final ExecutionReport executionReport) throws FieldNotFound {

        if (executionReport.isSetExecTransType()) {
            ExecTransType execTransType = executionReport.getExecTransType();
            if (execTransType.getValue() == ExecTransType.STATUS) {
                if (LOGGER.isInfoEnabled()) {
                    if (executionReport.isSetClOrdID()) {
                        String orderIntId = executionReport.getClOrdID().getValue();
                        LOGGER.info("Working order at the TT side: IntId = {}", orderIntId);
                    } else {
                        String orderExtId = executionReport.getOrderID().getValue();
                        LOGGER.info("Working order at the TT side: ExtId = {}", orderExtId);
                    }
                }
                return true;
            }
        }
        return super.discardReport(executionReport);
    }

    @Override
    protected void handleExternal(final ExecutionReport executionReport) throws FieldNotFound {

        if (this.dropCopyAllocator == null) {
            super.handleExternal(executionReport);
        } else {
            handleExternalReport(executionReport);
        }
    }

    @Override
    protected void handleUnknown(final ExecutionReport executionReport) throws FieldNotFound {

        if (this.dropCopyAllocator == null) {
            super.handleUnknown(executionReport);
        } else {
            handleExternalReport(executionReport);
        }
    }

    private void handleExternalReport(final ExecutionReport executionReport) throws FieldNotFound {

        String extId = executionReport.getOrderID().getValue();

        ExecType execType = executionReport.getExecType();
        Status status = getStatus(execType, (long) executionReport.getCumQty().getValue());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Received order status {} for external order {}", status, extId);
        }

        if (status != Status.PARTIALLY_EXECUTED && status != Status.EXECUTED) {
            return;
        }

        if (executionReport.isSetExecTransType() && executionReport.getExecTransType().getValue() != ExecTransType.NEW) {
            return;
        }

        String ttid = executionReport.getSecurityID().getValue();
        String extAccount = executionReport.getAccount().getValue();

        DropCopyAllocationVO allocation = this.dropCopyAllocator.allocate(ttid, extAccount);
        if (allocation == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Unable to allocate an external (drop-copy) fill: " +
                        "extID = {}, TT security id = {}, external account = {}", extId, ttid, extAccount);
            }
            return;
        }
        if (allocation.getStrategy() == null) {
            throw new FixApplicationException("External (drop-copy) fill could not be allocated to a strategy");
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("External (drop-copy) fill {} allocated to " +
                    "strategy {},  account = {}", extId, allocation.getStrategy(), allocation.getAccount());
        }

        ExternalFill fill = createFill(executionReport, allocation);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(fill);
        }

        if (fill != null) {
            this.serverEngine.sendEvent(fill);
            this.transactionService.createTransaction(fill);
            this.orderExecutionService.handleFill(fill);
        }
    }

    @Override
    protected void handleRestated(final ExecutionReport executionReport, final Order order) throws FieldNotFound {

        SimpleOrder restatedOrder;
        try {
            restatedOrder = BeanUtil.clone((SimpleOrder) order);
            restatedOrder.setId(0);
        } catch (ReflectiveOperationException ex) {
            throw new ServiceException(ex);
        }

        String extId = executionReport.getOrderID().getValue();
        restatedOrder.setExtId(extId);

        long quantity = (long) executionReport.getOrderQty().getValue();
        restatedOrder.setQuantity(quantity);

        if (restatedOrder instanceof LimitOrder) {
            LimitOrder limitOrder = (LimitOrder) restatedOrder;
            BigDecimal limit = PriceUtil.normalizePrice(order, executionReport.getPrice().getValue());
            limitOrder.setLimit(limit);
        } else if (restatedOrder instanceof StopOrder) {
            StopOrder stopOrder = (StopOrder) restatedOrder;
            BigDecimal stopPrice = PriceUtil.normalizePrice(order, executionReport.getStopPx().getValue());
            stopOrder.setStop(stopPrice);
        } else if (restatedOrder instanceof StopLimitOrder) {
            StopLimitOrder stopLimitOrder = (StopLimitOrder) restatedOrder;
            BigDecimal limit = PriceUtil.normalizePrice(order, executionReport.getPrice().getValue());
            stopLimitOrder.setLimit(limit);
            BigDecimal stopPrice = PriceUtil.normalizePrice(order, executionReport.getStopPx().getValue());
            stopLimitOrder.setStop(stopPrice);
        }

        TIF tif = FixUtil.getTimeInForce(executionReport.getTimeInForce());
        restatedOrder.setTif(tif);
        if (tif == TIF.GTD && executionReport.isSetExpireDate()) {
            ExpireDate expireDate = executionReport.getExpireDate();
            LocalDate localDate = DATE_FORMAT.parse(expireDate.getValue(), LocalDate::from);
            Exchange exchange = restatedOrder.getSecurity().getSecurityFamily().getExchange();
            TimeZone timeZone;
            if (exchange.getTimeZone() != null) {
                timeZone = TimeZone.getTimeZone(exchange.getTimeZone());
            } else {
                timeZone = TimeZone.getDefault();
            }
            Instant instant = localDate.atStartOfDay(timeZone.toZoneId()).toInstant();
            restatedOrder.setTifDateTime(new Date(instant.toEpochMilli()));
        }

        this.orderExecutionService.handleRestatedOrder(order, restatedOrder);

        OrderStatus orderStatus = createStatus(executionReport, restatedOrder);

        this.serverEngine.sendEvent(orderStatus);
        this.orderExecutionService.handleOrderStatus(orderStatus);
    }

    @Override
    public void onMessage(final BusinessMessageReject reject, final SessionID sessionID) throws FieldNotFound {

        String refMsgType = reject.getRefMsgType().getValue();
        if (refMsgType.equalsIgnoreCase(MsgType.ORDER_SINGLE) && reject.isSetBusinessRejectRefID()) {
            String intId = reject.getBusinessRejectRefID().getValue();
            Order order = this.orderExecutionService.getOpenOrderByIntId(intId);
            if (order != null) {
                if (LOGGER.isErrorEnabled()) {

                    StringBuilder buf = new StringBuilder();
                    buf.append("Order with IntID ").append(intId).append(" has been rejected");
                    if (reject.isSetText()) {

                        buf.append("; reason given: ").append(reject.getText().getValue());
                    }
                    LOGGER.error(buf.toString());
                }

                OrderStatus orderStatus = OrderStatus.Factory.newInstance();
                orderStatus.setStatus(Status.REJECTED);
                orderStatus.setIntId(intId);
                orderStatus.setSequenceNumber(reject.getHeader().getInt(MsgSeqNum.FIELD));
                orderStatus.setOrder(order);
                orderStatus.setExtDateTime(reject.getHeader().getUtcTimeStamp(SendingTime.FIELD));
                if (reject.isSetBusinessRejectReason()) {

                    orderStatus.setReason(reject.getText().getValue());
                }

                this.serverEngine.sendEvent(orderStatus);
                this.orderExecutionService.handleOrderStatus(orderStatus);
                return;
            }
        }
        super.onMessage(reject, sessionID);
    }

    @Override
    protected String getDefaultBroker() {
        return Broker.TT.name();
    }

}
