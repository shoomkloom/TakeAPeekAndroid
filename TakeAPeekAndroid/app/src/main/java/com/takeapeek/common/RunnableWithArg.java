package com.takeapeek.common;

/**
 * Created by orenslev on 07/06/2016.
 */
public abstract class RunnableWithArg implements Runnable
{
    Object[] m_args;

    public RunnableWithArg(Object... args)
    {
        m_args = args;
    }

    public int getArgCount()
    {
        return m_args == null ? 0 : m_args.length;
    }

    public Object[] getArgs()
    {
        return m_args;
    }
}
