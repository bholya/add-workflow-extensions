package com.example.plugins.tutorial.jira.workflow;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.attachment.TemporaryWebAttachment;
import com.atlassian.jira.issue.attachment.TemporaryWebAttachmentManager;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


@Scanned
public class SetUploadedDocumentsPostFunction extends AbstractJiraFunctionProvider {
    private static final Logger log = LoggerFactory.getLogger(SetUploadedDocumentsPostFunction.class);

    @ComponentImport
    private final WorkflowManager workflowManager;
    @ComponentImport
    private final JiraAuthenticationContext authenticationContext;
    @ComponentImport
    private IssueManager issueManager;
    @JiraImport
    private final CustomFieldManager customFieldManager;
    @ComponentImport
    private final CacheManager cacheManager;

    private final Status closedStatus;

    public SetUploadedDocumentsPostFunction(ConstantsManager constantsManager,
                                            WorkflowManager workflowManager,
                                            JiraAuthenticationContext authenticationContext,
                                            IssueManager issueManager,CustomFieldManager customFieldManager,CacheManager cacheManager) {
        this.workflowManager = workflowManager;
        this.authenticationContext = authenticationContext;
        this.issueManager = issueManager;
        this.customFieldManager = customFieldManager;
        this.cacheManager = cacheManager;

        closedStatus = constantsManager
            .getStatus(Integer.toString(IssueFieldConstants.CLOSED_STATUS_ID));
    }

    public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {


        MutableIssue issue = getIssue(transientVars);
        log.debug("################### [SetUploadedDocumentsPostFunction] START " + issue.getKey()  + " ################################## ");

        setUploadedFilesInCache(transientVars,issue);

        log.debug(" ################### [SetUploadedDocumentsPostFunction]  END " + issue.getKey()  + " ################################## " );

    }



    private  void setUploadedFilesInCache(Map transientVars, MutableIssue issue) {

        log.debug("[SetUploadedDocumentsPostFunction] setUploadedFilesInCache " +  issue.getKey() );

        Map<String, ModifiedValue> fields = issue.getModifiedFields();

        final List<String> uploadedFiles = new ArrayList<String>( );

        StringBuffer uploadedFileString = new StringBuffer(  );

        fields.forEach((k, v) -> {
            log.info(("[SetUploadedDocumentsPostFunction] Modified-Fields: Key : " + k + " :  Old-Value " + v.getOldValue() + "  New-Value "+ v.getNewValue() ));

            if(k.equalsIgnoreCase( IssueFieldConstants.ATTACHMENT)) {

                TemporaryWebAttachmentManager webAttManager = ComponentAccessor.getComponent(TemporaryWebAttachmentManager.class);

                List valList = (List<String>) (v.getNewValue());

                for (Object fName: valList) {

                    TemporaryWebAttachment oneAttachment = webAttManager.getTemporaryWebAttachment( (String) fName ).get();

                    log.info( "[SetUploadedDocumentsPostFunction] Recently Uploaded File Found : "+  oneAttachment.getFilename() );

                    uploadedFiles.add( oneAttachment.getFilename() ) ;
                }

            }
        });


        WFCacheManager.getUploadedDocCache(cacheManager).put( issue.getKey(), uploadedFiles );

        AttachmentManager attachmentManager = ComponentAccessor.getAttachmentManager();
        Iterable<Attachment> attachList = attachmentManager.getAttachments(issue) ;

        log.debug("[SetUploadedDocumentsPostFunction] Listing Attachment in Issue. TOTAL Attachment in Issue :  -> "
                + ((Collection<Attachment>) attachList).size() ) ;

        List<Long> fileID = new ArrayList<Long>( );

        for(Attachment attchEntry: attachList){
            log.debug("[SetUploadedDocumentsPostFunction] Attachment : Filename -> " + attchEntry.getFilename()
                    + " , ID -> " + attchEntry.getId()  + " , Created " + attchEntry.getCreated() );

            fileID.add( attchEntry.getId() );
        }

    }

    private List<ChangeItemBean> printChangeItems(Map transientVars) {
        List<ChangeItemBean> changeItems = (List<ChangeItemBean>) transientVars.get("changeItems");

        if(changeItems != null) {

            for (ChangeItemBean changes  : changeItems) {
                log.debug("[SetUploadedDocumentsPostFunction] ChangeItem : Field  "+ changes.getField() + " , Type" + changes.getFieldType() + " , TO  "+ changes.getTo()  ) ;
            }
        } else {
            log.debug("[SetUploadedDocumentsPostFunction] NO CHANGE ITEMS !!!!  " ) ;
        }

        return changeItems;
    }

}