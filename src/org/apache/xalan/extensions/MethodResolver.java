package org.apache.xalan.extensions;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.traversal.NodeIterator;

import org.apache.xpath.objects.XObject;
import org.apache.xpath.objects.XString;

import org.w3c.xslt.ExpressionContext;
import org.xml.sax.SAXException;

/**
 * Utility class to help resolve method overloading with Xalan XSLT 
 * argument types.
 */
public class MethodResolver
{

  /**
   * Specifies a search for static methods only.
   */
  public static final int STATIC_ONLY         = 1;

  /**
   * Specifies a search for instance methods only.
   */
  public static final int INSTANCE_ONLY       = 2;

  /**
   * Specifies a search for both static and instance methods.
   */
  public static final int STATIC_AND_INSTANCE = 3;

  /**
   * Specifies a Dynamic method search.  If the method being
   * evaluated is a static method, all arguments are used.
   * Otherwise, it is an instance method and only arguments
   * beginning with the second argument are used.
   */
  public static final int DYNAMIC             = 4;

  /**
   * Given a class, figure out the resolution of 
   * the Java Constructor from the XSLT argument types, and perform the 
   * conversion of the arguments.
   * @param classObj the Class of the object to be constructed.
   * @param argsIn An array of XSLT/XPath arguments.
   * @param argsOut An array of the exact size as argsIn, which will be 
   * populated with converted arguments if a suitable method is found.
   * @return A constructor that will work with the argsOut array.
   * @exception SAXException may be thrown for Xalan conversion
   * exceptions.
   */
  public static Constructor getConstructor(Class classObj, 
                                           Object[] argsIn, 
                                           Object[][] argsOut,
                                           ExpressionContext exprContext)
    throws NoSuchMethodException,
           SecurityException,
           SAXException
  {
    Constructor bestConstructor = null;
    Class[] bestParamTypes = null;
    Constructor[] constructors = classObj.getConstructors();
    int nMethods = constructors.length;
    int bestScore = Integer.MAX_VALUE;
    int bestScoreCount = 0;
    for(int i = 0; i < nMethods; i++)
    {
      Constructor ctor = constructors[i];
      Class[] paramTypes = ctor.getParameterTypes();
      int numberMethodParams = paramTypes.length;
      int paramStart = 0;
      boolean isFirstExpressionContext = false;
      int scoreStart;
      // System.out.println("numberMethodParams: "+numberMethodParams);
      // System.out.println("argsIn.length: "+argsIn.length);
      // System.out.println("exprContext: "+exprContext);
      if(numberMethodParams == (argsIn.length+1))
      {
        Class javaClass = paramTypes[0];
        // System.out.println("first javaClass: "+javaClass.getName());
        if(org.w3c.xslt.ExpressionContext.class.isAssignableFrom(javaClass))
        {
          isFirstExpressionContext = true;
          scoreStart = 0;
          paramStart++;
          // System.out.println("Incrementing paramStart: "+paramStart);
        }
        else
          continue;
      }
      else
          scoreStart = 100;
      
      if(argsIn.length == (numberMethodParams - paramStart))
      {
        // then we have our candidate.
        int score = scoreMatch(paramTypes, paramStart, argsIn, scoreStart);
        // System.out.println("score: "+score);
        if(-1 == score)	
          continue;
        if(score < bestScore)
        {
          // System.out.println("Assigning best ctor: "+ctor);
          bestConstructor = ctor;
          bestParamTypes = paramTypes;
          bestScore = score;
          bestScoreCount = 1;
        }
        else if (score == bestScore)
          bestScoreCount++;
      }
    }

    if(null == bestConstructor)
      throw new NoSuchMethodException(classObj.getName()); // Should give more info...
    /*** This is commented out until we can do a better object -> object scoring 
    else if (bestScoreCount > 1)
      throw new SAXException("More than one best match for constructor for "
                                                                   + classObj.getName());
    ***/
    else
      convertParams(argsIn, argsOut, bestParamTypes, exprContext);
    
    return bestConstructor;
  }

  
  /**
   * Given the name of a method, figure out the resolution of 
   * the Java Method from the XSLT argument types, and perform the 
   * conversion of the arguments.
   * @param classObj The Class of the object that should have the method.
   * @param name The name of the method to be invoked.
   * @param argsIn An array of XSLT/XPath arguments.
   * @param argsOut An array of the exact size as argsIn, which will be 
   * populated with converted arguments if a suitable method is found.
   * @return A method that will work with the argsOut array.
   * @exception SAXException may be thrown for Xalan conversion
   * exceptions.
   */
  public static Method getMethod(Class classObj,
                                 String name, 
                                 Object[] argsIn, 
                                 Object[][] argsOut,
                                 ExpressionContext exprContext,
                                 int searchMethod)
    throws NoSuchMethodException,
           SecurityException,
           SAXException
  {
    // System.out.println("---> Looking for method: "+name);
    // System.out.println("---> classObj: "+classObj);
    Method bestMethod = null;
    Class[] bestParamTypes = null;
    Method[] methods = classObj.getMethods();
    int nMethods = methods.length;
    int bestScore = Integer.MAX_VALUE;
    int bestScoreCount = 0;
    boolean isStatic;
    for(int i = 0; i < nMethods; i++)
    {
      Method method = methods[i];
      // System.out.println("looking at method: "+method);
      int xsltParamStart = 0;
      if(method.getName().equals(name))
      {
        isStatic = Modifier.isStatic(method.getModifiers());
        switch(searchMethod)
        {
          case STATIC_ONLY:
            if (!isStatic)
            {
              continue;
            }
            break;

          case INSTANCE_ONLY:
            if (isStatic)
            {
              continue;
            }
            break;

          case STATIC_AND_INSTANCE:
            break;

          case DYNAMIC:
            if (!isStatic)
              xsltParamStart = 1;
        }
        int javaParamStart = 0;
        Class[] paramTypes = method.getParameterTypes();
        int numberMethodParams = paramTypes.length;
        boolean isFirstExpressionContext = false;
        int scoreStart;
        // System.out.println("numberMethodParams: "+numberMethodParams);
        // System.out.println("argsIn.length: "+argsIn.length);
        // System.out.println("exprContext: "+exprContext);
        int argsLen = (null != argsIn) ? argsIn.length : 0;
        if(numberMethodParams == (argsLen-xsltParamStart+1))
        {
          Class javaClass = paramTypes[0];
          if(org.w3c.xslt.ExpressionContext.class.isAssignableFrom(javaClass))
          {
            isFirstExpressionContext = true;
            scoreStart = 0;
            javaParamStart++;
          }
          else
          {
            continue;
          }
        }
        else
            scoreStart = 100;
        
        if((argsLen - xsltParamStart) == (numberMethodParams - javaParamStart))
        {
          // then we have our candidate.
          int score = scoreMatch(paramTypes, javaParamStart, argsIn, scoreStart);
          // System.out.println("score: "+score);
          if(-1 == score)
            continue;
          if(score < bestScore)
          {
            // System.out.println("Assigning best method: "+method);
            bestMethod = method;
            bestParamTypes = paramTypes;
            bestScore = score;
            bestScoreCount = 1;
          }
          else if (score == bestScore)
            bestScoreCount++;
        }
      }
    }
    
    if (null == bestMethod)
      throw new NoSuchMethodException(name); // Should give more info...
    /*** This is commented out until we can do a better object -> object scoring 
    else if (bestScoreCount > 1)
      throw new SAXException("More than one best match for method " + name);
    ***/
    else
      convertParams(argsIn, argsOut, bestParamTypes, exprContext);
    
    return bestMethod;
  }

  
  /**
   * Given the name of a method, figure out the resolution of 
   * the Java Method
   * @param classObj The Class of the object that should have the method.
   * @param name The name of the method to be invoked.
   * @return A method that will work to be called as an element.
   * @exception SAXException may be thrown for Xalan conversion
   * exceptions.
   */
  public static Method getElementMethod(Class classObj,
                                        String name)
    throws NoSuchMethodException,
           SecurityException,
           SAXException
  {
    // System.out.println("---> Looking for element method: "+name);
    // System.out.println("---> classObj: "+classObj);
    Method bestMethod = null;
    Method[] methods = classObj.getMethods();
    int nMethods = methods.length;
    int bestScore = Integer.MAX_VALUE;
    int bestScoreCount = 0;
    for(int i = 0; i < nMethods; i++)
    {
      Method method = methods[i];
      // System.out.println("looking at method: "+method);
      if(method.getName().equals(name))
      {
        Class[] paramTypes = method.getParameterTypes();
        if ( (paramTypes.length == 2)
           && paramTypes[1].isAssignableFrom(org.w3c.dom.Element.class) )
        {
          int score = -1;
          if (paramTypes[0].isAssignableFrom(
                                      org.apache.xalan.extensions.XSLProcessorContext.class))
          {
            score = 10;
          }
          /*******
          else if (paramTypes[0].isAssignableFrom(
                                      org.apace.xalan.xslt.XSLProcessorContext.class))
          {
            score = 5;
          }
          ********/
          else 
            continue;

          if (score < bestScore)
          {
            // System.out.println("Assigning best method: "+method);
            bestMethod = method;
            bestScore = score;
            bestScoreCount = 1;
          }
          else if (score == bestScore)
            bestScoreCount++;
        }
      }
    }
    
    if (null == bestMethod)
      throw new NoSuchMethodException(name); // Should give more info...
    else if (bestScoreCount > 1)
      throw new SAXException("More than one best match for element method " + name);
    
    return bestMethod;
  }
  

