/*******************************************************************************
 * Copyright 2013-16 Indra Sistemas S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 ******************************************************************************/
package com.indra.sofia2.ssap.kp.implementations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.fusesource.mqtt.client.QoS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.indra.sofia2.ssap.kp.Kp;
import com.indra.sofia2.ssap.kp.Listener4SIBIndicationNotifications;
import com.indra.sofia2.ssap.kp.config.MQTTConnectionConfig;
import com.indra.sofia2.ssap.kp.exceptions.UnsupportedSSAPMessageTypeException;
import com.indra.sofia2.ssap.kp.implementations.mqtt.KpMQTTClient;
import com.indra.sofia2.ssap.ssap.SSAPBulkMessage;
import com.indra.sofia2.ssap.ssap.SSAPMessage;
import com.indra.sofia2.ssap.ssap.SSAPMessageGenerator;
import com.indra.sofia2.ssap.ssap.SSAPQueryType;
import com.indra.sofia2.ssap.ssap.body.SSAPBodyReturnMessage;
import com.indra.sofia2.ssap.ssap.body.bulk.message.SSAPBodyBulkReturnMessage;

public class KpMqttFuncional {
	
	private static final Logger log = LoggerFactory.getLogger(KpMqttFuncional.class);
	
	private final static String HOST="sofia2.com";
	private final static int PORT=1880;
	
	private final static String TOKEN = "f0516016ef2647aeb115ad035e18f0fb";
	private final static String KP_INSTANCE = "kpParadaAutobus:kp01";
	
	private final static String ONTOLOGY_NAME = "feedBiciCoruna";
	private final static String ONTOLOGY_INSTANCE = "{\"Feed\": {\"assetId\": \"5\", \"assetName\": \"Aquarium\", \"assetSource\": \"BiciCoruna\", \"assetType\": \"\", \"attribs\": [{\"name\": \"nombre\", \"value\": \"Aquarium\"}, {\"name\": \"direccion\", \"value\": \"As Lagoas\"}, {\"name\": \"horaDeInicio\", \"value\": \"7:30\"}, {\"name\": \"horaDeFin\", \"value\": \"22:30\"}], \"feedId\": \"feed_5_2015-05-11T08:19:01\", \"feedSource\": \"BiciCoruna\", \"geometry\": {\"coordinates\": [-8.41003131866455, 43.38148880004883], \"type\": \"Point\"}, \"measures\": [{\"desc\": \"Total de Puestos\", \"measure\": \"16\", \"method\": \"REAL\", \"name\": \"PuestosActivos\", \"unit\": \"u\"}, {\"desc\": \"Bicicletas disponibles\", \"measure\": \"5\", \"method\": \"REAL\", \"name\": \"BicisDisponibles\", \"unit\": \"u\"}], \"measuresPeriod\": 60, \"measuresPeriodUnit\": \"s\", \"measuresTimestamp\": {\"$date\": \"2015-05-11T08:19:01.443Z\"}, \"measuresType\": \"INSTANT\", \"timestamp\": {\"$date\": \"2015-05-11T08:19:01.443Z\"}, \"type\": \"VIRTUAL\"}}";
	
	private final static String ONTOLOGY_UPDATE = "{\"Sensor\": { \"geometry\": { \"coordinates\": [ 40.512967, -3.67495 ], \"type\": \"Point\" }, \"assetId\": \"S_Temperatura_00066\", \"measure\": 20, \"timestamp\": { \"$date\": \"2014-04-29T08:24:54.005Z\"}}}";
	private final static String ONTOLOGY_UPDATE_WHERE = "{Sensor.assetId:\"S_Temperatura_00066\"}";
	
	private final static String ONTOLOGY_QUERY_NATIVE = "{Sensor.measure:{$gt:10}}";
	
