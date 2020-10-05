package com.plugin.prometheuspush;

import java.io.*;
import java.util.ArrayList;


public class Shell {
    
    public static String command(final String cmdline, final String directory) throws Exception
    {
        Process process = 
            new ProcessBuilder(new String[] {"bash", "-c", cmdline})
                .redirectErrorStream(true)
                .directory(new File(directory))
                .start();
        
        String stdout = getOutput(process.getInputStream());
        //String stderr = getOutput(process.getErrorStream());
        
        if (process.waitFor() != 0 || stdout.contains("error"))
            throw new Exception("Command execution failed with error: " + stdout);
        
        return stdout;
    }
    
    private static String getOutput(InputStream stream) throws IOException
    {
        ArrayList<String> output = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        String line = null;
        
        while ( (line = br.readLine()) != null )
            output.add(line);
        
        if (output.size() > 0)
            return String.join("\n", output);
        else
            return null;
    }
}