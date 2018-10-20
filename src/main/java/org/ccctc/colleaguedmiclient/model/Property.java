package org.ccctc.colleaguedmiclient.model;

import org.ccctc.colleaguedmiclient.exception.DmiServiceException;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Property information used by DmiEntityService
 */
public class Property {

    private final PropertyDescriptor pd;
    private final Class<?> type;
    private final Type[] genericTypeArguments;
    private final Method reader;
    private final Method writer;

    public Property(PropertyDescriptor pd) {
        this.pd = pd;
        this.type = pd.getPropertyType();
        this.reader = pd.getReadMethod();
        this.writer = pd.getWriteMethod();
        this.genericTypeArguments = getGenericTypeArgument();
    }

    public void setProperty(Object o, Object v) {
        try {
            if (writer == null)
                throw new DmiServiceException("Write method not found for " + pd.getName());

            // special processing for Booleans - convert "Y" to true, "N" to false, anything else to null
            if (type == Boolean.class && v != null && v.getClass() != Boolean.class) {
                String s = v.toString();
                if ("Y".equals(s)) v = Boolean.TRUE;
                else if ("N".equals(s)) v = Boolean.FALSE;
            }


            writer.invoke(o, v);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new DmiServiceException("Unable to set value of property " + pd.getName() + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new DmiServiceException("Type mismatch - unable to assign value of type " + v.getClass().getName() +
                    " to field " + pd.getName() + " (type " + type.getName() + ")");
        }
    }

    public String getName() {
        return pd.getName();
    }

    public Class<?> getType() {
        return type;
    }

    public Type[] getGenericTypeArguments() {
        return genericTypeArguments;
    }

    private Type[] getGenericTypeArgument() {
        if (reader == null)
            throw new DmiServiceException("Read method not found for " + pd.getName());

        Type t = reader.getGenericReturnType();

        if (t instanceof ParameterizedType)
            return ((ParameterizedType) t).getActualTypeArguments();

        return null;
    }
}
