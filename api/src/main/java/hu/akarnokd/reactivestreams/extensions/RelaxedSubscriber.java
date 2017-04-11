package hu.akarnokd.reactivestreams.extensions;

import org.reactivestreams.*;

public interface RelaxedSubscriber<T> extends Subscriber<T> {

    @Override
    void onSubscribe(Subscription s);

}