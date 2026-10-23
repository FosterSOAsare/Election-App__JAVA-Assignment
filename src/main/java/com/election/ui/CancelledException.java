package com.election.ui;

/**
 * The person typed 0 at a question inside a multi-step flow (Create election, Add categories, ...).
 * It is unchecked so it can leave from any question in the flow at once; the menu that started the flow
 * catches it and says nothing was saved. It is not a business error, so it is not an ElectionException.
 */
public class CancelledException extends RuntimeException {

    public CancelledException() {
        super("Cancelled");
    }
}
