package org.asni.storage;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.InvalidConfigurationException;
import org.testcontainers.dockerclient.TransportConfig;

import java.net.URI;

public class HttpClientDockerProviderStrategy extends DockerClientProviderStrategy {

    private static final String SOCKET_PATH = resolveSocketPath();
    private static final URI DOCKER_HOST = URI.create("unix://" + SOCKET_PATH);

    private static String resolveSocketPath() {
        String homeSocket = System.getProperty("user.home") + "/.docker/run/docker.sock";
        if (new java.io.File(homeSocket).exists()) {
            return homeSocket;
        }
        return "/var/run/docker.sock";
    }

    private volatile DockerClient instance;

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
        return TransportConfig.builder()
                .dockerHost(DOCKER_HOST)
                .build();
    }

    @Override
    public DockerClient getDockerClient() {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = buildClient();
                }
            }
        }
        return instance;
    }

    private DockerClient buildClient() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(DOCKER_HOST.toString())
                .withApiVersion("1.44")
                .build();

        JavaUnixSocketDockerHttpClient httpClient = new JavaUnixSocketDockerHttpClient(SOCKET_PATH);

        return DockerClientImpl.getInstance(config, httpClient);
    }

    @Override
    public String getDescription() {
        return "Java unix socket (no dependencies)";
    }

    @Override
    protected int getPriority() {
        return 999;
    }
}
