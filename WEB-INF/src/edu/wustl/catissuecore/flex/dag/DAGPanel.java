package edu.wustl.catissuecore.flex.dag;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import edu.common.dynamicextensions.domaininterface.AssociationInterface;
import edu.common.dynamicextensions.domaininterface.AttributeInterface;
import edu.common.dynamicextensions.domaininterface.EntityInterface;
import edu.common.dynamicextensions.exception.DynamicExtensionsApplicationException;
import edu.common.dynamicextensions.exception.DynamicExtensionsSystemException;
import edu.wustl.cab2b.client.ui.dag.PathLink;
import edu.wustl.cab2b.client.ui.dag.ambiguityresolver.AmbiguityObject;
import edu.wustl.cab2b.client.ui.query.ClientQueryBuilder;
import edu.wustl.cab2b.client.ui.query.IClientQueryBuilderInterface;
import edu.wustl.cab2b.client.ui.query.IPathFinder;
import edu.wustl.cab2b.server.cache.EntityCache;
import edu.wustl.catissuecore.applet.AppletConstants;
import edu.wustl.catissuecore.bizlogic.querysuite.CreateQueryObjectBizLogic;
import edu.wustl.catissuecore.bizlogic.querysuite.GenerateHtmlForAddLimitsBizLogic;
import edu.wustl.catissuecore.util.global.Constants;
import edu.wustl.catissuecore.util.querysuite.QueryModuleError;
import edu.wustl.catissuecore.util.querysuite.QueryModuleUtil;
import edu.wustl.common.querysuite.exceptions.CyclicException;
import edu.wustl.common.querysuite.exceptions.MultipleRootsException;
import edu.wustl.common.querysuite.factory.QueryObjectFactory;
import edu.wustl.common.querysuite.metadata.associations.IAssociation;
import edu.wustl.common.querysuite.metadata.associations.IIntraModelAssociation;
import edu.wustl.common.querysuite.metadata.path.IPath;
import edu.wustl.common.querysuite.queryobject.ArithmeticOperator;
import edu.wustl.common.querysuite.queryobject.DSInterval;
import edu.wustl.common.querysuite.queryobject.ICondition;
import edu.wustl.common.querysuite.queryobject.IConnector;
import edu.wustl.common.querysuite.queryobject.IConstraints;
import edu.wustl.common.querysuite.queryobject.ICustomFormula;
import edu.wustl.common.querysuite.queryobject.IDateOffsetAttribute;
import edu.wustl.common.querysuite.queryobject.IDateOffsetLiteral;
import edu.wustl.common.querysuite.queryobject.IExpression;
import edu.wustl.common.querysuite.queryobject.IExpressionAttribute;
import edu.wustl.common.querysuite.queryobject.IExpressionOperand;
import edu.wustl.common.querysuite.queryobject.IJoinGraph;
import edu.wustl.common.querysuite.queryobject.ILiteral;
import edu.wustl.common.querysuite.queryobject.IQuery;
import edu.wustl.common.querysuite.queryobject.IQueryEntity;
import edu.wustl.common.querysuite.queryobject.ITerm;
import edu.wustl.common.querysuite.queryobject.ITimeIntervalEnum;
import edu.wustl.common.querysuite.queryobject.LogicalOperator;
import edu.wustl.common.querysuite.queryobject.RelationalOperator;
import edu.wustl.common.querysuite.queryobject.YMInterval;
import edu.wustl.common.querysuite.queryobject.impl.Expression;
import edu.wustl.common.querysuite.queryobject.impl.JoinGraph;
import edu.wustl.common.querysuite.queryobject.impl.Rule;
import edu.wustl.common.querysuite.queryobject.locator.Position;
import edu.wustl.common.querysuite.queryobject.locator.QueryNodeLocator;
import edu.wustl.common.util.Utility;
import edu.wustl.common.util.logger.Logger;
/**
 *The class is responsibel controlling all activities of Flex DAG
 *  
 *@author aniket_pandit
 */

public class DAGPanel {

	private IClientQueryBuilderInterface m_queryObject;
	private IPathFinder m_pathFinder;
	private IExpression expression;
	private HashMap<String,IPath> m_pathMap = new HashMap<String, IPath>();
	private Map<Integer, Position> positionMap;	
	public DAGPanel(IPathFinder pathFinder)
	{
		m_pathFinder =pathFinder;
	}
	/**
	 * 
	 * @param expressionId
	 * @param isOutputView
	 * @return
	 */
	private DAGNode createNode(int expressionId,boolean isOutputView)
	{
		IExpression expression = m_queryObject.getQuery().getConstraints().getExpression(expressionId);
        IQueryEntity constraintEntity = expression.getQueryEntity();
        DAGNode dagNode = new DAGNode();
		dagNode.setNodeName(edu.wustl.cab2b.common.util.Utility.getOnlyEntityName(constraintEntity.getDynamicExtensionsEntity()));
		dagNode.setExpressionId(expression.getExpressionId());
		if(isOutputView)
		{
			dagNode.setNodeType(DAGConstant.VIEW_ONLY_NODE);
		}
		else
		{
			dagNode.setToolTip(expression);
		}
		return dagNode;
	}

