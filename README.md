# Thread Monitor Service

A Java service for monitoring and detecting problematic threads in a running application. This service helps identify threads that are stuck in blocked, waiting, or long-running states, which can help diagnose performance issues and deadlocks.

## Features

- Monitors threads with a specific name prefix
- Detects threads in BLOCKED state
- Detects threads in WAITING/TIMED_WAITING state (excluding parking)
- Identifies RUNNABLE threads that:
  - Have been holding the same lock for an extended period
  - Are causing lock contention (other threads waiting for their locks)
- Configurable sampling interval and threshold
- Detailed logging of problematic threads including stack traces and lock information
- REST API for remote control of monitors

## REST API

The service provides a REST API for managing thread monitors. The API runs on port 8080 by default.

### Start a Monitor

```
GET /monitor/start?threadPrefix=<prefix>&interval=<seconds>&sampleCount=<count>&threshold=<milliseconds>
```

Parameters:
- `threadPrefix`: Prefix of threads to monitor
- `interval`: Sampling interval in seconds
- `sampleCount`: Number of consecutive samples before logging
- `threshold`: Threshold for RUNNABLE threads in milliseconds

Example:
```
GET /monitor/start?threadPrefix=synapse-worker&interval=5&sampleCount=3&threshold=10000
```

Response:
```json
{
    "status": "success",
    "message": "Monitor started for thread prefix: synapse-worker"
}
```

### Stop a Monitor

```
GET /monitor/stop?threadPrefix=<prefix>
```

Parameters:
- `threadPrefix`: Prefix of the monitor to stop

Example:
```
GET /monitor/stop?threadPrefix=synapse-worker
```

Response:
```json
{
    "status": "success",
    "message": "Monitor stopped for thread prefix: synapse-worker"
}
```

### Get Monitor Status

```
GET /monitor/status
```

Example:
```
GET /monitor/status
```

Response:
```json
{
    "monitors": [
        {
            "threadPrefix": "synapse-worker",
            "status": "running"
        }
    ]
}
```

## Usage

### Programmatic Usage

```java
// Create a monitor instance
ThreadMonitor monitor = new ThreadMonitor(
    "thread-prefix",  // Prefix of threads to monitor
    5,               // Sampling interval in seconds
    3,               // Number of consecutive samples before logging
    10000            // Threshold for RUNNABLE threads in milliseconds
);

// Start monitoring
monitor.start();

// ... application runs ...

// Stop monitoring when done
monitor.stop();
```

### Starting the REST Server

```java
// Create and start the REST server
ThreadMonitorServer server = new ThreadMonitorServer(8080);
server.start();

// ... server runs ...

// Stop the server when done
server.stop();
```

## Configuration Parameters

- `threadNamePrefix`: String prefix to identify which threads to monitor
- `samplingInterval`: How often to check thread states (in seconds)
- `sampleCount`: Number of consecutive problematic samples before logging
- `runnableThreshold`: How long a RUNNABLE thread can hold a lock before being considered problematic (in milliseconds)

## What Gets Logged

When a thread is identified as problematic, the following information is logged:
- Thread name and ID
- Current thread state
- Lock information (if applicable)
- Time spent in the problematic state
- Full stack trace
- Number of consecutive samples where the thread was problematic

## Example Log Output

```
Thread 'synapse-worker-1' has been problematic for 3 consecutive samples.
Current state: RUNNABLE
Lock name: java.util.concurrent.locks.ReentrantLock@12345678
Time in current state: 15 seconds
Stack trace:
    at java.net.SocketInputStream.socketRead0(Native Method)
    at java.net.SocketInputStream.read(SocketInputStream.java:150)
    ...
```

## Best Practices

1. Choose an appropriate `threadNamePrefix` that matches your application's thread naming convention
2. Set the `samplingInterval` based on your application's requirements:
   - Shorter intervals (1-5 seconds) for more responsive monitoring
   - Longer intervals (10-30 seconds) for less overhead
3. Adjust the `runnableThreshold` based on your application's expected behavior:
   - Lower values (5000-10000ms) for interactive applications
   - Higher values (30000-60000ms) for batch processing
4. Use the `sampleCount` to avoid false positives:
   - Higher values (3-5) for more stable detection
   - Lower values (1-2) for more sensitive detection

## Dependencies

- apache.commons.logging for logging
- Java Management Extensions (JMX) for thread monitoring
- Apache HTTP Components for REST API

## Deploy into the WSO2 server

To deploy this into the WSO2 servers, you just need to build and copy the OSGi bundle into the /dropins directory. Then we can use the REST API to start/stop the monitoring service. 
