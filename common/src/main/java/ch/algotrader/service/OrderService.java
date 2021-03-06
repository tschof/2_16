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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import ch.algotrader.entity.trade.Order;
import ch.algotrader.entity.trade.OrderDetailsVO;
import ch.algotrader.entity.trade.OrderI;
import ch.algotrader.entity.trade.OrderStatusVO;
import ch.algotrader.entity.trade.OrderVO;
import ch.algotrader.entity.trade.OrderValidationException;

/**
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public interface OrderService {

    /**
     * Creates a new Order based on the {@link ch.algotrader.entity.trade.OrderPreference
     * OrderPreference} selected by its {@code name}.
     */
    public Order createOrderByOrderPreference(final String name);

    /**
     * Validates an Order. It is suggested to call this method by itself prior to sending an Order.
     * However {@link #sendOrder} will invoke this method again.
     */
    public void validateOrder(Order order) throws OrderValidationException;

    /**
     * Sends an Order.
     */
    public String sendOrder(Order order);

    /**
     * Sends an Order.
     */
    public String sendOrder(OrderVO order);

    /**
     * Sends multiple Orders.
     */
    public Collection<String> sendOrders(Collection<Order> orders);

    /**
     * Cancels an Order.
     */
    public String cancelOrder(Order order);

    /**
     * Cancels an Order by its {@code intId}.
     */
    public String cancelOrder(String intId);

    /**
     * Cancels all Orders.
     */
    public void cancelAllOrders();

    /**
     * Modifies an Order by overwriting the current Order with the Order passed to this method.
     */
    public String modifyOrder(Order order);

    /**
     * Modifies an Order defined by its {@code intId} by overwriting the current Order with the
     * defined {@code properties}.
     */
    public String modifyOrder(String intId, Map<String, String> properties);

    /**
     * Modifies an Order by overwriting the current Order with the Order passed to this method.
     */
    public String modifyOrder(OrderVO order);

    /**
     * Generates next order intId for the given account.
     */
    public String getNextOrderId(Class<? extends OrderI> orderClass, long accountId);

    /**
     * Returns details of currently open orders.
     */
    public List<OrderDetailsVO> getOpenOrderDetails();

    /**
     * Returns details of recently executed orders.
     */
    public List<OrderDetailsVO> getRecentOrderDetails();

    /**
     * Returns execution status of the order with the given {@code IntId} or {@code null}
     * if an order with this {@code IntId} has been fully executed.
     */
    OrderStatusVO getStatusByIntId(String intId);

    /**
     * Gets an open order by its {@code intId}.
     */
    public Order getOpenOrderByIntId(String intId);

    /**
     * Returns open orders for the given strategy.
     */
    List<Order> getOpenOrdersByStrategy(long strategyId);

    /**
     * Returns open ordesr for the given security.
     */
    List<Order> getOpenOrdersBySecurity(long securityId);

    /**
     * Returns open orders for the given strategy and security.
     */
    List<Order> getOpenOrdersByStrategyAndSecurity(long strategyId, long securityId);

    /**
     * Gets an order (open or completed) by its {@code intId}.
     */
    public Order getOrderByIntId(String intId);

    /**
     * Evicts executed orders from the internal cache.
     */
    public void evictExecutedOrders();

    /**
     * Sends a Trade Suggestion via Email / Text Message.
     */
    public void suggestOrder(Order order);

}
