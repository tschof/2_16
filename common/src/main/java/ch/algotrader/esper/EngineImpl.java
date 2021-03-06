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
package ch.algotrader.esper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationOperations;
import com.espertech.esper.client.ConfigurationVariable;
import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPOnDemandQueryResult;
import com.espertech.esper.client.EPPreparedStatement;
import com.espertech.esper.client.EPRuntime;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.SafeIterator;
import com.espertech.esper.client.StatementAwareUpdateListener;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.annotation.Name;
import com.espertech.esper.client.deploy.DeploymentException;
import com.espertech.esper.client.deploy.DeploymentInformation;
import com.espertech.esper.client.deploy.EPDeploymentAdmin;
import com.espertech.esper.client.deploy.Module;
import com.espertech.esper.client.deploy.ModuleItem;
import com.espertech.esper.client.deploy.ParseException;
import com.espertech.esper.client.soda.AnnotationPart;
import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.client.time.TimerControlEvent;
import com.espertech.esper.core.deploy.EPLModuleUtil;
import com.espertech.esper.core.service.EPServiceProviderImpl;
import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.epl.annotation.AnnotationUtil;
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.spec.AnnotationDesc;
import com.espertech.esper.epl.spec.StatementSpecMapper;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esperio.AdapterCoordinatorImpl;
import com.espertech.esperio.CoordinatedAdapter;

import ch.algotrader.config.ConfigParams;
import ch.algotrader.entity.PositionVO;
import ch.algotrader.entity.marketData.TickVO;
import ch.algotrader.entity.trade.OrderCompletionVO;
import ch.algotrader.entity.trade.OrderStatusVO;
import ch.algotrader.esper.annotation.Condition;
import ch.algotrader.esper.annotation.Listeners;
import ch.algotrader.esper.annotation.RunTimeOnly;
import ch.algotrader.esper.annotation.SimulationOnly;
import ch.algotrader.esper.annotation.Subscriber;
import ch.algotrader.esper.io.CustomSender;
import ch.algotrader.util.DateTimeUtil;
import ch.algotrader.util.metric.MetricsUtil;

/**
 * Esper based implementation of an {@link Engine}
 *
 * @author <a href="mailto:aflury@algotrader.ch">Andy Flury</a>
 */
public class EngineImpl extends AbstractEngine {

    private static final Logger LOGGER = LogManager.getLogger(EngineImpl.class);
    private static final String newline = System.getProperty("line.separator");

    private final ServiceResolver subscriberResolver;
    private final String[] initModules;
    private final String[] runModules;
    private final ConfigParams configParams;
    private final boolean simulation;
    private final EPServiceProvider serviceProvider;

    private volatile AdapterCoordinatorImpl coordinator;

    private final ThreadLocal<AtomicBoolean> processing = new ThreadLocal<AtomicBoolean>() {
        @Override
        protected AtomicBoolean initialValue() {
            return new AtomicBoolean(false);
        }
    };

    EngineImpl(final String strategyName, final ServiceResolver subscriberResolver,
               final Configuration configuration, final String[] initModules, final String[] runModules, final ConfigParams configParams) {

        super(strategyName);

        Validate.notNull(subscriberResolver, "SubscriberResolver is null");
        Validate.notNull(configuration, "Configuration is null");
        Validate.notNull(configParams, "ConfigParams is null");

        this.subscriberResolver = subscriberResolver;
        this.initModules = initModules;
        this.runModules = runModules;
        this.configParams = configParams;
        this.simulation = configParams.getBoolean("simulation");

        initVariables(configuration, configParams);
        configuration.getVariables().get("strategyName").setInitializationValue(strategyName);

        if (!this.simulation) {
            Properties props = new Properties();
            props.put("use-platform-mbean-server", "true");
            configuration.addPluginLoader("EsperJMX", "com.espertech.esper.jmx.client.EsperJMXPlugin", props);
        }

        this.serviceProvider = EPServiceProviderManager.getProvider(strategyName, configuration);
    }

