/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model.statement;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.sonar.sslr.api.AstNode;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.WhileStatementTree;

import java.util.Iterator;

public class WhileStatementTreeImpl extends JavaTree implements WhileStatementTree {
  private final ExpressionTree condition;
  private final StatementTree statement;
  private final InternalSyntaxToken whileKeyword;
  private final InternalSyntaxToken openParenToken;
  private final InternalSyntaxToken closeParenToken;

  public WhileStatementTreeImpl(InternalSyntaxToken whileKeyword, InternalSyntaxToken openParenToken, ExpressionTree condition, InternalSyntaxToken closeParenToken,
    StatementTree statement) {

    super(Kind.WHILE_STATEMENT);
    this.whileKeyword = whileKeyword;
    this.openParenToken = openParenToken;
    this.condition = Preconditions.checkNotNull(condition);
    this.closeParenToken = closeParenToken;
    this.statement = Preconditions.checkNotNull(statement);

    addChild(whileKeyword);
    addChild(openParenToken);
    addChild((AstNode) condition);
    addChild(closeParenToken);
    addChild((AstNode) statement);
  }

  @Override
  public Kind getKind() {
    return Kind.WHILE_STATEMENT;
  }

  @Override
  public SyntaxToken whileKeyword() {
    return whileKeyword;
  }

  @Override
  public SyntaxToken openParenToken() {
    return openParenToken;
  }

  @Override
  public ExpressionTree condition() {
    return condition;
  }

  @Override
  public SyntaxToken closeParenToken() {
    return closeParenToken;
  }

  @Override
  public StatementTree statement() {
    return statement;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitWhileStatement(this);
  }

  @Override
  public Iterator<Tree> childrenIterator() {
    return Iterators.forArray(
      condition,
      statement);
  }

}
