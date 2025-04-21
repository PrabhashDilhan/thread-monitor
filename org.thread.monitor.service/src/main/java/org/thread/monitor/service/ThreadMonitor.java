package org.thread.monitor.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ThreadMonitor {
    private static final Log logger = LogFactory.getLog(ThreadMonitor.class);
    private final String threadNamePrefix;
    private final int samplingInterval;
    private final int sampleCount;
    private final ScheduledExecutorService scheduler;
    private final Map<Long, List<ThreadInfo>> problematicThreads;
    private final Map<Long, ThreadStateInfo> threadStateInfo;
    private final AtomicBoolean isRunning;
    private final ThreadMXBean threadBean;
    private final long runnableThreshold; // milliseconds

    private static class ThreadStateInfo {
        final long startTime;
        final String lockName;
        final long lockOwnerId;

        ThreadStateInfo(long startTime, String lockName, long lockOwnerId) {
            this.startTime = startTime;
            this.lockName = lockName;
            this.lockOwnerId = lockOwnerId;
        }
    }

    public ThreadMonitor(String threadNamePrefix, int samplingInterval, int sampleCount, long runnableThreshold) {
        this.threadNamePrefix = threadNamePrefix;
        this.samplingInterval = samplingInterval;
        this.sampleCount = sampleCount;
        this.runnableThreshold = runnableThreshold;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.problematicThreads = new ConcurrentHashMap<>();
        this.threadStateInfo = new ConcurrentHashMap<>();
        this.isRunning = new AtomicBoolean(false);
        this.threadBean = ManagementFactory.getThreadMXBean();
    }

    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::monitorThreads, 0, samplingInterval, TimeUnit.SECONDS);
            logger.info("Thread monitoring started for prefix: {"+threadNamePrefix+"}, interval: {"+samplingInterval+"}s, sample count: {"+sampleCount+"}, runnable threshold: {"+runnableThreshold+"}ms");
        }
    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("Thread monitoring stopped for prefix: "+ threadNamePrefix);
        }
    }

    private void monitorThreads() {
        ThreadInfo[] allThreads = threadBean.dumpAllThreads(true, true);
        Map<Long, ThreadInfo> currentProblematicThreads = new HashMap<>();

        // Find currently problematic threads
        for (ThreadInfo threadInfo : allThreads) {
            if (threadInfo != null && threadInfo.getThreadName().startsWith(threadNamePrefix)) {
                if (isProblematic(threadInfo)) {
                    currentProblematicThreads.put(threadInfo.getThreadId(), threadInfo);
                }
            }
        }

        // Update tracking of problematic threads
        for (Map.Entry<Long, ThreadInfo> entry : currentProblematicThreads.entrySet()) {
            long threadId = entry.getKey();
            ThreadInfo threadInfo = entry.getValue();
            
            problematicThreads.computeIfAbsent(threadId, k -> new ArrayList<>()).add(threadInfo);
            
            // Check if we have enough samples to log
            List<ThreadInfo> samples = problematicThreads.get(threadId);
            if (samples.size() >= sampleCount) {
                logProblematicThread(threadInfo, samples);
                problematicThreads.remove(threadId);
                threadStateInfo.remove(threadId);
            }
        }

        // Remove threads that are no longer problematic
        problematicThreads.keySet().removeIf(threadId -> !currentProblematicThreads.containsKey(threadId));
        threadStateInfo.keySet().removeIf(threadId -> !currentProblematicThreads.containsKey(threadId));
    }

    private boolean isProblematic(ThreadInfo threadInfo) {
        Thread.State state = threadInfo.getThreadState();
        long threadId = threadInfo.getThreadId();
        long currentTime = System.currentTimeMillis();
        
        // Check for blocked/waiting state
        if (state.equals(Thread.State.BLOCKED)) {
            return true;
        } else if (state.equals(Thread.State.WAITING) ||
                state.equals(Thread.State.TIMED_WAITING)) {
            StackTraceElement[] stackTrace = threadInfo.getStackTrace();
            if (stackTrace.length > 0 && !"park".equals(stackTrace[0].getMethodName())) {
                return true;
            }
        }
        
        // Check for long-running RUNNABLE state
        if (state.equals(Thread.State.RUNNABLE)) {
            ThreadStateInfo currentState = new ThreadStateInfo(
                currentTime,
                threadInfo.getLockName(),
                threadInfo.getLockOwnerId()
            );
            
            ThreadStateInfo previousState = threadStateInfo.get(threadId);
            
            if (previousState == null) {
                // First time seeing this thread in RUNNABLE state
                threadStateInfo.put(threadId, currentState);
                return false;
            }
            
            // Check if thread has been in RUNNABLE state for too long
            if (currentTime - previousState.startTime > runnableThreshold) {
                // Check if it's holding the same lock or if other threads are waiting for its locks
                if (isHoldingSameLock(previousState, currentState) || isHoldingContendedLocks(threadInfo)) {
                    return true;
                }
            }
            
            // Update the state info
            threadStateInfo.put(threadId, currentState);
        }
        
        return false;
    }

    private boolean isHoldingSameLock(ThreadStateInfo previousState, ThreadStateInfo currentState) {
        // Check if the thread is holding the same lock as before
        return previousState.lockName != null && 
               previousState.lockName.equals(currentState.lockName);
    }

    private boolean isHoldingContendedLocks(ThreadInfo threadInfo) {
        // Check if this thread is holding locks that other threads are waiting for
        ThreadInfo[] allThreads = threadBean.dumpAllThreads(false, false);
        for (ThreadInfo otherThread : allThreads) {
            if (otherThread != null && otherThread.getThreadState() == Thread.State.BLOCKED) {
                // Check if the blocked thread is waiting for a lock held by our thread
                if (otherThread.getLockOwnerId() == threadInfo.getThreadId()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void logProblematicThread(ThreadInfo threadInfo, List<ThreadInfo> samples) {
        StringBuilder sb = new StringBuilder();
        sb.append("\nThread '").append(threadInfo.getThreadName())
          .append("' has been problematic for ").append(samples.size())
          .append(" consecutive samples.\n");
        
        sb.append("Current state: ").append(threadInfo.getThreadState()).append("\n");
        
        if (threadInfo.getLockOwnerId() != -1) {
            sb.append("Lock owner ID: ").append(threadInfo.getLockOwnerId()).append("\n");
        }
        
        if (threadInfo.getLockName() != null) {
            sb.append("Lock name: ").append(threadInfo.getLockName()).append("\n");
        }
        
        ThreadStateInfo stateInfo = threadStateInfo.get(threadInfo.getThreadId());
        if (stateInfo != null) {
            sb.append("Time in current state: ")
              .append((System.currentTimeMillis() - stateInfo.startTime) / 1000)
              .append(" seconds\n");
        }
        
        sb.append("Stack trace:\n");
        for (StackTraceElement element : threadInfo.getStackTrace()) {
            sb.append("\tat ").append(element).append("\n");
        }
        
        logger.warn(sb.toString());
    }
}
