package ru.diasoft.integration.vtb.service.stub.kafka;


import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.ClientDnsLookup;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.RangeAssignor;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.NonNullValidator;
import org.apache.kafka.common.config.ConfigDef.Range;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.ValidString;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.metrics.Sensor.RecordingLevel;
import org.apache.kafka.common.requests.JoinGroupRequest;
import org.apache.kafka.common.serialization.Deserializer;

public class ConsumerConfig extends AbstractConfig {
    private static final ConfigDef CONFIG;
    public static final String GROUP_ID_CONFIG = "group.id";
    private static final String GROUP_ID_DOC = "A unique string that identifies the consumer group this consumer belongs to. This property is required if the consumer uses either the group management functionality by using <code>subscribe(topic)</code> or the Kafka-based offset management strategy.";
    public static final String GROUP_INSTANCE_ID_CONFIG = "group.instance.id";
    private static final String GROUP_INSTANCE_ID_DOC = "A unique identifier of the consumer instance provided by the end user. Only non-empty strings are permitted. If set, the consumer is treated as a static member, which means that only one instance with this ID is allowed in the consumer group at any time. This can be used in combination with a larger session timeout to avoid group rebalances caused by transient unavailability (e.g. process restarts). If not set, the consumer will join the group as a dynamic member, which is the traditional behavior.";
    public static final String MAX_POLL_RECORDS_CONFIG = "max.poll.records";
    private static final String MAX_POLL_RECORDS_DOC = "The maximum number of records returned in a single call to poll(). Note, that <code>max.poll.records</code> does not impact the underlying fetching behavior. The consumer will cache the records from each fetch request and returns them incrementally from each poll.";
    public static final String MAX_POLL_INTERVAL_MS_CONFIG = "max.poll.interval.ms";
    private static final String MAX_POLL_INTERVAL_MS_DOC = "The maximum delay between invocations of poll() when using consumer group management. This places an upper bound on the amount of time that the consumer can be idle before fetching more records. If poll() is not called before expiration of this timeout, then the consumer is considered failed and the group will rebalance in order to reassign the partitions to another member. For consumers using a non-null <code>group.instance.id</code> which reach this timeout, partitions will not be immediately reassigned. Instead, the consumer will stop sending heartbeats and partitions will be reassigned after expiration of <code>session.timeout.ms</code>. This mirrors the behavior of a static consumer which has shutdown.";
    public static final String SESSION_TIMEOUT_MS_CONFIG = "session.timeout.ms";
    private static final String SESSION_TIMEOUT_MS_DOC = "The timeout used to detect client failures when using Kafka's group management facility. The client sends periodic heartbeats to indicate its liveness to the broker. If no heartbeats are received by the broker before the expiration of this session timeout, then the broker will remove this client from the group and initiate a rebalance. Note that the value must be in the allowable range as configured in the broker configuration by <code>group.min.session.timeout.ms</code> and <code>group.max.session.timeout.ms</code>.";
    public static final String HEARTBEAT_INTERVAL_MS_CONFIG = "heartbeat.interval.ms";
    private static final String HEARTBEAT_INTERVAL_MS_DOC = "The expected time between heartbeats to the consumer coordinator when using Kafka's group management facilities. Heartbeats are used to ensure that the consumer's session stays active and to facilitate rebalancing when new consumers join or leave the group. The value must be set lower than <code>session.timeout.ms</code>, but typically should be set no higher than 1/3 of that value. It can be adjusted even lower to control the expected time for normal rebalances.";
    public static final String BOOTSTRAP_SERVERS_CONFIG = "bootstrap.servers";
    public static final String CLIENT_DNS_LOOKUP_CONFIG = "client.dns.lookup";
    public static final String ENABLE_AUTO_COMMIT_CONFIG = "enable.auto.commit";
    private static final String ENABLE_AUTO_COMMIT_DOC = "If true the consumer's offset will be periodically committed in the background.";
    public static final String AUTO_COMMIT_INTERVAL_MS_CONFIG = "auto.commit.interval.ms";
    private static final String AUTO_COMMIT_INTERVAL_MS_DOC = "The frequency in milliseconds that the consumer offsets are auto-committed to Kafka if <code>enable.auto.commit</code> is set to <code>true</code>.";
    public static final String PARTITION_ASSIGNMENT_STRATEGY_CONFIG = "partition.assignment.strategy";
    private static final String PARTITION_ASSIGNMENT_STRATEGY_DOC = "A list of class names or class types, ordered by preference, of supported partition assignment strategies that the client will use to distribute partition ownership amongst consumer instances when group management is used. Available options are:<ul><li><code>org.apache.kafka.clients.consumer.RangeAssignor</code>: The default assignor, which works on a per-topic basis.</li><li><code>org.apache.kafka.clients.consumer.RoundRobinAssignor</code>: Assigns partitions to consumers in a round-robin fashion.</li><li><code>org.apache.kafka.clients.consumer.StickyAssignor</code>: Guarantees an assignment that is maximally balanced while preserving as many existing partition assignments as possible.</li><li><code>org.apache.kafka.clients.consumer.CooperativeStickyAssignor</code>: Follows the same StickyAssignor logic, but allows for cooperative rebalancing.</li></ul><p>Implementing the <code>org.apache.kafka.clients.consumer.ConsumerPartitionAssignor</code> interface allows you to plug in a custom assignment strategy.";
    public static final String AUTO_OFFSET_RESET_CONFIG = "auto.offset.reset";
    public static final String AUTO_OFFSET_RESET_DOC = "What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server (e.g. because that data has been deleted): <ul><li>earliest: automatically reset the offset to the earliest offset<li>latest: automatically reset the offset to the latest offset</li><li>none: throw exception to the consumer if no previous offset is found for the consumer's group</li><li>anything else: throw exception to the consumer.</li></ul>";
    public static final String FETCH_MIN_BYTES_CONFIG = "fetch.min.bytes";
    private static final String FETCH_MIN_BYTES_DOC = "The minimum amount of data the server should return for a fetch request. If insufficient data is available the request will wait for that much data to accumulate before answering the request. The default setting of 1 byte means that fetch requests are answered as soon as a single byte of data is available or the fetch request times out waiting for data to arrive. Setting this to something greater than 1 will cause the server to wait for larger amounts of data to accumulate which can improve server throughput a bit at the cost of some additional latency.";
    public static final String FETCH_MAX_BYTES_CONFIG = "fetch.max.bytes";
    private static final String FETCH_MAX_BYTES_DOC = "The maximum amount of data the server should return for a fetch request. Records are fetched in batches by the consumer, and if the first record batch in the first non-empty partition of the fetch is larger than this value, the record batch will still be returned to ensure that the consumer can make progress. As such, this is not a absolute maximum. The maximum record batch size accepted by the broker is defined via <code>message.max.bytes</code> (broker config) or <code>max.message.bytes</code> (topic config). Note that the consumer performs multiple fetches in parallel.";
    public static final int DEFAULT_FETCH_MAX_BYTES = 52428800;
    public static final String FETCH_MAX_WAIT_MS_CONFIG = "fetch.max.wait.ms";
    private static final String FETCH_MAX_WAIT_MS_DOC = "The maximum amount of time the server will block before answering the fetch request if there isn't sufficient data to immediately satisfy the requirement given by fetch.min.bytes.";
    public static final String METADATA_MAX_AGE_CONFIG = "metadata.max.age.ms";
    public static final String MAX_PARTITION_FETCH_BYTES_CONFIG = "max.partition.fetch.bytes";
    private static final String MAX_PARTITION_FETCH_BYTES_DOC = "The maximum amount of data per-partition the server will return. Records are fetched in batches by the consumer. If the first record batch in the first non-empty partition of the fetch is larger than this limit, the batch will still be returned to ensure that the consumer can make progress. The maximum record batch size accepted by the broker is defined via <code>message.max.bytes</code> (broker config) or <code>max.message.bytes</code> (topic config). See fetch.max.bytes for limiting the consumer request size.";
    public static final int DEFAULT_MAX_PARTITION_FETCH_BYTES = 1048576;
    public static final String SEND_BUFFER_CONFIG = "send.buffer.bytes";
    public static final String RECEIVE_BUFFER_CONFIG = "receive.buffer.bytes";
    public static final String CLIENT_ID_CONFIG = "client.id";
    public static final String CLIENT_RACK_CONFIG = "client.rack";
    public static final String RECONNECT_BACKOFF_MS_CONFIG = "reconnect.backoff.ms";
    public static final String RECONNECT_BACKOFF_MAX_MS_CONFIG = "reconnect.backoff.max.ms";
    public static final String RETRY_BACKOFF_MS_CONFIG = "retry.backoff.ms";
    public static final String METRICS_SAMPLE_WINDOW_MS_CONFIG = "metrics.sample.window.ms";
    public static final String METRICS_NUM_SAMPLES_CONFIG = "metrics.num.samples";
    public static final String METRICS_RECORDING_LEVEL_CONFIG = "metrics.recording.level";
    public static final String METRIC_REPORTER_CLASSES_CONFIG = "metric.reporters";
    public static final String CHECK_CRCS_CONFIG = "check.crcs";
    private static final String CHECK_CRCS_DOC = "Automatically check the CRC32 of the records consumed. This ensures no on-the-wire or on-disk corruption to the messages occurred. This check adds some overhead, so it may be disabled in cases seeking extreme performance.";
    public static final String KEY_DESERIALIZER_CLASS_CONFIG = "key.deserializer";
    public static final String KEY_DESERIALIZER_CLASS_DOC = "Deserializer class for key that implements the <code>org.apache.kafka.common.serialization.Deserializer</code> interface.";
    public static final String VALUE_DESERIALIZER_CLASS_CONFIG = "value.deserializer";
    public static final String VALUE_DESERIALIZER_CLASS_DOC = "Deserializer class for value that implements the <code>org.apache.kafka.common.serialization.Deserializer</code> interface.";
    public static final String SOCKET_CONNECTION_SETUP_TIMEOUT_MS_CONFIG = "socket.connection.setup.timeout.ms";
    public static final String SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS_CONFIG = "socket.connection.setup.timeout.max.ms";
    public static final String CONNECTIONS_MAX_IDLE_MS_CONFIG = "connections.max.idle.ms";
    public static final String REQUEST_TIMEOUT_MS_CONFIG = "request.timeout.ms";
    private static final String REQUEST_TIMEOUT_MS_DOC = "The configuration controls the maximum amount of time the client will wait for the response of a request. If the response is not received before the timeout elapses the client will resend the request if necessary or fail the request if retries are exhausted.";
    public static final String DEFAULT_API_TIMEOUT_MS_CONFIG = "default.api.timeout.ms";
    public static final String INTERCEPTOR_CLASSES_CONFIG = "interceptor.classes";
    public static final String INTERCEPTOR_CLASSES_DOC = "A list of classes to use as interceptors. Implementing the <code>org.apache.kafka.clients.consumer.ConsumerInterceptor</code> interface allows you to intercept (and possibly mutate) records received by the consumer. By default, there are no interceptors.";
    public static final String EXCLUDE_INTERNAL_TOPICS_CONFIG = "exclude.internal.topics";
    private static final String EXCLUDE_INTERNAL_TOPICS_DOC = "Whether internal topics matching a subscribed pattern should be excluded from the subscription. It is always possible to explicitly subscribe to an internal topic.";
    public static final boolean DEFAULT_EXCLUDE_INTERNAL_TOPICS = true;
    static final String LEAVE_GROUP_ON_CLOSE_CONFIG = "internal.leave.group.on.close";
    static final String THROW_ON_FETCH_STABLE_OFFSET_UNSUPPORTED = "internal.throw.on.fetch.stable.offset.unsupported";
    public static final String ISOLATION_LEVEL_CONFIG = "isolation.level";
    public static final String ISOLATION_LEVEL_DOC = "Controls how to read messages written transactionally. If set to <code>read_committed</code>, consumer.poll() will only return transactional messages which have been committed. If set to <code>read_uncommitted</code> (the default), consumer.poll() will return all messages, even transactional messages which have been aborted. Non-transactional messages will be returned unconditionally in either mode. <p>Messages will always be returned in offset order. Hence, in  <code>read_committed</code> mode, consumer.poll() will only return messages up to the last stable offset (LSO), which is the one less than the offset of the first open transaction. In particular any messages appearing after messages belonging to ongoing transactions will be withheld until the relevant transaction has been completed. As a result, <code>read_committed</code> consumers will not be able to read up to the high watermark when there are in flight transactions.</p><p> Further, when in <code>read_committed</code> the seekToEnd method will return the LSO";
    public static final String DEFAULT_ISOLATION_LEVEL;
    public static final String ALLOW_AUTO_CREATE_TOPICS_CONFIG = "allow.auto.create.topics";
    private static final String ALLOW_AUTO_CREATE_TOPICS_DOC = "Allow automatic topic creation on the broker when subscribing to or assigning a topic. A topic being subscribed to will be automatically created only if the broker allows for it using `auto.create.topics.enable` broker configuration. This configuration must be set to `false` when using brokers older than 0.11.0";
    public static final boolean DEFAULT_ALLOW_AUTO_CREATE_TOPICS = true;
    public static final String SECURITY_PROVIDERS_CONFIG = "security.providers";
    private static final String SECURITY_PROVIDERS_DOC = "A list of configurable creator classes each returning a provider implementing security algorithms. These classes should implement the <code>org.apache.kafka.common.security.auth.SecurityProviderCreator</code> interface.";
    private static final AtomicInteger CONSUMER_CLIENT_ID_SEQUENCE;

