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
public class ReversedTimeBasedGenerator extends TimeBasedGenerator
{
    public ReversedTimeBasedGenerator(EthernetAddress ethAddr, UUIDTimer timer)
    {
        super(ethAddr, timer);
    }

    protected long getTimestamp()
    {
        return _timer.getTimestamp();
    }

    @Override
    public UUID generate()
    {
        // this always will end with last byte "0X"...
        long timestamp = Long.reverseBytes(getTimestamp());

        // use the hole at ...F0 to insert the UUID version...
        timestamp = (timestamp >>> 16 << 16) | ((timestamp & 0xFFFF | 0x10000) >>> 4) | (timestamp & 0xF);

        return new UUID(timestamp, _uuidL2);
    }
}
