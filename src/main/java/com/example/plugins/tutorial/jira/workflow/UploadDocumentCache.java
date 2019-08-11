package com.example.plugins.tutorial.jira.workflow;

import com.atlassian.cache.CacheLoader;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadDocumentCache implements CacheLoader<String, Map<String, List<String>> > {

    private final Map<String, Map<String, List<String>>  > fCache = new HashMap<>();

    @Nonnull
    @Override
    public Map<String, List<String>>  load(@Nonnull String projectKey) {

       if( fCache.get( projectKey ) == null ) {

           List<String> list = new ArrayList<String>( );

           Map<String, List<String>> map= new HashMap<String, List<String>>( );

           fCache.put( projectKey, map );
       }

        return fCache.get( projectKey );
    }
}
