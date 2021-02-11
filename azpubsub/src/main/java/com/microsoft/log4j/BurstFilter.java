/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.log4j;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Level;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class BurstFilter extends Filter {
    private static final long NANOS_IN_SECONDS = 1000000000;
    private static final int DEFAULT_RATE = 10;
    private static final int DEFAULT_RATE_MULTIPLE = 100;
    private static final int HASH_SHIFT = 32;

    private Level level = Level.WARN;
    private int rate = DEFAULT_RATE;
    private int maxBurst = DEFAULT_RATE * DEFAULT_RATE_MULTIPLE;

    private long burstInterval;
    private final DelayQueue<LogDelay> history = new DelayQueue<>();
    private final Queue<LogDelay> available = new ConcurrentLinkedQueue<>();

    /**
     * Sets the logging level to use.
     * @param level the logging level to use. 
     */
    public void setLevel(final Level level) {
        this.level = level;
    }

    /**
     * Sets the average number of events per second to allow.
     * @param rate the average number of events per second to allow. This must be a positive number. 
     */
    public void setRate(final int rate) {
        this.rate = rate;
        if (this.rate <= 0) {
            this.rate = DEFAULT_RATE;
        }
    }

    /**
     * Sets the maximum number of events that can occur before events are filtered for exceeding the average rate.
     * @param maxBurst Sets the maximum number of events that can occur before events are filtered for exceeding the average rate.
     * The default is 10 times the rate.
     */
    public void setMaxBurst(final int maxBurst) {
        this.maxBurst = maxBurst;
        if (this.maxBurst <= 0) {
            this.maxBurst = this.rate * DEFAULT_RATE_MULTIPLE;
        }
    }

    @Override
    public void activateOptions() {
        this.burstInterval = NANOS_IN_SECONDS * (this.maxBurst / this.rate);
        for (int i = 0; i < this.maxBurst; ++i) {
            available.add(createLogDelay(0));
        }
    }

    @Override
    public int decide(LoggingEvent event) {
        if (this.level == null) {
            return Filter.NEUTRAL;
        }

        if (event.getLevel().toInt() <= this.level.toInt()) {
            LogDelay delay = history.poll();
            while (delay != null) {
                available.add(delay);
                delay = history.poll();
            }

            delay = available.poll();
            if (delay != null) {
                delay.setDelay(burstInterval);
                history.add(delay);
                return Filter.ACCEPT;
            }

            return Filter.DENY;
        }

        return Filter.ACCEPT;
    }

    @Override
    public String toString() {
        return "level=" + level.toString() + ", interval=" + burstInterval + ", max=" + history.size();
    }

    static LogDelay createLogDelay(final long expireTime) {
        return new LogDelay(expireTime);
    }

    /**
     * Delay object to represent each log event that has occurred within the timespan.
     *
     * Consider this class private, package visibility for testing.
     */
    private static class LogDelay implements Delayed {
        LogDelay(final long expireTime) {
            this.expireTime = expireTime;
        }

        private long expireTime;

        public void setDelay(final long delay) {
            this.expireTime = delay + System.nanoTime();
        }

        @Override
        public long getDelay(final TimeUnit timeUnit) {
            return timeUnit.convert(expireTime - System.nanoTime(), TimeUnit.NANOSECONDS);
        }

        @Override
        public int compareTo(final Delayed delayed) {
            final long diff = this.expireTime - ((LogDelay) delayed).expireTime;
            return Long.signum(diff);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final LogDelay logDelay = (LogDelay) o;

            if (expireTime != logDelay.expireTime) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return (int) (expireTime ^ (expireTime >>> HASH_SHIFT));
        }
    }
}
