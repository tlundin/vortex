package com.teraim.vortex.expr;

import com.teraim.vortex.dynamic.types.Numerable;
import com.teraim.vortex.dynamic.types.Numerable.Type;

public class Numeric extends Aritmetic {

	public Numeric(String name, String label) {
		super(name,label);
	}

	@Override
	public Type getType() {
		return Type.NUMERIC;
	}
	
}
