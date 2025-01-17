//   RectangleTest.java
//   Java Spatial Index Library
//   Copyright (C) 2002-2005 Infomatiq Limited.
//  
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//  
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//  
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 USA

package net.sf.jsi;

import junit.framework.TestCase;

import net.sf.jsi.Area;


/**
 * RectangleTest
 */
public class RectangleTest extends TestCase
{
	public RectangleTest(String s)
	{
		super(s);
	}


	public void testContains()
	{

	}


	public void testDistance()
	{

	}


	public void testIntersects()
	{
		Area r0_0_0_0 = new Area(0, 0, 0, 0);
		Area r1_1_1_1 = new Area(1, 1, 1, 1);
		Area r2_2_6_6 = new Area(2, 2, 6, 6);
		Area r3_3_7_5 = new Area(3, 3, 7, 5);
		Area r3_3_5_7 = new Area(3, 3, 5, 7);
		Area r1_3_5_5 = new Area(1, 3, 5, 5);
		Area r3_1_5_5 = new Area(3, 1, 5, 5);

		// A rectangle always intersects itself
		assertTrue(r0_0_0_0.intersects(r0_0_0_0));
		assertTrue(r2_2_6_6.intersects(r2_2_6_6));

		assertTrue(r0_0_0_0.intersects(r1_1_1_1) == false);
		assertTrue(r1_1_1_1.intersects(r0_0_0_0) == false);

		// Rectangles that intersect only on the right-hand side
		assertTrue(r2_2_6_6.intersects(r3_3_7_5));
		assertTrue(r3_3_7_5.intersects(r2_2_6_6));

		// Rectangles that touch only on the right hand side
		// assertTrue(r

		// Rectangles that intersect only on the top side
		assertTrue(r2_2_6_6.intersects(r3_3_5_7));
		assertTrue(r3_3_5_7.intersects(r2_2_6_6));

		// Rectangles that intersect only on the left-hand side
		assertTrue(r2_2_6_6.intersects(r1_3_5_5));
		assertTrue(r1_3_5_5.intersects(r2_2_6_6));

		// Rectangles that intersect only on the bottom side
		assertTrue(r2_2_6_6.intersects(r3_1_5_5));
		assertTrue(r3_1_5_5.intersects(r2_2_6_6));

	}
}