	/**
	 * 
	 * @param strToCreateQueryObject
	 * @param entityName
	 * @param mode
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public DAGNode createQueryObject(String strToCreateQueryObject,String entityName,String mode) 
	{
		Map ruleDetailsMap = null;
		int expressionId ;
		DAGNode node = null;
		
		HttpServletRequest request = flex.messaging.FlexContext.getHttpRequest();
		HttpSession session = request.getSession();
		
		IQuery query = (IQuery)session.getAttribute(DAGConstant.QUERY_OBJECT);// Get existing Query object from server  

		if(query != null)
		{
			m_queryObject.setQuery(query);
		}
		else
		{
			query = m_queryObject.getQuery();
		}
		session.setAttribute(DAGConstant.QUERY_OBJECT, query);

		try {
			Long entityId = Long.parseLong(entityName);
			EntityInterface entity =EntityCache.getCache().getEntityById(entityId);

			CreateQueryObjectBizLogic queryBizLogic = new CreateQueryObjectBizLogic();
			if (!strToCreateQueryObject.equalsIgnoreCase("")) {
				ruleDetailsMap = queryBizLogic.getRuleDetailsMap(strToCreateQueryObject, entity.getEntityAttributesForQuery());
				List<AttributeInterface> attributes = (List<AttributeInterface>) ruleDetailsMap.get(AppletConstants.ATTRIBUTES);
				List<String> attributeOperators = (List<String>) ruleDetailsMap.get(AppletConstants.ATTRIBUTE_OPERATORS);
				List<List<String>> conditionValues = (List<List<String>>) ruleDetailsMap.get(AppletConstants.ATTR_VALUES);
				String errMsg = (String)ruleDetailsMap.get(AppletConstants.ERROR_MESSAGE);
				if(errMsg.equals(""))
				{
					if(mode.equals("Edit"))
					{
						Rule rule = ((Rule) (expression.getOperand(0)));
						rule.removeAllConditions();
						List<ICondition> conditionsList = ((ClientQueryBuilder)m_queryObject).getConditions(attributes, attributeOperators,conditionValues);
						for (ICondition condition : conditionsList)
						{
							rule.addCondition(condition);
						}
						expressionId = expression.getExpressionId();
						node = createNode(expressionId,false);
					}
					else
					{
						expressionId = m_queryObject.addRule(attributes, attributeOperators, conditionValues,entity);
						node = createNode(expressionId,false);
					}
					node.setErrorMsg(errMsg);
				}else
				{
					node= new DAGNode();
					node.setErrorMsg(errMsg);
				}
				
			}
		} catch (DynamicExtensionsSystemException e) {
			e.printStackTrace();
		} catch (DynamicExtensionsApplicationException e) {
			e.printStackTrace();
		}
		return node;
	}

	/**
	 * Sets ClientQueryBuilder object
	 * @param queryObject
	 */
	public void setQueryObject(IClientQueryBuilderInterface queryObject) {
		m_queryObject = queryObject;
	}
	/**
	 * Sets Expression
	 * @param expression
	 */
	public void setExpression(IExpression expression)
	{
		this.expression = expression;
	}

