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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.TimestampSynchronizer;
import com.fasterxml.uuid.UUIDTimer;
import com.fasterxml.uuid.ext.FileBasedTimestampSynchronizer;
import com.fasterxml.uuid.ext.JavaUtilLogger;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.After;

/**
 *
 * @author ancoron
 */
@RunWith(Parameterized.class)
public class SerialTimeBasedGeneratorTest
{

    private static final Logger LOG = Logger.getLogger(SerialTimeBasedGeneratorTest.class.getName());

    private static final SecureRandom RAND = new SecureRandom();

    private static final int BUFFER_SIZE = 4 * 1024 * 1024;
    private static final int THREADS = Integer.getInteger("nodes", Runtime.getRuntime().availableProcessors());
    private static final int COUNT = Integer.getInteger("count", 1_000_000);
    private static final String DIR_OUTPUT = System.getProperty("output.dir");
    private static final Integer DAYS_TIMERANGE = Integer.getInteger("interval_days", 365);
    private static final Integer DAYS_OFFSET = Integer.getInteger("offset_days", 0);
    private static final boolean HISTORIC = Boolean.getBoolean("uuid.historic");
    private static final boolean REVERSED = Boolean.getBoolean("uuid.reversed");
    private static final String SHIFT = System.getProperty("uuid.shifts");
    private static final boolean SKIP_V1 = Boolean.getBoolean("uuid.skip.v1");

    private static final Map<EthernetAddress, UUIDTimer> CONFIGS = new LinkedHashMap<>();

    private NoArgGenerator[] instances;

    @Parameterized.Parameter(0)
    public int shift;

    @Parameterized.Parameter(1)
    public boolean historic;

    @Parameterized.Parameter(2)
    public Long startTime;

    @Parameterized.Parameter(3)
    public Long interval;

    @Parameterized.Parameters(name = "Test UUID generator for shift = {0} (historic: {1})")
    public static Object[][] data() throws Exception
    {
        Duration duration = Duration.ofDays(DAYS_TIMERANGE);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Zulu"));
        ZonedDateTime start = now.minus(duration).minusDays(DAYS_OFFSET);

        long startTime = start.toInstant().toEpochMilli();
        long interval = duration.dividedBy(COUNT).toNanos();

        LOG.log(Level.INFO, "Start time for UUID timestamp is {0} (interval: {1} nanoseconds)",
                new Object[]{start, interval});

        List<Object[]> params = new ArrayList<>();

        if (!SKIP_V1) {
            if (HISTORIC) {
                params.add(new Object[] {-1, true, startTime, interval});
            } else {
                params.add(new Object[] {-1, false, null, null});
            }
        }

        if (HISTORIC) {
            params.add(new Object[] {0, true, startTime, interval});
        } else {
            params.add(new Object[] {0, false, null, null});
        }

        if (SHIFT != null && !SHIFT.trim().isEmpty()) {
            Stream.of(SHIFT.split(","))
                    .map(Integer::valueOf)
                    .forEach((shift) -> {
                        if (HISTORIC) {
                            params.add(new Object[] {shift, true, startTime, interval});
                        } else {
                            params.add(new Object[] {shift, false, null, null});
                        }
                    });
        }

        if (REVERSED) {
            if (HISTORIC) {
                params.add(new Object[] {-2, true, startTime, interval});
            } else {
                params.add(new Object[] {-2, false, null, null});
            }
        }

        return params.toArray(new Object[0][]);
    }

    @BeforeClass
    public static void init() throws IOException
    {
        JavaUtilLogger.setLogLevel(JavaUtilLogger.LOG_ERROR_AND_ABOVE);

        Map<EthernetAddress, UUIDTimer> configs = new LinkedHashMap<>();

        for (int i = 0; i < THREADS; i++) {
            EthernetAddress eth = generateNode();
            UUIDTimer timer = new UUIDTimer(RAND, getSync(eth));
            configs.put(eth, timer);
        }

        CONFIGS.putAll(configs);
    }

    private static EthernetAddress generateNode()
    {
        long real = EthernetAddress.fromInterface().toLong();
        real = real | 0xFFFFFF & (RAND.nextLong() & 0xFFFFFF);
        return new EthernetAddress(real);
    }

    private static TimestampSynchronizer getSync(EthernetAddress node) throws IOException
    {
        File lock_a = new File(DIR_OUTPUT, "uuid." + Long.toHexString(node.toLong()) + ".a.lck");
        File lock_b = new File(DIR_OUTPUT, "uuid." + Long.toHexString(node.toLong()) + ".b.lck");
        return new FileBasedTimestampSynchronizer(lock_a, lock_b);
    }

