package vsue.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class VSRemoteObjectManager {
	
	private static final VSRemoteObjectManager instance = createInstance();
	private final AtomicInteger idGenerator = new AtomicInteger(0);
	private final ConcurrentHashMap<Integer, Object> managedObjects = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Object, Integer> exportedObjects = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, Remote> managedObjectStubs = new ConcurrentHashMap<>();
	private final VSConnectionManager connections = new VSConnectionManager();
	
	private static VSRemoteObjectManager createInstance() {
		return new VSRemoteObjectManager();
	}

	public VSConnectionManager getConnections(){
		return connections;
	}
	
	private VSRemoteObjectManager(){}
	
	private VSRemoteReference generateNewReference() throws VSRemoteException {
		final int objectId = idGenerator.getAndIncrement();
		final int port = connections.enableRemoteConnection();
		String localhostId = "localhost";
		
		try {
			localhostId = Inet4Address.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {}
		
		
		return new VSRemoteReference(localhostId,port, objectId);
	}

	private Class<?>[] resolveInterfaces(final Class<?> clazz) {
		if (clazz == null) {
			return new Class<?>[0];
		}
		final Class<?> baseClass = clazz.getSuperclass();
		List<Class<?>> remoteInterfaces = new ArrayList<Class<?>>();
		for(Class<?> iface : clazz.getInterfaces()){
			if(Remote.class.isAssignableFrom(iface)){
				remoteInterfaces.add(iface);
			}
		}
		
		final Class<?>[] parentInterfaces = resolveInterfaces(baseClass);
		final Class<?>[] interfaces = remoteInterfaces.toArray(new Class<?>[remoteInterfaces.size()]);
		final Class<?>[] result = new Class<?>[interfaces.length + parentInterfaces.length];
		System.arraycopy(interfaces, 0, result, 0, interfaces.length);
		System.arraycopy(parentInterfaces, 0, result, interfaces.length, parentInterfaces.length);

		return result;
	}

	public static VSRemoteObjectManager getInstance() {
		return instance;
	}

	public void unexportObject(Remote object) {
		final Integer id = exportedObjects.get(object);
		if (id == null) {
			throw new RuntimeException("unknown object");
		}
		managedObjectStubs.remove(id);
		exportedObjects.remove(object);
		managedObjects.remove(id);
	}

	public Object getStubIfExported(final Object o){
		if(o != null && !Proxy.isProxyClass(o.getClass()) &&exportedObjects.containsKey(o)){
			return managedObjects.get(exportedObjects.get(o));
		}
		return o;
	}
	
	public Remote exportObject(Remote object) throws VSRemoteException {
		if(object != null && exportedObjects.containsKey(object)){
			return managedObjectStubs.get(exportedObjects.get(object));
		}
		final Class<?> clazz = object.getClass();
		final ClassLoader originalClassLoader = clazz.getClassLoader();
		final Class<?>[] interfaces = resolveInterfaces(clazz);
		final VSRemoteReference reference = generateNewReference();
		final InvocationHandler handler = new VSInvocationHandler(reference);
		final Object obj = Proxy.newProxyInstance(originalClassLoader, interfaces, handler);
		if (obj instanceof Remote) {
			managedObjects.put(reference.getObjectId(), object);
			exportedObjects.put(object, reference.getObjectId());
			managedObjectStubs.put(reference.getObjectId(), (Remote)obj);
			return (Remote) obj;
		}
		throw new VSRemoteException("casting class " + obj.getClass() + " to Remote failed");

	}

	public Object invokeMethod(int objectId, String genericMethodName, Object[] args) throws Throwable {
		final Object obj = managedObjects.get(objectId);
		final Class<?> clazz = managedObjects.get(objectId).getClass();
		final Class<?>[] interfaces = resolveInterfaces(clazz);
		for (Class<?> iface : interfaces) {
			for (Method m : iface.getMethods()) {
				if (m.toGenericString().equals(genericMethodName)) {
					try {
						final Object result = m.invoke(obj, args);
						return getStubIfExported(result);
					} catch (IllegalAccessException e) {
						throw new VSRemoteException("can not access private method"+ e);
					} catch (IllegalArgumentException e) {
						throw new VSRemoteException("illegal arguments"+ e);
					} catch (InvocationTargetException e) {
						throw e;
					}
				}
			}
		}
		return null;
	}
}
