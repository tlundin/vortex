package com.teraim.fieldapp.dynamic.blocks;

import java.util.List;

import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

/**
 * Page Definition block
 * @author Terje
 *
 */
public  class PageDefineBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5680503647867451267L;
	private String pageName="",pageType=null;
	private boolean hasGPS;
	private boolean goBackAllowed=true;
	private List<EvalExpr>pageLabelE;


	public String getPageName() {
		return pageName;
	}
	public String getPageType() {
		return pageType;
	}
	public String getPageLabel() {
		return Expressor.analyze(pageLabelE);
	}
	public boolean hasGPS() {
		return hasGPS;
	}
	
	
	public PageDefineBlock(String id,String pageName,String pageType,String pageLabel,boolean hasGPS, boolean goBackAllowed) {
		this.pageName =pageName;
		this.pageType = pageType;
		this.pageLabelE=Expressor.preCompileExpression(pageLabel);
		this.blockId=id;
		this.hasGPS=hasGPS;
		this.goBackAllowed = goBackAllowed;

	}
	public boolean goBackAllowed() {
		return goBackAllowed;
	}
}