	private final static String ONTOLOGY_INSERT_SQLLIKE = "insert into TestSensorTemperatura(geometry, assetId, measure, timestamp) values (\"{ 'coordinates': [ 40.512967, -3.67495 ], 'type': 'Point' }\", \"S_Temperatura_00066\", 15, \"{ '$date': '2014-04-29T08:24:54.005Z'}\")";
	private final static String ONTOLOGY_UPDATE_SQLLIKE = "update TestSensorTemperatura set measure = 20 where Sensor.assetId = \"S_Temperatura_00066\"";
	
//	private final static String ONTOLOGY_QUERY_SQLLIKE = "select * from TestSensorTemperatura where Sensor.assetId = \"S_Temperatura_00066\"";

	private Kp kp;
	
	private final static String MQTTUSERNAME = "sofia2";
	private final static String MQTTPASSWORD = "indra2014";
	private final static boolean ENABLEMQTTAUTHENTICATION = false;
	
	
	@Before
	public void setUpBeforeClass() throws Exception {
		
		MQTTConnectionConfig config=new MQTTConnectionConfig();
		config.setSibHost(HOST);
		config.setSibPort(PORT);
		config.setKeepAliveInSeconds(5);
		config.setQualityOfService(QoS.AT_LEAST_ONCE);
		config.setSibConnectionTimeout(6000);
		config.setSsapResponseTimeout(1000000);
		if (ENABLEMQTTAUTHENTICATION){
			config.setUser(MQTTUSERNAME);
			config.setPassword(MQTTPASSWORD);
		}
		
		this.kp=new KpMQTTClient(config);
		
		this.kp.connect();
		
	}
	
