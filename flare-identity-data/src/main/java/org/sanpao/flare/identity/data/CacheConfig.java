package org.sanpao.flare.identity.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.cache.configuration.FactoryBuilder;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.springdata.repository.config.EnableIgniteRepositories;
import org.sanpao.flare.common.ignite.IgniteNodeTypes;
import org.sanpao.flare.identity.data.store.UserInfoStore;
import org.sanpao.flare.identity.domain.entity.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration("identityCacheConfig")
@EnableIgniteRepositories({ "org.sanpao.flare.identity.data.cao" })
public class CacheConfig {

	//@Value("${flare.ignite.name}")
	private String name;

	@Value("${flare.ignite.addresses}")
	private String[] addresses;

	@Bean
	public CacheConfiguration<Long, UserInfo> userInfoCacheConfig() {
		CacheConfiguration<Long, UserInfo> cacheConfig = new CacheConfiguration<Long, UserInfo>(CacheNames.USER_INFO_CACHE_NAME);
		// Setting SQL schema for the cache.
		cacheConfig.setIndexedTypes(String.class, UserInfo.class);
		cacheConfig.setNodeFilter(node -> {
			Boolean value = node.attribute(IgniteNodeTypes.DATA_NODE.name());
			if (null != value) {
				return value.booleanValue();
			}
			return false;
		});
		cacheConfig.setCacheStoreFactory(FactoryBuilder.factoryOf(UserInfoStore.class));
		return cacheConfig;
	}

	@Bean
	public List<CacheConfiguration<?, ?>> identityCacheConfigurations() {
		List<CacheConfiguration<?, ?>> cacheConfigurations = new ArrayList<CacheConfiguration<?, ?>>();
		cacheConfigurations.add(userInfoCacheConfig());
		return cacheConfigurations;
	}

	//@Bean
	public Ignite igniteInstance() {
		TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
		TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder();
		ipFinder.setAddresses(Arrays.asList(addresses));
		discoverySpi.setIpFinder(ipFinder);
		// Ignite persistence configuration.
		DataStorageConfiguration storageConfiguration = new DataStorageConfiguration();
		// Enabling the persistence.
		storageConfiguration.getDefaultDataRegionConfiguration().setPersistenceEnabled(true);
		IgniteConfiguration configuration = new IgniteConfiguration();
		configuration.setDiscoverySpi(discoverySpi);
		configuration.setUserAttributes(Collections.singletonMap(IgniteNodeTypes.DATA_NODE.name(), true));
		// Setting some custom name for the node.
		configuration.setIgniteInstanceName(name);
		// Enabling peer-class loading feature.
		// configuration.setPeerClassLoadingEnabled(true);
		configuration.setDataStorageConfiguration(storageConfiguration);
		configuration.setCacheConfiguration(new CacheConfiguration[] { userInfoCacheConfig() });
		Ignite ignite = Ignition.getOrStart(configuration);
		return ignite;
	}

}
