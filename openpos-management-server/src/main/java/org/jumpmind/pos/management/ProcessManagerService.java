package org.jumpmind.pos.management;


import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.jumpmind.pos.util.AppUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProcessManagerService {
    @Autowired
    OpenposManagementServerConfig config;

    @Autowired
    private ProcessManagerEnvironmentService processMgrEnvSvc;
    
    @Autowired
    private DeviceProcessTracker processTracker;
    
    @Autowired
    private DeviceProcessLauncher processLauncher;
    
    
    // TODO: come up with better name
    public DeviceProcessInfo queryOrLaunchDeviceProcess(String deviceId) {
        long processStartMaxWaitMillis = config.getDeviceProcess().getStartMaxWaitMillis();
        processMgrEnvSvc.ensureMainWorkDirExists();

        Instant start = Instant.now();
        while (Duration.between(start, Instant.now()).toMillis() < processStartMaxWaitMillis) {
            long timeRemaining = processStartMaxWaitMillis - Duration.between(start, Instant.now()).toMillis();
            // TODO: Need to read device process status from file system?
            // Need to add logic to shutdown all processes on exit
            // DeviceProcessInfo processInfo = processTracker.waitIfStarting(deviceId, timeRemaining);
            boolean gotLock = false;
            try {
                if (processTracker.waitForLock(deviceId, timeRemaining)) {
                    gotLock = true;
                    DeviceProcessInfo pi = processTracker.getDeviceProcessInfo(deviceId);
                    if (pi.getStatus() == DeviceProcessStatus.Running) {
                        return pi;
                    } else if (pi.getStatus() == DeviceProcessStatus.NotRunning) {
                        if (pi.isNotPreviouslyStarted()) {
                            try {
                                pi = this.processLauncher.launch(pi, timeRemaining);
                                if (pi.isProcessAlive()) {
                                    processTracker.updateDeviceProcessStatus(pi, DeviceProcessStatus.Starting);
                                    addShutdownHook(pi);
                                } else {
                                    throw new DeviceProcessLaunchException(
                                            String.format("Failed to create process for Device '%s'.  Check the process log for the additional info.", pi.getDeviceId())
                                    );
                                }
                            } catch (DeviceProcessLaunchException dplEx) {
                                throw dplEx;
                            } catch (Exception e) {
                                throw new OpenposManagementException(
                                   String.format("Unexpected failure during launch of Device Process for '%s'", pi.getDeviceId()),
                                   e
                                );
                            }
                        }
                    } else if (pi.getStatus() == DeviceProcessStatus.Starting) {
                        log.info("Device Process '{}' is still starting, will check it again in a second. Time remaining: {}", deviceId, timeRemaining);
                        try { Thread.sleep(1000); } 
                        catch (InterruptedException ex) {
                            log.warn("Thread interrupted", ex);
                        }
                    } else if (pi.getStatus() == DeviceProcessStatus.StartupFailed) {
                        break;
                    }
                } // Never got the lock and timed out
            } finally {
                if (gotLock) {
                    processTracker.unlock(deviceId);
                }
            }
        }
        
        if (processTracker.waitForLock(deviceId, 3000)) {
            DeviceProcessInfo pi = processTracker.getDeviceProcessInfo(deviceId);
            if (pi.getStatus() == DeviceProcessStatus.Starting) {
                // We exhausted the timeout period waiting for the process to finish
                // starting, we'll have to mark it as a startup failure
                processTracker.updateDeviceProcessStatus(pi, DeviceProcessStatus.StartupFailed);
            }
        }
        
        return null;
        
    }
    
    protected void addShutdownHook(DeviceProcessInfo pi) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (pi.getProcess().isAlive()) {
                log.info("Destroying child Device Process '{}', pid: {}", 
                    pi.getDeviceId(), pi.getPid() != null ? pi.getPid() : "unknown");
                pi.getProcess().destroy();
                this.processTracker.removeDeviceProcessInfo(pi);
            }
        }));
    }

    public ClientConnectInfo constructClientConnectInfo(DeviceProcessInfo pi) {
        ClientConnectInfo cci = new ClientConnectInfo(
            this.resolveHostname(),
            pi.getPort(),
            makeUrl(config.getClientConnect().getWebServiceBaseUrlTemplate(), pi),
            makeUrl(config.getClientConnect().getSecureWebServiceBaseUrlTemplate(), pi),
            makeUrl(config.getClientConnect().getWebSocketBaseUrlTemplate(), pi),
            makeUrl(config.getClientConnect().getSecureWebSocketBaseUrlTemplate(), pi)
        );        
        return cci;
    }
    
    protected String resolveHostname() {
        return StringUtils.isNotBlank(config.getClientConnect().getHostname()) ? config.getClientConnect().getHostname() : AppUtils.getHostName();
    }
    protected String makeUrl(String urlTemplate, DeviceProcessInfo pi) {
        if (StringUtils.isNotBlank(urlTemplate)) {
            String url = urlTemplate;
            if (url.contains("$host")) {
                url = url.replace("$host", this.resolveHostname());
            }
            
            if (url.contains("$port") && pi.getPort() != null) {
                url = url.replace("$port", pi.getPort().toString());
            }
            
            return url;
        } else {
            return null;
        }
    }
    
}
