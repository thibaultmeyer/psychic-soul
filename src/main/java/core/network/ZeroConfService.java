package core.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;

public class ZeroConfService extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(ZeroConfService.class.getName());
    private JmDNS _mdns;
    private ServiceInfo _service_info;

    public ZeroConfService(int port, String description) {
        try {
            _mdns = JmDNS.create();
            _service_info = ServiceInfo.create("_ns-server._tcp.local.", "ns-server", port, 0, 0, description);
        } catch (IOException ignore) {
            LOG.warn("Can't initialize auto discovery service (mDNS)");
            _mdns = null;
        }
    }

    public void unregisterService() {
        if (_mdns != null) {
            try {
                LOG.info("Unregistering service from DNS (mDNS)");
                _mdns.close();
                _mdns = null;
            } catch (IOException ignore) {
            }
        }
    }

    @Override
    public void run() {
        try {
            LOG.info("Registering service to DNS (mDNS)");
            if (_mdns != null) {
                _mdns.registerService(_service_info);
            }
        } catch (IOException e) {
            LOG.warn("Registration failed: %s", e);
        }
    }
}
