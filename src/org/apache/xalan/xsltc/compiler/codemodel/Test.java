/*
 * @(#)$Id$
 *
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
 * originally based on software copyright (c) 2001, Sun
 * Microsystems., http://www.sun.com.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * @author Santiago Pericas-Geertsen
 *
 */

package org.apache.xalan.xsltc.compiler.codemodel;

import java.util.List;
import java.io.OutputStreamWriter;

public class Test {

    /*
    public class Factorial {
	int n;

	public Factorial(int m) {
	    n = m;
	}

	public int getValue() {
	    if (n > 0) {
                return factorial(n);
            }
            else {
                return 1;
            }
	}

	private static int factorial(int n) {
	    return (n == 0) ? 1 : n * factorial(n - 1);
	}
    }
    */

    public static void main(String[] args) {
	CmVariableRefExpr n = new CmVariableRefExpr("n");

	CmStatement ret =
	    new CmReturnStmt(
		new CmConditionalExpr(
		    new CmBinaryExpr(n,
				   CmOperator.EQ,
				   CmIntegerExpr.zero),
		    CmIntegerExpr.one,
		    new CmBinaryExpr(n,
				   CmOperator.STAR,
				   new CmMethodCallExpr("factorial",
					new CmBinaryExpr(n,
						       CmOperator.MINUS,
						       CmIntegerExpr.one)))));
	CmMethodDecl factorial =
	    new CmMethodDecl(CmModifier.PRIVATE | CmModifier.STATIC,
                           CmIntegerType.instance,
			   "factorial",
			   new CmParameterDecl(CmIntegerType.instance, "n"),
			   ret);

	CmStatement ret2 =
            new CmIfStmt(
                new CmBinaryExpr(n, CmOperator.GT, CmIntegerExpr.zero),
	        new CmReturnStmt(new CmMethodCallExpr("factorial", n)),
                new CmReturnStmt(CmIntegerExpr.one));

	CmMethodDecl getValue =
	    new CmMethodDecl(CmModifier.PUBLIC, CmIntegerType.instance, "getValue",
                (List) null, ret2);

	CmStatement assign =
	    new CmExprStmt(
		new CmBinaryExpr(n, CmOperator.ASGN, new CmVariableRefExpr("m")));

	CmMethodDecl init =
	    new CmMethodDecl(CmModifier.PUBLIC,
                           null,	// indicates that this is a constructor
			   "Factorial",
			   new CmParameterDecl(CmIntegerType.instance, "m"),
			   assign);

        CmClassDecl classDecl =
            new CmClassDecl(CmModifier.PUBLIC, "Factorial", null, null);
        classDecl.addCmDeclaration(new CmVariableDecl(CmIntegerType.instance, "n"))
                 .addCmDeclaration(init)
                 .addCmDeclaration(getValue)
                 .addCmDeclaration(factorial);

	JavaCmVisitor visitor = new JavaCmVisitor(new OutputStreamWriter(System.out));
	classDecl.accept(visitor, null);
        visitor.flush();
    }

    /*
	1 + 2;
    public static void main(String[] args) {
	CmExpression expr =
	    new CmBinaryExpr(CmIntegerExpr.one, CmOperator.PLUS, CmIntegerExpr.two);
	JavaCmVisitor visitor = new JavaCmVisitor(new OutputStreamWriter(System.out));
	expr.accept(visitor, null);
        visitor.flush();
    }
     */
}