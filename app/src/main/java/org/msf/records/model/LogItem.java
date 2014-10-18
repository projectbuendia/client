package org.msf.records.model;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Gil on 03/10/2014.
 */
public class LogItem {

    public String mContent;
    public Date mTimestamp;


    public LogItem(String content, Date timestamp){
        mContent = content;
        mTimestamp = timestamp;
    }

    public static ArrayList<LogItem> GETDUMMYCONTENT(){
        ArrayList<LogItem> items = new ArrayList<LogItem>();
        items.add(new LogItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, ", new Date()));
        items.add(new LogItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, ", new Date()));
        items.add(new LogItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, ", new Date()));
        items.add(new LogItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, ", new Date()));
        items.add(new LogItem("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, ", new Date()));
        return items;
    }
}
