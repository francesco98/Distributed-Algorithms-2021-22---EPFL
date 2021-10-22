package cs451.util;

import cs451.Host;

import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/*
    It defines a send or deliver primitive giving them the common actions (line logging and thread spawning)
 */
public abstract class AbstractPrimitive {
    protected final Host host;
    private final Set<String> log;
    private final ThreadPoolExecutor executorService;

    public AbstractPrimitive(Host host, Set<String> log, ThreadPoolExecutor executorService) {
        this.host = host;
        this.log = log;
        this.executorService = executorService;
    }

    protected boolean log(String log) {
        return this.log.add(log);
    }

    // Only ComparableRunnable are acceptable because of the PriorityBlockingQueue
    protected void start(Runnable runnable) {
        if(!executorService.isShutdown()) {
            executorService.execute(runnable);
        }
    }
}
