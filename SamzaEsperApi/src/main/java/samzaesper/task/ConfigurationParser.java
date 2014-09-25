package samzaesper.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConfigurationParser {
    
    public static List<String> parseConfig(String configurationString)
    {
        
        String[] eventNameArray = configurationString.split(",");
        List<String> eventNamesList = Arrays.asList(eventNameArray);
        
        return eventNamesList;
    }

}