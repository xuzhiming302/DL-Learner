package org.dllearner.algorithm.tbsl.sparql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Slot {

	String anchor;
	String token;
	SlotType type;
	List<String> words;
	
	public Slot(String a,List<String> ws) {
		anchor = a;
		token = "";
		type = SlotType.UNSPEC; 
		words = ws;
		replaceUnderscores();
	}
	public Slot(String a,SlotType t,List<String> ws) {
		anchor = a;
		token = "";
		type = t;
		words = ws;
		replaceUnderscores();
	}
	
	public void setSlotType(SlotType st) {
		type = st;
	}
	
	public SlotType getSlotType(){
		return type;
	}
	
	public String getAnchor() {
		return anchor;
	}
	public void setAnchor(String s) {
		anchor = s;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String t) {
		token = t;
	}
	
	public List<String> getWords() {
		return words;
	}
	public void setWords(List<String> ws) {
		words = ws;
	}
	
	public void replaceReferent(String ref1,String ref2) {
		if (anchor.equals(ref1)) {
			anchor = ref2;
		}
	}
	
	public void replaceUnderscores() {
		ArrayList<String> newWords = new ArrayList<String>();
		for (String w : words) {
			newWords.add(w.replaceAll("_"," "));
		}
		words = newWords;
	}
	
	public String toString() {
		
		String out = anchor + ": " + type + " {";
		
		for (Iterator<String> i = words.iterator(); i.hasNext();) {
			out += i.next();
			if (i.hasNext()) {
				out += ",";
			}
		}
		
		out += "}";
		
		return out;
	}
	
	public Slot clone() {
		
		List<String> newWords = new ArrayList<String>();
		for (String word : words) {
			newWords.add(word);
		}
		
		return new Slot(anchor,type,newWords);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((anchor == null) ? 0 : anchor.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((words == null) ? 0 : words.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Slot other = (Slot) obj;
		if (anchor == null) {
			if (other.anchor != null)
				return false;
		} else if (!anchor.equals(other.anchor))
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (words == null) {
			if (other.words != null)
				return false;
		} else if (!words.equals(other.words))
			return false;
		return true;
	}
	
	
}
