package com.example.blogp.collapsingavatar


import android.animation.ValueAnimator
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.Toolbar
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import timber.log.Timber


class MainActivity : AppCompatActivity() {

    private lateinit var avatarContainerView: ImageView
    private var expandedImageSize: Float = 0F
    private var collapsedImageSize: Float = 0F
    private var activityMargin: Float = 0F
    private var isValuesCalculatedAlready = false
    private lateinit var toolbar: Toolbar
    private lateinit var appBarLayout: AppBarLayout
    private var collapsedHeight: Float = 0F
    private var expandedHeight: Float = 0F
    private var cashCollapseState: Pair<Int, Int>? = null

    private lateinit var titleToolbarText: AppCompatTextView
    private lateinit var titleToolbarTextSingle: AppCompatTextView
    private lateinit var collapsingAvatarContainer: FrameLayout
    private lateinit var background: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        /**/
        expandedImageSize = resources.getDimension(R.dimen.default_expanded_image_size)
        collapsedImageSize = resources.getDimension(R.dimen.default_collapsed_image_size)
        activityMargin = resources.getDimension(R.dimen.activity_margin)
        collapsingAvatarContainer = findViewById(R.id.stuff_container)
        appBarLayout = findViewById(R.id.app_bar_layout)
        toolbar = findViewById(R.id.anim_toolbar)
        avatarContainerView = findViewById(R.id.imgb_avatar_wrap)
        titleToolbarText = findViewById(R.id.tv_profile_name)
        titleToolbarTextSingle = findViewById(R.id.tv_profile_name_single)
        background = findViewById(R.id.fl_background)
        /**/
        appBarLayout.addOnOffsetChangedListener(
                AppBarLayout.OnOffsetChangedListener { appBarLayout, i ->
                    if (!isValuesCalculatedAlready) {
                        collapsedHeight = toolbar.height.toFloat()
                        expandedHeight = appBarLayout.totalScrollRange.toFloat() //appBarLayout.height - toolbar.height
                        isValuesCalculatedAlready = true
                        Timber.d(" \n app bar height = ${appBarLayout.totalScrollRange.toFloat()} ; \n toolbar height = ${toolbar.height.toFloat()} ;  \n appbar height = ${appBarLayout.height} ; \n  collapsed height = $collapsedHeight ;\n expanded height (max offset) = $expandedHeight ")
                    }

                    val offset = Math.abs(i / appBarLayout.totalScrollRange.toFloat())

                    Timber.d(" expand perc = $offset  offset = $offset")

                    updateViews(offset)
                })
    }

    private fun updateViews(percentOffset: Float) {
        var translationY = 0f
        var currHeight = 0f
        var translationX: Float
        var currentImageSize = 0

        //Collapsing avatar transparent
        when {
            percentOffset > mLowerLimitTransparently && percentOffset < mUpperLimitTransparently -> {
                if (avatarContainerView.alpha != percentOffset) {
                    avatarContainerView.alpha = percentOffset
                    avatarContainerView.invalidate()
                }
            }
            percentOffset > mUpperLimitTransparently && percentOffset < ABROAD -> {
                avatarContainerView.alpha = 0.0f
                avatarContainerView.invalidate()
            }
            else ->
                if (avatarContainerView.alpha != 1f) {
                    avatarContainerView.alpha = 1f
                    avatarContainerView.invalidate()
                }
        }

        //Collapsed/expended sizes for views
        when {
            percentOffset < ABROAD -> Pair(TO_EXPANDED_STATE, cashCollapseState?.second
                    ?: WAIT_FOR_SWITCH)
            else -> Pair(TO_COLLAPSED_STATE, cashCollapseState?.second ?: WAIT_FOR_SWITCH)
        }.apply {
            when {
                cashCollapseState != null && cashCollapseState != this -> {
                    when (first) {
                        TO_EXPANDED_STATE -> {
                            translationY = toolbar.height.toFloat()
                            currHeight = expandedHeight
                            currentImageSize = expandedImageSize.toInt()
                            avatarContainerView.translationX = 0F
                            titleToolbarText.visibility = View.VISIBLE
                            titleToolbarTextSingle.visibility = View.INVISIBLE
                            background.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.color_transparent))
                        }

                        TO_COLLAPSED_STATE -> {
                            background.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.colorPrimary))
                            currentImageSize = collapsedImageSize.toInt()
                            translationY = (appBarLayout.height - toolbar.height).toFloat()
                            currHeight = collapsedHeight
                            translationX = appBarLayout.width / 2f - collapsedImageSize / 2 - activityMargin * 2
                            /**/
                            ValueAnimator.ofFloat(avatarContainerView.translationX, translationX).apply {
                                addUpdateListener { avatarContainerView.translationX = it.animatedValue as Float }
                                (first == TO_COLLAPSED_STATE).apply {
                                    if (this) interpolator = LinearInterpolator()
                                }
                                start()
                            }
                            /**/
                            titleToolbarText.visibility = View.INVISIBLE
                            titleToolbarTextSingle.visibility = View.VISIBLE
                            titleToolbarTextSingle.let {
                                it.apply {
                                    alpha = 0.0f
                                    this.translationX = width.toFloat() / 2
                                    animate().translationX(0f)
                                            .alpha(1.0f)
                                            .setDuration(450)
                                            .setListener(null)
                                }
                            }

                        }
                    }
                    collapsingAvatarContainer.translationY = translationY
                    collapsingAvatarContainer.layoutParams.height = currHeight.toInt()
                    avatarContainerView.layoutParams.height = currentImageSize
                    avatarContainerView.layoutParams.width = currentImageSize
                    collapsingAvatarContainer.requestLayout()
                    /**/
                    cashCollapseState = Pair(first, SWITCHED)
                }
                else -> {
                    cashCollapseState = Pair(first, WAIT_FOR_SWITCH)
                }
            }
        }
    }

    companion object {
        const val ABROAD = 0.99f
        const val TO_EXPANDED_STATE = 0
        const val TO_COLLAPSED_STATE = 1
        const val WAIT_FOR_SWITCH = 0
        const val SWITCHED = 1
    }

    private val mLowerLimitTransparently = ABROAD * 0.45
    private val mUpperLimitTransparently = ABROAD * 0.69
}
