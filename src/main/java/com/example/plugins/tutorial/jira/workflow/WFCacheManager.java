package com.example.plugins.tutorial.jira.workflow;


import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class WFCacheManager {

    private static final Logger log = LoggerFactory.getLogger(WFCacheManager.class);
    private static final String UPLOADED_FILES = "UPLOADED_FILES" ;

    private CacheManager fCacheManager;

    private  Cache<String, Map< String, List<String>>> fCache;

    private static WFCacheManager fCacheManagerInstance = null;

    private  WFCacheManager(CacheManager cacheManager) {

        fCacheManager= cacheManager;
        fCache = fCacheManager.getCache(UPLOADED_FILES ,
                new UploadDocumentCache(),
                new CacheSettingsBuilder().expireAfterAccess(30, TimeUnit.DAYS).build());
    }


    public static Map<String, List<String>> getUploadedDocCache(CacheManager cacheManager) {

        if(fCacheManagerInstance == null) {
            log.debug( "[WFCacheManager] fCacheManagerInstance is null,so initialise it  " );
            fCacheManagerInstance = new WFCacheManager(cacheManager);
        }

        if( fCacheManagerInstance.fCache.get( UPLOADED_FILES ) == null) {

            Map<String, List<String>> map= new HashMap<String, List<String>>( );

            fCacheManagerInstance.fCache.put( UPLOADED_FILES,map );
        }

        return fCacheManagerInstance.fCache.get( UPLOADED_FILES );
    }


    public static Map<String, List<String>> getUploadedDocCacheForRead() {

        return fCacheManagerInstance.fCache.get( UPLOADED_FILES );
    }

}
