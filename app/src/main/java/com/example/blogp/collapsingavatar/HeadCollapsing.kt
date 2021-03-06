package com.example.blogp.collapsingavatar


import android.animation.ValueAnimator
import android.content.Context
import android.support.design.widget.AppBarLayout
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView


class HeadCollapsing(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs), AppBarLayout.OnOffsetChangedListener {

    private lateinit var avatarContainerView: ImageView
    //private lateinit var avatar: ImageView

    private val expandedImageSize: Float
    private val collapsedImageSize: Float

    private val activityMargin: Float

    private var valuesCalculatedAlready = false
    private lateinit var toolbar: Toolbar
    private lateinit var appBarLayout: AppBarLayout
    private var collapsedHeight: Float = 0.toFloat()
    private var expandedHeight: Float = 0.toFloat()
    private var maxOffset: Float = 0.toFloat()
    private var lastOffset = -1
    private var cashCollapseState: kotlin.Pair<Int, Int>? = null

    //profile title
    private var titleToolbarText: AppCompatTextView? = null
    private var titleTolbarTextSingle: AppCompatTextView? = null

    companion object {
        const val ABROAD = 0.95f
        const val TO_EXPANDED_STATE = 0
        const val TO_COLLAPSED_STATE = 1
        const val WAIT_FOR_SWITCH = 0
        const val SWITCHED = 1
    }

    constructor(context: Context) : this(context, null) {
        init()
    }

    init {
        init()
        val resources = resources
        collapsedImageSize = resources.getDimension(R.dimen.default_collapsed_image_size)
        expandedImageSize = resources.getDimension(R.dimen.default_expanded_image_size)
        activityMargin = resources.getDimension(R.dimen.activity_margin)
    }

    private fun init() {
        //sdo nothing
    }

    private fun findParentAppBarLayout(): AppBarLayout {
        val parent = this.parent
        return parent as? AppBarLayout ?: if (parent.parent is AppBarLayout) {
            parent.parent as AppBarLayout
        } else {
            throw IllegalStateException("Must be inside an AppBarLayout")
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        findViews()
        appBarLayout.addOnOffsetChangedListener(this)
    }

    private fun findViews() {
        appBarLayout = findParentAppBarLayout()
        toolbar = findSiblingToolbar()
        avatarContainerView = findAvatarContainer()

        titleToolbarText = findTextView(appBarLayout, R.id.tv_profile_name)

        if (titleToolbarText != null) {
            titleTolbarTextSingle = findTextView(appBarLayout, R.id.tv_profile_name_single, true)
        }
    }

    private fun findTextView(appBarLayout: AppBarLayout, id: Int, getItAnuWay: Boolean = false): AppCompatTextView? {
        return appBarLayout.findViewById<AppCompatTextView>(id).run {
            when {
                visibility == View.VISIBLE -> this
                getItAnuWay.not() -> null
                else -> this
            }
        }
    }

    private fun findAvatarContainer(): ImageView {
        return findViewById(R.id.imgb_avatar_wrap)
                ?: throw IllegalStateException("View with id imgb_avatar_wrap not found")
    }

    private fun findSiblingToolbar(): Toolbar {
        val parent = this.parent as ViewGroup
        var i = 0
        val c = parent.childCount
        while (i < c) {
            val child = parent.getChildAt(i)
            if (child is Toolbar) {
                return child
            }
            i++
        }
        throw IllegalStateException("No toolbar found as sibling")
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, offset: Int) {
        if (lastOffset == offset) {
            return
        }
        lastOffset = offset
        if (!valuesCalculatedAlready) {
            calculateValues()
            valuesCalculatedAlready = true
        }
        val expandedPercentage = 1 - -offset / maxOffset
        updateViews(expandedPercentage)
    }

    private fun calculateValues() {
        collapsedHeight = toolbar.height.toFloat()
        expandedHeight = (appBarLayout.height - toolbar.height).toFloat()
        maxOffset = expandedHeight
    }


    private fun updateViews(updatePercentage: Float) {
        val inversePercentage = 1 - updatePercentage
        var translationY = 0f //\* expandedPercentage
        var currHeight = 0f
        var state = kotlin.Pair(0, 0)
        var translationX = 0f
        var currentImageSize = 0

        //PUT collapsing avatar transparent
        when {
            inversePercentage > ABROAD * 0.47 && inversePercentage < ABROAD * 0.77 -> {
                if (avatarContainerView.alpha != updatePercentage) {
                    avatarContainerView.alpha = updatePercentage
                    avatarContainerView.invalidate()
                }
            }

            inversePercentage > ABROAD * 0.77 && inversePercentage < ABROAD -> {
                avatarContainerView.alpha = 0.0f
                avatarContainerView.invalidate()
            }

            else -> handler.post {
                if (avatarContainerView.alpha != 1f) {
                    avatarContainerView.alpha = 1f
                    avatarContainerView.invalidate()
                }
            }
        }

        //PUT TOOLBAR title
        when {

            inversePercentage < ABROAD -> {
                state = kotlin.Pair(TO_EXPANDED_STATE, cashCollapseState?.second ?: WAIT_FOR_SWITCH)
                titleToolbarText?.visibility = View.VISIBLE
                titleTolbarTextSingle?.visibility = View.INVISIBLE
            }

            inversePercentage > ABROAD -> {
                state = kotlin.Pair(TO_COLLAPSED_STATE, cashCollapseState?.second
                        ?: WAIT_FOR_SWITCH)

                titleToolbarText?.visibility = View.INVISIBLE
                titleTolbarTextSingle?.visibility = View.VISIBLE


                titleTolbarTextSingle?.let {
                    animateShowText(it)
                }
            }
        }

        //PUT collapsed/expended sizes for views
        when {
            cashCollapseState != null && cashCollapseState != state -> {
                when (state.first) {
                    TO_EXPANDED_STATE -> {
                        translationY = toolbar.height.toFloat()
                        currHeight = expandedHeight
                        translationX = 0f
                        currentImageSize = expandedImageSize.toInt()
                    }
                    TO_COLLAPSED_STATE -> {
                        currentImageSize = collapsedImageSize.toInt()
                        translationY = (appBarLayout.height - toolbar.height).toFloat()
                        currHeight = collapsedHeight
                        translationX = appBarLayout.width / 2f - collapsedImageSize / 2 - activityMargin * 2
                    }
                }
                this.translationY = translationY

                this.layoutParams.height = currHeight.toInt()

                ValueAnimator.ofFloat(avatarContainerView.translationX, translationX).apply {
                    addUpdateListener {
                        avatarContainerView.translationX = it.animatedValue as Float
                    }

                    duration = 350
                    (state.first == TO_COLLAPSED_STATE).apply {
                        if (this) interpolator = LinearInterpolator()
                    }
                    start()
                }

                avatarContainerView.layoutParams.height = currentImageSize
                avatarContainerView.layoutParams.width = currentImageSize

                this.requestLayout()

                //SWITCH STATE CASE
                cashCollapseState = kotlin.Pair(state.first, SWITCHED)
            }
            else -> {
                cashCollapseState = kotlin.Pair(state.first, WAIT_FOR_SWITCH)
            }
        }
    }

    private fun animateShowText(appCompatTextView: AppCompatTextView) {
        appCompatTextView.apply {
            alpha = 0.0f
            this.translationX = width.toFloat() / 2
            animate()
                    .translationX(0f)
                    .alpha(1.0f)
                    .setDuration(500)
                    .setListener(null)
        }
    }

}