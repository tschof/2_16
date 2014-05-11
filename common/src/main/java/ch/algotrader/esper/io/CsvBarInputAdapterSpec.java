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
package ch.algotrader.esper.io;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import com.espertech.esperio.AdapterInputSource;
import com.espertech.esperio.csv.CSVInputAdapterSpec;

import ch.algotrader.enumeration.Duration;
import ch.algotrader.util.CustomDate;

/**
 * A {@link CSVInputAdapterSpec} used to input {@link ch.algotrader.entity.marketData.Bar Bars}.
 * Will use {@code dateTime} as {@code timestampColumn}.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
public class CsvBarInputAdapterSpec extends CSVInputAdapterSpec {

    private File file;
    private Duration barSize;

    public CsvBarInputAdapterSpec(File file, Duration barSize) {

        super(new AdapterInputSource(file), "RawBar");

        this.file = file;
        this.barSize = barSize;

        //@formatter:off
        String[] barPropertyOrder = new String[] {
                "dateTime",
                "open",
                "high",
                "low",
                "close",
                "vol",
                "security"};
        //@formatter:on

        setPropertyOrder(barPropertyOrder);

        Map<String, Object> barPropertyTypes = new HashMap<String, Object>();

        barPropertyTypes.put("dateTime", CustomDate.class);
        barPropertyTypes.put("open", BigDecimal.class);
        barPropertyTypes.put("high", BigDecimal.class);
        barPropertyTypes.put("low", BigDecimal.class);
        barPropertyTypes.put("close", BigDecimal.class);
        barPropertyTypes.put("vol", int.class);
        barPropertyTypes.put("security", String.class);

        setPropertyTypes(barPropertyTypes);

        setTimestampColumn("dateTime");

        setUsingExternalTimer(true);
    }

    public File getFile() {

        return this.file;
    }

    public Duration getBarSize() {
        return this.barSize;
    }
}
