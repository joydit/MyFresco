package com.facebook.imagepipeline.producers.networkFetchAndProducer;

/**
 * Created by heshixiyang on 2017/3/9.
 */

import android.net.Uri;

import com.facebook.imagepipeline.image.impl.EncodedImage;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.imagepipeline.producers.ProducerListener;

/**
 * 被{@link NetworkFetcher}使用与描述 获取网络图片的状态
 * Used by {@link NetworkFetcher} to encapsulate the state of one network fetch.
 *
 * 实现的子类可以储存更多在获取过成之中的字段
 * <p>Implementations can subclass this to store additional fetch-scoped fields.
 */
public class FetchState {

    private final Consumer<EncodedImage> mConsumer;
    private final ProducerContext mContext;
    private long mLastIntermediateResultTimeMs;

    public FetchState(
            Consumer<EncodedImage> consumer,
            ProducerContext context) {
        mConsumer = consumer;
        mContext = context;
        mLastIntermediateResultTimeMs = 0;
    }

    public Consumer<EncodedImage> getConsumer() {
        return mConsumer;
    }

    public ProducerContext getContext() {
        return mContext;
    }

    public String getId() {
        return mContext.getId();
    }

    public ProducerListener getListener() {
        return mContext.getListener();
    }

    public Uri getUri() {
        return mContext.getImageRequest().getSourceUri();
    }

    public long getLastIntermediateResultTimeMs() {
        return mLastIntermediateResultTimeMs;
    }

    public void setLastIntermediateResultTimeMs(long lastIntermediateResultTimeMs) {
        mLastIntermediateResultTimeMs = lastIntermediateResultTimeMs;
    }
}
