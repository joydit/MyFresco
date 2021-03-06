package com.facebook.animation.factory;

/**
 * Created by Administrator on 2017/3/14 0014.
 */

import android.graphics.Rect;
import android.net.Uri;

import com.facebook.cache.commom.CacheKey;
import com.facebook.commom.internal.Supplier;
import com.facebook.commom.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.DrawableFactory;
import com.facebook.animation.backend.AnimationBackend;
import com.facebook.animation.backend.impl.AnimationBackendDelegateWithInactivityCheck;
import com.facebook.animation.bitmap.BitmapAnimationBackend;
import com.facebook.animation.bitmap.BitmapFrameCache;
import com.facebook.animation.bitmap.cache.FrescoFrameCache;
import com.facebook.animation.bitmap.cache.KeepLastFrameCache;
import com.facebook.animation.bitmap.cache.NoOpCache;
import com.facebook.animation.bitmap.wrapper.AnimatedDrawableBackendAnimationInformation;
import com.facebook.animation.bitmap.wrapper.AnimatedDrawableBackendFrameRenderer;
import com.facebook.animation.drawable.AnimatedDrawable2;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableCachingBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.impl.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.animation.bitmap.cache.AnimatedFrameCache;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.cache.impl.CountingMemoryCache;
import com.facebook.imagepipeline.image.impl.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.impl.CloseableImage;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 一个{@link AnimatedDrawable2}的工厂
 * Animation factory for {@link AnimatedDrawable2}.
 *
 * 这个工程和{@link ExperimentalAnimationFactory}相似，但是{@link ExperimentalAnimationFactory}被包裹在新的{@link BitmapAnimationBackend}中
 * 并且没有提供{@link AnimatedDrawableCachingBackend}
 * This factory is similar to {@link ExperimentalAnimationFactory} but it wraps around the new
 * {@link BitmapAnimationBackend} and does not rely on
 * {@link AnimatedDrawableCachingBackend}
 * to do the caching.
 */
public class ExperimentalBitmapAnimationDrawableFactory implements DrawableFactory {

    public static final int CACHING_STRATEGY_NO_CACHE = 0;
    public static final int CACHING_STRATEGY_FRESCO_CACHE = 1;
    public static final int CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING = 2;
    public static final int CACHING_STRATEGY_KEEP_LAST_CACHE = 3;

    private final AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
    private final ScheduledExecutorService mScheduledExecutorServiceForUiThread;
    private final MonotonicClock mMonotonicClock;
    private final PlatformBitmapFactory mPlatformBitmapFactory;
    private final CountingMemoryCache<CacheKey, CloseableImage> mBackingCache;
    private final Supplier<Integer> mCachingStrategySupplier;

    public ExperimentalBitmapAnimationDrawableFactory(
            AnimatedDrawableBackendProvider animatedDrawableBackendProvider,
            ScheduledExecutorService scheduledExecutorServiceForUiThread,
            MonotonicClock monotonicClock,
            PlatformBitmapFactory platformBitmapFactory,
            CountingMemoryCache<CacheKey, CloseableImage> backingCache,
            Supplier<Integer> cachingStrategySupplier) {
        mAnimatedDrawableBackendProvider = animatedDrawableBackendProvider;
        mScheduledExecutorServiceForUiThread = scheduledExecutorServiceForUiThread;
        mMonotonicClock = monotonicClock;
        mPlatformBitmapFactory = platformBitmapFactory;
        mBackingCache = backingCache;
        mCachingStrategySupplier = cachingStrategySupplier;
    }

    @Override
    public boolean supportsImageType(CloseableImage image) {
        return image instanceof CloseableAnimatedImage;
    }

    @Override
    public AnimatedDrawable2 createDrawable(CloseableImage image) {
        return new AnimatedDrawable2(
                createAnimationBackend(
                        ((CloseableAnimatedImage) image).getImageResult()));
    }

    private AnimationBackend createAnimationBackend(AnimatedImageResult animatedImageResult) {
        AnimatedDrawableBackend animatedDrawableBackend =
                createAnimatedDrawableBackend(animatedImageResult);

        BitmapFrameCache bitmapFrameCache = createBitmapFrameCache(animatedImageResult);

        BitmapAnimationBackend bitmapAnimationBackend = new BitmapAnimationBackend(
                mPlatformBitmapFactory,
                bitmapFrameCache,
                new AnimatedDrawableBackendAnimationInformation(animatedDrawableBackend),
                new AnimatedDrawableBackendFrameRenderer(bitmapFrameCache, animatedDrawableBackend));

        return AnimationBackendDelegateWithInactivityCheck.createForBackend(
                bitmapAnimationBackend,
                mMonotonicClock,
                mScheduledExecutorServiceForUiThread);
    }

    private AnimatedDrawableBackend createAnimatedDrawableBackend(
            AnimatedImageResult animatedImageResult) {
        AnimatedImage animatedImage = animatedImageResult.getImage();
        Rect initialBounds = new Rect(0, 0, animatedImage.getWidth(), animatedImage.getHeight());
        return mAnimatedDrawableBackendProvider.get(animatedImageResult, initialBounds);
    }

    private BitmapFrameCache createBitmapFrameCache(AnimatedImageResult animatedImageResult) {
        switch (mCachingStrategySupplier.get()) {
            case CACHING_STRATEGY_FRESCO_CACHE:
                return new FrescoFrameCache(createAnimatedFrameCache(animatedImageResult), true);
            case CACHING_STRATEGY_FRESCO_CACHE_NO_REUSING:
                return new FrescoFrameCache(createAnimatedFrameCache(animatedImageResult), false);
            case CACHING_STRATEGY_KEEP_LAST_CACHE:
                return new KeepLastFrameCache();
            case CACHING_STRATEGY_NO_CACHE:
            default:
                return new NoOpCache();
        }
    }

    private AnimatedFrameCache createAnimatedFrameCache(
            final AnimatedImageResult animatedImageResult) {
        return new AnimatedFrameCache(
                new AnimationFrameCacheKey(animatedImageResult.hashCode()),
                mBackingCache);
    }

    public static class AnimationFrameCacheKey implements CacheKey {

        private static final String URI_PREFIX = "anim://";

        private final String mAnimationUriString;

        public AnimationFrameCacheKey(int imageId) {
            mAnimationUriString = URI_PREFIX + imageId;
        }

        @Override
        public boolean containsUri(Uri uri) {
            return uri.toString().startsWith(mAnimationUriString);
        }

        @Override
        public String getUriString() {
            return mAnimationUriString;
        }
    }
}
