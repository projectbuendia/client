package org.odk.collect.android.logic;

/**
 * A visitor of forms traversed by {@link FormTraverser}.
 */
public interface FormVisitor {

    void visit(int event, FormController formController);
}
