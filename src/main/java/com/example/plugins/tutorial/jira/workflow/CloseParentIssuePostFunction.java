package com.example.plugins.tutorial.jira.workflow;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.changehistory.ChangeHistory;
import com.atlassian.jira.issue.history.ChangeItemBean;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.jira.workflow.function.issue.AbstractJiraFunctionProvider;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.WorkflowException;
import com.opensymphony.workflow.loader.ActionDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This is the post-function class that gets executed at the end of the transition.
 * Any parameters that were saved in your factory class will be available in the transientVars Map.
 */
@Scanned
public class CloseParentIssuePostFunction extends AbstractJiraFunctionProvider {
    private static final Logger log = LoggerFactory.getLogger(CloseParentIssuePostFunction.class);

    @JiraImport
    private final WorkflowManager workflowManager;
    @JiraImport
    private final SubTaskManager subTaskManager;
    @JiraImport
    private final JiraAuthenticationContext authenticationContext;
    @JiraImport
    private final CustomFieldManager customFieldManager;
    @JiraImport
    private IssueManager issueManager;

    private final Status closedStatus;

    public CloseParentIssuePostFunction(ConstantsManager constantsManager,
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

        log.info("################### [CloseParentIssuePostFunction] START ################################## ");

        MutableIssue issue = getIssue(transientVars);
        printChangeItems(transientVars);
        doSomething(transientVars,issue);




        log.info(" ################### [CloseParentIssuePostFunction]  END ################################## " );

    }


    private void doSomething(Map transientVars,MutableIssue issue) {

        Map<String, List<String>> cache = WFCacheManager.getUploadedDocCacheForRead();

        if(cache.containsKey( issue.getKey() )) {

            List<String> files = cache.get( issue.getKey() );
            if ( (files != null) && files.size() > 0) {

                for(String fileEntry: files){
                    log.info("[CloseParentIssuePostFunction] From Cache : Uploaded File for " + issue.getKey() + "  -> "+ fileEntry) ;
                }
            }
        } else {
            log.info("No KEY for issue " + issue.getKey() + " found ") ;
        }

        AttachmentManager attachmentManager = ComponentAccessor.getAttachmentManager();
        Iterable<Attachment> attachList = attachmentManager.getAttachments(issue) ;

            log.info("TOTAL Attachment :  -> " + ((Collection<Attachment>) attachList).size() ) ;

            for(Attachment attchEntry: attachList){
                log.info("Attachment : Filename -> " + attchEntry.getFilename()
                        + " , ID -> " + attchEntry.getId()  + " , Created " + attchEntry.getCreated() );
            }
    }

    private List<ChangeItemBean> printChangeItems(Map transientVars) {
        List<ChangeItemBean> changeItems = (List<ChangeItemBean>) transientVars.get("changeItems");

        for (ChangeItemBean changes  : changeItems) {
            log.info("[CloseParentIssuePostFunction] ChangeItem : Field  "+ changes.getField() + " , Type" + changes.getFieldType() + " , TO  "+ changes.getTo()  ) ;

        }

        return changeItems;
    }


    private void closeIssue(Issue issue) throws WorkflowException {
        Status currentStatus = issue.getStatus();
        JiraWorkflow workflow = workflowManager.getWorkflow(issue);
        List<ActionDescriptor> actions = workflow.getLinkedStep(currentStatus).getActions();
        // look for the closed transition
        ActionDescriptor closeAction = null;
        for (ActionDescriptor descriptor : actions) {
            if (descriptor.getUnconditionalResult().getStatus().equals(closedStatus.getName())) {
                closeAction = descriptor;
                break;
            }
        }
        if (closeAction != null) {
            ApplicationUser currentUser = authenticationContext.getLoggedInUser();
            IssueService issueService = ComponentAccessor.getIssueService();
            IssueInputParameters parameters = issueService.newIssueInputParameters();
            parameters.setRetainExistingValuesWhenParameterNotProvided(true);
            IssueService.TransitionValidationResult validationResult =
                issueService.validateTransition(currentUser, issue.getId(),
                    closeAction.getId(), parameters);
            IssueService.IssueResult result = issueService.transition(currentUser, validationResult);
            // check result for errors
        }
    }
}