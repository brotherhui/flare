package org.sanpao.flare.api.gateway;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.ignite.Ignite;
import org.sanpao.flare.common.ApiFunction;
import org.sanpao.flare.common.ApiResult;
import org.sanpao.flare.common.ignite.IgniteFunctionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class FunctionProxy implements InitializingBean {

	final static Properties PROPERTIES = new Properties();

	final static Map<String, Class<ApiFunction<?>>> CACHE = new ConcurrentHashMap<String, Class<ApiFunction<?>>>();

	@Value("classpath:service-function.properties")
	private Resource resource;

	@Autowired
	private Ignite ignite;

	public ApiResult invoke(ServerHttpRequest request, String functionName, Map<String, Object> payload) {
		List<NameValuePair> nameValuePairs = URLEncodedUtils.parse(request.getURI(), Charset.forName("UTF-8"));
		Map<String, Object> map = new HashMap<String, Object>();
		nameValuePairs.stream().forEach(pair -> {
			map.put(pair.getName(), pair.getValue());
		});
		if (null != payload) {
			map.putAll(payload);
		}
		Class<ApiFunction<?>> functionInterface = load(functionName);
		ApiFunction<?> function = IgniteFunctionFactory.newFunction(ignite, functionInterface);
		return function.apply(map);
	}

	public ApiResult invoke(ServerHttpRequest request, String functionName) {
		return invoke(request, functionName);
	}

	@SuppressWarnings("unchecked")
	public Class<ApiFunction<?>> load(String functionName) {
		Class<ApiFunction<?>> serviceClass = null;
		if (CACHE.containsKey(functionName)) {
			serviceClass = CACHE.get(functionName);
		} else {
			String serviceClassName = PROPERTIES.getProperty(functionName);
			try {
				serviceClass = (Class<ApiFunction<?>>) Class.forName(serviceClassName);
				CACHE.putIfAbsent(functionName, serviceClass);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return serviceClass;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
		try {
			PROPERTIES.load(reader);
		} finally {
			reader.close();
		}
	}

}
