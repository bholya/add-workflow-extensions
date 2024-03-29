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


        /*
        List<ChangeHistory> changes = ComponentAccessor.getChangeHistoryManager().getChangeHistories( issue );

        for (ChangeHistory changeHistory  : changes) {
            List<ChangeItemBean> changeItemBeans = changeHistory.getChangeItemBeans();

            // log.info("[CloseParentIssuePostFunction] changeHistory :   "+  changeHistory.getId() );

            if(changeItemBeans != null) {

                for (ChangeItemBean changeBean  : changeItemBeans) {
                    log.info("[CloseParentIssuePostFunction] changeBean : ChangeHistoryID  "+   changeHistory.getId()   + " Field "  +  changeBean.getField() + " , Type" + changeBean.getFieldType() + " , TO  "+ changeBean.getTo()  ) ;

                }
            }

        }
        */

        log.info(" ################### [CloseParentIssuePostFunction]  END ################################## " );

        // Retrieve the sub-task
        MutableIssue subTask = getIssue(transientVars);


        // Retrieve the parent issue
        MutableIssue parentIssue = issueManager.getIssueObject(subTask.getParentId());

        // Ensure that the parent issue is not already closed
        if (parentIssue == null || IssueFieldConstants.CLOSED_STATUS_ID == Integer
            .parseInt(parentIssue.getStatusId())) {
            return;
        }




        // Check that ALL OTHER sub-tasks are closed
        Collection<Issue> subTasks = subTaskManager.getSubTaskObjects(parentIssue);

        for (Iterator<Issue> iterator = subTasks.iterator(); iterator.hasNext(); ) {
            Issue associatedSubTask = iterator.next();
            if (!subTask.getKey().equals(associatedSubTask.getKey()) &&
                IssueFieldConstants.CLOSED_STATUS_ID != Integer.parseInt(associatedSubTask.getStatus().getId())) {
                return;
            }
        }

        // All sub-tasks are now closed - close the parent issue
        try {
            closeIssue(parentIssue);
        } catch (WorkflowException e) {
            log.error(
                "Error occurred while closing the issue: " + parentIssue.getKey() + ": " + e, e);
            e.printStackTrace();
        }
    }


    private void doSomething(Map transientVars,MutableIssue issue) {


        WFHelper.doSomething( transientVars,issue,customFieldManager,false );

        /*
        final StringBuffer attachmentValue= new StringBuffer(  ) ;
        Map<String, ModifiedValue> fields = issue.getModifiedFields();
        fields.forEach((k, v) -> {
            log.info(("Key : " + k + " :  Value " + v.getOldValue() + "  New "+ v.getNewValue() ));

            if(k.equalsIgnoreCase( "attachment" )) {
                // attachmentValue = (String) v.getNewValue();
                // attachmentValue.append( (String) v.getNewValue() ) ;


            }
        });
        */
        AttachmentManager attachmentManager = ComponentAccessor.getAttachmentManager();
        Iterable<Attachment> attachList = attachmentManager.getAttachments(issue) ;

            log.info("TOTAL Attachment :  -> " + ((Collection<Attachment>) attachList).size() ) ;

            for(Attachment attchEntry: attachList){
                log.info("Attachment : Filename -> " + attchEntry.getFilename()
                        + " , ID -> " + attchEntry.getId()  + " , Created " + attchEntry.getCreated() );
            }


        if( transientVars.get( "LAST_ATTCHM_LIST") != null) {
            List<Long> old_attachList=  (  List<Long>) transientVars.get( "LAST_ATTCHM_LIST") ;
            Collection<Attachment> attachListAll = issue.getAttachments();

            log.info("FOUND LAST_ATTCHM_LIST  : OLD  -> " + old_attachList.size() + " , NEW -> "+ attachListAll.size() ) ;

            for(Attachment attchEntry: attachListAll){

                if(old_attachList.contains( attchEntry.getId() )) {
                    log.info(" +++++ NEW DOCUMENT : Filename -> " + attchEntry.getFilename()
                            + " , ID -> " + attchEntry.getId()  + " , Created " + attchEntry.getCreated() );
                }
            }


        } else {

            log.info("LAST_ATTCHM_LIST NOT FOUND :  !!!!!!!!!!!!!!!!!!  " ) ;
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