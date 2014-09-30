/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package samzaesper.task;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.samza.config.Config;
import org.apache.samza.system.IncomingMessageEnvelope;
import org.apache.samza.system.OutgoingMessageEnvelope;
import org.apache.samza.system.SystemStream;
import org.apache.samza.task.InitableTask;
import org.apache.samza.task.MessageCollector;
import org.apache.samza.task.StreamTask;
import org.apache.samza.task.TaskContext;
import org.apache.samza.task.TaskCoordinator;
import samzaesper.exception.CoonfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 *
 * @author stefan
 */
public class EsperTask implements StreamTask,InitableTask {

    private  EPServiceProvider esperProvider;
    private String evenNameKey;
    private static final SystemStream OUTPUT_STREAM = new SystemStream("kafka", "esper-output");
    private static final SystemStream LOG_STREAM = new SystemStream("kafka","log-output");
    private static final Log log = LogFactory.getLog(EsperTask.class);
    
    
    @Override
    public void init(Config config, TaskContext tc) throws Exception {
        log.info("initializing");
        System.out.println("initializing");
        esperProvider = EPServiceProviderManager.getDefaultProvider(); 
        evenNameKey = config.get("samzaesper.eventNameKey", "");
        log.info("event name key=" + evenNameKey);
        
        
      
      //  String topicName = tc.getSystemStreamPartitions().iterator().next().getSystemStream().getStream();
     //   registerEvents(config, topicName);
        
    }
    
    @Override
    public void process(IncomingMessageEnvelope ime, MessageCollector mc, TaskCoordinator tc) throws Exception {
      
        
         mc.send(new OutgoingMessageEnvelope(LOG_STREAM, "get messaage"));
         Map<String,Object> jsonMessage = (Map<String,Object>) ime.getMessage();
         String eventName = (String) jsonMessage.get(evenNameKey);
         jsonMessage.remove(evenNameKey);
         if(!esperProvider.getEPAdministrator().getConfiguration().isEventTypeExists(eventName))
         {
             String testKey = "";
             Map<String,Object> eventTypeMap = new HashMap<>();
             for(String key : jsonMessage.keySet())
             {
                 eventTypeMap.put(key,Object.class);
                 if(testKey.equals(""))
                 {
                     testKey = key;
                 }
             }
             esperProvider.getEPAdministrator().getConfiguration().addEventType(eventName, eventTypeMap);
             EPStatement stmt = esperProvider.getEPAdministrator().createEPL("SELECT " + testKey + " FROM " + eventName); 
             stmt.addListener(new EventListener(mc));
             
             
         }
         esperProvider.getEPRuntime().sendEvent(jsonMessage, eventName);
         
         
        
        
        
    }
    
    private class EventListener implements UpdateListener
    {
        private final MessageCollector collector;

        public EventListener(MessageCollector collector)
        {
            this.collector = collector;
        }
        
        
        @Override
        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
            if(newEvents != null)
            {
                for(EventBean event : newEvents)
                {
                    collector.send(new OutgoingMessageEnvelope(OUTPUT_STREAM, event.getUnderlying()));
                    
                }
            }
        }
        
    }
    
    
  /*  private void registerEvents(Config config,String topicName) throws MissingEventDefinitionException
    {
        List<String> eventNames = ConfigurationParser.parseConfig(config.get("events." + topicName + ".eventNames",""));
        if(eventNames.isEmpty())
        {
            throw new MissingEventDefinitionException("no events defined");
        }
        for(String eventName: eventNames)
        {
           List<String> properties = ConfigurationParser.parseConfig(config.get("events." + topicName + "." + eventName,""));
           if(properties.isEmpty())
           {
               throw new MissingEventDefinitionException("event properties definition of " + eventName + "is missing");
           }
             
           Map<String,Object> eventDef = new HashMap<>();
           for(String p : properties)
           {
               eventDef.put(p,Object.class);
           }
           esperProvider.getEPAdministrator().getConfiguration().addEventType(eventName, eventDef);
        }
    }*/
    
}