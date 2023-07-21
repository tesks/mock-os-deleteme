package jpl.gds.product.context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.spring.context.IContextContainer;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.message.api.portal.IMessagePortal;

/**
 * Class PdppContextContainer
 */
public class PdppContextContainer implements IPdppContextContainer {
	
	private final IContextConfiguration parent;
	private final IContextConfiguration child;
	private final ApplicationContext childContext;
	private final StoreIdentifier[] stores;
	private final boolean useJms;
	private final boolean useDb;
	
	private boolean messagePortalStarted;
	private final Map<Object, ServiceHolder> registered;
	
	private final AtomicBoolean isServicesStarted;
	
	/**
	 * @param parent2
	 * @param childContext
	 * @param child2
	 */
	public PdppContextContainer(final IContextConfiguration parent2, 
			final ApplicationContext childContext,
			final IContextConfiguration child2,
			final StoreIdentifier[] stores,
			final boolean useJms,
			final boolean useDb
			) {
		this.parent = parent2;
		this.child = child2;
		this.childContext = childContext;
		this.stores = stores;
		this.useJms = useJms;
		this.useDb = useDb;
		
		this.messagePortalStarted = false;
		this.registered = new HashMap<Object, ServiceHolder>();
		this.isServicesStarted = new AtomicBoolean();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.context.IPdppContextContainer#isServicesStarted()
	 */
	@Override
	public boolean isServicesStarted() {
		return isServicesStarted.get();
	}

	/**
	 * This returns the child context.
	 * 
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextContainer#getApplicationContext()
	 */
	@Override
	public ApplicationContext getApplicationContext() {
		return getChildContext();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.context.IPdppContextContainer#getParentContext()
	 */
	@Override
	public ApplicationContext getParentContext() {
		return parent.getApplicationContext();
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.context.IPdppContextContainer#getChildContext()
	 */
	@Override
	public ApplicationContext getChildContext() {
		return childContext;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.context.IPdppContextContainer#getParentSessionConfig()
	 */
	@Override
	public IContextConfiguration getParentSessionConfig() {
		return this.parent;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.context.IPdppContextContainer#getChildSessionConfig()
	 */
	@Override
	public IContextConfiguration getChildSessionConfig() {
		return child;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextContainer#startChildDbStores()
	 */
	@Override
	public void startChildDbStores() {
		final IDbSqlArchiveController controller = childContext.getBean(IDbSqlArchiveController.class);

		if (useDb && controller != null && !controller.isUp()) {
			controller.init(stores);
			controller.startAllStores();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextContainer#stopChildDbStores()
	 */
	@Override
	public void stopChildDbStores() {
		final IDbSqlArchiveController controller = childContext.getBean(IDbSqlArchiveController.class);

		if (controller != null) {
			controller.shutDown();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextContainer#startChildMessagePortal()
	 */
	@Override
	public synchronized void startChildMessagePortal() {
		if (useJms && !messagePortalStarted) {
			final IMessagePortal m = childContext.getBean(IMessagePortal.class);
			m.startService();
			messagePortalStarted = true;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.common.spring.context.IContextContainer#stopChildMessagePortal()
	 */
	@Override
	public synchronized void stopChildMessagePortal() {
		if (useJms && messagePortalStarted) {
			final IMessagePortal m = childContext.getBean(IMessagePortal.class);
			m.stopService();
			messagePortalStarted = false;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.context.IPdppContextContainer#registerShutdownMethod(Object, Method, Method)
	 */
	@Override
	public void registerShutdownMethod(final Object instance, final Method startupMethod, final Method shutdownMethod) {
		this.registered.put(instance, new ServiceHolder(instance, startupMethod, shutdownMethod));
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.context.IPdppContextContainer#unregisterService(Class)
	 */
	@Override
	public void unregisterService(final Class<?> clazz) {
		this.registered.remove(clazz);
		
	}

	@Override
	public void startRequiredServices() {
		for (final ServiceHolder h : this.registered.values()) {
			try {
				h.doStartup();
			} catch (final IllegalAccessException e) {
				e.printStackTrace();
			} catch (final IllegalArgumentException e) {
				e.printStackTrace();
			} catch (final InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		
		isServicesStarted.set(true);
	}

	@Override
	public void shutdownRequiredServices() {
		for (final ServiceHolder h : this.registered.values()) {
			try {
				h.doShutdown();
			} catch (final IllegalAccessException e) {
				e.printStackTrace();
			} catch (final IllegalArgumentException e) {
				e.printStackTrace();
			} catch (final InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		isServicesStarted.set(false);;
	}

	/**
	 * Class used to hold start and shutdown methods for a service.
	 * 
	 */
	private class ServiceHolder {
		final Object instance;
		final Method startupMethod;
		final Method shutdownMethod;
		
		public ServiceHolder(final Object instance, final Method startupMethod,  final Method shutdownMethod) {
			super();
			this.instance = instance;
			this.shutdownMethod = shutdownMethod;
			this.startupMethod = startupMethod;
		}
		
		public void doStartup() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			if (startupMethod != null) {
				startupMethod.invoke(instance);
			}
		}

		public void doShutdown() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
			if (shutdownMethod != null) {
				shutdownMethod.invoke(instance);
			}
		}
	}
}
