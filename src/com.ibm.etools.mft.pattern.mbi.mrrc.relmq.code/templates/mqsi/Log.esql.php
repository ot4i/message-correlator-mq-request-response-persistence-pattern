BROKER SCHEMA mqsi

/**
 * Copyright (c) 2014 IBM Corporation and other Contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - initial implementation
**/

CREATE Compute MODULE CreateLogMessage

CREATE FUNCTION main() RETURNS BOOLEAN BEGIN
-- Create logging info in MQRFH2 - add to existing message
	SET OutputRoot = InputRoot;
DECLARE outRef REFERENCE TO OutputRoot.MQRFH2;
 IF LASTMOVE(outRef) THEN
 -- MQRFH2 EXISTS
    SET outRef.usr.BrokerName = SQL.BrokerName;
    SET outRef.usr.MessageFlowLabel = SQL.MessageFlowLabel; 
    SET outRef.usr.DTSTAMP =   CURRENT_TIMESTAMP; 

  
 ELSE
 -- CREATE THE MQRFH2 Header first
    DECLARE MQMDRef REFERENCE TO OutputRoot.MQMD;	
    CREATE NEXTSIBLING OF MQMDRef AS outRef DOMAIN('MQRFH2') NAME 'MQRFH2';
    SET outRef.(MQRFH2.Field)Version = 2;
    SET outRef.(MQRFH2.Field)Encoding = InputRoot.MQMD.Encoding;
    SET outRef.(MQRFH2.Field)CodedCharSetId = InputRoot.MQMD.CodedCharSetId;
    SET outRef.usr.BrokerName = SQL.BrokerName;
    SET outRef.usr.MessageFlowLabel = SQL.MessageFlowLabel; 
    SET outRef.usr.DTSTAMP =   CURRENT_TIMESTAMP; 
    SET OutputRoot.MQMD.Format = MQFMT_RF_HEADER_2;
 END IF;

END;
END MODULE;

CREATE Compute MODULE CreateTraceData
CREATE FUNCTION main() RETURNS BOOLEAN BEGIN
	DECLARE EnvVarRef REFERENCE TO Environment.Variables;
	SET EnvVarRef.DTSTAMP = CURRENT_TIMESTAMP; 
	SET EnvVarRef.BrokerName = Substring(SQL.BrokerName from 1 for 128);
    SET EnvVarRef.MessageFlowlabel = Substring(SQL.MessageFlowLabel from 1 for 128);
    
END;
END MODULE;
CREATE FILTER MODULE CheckLogging
	CREATE FUNCTION Main() RETURNS BOOLEAN
	BEGIN
	IF Environment.Variables.ResponseLoggingOn AND (Environment.Variables.RRMode = 'Response') THEN RETURN TRUE;
		END IF;
	IF Environment.Variables.RequestLoggingOn AND (Environment.Variables.RRMode = 'Request') THEN RETURN TRUE;
		END IF;
		RETURN FALSE;
	END;

END MODULE;