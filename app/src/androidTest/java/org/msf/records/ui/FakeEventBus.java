package org.msf.records.ui;

import static junit.framework.Assert.fail;

import org.msf.records.events.CrudEventBus;
import org.msf.records.utils.EventBusInterface;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import de.greenrobot.event.EventBus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

/**
 * Fake event bus implementation. The real {@link EventBus} is not suitable for unit tests
 * because it calls methods on a separate thread to the test thread. This is because the
 * 'main thread' is not the same as the test thread.
 */
public final class FakeEventBus implements EventBusInterface, CrudEventBus {

	private static final String METHOD_NAME_EVENT_RECEIVER_MAIN_THREAD = "onEventMainThread";
	private static final Set<String> IGNORED_METHOD_NAMES = ImmutableSet.of(
			"equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait");

	private final Set<Object> mRegisteredReceivers = new HashSet<Object>();
	private final List<Object> mEventLog = Lists.newArrayList();

	@Override
	public void register(Object receiver) {
		for (Method method : receiver.getClass().getMethods()) {
			// We only support a subset of the event bus functionality, so we check methods on the
			// receiver match a whitelist of supported methods. This should ensure the tests fail
			// in an explicit noisy way if the code under test is using event bus functionality that
			// we haven't implemented in this class.
			if (!IGNORED_METHOD_NAMES.contains((method.getName()))) {
				Preconditions.checkArgument(
						method.getName().equals(METHOD_NAME_EVENT_RECEIVER_MAIN_THREAD),
						"Method was called " + method.getName() + ". Fake event bus only supports methods called "
								+ METHOD_NAME_EVENT_RECEIVER_MAIN_THREAD);
				Preconditions.checkArgument(
						method.getParameterTypes().length == 1,
						"The fake event bus only supports methods with a single parameter");
			}
		}
		mRegisteredReceivers.add(receiver);
	}

	@Override
	public void unregister(Object receiver) {
		mRegisteredReceivers.remove(receiver);
	}

	public int countRegisteredReceivers() {
		return mRegisteredReceivers.size();
	}

	public void assertEventLogContains(Object event) {
	    if (!mEventLog.contains(event)) {
	        fail("Expected event not present. Actual events: " + mEventLog);
	    }
	}

	public List<Object> getEventLog() {
	    return ImmutableList.copyOf(mEventLog);
	}

	@Override
	public void post(Object event) {
	    mEventLog.add(event);
		for (Object receiver : mRegisteredReceivers) {
			for (Method method : receiver.getClass().getMethods()) {
				if (method.getName().equals(METHOD_NAME_EVENT_RECEIVER_MAIN_THREAD)) {
					Class<?> parameter = method.getParameterTypes()[0];
					if (parameter.isInstance(event)) {
						try {
							method.invoke(receiver, event);
						} catch (InvocationTargetException e) {
							throw new RuntimeException(e.getCause());
						} catch (IllegalAccessException | IllegalArgumentException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
	}
}