    private void initVariables(final Configuration configuration, final ConfigParams configParams) {

        Map<String, ConfigurationVariable> variables = configuration.getVariables();
        for (Map.Entry<String, ConfigurationVariable> entry : variables.entrySet()) {
            String variableName = entry.getKey().replace("_", ".");
            String value = configParams.getString(variableName);
            if (value != null) {
                String type = entry.getValue().getType();
                try {
                    Class clazz = Class.forName(type);
                    Object typedObj;
                    if (clazz.isEnum()) {
                        typedObj = Enum.valueOf(clazz, value);
                    } else if (clazz == BigDecimal.class) {
                        typedObj = new BigDecimal(value);
                    } else {
                        typedObj = JavaClassHelper.parse(clazz, value);
                    }
                    entry.getValue().setInitializationValue(typedObj);
                } catch (ClassNotFoundException ex) {
                    throw new InternalEngineException("Unknown variable type: " + type);
                }
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.serviceProvider.isDestroyed();
    }

    @Override
    public void destroy() {

        this.serviceProvider.destroy();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("destroyed service provider: {}", getStrategyName());
        }
    }

    @Override
    public void initialize() {

        ((EPServiceProviderSPI) this.serviceProvider).initialize(0l);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("initialized service provider: {}", getStrategyName());
        }
    }

    @Override
    public void initServices() {
        this.subscriberResolver.resolveServices(this);
    }

    @Override
    public void deployStatement(String moduleName, String statementName) {

        deployStatement(moduleName, statementName, null, new Object[]{}, null);
    }

    @Override
    public void deployStatement(String moduleName, String statementName, String alias, Object... params) {

        deployStatement(moduleName, statementName, alias, params, null);
    }

    @Override
    public void deployStatement(String moduleName, String statementName, String alias, Object[] params, Object callback) {

        deployStatement(moduleName, statementName, alias, params, callback, false);
    }

