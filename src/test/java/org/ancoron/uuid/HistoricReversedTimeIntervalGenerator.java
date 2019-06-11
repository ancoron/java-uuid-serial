/*
 * Copyright 2019 ancoron.
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
package org.ancoron.uuid;

import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.UUIDTimer;

/**
 *
 * @author ancoron
 */
public class HistoricReversedTimeIntervalGenerator extends ReversedTimeBasedGenerator
{
    private final long interval;

    private final AtomicLong current;

    public HistoricReversedTimeIntervalGenerator(EthernetAddress ethAddr, UUIDTimer timer, long startEpochMillis, long intervalNanos)
    {
        super(ethAddr, timer);

        this.current = new AtomicLong(startEpochMillis * 10000L + 0x01b21dd213814000L);

        // 1 ns -> 100 ns precision
        this.interval = intervalNanos / 100L;
    }

    @Override
    protected long getTimestamp()
    {
        return current.addAndGet(interval);
    }
}
