package app.revanced.integrations.youtube.sponsorblock.ui;

import static app.revanced.integrations.shared.utils.ResourceUtils.getDimension;
import static app.revanced.integrations.shared.utils.ResourceUtils.getIdentifier;
import static app.revanced.integrations.shared.utils.ResourceUtils.getLayoutIdentifier;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import app.revanced.integrations.shared.utils.Logger;
import app.revanced.integrations.shared.utils.ResourceUtils;
import app.revanced.integrations.youtube.patches.video.VideoInformation;
import app.revanced.integrations.youtube.settings.Settings;
import app.revanced.integrations.youtube.sponsorblock.SponsorBlockUtils;

public final class NewSegmentLayout extends FrameLayout {
    private static final ColorStateList rippleColorStateList = new ColorStateList(
            new int[][]{new int[]{android.R.attr.state_enabled}},
            new int[]{0x33ffffff} // sets the ripple color to white
    );
    final int defaultBottomMargin;
    final int ctaBottomMargin;
    final int hiddenBottomMargin;
    private final int rippleEffectId;

    public NewSegmentLayout(final Context context) {
        this(context, null);
    }

    public NewSegmentLayout(final Context context, final AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public NewSegmentLayout(final Context context, final AttributeSet attributeSet, final int defStyleAttr) {
        this(context, attributeSet, defStyleAttr, 0);
    }

    public NewSegmentLayout(final Context context, final AttributeSet attributeSet,
                            final int defStyleAttr, final int defStyleRes) {
        super(context, attributeSet, defStyleAttr, defStyleRes);

        LayoutInflater.from(context).inflate(getLayoutIdentifier("revanced_sb_new_segment"), this, true);


        TypedValue rippleEffect = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, rippleEffect, true);
        rippleEffectId = rippleEffect.resourceId;

        initializeButton(
                context,
                "revanced_sb_new_segment_rewind",
                () -> VideoInformation.seekToRelative(-Settings.SB_CREATE_NEW_SEGMENT_STEP.get()),
                "Rewind button clicked"
        );

        initializeButton(
                context,
                "revanced_sb_new_segment_forward",
                () -> VideoInformation.seekToRelative(Settings.SB_CREATE_NEW_SEGMENT_STEP.get()),
                "Forward button clicked"
        );

        initializeButton(
                context,
                "revanced_sb_new_segment_adjust",
                SponsorBlockUtils::onMarkLocationClicked,
                "Adjust button clicked"
        );

        initializeButton(
                context,
                "revanced_sb_new_segment_compare",
                SponsorBlockUtils::onPreviewClicked,
                "Compare button clicked"
        );

        initializeButton(
                context,
                "revanced_sb_new_segment_edit",
                SponsorBlockUtils::onEditByHandClicked,
                "Edit button clicked"
        );

        initializeButton(
                context,
                "revanced_sb_new_segment_publish",
                SponsorBlockUtils::onPublishClicked,
                "Publish button clicked"
        );

        defaultBottomMargin = getDimension("brand_interaction_default_bottom_margin");  // dimen:skip_button_default_bottom_margin
        ctaBottomMargin = getDimension("brand_interaction_cta_bottom_margin");  // dimen:skip_button_cta_bottom_margin
        hiddenBottomMargin = (int) Math.round((ctaBottomMargin) * 0.5);  // margin when the button container is hidden
    }

    /**
     * Initializes a segment button with the given resource identifier name with the given handler and a ripple effect.
     *
     * @param context                The context.
     * @param resourceIdentifierName The resource identifier name for the button.
     * @param handler                The handler for the button's click event.
     * @param debugMessage           The debug message to print when the button is clicked.
     */
    private void initializeButton(final Context context, final String resourceIdentifierName,
                                  final ButtonOnClickHandlerFunction handler, final String debugMessage) {
        final ImageButton button = findViewById(getIdentifier(resourceIdentifierName, ResourceUtils.ResourceType.ID, context));

        // Add ripple effect
        button.setBackgroundResource(rippleEffectId);
        RippleDrawable rippleDrawable = (RippleDrawable) button.getBackground();
        rippleDrawable.setColor(rippleColorStateList);

        button.setOnClickListener((v) -> {
            handler.apply();
            Logger.printDebug(() -> debugMessage);
        });
    }

    @FunctionalInterface
    public interface ButtonOnClickHandlerFunction {
        void apply();
    }
}
