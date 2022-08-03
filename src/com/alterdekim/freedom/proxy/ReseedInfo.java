package com.alterdekim.freedom.proxy;

import java.util.ArrayList;

public class ReseedInfo {

    private ArrayList<String> reseeders = new ArrayList<String>();

    public ReseedInfo( ArrayList<String> reseeders ) {
        this.reseeders = reseeders;
    }

    public ArrayList<String> getReseeders() {
        return reseeders;
    }
}
