package com.github.ddth.log2scribe.log4jappender;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import scribe.thrift.LogEntry;
import scribe.thrift.scribe;

import com.github.ddth.thriftpool.AbstractTProtocolFactory;
import com.github.ddth.thriftpool.ITProtocolFactory;
import com.github.ddth.thriftpool.PoolConfig;
import com.github.ddth.thriftpool.ThriftClientPool;

/**
 * Scribe-log appender for log4j.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public class ScribelogAppender extends AppenderSkeleton {

    private String scribeHostsAndPorts = "localhost:1463";
    private String scribeCategory = "default";
    private ThriftClientPool<scribe.Client, scribe.Iface> scribeClientPool;

    public void setScribeHostsAndPorts(String scribeHostsAndPorts) {
        this.scribeHostsAndPorts = scribeHostsAndPorts;
    }

    public void setScribeCategory(String scribeCategory) {
        this.scribeCategory = scribeCategory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        scribeClientPool.destroy();
    }

    /**
     * This appender does not require a layout, but layout is recommended!
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean requiresLayout() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void activateOptions() {
        ITProtocolFactory protocolFactory = new AbstractTProtocolFactory(scribeHostsAndPorts) {
            @Override
            protected TProtocol create(HostAndPort hostAndPort) throws Exception {
                TTransport transport = new TFramedTransport(new TSocket(hostAndPort.host,
                        hostAndPort.port));
                transport.open();
                TProtocol protocol = new TBinaryProtocol(transport);
                return protocol;
            }
        };

        scribeClientPool = new ThriftClientPool<scribe.Client, scribe.Iface>();
        scribeClientPool.setClientClass(scribe.Client.class).setClientInterface(scribe.Iface.class);
        scribeClientPool.setTProtocolFactory(protocolFactory);
        {
            PoolConfig poolConfig = new PoolConfig();
            poolConfig.setMaxActive(10);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            poolConfig.setTestWhileIdle(true);
            poolConfig.setMaxWaitTime(5000);
            scribeClientPool.setPoolConfig(poolConfig);
        }
        scribeClientPool.init();

        super.activateOptions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void append(LoggingEvent event) {
        String msg = layout != null ? layout.format(event) : (event != null ? event.toString()
                : null);
        if (msg != null) {
            try {
                scribe.Iface client = scribeClientPool.borrowObject();
                try {
                    LogEntry logEntry = new LogEntry(scribeCategory, msg);
                    List<LogEntry> logEntries = new ArrayList<LogEntry>();
                    logEntries.add(logEntry);
                    client.Log(logEntries);
                } finally {
                    scribeClientPool.returnObject(client);
                }
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
