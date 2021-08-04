/* Copyright (c) 2021 interface projects GmbH
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.sf.jsi;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.function.DoublePredicate;

/**
 * 
 * @author <a href="mailto:gunnar.brand@interface-projects.de">Gunnar Brand</a>
 * @since 04.08.2021
 */
final class FloatArray implements Externalizable
{
	private final static float[] EMPTY = {};
	
	private float[] _data = EMPTY;
	private int _size = 0;

	
	public FloatArray()
	{
		_data = EMPTY;
	}

	
	public FloatArray(int size)
	{
		_data = size==0 ? EMPTY : new float[size];
	}
	
	
	public boolean isEmpty()
	{
		return _size==0;
	}

	
	public int size()
	{
		return _size;
	}
	
	
	public void reset()
	{
		_size = 0;
	}

	
	public void clear()
	{
		if ( _data.length>16 ) _data = EMPTY;
		_size = 0;
	}
	
	
	public boolean add(float v)
	{
		push(v);
		return true;
	}
	

	public void push(float v)
	{
		if ( _size >= _data.length ) {
			_data = Arrays.copyOf(_data, _size<16 ? 16 : _size * 2);
		}
		_data[_size++] = v;
	}

	
	public float peek() throws ArrayIndexOutOfBoundsException
	{
		return _data[_size - 1];
	}
	
	
	public float pop() throws ArrayIndexOutOfBoundsException
	{
		if ( _size==0 ) throw new ArrayIndexOutOfBoundsException(-1);
		return _data[--_size];
	}
	
	
	public float get(int index) throws ArrayIndexOutOfBoundsException
	{
		if ( index>=_size ) throw new ArrayIndexOutOfBoundsException(index);
		return _data[index];
	}

	
	public float set(int index, float value) throws ArrayIndexOutOfBoundsException
	{
		if ( index>=_size ) throw new ArrayIndexOutOfBoundsException(index);
		float old = _data[index];
		_data[index] = value;
		return old;
	}

	
	public boolean forEach(DoublePredicate cb)
	{
		for ( int i = 0, e = _size; i<e; i++ ) {
			if ( !cb.test(_data[i]) ) return false;
		}
		return true;
	}
	
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeInt(_size);
		for ( int i = 0, e = _size; i<e; i++ ) out.writeFloat(_data[i]);
	}
	
	
	@Override
	public void readExternal(ObjectInput in) throws IOException
	{
		_size = in.readInt();
		_data = _size==0 ? EMPTY : new float[_size];
		for ( int i = 0, e = _size; i<e; i++ ) _data[i] = in.readFloat(); 
	}

}
