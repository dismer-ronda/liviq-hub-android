package hospital.linde.uk.apphubandroid.utils;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.gson.Gson;

@SuppressWarnings({"rawtypes","unchecked"})
public class JsonSerializer implements Serializable
{
	private static final long serialVersionUID = 3523992108681568731L;
	private static volatile Gson gson;
	
	public static Gson getInstance()
	{
		if ( gson == null )
			gson = new Gson();
		
		return gson;
	}
	
	public static String toJson( Object obj )
	{
		return getInstance().toJson( obj );
	}
	
	public static Object toPojo( String text, Class clazz )
	{
		return getInstance().fromJson( text, clazz );
	}
	
	public static ArrayList toArrayList( String text, Type listType )
	{
		return getInstance().fromJson( text, listType );
	}
}
