<?php
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

/**
 * Template for cities.
 *  
 * @author Jens Lehmann
 */
class CityTemplate extends PopulatedPlaceTemplate {

	function getTemplate($triples) {
		$content = "";
		$content .= '<table>';
		$content .= '<tr><td colspan="2">City Information</td></tr>';
		$content .= '<tr><td>total population</td><td>' + getPopulationString($triples) + '</td></tr>';
		$content .= '</table>';
		
		// .. continue ...
		
		return $content;
	}
	
}
?>