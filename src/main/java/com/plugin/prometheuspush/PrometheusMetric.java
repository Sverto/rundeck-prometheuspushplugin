package com.plugin.prometheuspush;

import java.net.URL;
import java.util.*;


/**
 * Extract parameters from Rundeck context required for Prometheus
 */
public class PrometheusMetric
{
    private String metricPrefix;
    private String jobId;
    private Map<String, String> labels = new Hashtable<String, String>();
    private Map<String, Object> metrics = new Hashtable<String, Object>();
    
    
    public PrometheusMetric(String trigger, Map<String, Object> executionData, Map<String, Object> configuration, String metricPrefix)
    {
        metricPrefix = metricPrefix.toLowerCase();
        // Get unique job identifier
        jobId = FormatLabelValue(getMapValue(executionData, "job", "id"));
        // Get labels & metrics
        setLabels(executionData);
        setMetrics(trigger, executionData);
    }
    
    
    private void setLabels(Map<String, Object> executionData)
    {
        labels.put("instance",  FormatLabelValue(getMapValue(executionData, "context", "job", "serverUrl")));
        labels.put("project",   FormatLabelValue(getMapValue(executionData, "project")));
        labels.put("name",      FormatLabelValue(getMapValue(executionData, "job", "name")));
        labels.put("execution", FormatLabelValue(getMapValue(executionData, "id")));
        
        // Extract type and environment labels from Rundeck job group property
        String group = FormatLabelValue(getMapValue(executionData, "job", "group"));
        String groups[] = group != null ? group.split("/") : new String[] {""};
        labels.put("type", groups[0]);
        labels.put("environment", groups.length > 1 ? groups[1] : "");
    }
    

    private void setMetrics(String trigger, Map<String, Object> executionData)
    {
        // Get execution start and endtime
        long started = (long)getMapValue(executionData, "dateStartedUnixtime") / 1000;
        long ended = (long)executionData.getOrDefault("dateEndedUnixtime", System.currentTimeMillis()) / 1000;
        
        metrics.put("", 1);
        metrics.put("availability", (trigger == "success" ? 1.0 : 0.0));
        metrics.put("epochtime", started);
        metrics.put("duration", ended - started);
        metrics.put("execution", 1);
    }
    
    
    /**
     * Convert extracted Rundeck parameters into a pushgateway push command
     * @param endpoint Pushgateway endpoint
     * @return Constructed shell command to push metrics
     */
    public String toCmd(URL endpoint) throws Exception
    {
        // Build metrics payload
        String base         = ToMetricString("", new String[] {"name", "project", "type", "environment"});
        String availability = ToMetricString("availability");
        String epochtime    = ToMetricString("epochtime");
        String duration     = ToMetricString("duration");
        String execution    = ToMetricString("execution", new String[] {"instance", "execution"});
        
        // Build CMD String
        String payload = base + "\n" + availability + "\n" + epochtime + "\n" + duration + "\n" + execution + "\n";
        return "cat <<EOF | curl --data-binary @- " + endpoint + "/metrics/job/" + jobId + "\n" + payload + "EOF\n"; // TODO: support authentication
    }
    
    
    //// Helper methods... ////
    
    private String ToMetricString(String name)
    {
        return ToMetricString(name, new String[] {});
    }
    
    private String ToMetricString(String name, String[] selectedLabels)
    {
        // Build labels string '{key="value", ...}'
        StringBuilder sbLabels = new StringBuilder("{");
        Iterator<Map.Entry<String, String>> it = labels.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, String> entry = it.next();
            if (Arrays.asList(selectedLabels).contains(entry.getKey()))
            {
                sbLabels.append(entry.getKey());
                sbLabels.append("=\"");
                sbLabels.append(entry.getValue());
                sbLabels.append("\"");
                if (it.hasNext())
                    sbLabels.append(",");
            }
        }
        sbLabels.append("}");
        
        // Build metric string 'name{labels} value'
        String fullName = metricPrefix + (name.length() > 0 ? "_" + name : "");
        return "# TYPE " + fullName + " gauge\n" + fullName + sbLabels.toString() + " " + metrics.get(name);
    }
    
    
    private static String FormatLabelValue(Object value)
    {
        return FormatLabelValue(value, false);
    }
    
    
    private static String FormatLabelValue(Object value, boolean lowerCase)
    {
        String returnValue;
        
        if (value == null || value.toString().length() == 0)
            return null;
        else
            returnValue = value.toString().replaceAll("[\"]", "'").replaceAll("/$|\\$|\n|\r", "");
            
        if (lowerCase)
            returnValue = returnValue.toLowerCase();
            
        return returnValue;
    }
    
    
    // Ridiculous that we need this...
    @SuppressWarnings("unchecked") // Rundeck uses rawtypes...
    private static Object getMapValue(Map<String, Object> map, LinkedList<String> keyChain)
    {
        if (keyChain.size() <= 1)
            return map.get(keyChain.get(0));
        
        Map<String, Object> childMap = (Map<String, Object>)map.get(keyChain.get(0));
        keyChain.remove(0);
        
        return getMapValue(childMap, keyChain);
    }
    private static Object getMapValue(Map<String, Object> map, String ... keys)
    {
        return getMapValue(map, new LinkedList<String>(Arrays.asList(keys)));
    }
    private static Object getMapValue(Map<String, Object> map, String key)
    {
        return getMapValue(map, new LinkedList<String>(Arrays.asList(key)));
    }
    
}