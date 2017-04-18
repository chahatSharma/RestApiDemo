package com.demo.service.impl;

import com.demo.configuration.RabbitConfiguration;
import com.demo.service.QueueService;
import org.apache.commons.lang.Validate;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;


@Service
public class QueueServiceImpl
        implements QueueService {
    public static final String OUT_QUEUE = "outQueue";

    Queue _outQueue;

    @Autowired
    private ApplicationContext _rabbitApplicationContext;
    private AmqpTemplate _amqpTemplate;
    private AmqpAdmin _amqpAdmin;

    public QueueServiceImpl() {
        super();
        _rabbitApplicationContext = new AnnotationConfigApplicationContext(RabbitConfiguration.class);
        _amqpTemplate = _rabbitApplicationContext.getBean(AmqpTemplate.class);
        _amqpAdmin = _rabbitApplicationContext.getBean(AmqpAdmin.class);
        _outQueue = new Queue(OUT_QUEUE);
        _amqpAdmin.declareQueue(_outQueue);
    }

    @Override
    public void sendMessage(String message) {
        _amqpTemplate.send(_outQueue.getName(), processAndGetMessage(message));
        // TODO: This is to demonstrate that String can be sent directly
        // _amqpTemplate.convertAndSend(_outQueue.getName(), message);
    }

    @Override
    public void sendMessage(JSONObject jsonObject) {
        Validate.notNull(jsonObject, "jsonObject is required");
        _amqpTemplate.convertAndSend(_outQueue.getName(), processAndGetMessage(jsonObject.toJSONString()));
        // onMessage(processAndGetMessage(jsonObject.toJSONString()));
    }

    private URL getURL(JSONObject jsonObject) {
        String id = (String) jsonObject.get("_id");
        String parentID =null;
        if(jsonObject.containsKey("ParentID")){
        	parentID = (String) jsonObject.get("ParentID");
        }
        Validate.notNull(id);
        String objectName = id.split("__", 2)[0];
        String key = id.split("__", 2)[1];
        Validate.notEmpty(objectName);
        Validate.notEmpty(key);
        try {
        	String file="";
        	if(null != parentID )
        	{
        		file = "/test" + "/" + objectName + "/" + key + "?parent=" +parentID;
        	}
        	else{
            file = "/test" + "/" + objectName + "/" + key;}
            return new URL("http", "localhost", 9200, file);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Message processAndGetMessage(String data) {
        MessageProperties properties = new MessageProperties();
        properties.setPriority(0);
        byte[] messageBytes = data.getBytes();
        return new Message(messageBytes, properties);
    }

    @Override
    public String receiveMessage(String data) {
        System.out.println("Data received: " + data);
        return data;
    }

// TODO: This is to demonstrate that String can be received directly
//    @RabbitListener(queues = OUT_QUEUE)
//    public void onString(String data)
//    {
//        System.out.println("Data Received: " + data);
//    }

    @RabbitListener(queues = OUT_QUEUE)
    public void onMessage(Message data) {
        String message = new String(data.getBody(), StandardCharsets.UTF_8);
        Validate.notEmpty(message);
        try {
            JSONObject jsonObject = (JSONObject) new JSONParser().parse(message);
            
            
            URL url = getURL(jsonObject);
            System.out.println("url"+url);
            
            jsonObject.remove("_id");
            String objectType = (String) jsonObject.get("_type");
            jsonObject.remove("_type");
            jsonObject.put("objectType", objectType);
            Set keys =jsonObject.keySet();
            Iterator it = keys.iterator();
            while(it.hasNext())
            {
            	String key = (String) it.next();
            	
            	if(jsonObject.get(key) instanceof JSONArray)
            	{
            		
						JSONArray array = (JSONArray) jsonObject.get(key);
						JSONArray newArray = new JSONArray();
						Iterator itt =array.iterator();
						while(itt.hasNext())
						{
							JSONObject obj = (JSONObject) itt.next();
							//array.remove(obj);
							if(obj.containsKey("_id"))
								obj.remove("_id");
							else if(obj.containsKey("_type"))
							{
								String objTy = (String) obj.get("_type");
								obj.remove("_type");
								obj.put("objectType", objTy);
							}
							
							newArray.add(obj);
									
						}
						jsonObject.replace(key, newArray);
            		
            	}
            	else if(jsonObject.get(key) instanceof JSONObject)
            	{
            		JSONObject tempObj = (JSONObject) jsonObject.get(key);
					if(tempObj.containsKey("_id"))
						tempObj.remove("_id");
					else if(tempObj.containsKey("_type"))
					{
						String objTy = (String) tempObj.get("_type");
						tempObj.remove("_type");
						tempObj.put("objectType", objTy);
					}
					jsonObject.replace(key, tempObj);
            	}
            }
            CloseableHttpClient httpClient = HttpClients.createDefault();
            assert url != null;
            try {
                HttpPost postRequest = new HttpPost(url.toURI());
                Validate.notNull(postRequest);
                StringEntity entity = new StringEntity(jsonObject.toJSONString(), ContentType.APPLICATION_JSON);
                Validate.notNull(entity, "Entity can not be empty");
                postRequest.setEntity(entity);
                CloseableHttpResponse httpResponse = httpClient.execute(postRequest);
                System.out.print("url" +jsonObject + "response for url"+httpResponse.toString());

            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.println("Data Received: " + message);
    }
}
