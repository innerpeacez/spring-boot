/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.boot.actuate.couchbase;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.couchbase.client.java.bucket.BucketInfo;
import com.couchbase.client.java.cluster.ClusterInfo;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.RxReactiveStreams;
import rx.Single;

import org.springframework.boot.actuate.health.AbstractReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.data.couchbase.core.RxJavaCouchbaseOperations;
import org.springframework.util.StringUtils;

/**
 * A {@link ReactiveHealthIndicator} for Couchbase.
 *
 * @author Mikalai Lushchytski
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class CouchbaseReactiveHealthIndicator extends AbstractReactiveHealthIndicator {

	private final RxJavaCouchbaseOperations couchbaseOperations;

	private final Duration timeout;

	/**
	 * Create a new {@link CouchbaseReactiveHealthIndicator} instance.
	 * @param couchbaseOperations the reactive couchbase operations
	 * @param timeout the request timeout
	 */
	public CouchbaseReactiveHealthIndicator(RxJavaCouchbaseOperations couchbaseOperations,
			Duration timeout) {
		this.couchbaseOperations = couchbaseOperations;
		this.timeout = timeout;
	}

	@Override
	protected Mono<Health> doHealthCheck(Health.Builder builder) {
		ClusterInfo cluster = this.couchbaseOperations.getCouchbaseClusterInfo();
		String versions = StringUtils
				.collectionToCommaDelimitedString(cluster.getAllVersions());
		Observable<BucketInfo> bucket = this.couchbaseOperations.getCouchbaseBucket()
				.bucketManager().async().info()
				.timeout(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
		Single<Health> health = bucket.map(BucketInfo::nodeList)
				.map(StringUtils::collectionToCommaDelimitedString)
				.map((nodes) -> builder.up().withDetail("versions", versions)
						.withDetail("nodes", nodes).build())
				.toSingle();
		return Mono.from(RxReactiveStreams.toPublisher(health));
	}

}