    @Override
    public void deployStatement(String moduleName, String statementName, String alias, Object[] params, Object callback, boolean force) {

        // check if statement with same name exists
        if (!force) {
            EPStatement existingStatement = this.serviceProvider.getEPAdministrator().getStatement(statementName);
            if (existingStatement != null && existingStatement.isStarted()) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("{} is already deployed and started", statementName);
                }
                return;
            }
        }

        Module module = getModule(moduleName);

        // go through all statements in the module
        EPStatement newStatement = null;
        for (ModuleItem moduleItem : module.getItems()) {

            if (isEligibleStatement(this.serviceProvider, moduleItem, statementName)) {

                newStatement = startStatement(moduleItem, this.serviceProvider, alias, params, callback);

                // break iterating over the statements
                break;

            } else {
                continue;
            }
        }

        if (newStatement == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("statement {} was not found", statementName);
            }
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("deployed statement {}", newStatement.getName());
            }
        }
    }

    @Override
    public void deployAllModules() {

        deployInitModules();
        deployRunModules();
    }

    @Override
    public void deployInitModules() {

        if (this.initModules != null) {
            for (String module : this.initModules) {
                deployModule(module.trim());
            }
        }
    }

    @Override
    public void deployRunModules() {

        if (this.runModules != null) {
            for (String module : this.runModules) {
                deployModule(module.trim());
            }
        }
    }

    @Override
    public void deployModule(String moduleName) {

        Module module = getModule(moduleName);

        for (ModuleItem moduleItem : module.getItems()) {

            if (isEligibleStatement(this.serviceProvider, moduleItem, null)) {

                startStatement(moduleItem, this.serviceProvider, null, new Object[] {}, null);

            } else {
                continue;
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("deployed module {}", moduleName);
        }
    }

    @Override
    public boolean isDeployed(final String statementNameRegex) {

        // find the first statement that matches the given statementName regex
        String[] statementNames = findStatementNames(statementNameRegex);

        if (statementNames.length == 0) {
            return false;
        } else if (statementNames.length > 1) {
            LOGGER.error("more than one statement matches: {}", statementNameRegex);
        }

        // get the statement
        EPAdministrator administrator = this.serviceProvider.getEPAdministrator();
        EPStatement statement = administrator.getStatement(statementNames[0]);

        if (statement != null && statement.isStarted()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void undeployAllStatements() {

        for (String statementName : this.serviceProvider.getEPAdministrator().getStatementNames()) {

            EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);

            if (statement != null && statement.isStarted()) {
                statement.destroy();
            }
        }

        LOGGER.debug("undeployed all statements");
    }

    @Override
    public void undeployStatement(String statementName) {

        // destroy the statement
        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);

        if (statement != null && statement.isStarted()) {
            statement.destroy();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("undeployed statement {}", statementName);
            }
        }
    }

    @Override
    public void restartStatement(String statementName) {

        // destroy the statement
        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);

        if (statement != null && statement.isStarted()) {
            statement.stop();
            statement.start();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("restarted statement {}", statementName);
            }
        }
    }

    @Override
    public void undeployModule(String moduleName) {

        EPAdministrator administrator = this.serviceProvider.getEPAdministrator();
        EPDeploymentAdmin deployAdmin = administrator.getDeploymentAdmin();
        for (DeploymentInformation deploymentInformation : deployAdmin.getDeploymentInformation()) {
            if (deploymentInformation.getModule().getName().equals(moduleName)) {
                try {
                    deployAdmin.undeploy(deploymentInformation.getDeploymentId());
                } catch (DeploymentException ex) {
                    throw new InternalEngineException("module " + moduleName + " could no be undeployed", ex);
                }
            }
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("undeployed module {}", moduleName);
        }
    }

    @Override
    public void addEventType(String eventTypeName, String eventClassName) {

        ConfigurationOperations configuration = this.serviceProvider.getEPAdministrator().getConfiguration();
        if (configuration.getEventType(eventTypeName) == null) {
            configuration.addEventType(eventTypeName, eventClassName);
        }
    }

    @Override
    public void sendEvent(Object obj) {

        long startTime = System.nanoTime();

        // if the engine is currently processing and event route the new event
        if (this.processing.get().get()) {
            this.serviceProvider.getEPRuntime().route(obj);
        } else {
            this.processing.get().set(true);
            this.serviceProvider.getEPRuntime().sendEvent(obj);
            this.processing.get().set(false);
        }

        MetricsUtil.accountEnd("EngineImpl." + getStrategyName() + "." + ClassUtils.getShortClassName(obj.getClass()), startTime);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List executeQuery(String query) {

        List<Object> objects = new ArrayList<>();
        EPOnDemandQueryResult result = this.serviceProvider.getEPRuntime().executeQuery(query);
        for (EventBean row : result.getArray()) {
            Object object = row.getUnderlying();
            objects.add(object);
        }
        return objects;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object executeSingelObjectQuery(String query, String attributeName) {

        List<Object> events = executeQuery(query);
        if (events.size() == 0) {
            return null;
        } else if (events.size() == 1) {
            Object result = events.get(0);
            if (attributeName != null && result instanceof Map) {
                return ((Map<?, ?>) result).get(attributeName);
            } else {
                return result;
            }
        } else {
            throw new IllegalArgumentException("query returned more than one object");
        }
    }

    @Override
    public Object executeSingelObjectQuery(String query) {

        return executeSingelObjectQuery(query, null);
    }

    @Override
    public Object getLastEvent(String statementName) {

        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                if (it.hasNext()) {
                    return it.next().getUnderlying();
                } else {
                    return null;
                }
            } finally {
                it.close();
            }
        }
    }

    @Override
    public Object getLastEventProperty(String statementName, String propertyName) {

        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                if (it.hasNext()) {
                    return it.next().get(propertyName);
                } else {
                    return null;
                }
            } finally {
                it.close();
            }
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List getAllEvents(String statementName) {

        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            List<Object> list = new ArrayList<>();
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                while (it.hasNext()) {
                    EventBean bean = it.next();
                    Object underlying = bean.getUnderlying();
                    list.add(underlying);
                }
                return list;
            } finally {
                it.close();
            }
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List getAllEventsProperty(String statementName, String property) {

        EPStatement statement = this.serviceProvider.getEPAdministrator().getStatement(statementName);
        if (statement == null) {
            throw new IllegalStateException("statement " + statementName + " does not exist");
        } else if (!statement.isStarted()) {
            throw new IllegalStateException("statement " + statementName + " is not started");
        } else {
            List<Object> list = new ArrayList<>();
            SafeIterator<EventBean> it = statement.safeIterator();
            try {
                while (it.hasNext()) {
                    EventBean bean = it.next();
                    Object underlying = bean.get(property);
                    list.add(underlying);
                }
                return list;
            } finally {
                it.close();
            }
        }
    }

    @Override
    public void setInternalClock(boolean internalClock) {

        super.setInternalClock(internalClock);

        if (internalClock) {
            sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_INTERNAL));
            EPServiceProviderImpl provider = (EPServiceProviderImpl) this.serviceProvider;
            provider.getTimerService().enableStats();
        } else {
            sendEvent(new TimerControlEvent(TimerControlEvent.ClockType.CLOCK_EXTERNAL));
            EPServiceProviderImpl provider = (EPServiceProviderImpl) this.serviceProvider;
            provider.getTimerService().disableStats();
        }

        setVariableValue("internal_clock", internalClock);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("set internal clock to: {}", internalClock);
        }
    }

    @Override
    public long getCurrentTimeInMillis() {

        return this.serviceProvider.getEPRuntime().getCurrentTime();
    }

    @Override
    public Date getCurrentTime() {

        return new Date(this.serviceProvider.getEPRuntime().getCurrentTime());
    }

    @Override
    public void initCoordination() {

        this.coordinator = new AdapterCoordinatorImpl(this.serviceProvider, true, true, true);
        this.coordinator.setSender(new CustomSender());
    }

    @Override
    public void coordinate(CoordinatedAdapter inputAdapter) {

        inputAdapter.setEPService(this.serviceProvider);

        this.coordinator.coordinate(inputAdapter);
    }

    @Override
    public void startCoordination() {

        this.coordinator.start();
    }

    @Override
    public void setVariableValue(String variableName, Object value) {

        variableName = variableName.replace(".", "_");
        EPRuntime runtime = this.serviceProvider.getEPRuntime();
        if (runtime.getVariableValueAll().containsKey(variableName)) {
            runtime.setVariableValue(variableName, value);
        }
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setVariableValueFromString(String variableName, String value) {

        variableName = variableName.replace(".", "_");
        EPRuntime runtime = this.serviceProvider.getEPRuntime();
        if (runtime.getVariableValueAll().containsKey(variableName)) {
            Class clazz = runtime.getVariableValue(variableName).getClass();

            Object castedObj = null;
            if (clazz.isEnum()) {
                castedObj = Enum.valueOf(clazz, value);
            } else {
                castedObj = JavaClassHelper.parse(clazz, value);
            }
            runtime.setVariableValue(variableName, castedObj);
        }
    }

    @Override
    public Object getVariableValue(String variableName) {

        variableName = variableName.replace(".", "_");
        EPRuntime runtime = this.serviceProvider.getEPRuntime();
        return runtime.getVariableValue(variableName);
    }

    private void addTradeCallback(final boolean expectFullExecution, final Collection<String> orderIntIds, final BiConsumer<String, List<OrderStatusVO>> consumer) {

        Validate.notEmpty(orderIntIds, "orderIntIds is empty");

        // get the statement alias based on all security ids
        String alias = "ON_TRADE_COMPLETED_" + StringUtils.join(orderIntIds, "_");

        if (isDeployed(alias)) {

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("{} is already deployed", alias);
            }
        } else {

            Object[] params = new Object[] { orderIntIds.size(), orderIntIds };
            deployStatement("prepared", "ON_TRADE_COMPLETED", alias, params, new TradeCallback(this, alias, expectFullExecution, consumer));
        }
    }

    @Override
    public void addTradeCallback(Collection<String> orderIntIds, BiConsumer<String, List<OrderStatusVO>> consumer) {

        addTradeCallback(false, orderIntIds, consumer);
    }

    @Override
    public void addFullExecutionCallback(Collection<String> orderIntIds) {

        addTradeCallback(true, orderIntIds, null);
    }

    @Override
    public void addTradePersistedCallback(Collection<String> orderIntIds, Consumer<List<OrderCompletionVO>> consumer) {

        Validate.notEmpty(orderIntIds, "orderIntIds is empty");

        // get the statement alias based on all security ids
        String alias = "ON_TRADE_PERSISTED_" + StringUtils.join(orderIntIds, "_");

        if (isDeployed(alias)) {

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("{} is already deployed", alias);
            }
        } else {

            Object[] params = new Object[] { orderIntIds.size(), orderIntIds };
            deployStatement("prepared", "ON_TRADE_PERSISTED", alias, params, new TradePersistedCallback(this, alias, consumer));
        }
    }

    @Override
    public void addFirstTickCallback(Collection<Long> securityIds, BiConsumer<String, List<TickVO>> consumer) {

        Validate.notEmpty(securityIds, "orderIntIds is empty");

        String alias = "ON_FIRST_TICK_" + StringUtils.join(securityIds, "_");

        if (isDeployed(alias)) {

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("{} is already deployed", alias);
            }
        } else {

            long[] securityIdsArray = ArrayUtils.toPrimitive(securityIds.toArray(new Long[0]));
            deployStatement("prepared", "ON_FIRST_TICK", alias, new Object[]{securityIds.size(), securityIdsArray}, new TickCallback(this, alias, consumer));
        }
    }

    @Override
    public void addOpenPositionCallback(long securityId, Consumer<PositionVO> consumer) {

        String alias = "ON_OPEN_POSITION_" + securityId;

        if (isDeployed(alias)) {

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("{} is already deployed", alias);
            }
        } else {

            deployStatement("prepared", "ON_OPEN_POSITION", alias, new Object[]{securityId}, new OpenPositionCallback(this, alias, consumer));
        }
    }

    @Override
    public void addClosePositionCallback(long securityId, Consumer<PositionVO> consumer) {

        String alias = "ON_CLOSE_POSITION_" + securityId;

        if (isDeployed(alias)) {

            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("{} is already deployed", alias);
            }
        } else {

            deployStatement("prepared", "ON_CLOSE_POSITION", alias, new Object[] { securityId }, new ClosePositionCallback(this, alias, consumer));
        }
    }

    @Override
    public void addTimerCallback(Date dateTime, String name, Consumer<Date> consumer) {

        // execute callback immediately if dateTime is in the past
        if (dateTime.compareTo(getCurrentTime()) < 0) {
            consumer.accept(getCurrentTime());
        } else {
            String alias = "ON_TIMER_" + DateTimeUtil.formatAsGMT(dateTime.toInstant()).replace(" ", "_").replace(":", "-") + (name != null ? "_" + name : "");

            Calendar cal = Calendar.getInstance();
            cal.setTime(dateTime);

            Object[] params = { alias, cal.get(Calendar.MINUTE), cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.SECOND),
                    cal.get(Calendar.YEAR) };

            deployStatement("prepared", "ON_TIMER", alias, params, new TimerCallback(this, alias, consumer), true);
        }
    }

    private EPStatement startStatement(ModuleItem moduleItem, EPServiceProvider serviceProvider, String alias, Object[] params, Object callback) {

        // create the statement and set the prepared statement params if a prepared statement
        EPStatement statement;
        String expression = moduleItem.getExpression();
        EPAdministrator administrator = serviceProvider.getEPAdministrator();
        if (expression.contains("?")) {
            EPPreparedStatement prepared = administrator.prepareEPL(expression);
            for (int i = 0; i < params.length; i++) {
                prepared.setObject(i + 1, params[i]);
            }
            if (alias != null) {
                statement = administrator.create(prepared, alias);
            } else {
                statement = administrator.create(prepared);
            }
        } else {
            if (alias != null) {
                statement = administrator.createEPL(expression, alias);
            } else {
                statement = administrator.createEPL(expression);
            }
        }

        // process annotations
        Annotation[] annotations = statement.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Subscriber) {

                this.subscriberResolver.resolve(statement, ((Subscriber) annotation).className());

            } else if (annotation instanceof Listeners) {

                Listeners listeners = (Listeners) annotation;
                for (String className : listeners.classNames()) {
                    try {
                        Class<?> cl = Class.forName(className);
                        Object obj = cl.newInstance();
                        if (obj instanceof StatementAwareUpdateListener) {
                            statement.addListener((StatementAwareUpdateListener) obj);
                        } else {
                            statement.addListener((UpdateListener) obj);
                        }
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException ex) {
                        throw new InternalEngineException("listener " + className + " could not be created for statement " + statement.getName(), ex);
                    }
                }
            }
        }

        // attach the callback if supplied
        // will override the Subscriber defined in Annotations
        // in live trading stop the statement before attaching (and restart afterwards)
        // to make sure that the subscriber receives the first event
        if (callback != null) {
            if (this.simulation) {
                statement.setSubscriber(callback);
            } else {
                statement.stop();
                statement.setSubscriber(callback);
                statement.start();
            }
        }
        return statement;
    }

    private Module getModule(String moduleName) {

        try {

            String fileName = "module-" + moduleName + ".epl";
            InputStream stream = getClass().getResourceAsStream("/" + fileName);
            if (stream == null) {
                throw new IllegalArgumentException(fileName + " does not exist");
            }

            // process loads
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                StringWriter buffer = new StringWriter();
                String strLine;
                while ((strLine = reader.readLine()) != null) {
                    if (!strLine.startsWith("load")) {
                        buffer.append(strLine);
                        buffer.append(newline);
                    } else {
                        String argument = StringUtils.substringAfter(strLine, "load").trim();
                        String moduleBaseName = argument.split("\\.")[0];
                        String statementName = argument.split("\\.")[1].split(";")[0];
                        Module module = EPLModuleUtil.readResource("module-" + moduleBaseName + ".epl");
                        for (ModuleItem item : module.getItems()) {
                            if (item.getExpression().contains("@Name('" + statementName + "')")) {
                                buffer.append(item.getExpression());
                                buffer.append(";");
                                buffer.append(newline);
                                break;
                            }
                        }
                    }
                }

                return EPLModuleUtil.parseInternal(buffer.toString(), fileName);

            }

        } catch (ParseException | IOException ex) {
            throw new InternalEngineException(moduleName + " could not be deployed", ex);
        }
    }

    private boolean isEligibleStatement(EPServiceProvider serviceProvider, ModuleItem item, String statementName) {

        if (item.isCommentOnly()) {
            return false;
        }

        String expression = item.getExpression().replace("?", "1"); // replace ? to prevent error during compile
        EPStatementObjectModel objectModel = serviceProvider.getEPAdministrator().compileEPL(expression);
        List<AnnotationPart> annotationParts = objectModel.getAnnotations();
        List<AnnotationDesc> annotationDescs = StatementSpecMapper.mapAnnotations(annotationParts);

        EngineImportService engineImportService = ((EPServiceProviderSPI) serviceProvider).getEngineImportService();
        Annotation[] annotations = AnnotationUtil.compileAnnotations(annotationDescs, engineImportService, item.getExpression());

        String excludeStatements = this.configParams.getString("misc.moduleDeployExcludeStatements");
        Set<String> moduleDeployExcludeStatements = new HashSet<>();
        if (excludeStatements != null) {
            moduleDeployExcludeStatements.addAll(Arrays.asList(excludeStatements.split(",")));
        }

        for (Annotation annotation : annotations) {
            if (annotation instanceof Name) {

                Name name = (Name) annotation;
                if (statementName != null && !statementName.equals(name.value())) {
                    return false;
                } else if (moduleDeployExcludeStatements.contains(name.value())) {
                    return false;
                }

            } else if (this.simulation && annotation instanceof RunTimeOnly) {

                return false;

            } else if (!this.simulation && annotation instanceof SimulationOnly) {

                return false;

            } else if (annotation instanceof Condition) {

                Condition condition = (Condition) annotation;
                String key = condition.key();
                if (!this.configParams.getBoolean(key)) {
                    return false;
                }
            }
        }

        return true;
    }

    private String[] findStatementNames(final String statementNameRegex) {

        EPAdministrator administrator = this.serviceProvider.getEPAdministrator();

        // find the first statement that matches the given statementName regex
        Collection<String> names = CollectionUtils.select(Arrays.asList(administrator.getStatementNames()), statement -> statement.matches(statementNameRegex));
        return names.toArray(new String[names.size()]);
    }

}
