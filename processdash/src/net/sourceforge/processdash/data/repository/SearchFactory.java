// Copyright (C) 2003-2006 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
//
// The author(s) may be contacted at:
// Process Dashboard Group
// c/o Ken Raisor
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
//
// E-Mail POC:  processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.data.repository;

import java.io.IOException;
import java.io.Serializable;



import net.sourceforge.processdash.data.ListFunction;
import net.sourceforge.processdash.data.compiler.CompilationException;
import net.sourceforge.processdash.data.compiler.CompiledScript;
import net.sourceforge.processdash.data.compiler.Compiler;
import net.sourceforge.processdash.data.compiler.node.ASearchDeclaration;
import net.sourceforge.processdash.data.compiler.node.ASimpleSearchDeclaration;
import net.sourceforge.processdash.data.compiler.node.TIdentifier;
import net.sourceforge.processdash.data.compiler.node.PValue;
import net.sourceforge.processdash.data.compiler.analysis.DepthFirstAdapter;

class SearchFactory implements ListFunction, Serializable {

    protected transient PValue expression;
    protected String start, tag, saveString;

    public SearchFactory(ASearchDeclaration decl) {
        this(decl.getExpression(),
             Compiler.trimDelim(decl.getStart()),
             Compiler.trimDelim(decl.getTag()));
    }

    public SearchFactory(ASimpleSearchDeclaration decl) {
        this(null,
             Compiler.trimDelim(decl.getStart()),
             Compiler.trimDelim(decl.getTag()));
    }


    private SearchFactory(PValue expression, String start, String tag) {
        // determine the expression to evaluate.
        this.expression = expression;

        // determine the starting prefix.
        this.start = start;
        if (this.start.equals(".") || this.start.equals("./"))
            this.start = "";

        // determine the tag we are looking for.
        this.tag = tag;
    }

    public SearchFunction buildFor(String name, DataRepository data,
                                   String prefix) {
        CompiledScript script = null;
        if (expression != null) try {
            // normalize "unvarying" references - that is,
            // references marked with braces like [{this}]
            PValue expr = (PValue) expression.clone();
            expr.apply(new NormalizeReferences(data, prefix));

            // compile the expression into a script.
            script = Compiler.compile(expr);
            script = Compiler.exprAndDefined(script, tag);
        } catch (CompilationException ce) {
            return null;
        }
        return new SearchFunction(this, name, DataRepository.createDataName(
                prefix, start), tag, script, data, prefix);
    }

    public boolean matches(Object o) {
        if (o instanceof SearchFunction) {
            SearchFunction sf = (SearchFunction) o;
            return sf.factory == this;
        }
        return false;
    }

    private String expressionStr = null;

    private void writeObject(java.io.ObjectOutputStream out)
        throws IOException
    {
        if (expression != null)
            expressionStr = expression.toString();
        out.defaultWriteObject();
    }
    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        if (expressionStr != null) try {
            expression = Compiler.compileVal(expressionStr);
            expressionStr = null;
        } catch (Exception e) {}
    }


    private class NormalizeReferences extends DepthFirstAdapter {
        DataRepository data;
        String prefix;
        public NormalizeReferences(DataRepository data, String prefix) {
            this.data = data;  this.prefix = prefix; }
        public void caseTIdentifier(TIdentifier node) {
            String text = Compiler.trimDelim(node);
            if (text.startsWith("{") && text.endsWith("}")) {
                text = text.substring(1, text.length()-1);
                text = DataRepository.createDataName(prefix, text);
                node.setText("[" + text + "]");
            }
        }
    }
}
