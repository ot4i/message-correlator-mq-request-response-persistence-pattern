/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and other Contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - initial implementation
 *******************************************************************************/
package com.ibm.etools.mft.pattern.mbi.mrrc.relmq.code;

import com.ibm.broker.config.appdev.ESQLModule;
import com.ibm.broker.config.appdev.FlowProperty;
import com.ibm.broker.config.appdev.MessageFlow;
import com.ibm.broker.config.appdev.SubFlowNode;
import com.ibm.broker.config.appdev.UserDefinedProperty;
import com.ibm.broker.config.appdev.nodes.ComputeNode;
import com.ibm.broker.config.appdev.nodes.FilterNode;
import com.ibm.broker.config.appdev.nodes.FlowOrderNode;
import com.ibm.broker.config.appdev.nodes.MQInputNode;
import com.ibm.broker.config.appdev.nodes.MQOutputNode;
import com.ibm.broker.config.appdev.nodes.MQOutputNode.ENUM_MQOUTPUT_PERSISTENCEMODE;
import com.ibm.broker.config.appdev.nodes.MQOutputNode.ENUM_MQOUTPUT_TRANSACTIONMODE;
import com.ibm.broker.config.appdev.nodes.ThrowNode;
import com.ibm.broker.config.appdev.patterns.GeneratePatternInstanceTransform;
import com.ibm.broker.config.appdev.patterns.PatternInstanceManager;

public class PatternGenerator implements GeneratePatternInstanceTransform {
	
	
	private final static String PROJECT_NAME = "MC_MQ_RequestResponse_Persistence";
	
	/** Properties **/
	private final static String PROPERTY_INPUT_DATA_TYPE = "pp3";
	private final static String PROPERTY_RESPONSE_DATA_TYPE = "pp10";
	private final static String PROPERTY_INPUT_VALIDATION = "pp4";
	private final static String PROPERTY_RESPONSE_VALIDATION = "pp16";
	private final static String PROPERTY_LOGGING_REQUIRED = "pp17";
	private final static String PROPERTY_ERROR_HANDLING_REQUIRED = "pp20";
	private final static String PROPERTY_ERROR_HANDLING_Q = "pp19";
	private final static String PROPERTY_ERROR_HANDLING_QM = "pp18";
	
	/** Flows **/
	private final static String FLOW_REQUEST = "mqsi/Request.msgflow";
	private final static String FLOW_RESPONSE= "mqsi/Response.msgflow";
	private final static String FLOW_REQUEST_PROCESSOR = "mqsi/RequestProcessor.subflow";
	private final static String FLOW_RESPONSE_PROCESSOR = "mqsi/ResponseProcessor.subflow";
	private final static String SUBFLOW_LOG = "mqsi/Log.subflow";
	private final static String SUBFLOW_ERROR = "mqsi/Error.subflow";
	
	/** Nodes **/
	private final static String NODE_REQUEST_READ_REQUEST = "Read Request";
	private final static String NODE_REQUEST_SAVE_FIRST = "Save First";
	private final static String NODE_REQUEST_REQUEST_PROCESSOR = "Request Processor";
	private final static String NODE_REQUEST_LOG_AFTER_PROCESSING = "Log after processing";
	private final static String NODE_REQUEST_LOG = "Log";
	
	private final static String NODE_RESPONSE_MQINPUT = "Response";
	private final static String NODE_RESPONSE_RESPONSE_PROCESSOR = "Response Processor";
	private final static String NODE_RESPONSE_LOG_AFTER_REPLY = "Log after Reply";
	private final static String NODE_RESPONSE_LOG = "Log";
	private final static String NODE_RESPONSE_INITIALISE = "Initialise";
	
	private final static String NODE_ERROR_REPLY_FIRST = "Reply First";
	private final static String NODE_ERROR_THROW = "Throw";
	private final static String NODE_ERROR_ERROR_LOGGING_ON = "Error Logging On?";
	private final static String NODE_ERROR_BUILD_ERROR_MESSAGE = "Build Error Message";
	private final static String NODE_ERROR_ERROR_OUTPUT = "Error Output";
	