    protected Map<String, Object> postProcessParsedConfig(Map<String, Object> parsedValues) {
        CommonClientConfigs.warnIfDeprecatedDnsLookupValue(this);
        Map<String, Object> refinedConfigs = CommonClientConfigs.postProcessReconnectBackoffConfigs(this, parsedValues);
        this.maybeOverrideClientId(refinedConfigs);
        return refinedConfigs;
    }

    private void maybeOverrideClientId(Map<String, Object> configs) {
        String clientId = this.getString("client.id");
        if (clientId == null || clientId.isEmpty()) {
            String groupId = this.getString("group.id");
            String groupInstanceId = this.getString("group.instance.id");
            if (groupInstanceId != null) {
                JoinGroupRequest.validateGroupInstanceId(groupInstanceId);
            }

            String groupInstanceIdPart = groupInstanceId != null ? groupInstanceId : CONSUMER_CLIENT_ID_SEQUENCE.getAndIncrement() + "";
            String generatedClientId = String.format("consumer-%s-%s", groupId, groupInstanceIdPart);
            configs.put("client.id", generatedClientId);
        }

    }

    /** @deprecated */
    @Deprecated
    public static Map<String, Object> addDeserializerToConfig(Map<String, Object> configs, Deserializer<?> keyDeserializer, Deserializer<?> valueDeserializer) {
        return appendDeserializerToConfig(configs, keyDeserializer, valueDeserializer);
    }