  /**
   * Convert a set of parameters based on a set of paramTypes.
   * @param argsIn An array of XSLT/XPath arguments.
   * @param argsOut An array of the exact size as argsIn, which will be 
   * populated with converted arguments.
   * @param paramTypes An array of class objects, of the exact same 
   * size as argsIn and argsOut.
   * @exception SAXException may be thrown for Xalan conversion
   * exceptions.
   */
  public static void convertParams(Object[] argsIn, 
                                   Object[][] argsOut, Class[] paramTypes,
                                   ExpressionContext exprContext)
    throws org.xml.sax.SAXException
  {
    // System.out.println("In convertParams");
    if (paramTypes == null)
      argsOut[0] = null;
    else
    {
      int nParams = paramTypes.length;
      argsOut[0] = new Object[nParams];
      int paramIndex = 0;
      if((nParams > 0) 
         && org.w3c.xslt.ExpressionContext.class.isAssignableFrom(paramTypes[0]))
      {
        argsOut[0][0] = exprContext;
        // System.out.println("Incrementing paramIndex in convertParams: "+paramIndex);
        paramIndex++;
      }

      if (argsIn != null)
      {
        for(int i = argsIn.length - nParams + paramIndex ; paramIndex < nParams; i++, paramIndex++)
        {
          // System.out.println("paramTypes[i]: "+paramTypes[i]);
          argsOut[0][paramIndex] = convert(argsIn[i], paramTypes[paramIndex]);
        }
      }
    }
  }
  
