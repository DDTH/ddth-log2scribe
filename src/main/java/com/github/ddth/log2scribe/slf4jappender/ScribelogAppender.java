package com.github.ddth.log2scribe.slf4jappender;

import java.util.ArrayList;
import java.util.List;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import scribe.thrift.LogEntry;
import scribe.thrift.scribe;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

import com.github.ddth.thriftpool.AbstractTProtocolFactory;
import com.github.ddth.thriftpool.ITProtocolFactory;
import com.github.ddth.thriftpool.PoolConfig;
import com.github.ddth.thriftpool.ThriftClientPool;

/**
 * Scribe-log appender for logback (slf4j).
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.1.0
 */
public class ScribelogAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

    private Layout<ILoggingEvent> layout;

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
     * @param layout
     */
    public void setLayout(Layout<ILoggingEvent> layout) {
        this.layout = layout;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
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

        super.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        try {
            scribeClientPool.destroy();
        } catch (Exception e) {
        }

        super.stop();
    }

    @Override
    protected void append(ILoggingEvent obj) {
        String msg = layout != null ? layout.doLayout(obj) : (obj != null ? obj.toString() : null);
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
