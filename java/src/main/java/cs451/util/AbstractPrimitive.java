package cs451.util;

import cs451.Constants;
import cs451.Host;
import cs451.interfaces.Writer;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

/*
    It defines a send or deliver primitive giving them the common actions (line logging and thread spawning)
 */
public abstract class AbstractPrimitive {
    protected final Host host;
    protected final List<Host> hosts;
    private final Set<String> log;
    private final ThreadPoolExecutor executorService;

    private final Writer writer;
    private final Integer writeLimit;

    public AbstractPrimitive(Host host, List<Host> hosts, Set<String> log, Writer writer, ThreadPoolExecutor executorService) {
        this.host = host;
        this.log = log;
        this.hosts = hosts;
        this.executorService = executorService;

        this.writer = writer;
        this.writeLimit = Constants.WRITE_LOG_LIMIT / this.hosts.size();
    }

    protected boolean log(String log) {
        if(this.log.size() > this.writeLimit) {
            this.writer.finalizeLog();
        }

        return this.log.add(log);
    }

    // Only ComparableRunnable are acceptable because of the PriorityBlockingQueue
    protected void start(Runnable runnable) {
        if(!executorService.isShutdown()) {
            executorService.execute(runnable);
        }
    }
}
