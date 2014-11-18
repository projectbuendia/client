package org.odk.collect.android.logic;

import org.javarosa.core.model.FormIndex;
import org.javarosa.form.api.FormEntryController;

import java.util.ArrayList;
import java.util.List;

/**
 * An object that traverses a form, calling {@link FormVisitor#visit} for each element in the form.
 */
public class FormTraverser {

    private final List<FormVisitor> mVisitors;

    private FormTraverser(List<FormVisitor> visitors) {
        mVisitors = new ArrayList<FormVisitor>(visitors);
    }

    public void traverse(FormController controller) {
        FormIndex originalFormIndex = controller.getFormIndex();

        try {
            controller.jumpToIndex(FormIndex.createBeginningOfFormIndex());

            int event = controller.getEvent();
            while (event != FormEntryController.EVENT_END_OF_FORM) {
                for (FormVisitor visitor : mVisitors) {
                    visitor.visit(event, controller);
                }
            }
        } finally {
            controller.jumpToIndex(originalFormIndex);
        }
    }

    public static class Builder {

        private List<FormVisitor> mVisitors = new ArrayList<FormVisitor>();

        public Builder addVisitor(FormVisitor visitor) {
            mVisitors.add(visitor);
            return this;
        }

        public FormTraverser build() {
            return new FormTraverser(mVisitors);
        }
    }
}
