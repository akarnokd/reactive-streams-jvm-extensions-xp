package hu.akarnokd.reactivestreams.extensions.tck;

import org.reactivestreams.Subscription;

import hu.akarnokd.reactivestreams.extensions.*;
import hu.akarnokd.reactivestreams.extensions.tools.SubscriptionTools;

public class TckFusedSubscriber<T> extends TckStandardSubscriber<T> implements RelaxedSubscriber<T> {

    protected int initialFusionMode;

    protected volatile int actualFusionMode;

    protected volatile FusedQueueSubscription<T> qs;

    public TckFusedSubscriber(int itemTimeoutMillis) {
        super(itemTimeoutMillis);
    }

    public final void setInitialFusionMode(int mode) {
        this.initialFusionMode = mode;
    }

    @Override
    public void onSubscribe(Subscription s) {
        if (upstream.compareAndSet(null, s)) {
            boolean requestInitial = true;
            if (s instanceof FusedQueueSubscription) {
                @SuppressWarnings("unchecked")
                FusedQueueSubscription<T> queue = (FusedQueueSubscription<T>)s;
                this.qs = queue;
                int ifm = initialFusionMode;
                if (ifm != FusedQueueSubscription.NONE) {
                    int m = qs.requestFusion(ifm);
                    if (m == FusedQueueSubscription.SYNC) {
                        requestInitial = false;
                        actualFusionMode = m;
                        try {
                            T v;

                            while ((v = queue.poll()) != null) {
                                super.onNext(v);
                            }
                            super.onComplete();
                        } catch (Throwable ex) {
                            queue.clear();
                            super.onError(ex);
                        }
                    } else
                    if (m == FusedQueueSubscription.ASYNC) {
                        actualFusionMode = m;
                    }
                }
            }
            if (requestInitial) {
                long r = requested.getAndSet(0L);
                if (r != 0L) {
                    s.request(r);
                }
            }
        } else {
            if (!SubscriptionTools.isCancelled(upstream)) {
                onError(new IllegalStateException("Subscription already set!"));
            }
        }
        subscribeCount++;
        subscribed.countDown();
    }

    @Override
    public void request(long n) {
        if (actualFusionMode == FusedQueueSubscription.SYNC) {
            throw fail("request() should not be called in SYNC-fused mode!");
        }
        super.request(n);
    }

    @Override
    public void onNext(T t) {
        int afm = actualFusionMode;
        if (afm == FusedQueueSubscription.NONE) {
            super.onNext(t);
        } else {
            if (afm == FusedQueueSubscription.SYNC) {
                super.onError(new IllegalStateException("onNext called in SYNC-fused mode: " + valueAndClass(t)));
            } else {
                FusedQueueSubscription<T> queue = qs;
                T v;

                try {
                    while ((v = queue.poll()) != null) {
                        super.onNext(v);
                    }
                } catch (Throwable ex) {
                    queue.clear();
                    super.onError(ex);
                }
            }
        }
    }

    @Override
    public void onError(Throwable t) {
        int afm = actualFusionMode;
        if (afm != FusedQueueSubscription.SYNC) {
            super.onError(t);
        } else {
            super.onError(new IllegalStateException("onError called in SYNC-fused mode", t));
        }
    }

    @Override
    public void onComplete() {
        int afm = actualFusionMode;
        if (afm != FusedQueueSubscription.SYNC) {
            super.onComplete();
        } else {
            super.onError(new IllegalStateException("onComplete called in SYNC-fused mode"));
        }
    }

    protected final String fusionString(int mode) {
        StringBuilder b = new StringBuilder();
        if ((mode & FusedQueueSubscription.ANY) == FusedQueueSubscription.ANY) {
            b.append("ANY(3)");
        } else
        if ((mode & FusedQueueSubscription.SYNC) == FusedQueueSubscription.SYNC) {
            b.append("SYNC(1)");
        } else
        if ((mode & FusedQueueSubscription.ASYNC) == FusedQueueSubscription.ASYNC) {
            b.append("ASYNC(2)");
        }
        if ((mode & FusedQueueSubscription.BOUNDARY) != 0) {
            if (b.length() > 0) {
                b.append(" | BOUNDARY(4)");
            } else {
                b.append("BOUNDARY(4)");
            }
        }
        return b.toString();
    }

    public final Subscription subscription() {
        return upstream.get();
    }

    public final FusedQueue<T> fusedQueue() {
        return qs;
    }

    public final void expectFusionMode(int mode) {
        int afm = actualFusionMode;
        if (mode == FusedQueueSubscription.ANY) {
            if ((afm & FusedQueueSubscription.ANY) == 0) {
                throw fail("Different fusion mode. Expected: " + fusionString(mode) + ", Actual: " + fusionString(afm));
            }
        } else
        if (mode != afm) {
            throw fail("Different fusion mode. Expected: " + fusionString(mode) + ", Actual: " + fusionString(afm));
        }
    }

    public final void expectFusedSubscribe() throws Throwable {
        expectSubscribe();
        if (qs == null) {
            throw fail("No FusedQueueSubscription received via onSubscribe");
        }
    }
}
