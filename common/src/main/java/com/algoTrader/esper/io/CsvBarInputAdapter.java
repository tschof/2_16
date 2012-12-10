package com.algoTrader.esper.io;

import com.algoTrader.enumeration.Duration;
import com.algoTrader.vo.RawBarVO;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esperio.SendableBeanEvent;
import com.espertech.esperio.SendableEvent;
import com.espertech.esperio.csv.CSVInputAdapter;

public class CsvBarInputAdapter extends CSVInputAdapter {

    private CsvBarInputAdapterSpec spec;

    public CsvBarInputAdapter(EPServiceProvider epService, CsvBarInputAdapterSpec spec) {

        super(epService, spec);
        this.spec = spec;
    }

    @Override
    public SendableEvent read() throws EPException {

        SendableBeanEvent event = (SendableBeanEvent) super.read();

        if (event != null && event.getBeanToSend() instanceof RawBarVO) {

            RawBarVO bar = (RawBarVO) event.getBeanToSend();

            String isin = this.spec.getFile().getName().split("\\.")[0];
            bar.setIsin(isin);

            Duration barSize = this.spec.getBarSize();
            bar.setBarSize(barSize);
        }
        return event;
    }
}
