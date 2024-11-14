package com.herontheb1rd.smcspeedtest;

public class ServerInfo {
    private String rawUrl;
    private String rawHost;

    String latencyUrl;
    String downloadUrl;
    String uploadUrl;
    String name;
    
    public ServerInfo(String rawUrl, String rawHost, String name){
        this.rawUrl = rawUrl;
        this.rawHost = rawHost;
        this.name = name;


        latencyUrl = rawHost.substring(0, rawHost.length()-5);
        downloadUrl = rawUrl.substring(0, rawUrl.length()-10).concat("/random2000x2000.jpg");
        uploadUrl = rawUrl;
    }
    
}
