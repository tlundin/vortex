package com.teraim.vortex.expr;

import com.teraim.vortex.dynamic.types.Numerable;
import com.teraim.vortex.dynamic.types.Numerable.Type;

public class Literal implements Numerable {

	private String name,label,value;
	
	public Literal(String name,String label) {
		value=null;
		this.name=name;
		this.label=label;
	}

	

	public void setValue(String val) {
		value = val;
	}

	@Override
	public Type getType() {
		return Type.LITERAL;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
	

}
