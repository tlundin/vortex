package com.teraim.vortex.dynamic.workflow_realizations.gis;

import java.util.List;

import com.teraim.vortex.utils.Expressor.EvalExpr;

public interface GisObjectBaseAttributes {

	public boolean isVisible();
	public List<EvalExpr> getLabelExpression();

}
