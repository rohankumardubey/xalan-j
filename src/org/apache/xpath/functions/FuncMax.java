/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 1999-2003 The Apache Software Foundation.  All rights 
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:  
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Xalan" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written 
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation and was
 * originally based on software copyright (c) 1999, Lotus
 * Development Corporation., http://www.lotus.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.apache.xpath.functions;

import org.apache.xpath.res.XPATHErrorResources;

//import org.w3c.dom.Node;
//import org.w3c.dom.traversal.NodeIterator;
import org.apache.xml.dtm.DTM;
import org.apache.xml.dtm.DTMIterator;
import org.apache.xml.dtm.DTMSequence;
import org.apache.xml.dtm.XType;

import java.util.Vector;

import org.apache.xpath.XPathContext;
import org.apache.xpath.XPath;
import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XNumber;
import org.apache.xpath.objects.XSequence;
import org.apache.xpath.objects.XSequenceImpl;
import org.apache.xpath.objects.XString;
import org.apache.xpath.objects.XNodeSequenceSingleton;
import org.apache.xpath.objects.XObjectFactory;
import org.apache.xpath.objects.XDouble;
import org.apache.xml.dtm.XType;
import org.apache.xalan.res.XSLMessages;

import java.text.Collator;
import java.net.URL;
import java.util.Comparator;

/**
 * <meta name="usage" content="advanced"/>
 * Execute the Count() function.
 */
public class FuncMax extends Function2Args
{

  /**
   * Execute the function.  The function must return
   * a valid object.
   * @param xctxt The current execution context.
   * @return A valid XObject.
   *
   * @throws javax.xml.transform.TransformerException
   */
  public XObject execute(XPathContext xctxt) throws javax.xml.transform.TransformerException
  {
    XSequence nl = m_arg0.execute(xctxt).xseq();
	if (nl.equals(XSequence.EMPTY))
	 return XSequence.EMPTY;
	 
	if (XType.NOTHOMOGENOUS == nl.getTypes())
  	 this.error(xctxt, XPATHErrorResources.ER_ERROR_OCCURED, null);
  	
	 	 
  	Comparator comparator = null;
  	if (m_arg1 != null)
  	{
  	  String collation = m_arg1.execute(xctxt).str();
  	  try{
  	    URL uri = new URL(collation);
  	    comparator = Collator.getInstance();
  	  }
  	  catch(java.net.MalformedURLException mue)
  	  {
  	    comparator = null;
  	  }
  	}
		
  	XObject max = null;
	XObject item;
	while ((item = nl.next()) != null)
	{
	  int type = item.getType();
	  
	  if(type == XType.NODE)
  	  {
  	    if(item instanceof XNodeSequenceSingleton)
        {
          XNodeSequenceSingleton xnss = (XNodeSequenceSingleton)item;
          if (max == null || xnss.greaterThan(max))
          {
            max = xnss;
          }
  	    }
  	  }
  	  else if (type == XObject.CLASS_STRING)
  	  {  
  	    if (comparator == null)
  	    {
  	      if (max == null || ((XString)item).compareTo(max.xstr()) > 0)
  	      {
  	        max = item;
  	      }  	      
  	    } 
  	    else
  	    {
  	      if (max == null || comparator.compare(item.str(), max.str()) > 0)
  	      {
  	        max = item;
  	      }    	      
  	    }  	        	    
  	  }
  	  else
  	  {
  	    if(max == null || item.greaterThan(max))  	      	    
  	    {
  	      max = item;
  	    }
  	  }
	}

    return max;
  }
  
  /**
   * Check that the number of arguments passed to this function is correct. 
   *
   *
   * @param argNum The number of arguments that is being passed to the function.
   *
   * @throws WrongNumberArgsException
   */
  public void checkNumberArgs(int argNum) throws WrongNumberArgsException
  {
    if (argNum < 1 || argNum > 2)
      reportWrongNumberArgs();
  }

  /**
   * Constructs and throws a WrongNumberArgException with the appropriate
   * message for this function object.
   *
   * @throws WrongNumberArgsException
   */
  protected void reportWrongNumberArgs() throws WrongNumberArgsException {
      throw new WrongNumberArgsException(XSLMessages.createXPATHMessage("oneortwo", null));
  }
  
  /** Return the number of children the node has. */
  public int exprGetNumChildren()
  {
  	return (m_arg1 == null) ?  1 :  2;
  }
}