	/**
	 * Links two nodes
	 * @param sourceNode
	 * @param destNode
	 * @param paths
	 */
	public  List<DAGPath> linkNode(final DAGNode sourceNode, final DAGNode destNode,List<IPath> paths) {

		List<DAGPath> dagPathList = null;
		if (paths != null && !paths.isEmpty()) {
			dagPathList = new ArrayList<DAGPath>();
			int sourceExpressionId = sourceNode.getExpressionId();
			int destExpressionId = destNode.getExpressionId();
			if (!m_queryObject.isPathCreatesCyclicGraph(sourceExpressionId, destExpressionId,
					paths.get(0))) {
				for (int i = 0; i < paths.size(); i++) {
					IPath path = paths.get(i);
					LinkTwoNode(sourceNode, destNode, paths.get(i), new ArrayList());
					String pathStr = new Long(path.getPathId()).toString();
					DAGPath dagPath = new DAGPath();
					dagPath.setToolTip(getPathDisplayString(path));
					dagPath.setId(pathStr);
					dagPath.setSourceExpId(sourceNode.getExpressionId());
					dagPath.setDestinationExpId(destNode.getExpressionId());
					dagPathList.add(dagPath);
					String key =pathStr+"_"+sourceNode.getExpressionId()+"_"+destNode.getExpressionId();
					m_pathMap.put(key,path);
				}
			}
		}
		return dagPathList;
	}
	/**
	 * Gets list of paths between two nodes
	 * @param sourceNode
	 * @param destNode
	 * @return
	 */
	public List<IPath> getPaths(DAGNode sourceNode, DAGNode destNode) {
		Map<AmbiguityObject, List<IPath>> map =  null;
		AmbiguityObject ambiguityObject = null;
		try {
			IQuery query = m_queryObject.getQuery();
			IConstraints constraints = query.getConstraints();
			int expressionId = sourceNode.getExpressionId();
			IExpression expression = constraints
			.getExpression(expressionId);
			IQueryEntity sourceEntity = expression
			.getQueryEntity();
			expressionId = destNode.getExpressionId();
			expression = constraints.getExpression(expressionId);
			IQueryEntity destinationEntity = expression
			.getQueryEntity();

			ambiguityObject = new AmbiguityObject(
					sourceEntity.getDynamicExtensionsEntity(),
					destinationEntity.getDynamicExtensionsEntity());
//			ResolveAmbiguity resolveAmbigity = new ResolveAmbiguity(
//			ambiguityObject, m_pathFinder);
			DAGResolveAmbiguity resolveAmbigity = new DAGResolveAmbiguity(ambiguityObject, m_pathFinder);
			map = resolveAmbigity.getPathsForAllAmbiguities();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return map.get(ambiguityObject);
	}
	
	private  AttributeInterface getAttributeIdentifier(IQuery query,DAGNode dagNode, String firstAttributeId)
 	{
		Long identifier = new Long(firstAttributeId);
		//IQuery query = m_queryObject.getQuery();
		IConstraints constraints = query.getConstraints();
		
		int expressionId = dagNode.getExpressionId();
		IExpression expression = constraints.getExpression(expressionId);
		IQueryEntity sourceEntity = expression.getQueryEntity();
		
		AttributeInterface srcAttributeByIdentifier = sourceEntity.getDynamicExtensionsEntity().getAttributeByIdentifier(identifier);
		
		return srcAttributeByIdentifier;
	}
	/**
	 * Gets list of paths between two nodes
	 * @param sourceNode
	 * @param destNode
	 * @param tempQuery 
	 * @return
	 */
	public void formTemporalQuery(DAGNode sourceNode, DAGNode destNode, String tempQuery)
	{
			String[] tokens = tempQuery.split("##");
			String firstAttributeId = tokens[0];
			String firstAttributeDataType = tokens[1];
			String secondAttributeId = tokens[2];		
			String secondAttributeDataType = tokens[3];
			String arithmeticOp = tokens[4];
			String relationalOp = tokens[5];
			String timeValue = tokens[6];
			String timeIntervalValue = tokens[7];
			IDateOffsetAttribute dateOffsetAttr1 = null;
			IDateOffsetAttribute dateOffsetAttr2 = null;
			IExpressionAttribute IExpression1 =  null;
			IExpressionAttribute IExpression2 = null;
			IDateOffsetLiteral dateOffSetLiteral =  null;
			ILiteral dateLiteral = null;
			ITerm lhsTerm = null;
			ITerm rhsTerm = null;
			IConnector iCon = null;
			ICustomFormula customFormula = null;
			IExpression srcIExpression = null;
			IQuery query = m_queryObject.getQuery();
			IConstraints constraints = query.getConstraints();
			int srcExpressionId = sourceNode.getExpressionId();
			IExpression expression = constraints.getExpression(srcExpressionId);
			srcIExpression = expression;
			AttributeInterface srcAttributeByIdentifier = getAttributeIdentifier(query,sourceNode,firstAttributeId);
			int destExpressionId = destNode.getExpressionId();
			AttributeInterface destAttributeByIdentifier = getAttributeIdentifier(query,destNode,secondAttributeId);
			ArithmeticOperator arithOp = getArithmeticOperator(arithmeticOp);
			RelationalOperator relOp = getRelationalOperator(relationalOp); 
			IExpression destExpression = constraints.getExpression(destExpressionId);
			if(firstAttributeDataType.equals(secondAttributeDataType))
			{
				IExpression1 = QueryObjectFactory.createExpressionAttribute(expression,srcAttributeByIdentifier);
				IExpression2 = QueryObjectFactory.createExpressionAttribute(destExpression,destAttributeByIdentifier);
			}
			else
			{
			    if((firstAttributeDataType.equals(Constants.DATE_TYPE)))    
			    {
			    	IExpression1 = QueryObjectFactory.createExpressionAttribute(expression,srcAttributeByIdentifier);
			    	dateOffsetAttr2 = QueryObjectFactory.createDateOffsetAttribute(destExpression,destAttributeByIdentifier,DSInterval.Day);
			    }
			    else
			    {
			    	IExpression2 = QueryObjectFactory.createExpressionAttribute(destExpression,destAttributeByIdentifier);
			    	dateOffsetAttr1 = QueryObjectFactory.createDateOffsetAttribute(destExpression,destAttributeByIdentifier,DSInterval.Day);
			    }
			}
			
			iCon = QueryObjectFactory.createArithmeticConnector(arithOp);
			if((timeValue.equals("null")) && (timeIntervalValue.equals("null")))
			{
				lhsTerm = createOnlyLHS(dateOffsetAttr1, dateOffsetAttr2, IExpression1, IExpression2, iCon);
			}
			else
			{	
				//Creating IDateOffSetLiteral
				ITimeIntervalEnum timeInterval = null;
				if((!timeIntervalValue.equals("null")) && (timeValue != null))
				{
					timeInterval = getTimeInterval(timeIntervalValue, timeInterval);
					if(timeInterval instanceof DSInterval) {
						dateOffSetLiteral = QueryObjectFactory.createDateOffsetLiteral(timeValue, (DSInterval)timeInterval);
					} else if(timeInterval instanceof YMInterval) {
						dateOffSetLiteral = QueryObjectFactory.createDateOffsetLiteral(timeValue, (YMInterval)timeInterval);
					} else {
						throw new RuntimeException("can't occur.");
					}
				}
				else
				{
					//It will be a date, We need to create a Literal
					if(timeValue != null) {
						Date time = Calendar.getInstance().getTime();
						
						dateLiteral = QueryObjectFactory.createDateLiteral(new java.sql.Date(time.getTime()));
					}
				}
				
				//Creating left ITERM and Right ITERM 
				lhsTerm = QueryObjectFactory.createTerm();
			    rhsTerm = 	QueryObjectFactory.createTerm();
			    //This is the case when both attributes are either Date or Integer
				if(IExpression1 != null && IExpression2 != null)
				{
					lhsTerm.addOperand(IExpression1);
				    lhsTerm.addOperand(iCon,dateOffSetLiteral);
				    rhsTerm.addOperand(IExpression2);
				}
				else
				{
	                //This is the case when There is first attribute is of type Date and other is of type Integer
					if(IExpression1 != null && dateOffsetAttr2 != null)
					{
						lhsTerm.addOperand(IExpression1);
					    lhsTerm.addOperand(iCon,dateOffsetAttr2);
					    if(dateLiteral != null)
					    {
					    	rhsTerm.addOperand(dateLiteral);
					    }
					}
					else
					{
						//This is the case First node attribute is of type Integer and second node attribute is of Integer
						lhsTerm.addOperand(dateOffsetAttr1);
						lhsTerm.addOperand(iCon,IExpression2);
						if(dateLiteral != null)
						{
							rhsTerm.addOperand(dateLiteral);
						}
					}
				}
			}
			
		    //Here we create the custom formula 
			customFormula = getCustomFormula(lhsTerm, rhsTerm, relOp);
			//Adding custom formula to src node
			srcIExpression.addOperand(getAndConnector(),customFormula);
			srcIExpression.setInView(true);
		
	}
	/**
	 * 
	 * @param dateOffsetAttr1
	 * @param dateOffsetAttr2
	 * @param IExpression1
	 * @param IExpression2
	 * @param iCon
	 * @return
	 */
	private ITerm createOnlyLHS(IDateOffsetAttribute dateOffsetAttr1, IDateOffsetAttribute dateOffsetAttr2, IExpressionAttribute IExpression1, IExpressionAttribute IExpression2, IConnector iCon)
	{
		ITerm lhsTerm;
		//Then the custom formula will not have RHS
		lhsTerm = QueryObjectFactory.createTerm();
		if(IExpression1 != null && IExpression2 != null)
		{
			lhsTerm.addOperand(IExpression1);
		    lhsTerm.addOperand(iCon,IExpression2);
		}
		else
		{
			if(IExpression1 != null && dateOffsetAttr2 != null)
			{
				 lhsTerm.addOperand(IExpression1);
				 lhsTerm.addOperand(iCon,dateOffsetAttr2);
			}
			else
			{
		        lhsTerm.addOperand(IExpression2);
		        lhsTerm.addOperand(iCon,dateOffsetAttr1);
			}
		}
		return lhsTerm;
	}
	/**
	 * 
	 * @param lhsTerm
	 * @param rhsTerm
	 * @param relOp
	 * @return
	 */
	private ICustomFormula getCustomFormula(ITerm lhsTerm, ITerm rhsTerm, RelationalOperator relOp)
	{
		ICustomFormula customFormula = QueryObjectFactory.createCustomFormula();
		if(rhsTerm == null)
		{ 
			//Then custom formula will have only lhs and relational Operator
			customFormula.setLhs(lhsTerm);
			customFormula.setOperator(relOp);
		}
		else
		{
			customFormula.setLhs(lhsTerm);
			customFormula.addRhs(rhsTerm);
			customFormula.setOperator(relOp);
		}
		return customFormula;
	}
	private ITimeIntervalEnum getTimeInterval(String timeIntervalValue, ITimeIntervalEnum timeInterval)
	{
		for(DSInterval time: DSInterval.values())
		{
			if(timeIntervalValue.equals(time.name()))
			{
				timeInterval = time;
				break;
			}
		}
		
		if(timeInterval == null)
		{
			for(YMInterval time : YMInterval.values())
			{
				if(timeIntervalValue.equals(time.name()))
				{
					timeInterval = time;
					break;
				}
			}
		}
		return timeInterval;
	}
	private RelationalOperator getRelationalOperator(String relationalOp)
	{
		RelationalOperator relOp = null;
		for(RelationalOperator operator : RelationalOperator.values())
		{
			if((operator.getStringRepresentation().equals(relationalOp)))
			{
				relOp = operator;
				break;
			}
		}
		return relOp;
	}
	private ArithmeticOperator getArithmeticOperator(String arithmeticOp)
	{
		ArithmeticOperator arithOp = null;
		for(ArithmeticOperator operator :ArithmeticOperator.values())
		{
			if(operator.mathString().equals(arithmeticOp))
			{
				arithOp = operator;
				break;
			}
		}
		return arithOp;
	}
	
	 private static IConnector<LogicalOperator> getAndConnector() {
	        return QueryObjectFactory.createLogicalConnector(LogicalOperator.And);
	    }
	
	public boolean checkForValidAttributes(List<DAGNode> linkedNodeList)
	{
		boolean areNodesValid = false;
		DAGNode sourceNode = linkedNodeList.get(0);
		DAGNode destinationNode = linkedNodeList.get(1);
		boolean isSourceNodeValid = checkIfValidNode(sourceNode);
		boolean isDestNodeValid = checkIfValidNode(destinationNode);
		
		if((isSourceNodeValid && isDestNodeValid))
		{
			areNodesValid = true;
		}
	    
		return areNodesValid;
		
	}
	private boolean checkIfValidNode(DAGNode sourceNode)
	{
		boolean isValid = false;
		IQuery query = m_queryObject.getQuery();
		IConstraints constraints = query.getConstraints();
		int expressionId = sourceNode.getExpressionId();
		IExpression expression = constraints.getExpression(expressionId);
		
		/**
		 * Checking if source node has a Date attribute
		 */
		IQueryEntity sourceEntity = expression.getQueryEntity();
		Collection<AttributeInterface> sourceAttributeCollection = sourceEntity.getDynamicExtensionsEntity().getEntityAttributesForQuery();
		
		for(AttributeInterface attribute : sourceAttributeCollection)
		{
		   if(((attribute.getDataType().equals(Constants.DATE_TYPE) || 
				   attribute.getDataType().equals(Constants.INTEGER_TYPE) || 
				   attribute.getDataType().equals(Constants.DOUBLE_TYPE) || 
				   attribute.getDataType().equals(Constants.LONG_TYPE) || 
				   attribute.getDataType().equals(Constants.FLOAT_TYPE)) ||
				   attribute.getDataType().equals(Constants.SHORT_TYPE)) && 
				   (!attribute.getName().equals("id")))
		   {
			   isValid = true;
		   }
	    }
		return isValid;
	}
	private Collection<AttributeInterface> getAttributeCollection(DAGNode node)
	{
		IQuery query = m_queryObject.getQuery();
		IConstraints constraints = query.getConstraints();
		int expressionId =node.getExpressionId();
		IExpression expression = constraints.getExpression(expressionId);
		IQueryEntity sourceEntity = expression.getQueryEntity();
		Collection<AttributeInterface> sourceAttributeCollection = sourceEntity.getDynamicExtensionsEntity().getEntityAttributesForQuery();
		return sourceAttributeCollection;
	
	}
	public Map getQueryData(DAGNode sourceNode, DAGNode destNode)
	{ 
		Map <String, Object> queryDataMap= new HashMap<String, Object>();
		Map <String,List<String>>sourceNodeAttributesMap = new HashMap<String, List<String>>(); 
	    Map <String,List<String >>destNodeAttributesMap = new HashMap<String,List<String>>(); 
	    List<String> entityLabelsList = getEntityLabelsList(sourceNode, destNode);
 		Collection<AttributeInterface> sourceAttributeCollection = getAttributeCollection(sourceNode);
		populateMap(sourceNodeAttributesMap, sourceAttributeCollection);
		
		Collection<AttributeInterface> destAttributeCollection = getAttributeCollection(destNode);
		populateMap(destNodeAttributesMap, destAttributeCollection);
		List<String> arithmeticOperaorsList = getArithmeticOperators();
		List<String> relationalOperatorsList = getRelationalOperators();
		List<String> timeIntervalList = getTimeIntervals();
		
		queryDataMap.put(Constants.FIRST_NODE_ATTRIBUTES,sourceNodeAttributesMap);
		queryDataMap.put(Constants.ARITHMETIC_OPERATORS,arithmeticOperaorsList);
		queryDataMap.put(Constants.SECOND_NODE_ATTRIBUTES,destNodeAttributesMap);
		queryDataMap.put(Constants.RELATIONAL_OPERATORS,relationalOperatorsList);
		queryDataMap.put("timeIntervals",timeIntervalList);
		queryDataMap.put("entityList",entityLabelsList);
		
		return queryDataMap;
	}
	private List<String> getEntityLabelsList(DAGNode sourceNode, DAGNode destNode)
	{
		List <String> entityList = new ArrayList<String>();
        
	    String srcNodename = Utility.getDisplayLabel(sourceNode.getNodeName());
 		String destNodeName = Utility.getDisplayLabel(destNode.getNodeName());
 		
 		entityList.add(0,srcNodename);
 		entityList.add(1,destNodeName);
		return entityList;
	}
	private void populateMap(Map<String, List<String>> destNodeAttributesMap, Collection<AttributeInterface> destAttributeCollection)
	{
		List<String> destNodeList;
		/**
		 * Storing all attributes of destination entity having DataType as Date
		 */
		for(AttributeInterface attribute : destAttributeCollection)
        {
        	 String destDataType  = attribute.getDataType();
			if(destDataType.equals(Constants.DATE_TYPE))
			{
        		destNodeList = new ArrayList<String>();
				//Putting attribute name and attribute data type in Map
        		destNodeList.add(0,attribute.getId().toString());
        		destNodeList.add(1,attribute.getDataType());
        		destNodeAttributesMap.put(attribute.getName(),destNodeList);
			}
			else
			{
				if((destDataType.equals(Constants.INTEGER_TYPE) || destDataType.equals(Constants.LONG_TYPE) || destDataType.equals(Constants.DOUBLE_TYPE)|| destDataType.equals(Constants.FLOAT_TYPE) || destDataType.equals(Constants.SHORT_TYPE)) && (!attribute.getName().equals("id")))
				{
					destNodeList = new ArrayList<String>();
					destNodeList.add(0,attribute.getId().toString());
					destNodeList.add(1,Constants.INTEGER_TYPE);
					destNodeAttributesMap.put(attribute.getName(),destNodeList);
				}
			}
        }
		
		//Adding "Today" as default attribute and id is null and type is Date
		/*destNodeList = new ArrayList<String>();
		destNodeList.add(0,null);
		destNodeList.add(1,"Date");
		destNodeAttributesMap.put("today",destNodeList);
	       */  	
	}
	private List<String> getTimeIntervals()
	{
		List <String>timeIntervalList = new ArrayList<String>(); 
		/**
		 * Getting all days time Intervals
		 */
		for(DSInterval timeInterval : DSInterval.values())
		{
			timeIntervalList.add(timeInterval.name());
		}
		
		for(YMInterval timeInterval1 : YMInterval.values())
		{
			timeIntervalList.add(timeInterval1.name());
		}
		return timeIntervalList;
	}
	private List<String> getRelationalOperators()
	{
		/**
		 * Getting relational operators excluding those deals with Strings
		 */
		 List <String>relationalOperatorsList = new ArrayList<String>(); 
		for(RelationalOperator operator : RelationalOperator.values())
		{
			if((!operator.getStringRepresentation().equals(Constants.Contains)) &&
					(!operator.getStringRepresentation().equals(Constants.STRATS_WITH)) &&
					(!operator.getStringRepresentation().equals(Constants.ENDS_WITH)) && 
					(!operator.getStringRepresentation().equals(Constants.In)) &&
					(!operator.getStringRepresentation().equals(Constants.Between)) &&
					(!operator.getStringRepresentation().equals(Constants.Not_In)) )
			{
				relationalOperatorsList.add(operator.getStringRepresentation());	
			}
		}
		return relationalOperatorsList;
	}
	private List<String> getArithmeticOperators()
	{
		List <String>arithmeticOperaorsList =  new ArrayList<String>();
		/**
		 * Getting all arithmetic operators
		 */
		for(ArithmeticOperator operator : ArithmeticOperator.values())
		{
			if((!operator.mathString().equals("")) && (!operator.mathString().equals("*")) && (!operator.mathString().equals("/")))
			{
				arithmeticOperaorsList.add(operator.mathString());
			}
		}
		return arithmeticOperaorsList;
	}
	
	/**
	 * This method removes the Custom formula from query on delete of custom Node 
	 * @throws MultipleRootsException
	 */
	public void removeCustomFormula()	
	{
		IQuery query = m_queryObject.getQuery();
		IConstraints c = query.getConstraints();
		for(IExpression expression2 : c) {
			int numberOfOperands = expression2.numberOfOperands();
			for(int i=0;i<numberOfOperands;i++)
			{
				IExpressionOperand operand = expression2.getOperand(i);
				if(operand instanceof ICustomFormula)
				{
					expression2.removeOperand(i);
					break;
				}
			}
		}
	}
	/**
	 * Link 2 nodes
	 * @param sourceNode
	 * @param destNode
	 * @param path
	 * @param intermediateExpressions
	 */
	private void LinkTwoNode(final DAGNode sourceNode, final DAGNode destNode, final IPath path,
			List<Integer> intermediateExpressions) {

		try {
			int sourceexpressionId = sourceNode.getExpressionId();
			int destexpressionId = destNode.getExpressionId();
			intermediateExpressions = m_queryObject.addPath(sourceexpressionId,
					destexpressionId, path);
			//DAGPath dagpath  
			PathLink link = new PathLink();
			link.setAssociationExpressions(intermediateExpressions);
			link.setDestinationExpressionId(destexpressionId);
			link.setSourceExpressionId(sourceexpressionId);
			link.setPath(path);
			updateQueryObject(link,sourceNode, destNode);
		} catch (CyclicException e) {
			e.printStackTrace();

		}
	}

	/**
	 * Updates query object
	 * @param link
	 * @param sourceNode
	 * @param destNode
	 */
	private void updateQueryObject(PathLink link, DAGNode sourceNode, DAGNode destNode) {
		//TODO required to modify code logic will not work for multiple association
		int sourceexpressionId =sourceNode.getExpressionId();
		//IExpressionId destexpressionId = new ExpressionId(destNode
				//.getExpressionId());

		// If the first association is added, put operator between attribute condition and association
		String operator = null;
		// if (sourcePort == null) {
		operator = sourceNode.getOperatorBetweenAttrAndAssociation();
		//} else { // Get the logical operator associated with previous association
		//   operator = sourceNode.getLogicalOperator(sourcePort);
		// }

		// Get the expressionId between which to add logical operator
		int destId = link.getLogicalConnectorExpressionId();

		m_queryObject.setLogicalConnector(sourceexpressionId, destId,
				edu.wustl.cab2b.client.ui.query.Utility.getLogicalOperator(operator), false);

		// Put appropriate parenthesis
		// The code is required for multiple associations
		//if (sourcePort != null) {
		//IExpressionId previousExpId = link.getLogicalConnectorExpressionId();
		//m_queryObject.addParantheses(sourceexpressionId, previousExpId, destId);
		// }
	}
	/**
	 * Gets display path string
	 * @param path
	 * @return
	 */
	public static String getPathDisplayString(IPath path) {

		String text = "";
		List<IAssociation> pathList = path.getIntermediateAssociations();
		text = text.concat(edu.wustl.cab2b.common.util.Utility.getOnlyEntityName(path.getSourceEntity()));
		for (int i = 0; i < pathList.size(); i++) {
			text = text.concat(">>");
			IAssociation association = pathList.get(i);
			if (association instanceof IIntraModelAssociation)
			{
				IIntraModelAssociation iAssociation = (IIntraModelAssociation)association;
				AssociationInterface dynamicExtensionsAssociation = iAssociation.getDynamicExtensionsAssociation();
				String role = "("+dynamicExtensionsAssociation.getTargetRole().getName()+")";
				text = text.concat(role+">>");
			}
			text = text.concat(edu.wustl.cab2b.common.util.Utility.getOnlyEntityName(association.getTargetEntity()));
		}
		Logger.out.debug(text );
		return text;
		
		
	}

	/**
	 * Generates sql query
	 * @return
	 */
	public int search()
	{
		QueryModuleError status = QueryModuleError.SUCCESS;
		IQuery query = m_queryObject.getQuery();
		HttpServletRequest request = flex.messaging.FlexContext.getHttpRequest();
		boolean isRulePresentInDag = QueryModuleUtil.checkIfRulePresentInDag(query) ;
		if (isRulePresentInDag)
		{
			status=QueryModuleUtil.searchQuery(request, query,null);
		}
		else
		{
			status = QueryModuleError.EMPTY_DAG;
		}		
		return status.getErrorCode();
	}
	/**
	 * Repaints DAG
	 * @return
	 */
	public List<DAGNode> repaintDAG()
	{
		List<DAGNode> nodeList = new ArrayList<DAGNode>();
		HttpServletRequest request = flex.messaging.FlexContext.getHttpRequest();
		HttpSession session = request.getSession();
		IQuery query =(IQuery)session.getAttribute(DAGConstant.QUERY_OBJECT);
		m_queryObject.setQuery(query);
		IConstraints constraints = query.getConstraints();

		positionMap = new QueryNodeLocator(400,query).getPositionMap();

		HashSet<Integer> visibleExpression = new HashSet<Integer>();

		//Enumeration<IExpressionId> expressionIds = constraints.getExpressionIds();
		for(IExpression expression :constraints)
	//	while(expressionIds.hasMoreElements())
		//{
			//IExpressionId id = expressionIds.nextElement();
			//IExpression expression  = constraints.getExpression(id);
			if(expression.isVisible())
			{
				visibleExpression.add(new Integer(expression.getExpressionId()));
			}
	//	}
		for(Integer expressionId:visibleExpression){

			IExpression exp = constraints.getExpression(expressionId.intValue());
			
			IQueryEntity constraintEntity = exp.getQueryEntity();
			String nodeDisplayName = edu.wustl.cab2b.common.util.Utility.getOnlyEntityName(constraintEntity.getDynamicExtensionsEntity()); 
			DAGNode dagNode = new DAGNode();
			dagNode.setExpressionId(exp.getExpressionId());
			dagNode.setNodeName(nodeDisplayName);
			dagNode.setToolTip(exp);
			Position position = positionMap.get(exp.getExpressionId());
			if (position!=null)
			{
				dagNode.setX(position.getX());
				dagNode.setY(position.getY());
			}
			if(!exp.containsRule())
			{
				dagNode.setNodeType(DAGConstant.VIEW_ONLY_NODE);
			}
			if(!exp.isInView())
			{
				dagNode.setNodeType(DAGConstant.CONSTRAINT_ONLY_NODE);
			}
			
			nodeform(expressionId,dagNode,constraints,new ArrayList<IIntraModelAssociation>());
			
			int numOperands = exp.numberOfOperands();
			int numOperator = numOperands-1;
			for(int i=0;i<numOperator;i++)
			{
				String operator  = exp.getConnector(i, i+1).getOperator().toString();
				dagNode.setOperatorList(operator.toUpperCase());
			}
			nodeList.add(dagNode);
		}
		return nodeList;

	}
	/**
	 * 
	 * @param expressionId
	 * @param node
	 * @param constraints
	 * @param path
	 */
	private void nodeform(int expressionId,DAGNode node,IConstraints constraints,List<IIntraModelAssociation> intraModelAssociationList)
	{
		IJoinGraph graph = constraints.getJoinGraph();
		IExpression expression = constraints.getExpression(expressionId);
		
		List<IExpression> childList = graph.getChildrenList(expression);
		
		for(IExpression exp :childList)
		{
			
			//int newExpId = childList.get(i);
			//IExpression exp = constraints.getExpression(newExpId);
			IQueryEntity constraintEntity = exp.getQueryEntity();
		/*	Code to get IPath Object*/
		 	IIntraModelAssociation association  =(IIntraModelAssociation)(graph.getAssociation(expression,exp));
		 	
		 	intraModelAssociationList.add(association);
						
			if(exp.isVisible())
			{
				IPath pathObj = (IPath)m_pathFinder.getPathForAssociations(intraModelAssociationList);
				long pathId =pathObj.getPathId();
					
				DAGNode dagNode = new DAGNode();
				dagNode.setExpressionId(exp.getExpressionId());
				dagNode.setNodeName(edu.wustl.cab2b.common.util.Utility.getOnlyEntityName(constraintEntity.getDynamicExtensionsEntity()));
				dagNode.setToolTip(exp);
				
			/*	Adding Dag Path in each visible list which have childrens*/
				String pathStr =new Long(pathId).toString();
			  	DAGPath dagPath  = new DAGPath();
				dagPath.setToolTip(getPathDisplayString(pathObj));
				dagPath.setId(pathStr);
				dagPath.setSourceExpId(node.getExpressionId());
				dagPath.setDestinationExpId(dagNode.getExpressionId());
				
				String key =pathStr+"_"+node.getExpressionId()+"_"+dagNode.getExpressionId();
				m_pathMap.put(key,pathObj);
				
				node.setDagpathList(dagPath);
				node.setAssociationList(dagNode);
				intraModelAssociationList.clear();
		
			}
			else
			{
				nodeform(exp.getExpressionId(),node,constraints,intraModelAssociationList);
			}
		}
	}
	/**
	 * 
	 * @param parentExpId
	 * @param parentIndex
	 * @param operator
	 */

	public void updateLogicalOperator(int parentExpId,int parentIndex,String operator )
	{
		int parentExpressionId =parentExpId;
		IQuery query = m_queryObject.getQuery();
		IExpression parentExpression = query.getConstraints().getExpression(parentExpressionId);
		LogicalOperator logicOperator = edu.wustl.cab2b.client.ui.query.Utility.getLogicalOperator(operator);
		int childIndex = parentIndex +1;
		parentExpression.setConnector(parentIndex, childIndex,QueryObjectFactory.createLogicalConnector(logicOperator));
		m_queryObject.setQuery(query);

	}
	/**
	 * 
	 * @param expId
	 * @return
	 */
	public Map editAddLimitUI(int expId)
	{
		Map<String, Object> map = new HashMap<String,Object>();
		int expressionId =expId;
		IExpression expression = m_queryObject.getQuery().getConstraints().getExpression(expressionId);
		EntityInterface entity = expression.getQueryEntity().getDynamicExtensionsEntity();
		GenerateHtmlForAddLimitsBizLogic generateHTMLBizLogic = new GenerateHtmlForAddLimitsBizLogic();
		Rule rule = ((Rule) (expression.getOperand(0)));
		List<ICondition> conditions = rule.getConditions();
		String html = generateHTMLBizLogic.generateHTML(entity, conditions);
		map.put(DAGConstant.HTML_STR, html);
		map.put(DAGConstant.EXPRESSION, expression);
		return map;
	}
	/**
	 * 
	 * @param nodesStr
	 * @return
	 */
	public DAGNode addNodeToOutPutView(String id)
	{
		DAGNode node = null;
		if (!id.equalsIgnoreCase(""))
		{
			Long entityId = Long.parseLong(id);
			EntityInterface entity =EntityCache.getCache().getEntityById(entityId);
			int expressionId = ((ClientQueryBuilder)m_queryObject).addExpression(entity);
			node = createNode(expressionId,true);
		}
		return node;
	}
	/**
	 * 
	 *
	 */
	public void restoreQueryObject()
	{
		HttpServletRequest request = flex.messaging.FlexContext.getHttpRequest();
		HttpSession session = request.getSession();
		IQuery query = (IQuery) session.getAttribute(DAGConstant.QUERY_OBJECT);
		m_queryObject.setQuery(query);
	}
	/**
	 * 
	 * @param expId
	 */
	public void deleteExpression(int expId)
	{
		int expressionId =expId;
		m_queryObject.removeExpression(expressionId);
	}
	/**
	 * 
	 * @param expId
	 */
	public void addExpressionToView(int expId) 
	{
		int expressionId =expId;
		Expression expression = (Expression) m_queryObject.getQuery().getConstraints().getExpression(expressionId);
		expression.setInView(true);
	}
	/**
	 * 
	 * @param expId
	 */
	public void deleteExpressionFormView(int expId)
	{
		int expressionId =expId;
		Expression expression = (Expression) m_queryObject.getQuery().getConstraints().getExpression(expressionId);
		expression.setInView(false);
	}
	/**
	 * 
	 * @param path
	 * @param linkedNodeList
	 */
	public void deletePath(String pathName,List<DAGNode>linkedNodeList)
	{
		IPath path = m_pathMap.remove(pathName);
		int sourceexpressionId =linkedNodeList.get(0)
					.getExpressionId();
		int destexpressionId =linkedNodeList.get(1)
					.getExpressionId();
		List<IAssociation> associations =path.getIntermediateAssociations();
		IConstraints constraints = m_queryObject.getQuery().getConstraints();
		JoinGraph graph =(JoinGraph)constraints.getJoinGraph();
		IExpression srcExpression = constraints.getExpression(sourceexpressionId);
		IExpression destExpression = constraints.getExpression(destexpressionId);
		List<IExpression> expressions = graph.getIntermediateExpressions(srcExpression, destExpression, associations);
		// If the association is direct association, remove the respective association 
		if (0 == expressions.size()) {
			m_queryObject.removeAssociation(sourceexpressionId, destexpressionId);
		} else {
			for (int i = 0; i < expressions.size(); i++) {
				m_queryObject.removeExpression(expression.getExpressionId());
			}
		}

	}

}
