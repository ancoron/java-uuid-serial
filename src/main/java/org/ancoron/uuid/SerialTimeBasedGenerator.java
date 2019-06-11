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

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.UUIDTimer;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

/**
 *
 * @author ancoron
 */
public class SerialTimeBasedGenerator extends TimeBasedGenerator
{
    private final boolean shift;
    private final int shift_bits_r;
    private final int shift_bits_l;

    public SerialTimeBasedGenerator(EthernetAddress ethAddr, UUIDTimer timer)
    {
        this(ethAddr, timer, 0);
    }

    public SerialTimeBasedGenerator(EthernetAddress ethAddr, UUIDTimer timer, int shift)
    {
        super(ethAddr, timer);
        if (shift < 0 || shift > 7) {
            throw new IllegalArgumentException("Invalid value for parameter shift: '" + shift + "' (must be within 0 and 7)");
        }

        if (shift == 0) {
            this.shift = false;
            this.shift_bits_r = 0;
            this.shift_bits_l = 0;
        } else {
            this.shift = true;
            this.shift_bits_r = shift * 8;
            this.shift_bits_l = 64 - this.shift_bits_r - 4;
        }
    }

    protected long getTimestamp()
    {
        return _timer.getTimestamp();
    }

    @Override
    public UUID generate()
    {
        long timestamp = getTimestamp();

        // shift for desired prefix rollover...
        if (shift) {
            // ...retain the 4 bits at the end...
            timestamp = (timestamp >>> shift_bits_r) | (timestamp << shift_bits_l);
        }

        // insert the UUID version...
        timestamp = (timestamp >>> 12 << 16) | (timestamp & 0xFFF | 0x1000);

        return new UUID(timestamp, _uuidL2);
    }
}
