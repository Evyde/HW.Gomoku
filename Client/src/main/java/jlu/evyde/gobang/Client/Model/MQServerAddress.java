package jlu.evyde.gobang.Client.Model;

import java.beans.BeanProperty;
import java.net.InetSocketAddress;
import java.net.URI;

public class MQServerAddress {
    private InetSocketAddress isa;
    private URI uri;

    public MQServerAddress() { this(SystemConfiguration.getMQServerHost(), SystemConfiguration.getMQServerPort()); }
    public MQServerAddress(String host) { this(host, SystemConfiguration.getMQServerPort()); }
    public MQServerAddress(Integer port) { this("localhost", port); }
    public MQServerAddress(String host, Integer port) {
        setUri(URI.create("ws://" + host + ":" + port + "/"));
        setIsa(new InetSocketAddress(host, port));
    }

    @BeanProperty
    public InetSocketAddress getIsa() {
        return isa;
    }

    @BeanProperty
    public URI getUri() {
        return uri;
    }

    @BeanProperty
    public void setIsa(InetSocketAddress isa) {
        this.isa = isa;
    }

    @BeanProperty
    public void setUri(URI uri) {
        this.uri = uri;
    }
}
