package com.example.plugins.tutorial.jira.workflow;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.attachment.TemporaryWebAttachment;
import com.atlassian.jira.issue.attachment.TemporaryWebAttachmentManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.history.ChangeItemBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class WFHelper {

    private static final Logger log = LoggerFactory.getLogger(WFHelper.class);
    public static final String UPLOADED_FILES = "CF_UPLOADED_FILES" ;

    public static void doSomething(Map transientVars, MutableIssue issue,CustomFieldManager customFieldManager,boolean setValue) {

        log.info("DoSomething  for " +  issue.getKey() );

        Map<String, ModifiedValue> fields = issue.getModifiedFields();

        final List<String> uploadedFiles = new ArrayList<String>( );

        CustomField cfUpload = customFieldManager.getCustomFieldObject( new Long(10000) );
        StringBuffer uploadedFileString = new StringBuffer(  );

        fields.forEach((k, v) -> {
            log.info(("Key : " + k + " :  Old-Value " + v.getOldValue() + "  New-Value "+ v.getNewValue() ));

            if(k.equalsIgnoreCase( IssueFieldConstants.ATTACHMENT)) {

                ChangeItemBean attchEntry= new ChangeItemBean( );
                attchEntry.setField( k );
                attchEntry.setFieldType( "jira" );

                TemporaryWebAttachmentManager webAttManager = ComponentAccessor.getComponent(TemporaryWebAttachmentManager.class);


                // log.info("NEWVALUE TYPE - "+ v.getNewValue().getClass() );
                List valList = (List<String>) (v.getNewValue());

                for (Object fName: valList) {
                    log.info("Filelist-Entry TYPE - "+ fName.getClass()  + " -> " + fName );
                    TemporaryWebAttachment oneAttachment = webAttManager.getTemporaryWebAttachment( (String) fName ).get();

                    log.info( "File Found : "+  oneAttachment.getFilename() );

                    if(setValue) {
                        uploadedFileString.append( oneAttachment.getFilename()  );
                        uploadedFiles.add( oneAttachment.getFilename() ) ;
                    }

                    // FileSystemAttachmentDirectoryAccessor attachmentDirectoryAccessor = ComponentAccessor.getComponent( FileSystemAttachmentDirectoryAccessor.class );
                    // File temporaryAttachmentDirectory = attachmentDirectoryAccessor.getTemporaryAttachmentDirectory();

                    // File newFile = new File( temporaryAttachmentDirectory, (String) fName );
                    // log.info( "File Found : "+ newFile.getName() + " -> "+ newFile.getAbsoluteFile().getName() );

                }

            }
        });

        if(cfUpload == null) {
            log.info("Upload-CustomField is NULL !!!!!!!!!!!!!  : " );
        }

        if(setValue && (cfUpload != null) )  {

            issue.setCustomFieldValue (cfUpload, uploadedFileString.toString());
        }

        if(!setValue) {
            // readValues
           // List<String> newFiles = (List<String>) issue.getExternalFieldValue( UPLOADED_FILES );
           // List<String> newFiles = (List<String>) issue.getCustomFieldValue(cfUpload);
            String newFiles = (String) issue.getCustomFieldValue( cfUpload );

            if(newFiles != null) {
               // for (String fileEntry: newFiles) {
                    log.info( "EARLIER UPLOADED FILES " + newFiles );
               // }
            } else {
                log.warn("Cannot READ Uploaded file list !!!!!!!!!!! " );
            }

        }


        AttachmentManager attachmentManager = ComponentAccessor.getAttachmentManager();
        Iterable<Attachment> attachList = attachmentManager.getAttachments(issue) ;

        log.info("TOTAL Attachment in Issue :  -> " + ((Collection<Attachment>) attachList).size() ) ;

        List<Long> fileID = new ArrayList<Long>( );

        for(Attachment attchEntry: attachList){
            log.info("Attachment : Filename -> " + attchEntry.getFilename()
                    + " , ID -> " + attchEntry.getId()  + " , Created " + attchEntry.getCreated() );

            fileID.add( attchEntry.getId() );
        }

        transientVars.put( "LAST_ATTCHM_LIST",  fileID);
    }
}
