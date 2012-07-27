package com.algoTrader.service.ib;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;

import com.algoTrader.entity.StrategyImpl;
import com.algoTrader.entity.marketData.Tick;
import com.algoTrader.entity.security.Security;
import com.algoTrader.enumeration.ConnectionState;
import com.algoTrader.esper.EsperManager;
import com.algoTrader.util.MyLogger;
import com.algoTrader.vo.SubscribeTickVO;
import com.algoTrader.vo.UnsubscribeTickVO;
import com.ib.client.Contract;

public class IBMarketDataServiceImpl extends IBMarketDataServiceBase implements DisposableBean {

    private static final long serialVersionUID = -4704799803078842628L;
    private static Logger logger = MyLogger.getLogger(IBMarketDataServiceImpl.class.getName());
    private static IBClient client;

    private @Value("${simulation}") boolean simulation;
    private @Value("${ib.genericTickList}") String genericTickList;

    @Override
    protected void handleInit() throws Exception {

        client = IBClient.getDefaultInstance();
    }

    @Override
    protected void handleConnect() {

        client.connect();
    }

    @Override
    protected ConnectionState handleGetConnectionState() {

        if (client == null) {
            return ConnectionState.DISCONNECTED;
        } else {
            return client.getMessageHandler().getState();
        }
    }

    @Override
    protected void handleInitSubscriptions() {

        if (client != null
                && (client.getMessageHandler().getState().equals(ConnectionState.READY) || client.getMessageHandler().getState().equals(ConnectionState.SUBSCRIBED))
                && !client.getMessageHandler().isRequested() && !this.simulation) {

            client.getMessageHandler().setRequested(true);
            client.getMessageHandler().setState(ConnectionState.SUBSCRIBED);

            super.handleInitSubscriptions();
        }
    }

    @Override
    protected void handleExternalSubscribe(Security security) throws Exception {

        if (!client.getMessageHandler().getState().equals(ConnectionState.READY) && !client.getMessageHandler().getState().equals(ConnectionState.SUBSCRIBED)) {
            throw new IBMarketDataServiceException("IB is not ready for market data subscription on " + security);
        }

        // create the SubscribeTickEvent (must happen before reqMktData so that Esper is ready to receive marketdata)
        int tickerId = RequestIDGenerator.singleton().getNextRequestId();
        Tick tick = Tick.Factory.newInstance();
        tick.setSecurity(security);

        SubscribeTickVO subscribeTickEvent = new SubscribeTickVO();
        subscribeTickEvent.setTick(tick);
        subscribeTickEvent.setTickerId(tickerId);

        EsperManager.sendEvent(StrategyImpl.BASE, subscribeTickEvent);

        // requestMarketData from IB
        Contract contract = IBUtil.getContract(security);

        client.reqMktData(tickerId, contract, this.genericTickList, false);

        logger.debug("request " + tickerId + " for : " + security);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void handleExternalUnsubscribe(Security security) throws Exception {

        if (!client.getMessageHandler().getState().equals(ConnectionState.SUBSCRIBED)) {
            throw new IBMarketDataServiceException("IB ist not subscribed, security cannot be unsubscribed " + security);
        }

        // get the tickerId by querying the TickWindow
        List<Map> events = EsperManager.executeQuery(StrategyImpl.BASE, "select tickerId from TickWindow where security.id = " + security.getId());

        if (events.size() == 1) {

            Integer tickerId = (Integer) events.get(0).get("tickerId");
            client.cancelMktData(tickerId);

            UnsubscribeTickVO unsubscribeTickEvent = new UnsubscribeTickVO();
            unsubscribeTickEvent.setSecurityId(security.getId());
            EsperManager.sendEvent(StrategyImpl.BASE, unsubscribeTickEvent);

            logger.debug("cancelled market data for : " + security);
        } else {
            throw new IBMarketDataServiceException("tickerId for security " + security + " was not found");
        }
    }

    @Override
    public void destroy() throws Exception {

        if (client != null && client.isConnected()) {
            client.disconnect();
        }
    }
}
