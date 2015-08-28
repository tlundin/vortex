package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Paint.Style;

import com.teraim.vortex.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.vortex.utils.RuleExecutor.TokenizedItem;

public interface GisFilter {

	String getExpression();

	String getLabel();

	Bitmap getBitmap();

	float getRadius();

	String getColor();

	Style getStyle();

	PolyType getShape();
	
	boolean isActive();

	boolean hasCachedFilterResult();

	void setTokens(List<TokenizedItem> myTokens);

	List<TokenizedItem> getTokens();

}
