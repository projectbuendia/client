package org.msf.records.events;

public interface CrudEventBus {

    void register(Object subscriber);

    void unregister(Object subscriber);

    void post(Object event);

}