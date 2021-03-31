/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quickstep.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.widget.Button;

import com.android.launcher3.statemanager.StatefulActivity;
import com.android.launcher3.touch.PagedOrientationHandler;
import com.android.quickstep.views.RecentsView.PageCallbacks;
import com.android.quickstep.views.RecentsView.ScrollState;

public class ClearAllButton extends Button implements PageCallbacks {

    public static final FloatProperty<ClearAllButton> VISIBILITY_ALPHA =
            new FloatProperty<ClearAllButton>("visibilityAlpha") {
                @Override
                public Float get(ClearAllButton view) {
                    return view.mVisibilityAlpha;
                }

                @Override
                public void setValue(ClearAllButton view, float v) {
                    view.setVisibilityAlpha(v);
                }
            };

    private final StatefulActivity mActivity;
    private float mScrollAlpha = 1;
    private float mContentAlpha = 1;
    private float mVisibilityAlpha = 1;
    private float mGridProgress = 1;

    private boolean mIsRtl;
    private float mNormalTranslationPrimary;
    private float mGridTranslationPrimary;
    private float mGridTranslationSecondary;
    private float mGridScrollOffset;
    private float mScrollOffsetPrimary;

    private int mSidePadding;

    public ClearAllButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        mActivity = StatefulActivity.fromContext(context);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        PagedOrientationHandler orientationHandler = getRecentsView().getPagedOrientationHandler();
        mSidePadding = orientationHandler.getClearAllSidePadding(getRecentsView(), mIsRtl);
    }

    private RecentsView getRecentsView() {
        return (RecentsView) getParent();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        mIsRtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setContentAlpha(float alpha) {
        if (mContentAlpha != alpha) {
            mContentAlpha = alpha;
            updateAlpha();
        }
    }

    public void setVisibilityAlpha(float alpha) {
        if (mVisibilityAlpha != alpha) {
            mVisibilityAlpha = alpha;
            updateAlpha();
        }
    }

    @Override
    public void onPageScroll(ScrollState scrollState, boolean gridEnabled) {
        RecentsView recentsView = getRecentsView();
        if (recentsView == null) {
            return;
        }

        PagedOrientationHandler orientationHandler = recentsView.getPagedOrientationHandler();
        float orientationSize = orientationHandler.getPrimaryValue(getWidth(), getHeight());
        if (orientationSize == 0) {
            return;
        }

        int leftEdgeScroll = recentsView.getLeftMostChildScroll();
        float adjustedScrollFromEdge = scrollState.scrollFromEdge - leftEdgeScroll;
        float shift = Math.min(adjustedScrollFromEdge, orientationSize);
        mNormalTranslationPrimary = mIsRtl ? -shift : shift;
        if (!gridEnabled) {
            mNormalTranslationPrimary += mSidePadding;
        }
        applyPrimaryTranslation();
        applySecondaryTranslation();
        mScrollAlpha = 1 - shift / orientationSize;
        updateAlpha();
    }

    private void updateAlpha() {
        final float alpha = mScrollAlpha * mContentAlpha * mVisibilityAlpha;
        setAlpha(alpha);
        setClickable(Math.min(alpha, 1) == 1);
    }

    public void setGridTranslationPrimary(float gridTranslationPrimary) {
        mGridTranslationPrimary = gridTranslationPrimary;
        applyPrimaryTranslation();
    }

    public void setGridTranslationSecondary(float gridTranslationSecondary) {
        mGridTranslationSecondary = gridTranslationSecondary;
        applyPrimaryTranslation();
    }

    public void setGridScrollOffset(float gridScrollOffset) {
        mGridScrollOffset = gridScrollOffset;
    }

    public void setScrollOffsetPrimary(float scrollOffsetPrimary) {
        mScrollOffsetPrimary = scrollOffsetPrimary;
    }

    public float getScrollAdjustment(boolean gridEnabled) {
        float scrollAdjustment = 0;
        if (gridEnabled) {
            scrollAdjustment += mGridTranslationPrimary + mGridScrollOffset;
        }
        scrollAdjustment += mScrollOffsetPrimary;
        return scrollAdjustment;
    }

    public float getOffsetAdjustment(boolean gridEnabled) {
        return getScrollAdjustment(gridEnabled);
    }

    /**
     * Moves ClearAllButton between carousel and 2 row grid.
     *
     * @param gridProgress 0 = carousel; 1 = 2 row grid.
     */
    public void setGridProgress(float gridProgress) {
        mGridProgress = gridProgress;
        applyPrimaryTranslation();
    }

    private void applyPrimaryTranslation() {
        RecentsView recentsView = getRecentsView();
        if (recentsView == null) {
            return;
        }

        PagedOrientationHandler orientationHandler = recentsView.getPagedOrientationHandler();
        orientationHandler.getPrimaryViewTranslate().set(this,
                orientationHandler.getPrimaryValue(0f, getOriginalTranslationY())
                        + mNormalTranslationPrimary + getGridTrans(mGridTranslationPrimary));
    }

    private void applySecondaryTranslation() {
        RecentsView recentsView = getRecentsView();
        if (recentsView == null) {
            return;
        }

        PagedOrientationHandler orientationHandler = recentsView.getPagedOrientationHandler();
        orientationHandler.getSecondaryViewTranslate().set(this,
                orientationHandler.getSecondaryValue(0f, getOriginalTranslationY())
                        + getGridTrans(mGridTranslationSecondary));
    }

    private float getGridTrans(float endTranslation) {
        return mGridProgress > 0 ? endTranslation : 0;
    }

    /**
     * Get the Y translation that is set in the original layout position, before scrolling.
     */
    private float getOriginalTranslationY() {
        return mActivity.getDeviceProfile().overviewTaskThumbnailTopMarginPx / 2.0f;
    }
}