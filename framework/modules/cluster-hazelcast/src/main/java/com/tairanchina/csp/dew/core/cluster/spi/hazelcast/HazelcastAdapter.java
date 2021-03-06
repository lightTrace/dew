/*
 * Copyright 2019. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tairanchina.csp.dew.core.cluster.spi.hazelcast;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class HazelcastAdapter {

    private HazelcastConfig hazelcastConfig;

    private HazelcastInstance hazelcastInstance;
    private boolean active;

    public HazelcastAdapter(HazelcastConfig hazelcastConfig) {
        this.hazelcastConfig = hazelcastConfig;
    }

    @PostConstruct
    public void init() {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.setProperty("hazelcast.logging.type", "slf4j");
        if (hazelcastConfig.getUsername() != null) {
            clientConfig.getGroupConfig().setName(hazelcastConfig.getUsername()).setPassword(hazelcastConfig.getPassword());
        }
        clientConfig.getNetworkConfig().setConnectionTimeout(hazelcastConfig.getConnectionTimeout());
        clientConfig.getNetworkConfig().setConnectionAttemptLimit(hazelcastConfig.getConnectionAttemptLimit());
        clientConfig.getNetworkConfig().setConnectionAttemptPeriod(hazelcastConfig.getConnectionAttemptPeriod());
        hazelcastConfig.getAddresses().forEach(i -> clientConfig.getNetworkConfig().addAddress(i));
        hazelcastInstance = HazelcastClient.newHazelcastClient(clientConfig);
        active = true;
    }

    public HazelcastInstance getHazelcastInstance() {
        return hazelcastInstance;
    }

    boolean isActive() {
        return active;
    }

    @PreDestroy
    public void shutdown() {
        active = false;
        hazelcastInstance.shutdown();
    }

}
