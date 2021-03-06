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

import com.google.common.collect.ImmutableList;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(
  key = "S2114",
  name = "Collections should not be passed as arguments to their own methods",
  tags = {"bug"},
  priority = Priority.CRITICAL)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
@SqaleConstantRemediation("15min")
public class CollectionCallingItselfCheck extends SubscriptionBaseVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    if (hasSemantic()) {
      MethodInvocationTree methodInvocationTree = (MethodInvocationTree) tree;
      Symbol symbolReference = null;
      Symbol method = null;
      String reportedName = "";
      if (methodInvocationTree.methodSelect().is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) methodInvocationTree.methodSelect();
        IdentifierTree identifier = mse.identifier();
        reportedName = identifier.name();
        method = identifier.symbol();
        if (mse.expression().is(Tree.Kind.IDENTIFIER)) {
          symbolReference = ((IdentifierTree) mse.expression()).symbol();
        }
      }
      if (symbolReference != null && isMethodFromCollection(method)) {
        reportIssueForParameters(methodInvocationTree, symbolReference, reportedName);
      }
    }
  }

  private void reportIssueForParameters(MethodInvocationTree methodInvocationTree, Symbol symbolReference, String reportedName) {
    for (ExpressionTree arg : methodInvocationTree.arguments()) {
      if (arg.is(Tree.Kind.IDENTIFIER)) {
        Symbol reference = ((IdentifierTree) arg).symbol();
        if (reference == symbolReference) {
          addIssue(methodInvocationTree, "Remove or correct this \"" + reportedName + "\" call.");
        }
      }
    }
  }

  private static boolean isMethodFromCollection(Symbol methodSymbol) {
    if(!methodSymbol.isMethodSymbol()) {
      return false;
    }
    Type ownerType = methodSymbol.owner().type();
    return !ownerType.is("java.util.Collection") && ownerType.isSubtypeOf("java.util.Collection");
  }

}
