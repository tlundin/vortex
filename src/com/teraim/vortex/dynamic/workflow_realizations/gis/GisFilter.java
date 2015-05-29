package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;

import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;

import android.graphics.Bitmap;
import android.graphics.Paint.Style;

public interface GisFilter {

	String getExpression();

	String getLabel();

	Bitmap getBitmap();

	float getRadius();

	String getColor();

	Style getStyle();

	boolean isCircle();

	boolean isActive();

	boolean hasCachedFilterResult();

	void setTokens(List<TokenizedItem> myTokens);

	List<TokenizedItem> getTokens();

}
