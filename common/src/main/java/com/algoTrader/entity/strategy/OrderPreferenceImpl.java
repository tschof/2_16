package com.algoTrader.entity.strategy;

import org.apache.commons.beanutils.BeanUtils;

import com.algoTrader.entity.trade.Order;

public class OrderPreferenceImpl extends OrderPreference {

    private static final long serialVersionUID = -755368809250236972L;

    @Override
    public Order createOrder() {
        try {

            // create an order instance
            Class<?> orderClazz = Class.forName("com.algoTrader.entity.trade." + getOrderType() + "OrderImpl");
            Order order = (Order) orderClazz.newInstance();

            // populate the order with the properities
            BeanUtils.populate(order, getPropertyValueMap());

            return order;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {

        return getName() + " " + getOrderType() + " " + getPropertyValueMap();
    }
}
