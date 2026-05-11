package com.aexon.button;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import com.aexon.annotation.NonNull;
import com.aexon.annotation.Nullable;
import com.aexon.R;

public class AexonSliderToAction extends LinearLayout {
	
	// ─── Constants ────────────────────────────────────────────────────────────
	public static final int MODE_BOX  = 0;
	public static final int MODE_OVAL = 1;
	
	// Mulai rotasi saat progress 70%, selesai di 100% — mirip cortinico
	private static final float ROTATION_START = 0.7f;
	private static final float ROTATION_END   = 1.0f;
	private static final float MAX_ROTATION   = 180f;
	
	// ─── Listener ─────────────────────────────────────────────────────────────
	public interface OnSlideCompleteListener {
		void onSlideComplete();
	}
	
	// ─── Paints ───────────────────────────────────────────────────────────────
	private Paint backgroundPaint;
	private Paint thumbPaint;
	private Paint textPaint;
	private Paint arrowPaint;
	private Paint checkPaint;
	
	// ─── Geometry ─────────────────────────────────────────────────────────────
	private RectF backgroundRect;
	private RectF thumbRect;
	private Path  checkPath;
	
	// ─── Drawable ─────────────────────────────────────────────────────────────
	private Drawable iconThumb;
	
	// ─── State ────────────────────────────────────────────────────────────────
	private float   thumbPosition;
	private float   startX;
	private boolean isSliding;
	private boolean isCompleted;
	private boolean hideThumbImmediately;
	private float   thumbMargin;
	private float   maxThumbPosition;
	
	// ─── Radius/margin overrides ──────────────────────────────────────────────
	private float customTrackRadiusPx = -1f;
	private float customThumbRadiusPx = -1f;
	private float customThumbMarginPx = -1f;
	private float trackRadius;
	private float thumbRadius;
	
	// ─── Properties ───────────────────────────────────────────────────────────
	private String  slideText        = "Slide to act";
	private int     sliderColor      = Color.parseColor("#673AB7");
	private int     thumbColor       = Color.parseColor("#EEEEEE");
	private int     textColor        = Color.parseColor("#EEEEEE");
	private int     iconTint;
	private int     completeIconColor = Color.WHITE;
	private int     backgroundMode   = MODE_BOX;
	private boolean useThreeArrows   = false;
	private boolean slideFromLeft    = true;
	private boolean bounceOnStart    = false;
	private boolean bounceExecuted   = false;
	private boolean isLocked         = true;
	
	// ─── Animators ────────────────────────────────────────────────────────────
	private ValueAnimator bounceAnimator;
	private ValueAnimator slideAnimator;
	private ValueAnimator checkmarkAnimator;
	private ValueAnimator backgroundShrinkAnimator;
	
	private float checkmarkProgress  = 0f;
	private float backgroundProgress = 0f;
	
	// ─── Listener ─────────────────────────────────────────────────────────────
	private OnSlideCompleteListener slideCompleteListener;
	
	// ─── Constructors ─────────────────────────────────────────────────────────
	
	public AexonSliderToAction(@NonNull Context context) {
		super(context);
		init(context, null);
	}
	
	public AexonSliderToAction(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}
	
	public AexonSliderToAction(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}
	
	// ─── Init ─────────────────────────────────────────────────────────────────
	
	private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
		setWillNotDraw(false);
		setLayerType(LAYER_TYPE_SOFTWARE, null);
		
		iconTint = sliderColor;
		
		float textSizePx = TypedValue.applyDimension(
		TypedValue.COMPLEX_UNIT_SP, 12f, getResources().getDisplayMetrics()
		);
		Typeface typeface = null;
		
