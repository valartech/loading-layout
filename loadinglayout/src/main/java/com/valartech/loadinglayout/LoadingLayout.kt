package com.valartech.loadinglayout

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IntDef
import androidx.annotation.StringRes
import androidx.core.view.children

/**
 * Layout with support for easily switching between a loading state, a final loaded state and an
 * optional zero ("empty") state.
 * <p />
 *
 * There are 2 ways to use this layout:
 * 1. Tags (preferred): tag your views with the strings [R.string.ll_loading], [R.string.ll_complete],
 * [R.string.ll_error] and [R.string.ll_empty].
 * 2. Ordering:
 * Add in, top to bottom: loading view(like a progressbar), loaded view(actual layout), a view for
 * the zero state and a view for the error state.
 * <p />
 * Note that the order of the views as laid out in XML is significant if the second method is
 * used: this layout will misbehave if the order noted above isn't followed.
 */
@Suppress("MemberVisibilityCanBePrivate")
class LoadingLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var loadingView: View? = null
    private var completeView: View? = null
    private var emptyView: View? = null
    private var overlayView: View? = null
    private var errorView: View? = null
    private val defaultState: Int
    private val overlayTint: Int
    private val crossFadeSuccess: Boolean
    private var currentState: Int? = null
    private val shortAnimDuration =
        resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

    /**
     * Duration of the crossfade animation for LOADING -> COMPLETE. Set to 0 to disable.
     */
    var animationDuration = shortAnimDuration
        set(value) {
            require(value >= 0) { "Duration needs to be >= 0" }
            field = value
        }

    init {
        //get values from attrs
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.LoadingLayout, 0, 0
        )

        defaultState = a.getInt(R.styleable.LoadingLayout_default_state, COMPLETE)
        overlayTint = a.getColor(R.styleable.LoadingLayout_overlay_tint, Color.TRANSPARENT)
        crossFadeSuccess = a.getBoolean(R.styleable.LoadingLayout_cross_fade_success, true)
        a.recycle()
    }

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(LOADING, LOADING_OVERLAY, COMPLETE, EMPTY, ERROR)
    annotation class ViewState

    override fun onFinishInflate() {
        super.onFinishInflate()
        //Use the default order to find views
        loadingView = getChildAt(0)
        completeView = getChildAt(1)
        emptyView = getChildAt(2)
        errorView = getChildAt(3)

        //If views are specified with tags, override
        findChildViewWithTag(R.string.ll_loading)?.let { loadingView = it }
        findChildViewWithTag(R.string.ll_complete)?.let { completeView = it }
        findChildViewWithTag(R.string.ll_empty)?.let { emptyView = it }
        findChildViewWithTag(R.string.ll_error)?.let { errorView = it }

        if (!isInEditMode) {
            if (loadingView == null) {
                throw IllegalStateException("No child views present in this layout. Loading and Loaded view are required.")
            }
            if (completeView == null) {
                throw IllegalStateException("Either loading or complete view is missing. Both are required")
            }
        }
        //add in overlay view
        overlayView = View.inflate(context, R.layout.loading_layout_overlay, null)
        overlayView?.setBackgroundColor(overlayTint)
        addView(overlayView)

        //make sure the views are in order
        errorView?.bringToFront() //bottom-most
        emptyView?.bringToFront()
        completeView?.bringToFront()
        overlayView?.bringToFront()
        loadingView?.bringToFront() //top-most
        invalidate()

        //default state
        setState(defaultState)
    }

    fun setState(@ViewState viewState: Int) {
        if (viewState == currentState) {
            return
        }
        when (viewState) {
            EMPTY -> {
                loadingView?.visibility = View.GONE
                completeView?.visibility = View.GONE
                emptyView?.visibility = View.VISIBLE
                errorView?.visibility = View.GONE
                overlayView?.visibility = View.GONE
            }
            LOADING -> {
                loadingView?.alpha = 1f
                loadingView?.visibility = View.VISIBLE
                completeView?.visibility = View.GONE
                emptyView?.visibility = View.GONE
                errorView?.visibility = View.GONE
                overlayView?.visibility = View.GONE
            }
            LOADING_OVERLAY -> {
                loadingView?.alpha = 1f
                loadingView?.visibility = View.VISIBLE
                completeView?.visibility = View.VISIBLE
                emptyView?.visibility = View.GONE
                errorView?.visibility = View.GONE
                overlayView?.visibility = View.VISIBLE
            }
            COMPLETE -> {
                emptyView?.visibility = View.GONE
                errorView?.visibility = View.GONE
                overlayView?.visibility = View.GONE
                if (isInEditMode) {
                    loadingView?.visibility = View.GONE
                    completeView?.visibility = View.VISIBLE
                } else if (currentState == LOADING && crossFadeSuccess) {
                    //if we're showing results after loading, animate the appearance of the
                    //complete view
                    crossfadeCompleteView()
                } else {
                    //if we're showing the complete state in any other case, then just show the
                    //view immediately
                    loadingView?.visibility = View.GONE
                    completeView?.visibility = View.VISIBLE
                }
            }
            ERROR -> {
                loadingView?.visibility = View.GONE
                completeView?.visibility = View.GONE
                emptyView?.visibility = View.GONE
                errorView?.visibility = View.VISIBLE
                overlayView?.visibility = View.GONE
            }
        }
        currentState = viewState
    }

    /**
     * https://developer.android.com/training/animation/reveal-or-hide-view#kotlin
     */
    private fun crossfadeCompleteView() {
        completeView?.apply {
            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            alpha = 0f
            visibility = View.VISIBLE

            // Animate the content view to 100% opacity, and clear any animation
            // listener set on the view.
            animate()
                .alpha(1f)
                .setDuration(animationDuration)
                .setListener(null)
        }
        // Animate the loading view to 0% opacity. After the animation ends,
        // set its visibility to GONE as an optimization step (it won't
        // participate in layout passes, etc.)
        loadingView?.animate()
            ?.alpha(0f)
            ?.setDuration(animationDuration)
            ?.setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    loadingView?.visibility = View.GONE
                }
            })
    }

    /**
     * Searches only the direct children of this view for a child view with the specified tag.
     */
    private fun findChildViewWithTag(@StringRes tagRes: Int): View? {
        children.forEach {
            if (context.getString(tagRes) == it.tag) {
                return it
            }
        }
        return null
    }

    companion object {
        //the values of these constants is significant, they correspond to params in attrs.xml
        const val LOADING = 1
        const val LOADING_OVERLAY = 2
        const val COMPLETE = 3
        const val EMPTY = 4
        const val ERROR = 5
    }
}