  /**
   * Simple class to hold information about allowed conversions 
   * and their relative scores, for use by the table below.
   */
  static class ConversionInfo
  {
    ConversionInfo(Class cl, int score)
    {
      m_class = cl;
      m_score = score;
    }
    
    Class m_class;  // Java class to convert to.
    int m_score; // Match score, closer to zero is more matched.
  }
  
  private static final int SCOREBASE=1;
  
  /**
   * Specification of conversions from XSLT type CLASS_UNKNOWN
   * (i.e. some unknown Java object) to allowed Java types.
   */
  static ConversionInfo[] m_javaObjConversions = {
    new ConversionInfo(Double.TYPE, 0),
    new ConversionInfo(Float.TYPE, 1),
    new ConversionInfo(Long.TYPE, 2),
    new ConversionInfo(Integer.TYPE, 3),
    new ConversionInfo(Short.TYPE, 4),
    new ConversionInfo(Character.TYPE, 5),
    new ConversionInfo(Byte.TYPE, 6),
    new ConversionInfo(java.lang.String.class, 7),
    new ConversionInfo(java.lang.Object.class, 8)
  };
  
  /**
   * Specification of conversions from XSLT type CLASS_BOOLEAN
   * to allowed Java types.
   */
  static ConversionInfo[] m_booleanConversions = {
    new ConversionInfo(Boolean.TYPE, 0),
    new ConversionInfo(java.lang.Boolean.class, 1),
    new ConversionInfo(java.lang.Object.class, 1),
    new ConversionInfo(java.lang.String.class, 2)
  };

