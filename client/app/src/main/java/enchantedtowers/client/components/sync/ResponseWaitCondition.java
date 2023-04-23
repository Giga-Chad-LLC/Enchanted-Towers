package enchantedtowers.client.components.sync;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.grpc.stub.StreamObserver;


public class ResponseWaitCondition {
    private final Lock lock = new ReentrantLock();
    private final Condition responseReceivedCondition  = lock.newCondition();
    private boolean responseReceived = false;
    private boolean serverReturnedError = false;

    public void lock() {
        lock.lock();
    }

    public void unlock() {
        lock.unlock();
    }

    public void await() {
        try {
            while(!responseReceived) {
                responseReceivedCondition.await();
            }
        }
        catch (InterruptedException err) {
            throw new RuntimeException(err);
        }
    }

    public void signal() {
        responseReceived = true;
        responseReceivedCondition.signal();
    }

    public void registerServerError() {
        serverReturnedError = true;
    }

    public boolean serverReturnedError() {
        return this.serverReturnedError;
    }
}
