package ru.diasoft.integration.vtb.sheduller;

import org.apache.log4j.Logger;
import ru.diasoft.integration.vtb.service.stub.impl.StubConfig;

import java.util.Date;
import java.util.TimerTask;

public class SchedulerConfigUpdate extends TimerTask {

    private static final Logger logger = Logger.getLogger(SchedulerConfigUpdate.class);

    @Override
    public void run() {
        StubConfig.reloadConfig();
        //logger.debug("Reloading config for fakeResponser finished " + new Date());
    }
}
