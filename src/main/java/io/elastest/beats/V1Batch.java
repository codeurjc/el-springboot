package io.elastest.beats;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of {@link Batch} intended for batches constructed from v1 protocol
 *
 */
public class V1Batch implements Batch{

    private int batchSize;
    private List<Message> messages = new ArrayList<>();
    private byte protocol = Protocol.VERSION_1;

    @Override
    public byte getProtocol() {
        return protocol;
    }

    public void setProtocol(byte protocol){
        this.protocol = protocol;
    }

    /**
     * Add Message to the batch
     * @param message Message to add to the batch
     */
    void addMessage(Message message){
        message.setBatch(this);
        messages.add(message);
    }

    @Override
    public Iterator<Message> iterator(){
        return messages.iterator();
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public void setBatchSize(int batchSize){
        this.batchSize = batchSize;
    }

    @Override
    public int size() {
        return messages.size();
    }

    @Override
    public boolean isEmpty() {
        return 0 == messages.size();
    }

    @Override
    public boolean isComplete() {
        return size() == getBatchSize();
    }

    @Override
    public void release() {
        //no-op
    }
}
