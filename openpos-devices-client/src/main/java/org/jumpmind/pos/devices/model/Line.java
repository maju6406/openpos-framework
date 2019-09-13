package org.jumpmind.pos.devices.model;

public class Line extends DocumentElement {

    String data;
    
    public Line() {
    }
    
    public Line(String data) {
        this.data = data;
    }
    
    public void setData(String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
    
}