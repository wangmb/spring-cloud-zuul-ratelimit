/*
 * Copyright 2012-2017 the original author or authors.
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

package com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.repository;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.Rate;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.RateLimiter;
import com.marcosbarbero.cloud.autoconfigure.zuul.ratelimit.config.properties.RateLimitProperties.Policy;
import java.util.Date;

/**
 * Abstract implementation for {@link RateLimiter}.
 *
 * @author Liel Chayoun
 * @author Marcos Barbero
 * @since 2017-08-28
 */
public abstract class AbstractRateLimiter implements RateLimiter {

    protected abstract Rate getRate(String key);
    protected abstract void saveRate(Rate rate);

    @Override
    public synchronized Rate consume(final Policy policy, final String key, final Long requestTime) {
        Rate rate = this.create(policy, key);
        this.updateRate(policy, rate, requestTime);
        this.saveRate(rate);
        return rate;
    }

    private Rate create(final Policy policy, final String key) {
        Rate rate = this.getRate(key);
        if (isExpired(rate)) {

            final Long limit = policy.getLimit();
            final Long quota = policy.getQuota() != null ? SECONDS.toMillis(policy.getQuota()) : null;
            final Long refreshInterval = SECONDS.toMillis(policy.getRefreshInterval());
            final Date expiration = new Date(System.currentTimeMillis() + refreshInterval);

            rate = new Rate(key, limit, quota, refreshInterval, expiration);
        }
        return rate;
    }

    private void updateRate(final Policy policy, final Rate rate, final Long requestTime) {
        if (rate.getReset() > 0) {
            Long reset = rate.getExpiration().getTime() - System.currentTimeMillis();
            rate.setReset(reset);
        }
        if (policy.getLimit() != null && requestTime == null) {
            rate.setRemaining(Math.max(-1, rate.getRemaining() - 1));
        }
        if (policy.getQuota() != null && requestTime != null) {
            rate.setRemainingQuota(Math.max(-1, rate.getRemainingQuota() - requestTime));
        }
    }

    private boolean isExpired(final Rate rate) {
        return rate == null || (rate.getExpiration().getTime() < System.currentTimeMillis());
    }
}