    static Map<String, Object> appendDeserializerToConfig(Map<String, Object> configs, Deserializer<?> keyDeserializer, Deserializer<?> valueDeserializer) {
        Map<String, Object> newConfigs = new HashMap(configs);
        if (keyDeserializer != null) {
            newConfigs.put("key.deserializer", keyDeserializer.getClass());
        }

        if (valueDeserializer != null) {
            newConfigs.put("value.deserializer", valueDeserializer.getClass());
        }

        return newConfigs;
    }

    /** @deprecated */
    @Deprecated
    public static Properties addDeserializerToConfig(Properties properties, Deserializer<?> keyDeserializer, Deserializer<?> valueDeserializer) {
        Properties newProperties = new Properties();
        newProperties.putAll(properties);
        if (keyDeserializer != null) {
            newProperties.put("key.deserializer", keyDeserializer.getClass().getName());
        }

        if (valueDeserializer != null) {
            newProperties.put("value.deserializer", valueDeserializer.getClass().getName());
        }

        return newProperties;
    }

    boolean maybeOverrideEnableAutoCommit() {
        Optional<String> groupId = Optional.ofNullable(this.getString("group.id"));
        boolean enableAutoCommit = this.getBoolean("enable.auto.commit");
        if (!groupId.isPresent()) {
            if (!this.originals().containsKey("enable.auto.commit")) {
                enableAutoCommit = false;
            } else if (enableAutoCommit) {
                throw new InvalidConfigurationException("enable.auto.commit cannot be set to true when default group id (null) is used.");
            }
        }

        return enableAutoCommit;
    }

