package com.espertech.esperio.csv;

import com.algoTrader.ServiceLocator;
import com.algoTrader.entity.Security;
import com.algoTrader.entity.Tick;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esperio.SendableBeanEvent;
import com.espertech.esperio.SendableEvent;

public class TickCSVInputAdapter extends CSVInputAdapter {

    private int securityId;

    public TickCSVInputAdapter(EPServiceProvider epService, CSVInputAdapterSpec spec, int id) {

        super(epService, spec);
        securityId = id;
    }

    public SendableEvent read() throws EPException {
        SendableBeanEvent event = (SendableBeanEvent)super.read();

        if (event != null) {
            Tick tick = (Tick)event.getBeanToSend();
            Security security = ServiceLocator.instance().getLookupService().getSecurity(securityId);
            tick.setSecurity(security);
        }

        return event;
    }
}
