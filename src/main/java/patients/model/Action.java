package patients.model;

public enum Action {
    VIEW_LIST("View patients"),
    ADD_PATIENT("Add patient"),
    EDIT_PATIENT("Edit Patient");

    public final String label;

    private Action(String label) {
        this.label = label;
    }
}
