package com.example.plugins.tutorial.jira.workflow;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.attachment.AttachmentService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.attachment.TemporaryWebAttachment;
import com.atlassian.jira.issue.attachment.TemporaryWebAttachmentManager;
import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.loader.ActionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * This is the post-function class that gets executed at the end of the transition.
 * Any parameters that were saved in your factory class will be available in the transientVars Map.
 */
@Scanned
public class SetUploadedDocumentsPostFunction extends AbstractJiraFunctionProvider {
    private static final Logger log = LoggerFactory.getLogger(SetUploadedDocumentsPostFunction.class);

    @JiraImport
    private final WorkflowManager workflowManager;
    @JiraImport
    private final SubTaskManager subTaskManager;
    @JiraImport
    private final JiraAuthenticationContext authenticationContext;
    @JiraImport
    private IssueManager issueManager;

    private final Status closedStatus;
    @JiraImport
    private final CustomFieldManager customFieldManager;

    public SetUploadedDocumentsPostFunction(ConstantsManager constantsManager,
                                            WorkflowManager workflowManager,
                                            SubTaskManager subTaskManager,
                                            JiraAuthenticationContext authenticationContext,
                                            IssueManager issueManager,CustomFieldManager customFieldManager) {
        this.workflowManager = workflowManager;
        this.subTaskManager = subTaskManager;
        this.authenticationContext = authenticationContext;
        this.issueManager = issueManager;
        this.customFieldManager = customFieldManager;
        closedStatus = constantsManager
            .getStatus(Integer.toString(IssueFieldConstants.CLOSED_STATUS_ID));


    }

    public void execute(Map transientVars, Map args, PropertySet ps) throws WorkflowException {

        log.info("################### [SetUploadedDocumentsPostFunction] START ################################## ");

        MutableIssue issue = getIssue(transientVars);
        printChangeItems(transientVars);
        WFHelper.doSomething(transientVars,issue,customFieldManager,true);

        log.info(" ################### [SetUploadedDocumentsPostFunction]  END ################################## " );

        /*
        List<ChangeHistory> changes = ComponentAccessor.getChangeHistoryManager().getChangeHistories( issue );

        for (ChangeHistory changeHistory  : changes) {
            List<ChangeItemBean> changeItemBeans = changeHistory.getChangeItemBeans();

            if(changeItemBeans != null) {

                for (ChangeItemBean changeBean  : changeItemBeans) {
                    log.info("[SetUploadedDocumentsPostFunction] changeBean : ChangeHistoryID  "+   changeHistory.getId()   + " Field "  + changeBean.getField() + " , Type" + changeBean.getFieldType() + " , TO  "+ changeBean.getTo()  ) ;

                }
            }

        }
        */
    }


    private void doSomething(Map transientVars,MutableIssue issue) {

        log.info("DoSomething  for " +  issue.getKey() );

        Map<String, ModifiedValue> fields = issue.getModifiedFields();

        fields.forEach((k, v) -> {
            log.info(("Key : " + k + " :  Value " + v.getOldValue() + "  New "+ v.getNewValue() ));

            if(k.equalsIgnoreCase( IssueFieldConstants.ATTACHMENT)) {

                ChangeItemBean attchEntry= new ChangeItemBean( );
                attchEntry.setField( k );
                attchEntry.setFieldType( "jira" );

                TemporaryWebAttachmentManager webAttManager = ComponentAccessor.getComponent(TemporaryWebAttachmentManager.class);


                log.info("NEWVALUE TYPE - "+ v.getNewValue().getClass() );
                List valList = (List<String>) (v.getNewValue());

                for (Object fName: valList) {
                    log.info("valList TYPE - "+ fName.getClass()  + " -> " + fName );
                    TemporaryWebAttachment oneAttachment = webAttManager.getTemporaryWebAttachment( (String) fName ).get();

                    log.info( "File Found : "+  oneAttachment.getFilename() );

                   // FileSystemAttachmentDirectoryAccessor attachmentDirectoryAccessor = ComponentAccessor.getComponent( FileSystemAttachmentDirectoryAccessor.class );
                   // File temporaryAttachmentDirectory = attachmentDirectoryAccessor.getTemporaryAttachmentDirectory();

                   // File newFile = new File( temporaryAttachmentDirectory, (String) fName );
                   // log.info( "File Found : "+ newFile.getName() + " -> "+ newFile.getAbsoluteFile().getName() );

                }

            }
        });

        /*
        Map<String, ModifiedValue> fields = issue.getModifiedFields();

        fields.forEach((k, v) -> {
            log.info(("Key : " + k + " :  Value " + v.getOldValue() + "  New "+ v.getNewValue() ));

            if(k.equalsIgnoreCase( "attachment" )) {
                // attachmentValue = (String) v.getNewValue();
                // attachmentValue.append( (String) v.getNewValue() ) ;
                ChangeItemBean attchEntry= new ChangeItemBean( );
                attchEntry.setField( k );
                attchEntry.setFieldType( "jira" );


            }
        });
        */

            AttachmentManager attachmentManager = ComponentAccessor.getAttachmentManager();
           Iterable<Attachment> attachList = attachmentManager.getAttachments(issue) ;

            // Iterable<Attachment> attachList = issue.getAttachments();

            log.info("TOTAL Attachment :  -> " + ((Collection<Attachment>) attachList).size() ) ;

            List<Long> fileID = new ArrayList<Long>( );

            for(Attachment attchEntry: attachList){
                log.info("Attachment : Filename -> " + attchEntry.getFilename()
                        + " , ID -> " + attchEntry.getId()  + " , Created " + attchEntry.getCreated() );

                fileID.add( attchEntry.getId() );
            }


           transientVars.put( "LAST_ATTCHM_LIST",  fileID);

    }

    private List<ChangeItemBean> printChangeItems(Map transientVars) {
        List<ChangeItemBean> changeItems = (List<ChangeItemBean>) transientVars.get("changeItems");

        if(changeItems != null) {

            for (ChangeItemBean changes  : changeItems) {
                log.info("[SetUploadedDocumentsPostFunction] ChangeItem : Field  "+ changes.getField() + " , Type" + changes.getFieldType() + " , TO  "+ changes.getTo()  ) ;

            }

        } else {

            log.info("[SetUploadedDocumentsPostFunction] NO CHANGE ITEMS !!!!  " ) ;
        }


        return changeItems;
    }

}