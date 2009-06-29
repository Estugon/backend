package sc.helpers;

import java.util.Iterator;

public abstract class StringHelper
{
	public static <T> String join(final Iterable<T> objs, final String delimiter)
	{
		Iterator<T> iter = objs.iterator();

		if (!iter.hasNext())
			return "";

		StringBuffer buffer = new StringBuffer(String.valueOf(iter.next()));

		while (iter.hasNext())
			buffer.append(delimiter).append(String.valueOf(iter.next()));

		return buffer.toString();
	}
}
