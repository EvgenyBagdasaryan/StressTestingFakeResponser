package ru.diasoft.integration.vtb.service.stub.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;

import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.rest.RestServer;
import ru.diasoft.integration.vtb.utils.Utl;
import ru.diasoft.integration.vtb.service.stub.AdtConfig;
import ru.diasoft.integration.vtb.service.stub.jms.JmsConfig;
import ru.diasoft.integration.vtb.sheduller.SchedulerConfigUpdate;

public class StubConfig {

	private static Logger logger = Logger.getLogger(RestServer.class);

	public static class Response {

		private final String template;
		private final long timeout;
		private volatile String responseData = null;
		private final String command;
		private final String postProcessor;
		private final String operation;
		private final String condition;

		private final Map<String, Replacer> replaces;

		public Response(Map<String, Object> value) {
			this.template = (String) value.get("template");
			long _timeout = 0;
			if (value.get("timeout") != null) {
				_timeout = Long.parseLong((String) value.get("timeout"));
			}

			this.timeout = _timeout;

			Map<String, Replacer> _replaces = new HashMap<String, Replacer>();

			Map<String, Object> replaceConf = (Map<String, Object>) value.get("replace");

			if (replaceConf != null) {
				for (Entry<String, Object> e : replaceConf.entrySet()) {
					Map<String, Object> value2 = (Map<String, Object>) e.getValue();
					for (Entry<String, Object> e2:value2.entrySet()) {
						_replaces.put(e.getKey(), new Replacer(e2.getKey(), e.getKey(), (String) e2.getValue()));
						break;
					}
				}
			}
			this.command = (String) value.get("command");
			this.postProcessor = (String) value.get("postProcessor");

			this.replaces = Collections.unmodifiableMap(_replaces);

			this.operation = (String) value.get("operation");
			this.condition = (String) value.get("condition");
		}

		public String getResponseData() throws Exception {

			if (notUseCacheForResponse) {
				synchronized (this) {
					readResponseFromFile();
				}
			} else {
				if (responseData == null) {
					synchronized (this) {
						if (responseData == null) {
							readResponseFromFile();
						}
					}
				}
			}
			return responseData;
		}

		private void readResponseFromFile() throws FileNotFoundException, IOException {
			if (template != null && !template.isEmpty()) {
				File file = new File(template);
				FileInputStream fis = new FileInputStream(file);
				try {
					byte[] bytes = IOUtils.toByteArray(fis);
					responseData = new String(bytes, Utl.CHARSET_UTF_8);

				} finally {
					fis.close();
				}
			} else {
				responseData = "";
			}
		}


		public long getTimeout() {
			return timeout;
		}


		public String getTemplate() {
			return template;
		}


		public Map<String, Replacer> getReplaces() {
			return replaces;
		}


		public String getCommand() {
			return command;
		}


		public String getPostProcessor() {
			return postProcessor;
		}


		public String getOperation() {
			return operation;
		}


		public String getCondition() {
			return condition;
		}


	}

	public static class Command {

		private final String command;
		private final String type;
		private final List<Response> reponses;
		private final AtomicInteger count = new AtomicInteger(0);

		public Command(String command, Map<String, Object> value) {
			this.command = command;
			this.type = (String) value.get("type");

			Object _resp = value.get("response");

			ArrayList<Response> _reponses = new ArrayList<StubConfig.Response>();

			if (_resp instanceof Map<?, ?>) {
				_reponses.add(new Response((Map<String, Object>)_resp));
			} else if (_resp instanceof Collection<?>) {
				for (Object o : ((Collection<?>)_resp)) {
					_reponses.add(new Response((Map<String, Object>)o));
				}
			}

			this.reponses = Collections.unmodifiableList(_reponses);
		}

		public String getCommand() {
			return command;
		}

		public AtomicInteger getCount() {
			return count;
		}

		public List<Response> getReponses() {
			return reponses;
		}

	}

	public static class Adapter {
		private final String name;
		private final Map<String, Command> commands = new HashMap<String, StubConfig.Command>();

		public Adapter(String name, Map<String, Object> value) {
			this.name = name;

			for (Entry<String, Object> e : value.entrySet()) {
				Command command = new Command(e.getKey(), (Map<String, Object>)e.getValue());
				getCommands().put(e.getKey().toLowerCase(), command);
			}

		}

		public String getName() {
			return name;
		}