    private static NoArgGenerator generate(EthernetAddress eth, UUIDTimer timer,
            int shift, boolean historic, Long startTime, Long interval)
    {
        final NoArgGenerator gen;

        switch (shift) {
            case -2: {
                if (historic) {
                    gen = new HistoricReversedTimeIntervalGenerator(eth, timer, startTime, interval);
                } else {
                    gen = new ReversedTimeBasedGenerator(eth, timer);
                }
                break;
            }

            case -1: {
                if (historic) {
                    gen = new HistoricTimeBasedGenerator(eth, timer, startTime, interval);
                } else {
                    gen = new TimeBasedGenerator(eth, timer);
                }
                break;
            }

            default: {
                if (historic) {
                    gen = new HistoricSerialTimeBasedGenerator(eth, timer, shift, startTime, interval);
                } else {
                    gen = new SerialTimeBasedGenerator(eth, timer, shift);
                }
            }
        }

        return gen;
    }

    @Before
    public void initTest() throws Exception
    {
        List<NoArgGenerator> tmp = new ArrayList<>();
        CONFIGS.entrySet().forEach((entry) -> {
            tmp.add(generate(entry.getKey(), entry.getValue(), shift, historic, startTime, interval));
        });
        instances = tmp.toArray(new NoArgGenerator[tmp.size()]);
    }

    @Test
    public void format() throws Exception
    {
        UUID uuid = instances[0].generate();
        logUuid("UUID", uuid);
    }

    protected void logUuid(String prefix, UUID uuid)
    {
        ZonedDateTime ts = null;
        if (shift == -1) {
            long timestamp = (uuid.timestamp() - 0x01b21dd213814000L) / 10000;
            ts = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.of("Zulu"));
        }

        LOG.log(Level.INFO, "{0}: {1} (version={2}, variant={3}, MSBs=0x{4},"
                + " clock-sequence={5,number,#}, shift={6,number,#},"
                + " timestamp={7})",
                new Object[]{
                    prefix,
                    uuid,
                    uuid.version(),
                    uuid.variant(),
                    Long.toHexString(uuid.getMostSignificantBits()).toUpperCase(),
                    uuid.clockSequence(),
                    shift,
                    ts
                }
        );
    }

    @Test
    @SuppressWarnings("UnusedAssignment")
    public void generate() throws Exception
    {
        final int coreCount = instances.length;
        final int perNode = COUNT / coreCount + 1;

        final UUID[] uuids = new UUID[COUNT];

        ExecutorService exe = Executors.newFixedThreadPool(coreCount);

        for (int i = 0; i < coreCount; i++) {
            final NoArgGenerator gen = instances[i];
            synchronized (gen) {
                gen.wait(0, 123);
            }

            final int offset = i;
            exe.submit(() -> {
                for (int j = 0; j < perNode; j++) {
                    int index = j * coreCount + offset;
                    if (index < COUNT) {
                        uuids[index] = gen.generate();
                    }
                }
            });
        }

        exe.shutdown();
        exe.awaitTermination(5, TimeUnit.MINUTES);

        logUuid("First", uuids[0]);
        logUuid(" Last", uuids[uuids.length - 1]);

        UUID curr;
        for (int i = 0; i < COUNT; i++) {
            curr = uuids[i];

            Assert.assertEquals("Unexpected UUID version in '" + curr + "'", 1, curr.version());
            Assert.assertEquals("Unexpected UUID variant in '" + curr + "'", 2, curr.variant());
        }


        String filename = "uuids";
        switch (shift) {
            case -2:
                filename += ".reversed-serial";
                break;
            case -1:
                filename += ".v1";
                break;
            default:
                filename += ".serial";
                break;
        }

        if (shift > 0) {
            filename += "_" + (shift * 8);
        }

        if (historic) {
            filename += ".historic";
        }
        writeUUIDs(filename + ".txt", uuids);

        // cleanup memory for the next run...
        exe = null;
        Arrays.fill(uuids, null);
    }

    @After
    public void cleanup() {
        System.gc();
    }

    private void writeUUIDs(String filename, UUID[] uuids) throws IOException
    {
        if (DIR_OUTPUT == null || DIR_OUTPUT.trim().isEmpty()
                || !Files.isDirectory(Paths.get(DIR_OUTPUT))) {
            return;
        }

        File out = new File(DIR_OUTPUT, filename);
        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(out), BUFFER_SIZE)) {
            for (UUID uuid : uuids) {
                fos.write(uuid.toString().getBytes(StandardCharsets.US_ASCII));
                fos.write('\n');
            }
        }
    }
}