	@After
	public void disconnectAfterClass() throws Exception {
		
		this.kp.disconnect();
	}
	
	
	@Test
	public void testJoinByTokenLeave() throws Exception {
		
		//Genera mensaje de JOIN
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		
		System.out.println("Envia mensaje JOIN al SIB: "+msgJoin.toJson());
		
		//Envia el mensaje
		SSAPMessage response=kp.send(msgJoin);
		
		log.info("Recibe respuesta desde el SIB: "+response.toJson());
		
		//Comprueba que el mensaje trae session key
		assertNotSame(response.getSessionKey(), null);
		System.out.println("Sessionkey recibida: "+ response.getSessionKey());
		
		String sessionKey=response.getSessionKey();
		
		
		//Comprueba el BodyResponse
		SSAPBodyReturnMessage bodyReturn=SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
		assertEquals(bodyReturn.getData(), sessionKey);
		assertTrue(bodyReturn.isOk());
		assertSame(bodyReturn.getError(), null);
		
		
		//Genera un mensaje de LEAVE
		SSAPMessage msgLeave=SSAPMessageGenerator.getInstance().generateLeaveMessage(sessionKey);
		System.out.println("Envia mensaje LEAVE al SIB: "+msgLeave.toJson());
		
		//Envia el mensaje
		SSAPMessage responseLeave=kp.send(msgLeave);
		
		System.out.println("Recibe respuesta desde el SIB: "+responseLeave.toJson());
		
		//Comprueba el BodyResponse
		SSAPBodyReturnMessage bodyReturnLeave=SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseLeave.getBody());
		assertEquals(bodyReturnLeave.getData(), sessionKey);
		assertTrue(bodyReturnLeave.isOk());
		assertSame(bodyReturnLeave.getError(), null);
	}
	
	@Test
	public void testInsertNative() throws Exception {
		//Genera mensaje de JOIN
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		
		//Envia el mensaje
		SSAPMessage response=kp.send(msgJoin);
		
		
		//Comprueba que el mensaje trae session key
		assertNotSame(response.getSessionKey(), null);
		final String sessionKey=response.getSessionKey();
		
		//Genera un mensaje de INSERT
		
		SSAPMessage msgInsert=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSTANCE);
		log.info("Envia mensaje INSERT al SIB: "+msgInsert.toJson());
		SSAPMessage responseInsert=kp.send(msgInsert);
		
		//Checks if insert message was OK in SIB
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseInsert.getBody());
		assertTrue(returned.isOk());
		log.info("Intancia de ontologia insertada con objectId: "+returned.getData());
				
		
		
		
		//Genera un mensaje de LEAVE
		SSAPMessage msgLeave=SSAPMessageGenerator.getInstance().generateLeaveMessage(sessionKey);
		
		//Envia el mensaje
		kp.send(msgLeave);
		
	}
	
	@Test
	public void testUpdateNative() throws Exception {
		//Genera mensaje de JOIN
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		
		//Envia el mensaje
		SSAPMessage response=kp.send(msgJoin);
		
		//Comprueba que el mensaje trae session key
		assertNotSame(response.getSessionKey(), null);
		String sessionKey=response.getSessionKey();
		
		
		//Genera un mensaje de UPDATE
		SSAPMessage msgUpate=SSAPMessageGenerator.getInstance().generateUpdateMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_UPDATE, ONTOLOGY_UPDATE_WHERE);
		log.info("Envia mensaje UPDATE al SIB: "+msgUpate.toJson());
		SSAPMessage responseUpdate=kp.send(msgUpate);
		
		
		
		//Checks if update message was OK in SIB
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseUpdate.getBody());
		assertTrue(returned.isOk());
		log.info("Intancias de ontologia actualizadas: "+returned.getData());
		
		
		//Genera un mensaje de LEAVE
		SSAPMessage msgLeave=SSAPMessageGenerator.getInstance().generateLeaveMessage(sessionKey);
		
		//Envia el mensaje
		kp.send(msgLeave);
	}
	
	@Test
	public void testQueryNative() throws Exception {
		//Genera mensaje de JOIN
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		
		//Envia el mensaje
		SSAPMessage response=kp.send(msgJoin);
		
		//Comprueba que el mensaje trae session key
		assertNotSame(response.getSessionKey(), null);
		String sessionKey=response.getSessionKey();
		
		
		//Genera un mensaje de QUERY
		SSAPMessage msgQuery=SSAPMessageGenerator.getInstance().generateQueryMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_QUERY_NATIVE);
		log.info("Envia mensaje QUERY al SIB: "+msgQuery.toJson());
		SSAPMessage responseQuery=kp.send(msgQuery);
		
		
		
		//Checks if update message was OK in SIB
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseQuery.getBody());
		assertTrue(returned.isOk());
		log.info("Resutado de la query: "+returned.getData());
		
		
		//Genera un mensaje de LEAVE
		SSAPMessage msgLeave=SSAPMessageGenerator.getInstance().generateLeaveMessage(sessionKey);
		
		//Envia el mensaje
		kp.send(msgLeave);
	}
	
	@Test
	public void testInsertSqlLike() throws Exception {
		//Genera mensaje de JOIN
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		
		//Envia el mensaje
		SSAPMessage response=kp.send(msgJoin);
		
		//Comprueba que el mensaje trae session key
		assertNotSame(response.getSessionKey(), null);
		String sessionKey=response.getSessionKey();
		
		
		//Genera un mensaje de INSERT
		SSAPMessage msgInsert=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSERT_SQLLIKE, SSAPQueryType.SQLLIKE);
		log.info("Envia mensaje INSERT al SIB: "+msgInsert.toJson());
		SSAPMessage responseInsert=kp.send(msgInsert);
		
		
		
		//Checks if insert message was OK in SIB
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseInsert.getBody());
		assertTrue(returned.isOk());
		log.info("Intancia de ontologia insertada con objectId: "+returned.getData());
		
		
		
		//Genera un mensaje de LEAVE
		SSAPMessage msgLeave=SSAPMessageGenerator.getInstance().generateLeaveMessage(sessionKey);
		
		//Envia el mensaje
		kp.send(msgLeave);
		
	}
	
	
	@Test
	public void testUpdateSqlLike() throws Exception {
		//Genera mensaje de JOIN
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		
		//Envia el mensaje
		SSAPMessage response=kp.send(msgJoin);
		
		//Comprueba que el mensaje trae session key
		assertNotSame(response.getSessionKey(), null);
		String sessionKey=response.getSessionKey();
		
		
		//Genera un mensaje de UPDATE
		SSAPMessage msgUpate=SSAPMessageGenerator.getInstance().generateUpdateMessage(sessionKey, ONTOLOGY_NAME, null, ONTOLOGY_UPDATE_SQLLIKE, SSAPQueryType.SQLLIKE);
		log.info("Envia mensaje UPDATE al SIB: "+msgUpate.toJson());
		SSAPMessage responseUpdate=kp.send(msgUpate);
		
		
		
		//Checks if update message was OK in SIB
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseUpdate.getBody());
		assertTrue(returned.isOk());
		log.info("Intancias de ontologia actualizadas: "+returned.getData());
		
		
		//Genera un mensaje de LEAVE
		SSAPMessage msgLeave=SSAPMessageGenerator.getInstance().generateLeaveMessage(sessionKey);
		
		//Envia el mensaje
		kp.send(msgLeave);
	}
	
	
	@Test
	public void testQuerySql() throws Exception {
		//Genera mensaje de JOIN
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		
		//Envia el mensaje
		SSAPMessage response=kp.send(msgJoin);
		
		//Comprueba que el mensaje trae session key
		assertNotSame(response.getSessionKey(), null);
		String sessionKey=response.getSessionKey();
		
		
		//Genera un mensaje de QUERY
		SSAPMessage msgQuery=SSAPMessageGenerator.getInstance().generateQueryMessage(sessionKey, "feedForecastMeteorologico", "select * from feedForecastMeteorologico ", SSAPQueryType.SQLLIKE);
		log.info("Envia mensaje QUERY al SIB: "+msgQuery.toJson());
		SSAPMessage responseQuery=kp.send(msgQuery);
		
		
		
		//Checks if update message was OK in SIB
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseQuery.getBody());
		log.info("Resutado de la query: "+responseQuery.toJson());
		assertTrue(returned.isOk());
		log.info("Resutado de la query: "+returned.getData());
		
		
		//Genera un mensaje de LEAVE
		SSAPMessage msgLeave=SSAPMessageGenerator.getInstance().generateLeaveMessage(sessionKey);
		
		//Envia el mensaje
		kp.send(msgLeave);
	}
	
	
	@Test
	public void testQuerySqlBDC() throws Exception {
		//Genera mensaje de JOIN
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		
		//Envia el mensaje
		SSAPMessage response=kp.send(msgJoin);
		
		//Comprueba que el mensaje trae session key
		assertNotSame(response.getSessionKey(), null);
		String sessionKey=response.getSessionKey();
		
		
		//Genera un mensaje de QUERY
		SSAPMessage msgQuery=SSAPMessageGenerator.getInstance().generateQueryMessage(sessionKey, null, "select * from Asset where identificacion='tweets_sofia'", SSAPQueryType.BDC);
		log.info("Envia mensaje QUERY al SIB: "+msgQuery.toJson());
		SSAPMessage responseQuery=kp.send(msgQuery);
		
		
		log.info(responseQuery.toJson());
		//Checks if update message was OK in SIB
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseQuery.getBody());
		assertTrue(returned.isOk());
		log.info("Resutado de la query: "+returned.getData());
		
		
		//Genera un mensaje de LEAVE
		SSAPMessage msgLeave=SSAPMessageGenerator.getInstance().generateLeaveMessage(sessionKey);
		
		//Envia el mensaje
		kp.send(msgLeave);
	}
	

	@Test
	public void testQueryBDC() throws Exception {
		//Genera mensaje de JOIN
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		
		//Envia el mensaje
		SSAPMessage response=kp.send(msgJoin);
		
		//Comprueba que el mensaje trae session key
		assertNotSame(response.getSessionKey(), null);
		String sessionKey=response.getSessionKey();
		
		//Genera un mensaje de QUERY
		SSAPMessage msgQuery=SSAPMessageGenerator.getInstance().generateQueryMessage(sessionKey, null, "select * from Asset", SSAPQueryType.BDC);
		log.info("Envia mensaje QUERY al SIB: "+msgQuery.toJson());
		SSAPMessage responseQuery=kp.send(msgQuery);
		
		
		
		//Checks if update message was OK in SIB
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseQuery.getBody());
		log.info("Resutado de la query: "+returned.getData());
		log.info("Resutado de la query: "+returned.getError());
		assertTrue(returned.isOk());
		
		
		
		//Genera un mensaje de LEAVE
		SSAPMessage msgLeave=SSAPMessageGenerator.getInstance().generateLeaveMessage(sessionKey);
		
		//Envia el mensaje
		kp.send(msgLeave);
	}
	
	
	public static boolean indicationReceived=false;
	
	@Test
	public void testSubscribeUnsubscribe() throws Exception {
		
		//Hace JOIN al SIB
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		SSAPMessage responseJoin=kp.send(msgJoin);
		
		final String sessionKey=responseJoin.getSessionKey();
		
		//Registra un listener para recibir notificaciones
		kp.addListener4SIBNotifications(new Listener4SIBIndicationNotifications() {
			
			@Override
			public void onIndication(String messageId, SSAPMessage ssapMessage) {
				
				log.info("Recibe mensaje INDICATION para la suscripción con identificador: "+messageId+" with indication message: "+ssapMessage.toJson());
				
				KpMqttFuncional.indicationReceived=true;
			
				SSAPBodyReturnMessage indicationMessage=SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(ssapMessage.getBody());
				assertNotSame(indicationMessage.getData(), null);
				assertTrue(indicationMessage.isOk());
				assertSame(indicationMessage.getError(), null);
			}
		});
		
		
		//Envia el mensaje de SUBSCRIBE
		
		SSAPMessage msg=SSAPMessageGenerator.getInstance().generateSubscribeMessage(sessionKey, ONTOLOGY_NAME, 0, "", SSAPQueryType.SQLLIKE);
		
		SSAPMessage msgSubscribe = kp.send(msg);
		
		SSAPBodyReturnMessage responseSubscribeBody = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(msgSubscribe.getBody());
		
		assertNotSame(responseSubscribeBody.getData(), null);
		assertTrue(responseSubscribeBody.isOk());
		assertSame(responseSubscribeBody.getError(), null);
		
		//Recupera el id de suscripcion
		final String subscriptionId=responseSubscribeBody.getData();
		
		
		//Envia un mensaje INSERT para recibir la notificacion de suscripcion
		SSAPMessage msgInsert=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSERT_SQLLIKE, SSAPQueryType.SQLLIKE);
		log.info("Envia mensaje INSERT al SIB: "+msgInsert.toJson());
		SSAPMessage responseInsert=kp.send(msgInsert);
		
		SSAPBodyReturnMessage returned = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseInsert.getBody());
		assertTrue(returned.isOk());
		
		
		//Comprueba si se ha recibido el mensaje de notificación
		Thread.sleep(5000);
		assertTrue(indicationReceived);
		
		
		//Envia el mensaje de UNSUBSCRIBE
		SSAPMessage msgUnsubscribe=SSAPMessageGenerator.getInstance().generateUnsubscribeMessage(sessionKey, ONTOLOGY_NAME, subscriptionId);
		
		SSAPMessage responseUnsubscribe=kp.send(msgUnsubscribe);
		SSAPBodyReturnMessage responseUnSubscribeBody = SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseUnsubscribe.getBody());
		
		assertEquals(responseUnSubscribeBody.getData(), "");
		assertTrue(responseUnSubscribeBody.isOk());
		assertSame(responseUnSubscribeBody.getError(), null);
		
	}
	
	@Test
	public void testBulk() throws Exception {
		
		//Genera mensaje de JOIN
		SSAPMessage msgJoin=SSAPMessageGenerator.getInstance().generateJoinByTokenMessage(TOKEN, KP_INSTANCE);
		
		log.info("Envia mensaje JOIN al SIB: "+msgJoin.toJson());
		
		//Envia el mensaje
		SSAPMessage response=kp.send(msgJoin);
		
		log.info("Recibe respuesta desde el SIB: "+response.toJson());
		
		//Comprueba que el mensaje trae session key
		assertNotSame(response.getSessionKey(), null);
		log.info("Sessionkey recibida: "+ response.getSessionKey());
		
		final String sessionKey=response.getSessionKey();
		
		
		//Comprueba el BodyResponse
		SSAPBodyReturnMessage bodyReturn=SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(response.getBody());
		assertEquals(bodyReturn.getData(), sessionKey);
		assertTrue(bodyReturn.isOk());
		assertSame(bodyReturn.getError(), null);
		
		
		
		//Genera un mensaje de INSERT
		SSAPMessage msgInsert1=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSTANCE);
		//Genera un mensaje de INSERT
		SSAPMessage msgInsert2=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSTANCE);
	
		//Genera un mensaje INSERT de SQLLIKE
		SSAPMessage msgInsert3=SSAPMessageGenerator.getInstance().generateInsertMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_INSERT_SQLLIKE, SSAPQueryType.SQLLIKE);
		
		//Genera un mensaje de UPDATE
		SSAPMessage msgUpate1=SSAPMessageGenerator.getInstance().generateUpdateMessage(sessionKey, ONTOLOGY_NAME, ONTOLOGY_UPDATE, ONTOLOGY_UPDATE_WHERE);
		
		//Genera un mensaje UPDATE de SQLLIKE
		SSAPMessage msgUpate2=SSAPMessageGenerator.getInstance().generateUpdateMessage(sessionKey, ONTOLOGY_NAME, null, ONTOLOGY_UPDATE_SQLLIKE, SSAPQueryType.SQLLIKE);
		
		
		//Genera un mensaje DELETE
