package org.jumpmind.pos.config.model;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jumpmind.pos.config.ConfigException;
import org.jumpmind.pos.persist.DBSession;
import org.jumpmind.pos.persist.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;

@Repository
@DependsOn(value = { "ConfigModule" })
public class ConfigRepository {
    
    private static Logger log = Logger.getLogger(ConfigRepository.class);
    
    public static final String TAG_REGION = LocationType.REGION.name();
    public static final String TAG_COUNTRY = LocationType.COUNTRY.name();
    public static final String TAG_STATE = LocationType.STATE.name();
    public static final String TAG_STORE = LocationType.STORE.name();
    public static final String TAG_NODE_ID = LocationType.NODE_ID.name();
    public static final String TAG_STORE_TYPE = "STORE_TYPE";
    public static final String TAG_DEPARTMENT_ID = "DEPARTMENT_ID";
    public static final String TAG_BRAND_ID = "BRAND_ID";
    public static final String TAG_DEVICE_TYPE = "DEVICE_TYPE";

    private static final int DISQUALIFIED = Integer.MIN_VALUE;
    
    private Query<ConfigModel> configLookup = new Query<ConfigModel>()
            .named("configLookup")
            .result(ConfigModel.class);
    
    @Autowired
    @Qualifier("configDbSession")
    @Lazy
    private DBSession dbSession;    
    
    public ConfigModel findConfigValue(Date currentTime, Map<String, String> tags, String configName) {
        
        Map<String, Object> params = new HashMap<>();
        params.put("configName", configName);
        params.put("currentTime", currentTime);
        List<ConfigModel> configs = dbSession.query(configLookup, params);
        if (CollectionUtils.isEmpty(configs)) {
            if (log.isDebugEnabled()) {                
                log.debug("No configuration found for " + configName);
            }
            return null;
        }
        
        ConfigModel config = findMostSpecificConfig(tags, configs);
        if (config != null) {
            return config;
        } else {            
            log.debug("No matching configuration found for " + configName + " and tags " + tags);
            return null;
        }
     }

    protected ConfigModel findMostSpecificConfig(Map<String, String> tags, List<ConfigModel> configs) {
        if (tags == null) {
            throw new ConfigException("tags cannot be null");
        }
        if (configs == null) {
            throw new ConfigException("configs cannot be null");
        }
        
        ConfigModel bestMatchConfig = null;
        
        int maxMatchScore = -1;
        
        for (ConfigModel config : configs) {
            int matchScore = 0; 
            if (config.getLocationType() != null) {
                if (!matchLocation(config, tags)) {
                    continue;
                }
            }
            matchScore += evaluateLocation(config);
            matchScore += evaluateTag(config.getBrandId(), tags.get(TAG_BRAND_ID), TAG_BRAND_ID, 1000);
            matchScore += evaluateTag(config.getStoreType(), tags.get(TAG_STORE_TYPE), TAG_STORE_TYPE, 500);
            matchScore += evaluateTag(config.getDepartmentId(), tags.get(TAG_DEPARTMENT_ID), TAG_DEPARTMENT_ID, 250);
            matchScore += evaluateTag(config.getDeviceType(), tags.get(TAG_DEVICE_TYPE), TAG_DEVICE_TYPE, 50);
            
            if (matchScore > maxMatchScore) {
                bestMatchConfig = config;
                maxMatchScore = matchScore;
            }
        }
        
        if (bestMatchConfig == null) {
            return null;
        } else {
            return bestMatchConfig;
        } 
    }
    
    protected int evaluateLocation(ConfigModel config) {
        if (config.getLocationType() == null) {
            return 0;
        }
        
        switch (config.getLocationType()) {
            case REGION:
                return 1;
            case COUNTRY:
                return 2;
            case STATE:
                return 3;
            case STORE:
                return 4;
            case NODE_ID:
                return 5;                
        }
        return 0;
    }

    protected boolean matchLocation(ConfigModel config, Map<String, String> tags) {
        String actualLocationValue = tags.get(config.getLocationType().name());
        
        if (StringUtils.isEmpty(actualLocationValue)) {
            throw new ConfigException("Can't find tag value for location type: " + config.getLocationType() + " in tags: " + tags);
        }
        
        return actualLocationValue.equals(config.getLocationValue());
    }

    protected int evaluateTag(String configValue, String nodeValue, final String tagName, int points) {
        if (nodeValue != null) {
            if (StringUtils.isEmpty(configValue) || configValue.equals(ConfigModel.TAG_ALL)) {
                return 0;
            } else if (configValue.equals(nodeValue)) {
                return points;
            } else {
                return DISQUALIFIED;
            }
        } else {
            return 0;
        }
    }
}
