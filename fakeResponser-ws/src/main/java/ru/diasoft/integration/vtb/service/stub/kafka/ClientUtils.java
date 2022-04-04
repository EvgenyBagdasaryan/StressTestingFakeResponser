package ru.diasoft.integration.vtb.service.stub.kafka;

import org.apache.kafka.clients.ClientDnsLookup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.HostResolver;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigException;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.network.ChannelBuilder;
import org.apache.kafka.common.network.ChannelBuilders;
import org.apache.kafka.common.security.JaasContext;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.utils.LogContext;
import org.apache.kafka.common.utils.Time;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.kafka.common.utils.Utils.getHost;
import static org.apache.kafka.common.utils.Utils.getPort;

public final class ClientUtils {
    private static final Logger log = LogManager.getLogger(ClientUtils.class);

    private ClientUtils() {
    }

    public static List<InetSocketAddress> parseAndValidateAddresses(List<String> urls, String clientDnsLookupConfig) {
        return parseAndValidateAddresses(urls, ClientDnsLookup.forConfig(clientDnsLookupConfig));
    }

    public static List<InetSocketAddress> parseAndValidateAddresses(List<String> urls, ClientDnsLookup clientDnsLookup) {
        List<InetSocketAddress> addresses = new ArrayList<>();
        for (String url : urls) {
            if (url != null && !url.isEmpty()) {
                try {
                    String host = getHost(url);
                    Integer port = getPort(url);
                    if (host == null || port == null)
                        throw new ConfigException("Invalid url in " + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + ": " + url);

                    if (clientDnsLookup == ClientDnsLookup.RESOLVE_CANONICAL_BOOTSTRAP_SERVERS_ONLY) {
                        InetAddress[] inetAddresses = InetAddress.getAllByName(host);
                        for (InetAddress inetAddress : inetAddresses) {
                            String resolvedCanonicalName = inetAddress.getCanonicalHostName();
                            InetSocketAddress address = new InetSocketAddress(resolvedCanonicalName, port);
                            if (address.isUnresolved()) {
                                log.warn("Couldn't resolve server " + url + " from " + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + " as DNS resolution of the canonical hostname " + resolvedCanonicalName + " failed for " + host);
                            } else {
                                addresses.add(address);
                            }
                        }
                    } else {
                        InetSocketAddress address = new InetSocketAddress(host, port);
                        if (address.isUnresolved()) {
                            log.warn("Couldn't resolve server " + url + " from " + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + " as DNS resolution failed for " + host);
                        } else {
                            addresses.add(address);
                        }
                    }

                } catch (IllegalArgumentException e) {
                    throw new ConfigException("Invalid port in " + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + ": " + url);
                } catch (UnknownHostException e) {
                    throw new ConfigException("Unknown host in " + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG + ": " + url);
                }
            }
        }
        if (addresses.isEmpty()){
            throw new ConfigException("No resolvable bootstrap urls given in " + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
        }
        return addresses;
    }

    /**
     * Create a new channel builder from the provided configuration.
     *
     * @param config client configs
     * @param time the time implementation
     * @param logContext the logging context
     *
     * @return configured ChannelBuilder based on the configs.
     */
    public static ChannelBuilder createChannelBuilder(AbstractConfig config, Time time, LogContext logContext) {
        SecurityProtocol securityProtocol = SecurityProtocol.forName(config.getString(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        String clientSaslMechanism = config.getString(SaslConfigs.SASL_MECHANISM);
        return ChannelBuilders.clientChannelBuilder(securityProtocol, JaasContext.Type.CLIENT, config, null,
                clientSaslMechanism, time, true, logContext);
    }

    static List<InetAddress> resolve(String host, HostResolver hostResolver) throws UnknownHostException {
        InetAddress[] addresses = hostResolver.resolve(host);
        List<InetAddress> result = filterPreferredAddresses(addresses);
        if (log.isDebugEnabled())
            log.debug("Resolved host " + host + " as " + result.stream().map(i -> i.getHostAddress()).collect(Collectors.joining(",")));
        return result;
    }

    /**
     * Return a list containing the first address in `allAddresses` and subsequent addresses
     * that are a subtype of the first address.
     *
     * The outcome is that all returned addresses are either IPv4 or IPv6 (InetAddress has two
     * subclasses: Inet4Address and Inet6Address).
     */
    static List<InetAddress> filterPreferredAddresses(InetAddress[] allAddresses) {
        List<InetAddress> preferredAddresses = new ArrayList<>();
        Class<? extends InetAddress> clazz = null;
        for (InetAddress address : allAddresses) {
            if (clazz == null) {
                clazz = address.getClass();
            }
            if (clazz.isInstance(address)) {
                preferredAddresses.add(address);
            }
        }
        return preferredAddresses;
    }
}
