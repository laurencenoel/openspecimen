/**
 * <p>Title: DataViewAction Class>
 * <p>Description:	DataViewAction is used to show the query results data 
 * in spreadsheet or individaul view.</p>
 * Copyright:    Copyright (c) year
 * Company: Washington University, School of Medicine, St. Louis.
 * @author Gautam Shetty
 * @version 1.00
 */
package edu.wustl.catissuecore.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import edu.wustl.catissuecore.bizlogic.BizLogicFactory;
import edu.wustl.catissuecore.bizlogic.QueryBizLogic;
import edu.wustl.catissuecore.query.ResultData;
import edu.wustl.catissuecore.util.Permissions;
import edu.wustl.catissuecore.util.global.Constants;
import edu.wustl.common.beans.NameValueBean;
import edu.wustl.common.security.SecurityManager;
import edu.wustl.common.util.dbManager.DAOException;
import edu.wustl.common.util.logger.Logger;

/**
 * DataViewAction is used to show the query results data 
 * in spreadsheet or individaul view.
 * @author gautam_shetty
 */
public class DataViewAction extends BaseAction
{
    
    /**
     * Overrides the execute method in Action class.
     */
    public ActionForward executeAction(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception
    {
    	HttpSession session = request.getSession();
    	String target = Constants.SUCCESS;
    	String nodeName = request.getParameter("nodeName");
    	if(nodeName==null)
    		nodeName = (String)session.getAttribute(Constants.SELECTED_NODE);
    		
    	Logger.out.debug("nodename of selected node"+nodeName);
        StringTokenizer str = new StringTokenizer(nodeName,":");
        String name = str.nextToken().trim();
        
        String id = new String();
        String parentName = new String();
    	String parentId = new String();
    	
    	//Get the listData, column display names and select column names if it is configured and set in session
    	List filteredColumnDisplayNames=(List)session.getAttribute(Constants.CONFIGURED_COLUMN_DISPLAY_NAMES);
    	Logger.out.debug("ColumnDisplayNames from configuration"+filteredColumnDisplayNames);
    	String[] columnList= (String[])session.getAttribute(Constants.CONFIGURED_SELECT_COLUMN_LIST);
    	
    	//Retrieve the columnIdsMap from session
    	Map columnIdsMap = (Map)session.getAttribute(Constants.COLUMN_ID_MAP);
    	
    	//Set default select column list and column display names in case of Root and Participant nodes are selected
    	if (name.equals(Constants.ROOT)|| name.equals(Constants.PARTICIPANT))
        {
    		
    		if(filteredColumnDisplayNames==null)
    			filteredColumnDisplayNames = (List)session.getAttribute(Constants.COLUMN_DISPLAY_NAMES);
    		if(columnList==null)
    		{
    			columnList=new String[columnIdsMap.size()];
    			for(int count=0;count<columnIdsMap.size();count++)
    			{
    				columnList[count]=Constants.COLUMN+count;
    			}
    		}
    		
        }
        if (!name.equals(Constants.ROOT))
        {
        	id = str.nextToken();
        }
        /*Incase of collection protocol selected, the whereCondition should contain the participant conditions also
         * as Collection Protocol and Participant have many to many relationship
         */ 
        if(str.hasMoreTokens())
        {
        	parentName = str.nextToken();
        	parentId = str.nextToken();
        }

        //get the type of view to show (spreadsheet/individual)
        String viewType = request.getParameter(Constants.VIEW_TYPE);
        if(viewType==null)
        	viewType=Constants.SPREADSHEET_VIEW;
        if (viewType.equals(Constants.SPREADSHEET_VIEW))
        {
    		
        	if (!name.equals(Constants.ROOT))
            {	
        		Logger.out.debug("node name selected inside if condition:"+name);
        		Logger.out.debug("column list inside if condition::"+columnList);
                if (!name.equals(Constants.PARTICIPANT) && columnList==null)
                {
                	Logger.out.debug("Inside if condition of filtercolumns");
                	filteredColumnDisplayNames=new ArrayList();
                	columnList = getColumnNamesForFilter(name,filteredColumnDisplayNames,columnIdsMap);
                	Logger.out.debug("select column list size:"+columnList.length);
                	Logger.out.debug("filteredColumnDisplayNames after func call"+filteredColumnDisplayNames);
                }
                Logger.out.debug("columnList in dataview:"+columnList);
                Logger.out.debug("alias name of selected node in adv tree:"+name);
                Logger.out.debug("column ids map in data view action"+columnIdsMap);
                String key = name+"."+Constants.IDENTIFIER;
                int columnId = ((Integer)columnIdsMap.get(name+"."+Constants.IDENTIFIER)).intValue()-1;
                Logger.out.debug("columnid of selected node:"+columnId+"in the map for key:"+key);
                name=Constants.COLUMN+columnId;
                Logger.out.debug("Column name of the selected column in the tree:"+name);
                if(!parentName.equals(""))
                {
                    key = parentName+"."+Constants.IDENTIFIER;
                    columnId = ((Integer)columnIdsMap.get(parentName+"."+Constants.IDENTIFIER)).intValue()-1;
                    parentName=Constants.COLUMN+columnId;
                }
            }
    		//Add specimen identifier column if it is not there,required for shopping cart action
            int specimenColumnId = ((Integer)columnIdsMap.get(Constants.SPECIMEN+"."+Constants.IDENTIFIER)).intValue()-1;
            Logger.out.debug("specimenColumnId for adding compulsory specimen id:"+specimenColumnId);
            String specimenColumn = Constants.COLUMN+specimenColumnId;
            Logger.out.debug("specimenColumn for adding compulsory specimen id:"+specimenColumn);
            boolean exists = false;
            Logger.out.debug("columnList in dataview:"+columnList);
            if(columnList!=null)
            {
            	for(int i=0;i<columnList.length;i++)
            	{
            		Logger.out.debug("column loop :"+columnList[i]);
            		if(columnList[i].equals(specimenColumn))
            		{
            			Logger.out.debug("Specimen id column does not exist");
            			exists=true;
            		}
            	}
                if(!exists)
                {
                	Logger.out.debug("add specimen id column");
                	String columnListWithoutSpecimenId[] = columnList;
                	columnList = new String[columnListWithoutSpecimenId.length+1];
                    for(int i=0;i<columnListWithoutSpecimenId.length;i++)
                    {
                    	columnList[i] = columnListWithoutSpecimenId[i];
                    }
                    columnList[columnListWithoutSpecimenId.length]=specimenColumn;
                	//filteredColumnDisplayNames.add("Identifier");
                }
            }
            Logger.out.debug("columnList in dataview:"+columnList);
            Logger.out.debug("column names before func call to resultdata"+filteredColumnDisplayNames);
        	String[] whereColumnName= new String[1]; 
        	
            String[] whereColumnValue = new String[1];
            
            String[] whereColumnCondition = new String[1];

            if(!parentName.equals(""))
            {
            	whereColumnName=new String[2];
            	whereColumnValue = new String[2];
            	whereColumnCondition = new String[2];

            	Logger.out.debug("parentname & id for coll prot"+parentName+"&"+parentId);
            	whereColumnName[1]=parentName;
            	whereColumnValue[1]=parentId;
            	whereColumnCondition[1]="=";
            }
            whereColumnName[0]=name;
            whereColumnValue[0] =id;
            whereColumnCondition[0]="=";

        	List list = null;
            ResultData resultData = new ResultData();
            Logger.out.debug("columnList in dataview:"+columnList);
            list = resultData.getSpreadsheetViewData(whereColumnName,whereColumnValue,whereColumnCondition,columnList, getSessionData(request), Constants.OBJECT_LEVEL_SECURE_RETRIEVE);
            Logger.out.debug("list of data after advance search"+list);
            Logger.out.debug("column list after advance search"+filteredColumnDisplayNames);
     		// If the result contains no data, show error message.
    		if (list.isEmpty()) 
    		{
    			Logger.out.debug("inside if condition for empty list");
    			ActionErrors errors = new ActionErrors();
    			errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("advanceQuery.noRecordsFound"));
    			saveErrors(request, errors);
    			request.setAttribute(Constants.PAGEOF,Constants.PAGEOF_QUERY_RESULTS);
    			//target = Constants.FAILURE;
    		}
    		else
    		{
    	        Logger.out.debug("List of data in dataview action:"+list);
    	        Logger.out.debug("column names in dataview action:"+filteredColumnDisplayNames);
    	        //if specimen id is added to the columns then add display name identifier to the filteredColumnDisplayNames
    	        if(columnList!=null)
    	        {
    	        	if(columnList.length-filteredColumnDisplayNames.size()==1)
    	        		filteredColumnDisplayNames.add("Identifier");
    	        }
    			request.setAttribute(Constants.SPREADSHEET_COLUMN_LIST,filteredColumnDisplayNames);
   				request.setAttribute(Constants.SPREADSHEET_DATA_LIST,list);
    			request.setAttribute(Constants.PAGEOF,Constants.PAGEOF_QUERY_RESULTS);
    			Logger.out.debug("columnList in dataview:"+columnList);
        		session.setAttribute(Constants.SELECT_COLUMN_LIST,columnList);
    			session.setAttribute(Constants.SELECTED_NODE,nodeName);
    		}
        }
        else
        {
            String url = null;
            Logger.out.debug("selected node name in object view:"+name+"object");
            
//       	 Aarti: Check whether user has use permission to update this object
    		// or not
            if(!SecurityManager.getInstance(this.getClass()).isAuthorized(getUserLoginName(request)
            		,Constants.PACKAGE_DOMAIN+"."+name,Permissions.UPDATE))
    		{
            	ActionErrors errors = new ActionErrors();
             	ActionError error = new ActionError("access.edit.object.denied",getUserLoginName(request),Constants.PACKAGE_DOMAIN+"."+name
             	        				);
             	errors.add(ActionErrors.GLOBAL_ERROR,error);
             	saveErrors(request,errors);
            	return mapping.findForward(Constants.FAILURE);
    		}
            
            if (name.equals(Constants.PARTICIPANT))
            {
                url = new String(Constants.QUERY_PARTICIPANT_SEARCH_ACTION+id+"&"+Constants.PAGEOF+"="+
                														Constants.PAGEOF_PARTICIPANT_QUERY_EDIT);
            }
            else if (name.equals(Constants.COLLECTION_PROTOCOL))
            {
            	url = new String(Constants.QUERY_COLLECTION_PROTOCOL_SEARCH_ACTION+id+"&"+Constants.PAGEOF+"="+
            															Constants.PAGEOF_COLLECTION_PROTOCOL_QUERY_EDIT);
            			
            }
            else if (name.equals(Constants.SPECIMEN_COLLECTION_GROUP))
            {
            	url = new String(Constants.QUERY_SPECIMEN_COLLECTION_GROUP_SEARCH_ACTION+id+"&"+Constants.PAGEOF+"="+
            															Constants.PAGEOF_SPECIMEN_COLLECTION_GROUP_QUERY_EDIT);
            			
            }
            else if (name.equals(Constants.SPECIMEN))
            {
            	url = new String(Constants.QUERY_SPECIMEN_SEARCH_ACTION+id+"&"+Constants.PAGEOF+"="+
            															Constants.PAGEOF_SPECIMEN_QUERY_EDIT);
            			
            }
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(url);
            requestDispatcher.forward(request,response);
        }
        return mapping.findForward(target);
    }