		public Map<String, Command> getCommands() {
			return commands;
		}

	}

	private volatile static StubConfig INSTANCE = null;
	private volatile static SchedulerConfigUpdate instanceConfigUpdater = null;

	private Map<String, Adapter> config;
	private Map<String, Object> kafkaConfig;
	private Map<String, Object> kafkaTS73Config;
	private String defaultUrl;
	private static boolean notUseCacheForResponse;

	private JmsConfig jmsConfig;

	// Запускаем обновление конфига по таймеру
	static {
		initConfigUpdater();
	}

	private static StubConfig getStubConfig() {
		StubConfig ret;
		if ((ret = INSTANCE) == null) {
			synchronized (StubConfig.class) {
				if ((ret = INSTANCE) == null) {
					ret = new StubConfig();
					ret.loadConfig();
					INSTANCE = ret;
				}
			}
		}
		return ret;
	}

	public static void clear() {
		INSTANCE = null;
	}

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        config = new HashMap<>();

        Map<String, Object> allParams = AdtConfig.getAllParams();
        Map<String, Object> stubs = (Map<String, Object>) allParams.get("STUB");

        for (Entry<String, Object> e : stubs.entrySet()) {

            if ("JMS_CONFIG".equals(e.getKey())) {
                Map<String, Object> jmsConfigParams = (Map<String, Object>) e.getValue();
                if (jmsConfigParams != null && !jmsConfigParams.isEmpty()) {
                    jmsConfig = new JmsConfig(
                            BooleanUtils.toBoolean(String.valueOf(jmsConfigParams.get("useJMS"))),
                            String.valueOf(jmsConfigParams.get("initialContextFactory")),
                            String.valueOf(jmsConfigParams.get("jndiNameQueue")),
                            String.valueOf(jmsConfigParams.get("jndiNameConnectionFactory")),
                            String.valueOf(jmsConfigParams.get("jmsUrl")),
                            String.valueOf(jmsConfigParams.get("login")),
                            String.valueOf(jmsConfigParams.get("password"))
                    );
                }
                continue;

            } else if ("DEFAULT_CONFIG".equals(e.getKey())) {
                Map<String, Object> config = (Map<String, Object>) e.getValue();
                defaultUrl = (String) config.get("CallbackUrl");
                notUseCacheForResponse = Boolean.valueOf(String.valueOf(config.get("NotUseCaheForResponse")));
                continue;
            } else if ("KAFKA_CONFIG".equals(e.getKey())) {

				kafkaConfig = (Map<String, Object>) e.getValue();
				continue;
			}
			else if ("KAFKA_TS73_CONFIG".equals(e.getKey())) {

				kafkaTS73Config = (Map<String, Object>) e.getValue();
				continue;
			}
            Adapter adapter = new Adapter(e.getKey(), (Map<String, Object>) e.getValue());
            config.put(e.getKey(), adapter);
        }
    }

	public static void reloadConfig() {
		clear();
		getStubConfig();
	}

	private static void initConfigUpdater() {
		if (instanceConfigUpdater == null) {
			synchronized (StubConfig.class) {
				if (instanceConfigUpdater == null) {
					instanceConfigUpdater = new SchedulerConfigUpdate();
					Timer timer = new Timer();
					timer.schedule(instanceConfigUpdater, 0, 60000);
				}
			}
		}
	}

	public static Command getCommand(String adapter, String command, String type) throws Exception {
		StubConfig stubConfig = StubConfig.getStubConfig();
		Adapter adapterConf = stubConfig.config.get(adapter);
		if (adapterConf == null) {
			throw new Exception("Config for adapter " + adapter + " not found");
		}

		Command commandConf = adapterConf.getCommands().get(command.toLowerCase());

		if (commandConf == null) {
			throw new Exception("Config for command " + command + " not found");
		}

		if (!type.equalsIgnoreCase(commandConf.type)) {
			throw new Exception("Command type: " + type + " does not match");
		}

		return commandConf;
	}

	public static String getCallbackUrl() {
		return StubConfig.getStubConfig().defaultUrl;
	}

	public static JmsConfig getJmsConfig() {
		return StubConfig.getStubConfig().jmsConfig;
	}

	public static Map<String, Object> getKafkaConfig() {
		return StubConfig.getStubConfig().kafkaConfig;	}

	public static Map<String, Object> getKafkaTS73Config() {
		return StubConfig.getStubConfig().kafkaTS73Config;	}
}
