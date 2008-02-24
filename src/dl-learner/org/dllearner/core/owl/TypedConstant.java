/**
 * Copyright (C) 2007-2008, Jens Lehmann
 *
 * This file is part of DL-Learner.
 * 
 * DL-Learner is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * DL-Learner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.dllearner.core.owl;

import java.util.Map;

/**
 * A constant which has an explicitly assigned datatype.
 * 
 * @author Jens Lehmann
 *
 */
public class TypedConstant extends Constant {

	private Datatype datatype;
	
	public TypedConstant(String literal, Datatype datatype) {
		super(literal);
		this.datatype = datatype;
	}
	
	/* (non-Javadoc)
	 * @see org.dllearner.core.owl.KBElement#getLength()
	 */
	public int getLength() {
		return 1;
	}

	/* (non-Javadoc)
	 * @see org.dllearner.core.owl.KBElement#toString(java.lang.String, java.util.Map)
	 */
	public String toString(String baseURI, Map<String, String> prefixes) {
		return literal + "^^" + datatype;
	}

	/**
	 * @return the datatype
	 */
	public Datatype getDatatype() {
		return datatype;
	}
	
	/* (non-Javadoc)
	 * @see org.dllearner.core.owl.KBElement#accept(org.dllearner.core.owl.KBElementVisitor)
	 */
	public void accept(KBElementVisitor visitor) {
		visitor.visit(this);
	}	

}
