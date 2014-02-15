package com.github.jremoting.invoke;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import com.github.jremoting.core.Invoke;
import com.github.jremoting.core.RpcClient;
import com.github.jremoting.core.Serializer;

public class ClientInvocationHandler implements InvocationHandler {

	private final RpcClient rpcClient;
	private final Serializer serializer;
	private final String serviceName;
	private final String serviceVersion;
	private final String remoteAddress;
	private final long timeout;
	private final Executor defaultCallbackExecutor;
	
	
	public ClientInvocationHandler(RpcClient rpcClient,
			Serializer serializer,
			String serviceName, 
			String serviceVersion,
			String remoteAddress,
			long timeout, Executor callbackExecutor) {
		
		this.rpcClient = rpcClient;
		this.serializer = serializer;
		this.serviceVersion =serviceVersion;
		this.serviceName = serviceName;
		this.remoteAddress = remoteAddress;
		this.timeout = timeout;
		this.defaultCallbackExecutor= callbackExecutor;
	}
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		
		Invoke invoke = null;
		//isAsync
		if(method.getName().startsWith("$")) {
			invoke = createAsynInvoke(method, args);
		}
		//sync invoke
		else {
			invoke = new Invoke(serviceName, 
					serviceVersion,
					method.getName(),
					serializer,
					args ,
					method.getParameterTypes());
		}
		
		if(remoteAddress != null) {
			invoke.setRemoteAddress(remoteAddress);
		}
		invoke.setTimeout(this.timeout);
		return rpcClient.invoke(invoke);
	}
	private Invoke createAsynInvoke(Method method, Object[] args) {
		Invoke invoke = null;
		Runnable callback = null;
		Executor callbackExecutor = null;
		String methodName = method.getName().replace("$", "");
		
		if(args.length > 0 && args[args.length - 1] instanceof Runnable) {
			callback = (Runnable) args[args.length - 1];
		}
		if(callback != null && args.length > 1 && args[args.length - 2] instanceof Executor) {
			callbackExecutor = (Executor)args[args.length - 2];
		}
		
		if(callback == null && callbackExecutor == null) {
			   invoke = new Invoke(serviceName, 
						serviceVersion,
						methodName,
						serializer,
						args ,
						method.getParameterTypes());
		}
		else if (callback != null && callbackExecutor != null) {
			Object[] newArgs = new Object[args.length -2];
			Class<?>[] newParameterTypes = new Class<?>[args.length - 2];
			for (int i = 0; i < newArgs.length; i++) {
				newArgs[i] = args[i];
				newParameterTypes[i] = method.getParameterTypes()[i];
			}
			invoke = new Invoke(serviceName, 
					serviceVersion,
					methodName,
					serializer,
					newArgs ,
					newParameterTypes);
		}
		else if(callback != null) {
			Object[] newArgs = new Object[args.length -1];
			Class<?>[] newParameterTypes = new Class<?>[args.length - 1];
			for (int i = 0; i < newArgs.length; i++) {
				newArgs[i] = args[i];
				newParameterTypes[i] = method.getParameterTypes()[i];
			}
			invoke = new Invoke(serviceName, 
					serviceVersion,
					methodName,
					serializer,
					newArgs ,
					newParameterTypes);
		}
		invoke.setAsync(true);
		invoke.setCallback(callback);
		if(callbackExecutor == null) {
			callbackExecutor = defaultCallbackExecutor;
		}
		invoke.setCallbackExecutor(callbackExecutor);
		return invoke;
	}
	public long getTimeout() {
		return timeout;
	}
}
