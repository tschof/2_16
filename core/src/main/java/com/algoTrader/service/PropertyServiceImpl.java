package com.algoTrader.service;

import org.apache.log4j.Logger;

import com.algoTrader.entity.Property;
import com.algoTrader.entity.PropertyHolder;
import com.algoTrader.util.MyLogger;

public class PropertyServiceImpl extends PropertyServiceBase {

    private static Logger logger = MyLogger.getLogger(PropertyServiceImpl.class.getName());

    @Override
    protected PropertyHolder handleAddProperty(int propertyHolderId, String name, Object value, boolean persistent) throws Exception {

        // reattach the propertyHolder
        PropertyHolder propertyHolder = getPropertyHolderDao().load(propertyHolderId);

        Property property = propertyHolder.getProperties().get(name);
        if (property == null) {

            // create the property
            property = Property.Factory.newInstance();
            property.setName(name);
            property.setValue(value);
            property.setPersistent(persistent);

            // associate the propertyHolder
            propertyHolder.addProperties(name, property);

            getPropertyDao().create(property);

        } else {

            property.setValue(value);
        }

        logger.info("added property " + name + " value " + value + " to " + propertyHolder);

        return propertyHolder;
    }

    @Override
    protected PropertyHolder handleRemoveProperty(int propertyHolderId, String name) throws Exception {

        PropertyHolder propertyHolder = getPropertyHolderDao().load(propertyHolderId);
        Property property = propertyHolder.getProperties().get(name);

        if (property != null) {

            getPropertyDao().remove(property);

            propertyHolder.removeProperties(name);
        }

        logger.info("removed property " + name + " from " + propertyHolder);

        return propertyHolder;
    }
}