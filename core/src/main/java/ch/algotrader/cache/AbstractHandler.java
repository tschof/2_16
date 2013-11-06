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
package ch.algotrader.cache;

/**
 * Abstract Cache Handler class.
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 *
 * @version $Revision$ $Date$
 */
abstract class AbstractHandler {

    /**
     * return true if this Handler is responsible for the specified class
     */
    protected abstract boolean handles(Class<?> clazz);

    /**
     * Puts this object into the Cache or returns the existingObject if it was already in the Cache
     */
    protected abstract Object put(Object obj);

    /**
     * Invokes an update of the specified Object. Returns the updated Object
     */
    protected abstract Object update(Object obj);

    /**
     * Lazy-initializes the specified Object. Return null if the obj is not a Proxy or uninitialized PersistentCollection
     */
    protected abstract Object initialize(Object obj);
}
