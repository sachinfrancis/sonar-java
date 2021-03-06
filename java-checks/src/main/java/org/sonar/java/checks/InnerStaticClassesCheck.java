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
package org.sonar.java.checks;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.Deque;
import java.util.LinkedList;

@Rule(
  key = "S2694",
  name = "Inner classes which do not reference their owning classes should be \"static\"",
  tags = {"performance"},
  priority = Priority.MAJOR)
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.MEMORY_EFFICIENCY)
@SqaleConstantRemediation("15min")
public class InnerStaticClassesCheck extends BaseTreeVisitor implements JavaFileScanner {

  private JavaFileScannerContext context;
  private Deque<Symbol> outerClasses = new LinkedList<>();
  private Deque<Boolean> atLeastOneReference = new LinkedList<>();

  @Override
  public void scanFile(final JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());
  }

  @Override
  public void visitClass(ClassTree tree) {
    Symbol.TypeSymbol symbol = tree.symbol();
    if (!tree.is(Tree.Kind.CLASS)) {
      return;
    }
    outerClasses.push(symbol);
    atLeastOneReference.push(Boolean.FALSE);
    scan(tree.members());
    Boolean oneReference = atLeastOneReference.pop();
    outerClasses.pop();
    if (!symbol.isStatic() && !oneReference && !outerClasses.isEmpty() && isFirstParentStatic(outerClasses)) {
      String named = symbol.name().isEmpty() ? "named " : "";
      context.addIssue(tree, this, "Make this a " + named + "\"static\" inner class.");
    }
  }

  private static boolean isFirstParentStatic(Deque<Symbol> outerClasses) {
    if (outerClasses.size() == 1) {
      return true;
    }
    for (Symbol outerClass : outerClasses) {
      if (outerClass.isStatic()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void visitIdentifier(IdentifierTree tree) {
    super.visitIdentifier(tree);
    checkSymbol(tree.symbol());
  }

  @Override
  public void visitNewClass(NewClassTree tree) {
    super.visitNewClass(tree);
    checkSymbol(tree.symbolType().symbol());
  }

  private void checkSymbol(Symbol symbol) {
    if (!atLeastOneReference.isEmpty() && !atLeastOneReference.peek() && referenceInstance(symbol)) {
      atLeastOneReference.pop();
      atLeastOneReference.push(Boolean.TRUE);
    }
  }

  private boolean referenceInstance(Symbol symbol) {
    Symbol owner = symbol.owner();
    return !outerClasses.peek().equals(owner) && (symbol.isUnknown() || (!symbol.isStatic() && fromInstance(owner)));
  }

  private boolean fromInstance(Symbol owner) {
    for (Symbol outerClass : outerClasses) {
      Type ownerType = owner.type();
      if (owner.equals(outerClass) || (ownerType != null && outerClass.type().isSubtypeOf(ownerType))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void visitVariable(VariableTree tree) {
    Symbol symbol = tree.symbol();
    if (symbol != null && !symbol.isStatic()) {
      super.visitVariable(tree);
    }
  }

}
