package org.projectbuendia.client.utils;

public interface Receiver<T> {
    void receive(T result);
}
