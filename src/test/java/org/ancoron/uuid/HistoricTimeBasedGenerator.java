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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.UUIDTimer;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

/**
 *
 * @author ancoron
 */
public class HistoricTimeBasedGenerator extends TimeBasedGenerator
{
    private final long interval;

    private final AtomicLong current;

    public HistoricTimeBasedGenerator(EthernetAddress ethAddr, UUIDTimer timer, long startEpochMillis, long intervalNanos)
    {
        super(ethAddr, timer);

        this.current = new AtomicLong(startEpochMillis * 10000L + 0x01b21dd213814000L);

        // 1 ns -> 100 ns precision
        this.interval = intervalNanos / 100L;
    }

    @Override
    public UUID generate()
    {
        final long rawTimestamp = current.addAndGet(interval);
        // Time field components are kind of shuffled, need to slice:
        int clockHi = (int) (rawTimestamp >>> 32);
        int clockLo = (int) rawTimestamp;
        // and dice
        int midhi = (clockHi << 16) | (clockHi >>> 16);
        // need to squeeze in type (4 MSBs in byte 6, clock hi)
        midhi &= ~0xF000; // remove high nibble of 6th byte
        midhi |= 0x1000; // type 1
        long midhiL = (long) midhi;
        midhiL = ((midhiL << 32) >>> 32); // to get rid of sign extension
        // and reconstruct
        long l1 = (((long) clockLo) << 32) | midhiL;
        // last detail: must force 2 MSB to be '10'
        return new UUID(l1, _uuidL2);
    }
}
