/*-
 * #%L
 * HAPI FHIR Storage api
 * %%
 * Copyright (C) 2014 - 2025 Smile CDR, Inc.
 * %%
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
 * #L%
 */
package ca.uhn.fhir.jpa.subscription.channel.config;

import ca.uhn.fhir.broker.api.IBrokerClient;
import ca.uhn.fhir.broker.api.IChannelNamer;
import ca.uhn.fhir.broker.impl.LinkedBlockingBrokerClient;
import ca.uhn.fhir.jpa.subscription.channel.impl.LinkedBlockingChannelFactory;
import ca.uhn.fhir.jpa.subscription.channel.impl.RetryPolicyProvider;
import ca.uhn.fhir.jpa.subscription.channel.subscription.SubscriptionChannelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SubscriptionChannelConfig {

	/**
	 * We are autowiring this because we need to override retry policy
	 * in some tests
	 */
	@Autowired
	private RetryPolicyProvider myRetryPolicyProvider;

	/**
	 * Create a @Primary @Bean if you need a different implementation
	 */
	@Bean
	public LinkedBlockingChannelFactory queueChannelFactory(IChannelNamer theChannelNamer) {
		return new LinkedBlockingChannelFactory(theChannelNamer, myRetryPolicyProvider);
	}

	@Bean
	public IBrokerClient brokerClient(IChannelNamer theChannelNamer) {
		return new LinkedBlockingBrokerClient(theChannelNamer);
	}

	@Bean
	public RetryPolicyProvider retryPolicyProvider() {
		return new RetryPolicyProvider();
	}

	@Bean
	public SubscriptionChannelFactory subscriptionChannelFactory(IBrokerClient theBrokerClient) {
		return new SubscriptionChannelFactory(theBrokerClient);
	}

	/**
	 * Create a @Primary @Bean if you need a different implementation
	 */
	@Bean
	// Default implementation returns the name unchanged
	public IChannelNamer channelNamer() {
		return (theNameComponent, theChannelSettings) -> theNameComponent;
	}
}