//		SSAPMessage msgDelete1=SSAPMessageGenerator.getInstance().generateRemoveMessage(sessionKey, ONTOLOGY_NAME, "db.TestSensorTemperatura.remove({'Sensor.assetId':'S_Temperatura_00066'})");
		
		
		//SSAPBulkMessage msgBulk=SSAPMessageGenerator.getInstance().generateBulkMessage(sessionKey, ONTOLOGY_NAME);
//		try {
//			msgBulk.addMessage(msgInsert1);
//			msgBulk.addMessage(msgInsert2);
//			msgBulk.addMessage(msgInsert3);
//			msgBulk.addMessage(msgUpate1);
//			msgBulk.addMessage(msgUpate2);
//			msgBulk.addMessage(msgDelete1);
//			
//		} catch (NotSupportedMessageTypeException e) {
//			e.printStackTrace();
//		}
		
		SSAPBulkMessage msgBulk=SSAPMessageGenerator.getInstance().generateBulkMessage(sessionKey, "Ontologia");
		try{
			msgBulk.addMessage(msgInsert1).addMessage(msgInsert2).addMessage(msgInsert3).addMessage(msgUpate1).addMessage(msgUpate2);
		} catch(UnsupportedSSAPMessageTypeException e){
			e.printStackTrace();
		}
		
		log.info("Envia mensaje BULK al SIB: "+msgBulk.toJson());
		SSAPMessage respuesta=kp.send(msgBulk);
		
		log.info("Recibe respuesta BULK desde el SIB: "+respuesta.toJson());
		
		SSAPBodyReturnMessage bodyBulkReturn=SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(respuesta.getBody());
		SSAPBodyBulkReturnMessage summary=SSAPBodyBulkReturnMessage.fromJsonToSSAPBodyBulkReturnMessage(bodyBulkReturn.getData());
		
		assertEquals(3, summary.getInsertSummary().getObjectIds().size());
		
		log.info("ObjectIds insertados");
		for(String oid:summary.getInsertSummary().getObjectIds()){
			log.info(oid);
		}
		
		
		//Genera un mensaje de LEAVE
		SSAPMessage msgLeave=SSAPMessageGenerator.getInstance().generateLeaveMessage(sessionKey);
		log.info("Envia mensaje LEAVE al SIB: "+msgLeave.toJson());
		
		//Envia el mensaje
		SSAPMessage responseLeave=kp.send(msgLeave);
		
		log.info("Recive respuesta desde el SIB: "+responseLeave.toJson());
		
		//Comprueba el BodyResponse
		SSAPBodyReturnMessage bodyReturnLeave=SSAPBodyReturnMessage.fromJsonToSSAPBodyReturnMessage(responseLeave.getBody());
		assertEquals(bodyReturnLeave.getData(), sessionKey);
		assertTrue(bodyReturnLeave.isOk());
		assertSame(bodyReturnLeave.getError(), null);
		
	}
	
	
	
}
