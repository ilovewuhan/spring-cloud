package com.alibaba.cloud.sentinel.zuul.handler;

import java.util.Map;

import com.alibaba.csp.sentinel.adapter.gateway.zuul.fallback.DefaultBlockFallbackProvider;
import com.alibaba.csp.sentinel.adapter.gateway.zuul.fallback.ZuulBlockFallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.zuul.fallback.ZuulBlockFallbackProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.util.CollectionUtils;

/**
 * @author tiger
 */
public class FallBackProviderHandler implements SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory
			.getLogger(FallBackProviderHandler.class);

	private final DefaultListableBeanFactory beanFactory;

	public FallBackProviderHandler(DefaultListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Map<String, ZuulBlockFallbackProvider> providerMap = beanFactory
				.getBeansOfType(ZuulBlockFallbackProvider.class);
		if (!CollectionUtils.isEmpty(providerMap)) {
			for (String k : providerMap.keySet()) {
				logger.info("[Sentinel Zuul] Register provider name:{}, instance: {}", k,
						providerMap.get(k));
				ZuulBlockFallbackManager.registerProvider(providerMap.get(k));
			}
		}
		else {
			logger.info("[Sentinel Zuul] Register default fallback provider. ");
			ZuulBlockFallbackManager.registerProvider(new DefaultBlockFallbackProvider());
		}
	}
}