	@Override
	public void onGeneratePatternInstance(PatternInstanceManager patternInstanceManager) {
		
		// The location for the generated projects 
		String location = patternInstanceManager.getWorkspaceLocation();
		
		// The pattern instance name for this generation
		String patternInstanceName = patternInstanceManager.getPatternInstanceName();
		
		MessageFlow requestMessageFlow = patternInstanceManager.getMessageFlow(PROJECT_NAME, FLOW_REQUEST);
		MessageFlow responseMessageFlow = patternInstanceManager.getMessageFlow(PROJECT_NAME, FLOW_RESPONSE);
		
		//Get Common Nodes
		MQInputNode requestMQInput = (MQInputNode) requestMessageFlow.getNodeByName(NODE_REQUEST_READ_REQUEST);
		FlowOrderNode saveFirstNode = (FlowOrderNode) requestMessageFlow.getNodeByName(NODE_REQUEST_SAVE_FIRST);
		SubFlowNode requestProcessor = (SubFlowNode) requestMessageFlow.getNodeByName(NODE_REQUEST_REQUEST_PROCESSOR);
		
		MQInputNode responseMQInput = (MQInputNode) responseMessageFlow.getNodeByName(NODE_RESPONSE_MQINPUT);
		ComputeNode initialiseNode = (ComputeNode) responseMessageFlow.getNodeByName(NODE_RESPONSE_INITIALISE);
		SubFlowNode responseProcessor = (SubFlowNode) responseMessageFlow.getNodeByName(NODE_RESPONSE_RESPONSE_PROCESSOR);
		
		//Assign Subflows to Subflow Nodes
		requestProcessor.setSubFlow(patternInstanceManager.getMessageFlow(PROJECT_NAME, FLOW_REQUEST_PROCESSOR));
		responseProcessor.setSubFlow(patternInstanceManager.getMessageFlow(PROJECT_NAME, FLOW_RESPONSE_PROCESSOR));
		
		// Set Messages types
		String inputDataType = patternInstanceManager.getParameterValue(PROPERTY_INPUT_DATA_TYPE);
		String responseDataType = patternInstanceManager.getParameterValue(PROPERTY_RESPONSE_DATA_TYPE);
		
		
		if (!patternInstanceManager.getParameterValue(PROPERTY_INPUT_VALIDATION).equals("none")) {
			if (inputDataType.equals("xml")) {
				requestMQInput.setMessageDomainProperty("XMLNSC");
			} else if (inputDataType.equals("binary") || inputDataType.equals("text")) {
				requestMQInput.setMessageDomainProperty("MRM");
			}
		}	
		
		if (!patternInstanceManager.getParameterValue(PROPERTY_RESPONSE_VALIDATION).equals("none")) {
			if (responseDataType.equals("xml")) {
				responseMQInput.setMessageDomainProperty("XMLNSC");
			} else if (responseDataType.equals("binary") || responseDataType.equals("text")) {
				responseMQInput.setMessageDomainProperty("MRM");
			}
		}
		

		// Logging Required?
		MessageFlow logMessageFlow = patternInstanceManager.getMessageFlow(PROJECT_NAME, SUBFLOW_LOG);
		boolean loggingRequired = patternInstanceManager.getParameterValue(PROPERTY_LOGGING_REQUIRED).equals("true");
		if (!loggingRequired) {
				patternInstanceManager.removeMessageFlow(logMessageFlow);
				
				requestMessageFlow.connect(saveFirstNode.OUTPUT_TERMINAL_SECOND, requestProcessor.getInputTerminal("in"));
				responseMessageFlow.connect(initialiseNode.OUTPUT_TERMINAL_OUT, responseProcessor.getInputTerminal("in"));
		} else {
			// Add user defined properties
			UserDefinedProperty requestLoggingUDP = new UserDefinedProperty("Basic", "RequestLoggingOn", 
					FlowProperty.Usage.MANDATORY, FlowProperty.Type.BOOLEAN, true);
			requestMessageFlow.addFlowProperty(requestLoggingUDP);
			
			UserDefinedProperty responseLoggingUDP = new UserDefinedProperty("Basic", "ResponseLoggingOn", 
					FlowProperty.Usage.MANDATORY, FlowProperty.Type.BOOLEAN, true);
			responseMessageFlow.addFlowProperty(responseLoggingUDP);
			
			

			// Add Log to Request flow
			FlowOrderNode logAfterProcessingNode = new FlowOrderNode();
			logAfterProcessingNode.setNodeName(NODE_REQUEST_LOG_AFTER_PROCESSING);
			requestMessageFlow.addNode(logAfterProcessingNode);
			requestMessageFlow.connect(saveFirstNode.OUTPUT_TERMINAL_SECOND, logAfterProcessingNode.INPUT_TERMINAL_IN);
			requestMessageFlow.connect(logAfterProcessingNode.OUTPUT_TERMINAL_FIRST, requestProcessor.getInputTerminal("in"));
			
			SubFlowNode requestLogSubFlowNode = new SubFlowNode();
			requestLogSubFlowNode.setNodeName(NODE_REQUEST_LOG);
			requestLogSubFlowNode.setSubFlow(logMessageFlow);
			requestMessageFlow.addNode(requestLogSubFlowNode);
			requestMessageFlow.connect(logAfterProcessingNode.OUTPUT_TERMINAL_SECOND, requestLogSubFlowNode.getInputTerminal("Input"));
			
			
			// Add Log to Response flow
			FlowOrderNode logAfterReplyNode = new FlowOrderNode();
			logAfterReplyNode.setNodeName(NODE_RESPONSE_LOG_AFTER_REPLY);
			responseMessageFlow.addNode(logAfterReplyNode);
			responseMessageFlow.connect(initialiseNode.OUTPUT_TERMINAL_OUT, logAfterReplyNode.INPUT_TERMINAL_IN);
			responseMessageFlow.connect(logAfterReplyNode.OUTPUT_TERMINAL_FIRST, responseProcessor.getInputTerminal("in"));
			
			SubFlowNode responseLogSubFlowNode = new SubFlowNode();
			responseLogSubFlowNode.setNodeName(NODE_RESPONSE_LOG);
			responseLogSubFlowNode.setSubFlow(logMessageFlow);
			responseMessageFlow.addNode(responseLogSubFlowNode);
			responseMessageFlow.connect(logAfterReplyNode.OUTPUT_TERMINAL_SECOND, responseLogSubFlowNode.getInputTerminal("Input"));
		}
		
		// Error Handling Required
		MessageFlow errorMessageFlow = patternInstanceManager.getMessageFlow(PROJECT_NAME, SUBFLOW_ERROR);
		boolean errorHandlingRequired = patternInstanceManager.getParameterValue(PROPERTY_ERROR_HANDLING_REQUIRED).equals("true");
		
		FlowOrderNode replyFirstNode = (FlowOrderNode) errorMessageFlow.getNodeByName(NODE_ERROR_REPLY_FIRST);
		
		ThrowNode errorThrowNode = new ThrowNode();
		errorThrowNode.setNodeName(NODE_ERROR_THROW);
		errorThrowNode.setMessageNumber(3001);
		errorThrowNode.setMessageText("Pattern instance "+patternInstanceName+" of the Message Correlator for WebSphere MQ: request-response with " +
				"persistence pattern has rolled back the input message. See the previous messages for details of the error");
		errorMessageFlow.addNode(errorThrowNode);
		
		if (errorHandlingRequired) {
			// Add user defined properties
			UserDefinedProperty errorHandlingUDP = new UserDefinedProperty("Basic", "ErrorLoggingOn", 
					FlowProperty.Usage.MANDATORY, FlowProperty.Type.BOOLEAN, true);
			errorMessageFlow.addFlowProperty(errorHandlingUDP);
						
			// Update Error subflow to add error handling
			
			FilterNode filterNode = new FilterNode();
			filterNode.setNodeName(NODE_ERROR_ERROR_LOGGING_ON);
			ESQLModule filterExpression = new ESQLModule();
			filterExpression.setBrokerSchema("mqsi");
			filterExpression.setEsqlMain("ErrorQ_check");
			filterNode.setFilterExpression(filterExpression);
			filterNode.setThrowExceptionOnDatabaseError(false);
			errorMessageFlow.addNode(filterNode);
			errorMessageFlow.connect(replyFirstNode.OUTPUT_TERMINAL_SECOND, filterNode.INPUT_TERMINAL_IN);
			errorMessageFlow.connect(filterNode.OUTPUT_TERMINAL_FAILURE, errorThrowNode.INPUT_TERMINAL_IN);
			errorMessageFlow.connect(filterNode.OUTPUT_TERMINAL_UNKNOWN, errorThrowNode.INPUT_TERMINAL_IN);
			errorMessageFlow.connect(filterNode.OUTPUT_TERMINAL_FALSE, errorThrowNode.INPUT_TERMINAL_IN);
			
			ComputeNode buildErrorMessage = new ComputeNode();
			buildErrorMessage.setNodeName(NODE_ERROR_BUILD_ERROR_MESSAGE);
			ESQLModule buildErrorMessageESQL = new ESQLModule();
			buildErrorMessageESQL.setBrokerSchema("mqsi");
			buildErrorMessageESQL.setEsqlMain("Build_Error_Message");
			buildErrorMessage.setComputeExpression(buildErrorMessageESQL);
			buildErrorMessage.setThrowExceptionOnDatabaseError(false);
			errorMessageFlow.addNode(buildErrorMessage);
			errorMessageFlow.connect(filterNode.OUTPUT_TERMINAL_TRUE, buildErrorMessage.INPUT_TERMINAL_IN);
			
			MQOutputNode errorOutput = new MQOutputNode();
			errorOutput.setNodeName(NODE_ERROR_ERROR_OUTPUT);
			String qName = patternInstanceManager.getParameterValue(PROPERTY_ERROR_HANDLING_Q);
			String qmName = patternInstanceManager.getParameterValue(PROPERTY_ERROR_HANDLING_QM);
			errorOutput.setQueueName(qName);
			errorOutput.setQueueManagerName(qmName);
			errorOutput.setTransactionMode(ENUM_MQOUTPUT_TRANSACTIONMODE.no);
			errorOutput.setPersistenceMode(ENUM_MQOUTPUT_PERSISTENCEMODE.yes);
			errorMessageFlow.addNode(errorOutput);
			errorMessageFlow.connect(buildErrorMessage.OUTPUT_TERMINAL_OUT, errorOutput.INPUT_TERMINAL_IN);
			errorMessageFlow.connect(errorOutput.OUTPUT_TERMINAL_FAILURE, errorThrowNode.INPUT_TERMINAL_IN);
			errorMessageFlow.connect(errorOutput.OUTPUT_TERMINAL_OUT, errorThrowNode.INPUT_TERMINAL_IN);
			
		} else {
			errorMessageFlow.connect(replyFirstNode.OUTPUT_TERMINAL_SECOND, errorThrowNode.INPUT_TERMINAL_IN);
		}
	}
}
