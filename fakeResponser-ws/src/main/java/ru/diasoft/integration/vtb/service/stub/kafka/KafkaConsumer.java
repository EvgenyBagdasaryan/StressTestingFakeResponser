package ru.diasoft.integration.vtb.service.stub.kafka;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.apache.kafka.clients.ApiVersions;
//import org.apache.kafka.clients.ClientUtils;
import org.apache.kafka.clients.GroupRebalanceConfig;
//import org.apache.kafka.clients.NetworkClient;
import org.apache.kafka.clients.GroupRebalanceConfig.ProtocolType;
import org.apache.kafka.clients.Metadata.LeaderAndEpoch;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.consumer.internals.ConsumerCoordinator;
import org.apache.kafka.clients.consumer.internals.ConsumerInterceptors;
import org.apache.kafka.clients.consumer.internals.ConsumerMetadata;
import org.apache.kafka.clients.consumer.internals.ConsumerNetworkClient;
import org.apache.kafka.clients.consumer.internals.Fetcher;
import org.apache.kafka.clients.consumer.internals.FetcherMetricsRegistry;
import org.apache.kafka.clients.consumer.internals.KafkaConsumerMetrics;
import org.apache.kafka.clients.consumer.internals.NoOpConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.internals.PartitionAssignorAdapter;
import org.apache.kafka.clients.consumer.internals.SubscriptionState;
import org.apache.kafka.clients.consumer.internals.SubscriptionState.FetchPosition;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.InvalidGroupIdException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.internals.ClusterResourceListeners;
import org.apache.kafka.common.metrics.JmxReporter;
import org.apache.kafka.common.metrics.KafkaMetricsContext;
import org.apache.kafka.common.metrics.MetricConfig;
import org.apache.kafka.common.metrics.Metrics;
import org.apache.kafka.common.metrics.MetricsContext;
import org.apache.kafka.common.metrics.MetricsReporter;
import org.apache.kafka.common.metrics.Sensor;
import org.apache.kafka.common.metrics.Sensor.RecordingLevel;
import org.apache.kafka.common.network.ChannelBuilder;
import org.apache.kafka.common.network.Selector;
import org.apache.kafka.common.requests.MetadataRequest.Builder;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.utils.AppInfoParser;
import org.apache.kafka.common.utils.LogContext;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.common.utils.Timer;
import org.apache.kafka.common.utils.Utils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class KafkaConsumer<K, V> implements Consumer<K, V> {
    private static final String CLIENT_ID_METRIC_TAG = "client-id";
    private static final long NO_CURRENT_THREAD = -1L;
    private static final String JMX_PREFIX = "kafka.consumer";
    static final long DEFAULT_CLOSE_TIMEOUT_MS = 30000L;
    final Metrics metrics;
    final KafkaConsumerMetrics kafkaConsumerMetrics;
    private Logger log;
    private final String clientId;
    private final Optional<String> groupId;
    private final ConsumerCoordinator coordinator;
    private final Deserializer<K> keyDeserializer;
    private final Deserializer<V> valueDeserializer;
    private final Fetcher<K, V> fetcher;
    private final ConsumerInterceptors<K, V> interceptors;
    private final Time time;
    private final ConsumerNetworkClient client;
    private final SubscriptionState subscriptions;
    private final ConsumerMetadata metadata;
    private final long retryBackoffMs;
    private final long requestTimeoutMs;
    private final int defaultApiTimeoutMs;
    private volatile boolean closed;
    private List<ConsumerPartitionAssignor> assignors;
    private final AtomicLong currentThread;
    private final AtomicInteger refcount;
    private boolean cachedSubscriptionHashAllFetchPositions;

    public KafkaConsumer(Map<String, Object> configs) {
        this((Map)configs, (Deserializer)null, (Deserializer)null);
    }

    public KafkaConsumer(Properties properties) {
        this((Properties)properties, (Deserializer)null, (Deserializer)null);
    }

    public KafkaConsumer(Properties properties, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer) {
        this(Utils.propsToMap(properties), keyDeserializer, valueDeserializer);
    }

    public KafkaConsumer(Map<String, Object> configs, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer) {
        this(new ConsumerConfig(ConsumerConfig.appendDeserializerToConfig(configs, keyDeserializer, valueDeserializer)), keyDeserializer, valueDeserializer);
    }

    KafkaConsumer(ConsumerConfig config, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer) {
        this.closed = false;
        this.currentThread = new AtomicLong(-1L);
        this.refcount = new AtomicInteger(0);
        //this.log.debug("Start KafkaConsumer for fakeResponcer");
        try {
            GroupRebalanceConfig groupRebalanceConfig = new GroupRebalanceConfig(config, ProtocolType.CONSUMER);
            this.groupId = Optional.ofNullable(groupRebalanceConfig.groupId);
            this.clientId = config.getString("client.id");
            LogContext logContext;
            if (groupRebalanceConfig.groupInstanceId.isPresent()) {
                logContext = new LogContext("[Consumer instanceId=" + (String)groupRebalanceConfig.groupInstanceId.get() + ", clientId=" + this.clientId + ", groupId=" + (String)this.groupId.orElse("null") + "] ");
            } else {
                logContext = new LogContext("[Consumer clientId=" + this.clientId + ", groupId=" + (String)this.groupId.orElse("null") + "] ");
            }

            this.log = LogManager.getLogger(this.getClass());//logContext.logger(this.getClass());
            boolean enableAutoCommit = config.maybeOverrideEnableAutoCommit();
            this.groupId.ifPresent((groupIdStr) -> {
                if (groupIdStr.isEmpty()) {
                    this.log.warn("Support for using the empty group id by consumers is deprecated and will be removed in the next major release.");
                }

            });
            this.log.debug("Initializing the Kafka consumer for fakeResponcer");
            this.requestTimeoutMs = (long)config.getInt("request.timeout.ms");
            this.defaultApiTimeoutMs = config.getInt("default.api.timeout.ms");
            this.time = Time.SYSTEM;
            this.metrics = buildMetrics(config, this.time, this.clientId);
            this.retryBackoffMs = config.getLong("retry.backoff.ms");
            List<ConsumerInterceptor> interceptorList = config.getConfiguredInstances("interceptor.classes", ConsumerInterceptor.class, Collections.singletonMap("client.id", this.clientId));
            this.interceptors = new ConsumerInterceptors(interceptorList);
            if (keyDeserializer == null) {
                this.keyDeserializer = (Deserializer)config.getConfiguredInstance("key.deserializer", Deserializer.class);
                this.keyDeserializer.configure(config.originals(Collections.singletonMap("client.id", this.clientId)), true);
            } else {
                config.ignore("key.deserializer");
                this.keyDeserializer = keyDeserializer;
            }

            if (valueDeserializer == null) {
                this.valueDeserializer = (Deserializer)config.getConfiguredInstance("value.deserializer", Deserializer.class);
                this.valueDeserializer.configure(config.originals(Collections.singletonMap("client.id", this.clientId)), false);
            } else {
                config.ignore("value.deserializer");
                this.valueDeserializer = valueDeserializer;
            }

            OffsetResetStrategy offsetResetStrategy = OffsetResetStrategy.valueOf(config.getString("auto.offset.reset").toUpperCase(Locale.ROOT));
            this.subscriptions = new SubscriptionState(logContext, offsetResetStrategy);
            ClusterResourceListeners clusterResourceListeners = this.configureClusterResourceListeners(keyDeserializer, valueDeserializer, this.metrics.reporters(), interceptorList);
            this.metadata = new ConsumerMetadata(this.retryBackoffMs, config.getLong("metadata.max.age.ms"), !config.getBoolean("exclude.internal.topics"), config.getBoolean("allow.auto.create.topics"), this.subscriptions, logContext, clusterResourceListeners);
            List<InetSocketAddress> addresses = ClientUtils.parseAndValidateAddresses(config.getList("bootstrap.servers"), config.getString("client.dns.lookup"));
            this.metadata.bootstrap(addresses);
            String metricGrpPrefix = "consumer";
            FetcherMetricsRegistry metricsRegistry = new FetcherMetricsRegistry(Collections.singleton("client-id"), metricGrpPrefix);
            ChannelBuilder channelBuilder = ClientUtils.createChannelBuilder(config, this.time, logContext);
            IsolationLevel isolationLevel = IsolationLevel.valueOf(config.getString("isolation.level").toUpperCase(Locale.ROOT));
            Sensor throttleTimeSensor = Fetcher.throttleTimeSensor(this.metrics, metricsRegistry);
            int heartbeatIntervalMs = config.getInt("heartbeat.interval.ms");
            ApiVersions apiVersions = new ApiVersions();
            NetworkClient netClient = new NetworkClient(
                    new Selector(config.getLong("connections.max.idle.ms"), this.metrics, this.time, metricGrpPrefix, channelBuilder, logContext),
                    this.metadata,
                    this.clientId,
                    100,
                    config.getLong("reconnect.backoff.ms"),
                    config.getLong("reconnect.backoff.max.ms"),
                    config.getInt("send.buffer.bytes"),
                    config.getInt("receive.buffer.bytes"),
                    config.getInt("request.timeout.ms"),
                    config.getLong("socket.connection.setup.timeout.ms"),
                    config.getLong("socket.connection.setup.timeout.max.ms"),
                    //ClientDnsLookup.forConfig(config.getString("client.dns.lookup")),
                    this.time,
                    true,
                    apiVersions,
                    throttleTimeSensor,
                    logContext
            );
            this.client = new ConsumerNetworkClient(logContext, netClient, this.metadata, this.time, this.retryBackoffMs, config.getInt("request.timeout.ms"), heartbeatIntervalMs);
            this.assignors = PartitionAssignorAdapter.getAssignorInstances(config.getList("partition.assignment.strategy"), config.originals(Collections.singletonMap("client.id", this.clientId)));
            this.coordinator = !this.groupId.isPresent() ? null : new ConsumerCoordinator(groupRebalanceConfig, logContext, this.client, this.assignors, this.metadata, this.subscriptions, this.metrics, metricGrpPrefix, this.time, enableAutoCommit, config.getInt("auto.commit.interval.ms"), this.interceptors, config.getBoolean("internal.throw.on.fetch.stable.offset.unsupported"));
            this.fetcher = new Fetcher(logContext, this.client, config.getInt("fetch.min.bytes"), config.getInt("fetch.max.bytes"), config.getInt("fetch.max.wait.ms"), config.getInt("max.partition.fetch.bytes"), config.getInt("max.poll.records"), config.getBoolean("check.crcs"), config.getString("client.rack"), this.keyDeserializer, this.valueDeserializer, this.metadata, this.subscriptions, this.metrics, metricsRegistry, this.time, this.retryBackoffMs, this.requestTimeoutMs, isolationLevel, apiVersions);
            this.kafkaConsumerMetrics = new KafkaConsumerMetrics(this.metrics, metricGrpPrefix);
            config.logUnused();
            AppInfoParser.registerAppInfo("kafka.consumer", this.clientId, this.metrics, this.time.milliseconds());
            this.log.debug("Kafka consumer initialized for fake");
        } catch (Throwable var19) {
            if (this.log != null) {
                this.close(0L, true);
            }

            throw new KafkaException("Failed to construct kafka consumer", var19);
        }
    }

    KafkaConsumer(LogContext logContext, String clientId, ConsumerCoordinator coordinator, Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer, Fetcher<K, V> fetcher, ConsumerInterceptors<K, V> interceptors, Time time, ConsumerNetworkClient client, Metrics metrics, SubscriptionState subscriptions, ConsumerMetadata metadata, long retryBackoffMs, long requestTimeoutMs, int defaultApiTimeoutMs, List<ConsumerPartitionAssignor> assignors, String groupId) {
        this.closed = false;
        this.currentThread = new AtomicLong(-1L);
        this.refcount = new AtomicInteger(0);
        this.log = LogManager.getLogger(this.getClass());//logContext.logger(this.getClass());
        this.clientId = clientId;
        this.coordinator = coordinator;
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;
        this.fetcher = fetcher;
        this.interceptors = (ConsumerInterceptors)Objects.requireNonNull(interceptors);
        this.time = time;
        this.client = client;
        this.metrics = metrics;
        this.subscriptions = subscriptions;
        this.metadata = metadata;
        this.retryBackoffMs = retryBackoffMs;
        this.requestTimeoutMs = requestTimeoutMs;
        this.defaultApiTimeoutMs = defaultApiTimeoutMs;
        this.assignors = assignors;
        this.groupId = Optional.ofNullable(groupId);
        this.kafkaConsumerMetrics = new KafkaConsumerMetrics(metrics, "consumer");
    }

    private static Metrics buildMetrics(ConsumerConfig config, Time time, String clientId) {
        Map<String, String> metricsTags = Collections.singletonMap("client-id", clientId);
        MetricConfig metricConfig = (new MetricConfig()).samples(config.getInt("metrics.num.samples")).timeWindow(config.getLong("metrics.sample.window.ms"), TimeUnit.MILLISECONDS).recordLevel(RecordingLevel.forName(config.getString("metrics.recording.level"))).tags(metricsTags);
        List<MetricsReporter> reporters = config.getConfiguredInstances("metric.reporters", MetricsReporter.class, Collections.singletonMap("client.id", clientId));
        JmxReporter jmxReporter = new JmxReporter();
        jmxReporter.configure(config.originals(Collections.singletonMap("client.id", clientId)));
        reporters.add(jmxReporter);
        MetricsContext metricsContext = new KafkaMetricsContext("kafka.consumer", config.originalsWithPrefix("metrics.context."));
        return new Metrics(metricConfig, reporters, time, metricsContext);
    }

    public Set<TopicPartition> assignment() {
        this.acquireAndEnsureOpen();

        Set var1;
        try {
            var1 = Collections.unmodifiableSet(this.subscriptions.assignedPartitions());
        } finally {
            this.release();
        }

        return var1;
    }

    public Set<String> subscription() {
        this.acquireAndEnsureOpen();

        Set var1;
        try {
            var1 = Collections.unmodifiableSet(new HashSet(this.subscriptions.subscription()));
        } finally {
            this.release();
        }

        return var1;
    }

    public void subscribe(Collection<String> topics, ConsumerRebalanceListener listener) {
        this.acquireAndEnsureOpen();

        try {
            this.maybeThrowInvalidGroupIdException();
            if (topics == null) {
                throw new IllegalArgumentException("Topic collection to subscribe to cannot be null");
            }

            if (topics.isEmpty()) {
                this.unsubscribe();
            } else {
                Iterator var3 = topics.iterator();

                String topic;
                do {
                    if (!var3.hasNext()) {
                        this.throwIfNoAssignorsConfigured();
                        this.fetcher.clearBufferedDataForUnassignedTopics(topics);
                        this.log.info("Subscribed to topic(s): " +  Utils.join(topics, ", "));
                        if (this.subscriptions.subscribe(new HashSet(topics), listener)) {
                            this.metadata.requestUpdateForNewTopics();
                        }

                        return;
                    }

                    topic = (String)var3.next();
                } while(topic != null && !topic.trim().isEmpty());

                throw new IllegalArgumentException("Topic collection to subscribe to cannot contain null or empty topic");
            }
        } finally {
            this.release();
        }

    }

    public void subscribe(Collection<String> topics) {
        this.subscribe((Collection)topics, new NoOpConsumerRebalanceListener());
    }

    public void subscribe(Pattern pattern, ConsumerRebalanceListener listener) {
        this.maybeThrowInvalidGroupIdException();
        if (pattern != null && !pattern.toString().equals("")) {
            this.acquireAndEnsureOpen();

            try {
                this.throwIfNoAssignorsConfigured();
                this.log.info("Subscribed to pattern: " + pattern);
                this.subscriptions.subscribe(pattern, listener);
                this.coordinator.updatePatternSubscription(this.metadata.fetch());
                this.metadata.requestUpdateForNewTopics();
            } finally {
                this.release();
            }

        } else {
            throw new IllegalArgumentException("Topic pattern to subscribe to cannot be " + (pattern == null ? "null" : "empty"));
        }
    }

    public void subscribe(Pattern pattern) {
        this.subscribe((Pattern)pattern, new NoOpConsumerRebalanceListener());
    }

    public void unsubscribe() {
        this.acquireAndEnsureOpen();

        try {
            this.fetcher.clearBufferedDataForUnassignedPartitions(Collections.emptySet());
            if (this.coordinator != null) {
                this.coordinator.onLeavePrepare();
                this.coordinator.maybeLeaveGroup("the consumer unsubscribed from all topics");
            }

            this.subscriptions.unsubscribe();
            this.log.info("Unsubscribed all topics or patterns and assigned partitions");
        } finally {
            this.release();
        }

    }

    public void assign(Collection<TopicPartition> partitions) {
        this.acquireAndEnsureOpen();

        try {
            if (partitions == null) {
                throw new IllegalArgumentException("Topic partition collection to assign to cannot be null");
            }

            if (partitions.isEmpty()) {
                this.unsubscribe();
            } else {
                Iterator var2 = partitions.iterator();

                while(true) {
                    if (var2.hasNext()) {
                        TopicPartition tp = (TopicPartition)var2.next();
                        String topic = tp != null ? tp.topic() : null;
                        if (topic != null && !topic.trim().isEmpty()) {
                            continue;
                        }

                        throw new IllegalArgumentException("Topic partitions to assign to cannot have null or empty topic");
                    }

                    this.fetcher.clearBufferedDataForUnassignedPartitions(partitions);
                    if (this.coordinator != null) {
                        this.coordinator.maybeAutoCommitOffsetsAsync(this.time.milliseconds());
                    }

                    this.log.info("Subscribed to partition(s): " + Utils.join(partitions, ", "));
                    if (this.subscriptions.assignFromUser(new HashSet(partitions))) {
                        this.metadata.requestUpdateForNewTopics();
                    }
                    break;
                }
            }
        } finally {
            this.release();
        }

    }

    /** @deprecated */
    @Deprecated
    public ConsumerRecords<K, V> poll(long timeoutMs) {
        return this.poll(this.time.timer(timeoutMs), false);
    }

    public ConsumerRecords<K, V> poll(Duration timeout) {
        return this.poll(this.time.timer(timeout), true);
    }

    private ConsumerRecords<K, V> poll(Timer timer, boolean includeMetadataInTimeout) {
        this.acquireAndEnsureOpen();

        try {
            this.kafkaConsumerMetrics.recordPollStart(timer.currentTimeMs());
            if (this.subscriptions.hasNoSubscriptionOrUserAssignment()) {
                throw new IllegalStateException("Consumer is not subscribed to any topics or assigned any partitions");
            } else {
                do {
                    this.client.maybeTriggerWakeup();
                    if (includeMetadataInTimeout) {
                        this.updateAssignmentMetadataIfNeeded(timer, false);
                    } else {
                        while(!this.updateAssignmentMetadataIfNeeded(this.time.timer(9223372036854775807L), true)) {
                            this.log.warn("Still waiting for metadata");
                        }
                    }

                    Map<TopicPartition, List<ConsumerRecord<K, V>>> records = this.pollForFetches(timer);
                    if (!records.isEmpty()) {
                        if (this.fetcher.sendFetches() > 0 || this.client.hasPendingRequests()) {
                            this.client.transmitSends();
                        }

                        ConsumerRecords var4 = this.interceptors.onConsume(new ConsumerRecords(records));
                        return var4;
                    }
                } while(timer.notExpired());

                ConsumerRecords var8 = ConsumerRecords.empty();
                return var8;
            }
        } finally {
            this.release();
            this.kafkaConsumerMetrics.recordPollEnd(timer.currentTimeMs());
        }
    }

    boolean updateAssignmentMetadataIfNeeded(Timer timer, boolean waitForJoinGroup) {
        return this.coordinator != null && !this.coordinator.poll(timer, waitForJoinGroup) ? false : this.updateFetchPositions(timer);
    }

    private Map<TopicPartition, List<ConsumerRecord<K, V>>> pollForFetches(Timer timer) {
        long pollTimeout = this.coordinator == null ? timer.remainingMs() : Math.min(this.coordinator.timeToNextPoll(timer.currentTimeMs()), timer.remainingMs());
        Map<TopicPartition, List<ConsumerRecord<K, V>>> records = this.fetcher.fetchedRecords();
        if (!records.isEmpty()) {
            return records;
        } else {
            this.fetcher.sendFetches();
            if (!this.cachedSubscriptionHashAllFetchPositions && pollTimeout > this.retryBackoffMs) {
                pollTimeout = this.retryBackoffMs;
            }

            this.log.trace("Polling for fetches with timeout " + pollTimeout);
            Timer pollTimer = this.time.timer(pollTimeout);
            this.client.poll(pollTimer, () -> {
                return !this.fetcher.hasAvailableFetches();
            });
            timer.update(pollTimer.currentTimeMs());
            return this.fetcher.fetchedRecords();
        }
    }

    public void commitSync() {
        this.commitSync(Duration.ofMillis((long)this.defaultApiTimeoutMs));
    }

    public void commitSync(Duration timeout) {
        this.commitSync(this.subscriptions.allConsumed(), timeout);
    }

    public void commitSync(Map<TopicPartition, OffsetAndMetadata> offsets) {
        this.commitSync(offsets, Duration.ofMillis((long)this.defaultApiTimeoutMs));
    }

    public void commitSync(Map<TopicPartition, OffsetAndMetadata> offsets, Duration timeout) {
        this.acquireAndEnsureOpen();

        try {
            this.maybeThrowInvalidGroupIdException();
            offsets.forEach(this::updateLastSeenEpochIfNewer);
            if (!this.coordinator.commitOffsetsSync(new HashMap(offsets), this.time.timer(timeout))) {
                throw new TimeoutException("Timeout of " + timeout.toMillis() + "ms expired before successfully committing offsets " + offsets);
            }
        } finally {
            this.release();
        }

    }

    public void commitAsync() {
        this.commitAsync((OffsetCommitCallback)null);
    }

    public void commitAsync(OffsetCommitCallback callback) {
        this.commitAsync(this.subscriptions.allConsumed(), callback);
    }

    public void commitAsync(Map<TopicPartition, OffsetAndMetadata> offsets, OffsetCommitCallback callback) {
        this.acquireAndEnsureOpen();

        try {
            this.maybeThrowInvalidGroupIdException();
            this.log.debug("Committing offsets: " + offsets);
            offsets.forEach(this::updateLastSeenEpochIfNewer);
            this.coordinator.commitOffsetsAsync(new HashMap(offsets), callback);
        } finally {
            this.release();
        }

    }

    public void seek(TopicPartition partition, long offset) {
        if (offset < 0L) {
            throw new IllegalArgumentException("seek offset must not be a negative number");
        } else {
            this.acquireAndEnsureOpen();

            try {
                this.log.info("Seeking to offset " + offset + " for partition " + partition);
                FetchPosition newPosition = new FetchPosition(offset, Optional.empty(), this.metadata.currentLeader(partition));
                this.subscriptions.seekUnvalidated(partition, newPosition);
            } finally {
                this.release();
            }

        }
    }

    public void seek(TopicPartition partition, OffsetAndMetadata offsetAndMetadata) {
        long offset = offsetAndMetadata.offset();
        if (offset < 0L) {
            throw new IllegalArgumentException("seek offset must not be a negative number");
        } else {
            this.acquireAndEnsureOpen();

            try {
                if (offsetAndMetadata.leaderEpoch().isPresent()) {
                    this.log.info("Seeking to offset {} for partition {} with epoch {}" + new Object[]{offset, partition, offsetAndMetadata.leaderEpoch().get()});
                } else {
                    this.log.info("Seeking to offset " + offset + " for partition " + partition);
                }

                LeaderAndEpoch currentLeaderAndEpoch = this.metadata.currentLeader(partition);
                FetchPosition newPosition = new FetchPosition(offsetAndMetadata.offset(), offsetAndMetadata.leaderEpoch(), currentLeaderAndEpoch);
                this.updateLastSeenEpochIfNewer(partition, offsetAndMetadata);
                this.subscriptions.seekUnvalidated(partition, newPosition);
            } finally {
                this.release();
            }

        }
    }

    public void seekToBeginning(Collection<TopicPartition> partitions) {
        if (partitions == null) {
            throw new IllegalArgumentException("Partitions collection cannot be null");
        } else {
            this.acquireAndEnsureOpen();

            try {
                Collection<TopicPartition> parts = partitions.size() == 0 ? this.subscriptions.assignedPartitions() : partitions;
                this.subscriptions.requestOffsetReset((Collection)parts, OffsetResetStrategy.EARLIEST);
            } finally {
                this.release();
            }

        }
    }

    public void seekToEnd(Collection<TopicPartition> partitions) {
        if (partitions == null) {
            throw new IllegalArgumentException("Partitions collection cannot be null");
        } else {
            this.acquireAndEnsureOpen();

            try {
                Collection<TopicPartition> parts = partitions.size() == 0 ? this.subscriptions.assignedPartitions() : partitions;
                this.subscriptions.requestOffsetReset((Collection)parts, OffsetResetStrategy.LATEST);
            } finally {
                this.release();
            }

        }
    }

    public long position(TopicPartition partition) {
        return this.position(partition, Duration.ofMillis((long)this.defaultApiTimeoutMs));
    }

    public long position(TopicPartition partition, Duration timeout) {
        this.acquireAndEnsureOpen();

        try {
            if (!this.subscriptions.isAssigned(partition)) {
                throw new IllegalStateException("You can only check the position for partitions assigned to this consumer.");
            } else {
                Timer timer = this.time.timer(timeout);

                do {
                    FetchPosition position = this.subscriptions.validPosition(partition);
                    if (position != null) {
                        long var5 = position.offset;
                        return var5;
                    }

                    this.updateFetchPositions(timer);
                    this.client.poll(timer);
                } while(timer.notExpired());

                throw new TimeoutException("Timeout of " + timeout.toMillis() + "ms expired before the position for partition " + partition + " could be determined");
            }
        } finally {
            this.release();
        }
    }

    /** @deprecated */
    @Deprecated
    public OffsetAndMetadata committed(TopicPartition partition) {
        return this.committed(partition, Duration.ofMillis((long)this.defaultApiTimeoutMs));
    }

    /** @deprecated */
    @Deprecated
    public OffsetAndMetadata committed(TopicPartition partition, Duration timeout) {
        return (OffsetAndMetadata)this.committed(Collections.singleton(partition), timeout).get(partition);
    }

    public Map<TopicPartition, OffsetAndMetadata> committed(Set<TopicPartition> partitions) {
        return this.committed(partitions, Duration.ofMillis((long)this.defaultApiTimeoutMs));
    }

    public Map<TopicPartition, OffsetAndMetadata> committed(Set<TopicPartition> partitions, Duration timeout) {
        this.acquireAndEnsureOpen();

        Map var4;
        try {
            this.maybeThrowInvalidGroupIdException();
            Map<TopicPartition, OffsetAndMetadata> offsets = this.coordinator.fetchCommittedOffsets(partitions, this.time.timer(timeout));
            if (offsets == null) {
                throw new TimeoutException("Timeout of " + timeout.toMillis() + "ms expired before the last committed offset for partitions " + partitions + " could be determined. Try tuning default.api.timeout.ms larger to relax the threshold.");
            }

            offsets.forEach(this::updateLastSeenEpochIfNewer);
            var4 = offsets;
        } finally {
            this.release();
        }

        return var4;
    }

    public Map<MetricName, ? extends Metric> metrics() {
        return Collections.unmodifiableMap(this.metrics.metrics());
    }

    public List<PartitionInfo> partitionsFor(String topic) {
        return this.partitionsFor(topic, Duration.ofMillis((long)this.defaultApiTimeoutMs));
    }

    public List<PartitionInfo> partitionsFor(String topic, Duration timeout) {
        this.acquireAndEnsureOpen();

        List var5;
        try {
            Cluster cluster = this.metadata.fetch();
            List<PartitionInfo> parts = cluster.partitionsForTopic(topic);
            if (parts.isEmpty()) {
                Timer timer = this.time.timer(timeout);
                Map<String, List<PartitionInfo>> topicMetadata = this.fetcher.getTopicMetadata(new Builder(Collections.singletonList(topic), this.metadata.allowAutoTopicCreation()), timer);
                List var7 = (List)topicMetadata.get(topic);
                return var7;
            }

            var5 = parts;
        } finally {
            this.release();
        }

        return var5;
    }

    public Map<String, List<PartitionInfo>> listTopics() {
        return this.listTopics(Duration.ofMillis((long)this.defaultApiTimeoutMs));
    }

    public Map<String, List<PartitionInfo>> listTopics(Duration timeout) {
        this.acquireAndEnsureOpen();

        Map var2;
        try {
            var2 = this.fetcher.getAllTopicMetadata(this.time.timer(timeout));
        } finally {
            this.release();
        }

        return var2;
    }

    public void pause(Collection<TopicPartition> partitions) {
        this.acquireAndEnsureOpen();

        try {
            this.log.debug("Pausing partitions " + partitions);
            Iterator var2 = partitions.iterator();

            while(var2.hasNext()) {
                TopicPartition partition = (TopicPartition)var2.next();
                this.subscriptions.pause(partition);
            }
        } finally {
            this.release();
        }

    }

    public void resume(Collection<TopicPartition> partitions) {
        this.acquireAndEnsureOpen();

        try {
            this.log.debug("Resuming partitions " + partitions);
            Iterator var2 = partitions.iterator();

            while(var2.hasNext()) {
                TopicPartition partition = (TopicPartition)var2.next();
                this.subscriptions.resume(partition);
            }
        } finally {
            this.release();
        }

    }

    public Set<TopicPartition> paused() {
        this.acquireAndEnsureOpen();

        Set var1;
        try {
            var1 = Collections.unmodifiableSet(this.subscriptions.pausedPartitions());
        } finally {
            this.release();
        }

        return var1;
    }

    public Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes(Map<TopicPartition, Long> timestampsToSearch) {
        return this.offsetsForTimes(timestampsToSearch, Duration.ofMillis((long)this.defaultApiTimeoutMs));
    }

    public Map<TopicPartition, OffsetAndTimestamp> offsetsForTimes(Map<TopicPartition, Long> timestampsToSearch, Duration timeout) {
        this.acquireAndEnsureOpen();

        try {
            Iterator var3 = timestampsToSearch.entrySet().iterator();

            Entry entry;
            do {
                if (!var3.hasNext()) {
                    Map var8 = this.fetcher.offsetsForTimes(timestampsToSearch, this.time.timer(timeout));
                    return var8;
                }

                entry = (Entry)var3.next();
            } while((Long)entry.getValue() >= 0L);

            throw new IllegalArgumentException("The target time for partition " + entry.getKey() + " is " + entry.getValue() + ". The target time cannot be negative.");
        } finally {
            this.release();
        }
    }

    public Map<TopicPartition, Long> beginningOffsets(Collection<TopicPartition> partitions) {
        return this.beginningOffsets(partitions, Duration.ofMillis((long)this.defaultApiTimeoutMs));
    }

    public Map<TopicPartition, Long> beginningOffsets(Collection<TopicPartition> partitions, Duration timeout) {
        this.acquireAndEnsureOpen();

        Map var3;
        try {
            var3 = this.fetcher.beginningOffsets(partitions, this.time.timer(timeout));
        } finally {
            this.release();
        }

        return var3;
    }

    public Map<TopicPartition, Long> endOffsets(Collection<TopicPartition> partitions) {
        return this.endOffsets(partitions, Duration.ofMillis(this.requestTimeoutMs));
    }

    public Map<TopicPartition, Long> endOffsets(Collection<TopicPartition> partitions, Duration timeout) {
        this.acquireAndEnsureOpen();

        Map var3;
        try {
            var3 = this.fetcher.endOffsets(partitions, this.time.timer(timeout));
        } finally {
            this.release();
        }

        return var3;
    }

    public ConsumerGroupMetadata groupMetadata() {
        this.acquireAndEnsureOpen();

        ConsumerGroupMetadata var1;
        try {
            this.maybeThrowInvalidGroupIdException();
            var1 = this.coordinator.groupMetadata();
        } finally {
            this.release();
        }

        return var1;
    }

    public void enforceRebalance() {
        this.acquireAndEnsureOpen();

        try {
            if (this.coordinator == null) {
                throw new IllegalStateException("Tried to force a rebalance but consumer does not have a group.");
            }

            this.coordinator.requestRejoin();
        } finally {
            this.release();
        }

    }

    public void close() {
        this.close(Duration.ofMillis(30000L));
    }

    /** @deprecated */
    @Deprecated
    public void close(long timeout, TimeUnit timeUnit) {
        this.close(Duration.ofMillis(timeUnit.toMillis(timeout)));
    }

    public void close(Duration timeout) {
        if (timeout.toMillis() < 0L) {
            throw new IllegalArgumentException("The timeout cannot be negative.");
        } else {
            this.acquire();

            try {
                if (!this.closed) {
                    this.close(timeout.toMillis(), false);
                }
            } finally {
                this.closed = true;
                this.release();
            }

        }
    }

    public void wakeup() {
        this.client.wakeup();
    }

    private ClusterResourceListeners configureClusterResourceListeners(Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer, List<?>... candidateLists) {
        ClusterResourceListeners clusterResourceListeners = new ClusterResourceListeners();
        List[] var5 = candidateLists;
        int var6 = candidateLists.length;

        for(int var7 = 0; var7 < var6; ++var7) {
            List<?> candidateList = var5[var7];
            clusterResourceListeners.maybeAddAll(candidateList);
        }

        clusterResourceListeners.maybeAdd(keyDeserializer);
        clusterResourceListeners.maybeAdd(valueDeserializer);
        return clusterResourceListeners;
    }

    private void close(long timeoutMs, boolean swallowException) {
        this.log.trace("Closing the Kafka consumer");
        AtomicReference firstException = new AtomicReference();

        try {
            if (this.coordinator != null) {
                this.coordinator.close(this.time.timer(Math.min(timeoutMs, this.requestTimeoutMs)));
            }
        } catch (Throwable var6) {
            firstException.compareAndSet((Object)null, var6);
            this.log.error("Failed to close coordinator", var6);
        }

        Utils.closeQuietly(this.fetcher, "fetcher", firstException);
        Utils.closeQuietly(this.interceptors, "consumer interceptors", firstException);
        Utils.closeQuietly(this.kafkaConsumerMetrics, "kafka consumer metrics", firstException);
        Utils.closeQuietly(this.metrics, "consumer metrics", firstException);
        Utils.closeQuietly(this.client, "consumer network client", firstException);
        Utils.closeQuietly(this.keyDeserializer, "consumer key deserializer", firstException);
        Utils.closeQuietly(this.valueDeserializer, "consumer value deserializer", firstException);
        AppInfoParser.unregisterAppInfo("kafka.consumer", this.clientId, this.metrics);
        this.log.debug("Kafka consumer has been closed");
        Throwable exception = (Throwable)firstException.get();
        if (exception != null && !swallowException) {
            if (exception instanceof InterruptException) {
                throw (InterruptException)exception;
            } else {
                throw new KafkaException("Failed to close kafka consumer", exception);
            }
        }
    }

    private boolean updateFetchPositions(Timer timer) {
        this.fetcher.validateOffsetsIfNeeded();
        this.cachedSubscriptionHashAllFetchPositions = this.subscriptions.hasAllFetchPositions();
        if (this.cachedSubscriptionHashAllFetchPositions) {
            return true;
        } else if (this.coordinator != null && !this.coordinator.refreshCommittedOffsetsIfNeeded(timer)) {
            return false;
        } else {
            this.subscriptions.resetInitializingPositions();
            this.fetcher.resetOffsetsIfNeeded();
            return true;
        }
    }

    private void acquireAndEnsureOpen() {
        this.acquire();
        if (this.closed) {
            this.release();
            throw new IllegalStateException("This consumer has already been closed.");
        }
    }

    private void acquire() {
        long threadId = Thread.currentThread().getId();
        if (threadId != this.currentThread.get() && !this.currentThread.compareAndSet(-1L, threadId)) {
            throw new ConcurrentModificationException("KafkaConsumer is not safe for multi-threaded access");
        } else {
            this.refcount.incrementAndGet();
        }
    }

    private void release() {
        if (this.refcount.decrementAndGet() == 0) {
            this.currentThread.set(-1L);
        }

    }

    private void throwIfNoAssignorsConfigured() {
        if (this.assignors.isEmpty()) {
            throw new IllegalStateException("Must configure at least one partition assigner class name to partition.assignment.strategy configuration property");
        }
    }

    private void maybeThrowInvalidGroupIdException() {
        if (!this.groupId.isPresent()) {
            throw new InvalidGroupIdException("To use the group management or offset commit APIs, you must provide a valid group.id in the consumer configuration.");
        }
    }

    private void updateLastSeenEpochIfNewer(TopicPartition topicPartition, OffsetAndMetadata offsetAndMetadata) {
        if (offsetAndMetadata != null) {
            offsetAndMetadata.leaderEpoch().ifPresent((epoch) -> {
                this.metadata.updateLastSeenEpochIfNewer(topicPartition, epoch);
            });
        }

    }

    String getClientId() {
        return this.clientId;
    }

    boolean updateAssignmentMetadataIfNeeded(Timer timer) {
        return this.updateAssignmentMetadataIfNeeded(timer, true);
    }
}
