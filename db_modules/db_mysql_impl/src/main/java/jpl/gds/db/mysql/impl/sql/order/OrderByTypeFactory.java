/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.db.mysql.impl.sql.order;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ClassUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.order.IChannelAggregateOrderByType;
import jpl.gds.db.api.sql.order.IChannelValueOrderByType;
import jpl.gds.db.api.sql.order.ICommandOrderByType;
import jpl.gds.db.api.sql.order.IDbOrderByType;
import jpl.gds.db.api.sql.order.IEcdrMonitorOrderByType;
import jpl.gds.db.api.sql.order.IEcdrOrderByType;
import jpl.gds.db.api.sql.order.IEvrOrderByType;
import jpl.gds.db.api.sql.order.IFrameOrderByType;
import jpl.gds.db.api.sql.order.ILogOrderByType;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.order.IPacketOrderByType;
import jpl.gds.db.api.sql.order.IProductOrderByType;
import jpl.gds.db.api.sql.order.ISessionOrderByType;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.shared.types.Pair;


public class OrderByTypeFactory implements IOrderByTypeFactory {
    private static final String                  METHOD_NAME = "getOrderBy";
    private static final Map<Class<?>[], Method> methodCache  = new HashMap<Class<?>[], Method>();

    @SuppressWarnings("unused")
    private final ApplicationContext             appContext;

    @SuppressWarnings("serial")
    private class OrderByInterfaceAndClass
            extends Pair<Class<? extends IDbOrderByType>, Class<? extends AbstractOrderByType>> {
        public OrderByInterfaceAndClass(final Class<? extends IDbOrderByType> iface,
                final Class<? extends AbstractOrderByType> concreteClass) {
            super(iface, concreteClass);
        }

        /**
         * @return the Interface that this OrderByType implements
         */
        public Class<? extends IDbOrderByType> getInterfaceType() {
            return getOne();
        }

        /**
         * @return the Concrete Class of this OrderByType
         */
        public Class<? extends AbstractOrderByType> getConcreteClass() {
            return getTwo();
        }
    }

    // @formatter:off
    @SuppressWarnings("serial")
    private final Map<OrderByType, OrderByInterfaceAndClass> orderByMap = new HashMap<OrderByType, OrderByInterfaceAndClass>() {{
        put(OrderByType.CHANNEL_VALUE_ORDER_BY, new OrderByInterfaceAndClass(IChannelValueOrderByType.class, ChannelValueOrderByType.class));
        put(OrderByType.COMMAND_ORDER_BY, new OrderByInterfaceAndClass(ICommandOrderByType.class, CommandOrderByType.class));
        put(OrderByType.ECDR_MONITOR_ORDER_BY, new OrderByInterfaceAndClass(IEcdrMonitorOrderByType.class, EcdrMonitorOrderByType.class));
        put(OrderByType.ECDR_ORDER_BY, new OrderByInterfaceAndClass(IEcdrOrderByType.class, EcdrOrderByType.class));
        put(OrderByType.EVR_ORDER_BY, new OrderByInterfaceAndClass(IEvrOrderByType.class, EvrOrderByType.class));
        put(OrderByType.FRAME_ORDER_BY, new OrderByInterfaceAndClass(IFrameOrderByType.class, FrameOrderByType.class));
        put(OrderByType.LOG_ORDER_BY, new OrderByInterfaceAndClass(ILogOrderByType.class, LogOrderByType.class));
        put(OrderByType.PACKET_ORDER_BY, new OrderByInterfaceAndClass(IPacketOrderByType.class, PacketOrderByType.class));
        put(OrderByType.PRODUCT_ORDER_BY, new OrderByInterfaceAndClass(IProductOrderByType.class, ProductOrderByType.class));
        put(OrderByType.SESSION_ORDER_BY, new OrderByInterfaceAndClass(ISessionOrderByType.class, SessionOrderByType.class));
        put(OrderByType.CHANNEL_AGGREGATE_ORDER_BY, new OrderByInterfaceAndClass(IChannelAggregateOrderByType.class, ChannelAggregateOrderByType.class));
    }};
    // @formatter:on

    /**
     * No-Arg Constructor
     */
    public OrderByTypeFactory() {
        this(null);
    }

    /**
     * @param appContext
     *            the Spring Application Context (Currently not used -- may be null)
     */
    public OrderByTypeFactory(final ApplicationContext appContext) {
        this.appContext = appContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IDbOrderByType getOrderByType(final OrderByType orderByType, final Object... args) {
        final Class<?>[] argTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            final Class<?> clazz = args[i].getClass();
            final Class<?> primitive = ClassUtils.wrapperToPrimitive(clazz);
            argTypes[i] = (null == primitive) ? clazz : primitive;
        }
        final OrderByInterfaceAndClass iAndC = orderByMap.get(orderByType);
        final Class<? extends IDbOrderByType> iface = iAndC.getInterfaceType();
        Method theMethod = methodCache.get(argTypes);
        try {
            if (null == theMethod) {
                final Class<? extends AbstractOrderByType> klass = iAndC.getConcreteClass();
                theMethod = klass.getMethod(METHOD_NAME, argTypes);
                methodCache.put(argTypes, theMethod);
            }
            return iface.cast(theMethod.invoke(null, args));
        }
        catch (IllegalArgumentException | NoSuchMethodException | SecurityException
                | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to create OrderBy: " + e.toString(), e);
        }
    }
}