    //returns the filtered select column list to be shown when clicked on a node of the results tree
    private String[] getColumnNamesForFilter(String aliasName,List filteredColumnDisplayNames,Map columnIdsMap) throws DAOException,ClassNotFoundException
    {
    	List columnDisplayNames=new ArrayList();
    	QueryBizLogic bizLogic = (QueryBizLogic)BizLogicFactory.getBizLogic(Constants.SIMPLE_QUERY_INTERFACE_ID);
    	List columns =new ArrayList();
    	//Filter the data according to the node clicked. Show only the data lower in the heirarchy 
    	if(aliasName.equals(Constants.COLLECTION_PROTOCOL))
		{
    		columns.addAll(bizLogic.setColumnNames(Constants.COLLECTION_PROTOCOL));
    		columns.addAll(bizLogic.setColumnNames(Constants.COLLECTION_PROTOCOL_REGISTRATION));
    		columns.addAll(bizLogic.setColumnNames(Constants.SPECIMEN_COLLECTION_GROUP));
    		columns.addAll(bizLogic.setColumnNames(Constants.SPECIMEN));
		}
		else if(aliasName.equals(Constants.SPECIMEN_COLLECTION_GROUP))
		{
    		columns.addAll(bizLogic.setColumnNames(Constants.SPECIMEN_COLLECTION_GROUP));
    		columns.addAll(bizLogic.setColumnNames(Constants.SPECIMEN));
		}
		else if(aliasName.equals(Constants.SPECIMEN))
		{
    		columns.addAll(bizLogic.setColumnNames(Constants.SPECIMEN));
		}
		String selectColumnList[] = new String[columns.size()];
    	
    	Iterator columnsItr = columns.iterator();
    	int i=0;
    	while(columnsItr.hasNext())
    	{
    		NameValueBean columnsNameValues = (NameValueBean)columnsItr.next();
    		StringTokenizer columnsTokens = new StringTokenizer(columnsNameValues.getValue(),".");
    		Logger.out.debug("value in namevaluebean:"+columnsNameValues.getValue());
    		int columnId = ((Integer)columnIdsMap.get(columnsTokens.nextToken()+"."+columnsTokens.nextToken())).intValue()-1;
    		selectColumnList[i++]=(Constants.COLUMN+columnId);
    		columnDisplayNames.add(columnsTokens.nextToken());
    	}
    	filteredColumnDisplayNames.addAll(columnDisplayNames);
    	return selectColumnList;
    }

}
