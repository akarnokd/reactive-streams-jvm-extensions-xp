package hu.akarnokd.reactivestreams.extensions.tck;

import org.reactivestreams.*;

public abstract class ConstantPublisherVerification<T> {
    
    public abstract Publisher<T> createPublisher();
    
    public Publisher<T> createErrorPublisher() {
        return null;
    }
}