		if (attrs != null) {
			// ── Atribut Android standard ──
			int[] androidAttrs = new int[]{
				android.R.attr.text,
				android.R.attr.textColor,
				android.R.attr.textSize,
				android.R.attr.fontFamily,
				android.R.attr.textStyle
			};
			TypedArray androidTa = context.obtainStyledAttributes(attrs, androidAttrs);
			try {
				CharSequence text = androidTa.getText(0);
				if (text != null) slideText = text.toString();
				textColor  = androidTa.getColor(1, textColor);
				textSizePx = androidTa.getDimension(2, textSizePx);
				
				String fontFamily = androidTa.getString(3);
				int    textStyle  = androidTa.getInt(4, Typeface.NORMAL);
				
				if (fontFamily != null && !fontFamily.isEmpty()) {
					try {
						int fontResId = context.getResources().getIdentifier(
						fontFamily.replace("@font/", ""), "font", context.getPackageName()
						);
						if (fontResId != 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							typeface = context.getResources().getFont(fontResId);
						}
						if (typeface == null) typeface = Typeface.create(fontFamily, textStyle);
					} catch (Exception e) {
						typeface = Typeface.create(fontFamily, textStyle);
					}
				} else {
					typeface = Typeface.defaultFromStyle(textStyle);
				}
			} finally {
				androidTa.recycle();
			}
			
			// ── Atribut custom ──
			TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AexonSliderToAction);
			try {
				sliderColor         = ta.getColor(R.styleable.AexonSliderToAction_sliderColor, sliderColor);
				thumbColor          = ta.getColor(R.styleable.AexonSliderToAction_thumbColor, thumbColor);
				iconTint            = ta.getColor(R.styleable.AexonSliderToAction_tint, sliderColor);
				completeIconColor   = ta.getColor(R.styleable.AexonSliderToAction_complete_icon_color, Color.WHITE);
				customThumbMarginPx = ta.getDimension(R.styleable.AexonSliderToAction_margin_thumb, -1f);
				customThumbRadiusPx = ta.getDimension(R.styleable.AexonSliderToAction_radius_thumb, -1f);
				customTrackRadiusPx = ta.getDimension(R.styleable.AexonSliderToAction_radius_track, -1f);
				useThreeArrows      = ta.getBoolean(R.styleable.AexonSliderToAction_use_three_arrows, false);
				slideFromLeft       = ta.getBoolean(R.styleable.AexonSliderToAction_slide_from_left, true);
				backgroundMode      = ta.getInt(R.styleable.AexonSliderToAction_background_mode, MODE_BOX);
				bounceOnStart       = ta.getBoolean(R.styleable.AexonSliderToAction_bounce_on_start, false);
				isLocked            = ta.getBoolean(R.styleable.AexonSliderToAction_locked, true);
				boolean enabled     = ta.getBoolean(R.styleable.AexonSliderToAction_slide_enabled, true);
				setEnabled(enabled);
				
				int drawableResId = ta.getResourceId(R.styleable.AexonSliderToAction_icon, 0);
				if (drawableResId != 0) {
					try {
						iconThumb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
						? context.getDrawable(drawableResId)
						: getResources().getDrawable(drawableResId);
					} catch (Exception e) {
						iconThumb = null;
					}
				}
			} finally {
				ta.recycle();
			}
		}
		
		// ── Setup paints ──
		backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		backgroundPaint.setColor(sliderColor);
		
		thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		thumbPaint.setColor(thumbColor);
		thumbPaint.setShadowLayer(8, 0, 4, Color.parseColor("#40000000"));
		
		arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		arrowPaint.setStyle(Paint.Style.FILL);
		
		textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setColor(textColor);
		textPaint.setTextSize(textSizePx);
		textPaint.setTextAlign(Paint.Align.CENTER);
		if (typeface != null) textPaint.setTypeface(typeface);
		
		checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		checkPaint.setColor(completeIconColor);
		checkPaint.setStyle(Paint.Style.STROKE);
		checkPaint.setStrokeCap(Paint.Cap.ROUND);
		
		checkPath      = new Path();
		backgroundRect = new RectF();
		thumbRect      = new RectF();
		
		thumbPosition        = 0;
		isSliding            = false;
		isCompleted          = false;
		hideThumbImmediately = false;
	}
	
	// ─── Public API ───────────────────────────────────────────────────────────
	
	public void setText(@NonNull String text) {
		slideText = text;
		invalidate();
	}
	
	public void setTextColor(int color) {
		textColor = color;
		textPaint.setColor(color);
		invalidate();
	}
	
	public void setTextSize(float sp) {
		float px = TypedValue.applyDimension(
		TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics()
		);
		textPaint.setTextSize(px);
		invalidate();
	}
	
	public void setTypeface(@Nullable Typeface typeface) {
		textPaint.setTypeface(typeface);
		invalidate();
	}
	
	public void setThumbColor(int color) {
		thumbColor = color;
		thumbPaint.setColor(color);
		invalidate();
	}
	
	public void setSliderColor(int color) {
		sliderColor = color;
		backgroundPaint.setColor(color);
		invalidate();
	}
	
	public void setIcon(int drawableResId) {
		try {
			iconThumb = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
			? getContext().getDrawable(drawableResId)
			: getResources().getDrawable(drawableResId);
		} catch (Exception e) {
			iconThumb = null;
		}
		invalidate();
	}
	
	public void setIconTint(int color) {
		iconTint = color;
		invalidate();
	}
	
	public void setCompleteIconColor(int color) {
		completeIconColor = color;
		checkPaint.setColor(color);
		invalidate();
	}
	
	public void setSlideFromLeft(boolean fromLeft) {
		slideFromLeft = fromLeft;
		thumbPosition = fromLeft ? thumbMargin : maxThumbPosition;
		invalidate();
	}
	
	public void setLocked(boolean locked) {
		isLocked = locked;
	}
	
	public boolean isComplete() {
		return isCompleted;
	}
	
	public void reset() {
		cancelAnimators();
		isCompleted          = false;
		isSliding            = false;
		hideThumbImmediately = false;
		checkmarkProgress    = 0f;
		backgroundProgress   = 0f;
		thumbPosition        = slideFromLeft ? thumbMargin : maxThumbPosition;
		invalidate();
	}
	
	public void setOnSlideCompleteListener(@Nullable OnSlideCompleteListener listener) {
		slideCompleteListener = listener;
	}
	
	// ─── Layout ───────────────────────────────────────────────────────────────
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		
		float thumbSize;
		if (customThumbMarginPx != -1f) {
			thumbMargin = customThumbMarginPx;
			thumbSize   = Math.max(0, h - thumbMargin * 2);
		} else {
			thumbSize   = h * 0.8f;
			thumbMargin = (h - thumbSize) / 2f;
		}
		
		trackRadius = backgroundMode == MODE_OVAL
		? h / 2f
		: (customTrackRadiusPx != -1f ? customTrackRadiusPx : thumbMargin);
		
		thumbRadius = backgroundMode == MODE_OVAL
		? thumbSize / 2f
		: (customThumbRadiusPx != -1f ? customThumbRadiusPx : thumbMargin);
		
		thumbRect.set(0, 0, thumbSize, thumbSize);
		maxThumbPosition = w - thumbSize - thumbMargin;
		
		if (!isCompleted) {
			thumbPosition = slideFromLeft ? thumbMargin : maxThumbPosition;
		}
		
		checkPaint.setStrokeWidth(h * 0.05f);
		textPaint.setTextSize(h * 0.30f);
		
		if (bounceOnStart && !bounceExecuted && !isCompleted) {
			startBounceAnimation();
			bounceExecuted = true;
		}
	}
	
	// ─── Draw ─────────────────────────────────────────────────────────────────
	
	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
		
		backgroundPaint.setColor(sliderColor);
		
		float fullWidth    = getWidth();
		float circleWidth  = getHeight();
		float currentWidth = fullWidth - (fullWidth - circleWidth) * backgroundProgress;
		float left         = (fullWidth - currentWidth) / 2f;
		float right        = (fullWidth + currentWidth) / 2f;
		
		backgroundRect.set(left, 0, right, getHeight());
		canvas.drawRoundRect(backgroundRect, trackRadius, trackRadius, backgroundPaint);
		
		if (isCompleted || backgroundProgress > 0f) {
			drawCheckmark(canvas);
		} else if (!hideThumbImmediately) {
			drawThumb(canvas);
			drawText(canvas);
		}
	}
	
	private void drawThumb(@NonNull Canvas canvas) {
		float thumbX = clamp(thumbPosition, thumbMargin, maxThumbPosition);
		thumbRect.offsetTo(thumbX, thumbMargin);
		
		float maxSlide      = Math.max(1f, maxThumbPosition - thumbMargin);
		float slideProgress = slideFromLeft
		? (thumbX - thumbMargin) / maxSlide
		: (maxThumbPosition - thumbX) / maxSlide;
		
		// ── Rotasi thumb mirip cortinico ──
		// Mulai rotate di ROTATION_START (70%), selesai di ROTATION_END (100%)
		float rotationAngle = 0f;
		if (slideProgress >= ROTATION_START) {
			float rotProgress = (slideProgress - ROTATION_START) / (ROTATION_END - ROTATION_START);
			rotProgress    = Math.min(1f, rotProgress);
			rotationAngle  = rotProgress * (slideFromLeft ? MAX_ROTATION : -MAX_ROTATION);
		}
		
		canvas.save();
		if (rotationAngle != 0f) {
			canvas.rotate(rotationAngle, thumbRect.centerX(), thumbRect.centerY());
		}
		
		// Gambar thumb
		canvas.drawRoundRect(thumbRect, thumbRadius, thumbRadius, thumbPaint);
		
		// Gambar icon — selalu alpha 255, hilang bareng complete
		if (iconThumb != null) {
			int iconSize = (int) (thumbRect.width() * 0.6f);
			int iconLeft = (int) (thumbRect.centerX() - iconSize / 2f);
			int iconTop  = (int) (thumbRect.centerY() - iconSize / 2f);
			iconThumb.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize);
			// Set tint dulu sebelum alpha, biar ga override
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				iconThumb.setTint(iconTint);
			} else {
				iconThumb.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN);
			}
			iconThumb.setAlpha(255); // set alpha SETELAH tint
			iconThumb.draw(canvas);
		}
		
		canvas.restore();
	}
	
	private void drawArrows(@NonNull Canvas canvas, float slideProgress) {
		// Arrow fade 0→30% progress, setelah itu hilang
		float fadeEnd   = 0.3f;
		float alpha     = slideProgress >= fadeEnd ? 0f : 1f - (slideProgress / fadeEnd);
		
		arrowPaint.setColor(sliderColor);
		arrowPaint.setAlpha((int) (255 * alpha));
		if (arrowPaint.getAlpha() == 0) return;
		
		float cx          = thumbRect.centerX();
		float cy          = thumbRect.centerY();
		float arrowWidth  = thumbRect.width() * 0.15f;
		float arrowHeight = thumbRect.height() * 0.3f;
		float gap         = thumbRect.width() * 0.03f;
		int   numArrows   = useThreeArrows ? 3 : 2;
		float totalWidth  = numArrows * arrowWidth + (numArrows - 1) * gap;
		
		float currentX = slideFromLeft
		? cx - totalWidth / 2f
		: cx + totalWidth / 2f;
		
		for (int i = 0; i < numArrows; i++) {
			Path path = new Path();
			if (slideFromLeft) {
				path.moveTo(currentX,              cy - arrowHeight / 2f);
				path.lineTo(currentX + arrowWidth, cy);
				path.lineTo(currentX,              cy + arrowHeight / 2f);
				currentX += arrowWidth + gap;
			} else {
				path.moveTo(currentX,              cy - arrowHeight / 2f);
				path.lineTo(currentX - arrowWidth, cy);
				path.lineTo(currentX,              cy + arrowHeight / 2f);
				currentX -= arrowWidth + gap;
			}
			path.close();
			canvas.drawPath(path, arrowPaint);
		}
	}
	
	private void drawText(@NonNull Canvas canvas) {
		float maxSlide      = Math.max(1f, maxThumbPosition - thumbMargin);
		float thumbX        = clamp(thumbPosition, thumbMargin, maxThumbPosition);
		float slideProgress = slideFromLeft
		? (thumbX - thumbMargin) / maxSlide
		: (maxThumbPosition - thumbX) / maxSlide;
		
		float fadeEnd = 0.3f;
		float alpha   = slideProgress >= fadeEnd ? 0f : 1f - (slideProgress / fadeEnd);
		int   iAlpha  = (int) (255 * alpha);
		if (iAlpha <= 0) return;
		
		textPaint.setAlpha(iAlpha);
		float textY = getHeight() / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
		canvas.drawText(slideText, getWidth() / 2f, textY, textPaint);
	}
	
	private void drawCheckmark(@NonNull Canvas canvas) {
		float size    = getHeight() * 0.4f;
		float centerX = getWidth() / 2f;
		float centerY = getHeight() / 2f;
		
		float x1 = centerX - size / 2f;
		float y1 = centerY;
		float x2 = centerX - size / 6f;
		float y2 = centerY + size / 3f;
		float x3 = centerX + size / 2f;
		float y3 = centerY - size / 3f;
		
		checkPath.reset();
		if (checkmarkProgress < 0.5f) {
			float p = checkmarkProgress * 2f;
			checkPath.moveTo(x1, y1);
			checkPath.lineTo(x1 + (x2 - x1) * p, y1 + (y2 - y1) * p);
		} else {
			float p = (checkmarkProgress - 0.5f) * 2f;
			checkPath.moveTo(x1, y1);
			checkPath.lineTo(x2, y2);
			checkPath.lineTo(x2 + (x3 - x2) * p, y2 + (y3 - y2) * p);
		}
		canvas.drawPath(checkPath, checkPaint);
	}
	
	// ─── Touch ────────────────────────────────────────────────────────────────
	
	@Override
	public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {
		return isEnabled() && !isCompleted;
	}
	
	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		if (!isEnabled() || isCompleted) return true;
		
		float x = event.getX();
		
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			if (thumbRect.contains(x, event.getY())) {
				cancelAnimators();
				isSliding     = true;
				startX        = x;
				thumbPosition = thumbRect.left;
				invalidate();
			}
			break;
			
			case MotionEvent.ACTION_MOVE:
			if (isSliding) {
				thumbPosition += x - startX;
				thumbPosition  = clamp(thumbPosition, thumbMargin, maxThumbPosition);
				startX         = x;
				
				boolean reachedEnd = slideFromLeft
				? thumbPosition >= maxThumbPosition - 10
				: thumbPosition <= thumbMargin + 10;
				
				if (reachedEnd) completeSlide();
				else invalidate();
			}
			break;
			
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
			if (isSliding && !isCompleted) resetSlide();
			break;
		}
		
		return true;
	}
	
	// ─── Animations ───────────────────────────────────────────────────────────
	
	private void startBounceAnimation() {
		final float startPos     = slideFromLeft ? thumbMargin : maxThumbPosition;
		final float bounceOffset = getWidth() * 0.05f;
		final float targetPos    = slideFromLeft ? startPos + bounceOffset : startPos - bounceOffset;
		
		bounceAnimator = ValueAnimator.ofFloat(startPos, targetPos, startPos);
		bounceAnimator.setDuration(400);
		bounceAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
		bounceAnimator.addUpdateListener(a -> {
			thumbPosition = (float) a.getAnimatedValue();
			invalidate();
		});
		bounceAnimator.start();
	}
	
	private void completeSlide() {
		if (!isLocked) {
			isSliding = false;
			cancelAnimators();
			final float targetPos = slideFromLeft ? thumbMargin : maxThumbPosition;
			slideAnimator = ValueAnimator.ofFloat(thumbPosition, targetPos);
			slideAnimator.setDuration(400);
			slideAnimator.setInterpolator(new OvershootInterpolator(3f));
			slideAnimator.addUpdateListener(a -> {
				thumbPosition = (float) a.getAnimatedValue();
				invalidate();
			});
			slideAnimator.start();
			return;
		}
		
		isSliding = false;
		cancelAnimators();
		
		final float targetPos = slideFromLeft ? maxThumbPosition : thumbMargin;
		slideAnimator = ValueAnimator.ofFloat(thumbPosition, targetPos);
		slideAnimator.setDuration(150);
		slideAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
		slideAnimator.addUpdateListener(a -> {
			thumbPosition = (float) a.getAnimatedValue();
			invalidate();
		});
		slideAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				hideThumbImmediately = true;
				invalidate();
				startCompletionAnimations();
			}
		});
		slideAnimator.start();
	}
	
	private void startCompletionAnimations() {
		final long DURATION = 300L;
		
		backgroundProgress       = 0f;
		backgroundShrinkAnimator = ValueAnimator.ofFloat(0f, 1f);
		backgroundShrinkAnimator.setDuration(DURATION);
		backgroundShrinkAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
		backgroundShrinkAnimator.addUpdateListener(a -> {
			backgroundProgress = (float) a.getAnimatedValue();
			invalidate();
		});
		backgroundShrinkAnimator.start();
		
		checkmarkProgress = 0f;
		checkmarkAnimator = ValueAnimator.ofFloat(0f, 1f);
		checkmarkAnimator.setDuration(DURATION);
		checkmarkAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
		checkmarkAnimator.addUpdateListener(a -> {
			checkmarkProgress = (float) a.getAnimatedValue();
			invalidate();
		});
		checkmarkAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				isCompleted        = true;
				checkmarkProgress  = 1f;
				backgroundProgress = 1f;
				invalidate();
				if (slideCompleteListener != null) slideCompleteListener.onSlideComplete();
			}
		});
		checkmarkAnimator.start();
	}
	
	private void resetSlide() {
		isSliding = false;
		cancelAnimators();
		
		checkmarkProgress    = 0f;
		backgroundProgress   = 0f;
		hideThumbImmediately = false;
		
		final float targetPos = slideFromLeft ? thumbMargin : maxThumbPosition;
		slideAnimator = ValueAnimator.ofFloat(thumbPosition, targetPos);
		slideAnimator.setDuration(200);
		// Overshoot biar ada efek bump balik mirip cortinico
		slideAnimator.setInterpolator(new OvershootInterpolator(2f));
		slideAnimator.addUpdateListener(a -> {
			thumbPosition = (float) a.getAnimatedValue();
			invalidate();
		});
		slideAnimator.start();
	}
	
	// ─── Lifecycle ────────────────────────────────────────────────────────────
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		setAlpha(enabled ? 1f : 0.5f);
		if (!enabled && isSliding) resetSlide();
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		cancelAnimators();
	}
	
	private void cancelAnimators() {
		if (bounceAnimator != null && bounceAnimator.isRunning()) bounceAnimator.cancel();
		if (slideAnimator != null && slideAnimator.isRunning()) slideAnimator.cancel();
		if (checkmarkAnimator != null && checkmarkAnimator.isRunning()) checkmarkAnimator.cancel();
		if (backgroundShrinkAnimator != null && backgroundShrinkAnimator.isRunning()) backgroundShrinkAnimator.cancel();
	}
	
	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}