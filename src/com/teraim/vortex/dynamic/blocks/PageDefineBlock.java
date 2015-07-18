package com.teraim.vortex.dynamic.blocks;

/**
 * Page Definition block
 * @author Terje
 *
 */
public  class PageDefineBlock extends Block {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5680503647867451266L;
	private String pageName="",pageType=null,pageLabel="";
	private boolean hasGPS;
	private boolean goBackAllowed=true;
	private boolean hasSatNav=true;

	public String getPageName() {
		return pageName;
	}
	public String getPageType() {
		return pageType;
	}
	public String getPageLabel() {
		return pageLabel;
	}
	public boolean hasGPS() {
		return hasGPS;
	}
	
	public boolean hasSatNav() {
		return hasSatNav;
	}
	
	public PageDefineBlock(String id,String pageName,String pageType,String pageLabel,boolean hasGPS, boolean hasSatNav, boolean goBackAllowed) {
		this.pageName =pageName;
		this.pageType = pageType;
		this.pageLabel=pageLabel;
		this.blockId=id;
		this.hasGPS=hasGPS;
		this.goBackAllowed = goBackAllowed;
		this.hasSatNav = hasSatNav;
	}
	public boolean goBackAllowed() {
		return goBackAllowed;
	}
}