  /**
   * Specification of conversions from XSLT type CLASS_NUMBER
   * to allowed Java types.
   */
  static ConversionInfo[] m_numberConversions = {
    new ConversionInfo(Double.TYPE, 0),
    new ConversionInfo(java.lang.Double.class, 1),
    new ConversionInfo(Float.TYPE, 3),
    new ConversionInfo(Long.TYPE, 4),
    new ConversionInfo(Integer.TYPE, 5),
    new ConversionInfo(Short.TYPE, 6),
    new ConversionInfo(Character.TYPE, 7),
    new ConversionInfo(Byte.TYPE, 8),
    new ConversionInfo(Boolean.TYPE, 9),
    new ConversionInfo(java.lang.String.class, 10),
    new ConversionInfo(java.lang.Object.class, 11)
  };

  /**
   * Specification of conversions from XSLT type CLASS_STRING
   * to allowed Java types.
   */
  static ConversionInfo[] m_stringConversions = {
    new ConversionInfo(java.lang.String.class, 0),
    new ConversionInfo(java.lang.Object.class, 1),
    new ConversionInfo(Character.TYPE, 2),
    new ConversionInfo(Double.TYPE, 3),
    new ConversionInfo(Float.TYPE, 3),
    new ConversionInfo(Long.TYPE, 3),
    new ConversionInfo(Integer.TYPE, 3),
    new ConversionInfo(Short.TYPE, 3),
    new ConversionInfo(Byte.TYPE, 3),
    new ConversionInfo(Boolean.TYPE, 4)
  };

  /**
   * Specification of conversions from XSLT type CLASS_RTREEFRAG
   * to allowed Java types.
   */
  static ConversionInfo[] m_rtfConversions = {
    new ConversionInfo(org.w3c.dom.traversal.NodeIterator.class, 0),
    new ConversionInfo(org.w3c.dom.DocumentFragment.class, 1),
    new ConversionInfo(org.w3c.dom.Node.class, 2),
    new ConversionInfo(java.lang.String.class, 2+1),
    new ConversionInfo(Boolean.TYPE, 3+1),
    new ConversionInfo(java.lang.Object.class, 4+1),
    new ConversionInfo(Character.TYPE, 5+1),
    new ConversionInfo(Double.TYPE, 6+1),
    new ConversionInfo(Float.TYPE, 6+1),
    new ConversionInfo(Long.TYPE, 6+1),
    new ConversionInfo(Integer.TYPE, 6+1),
    new ConversionInfo(Short.TYPE, 6+1),
    new ConversionInfo(Byte.TYPE, 6+1),
    new ConversionInfo(Boolean.TYPE, 7+1)
  };
  
  /**
   * Specification of conversions from XSLT type CLASS_NODESET
   * to allowed Java types.  (This is the same as for CLASS_RTREEFRAG)
   */
  static ConversionInfo[] m_nodesetConversions = {
    new ConversionInfo(org.w3c.dom.traversal.NodeIterator.class, 0),
    new ConversionInfo(org.w3c.dom.NodeList.class, 1),
    new ConversionInfo(org.w3c.dom.Node.class, 2),
    new ConversionInfo(java.lang.String.class, 3),
    new ConversionInfo(Boolean.TYPE, 4),
    new ConversionInfo(java.lang.Object.class, 5),
    new ConversionInfo(Character.TYPE, 6),
    new ConversionInfo(Double.TYPE, 7),
    new ConversionInfo(Float.TYPE, 7),
    new ConversionInfo(Long.TYPE, 7),
    new ConversionInfo(Integer.TYPE, 7),
    new ConversionInfo(Short.TYPE, 7),
    new ConversionInfo(Byte.TYPE, 7),
    new ConversionInfo(Boolean.TYPE, 8)
  };
  
  /**
   * Order is significant in the list below, based on 
   * XObject.CLASS_XXX values.
   */
  static ConversionInfo[][] m_conversions = 
  {
    m_javaObjConversions, // CLASS_UNKNOWN = 0;
    m_booleanConversions, // CLASS_BOOLEAN = 1;
    m_numberConversions,  // CLASS_NUMBER = 2;
    m_stringConversions,  // CLASS_STRING = 3;
    m_nodesetConversions, // CLASS_NODESET = 4;
    m_rtfConversions      // CLASS_RTREEFRAG = 5;
  };
  
