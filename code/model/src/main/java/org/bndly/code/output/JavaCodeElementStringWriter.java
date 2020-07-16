package org.bndly.code.output;

/*-
 * #%L
 * Code Model
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.bndly.code.model.CodeArrayForLoop;
import org.bndly.code.model.CodeBlock;
import org.bndly.code.model.CodeBlockIndented;
import org.bndly.code.model.CodeBracket;
import org.bndly.code.model.CodeElement;
import org.bndly.code.model.CodeElseBlock;
import org.bndly.code.model.CodeElseIfBlock;
import org.bndly.code.model.CodeForInLoop;
import org.bndly.code.model.CodeForLoop;
import org.bndly.code.model.CodeIfBlock;
import org.bndly.code.model.CodeImport;
import org.bndly.code.model.CodeLine;
import org.bndly.code.model.CodePackage;
import org.bndly.code.renderer.ImportResolver;
import java.util.List;

public class JavaCodeElementStringWriter {

	public static String toString(CodeElement e) {
		StringBuffer sb = new StringBuffer();
		string(sb, e, 0, true);
		return sb.toString();
	}

	private static void string(StringBuffer sb, CodeElement e, int i, boolean isFirstLine) {
		if (e.is(CodeIfBlock.class)) {
			stringIfBlock(sb, (CodeIfBlock) e, i, isFirstLine);
		} else if (e.is(CodeElseIfBlock.class)) {
			stringElseIfBlock(sb, (CodeElseIfBlock) e, i, isFirstLine);
		} else if (e.is(CodeElseBlock.class)) {
			stringElseBlock(sb, (CodeElseBlock) e, i, isFirstLine);
		} else if (e.is(CodeForInLoop.class)) {
			stringForInLoop(sb, (CodeForInLoop) e, i, isFirstLine);
		} else if (e.is(CodeForLoop.class)) {
			stringForLoop(sb, (CodeForLoop) e, i, isFirstLine);
		} else if (e.is(CodePackage.class)) {
			stringPackage(sb, (CodePackage) e, i, isFirstLine);
		} else if (e.is(CodeBracket.class)) {
			stringBracket(sb, (CodeBracket) e, i, isFirstLine);
		} else if (e.is(CodeBlockIndented.class)) {
			stringBlockIndented(sb, (CodeBlockIndented) e, i, isFirstLine);
		} else if (e.is(CodeBlock.class)) {
			stringBlock(sb, (CodeBlock) e, i, isFirstLine);
		} else if (e.is(CodeImport.class)) {
			stringImport(sb, (CodeImport) e, i, isFirstLine);
		} else if (e.is(CodeLine.class)) {
			stringLine(sb, (CodeLine) e, i, isFirstLine);
		}
	}

	private static void stringLine(StringBuffer sb, CodeLine e, int i, boolean isFirstLine) {
		if (!isFirstLine) {
			sb.append('\n');
		}
		sb.append(getIndent(i));
		sb.append(e.getValue());
	}

	private static void stringImport(StringBuffer sb, CodeImport e, int i, boolean isFirstLine) {
		ImportResolver importResolver = e.getContext().getImportResolver();
		String v = null;
		if (importResolver != null) {
			String typeName = e.getTypeName();
			String fullName = importResolver.resolveImport(typeName);
			if (e.getImportedFromWithin() == null) {
				v = "import " + fullName + ";";
			} else {
				if (!fullName.startsWith(e.getImportedFromWithin())) {
					v = "import " + fullName + ";";
				}
			}
			sb.append('\n');
			sb.append(getIndent(i));
			sb.append(v);
		} else {
			throw new IllegalStateException("no ImportResolver registered in the CodeGenerationContext.");
		}
	}

	private static void stringBlock(StringBuffer sb, CodeBlock e, int i, boolean isFirstLine) {
		List<CodeElement> elements = e.getElements();
		if (elements != null) {
			for (CodeElement codeElement : elements) {
				string(sb, codeElement, i, isFirstLine);
				isFirstLine = false;
			}
		}
	}

	private static void stringBlockIndented(StringBuffer sb, CodeBlockIndented e, int i, boolean isFirstLine) {
		stringBlock(sb, e, i + 1, isFirstLine);
	}

	private static void stringBracket(StringBuffer sb, CodeBracket e, int i, boolean isFirstLine) {
		sb.append(" {");
		stringBlock(sb, e, i + 1, isFirstLine);
		sb.append('\n');
		sb.append(getIndent(i));
		sb.append('}');
	}

	private static void stringPackage(StringBuffer sb, CodePackage e, int i, boolean isFirstLine) {
		sb.append(getIndent(i));
		sb.append("package ").append(e.getPackageName()).append(";");
		List<CodeElement> elements = e.getElements();
		if (elements != null) {
			for (CodeElement codeElement : elements) {
				string(sb, codeElement, i, isFirstLine);
				isFirstLine = false;
			}
		}
		sb.append('\n');
		sb.append(getIndent(i));
	}

	private static void stringForInLoop(StringBuffer sb, CodeForInLoop e, int i, boolean isFirstLine) {
		String it = e.getIteratorVariable();
		String list = e.getListVariable();

		sb.append('\n');
		sb.append(getIndent(i));
		sb.append("for (").append(it).append(" in ").append(list).append(") {");
		List<CodeElement> elements = e.getElements();
		if (elements != null) {
			for (CodeElement codeElement : elements) {
				string(sb, codeElement, i + 1, isFirstLine);
				isFirstLine = false;
			}
		}
		sb.append('\n');
		sb.append(getIndent(i));
		sb.append("}");
	}

	private static void stringForLoop(StringBuffer sb, CodeForLoop e, int i, boolean isFirstLine) {
		String it = e.getIteratorVariable();
		String list = e.getListVariable();

		sb.append('\n');
		sb.append(getIndent(i));
		String countAccess = "size()";
		if (CodeArrayForLoop.class.isAssignableFrom(e.getClass())) {
			countAccess = "length";
		}
		sb.append("for (int ").append(it).append("=0; ").append(it).append("<").append(list).append(".").append(countAccess).append("; ").append(it).append("++) {");
		List<CodeElement> elements = e.getElements();
		if (elements != null) {
			for (CodeElement codeElement : elements) {
				string(sb, codeElement, i + 1, isFirstLine);
				isFirstLine = false;
			}
		}
		sb.append('\n');
		sb.append(getIndent(i));
		sb.append("}");
	}

	private static void stringElseBlock(StringBuffer sb, CodeElseBlock e, int i, boolean isFirstLine) {
		sb.append("else {");
		List<CodeElement> elements = e.getElements();
		if (elements != null) {
			for (CodeElement codeElement : elements) {
				string(sb, codeElement, i + 1, isFirstLine);
				isFirstLine = false;
			}
		}
		sb.append('\n');
		sb.append(getIndent(i));
		sb.append("}");
	}

	private static void stringIfBlock(StringBuffer sb, CodeIfBlock e, int i, boolean isFirstLine) {
		sb.append('\n');
		sb.append(getIndent(i));

		String condition = e.getCondition();
		sb.append("if ( ").append(condition).append(" ) {");
		List<CodeElement> elements = e.getElements();
		if (elements != null) {
			for (CodeElement codeElement : elements) {
				string(sb, codeElement, i + 1, isFirstLine);
				isFirstLine = false;
			}
		}
		sb.append('\n');
		sb.append(getIndent(i));
		sb.append("}");
	}

	private static void stringElseIfBlock(StringBuffer sb, CodeElseIfBlock e, int i, boolean isFirstLine) {
		sb.append('\n');
		sb.append(getIndent(i));

		String condition = e.getCondition();
		sb.append("else if ( ").append(condition).append(" ) {");
		List<CodeElement> elements = e.getElements();
		if (elements != null) {
			for (CodeElement codeElement : elements) {
				string(sb, codeElement, i + 1, isFirstLine);
				isFirstLine = false;
			}
		}
		sb.append('\n');
		sb.append(getIndent(i));
		sb.append("}");
	}

	private static String getIndent(int indent) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < indent; i++) {
			sb.append("\t");
		}
		return sb.toString();
	}
}
