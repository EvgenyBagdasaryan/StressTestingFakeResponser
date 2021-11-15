package ru.diasoft.integration.vtb.service.stub.jms;

public class JmsConfig {

    private Boolean useJMS;
    private String initialContextFactory;
    private String jndiNameQueue;
    private String jndiNameConnectionFactory;
    private String jmsUrl;
    private String login;
    private String password;

    public JmsConfig(
            Boolean useJMS,
            String initialContextFactory,
            String jndiNameQueue,
            String jndiNameConnectionFactory,
            String jmsUrl,
            String login,
            String password) {

        this.useJMS = useJMS;
        this.initialContextFactory = initialContextFactory;
        this.jndiNameQueue = jndiNameQueue;
        this.jndiNameConnectionFactory = jndiNameConnectionFactory;
        this.jmsUrl = jmsUrl;
        this.login = login;
        this.password = password;
    }

    public Boolean getUseJMS() {
        return useJMS;
    }

    public String getInitialContextFactory() {
        return initialContextFactory;
    }

    public String getJndiNameQueue() {
        return jndiNameQueue;
    }

    public String getJndiNameConnectionFactory() {
        return jndiNameConnectionFactory;
    }

    public String getJmsUrl() {
        return jmsUrl;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }
}