  /**
   * Score the conversion of a set of XSLT arguments to a 
   * given set of Java parameters.
   * If any invocations of this function for a method with 
   * the same name return the same positive value, then a conflict 
   * has occured, and an error should be signaled.
   * @param javaParamTypes Must be filled with valid class names, and 
   * of the same length as xsltArgs.
   * @param xsltArgs Must be filled with valid object instances, and 
   * of the same length as javeParamTypes.
   * @return -1 for no allowed conversion, or a positive score 
   * that is closer to zero for more preferred, or further from 
   * zero for less preferred.
   */
  public static int scoreMatch(Class[] javaParamTypes, int javaParamsStart,
                               Object[] xsltArgs, int score)
  {
    if ((xsltArgs == null) || (javaParamTypes == null))
      return score;
    int nParams = xsltArgs.length;
    for(int i = nParams - javaParamTypes.length + javaParamsStart, javaParamTypesIndex = javaParamsStart; 
        i < nParams; 
        i++, javaParamTypesIndex++)
    {
      Object xsltObj = xsltArgs[i];
      int xsltClassType = (xsltObj instanceof XObject) 
                          ? ((XObject)xsltObj).getType() 
                            : XObject.CLASS_UNKNOWN;
      Class javaClass = javaParamTypes[javaParamTypesIndex];
      
      // System.out.println("Checking xslt: "+xsltObj.getClass().getName()+
      //                   " against java: "+javaClass.getName());
      
      if(xsltClassType == XObject.CLASS_NULL)
      {
        // In Xalan I have objects of CLASS_NULL, though I'm not 
        // sure they're used any more.  For now, do something funky.
        if(!javaClass.isPrimitive())
        {
          // Then assume that a null can be used, but give it a low score.
          score += 10;
          continue;
        }
        else
          return -1;  // no match.
      }
      
      ConversionInfo[] convInfo = m_conversions[xsltClassType];
      int nConversions = convInfo.length;
      int k;
      for(k = 0; k < nConversions; k++)
      {
        ConversionInfo cinfo = convInfo[k];
        if(cinfo.m_class.isAssignableFrom(javaClass))
        {
          score += cinfo.m_score;
          break; // from k loop
        }
      }
      if(k == nConversions)
        return -1; // no match
    }
    return score;
  }
  
