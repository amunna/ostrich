package com.bazaarvoice.soa.discovery;

import com.bazaarvoice.soa.ServiceEndpoint;
import com.bazaarvoice.soa.registry.ZooKeeperServiceRegistry;
import com.bazaarvoice.soa.test.ZooKeeperTest;
import com.bazaarvoice.soa.zookeeper.ZooKeeperConfiguration;
import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class ZooKeeperHostDiscoveryTest extends ZooKeeperTest {
    private static final ServiceEndpoint FOO = new ServiceEndpoint("Foo", "server", 8080);

    private ZooKeeperServiceRegistry _registry;
    private ZooKeeperHostDiscovery _discovery;

    @Override
    public void setup() throws Exception {
        super.setup();
        _registry = new ZooKeeperServiceRegistry(newZooKeeperConfiguration());
        _discovery = new ZooKeeperHostDiscovery(newCurator(), FOO.getServiceName());
    }

    @Override
    public void teardown() throws Exception {
        Closeables.closeQuietly(_discovery);
        super.teardown();
    }

    @Test(expected = NullPointerException.class)
    public void testNullConfiguration() {
        new ZooKeeperHostDiscovery((ZooKeeperConfiguration)null, FOO.getServiceName());
    }

    @Test(expected = NullPointerException.class)
    public void testNullServiceName() throws Exception {
        new ZooKeeperHostDiscovery(newCurator(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyServiceName() throws Exception {
        new ZooKeeperHostDiscovery(newCurator(), "");
    }

    @Test
    public void testRegisterService() {
        _registry.register(FOO);
        assertTrue(waitUntilSize(_discovery.getHosts(), 1));
    }

    @Test
    public void testUnregisterService() {
        _registry.register(FOO);
        assertTrue(waitUntilSize(_discovery.getHosts(), 1));

        _registry.unregister(FOO);
        assertTrue(waitUntilSize(_discovery.getHosts(), 0));
    }

    @Test
    public void testServiceRemovedWhenSessionKilled() throws Exception {
        _registry.register(FOO);
        assertTrue(waitUntilSize(_discovery.getHosts(), 1));

        killSession(_discovery.getCurator());

        // The entry gets cleaned up because we've lost contact with ZooKeeper
        assertTrue(waitUntilSize(_discovery.getHosts(), 0));

    }

    @Test
    public void testServiceReRegisteredWhenSessionKilled() throws Exception {
        _registry.register(FOO);
        assertTrue(waitUntilSize(_discovery.getHosts(), 1));

        killSession(_discovery.getCurator());

        // The entry gets cleaned up because we've lost contact with ZooKeeper
        assertTrue(waitUntilSize(_discovery.getHosts(), 0));

        // Then it automatically gets created when the connection is re-established with ZooKeeper
        assertTrue(waitUntilSize(_discovery.getHosts(), 1));
    }

    private static <T> boolean waitUntilSize(Iterable<T> iterable, int size, long timeout, TimeUnit unit) {
        long start = System.nanoTime();
        while (System.nanoTime() - start <= unit.toNanos(timeout)) {
            if (Iterables.size(iterable) == size) {
                return true;
            }

            Thread.yield();
        }

        return false;
    }

    private static <T> boolean waitUntilSize(Iterable<T> iterable, int size) {
        return waitUntilSize(iterable, size, 10, TimeUnit.SECONDS);
    }
}
