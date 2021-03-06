/*
 * Copyright (C) 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.nacos.discovery;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.NacosServiceInstance;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * @author xiaojing
 * @author renhaojun
 */
public class NacosDiscoveryClient implements DiscoveryClient {

	private static final Logger log = LoggerFactory.getLogger(NacosDiscoveryClient.class);
	public static final String DESCRIPTION = "Spring Cloud Nacos Discovery Client";

	private NacosDiscoveryProperties discoveryProperties;

	public NacosDiscoveryClient(NacosDiscoveryProperties discoveryProperties) {
		this.discoveryProperties = discoveryProperties;
	}

	@Override
	public String description() {
		return DESCRIPTION;
	}

	@Override
	public ServiceInstance getLocalServiceInstance() {
		return new ServiceInstance() {
			@Override
			public String getServiceId() {
				return NacosDiscoveryClient.this.discoveryProperties.getService();
			}

			@Override
			public String getHost() {
				return NacosDiscoveryClient.this.discoveryProperties.getIp();
			}

			@Override
			public int getPort() {
				return NacosDiscoveryClient.this.discoveryProperties.getPort();
			}

			@Override
			public boolean isSecure() {
				return NacosDiscoveryClient.this.discoveryProperties.isSecure();
			}

			@Override
			public URI getUri() {
				return DefaultServiceInstance.getUri(this);
			}

			@Override
			public Map<String, String> getMetadata() {
				return NacosDiscoveryClient.this.discoveryProperties.getMetadata();
			}
		};
	}

	@Override
	public List<ServiceInstance> getInstances(String serviceId) {
		try {
			String group = discoveryProperties.getGroup();
			List<Instance> instances = discoveryProperties.namingServiceInstance()
					.selectInstances(serviceId, group, true);
			return hostToServiceInstanceList(instances, serviceId);
		}
		catch (Exception e) {
			throw new RuntimeException(
					"Can not get hosts from nacos server. serviceId: " + serviceId, e);
		}
	}

	public static ServiceInstance hostToServiceInstance(Instance instance,
			String serviceId) {
		if (instance == null || !instance.isEnabled() || !instance.isHealthy()) {
			return null;
		}
		NacosServiceInstance nacosServiceInstance = new NacosServiceInstance();
		nacosServiceInstance.setHost(instance.getIp());
		nacosServiceInstance.setPort(instance.getPort());
		nacosServiceInstance.setServiceId(serviceId);

		Map<String, String> metadata = new HashMap<>();
		metadata.put("nacos.instanceId", instance.getInstanceId());
		metadata.put("nacos.weight", instance.getWeight() + "");
		metadata.put("nacos.healthy", instance.isHealthy() + "");
		metadata.put("nacos.cluster", instance.getClusterName() + "");
		metadata.putAll(instance.getMetadata());
		nacosServiceInstance.setMetadata(metadata);

		if (metadata.containsKey("secure")) {
			boolean secure = Boolean.parseBoolean(metadata.get("secure"));
			nacosServiceInstance.setSecure(secure);
		}
		return nacosServiceInstance;
	}

	public static List<ServiceInstance> hostToServiceInstanceList(
			List<Instance> instances, String serviceId) {
		List<ServiceInstance> result = new ArrayList<>(instances.size());
		for (Instance instance : instances) {
			ServiceInstance serviceInstance = hostToServiceInstance(instance, serviceId);
			if (serviceInstance != null) {
				result.add(serviceInstance);
			}
		}
		return result;
	}

	@Override
	public List<String> getServices() {

		try {
			String group = discoveryProperties.getGroup();
			ListView<String> services = discoveryProperties.namingServiceInstance()
					.getServicesOfServer(1, Integer.MAX_VALUE, group);
			return services.getData();
		}
		catch (Exception e) {
			log.error("get service name from nacos server fail,", e);
			return Collections.emptyList();
		}
	}
}