  /**
   * Convert the given XSLT object to an object of 
   * the given class.
   * @param xsltObj The XSLT object that needs conversion.
   * @param javaClass The type of object to convert to.
   * @returns An object suitable for passing to the Method.invoke 
   * function in the args array, which may be null in some cases.
   * @exception SAXException may be thrown for Xalan conversion
   * exceptions.
   */
  static Object convert(Object xsltObj, Class javaClass)
    throws org.xml.sax.SAXException
  {
    if(xsltObj instanceof XObject)
    {
      XObject xobj = ((XObject)xsltObj);
      int xsltClassType = xobj.getType();

      switch(xsltClassType)
      {
      case XObject.CLASS_NULL:
        return null;
        
      case XObject.CLASS_BOOLEAN:
        {
          if(javaClass == java.lang.String.class)
            return xobj.str();
          else
            return new Boolean(xobj.bool());
        }
        // break; Unreachable
      case XObject.CLASS_NUMBER:
        {
          if(javaClass == java.lang.String.class)
            return xobj.str();
          else 
          {
            return convertDoubleToNumber(xobj.num(), javaClass);
          }
        }
        // break; Unreachable
        
      case XObject.CLASS_STRING:
        {
          if((javaClass == java.lang.String.class) ||
             (javaClass == java.lang.Object.class))
            return xobj.str();
          else if(javaClass == Character.TYPE)
          {
            String str = xobj.str();
            if(str.length() > 0)
              return new Character(str.charAt(0));
            else
              return null; // ??
          }
          else 
          {
            return convertDoubleToNumber(xobj.num(), javaClass);
          }
        }
        // break; Unreachable
        
      case XObject.CLASS_RTREEFRAG:
        {
          if((NodeIterator.class.isAssignableFrom(javaClass)) ||
             (javaClass == java.lang.Object.class))
          {
            // This will fail in Xalan right now, since RTFs aren't 
            // convertable to node-sets.
            return xobj.nodeset();
          }
          else if(Node.class.isAssignableFrom(javaClass))
          {
            // This will return a Document fragment in Xalan right 
            // now, which isn't what the we specify.
            return xobj.rtree();
          }
          else if(org.w3c.dom.DocumentFragment.class.isAssignableFrom(javaClass))
          {
            // This will return a Document fragment in Xalan right 
            // now, which isn't what the we specify.
            return xobj.rtree();
          }
          else if(javaClass == java.lang.String.class)
          {
            return xobj.str();
          }
          else if(javaClass == Boolean.TYPE)
          {
            return new Boolean(xobj.bool());
          }
          else
          {
            return convertDoubleToNumber(xobj.num(), javaClass);
          }
        }
        // break; Unreachable
        
      case XObject.CLASS_NODESET:
        {
          if((NodeIterator.class.isAssignableFrom(javaClass)) ||
             (javaClass == java.lang.Object.class))
          {
            return xobj.nodeset();
          }
          else if(NodeList.class.isAssignableFrom(javaClass))
          {
            return xobj.nodeset();
          }
          else if(Node.class.isAssignableFrom(javaClass))
          {
            // Xalan ensures that nodeset() always returns an
            // iterator positioned at the beginning.
            NodeIterator ni = xobj.nodeset();
            return ni.nextNode(); // may be null.
          }
          else if(javaClass == java.lang.String.class)
          {
            return xobj.str();
          }
          else if(javaClass == Boolean.TYPE)
          {
            return new Boolean(xobj.bool());
          }
          else
          {
            return convertDoubleToNumber(xobj.num(), javaClass);
          }
        }
        // break; Unreachable
        
        // No default:, fall-through on purpose
      } // end switch
      xsltObj = xobj.object();
      
    } // end if if(xsltObj instanceof XObject)
    
    // At this point, we have a raw java object.
    if(javaClass == java.lang.String.class)
    {
      return xsltObj.toString();
    }
    else if(javaClass.isPrimitive())
    {
      // Assume a number conversion
      XString xstr = new XString(xsltObj.toString());
      double num = xstr.num();
      return convertDoubleToNumber(num, javaClass);
    }
    else
    {
      // Just pass the object directly, and hope for the best.
      return xsltObj;
    }
  }
  
  /**
   * Do a standard conversion of a double to the specified type.
   * @param num The number to be converted.
   * @param javaClass The class type to be converted to.
   * @return An object specified by javaClass, or a Double instance.
   */
  static Object convertDoubleToNumber(double num, Class javaClass)
  {
    // In the code below, I don't check for NaN, etc., instead 
    // using the standard Java conversion, as I think we should 
    // specify.  See issue-runtime-errors.
    if((javaClass == Double.TYPE) ||
       (javaClass == java.lang.Double.class))
      return new Double(num);
    else if(javaClass == Float.TYPE)
      return new Float(num);
    else if(javaClass == Long.TYPE)
    {
      // Use standard Java Narrowing Primitive Conversion
      // See http://java.sun.com/docs/books/jls/html/5.doc.html#175672
      return new Long((long)num);
    }
    else if(javaClass == Integer.TYPE)
    {
      // Use standard Java Narrowing Primitive Conversion
      // See http://java.sun.com/docs/books/jls/html/5.doc.html#175672
      return new Integer((int)num);
    }
    else if(javaClass == Short.TYPE)
    {
      // Use standard Java Narrowing Primitive Conversion
      // See http://java.sun.com/docs/books/jls/html/5.doc.html#175672
      return new Short((short)num);
    }
    else if(javaClass == Character.TYPE)
    {
      // Use standard Java Narrowing Primitive Conversion
      // See http://java.sun.com/docs/books/jls/html/5.doc.html#175672
      return new Character((char)num);
    }
    else if(javaClass == Byte.TYPE)
    {
      // Use standard Java Narrowing Primitive Conversion
      // See http://java.sun.com/docs/books/jls/html/5.doc.html#175672
      return new Byte((byte)num);
    }
    else
    {
      // Should never get here??
      return new Double(num);
    }
  }

}