    public ConsumerConfig(Properties props) {
        super(CONFIG, props);
    }

    public ConsumerConfig(Map<String, Object> props) {
        super(CONFIG, props);
    }

    protected ConsumerConfig(Map<?, ?> props, boolean doLog) {
        super(CONFIG, props, doLog);
    }

    public static Set<String> configNames() {
        return CONFIG.names();
    }

    public static ConfigDef configDef() {
        return new ConfigDef(CONFIG);
    }

    public static void main(String[] args) {
        System.out.println(CONFIG.toHtml(4, (config) -> {
            return "consumerconfigs_" + config;
        }));
    }

    static {
        DEFAULT_ISOLATION_LEVEL = IsolationLevel.READ_UNCOMMITTED.toString().toLowerCase(Locale.ROOT);
        CONSUMER_CLIENT_ID_SEQUENCE = new AtomicInteger(1);
        CONFIG = (new ConfigDef()).define("bootstrap.servers", Type.LIST, Collections.emptyList(), new NonNullValidator(), Importance.HIGH, "A list of host/port pairs to use for establishing the initial connection to the Kafka cluster. The client will make use of all servers irrespective of which servers are specified here for bootstrapping&mdash;this list only impacts the initial hosts used to discover the full set of servers. This list should be in the form <code>host1:port1,host2:port2,...</code>. Since these servers are just used for the initial connection to discover the full cluster membership (which may change dynamically), this list need not contain the full set of servers (you may want more than one, though, in case a server is down).").define("client.dns.lookup", Type.STRING, ClientDnsLookup.USE_ALL_DNS_IPS.toString(), ValidString.in(new String[]{ClientDnsLookup.DEFAULT.toString(), ClientDnsLookup.USE_ALL_DNS_IPS.toString(), ClientDnsLookup.RESOLVE_CANONICAL_BOOTSTRAP_SERVERS_ONLY.toString()}), Importance.MEDIUM, "Controls how the client uses DNS lookups. If set to <code>use_all_dns_ips</code>, connect to each returned IP address in sequence until a successful connection is established. After a disconnection, the next IP is used. Once all IPs have been used once, the client resolves the IP(s) from the hostname again (both the JVM and the OS cache DNS name lookups, however). If set to <code>resolve_canonical_bootstrap_servers_only</code>, resolve each bootstrap address into a list of canonical names. After the bootstrap phase, this behaves the same as <code>use_all_dns_ips</code>. If set to <code>default</code> (deprecated), attempt to connect to the first IP address returned by the lookup, even if the lookup returns multiple IP addresses.").define("group.id", Type.STRING, (Object)null, Importance.HIGH, "A unique string that identifies the consumer group this consumer belongs to. This property is required if the consumer uses either the group management functionality by using <code>subscribe(topic)</code> or the Kafka-based offset management strategy.").define("group.instance.id", Type.STRING, (Object)null, Importance.MEDIUM, "A unique identifier of the consumer instance provided by the end user. Only non-empty strings are permitted. If set, the consumer is treated as a static member, which means that only one instance with this ID is allowed in the consumer group at any time. This can be used in combination with a larger session timeout to avoid group rebalances caused by transient unavailability (e.g. process restarts). If not set, the consumer will join the group as a dynamic member, which is the traditional behavior.").define("session.timeout.ms", Type.INT, 10000, Importance.HIGH, "The timeout used to detect client failures when using Kafka's group management facility. The client sends periodic heartbeats to indicate its liveness to the broker. If no heartbeats are received by the broker before the expiration of this session timeout, then the broker will remove this client from the group and initiate a rebalance. Note that the value must be in the allowable range as configured in the broker configuration by <code>group.min.session.timeout.ms</code> and <code>group.max.session.timeout.ms</code>.").define("heartbeat.interval.ms", Type.INT, 3000, Importance.HIGH, "The expected time between heartbeats to the consumer coordinator when using Kafka's group management facilities. Heartbeats are used to ensure that the consumer's session stays active and to facilitate rebalancing when new consumers join or leave the group. The value must be set lower than <code>session.timeout.ms</code>, but typically should be set no higher than 1/3 of that value. It can be adjusted even lower to control the expected time for normal rebalances.").define("partition.assignment.strategy", Type.LIST, Collections.singletonList(RangeAssignor.class), new NonNullValidator(), Importance.MEDIUM, "A list of class names or class types, ordered by preference, of supported partition assignment strategies that the client will use to distribute partition ownership amongst consumer instances when group management is used. Available options are:<ul><li><code>org.apache.kafka.clients.consumer.RangeAssignor</code>: The default assignor, which works on a per-topic basis.</li><li><code>org.apache.kafka.clients.consumer.RoundRobinAssignor</code>: Assigns partitions to consumers in a round-robin fashion.</li><li><code>org.apache.kafka.clients.consumer.StickyAssignor</code>: Guarantees an assignment that is maximally balanced while preserving as many existing partition assignments as possible.</li><li><code>org.apache.kafka.clients.consumer.CooperativeStickyAssignor</code>: Follows the same StickyAssignor logic, but allows for cooperative rebalancing.</li></ul><p>Implementing the <code>org.apache.kafka.clients.consumer.ConsumerPartitionAssignor</code> interface allows you to plug in a custom assignment strategy.").define("metadata.max.age.ms", Type.LONG, 300000, Range.atLeast(0), Importance.LOW, "The period of time in milliseconds after which we force a refresh of metadata even if we haven't seen any partition leadership changes to proactively discover any new brokers or partitions.").define("enable.auto.commit", Type.BOOLEAN, true, Importance.MEDIUM, "If true the consumer's offset will be periodically committed in the background.").define("auto.commit.interval.ms", Type.INT, 5000, Range.atLeast(0), Importance.LOW, "The frequency in milliseconds that the consumer offsets are auto-committed to Kafka if <code>enable.auto.commit</code> is set to <code>true</code>.").define("client.id", Type.STRING, "", Importance.LOW, "An id string to pass to the server when making requests. The purpose of this is to be able to track the source of requests beyond just ip/port by allowing a logical application name to be included in server-side request logging.").define("client.rack", Type.STRING, "", Importance.LOW, "A rack identifier for this client. This can be any string value which indicates where this client is physically located. It corresponds with the broker config 'broker.rack'").define("max.partition.fetch.bytes", Type.INT, 1048576, Range.atLeast(0), Importance.HIGH, "The maximum amount of data per-partition the server will return. Records are fetched in batches by the consumer. If the first record batch in the first non-empty partition of the fetch is larger than this limit, the batch will still be returned to ensure that the consumer can make progress. The maximum record batch size accepted by the broker is defined via <code>message.max.bytes</code> (broker config) or <code>max.message.bytes</code> (topic config). See fetch.max.bytes for limiting the consumer request size.").define("send.buffer.bytes", Type.INT, 131072, Range.atLeast(-1), Importance.MEDIUM, "The size of the TCP send buffer (SO_SNDBUF) to use when sending data. If the value is -1, the OS default will be used.").define("receive.buffer.bytes", Type.INT, 65536, Range.atLeast(-1), Importance.MEDIUM, "The size of the TCP receive buffer (SO_RCVBUF) to use when reading data. If the value is -1, the OS default will be used.").define("fetch.min.bytes", Type.INT, 1, Range.atLeast(0), Importance.HIGH, "The minimum amount of data the server should return for a fetch request. If insufficient data is available the request will wait for that much data to accumulate before answering the request. The default setting of 1 byte means that fetch requests are answered as soon as a single byte of data is available or the fetch request times out waiting for data to arrive. Setting this to something greater than 1 will cause the server to wait for larger amounts of data to accumulate which can improve server throughput a bit at the cost of some additional latency.").define("fetch.max.bytes", Type.INT, 52428800, Range.atLeast(0), Importance.MEDIUM, "The maximum amount of data the server should return for a fetch request. Records are fetched in batches by the consumer, and if the first record batch in the first non-empty partition of the fetch is larger than this value, the record batch will still be returned to ensure that the consumer can make progress. As such, this is not a absolute maximum. The maximum record batch size accepted by the broker is defined via <code>message.max.bytes</code> (broker config) or <code>max.message.bytes</code> (topic config). Note that the consumer performs multiple fetches in parallel.").define("fetch.max.wait.ms", Type.INT, 500, Range.atLeast(0), Importance.LOW, "The maximum amount of time the server will block before answering the fetch request if there isn't sufficient data to immediately satisfy the requirement given by fetch.min.bytes.").define("reconnect.backoff.ms", Type.LONG, 50L, Range.atLeast(0L), Importance.LOW, "The base amount of time to wait before attempting to reconnect to a given host. This avoids repeatedly connecting to a host in a tight loop. This backoff applies to all connection attempts by the client to a broker.").define("reconnect.backoff.max.ms", Type.LONG, 1000L, Range.atLeast(0L), Importance.LOW, "The maximum amount of time in milliseconds to wait when reconnecting to a broker that has repeatedly failed to connect. If provided, the backoff per host will increase exponentially for each consecutive connection failure, up to this maximum. After calculating the backoff increase, 20% random jitter is added to avoid connection storms.").define("retry.backoff.ms", Type.LONG, 100L, Range.atLeast(0L), Importance.LOW, "The amount of time to wait before attempting to retry a failed request to a given topic partition. This avoids repeatedly sending requests in a tight loop under some failure scenarios.").define("auto.offset.reset", Type.STRING, "latest", ValidString.in(new String[]{"latest", "earliest", "none"}), Importance.MEDIUM, "What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server (e.g. because that data has been deleted): <ul><li>earliest: automatically reset the offset to the earliest offset<li>latest: automatically reset the offset to the latest offset</li><li>none: throw exception to the consumer if no previous offset is found for the consumer's group</li><li>anything else: throw exception to the consumer.</li></ul>").define("check.crcs", Type.BOOLEAN, true, Importance.LOW, "Automatically check the CRC32 of the records consumed. This ensures no on-the-wire or on-disk corruption to the messages occurred. This check adds some overhead, so it may be disabled in cases seeking extreme performance.").define("metrics.sample.window.ms", Type.LONG, 30000, Range.atLeast(0), Importance.LOW, "The window of time a metrics sample is computed over.").define("metrics.num.samples", Type.INT, 2, Range.atLeast(1), Importance.LOW, "The number of samples maintained to compute metrics.").define("metrics.recording.level", Type.STRING, RecordingLevel.INFO.toString(), ValidString.in(new String[]{RecordingLevel.INFO.toString(), RecordingLevel.DEBUG.toString(), RecordingLevel.TRACE.toString()}), Importance.LOW, "The highest recording level for metrics.").define("metric.reporters", Type.LIST, Collections.emptyList(), new NonNullValidator(), Importance.LOW, "A list of classes to use as metrics reporters. Implementing the <code>org.apache.kafka.common.metrics.MetricsReporter</code> interface allows plugging in classes that will be notified of new metric creation. The JmxReporter is always included to register JMX statistics.").define("key.deserializer", Type.CLASS, Importance.HIGH, "Deserializer class for key that implements the <code>org.apache.kafka.common.serialization.Deserializer</code> interface.").define("value.deserializer", Type.CLASS, Importance.HIGH, "Deserializer class for value that implements the <code>org.apache.kafka.common.serialization.Deserializer</code> interface.").define("request.timeout.ms", Type.INT, 30000, Range.atLeast(0), Importance.MEDIUM, "The configuration controls the maximum amount of time the client will wait for the response of a request. If the response is not received before the timeout elapses the client will resend the request if necessary or fail the request if retries are exhausted.").define("default.api.timeout.ms", Type.INT, 60000, Range.atLeast(0), Importance.MEDIUM, "Specifies the timeout (in milliseconds) for client APIs. This configuration is used as the default timeout for all client operations that do not specify a <code>timeout</code> parameter.").define("socket.connection.setup.timeout.ms", Type.LONG, CommonClientConfigs.DEFAULT_SOCKET_CONNECTION_SETUP_TIMEOUT_MS, Importance.MEDIUM, "The amount of time the client will wait for the socket connection to be established. If the connection is not built before the timeout elapses, clients will close the socket channel.").define("socket.connection.setup.timeout.max.ms", Type.LONG, CommonClientConfigs.DEFAULT_SOCKET_CONNECTION_SETUP_TIMEOUT_MAX_MS, Importance.MEDIUM, "The maximum amount of time the client will wait for the socket connection to be established. The connection setup timeout will increase exponentially for each consecutive connection failure up to this maximum. To avoid connection storms, a randomization factor of 0.2 will be applied to the timeout resulting in a random range between 20% below and 20% above the computed value.").define("connections.max.idle.ms", Type.LONG, 540000, Importance.MEDIUM, "Close idle connections after the number of milliseconds specified by this config.").define("interceptor.classes", Type.LIST, Collections.emptyList(), new NonNullValidator(), Importance.LOW, "A list of classes to use as interceptors. Implementing the <code>org.apache.kafka.clients.consumer.ConsumerInterceptor</code> interface allows you to intercept (and possibly mutate) records received by the consumer. By default, there are no interceptors.").define("max.poll.records", Type.INT, 500, Range.atLeast(1), Importance.MEDIUM, "The maximum number of records returned in a single call to poll(). Note, that <code>max.poll.records</code> does not impact the underlying fetching behavior. The consumer will cache the records from each fetch request and returns them incrementally from each poll.").define("max.poll.interval.ms", Type.INT, 300000, Range.atLeast(1), Importance.MEDIUM, "The maximum delay between invocations of poll() when using consumer group management. This places an upper bound on the amount of time that the consumer can be idle before fetching more records. If poll() is not called before expiration of this timeout, then the consumer is considered failed and the group will rebalance in order to reassign the partitions to another member. For consumers using a non-null <code>group.instance.id</code> which reach this timeout, partitions will not be immediately reassigned. Instead, the consumer will stop sending heartbeats and partitions will be reassigned after expiration of <code>session.timeout.ms</code>. This mirrors the behavior of a static consumer which has shutdown.").define("exclude.internal.topics", Type.BOOLEAN, true, Importance.MEDIUM, "Whether internal topics matching a subscribed pattern should be excluded from the subscription. It is always possible to explicitly subscribe to an internal topic.").defineInternal("internal.leave.group.on.close", Type.BOOLEAN, true, Importance.LOW).defineInternal("internal.throw.on.fetch.stable.offset.unsupported", Type.BOOLEAN, false, Importance.LOW).define("isolation.level", Type.STRING, DEFAULT_ISOLATION_LEVEL, ValidString.in(new String[]{IsolationLevel.READ_COMMITTED.toString().toLowerCase(Locale.ROOT), IsolationLevel.READ_UNCOMMITTED.toString().toLowerCase(Locale.ROOT)}), Importance.MEDIUM, "Controls how to read messages written transactionally. If set to <code>read_committed</code>, consumer.poll() will only return transactional messages which have been committed. If set to <code>read_uncommitted</code> (the default), consumer.poll() will return all messages, even transactional messages which have been aborted. Non-transactional messages will be returned unconditionally in either mode. <p>Messages will always be returned in offset order. Hence, in  <code>read_committed</code> mode, consumer.poll() will only return messages up to the last stable offset (LSO), which is the one less than the offset of the first open transaction. In particular any messages appearing after messages belonging to ongoing transactions will be withheld until the relevant transaction has been completed. As a result, <code>read_committed</code> consumers will not be able to read up to the high watermark when there are in flight transactions.</p><p> Further, when in <code>read_committed</code> the seekToEnd method will return the LSO").define("allow.auto.create.topics", Type.BOOLEAN, true, Importance.MEDIUM, "Allow automatic topic creation on the broker when subscribing to or assigning a topic. A topic being subscribed to will be automatically created only if the broker allows for it using `auto.create.topics.enable` broker configuration. This configuration must be set to `false` when using brokers older than 0.11.0").define("security.providers", Type.STRING, (Object)null, Importance.LOW, "A list of configurable creator classes each returning a provider implementing security algorithms. These classes should implement the <code>org.apache.kafka.common.security.auth.SecurityProviderCreator</code> interface.").define("security.protocol", Type.STRING, "PLAINTEXT", Importance.MEDIUM, CommonClientConfigs.SECURITY_PROTOCOL_DOC).withClientSslSupport().withClientSaslSupport();
    }
}

