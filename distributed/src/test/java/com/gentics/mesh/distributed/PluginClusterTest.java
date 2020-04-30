package com.gentics.mesh.distributed;

import static com.gentics.mesh.core.rest.plugin.PluginStatus.PRE_REGISTERED;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.REGISTERED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * These tests require the test plugins to be build. You can build these plugins using the /core/build-test-plugins.sh script.
 */
public class PluginClusterTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	private static final int STARTUP_TIMEOUT = 500;

	private static final Logger log = LoggerFactory.getLogger(PluginClusterTest.class);

	@ClassRule
	public static MeshDockerServer serverA = new MeshDockerServer(vertx)
		.withClusterName(clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.waitForStartup()
		.withWriteQuorum(2)
		.withPlugin(new File("../core/target/test-plugins/basic/target/basic-plugin-0.0.1-SNAPSHOT.jar"), "basic.jar")
		.withClearFolders();

	@BeforeClass
	public static void waitForNodes() throws InterruptedException {
		LoggingConfigurator.init();
		serverA.awaitStartup(STARTUP_TIMEOUT);
	}

	@Before
	public void setupLogin() {
		login(serverA);
	}

	@Test
	public void testPluginDeployment() throws InterruptedException {
		// With one node the quorum is not reached and thus the plugin should not be registered.
		assertNoPluginRegistration(serverA, 3000);
		MeshDockerServer serverB = addSlave("nodeB");
		try {
			waitForPluginRegistration(serverA, 3000);
			waitForPluginRegistration(serverB, 3000);
		} finally {
			serverB.stop();
		}
	}

	/**
	 * Assert that no plugin registration happens within the given time.
	 * 
	 * @param server
	 * @param timeInMilliseconds
	 * @throws InterruptedException
	 */
	private void assertNoPluginRegistration(MeshDockerServer container, int timeInMilliseconds) throws InterruptedException {
		Thread.sleep(timeInMilliseconds);
		PluginListResponse plugins = call(() -> container.client().findPlugins());
		assertEquals("One plugin should be listed.", 1, plugins.getData().size());
		assertEquals("The plugin should still be registered.", PRE_REGISTERED, plugins.getData().get(0).getStatus());
	}

	private MeshDockerServer addSlave(String nodeName) throws InterruptedException {
		MeshDockerServer server = prepareSlave(clusterPostFix, nodeName, randomToken(), true, 2)
			.withPlugin(new File("../core/target/test-plugins/basic/target/basic-plugin-0.0.1-SNAPSHOT.jar"), "basic.jar");
		server.start();
		server.awaitStartup(STARTUP_TIMEOUT);
		login(server);
		return server;
	}

	/**
	 * Check whether the plugin can be found and is registered. Otherwise fail after the given timeout is exceeded.
	 * 
	 * @param container
	 * @param timeoutInMilliseconds
	 * @throws InterruptedException
	 */
	private void waitForPluginRegistration(MeshDockerServer container, int timeoutInMilliseconds) throws InterruptedException {
		long start = System.currentTimeMillis();
		PluginResponse plugin = null;
		while (true) {
			long dur = System.currentTimeMillis() - start;
			if (dur >= timeoutInMilliseconds) {
				if (plugin != null) {
					log.info("Last response: " + plugin.toJson());
				}
				fail("Timeout for plugin registration exceeded in " + container.getNodeName());
			}
			PluginListResponse plugins = call(() -> container.client().findPlugins());
			if (plugins.getData().size() == 1) {
				plugin = plugins.getData().get(0);
				if (REGISTERED == plugin.getStatus()) {
					log.info("Plugin registered in container {}", container.getNodeName());
					return;
				}
			}
			// Plugin not yet seen as ready. Lets wait.
			Thread.sleep(250);
		}
	}

	private void login(MeshDockerServer server) {
		server.client().setLogin("admin", "admin");
		server.client().login().blockingGet();
	}

}
