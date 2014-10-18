package org.msf.records.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Gil on 12/10/2014.
 */
public class Flag {

    public String type;
    public Date created;
    public String comment;
    public Date completed;

    public Flag(String type, String comment, Date created, Date completed){
        this.type = type;
        this.comment = comment;
        this.created = created;
        this.completed = completed;
    }


    public static List<Flag> GETDUMMYDATA1(){
        ArrayList<Flag> flags = new ArrayList<Flag>();
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), null));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), null));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), null));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), null));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), null));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), null));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), null));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), null));
        return flags;
    }

    public static List<Flag> GETDUMMYDATA2(){
        ArrayList<Flag> flags = new ArrayList<Flag>();
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), new Date()));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), new Date()));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), new Date()));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), new Date()));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), new Date()));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), new Date()));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), new Date()));
        flags.add(new Flag("Blood test", "Test required", new Date((new Date().getTime() - 86400 * 3)), new Date()));
        return flags;
    }
}
