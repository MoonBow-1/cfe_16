package com.teragrep.cfe_16.bo;

public interface HttpEventData {
    public String getEvent();

    public String getChannel();

    public String getAuthenticationToken();

    public Integer getAckID();
}
