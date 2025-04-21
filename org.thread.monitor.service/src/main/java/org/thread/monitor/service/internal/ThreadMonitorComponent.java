package org.thread.monitor.service.internal;


import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.thread.monitor.service.ThreadMonitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.thread.monitor.service.ThreadMonitorServer;

import java.io.IOException;

@Component(
        name = "thread.monitor.service",
        immediate = true
)
public class ThreadMonitorComponent {
    private static final Log log = LogFactory.getLog(ThreadMonitorComponent.class);

    @Activate
    protected void activate(ComponentContext context) {
        log.info("Thread monitor service bundle is activated");
        ThreadMonitorServer server = new ThreadMonitorServer(8888);
        try {
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //ThreadMonitor threadMonitor = new ThreadMonitor("PassThroughMessageProcessor",5,2,5000);
        //threadMonitor.start();
    }

}
