package com.failurelab.service_a.chaos;

import com.failurelab.service_a.error.ScenarioAlreadyRunningException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lab-only scenario: allocates heap until the JVM throws a real OutOfMemoryError, then releases it.
 * Catching OutOfMemoryError is deliberate here (ADR 0002) and must not be copied elsewhere.
 */
@Component
class ResourceExhaustionJob {

    static final String SCENARIO = "resource-exhaustion";

    private static final Logger log = LoggerFactory.getLogger(ResourceExhaustionJob.class);
    private static final int BYTES_PER_MB = 1024 * 1024;

    private final AtomicReference<Run> current = new AtomicReference<>();

    void start(ExhaustionSettings settings) {
        Run run = new Run(settings);
        if (!current.compareAndSet(null, run)) {
            throw new ScenarioAlreadyRunningException(SCENARIO);
        }
        run.start();
    }

    boolean isRunning() {
        return current.get() != null;
    }

    void stop() {
        Run run = current.getAndSet(null);
        if (run != null) {
            run.stop();
            log.info("Scenario {} stopped by reset, memory released", SCENARIO);
        }
    }

    record ExhaustionSettings(int chunkMb, int intervalMs, int cpuThreads) {
    }

    private final class Run {

        private final ExhaustionSettings settings;
        private final List<Thread> threads = new ArrayList<>();
        private volatile boolean active = true;
        private volatile double cpuSink;

        private Run(ExhaustionSettings settings) {
            this.settings = settings;
        }

        private void start() {
            log.warn("Scenario {} started: {} MB every {} ms, {} CPU threads",
                    SCENARIO, settings.chunkMb(), settings.intervalMs(), settings.cpuThreads());
            for (int i = 0; i < settings.cpuThreads(); i++) {
                threads.add(daemon(this::burnCpu, "exhaustion-cpu-" + i));
            }
            threads.add(daemon(this::allocateMemory, "exhaustion-memory"));
            threads.forEach(Thread::start);
        }

        private void stop() {
            active = false;
            threads.forEach(Thread::interrupt);
        }

        private void burnCpu() {
            double accumulator = 0;
            long i = 0;
            while (active) {
                accumulator += Math.sin(i) * Math.cos(i++);
            }
            cpuSink = accumulator;
        }

        private void allocateMemory() {
            List<byte[]> retained = new ArrayList<>();
            long allocatedMb = 0;
            try {
                while (active) {
                    retained.add(new byte[settings.chunkMb() * BYTES_PER_MB]);
                    allocatedMb += settings.chunkMb();
                    Thread.sleep(settings.intervalMs());
                }
            } catch (OutOfMemoryError e) {
                retained.clear();
                log.error("OutOfMemoryError during {} scenario after {} MB allocated", SCENARIO, allocatedMb, e);
                active = false;
                current.compareAndSet(this, null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                retained.clear();
            }
        }

        private Thread daemon(Runnable task, String name) {
            Thread thread = new Thread(task, name);
            thread.setDaemon(true);
            return thread;
        }
    }
}
